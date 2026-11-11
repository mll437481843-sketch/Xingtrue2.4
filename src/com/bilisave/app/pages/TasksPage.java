package com.bilisave.app.pages;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bilisave.app.AppState;
import com.bilisave.app.MainActivity;
import com.bilisave.app.data.Task;
import com.bilisave.app.download.DownloadService;
import com.bilisave.app.ui.BarView;
import com.bilisave.app.ui.Monet;
import com.bilisave.app.ui.Ui;

public class TasksPage extends ScrollView {
    private final MainActivity act;
    private final Monet m;
    private LinearLayout box;

    public TasksPage(MainActivity a) {
        super(a);
        act = a;
        m = a.theme();
        setFillViewport(true);
        setClipToPadding(false);
        setVerticalScrollBarEnabled(false);
        build();
    }

    private int dp(float v) {
        return Ui.dp(getContext(), v);
    }

    private void build() {
        removeAllViews();
        box = Ui.column(getContext());
        box.setPadding(dp(20), dp(6), dp(20), dp(28));
        addView(box, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout head = Ui.row(getContext());
        TextView title = Ui.text(getContext(), "下载任务", 16f, m.onSurface, true);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView clear = Ui.softButton(getContext(), m, "清空已完成");
        clear.setOnClickListener(v -> {
            for (int i = AppState.I.tasks.size() - 1; i >= 0; i--) {
                String s = AppState.I.tasks.get(i).state;
                if (Task.DONE.equals(s) || Task.FAIL.equals(s)) AppState.I.tasks.remove(i);
            }
            AppState.I.save(getContext());
            build();
        });
        head.addView(clear);
        box.addView(head);

        if (AppState.I.tasks.isEmpty()) {
            LinearLayout empty = Ui.card(getContext(), m);
            TextView t = Ui.text(getContext(), "还没有任务。\n回首页解析一个视频，挑好清晰度就会开始下载。", 13.5f,
                    Ui.alpha(m.onSurfaceVar, 235), false);
            t.setLineSpacing(dp(4), 1f);
            empty.addView(t);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(16);
            box.addView(empty, lp);
            return;
        }

        for (Task t : AppState.I.tasks) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(12);
            box.addView(taskCard(t), lp);
        }
    }

    private View taskCard(Task t) {
        LinearLayout card = Ui.card(getContext(), m);
        TextView title = Ui.text(getContext(), t.title == null || t.title.isEmpty() ? t.bvid : t.title, 15f, m.onSurface, true);
        title.setMaxLines(2);
        title.setLineSpacing(dp(2), 1f);
        card.addView(title);

        String sub = (t.part != null && !t.part.isEmpty() && !t.part.equals(t.title) ? t.part + " · " : "")
                + (t.audioOnly ? "音频" : (t.qlabel == null ? "" : t.qlabel)) + " · " + t.sizeText();
        TextView meta = Ui.text(getContext(), sub, 12f, Ui.alpha(m.onSurfaceVar, 230), false);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = dp(6);
        card.addView(meta, mlp);

        BarView bar = new BarView(getContext(), m);
        boolean failed = Task.FAIL.equals(t.state);
        if (failed) bar.setColors(Ui.alpha(m.error, 50), m.error);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(7));
        blp.topMargin = dp(12);
        card.addView(bar, blp);
        bar.setProgress(t.pct() / 100f);

        String status = t.state;
        if (Task.RUNNING.equals(t.state) || Task.MERGING.equals(t.state)) status = t.state + " · " + t.pct() + "%";
        if (failed && t.error != null && !t.error.isEmpty()) status = "失败：" + t.error;
        if (Task.INTERRUPTED.equals(t.state)) status = "已中断，点重试可续传";
        TextView st = Ui.text(getContext(), status, 12f, failed ? m.error : Ui.alpha(m.onSurfaceVar, 235), false);
        st.setMaxLines(2);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = dp(8);
        card.addView(st, slp);

        if (t.path != null && !t.path.isEmpty()) {
            TextView path = Ui.text(getContext(), t.path, 11f, Ui.alpha(m.onSurfaceVar, 180), false);
            path.setMaxLines(2);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
            plp.topMargin = dp(4);
            card.addView(path, plp);
        }

        LinearLayout actions = Ui.row(getContext());
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = dp(12);
        if (Task.DONE.equals(t.state)) {
            TextView open = Ui.softButton(getContext(), m, "打开");
            open.setOnClickListener(v -> openFile(t.path, t.audioOnly));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.rightMargin = dp(8);
            actions.addView(open, p);
        }
        if (failed || Task.INTERRUPTED.equals(t.state)) {
            TextView retry = Ui.softButton(getContext(), m, "重试");
            retry.setOnClickListener(v -> {
                t.state = Task.PENDING;
                t.bytes = 0;
                t.error = "";
                AppState.I.save(getContext());
                DownloadService.kick(getContext());
                build();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.rightMargin = dp(8);
            actions.addView(retry, p);
        }
        TextView del = Ui.softButton(getContext(), m, "删除");
        del.setOnClickListener(v -> {
            AppState.I.tasks.remove(t);
            AppState.I.save(getContext());
            build();
        });
        actions.addView(del);
        card.addView(actions, alp);
        return card;
    }

    private void openFile(String path, boolean audio) {
        if (path == null || path.isEmpty()) {
            act.toast("文件路径为空");
            return;
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        ContentResolver cr = getContext().getContentResolver();
        Uri collection = audio ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Uri found = null;
        Cursor c = null;
        try {
            c = cr.query(collection, new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DISPLAY_NAME + "=?", new String[]{name},
                    MediaStore.MediaColumns.DATE_ADDED + " DESC");
            if (c != null && c.moveToFirst()) {
                long id = c.getLong(0);
                found = android.content.ContentUris.withAppendedId(collection, id);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        if (found == null) {
            act.toast("文件在 " + path + "，可用相册/文件管理器打开");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(found, audio ? "audio/mp4" : "video/mp4");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getContext().startActivity(i);
        } catch (Exception e) {
            act.toast("没有可用的播放器：" + e.getMessage());
        }
    }

    public void refresh() {
        build();
    }
}

