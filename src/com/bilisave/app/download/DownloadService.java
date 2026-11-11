package com.bilisave.app.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.bilisave.app.AppState;
import com.bilisave.app.MainActivity;
import com.bilisave.app.data.Api;
import com.bilisave.app.data.Models;
import com.bilisave.app.data.Task;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Foreground service that survives the app being backgrounded while DASH streams download + mux. */
public class DownloadService extends Service {
    private static final String CHANNEL = "downloads";
    private static final int NOTIF_ID = 0x810;
    public static final String ACTION_ADD = "com.bilisave.app.action.ADD";

    private ExecutorService pool;
    private final AtomicBoolean working = new AtomicBoolean(false);
    private PowerManager.WakeLock wake;

    @Override
    public void onCreate() {
        super.onCreate();
        pool = Executors.newSingleThreadExecutor();
        NotificationChannel ch = new NotificationChannel(CHANNEL, "下载任务", NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BiliSave:download");
            wake.setReferenceCounted(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundCompat(buildNotification());
        nudge();
        return START_NOT_STICKY;
    }

    private void startForegroundCompat(Notification n) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("BiliSave")
                .setContentText(currentText())
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private String currentText() {
        int run = 0, done = 0;
        String last = "";
        for (Task t : AppState.I.tasks) {
            if (Task.RUNNING.equals(t.state) || Task.MERGING.equals(t.state)) {
                run++;
                last = t.title;
            }
            if (Task.DONE.equals(t.state)) done++;
        }
        if (run > 0) return "正在处理：" + last;
        return done > 0 ? "已完成 " + done + " 个任务" : "准备下载";
    }

    private final Runnable progressTick = new Runnable() {
        long last = 0;

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (now - last < 400) return;
            last = now;
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                Notification n = new Notification.Builder(DownloadService.this, CHANNEL)
                        .setContentTitle("BiliSave")
                        .setContentText(currentText())
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setOngoing(true)
                        .setProgress(100, overallPct(), false)
                        .build();
                nm.notify(NOTIF_ID, n);
            }
        }
    };

    private int overallPct() {
        for (Task t : AppState.I.tasks)
            if (Task.RUNNING.equals(t.state) || Task.MERGING.equals(t.state)) return Math.max(1, t.pct());
        return 0;
    }

    public static void enqueue(Context c, Task t) {
        AppState.I.tasks.add(0, t);
        AppState.I.save(c);
        AppState.I.tasksChanged();
        Intent i = new Intent(c, DownloadService.class).setAction(ACTION_ADD);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void kick(Context c) {
        Intent i = new Intent(c, DownloadService.class).setAction(ACTION_ADD);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    private void nudge() {
        if (!working.compareAndSet(false, true)) return;
        if (wake != null) wake.acquire(60 * 60 * 1000L);
        pool.execute(() -> {
            try {
                while (true) {
                    Task next = null;
                    for (Task t : AppState.I.tasks) {
                        if (Task.PENDING.equals(t.state) || Task.INTERRUPTED.equals(t.state)) {
                            next = t;
                            break;
                        }
                    }
                    if (next == null) break;
                    runTask(next);
                }
            } finally {
                working.set(false);
                if (wake != null && wake.isHeld()) wake.release();
                stopForeground(true);
                stopSelf();
            }
        });
    }

    private void runTask(Task t) {
        t.state = Task.RUNNING;
        t.error = "";
        t.bytes = 0;
        t.total = 0;
        save();
        try {
            Api.refresh(this);
            Api.PlayResult play = Api.play(t.bvid, t.cid, t.qn);
            File dir = new File(getCacheDir(), "dl");
            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("无法创建缓存目录");
            String base = t.id + "_" + System.currentTimeMillis();

            if (!play.durlUrl.isEmpty() && play.videos.isEmpty()) {
                File out = new File(dir, base + ".mp4");
                t.total = play.durlSize;
                Api.download(play.durlUrl, "", out, progress(t, 0, play.durlSize));
                finish(t, out, false);
                return;
            }

            Models.Dash v = Api.pickVideo(play, t.qn);
            Models.Dash a = Api.pickAudio(play);
            if (v == null) throw new java.io.IOException("没有可用视频流（可能需要登录或大会员）");
            if (a == null && !t.audioOnly) throw new java.io.IOException("没有可用音频流");
            String qlabel = labelOf(play.formats, v.id);
            if (qlabel != null && !qlabel.isEmpty()) t.qlabel = qlabel;

            File vf = new File(dir, base + ".v.m4s");
            File af = new File(dir, base + ".a.m4s");
            long vt = v.size > 0 ? v.size : 0;
            long at = (a != null && a.size > 0) ? a.size : 0;
            t.total = vt + at;
            if (t.audioOnly) {
                t.total = at;
                Api.download(a.url, a.backup, af, progress(t, 0, at));
                finish(t, af, true);
                return;
            }
            Api.download(v.url, v.backup, vf, progress(t, 0, vt));
            Api.download(a.url, a.backup, af, progress(t, vt, at));

            t.state = Task.MERGING;
            notifyChanged();
            File out = new File(dir, base + ".mp4");
            try {
                Remuxer.merge(vf, af, out);
            } catch (Exception me) {
                if (out.exists()) out.delete();
                Api.PlayResult fallback = Api.progressive(t.bvid, t.cid, t.qn);
                if (fallback.durlUrl.isEmpty())
                    throw new java.io.IOException("合封失败：" + me.getMessage());
                t.qn = fallback.durlQn;
                Api.download(fallback.durlUrl, "", out, progress(t, 0, fallback.durlSize));
            }
            //noinspection ResultOfMethodCallIgnored
            vf.delete();
            //noinspection ResultOfMethodCallIgnored
            af.delete();
            finish(t, out, false);
        } catch (Exception e) {
            t.state = Task.FAIL;
            t.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            save();
            notifyChanged();
        }
    }

    private void finish(Task t, File merged, boolean audio) {
        try {
            String name = safe(t.title) + (t.part != null && !t.part.isEmpty() && !t.part.equals(t.title) ? " - " + safe(t.part) : "")
                    + " [" + (t.qlabel == null ? "" : t.qlabel.replace(" ", "")) + "]" + (audio ? ".m4a" : ".mp4");
            String path = Export.publish(this, merged, name, audio);
            //noinspection ResultOfMethodCallIgnored
            merged.delete();
            t.path = path;
            t.state = Task.DONE;
            t.bytes = t.total;
            t.error = "";
        } catch (Exception e) {
            t.state = Task.FAIL;
            t.error = "保存失败：" + e.getMessage();
        }
        save();
        notifyChanged();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\\\/:*?\"<>|\\n\\r\\t]", "_").trim();
    }

    private String labelOf(List<Models.Format> fs, int qn) {
        for (Models.Format f : fs) if (f.qn == qn) return f.label;
        return qn + "P";
    }

    private Api.Progress progress(Task t, long base, long total) {
        return new Api.Progress() {
            long lastUi = 0;

            @Override
            public void onProgress(long done, long totalNow) {
                t.bytes = base + done;
                if (totalNow > 0) t.total = Math.max(t.total, base + totalNow);
                long now = System.currentTimeMillis();
                if (now - lastUi > 250) {
                    lastUi = now;
                    AppState.I.tasksChanged();
                    progressTick.run();
                }
            }

            @Override
            public boolean cancelled() {
                return false;
            }
        };
    }

    private void save() {
        AppState.I.save(this);
    }

    private void notifyChanged() {
        AppState.I.tasksChanged();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        save();
        if (pool != null) pool.shutdownNow();
        if (wake != null && wake.isHeld()) wake.release();
        super.onDestroy();
    }
}


