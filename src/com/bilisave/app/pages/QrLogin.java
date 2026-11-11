package com.bilisave.app.pages;

import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bilisave.app.AppState;
import com.bilisave.app.MainActivity;
import com.bilisave.app.data.Api;
import com.bilisave.app.data.Prefs;
import com.bilisave.app.ui.Monet;
import com.bilisave.app.ui.QrView;
import com.bilisave.app.ui.Ui;

import java.util.List;

/** Bilibili QR login: generate → poll → cookies saved locally. */
public final class QrLogin {

    public static void show(MainActivity act, Runnable onDone) {
        new Session(act, onDone).open();
    }

    private static final class Session {
        final MainActivity act;
        final Monet m;
        final Runnable onDone;
        final Dialog dlg;
        final LinearLayout col;
        final FrameLayout qrHost;
        final TextView status;
        final Handler h = new Handler(Looper.getMainLooper());
        volatile String key;
        volatile boolean alive = true;
        volatile boolean finished = false;

        Session(MainActivity act, Runnable onDone) {
            this.act = act;
            this.m = act.theme();
            this.onDone = onDone;
            dlg = new Dialog(act);
            col = Ui.column(act);
            col.setPadding(dp(20), dp(18), dp(20), dp(16));
            col.setBackground(Ui.round(Ui.alpha(m.surfaceHi, 253), dp(26), dp(1), Ui.alpha(m.outline, 60)));

            col.addView(Ui.text(act, "扫码登录", 19f, m.onSurface, true));
            TextView sub = Ui.text(act, "用 B 站 App 扫下面的二维码，手机上点确认即可", 12f,
                    Ui.alpha(m.onSurfaceVar, 225), false);
            sub.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
            slp.topMargin = dp(6);
            col.addView(sub, slp);

            qrHost = new FrameLayout(act);
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(-1, dp(248));
            qlp.topMargin = dp(16);
            col.addView(qrHost, qlp);

            status = Ui.text(act, "正在生成二维码…", 13f, Ui.alpha(m.onSurfaceVar, 240), false);
            status.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(-1, -2);
            stlp.topMargin = dp(12);
            col.addView(status, stlp);

            LinearLayout row = Ui.row(act);
            TextView refresh = Ui.softButton(act, m, "刷新二维码");
            refresh.setOnClickListener(v -> load());
            TextView close = Ui.softButton(act, m, "关闭");
            close.setOnClickListener(v -> dlg.dismiss());
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2);
            rp.rightMargin = dp(10);
            row.addView(refresh, rp);
            row.addView(close);
            LinearLayout.LayoutParams rowlp = new LinearLayout.LayoutParams(-1, -2);
            rowlp.topMargin = dp(14);
            col.addView(row, rowlp);

            dlg.setContentView(col);
            dlg.setOnDismissListener(d -> {
                alive = false;
                h.removeCallbacksAndMessages(null);
            });
        }

        private int dp(float v) {
            return Ui.dp(act, v);
        }

        void open() {
            Window w = dlg.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                android.util.DisplayMetrics dm = act.getResources().getDisplayMetrics();
                w.setLayout((int) (dm.widthPixels * 0.9f), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dlg.show();
            load();
        }

        void load() {
            if (!alive) return;
            key = null;
            qrHost.removeAllViews();
            status.setText("正在生成二维码…");
            new Thread(() -> {
                try {
                    Api.refresh(act);
                    Api.Qr q = Api.qrGenerate();
                    act.runOnUiThread(() -> {
                        if (!alive) return;
                        key = q.key;
                        QrView view = new QrView(act, q.url);
                        qrHost.addView(view, new FrameLayout.LayoutParams(dp(248), dp(248), Gravity.CENTER));
                        status.setText("请用 B 站 App 扫描二维码");
                        h.postDelayed(this::poll, 1500);
                    });
                } catch (Exception e) {
                    act.runOnUiThread(() -> {
                        if (alive) status.setText("二维码生成失败：" + msg(e));
                    });
                }
            }).start();
        }

        void poll() {
            if (!alive || key == null || finished) return;
            new Thread(() -> {
                try {
                    Api.QrState s = Api.qrPoll(key);
                    act.runOnUiThread(() -> {
                        if (!alive || finished) return;
                        if (s.state == 2) {
                            status.setText("登录成功，正在写入登录态…");
                            save(s.cookie);
                        } else if (s.state == 3) {
                            status.setText("二维码已过期，点「刷新二维码」重来");
                        } else if (s.state == 1) {
                            status.setText("已扫码，请在手机上点确认");
                            h.postDelayed(this::poll, 1500);
                        } else {
                            h.postDelayed(this::poll, 1500);
                        }
                    });
                } catch (Exception e) {
                    act.runOnUiThread(() -> {
                        if (!alive) return;
                        status.setText("网络异常：" + msg(e));
                        h.postDelayed(this::poll, 2500);
                    });
                }
            }).start();
        }

        void save(String cookie) {
            finished = true;
            new Thread(() -> {
                try {
                    if (cookie == null || cookie.isEmpty()) throw new Exception("没有拿到 Cookie，请重试");
                    Prefs.setCookie(act, cookie);
                    Api.refresh(act);
                    String name = "B站账号";
                    String face = "";
                    long mid = 0;
                    try {
                        Api.User u = Api.verifyCookie(cookie);
                        name = u.name;
                        face = u.face;
                        mid = u.mid;
                    } catch (Exception ignored) {
                        // cookies are already valid for downloads even if nav is rate-limited
                    }
                    final String finalName = name;
                    final String finalFace = face;
                    List<Prefs.Account> list = Prefs.accounts(act);
                    String id = String.valueOf(mid == 0 ? finalName.hashCode() : mid);
                    boolean found = false;
                    for (Prefs.Account a : list) {
                        if (a.id.equals(id)) {
                            a.name = finalName;
                            a.face = finalFace;
                            a.cookie = cookie;
                            found = true;
                        }
                    }
                    if (!found) {
                        Prefs.Account a = new Prefs.Account();
                        a.id = id;
                        a.name = finalName;
                        a.face = finalFace;
                        a.cookie = cookie;
                        list.add(a);
                    }
                    Prefs.saveAccounts(act, list);
                    Prefs.setCurrentAccountId(act, id);
                    AppState.I.userName = finalName;
                    AppState.I.userFace = finalFace;
                    act.runOnUiThread(() -> {
                        dlg.dismiss();
                        act.toast("登录成功：" + finalName);
                        if (onDone != null) onDone.run();
                    });
                } catch (Exception e) {
                    act.runOnUiThread(() -> {
                        finished = false;
                        status.setText("登录失败：" + msg(e));
                        h.postDelayed(this::poll, 2000);
                    });
                }
            }).start();
        }

        static String msg(Exception e) {
            return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }
}

