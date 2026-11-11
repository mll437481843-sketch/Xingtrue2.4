package com.bilisave.app.pages;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bilisave.app.AppState;
import com.bilisave.app.MainActivity;
import com.bilisave.app.data.Api;
import com.bilisave.app.data.Prefs;
import com.bilisave.app.ui.ChipView;
import com.bilisave.app.ui.Monet;
import com.bilisave.app.ui.Toggle;
import com.bilisave.app.ui.Ui;

import java.util.List;

public class SettingsPage extends ScrollView {
    public static final int REQ_DIR = 0x71;
    private final MainActivity act;
    private final Monet m;
    private LinearLayout box;
    private EditText cookieField;
    private EditText uaField;
    private ImageView avatar;
    private TextView nameView;
    private TextView tutorial;
    private boolean tutorialOpen = false;

    public SettingsPage(MainActivity a) {
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

    private LinearLayout.LayoutParams top(int t) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(t);
        return lp;
    }

    private void build() {
        removeAllViews();
        box = Ui.column(getContext());
        box.setPadding(dp(20), dp(6), dp(20), dp(28));
        addView(box, new FrameLayout.LayoutParams(-1, -2));
        box.addView(accountCard());
        box.addView(downloadCard(), top(14));
        box.addView(appearanceCard(), top(14));
        box.addView(networkCard(), top(14));
        box.addView(aboutCard(), top(14));
    }

    private LinearLayout accountCard() {
        LinearLayout card = Ui.card(getContext(), m);
        card.addView(Ui.label(getContext(), m, "登录态（Cookie）"));

        LinearLayout who = Ui.row(getContext());
        who.setPadding(0, dp(12), 0, 0);
        avatar = new ImageView(getContext());
        avatar.setBackground(Ui.round(Ui.alpha(m.surfaceVar, 190), dp(28)));
        avatar.setClipToOutline(true);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        who.addView(avatar, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout texts = Ui.column(getContext());
        nameView = Ui.text(getContext(), currentName(), 15f, m.onSurface, true);
        TextView hint = Ui.text(getContext(), Prefs.cookie(getContext()).isEmpty() ? "未导入，未登录下载通常只有 480P" : "已导入，凭证只存在本机",
                11.5f, Ui.alpha(m.onSurfaceVar, 200), false);
        texts.addView(nameView);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-2, -2);
        hlp.topMargin = dp(3);
        texts.addView(hint, hlp);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -2, 1f);
        wlp.leftMargin = dp(14);
        who.addView(texts, wlp);
        card.addView(who);
        loadAvatar();

        TextView scan = Ui.primaryButton(getContext(), m, "扫码登录（推荐）");
        scan.setOnClickListener(v -> QrLogin.show(act, this::build));
        LinearLayout.LayoutParams scanlp = new LinearLayout.LayoutParams(-1, -2);
        scanlp.topMargin = dp(14);
        card.addView(scan, scanlp);

        TextView orText = Ui.text(getContext(), "或者手动粘贴 Cookie ↓", 11.5f, Ui.alpha(m.onSurfaceVar, 190), false);
        LinearLayout.LayoutParams orlp = new LinearLayout.LayoutParams(-1, -2);
        orlp.topMargin = dp(12);
        card.addView(orText, orlp);

        cookieField = new EditText(getContext());
        cookieField.setHint("SESSDATA=xxxx; bili_jct=yyyy");
        cookieField.setHintTextColor(Ui.alpha(m.onSurfaceVar, 150));
        cookieField.setTextColor(m.onSurface);
        cookieField.setTextSize(13f);
        cookieField.setMinLines(3);
        cookieField.setGravity(Gravity.TOP | Gravity.START);
        cookieField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        cookieField.setBackground(Ui.round(Ui.alpha(m.surfaceVar, m.dark ? 150 : 190), dp(18), dp(1), Ui.alpha(m.outline, 50)));
        cookieField.setPadding(dp(14), dp(12), dp(14), dp(12));
        String existing = Prefs.cookie(getContext());
        if (existing != null && !existing.isEmpty()) cookieField.setText(existing);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = dp(14);
        card.addView(cookieField, clp);

        LinearLayout actions = Ui.row(getContext());
        actions.setPadding(0, dp(12), 0, 0);
        TextView verify = Ui.primaryButton(getContext(), m, "保存并验证登录态");
        verify.setOnClickListener(v -> verifyCookie());
        TextView clear = Ui.softButton(getContext(), m, "清空");
        clear.setOnClickListener(v -> {
            Prefs.setCookie(getContext(), "");
            Prefs.setCurrentAccountId(getContext(), "");
            cookieField.setText("");
            AppState.I.userName = "";
            AppState.I.userFace = "";
            Api.refresh(getContext());
            build();
            act.toast("已清除本机登录态");
        });
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-2, -2);
        plp.rightMargin = dp(10);
        actions.addView(clear, plp);
        actions.addView(verify, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(actions);

        List<Prefs.Account> accounts = Prefs.accounts(getContext());
        if (!accounts.isEmpty()) {
            card.addView(Ui.label(getContext(), m, "已保存账号"), top(14));
            HorizontalScrollView hs = new HorizontalScrollView(getContext());
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout row = Ui.row(getContext());
            String cur = Prefs.currentAccountId(getContext());
            for (Prefs.Account a : accounts) {
                ChipView chip = new ChipView(getContext(), m, a.name == null || a.name.isEmpty() ? "账号" : a.name);
                chip.setActive(a.id.equals(cur));
                chip.setOnClickListener(v -> {
                    Prefs.setCookie(getContext(), a.cookie);
                    Prefs.setCurrentAccountId(getContext(), a.id);
                    AppState.I.userName = a.name;
                    AppState.I.userFace = a.face;
                    Api.refresh(getContext());
                    build();
                    act.toast("已切换到 " + a.name);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.rightMargin = dp(8);
                row.addView(chip, lp);
            }
            hs.addView(row);
            card.addView(hs, top(8));
        }

        TextView guide = Ui.softButton(getContext(), m, tutorialOpen ? "收起取 Cookie 方法" : "怎么取 Cookie？");
        guide.setOnClickListener(v -> {
            tutorialOpen = !tutorialOpen;
            build();
        });
        card.addView(guide, top(14));

        tutorial = Ui.text(getContext(), "", 12f, Ui.alpha(m.onSurfaceVar, 230), false);
        tutorial.setLineSpacing(dp(4), 1f);
        if (tutorialOpen) {
            tutorial.setText("1. 电脑浏览器登录 bilibili.com，按 F12 打开开发者工具\n"
                    + "2. Application / 应用 → Cookies → https://www.bilibili.com\n"
                    + "3. 找到 SESSDATA 与 bili_jct，按 SESSDATA=值; bili_jct=值 的格式粘进来\n"
                    + "4. 手机上可用带开发者工具的浏览器（Kiwi 等）重复上面的步骤\n"
                    + "5. Cookie 等同账号凭证：只存本机、不上传、不写日志、不做崩溃上报");
            card.addView(tutorial, top(10));
        }
        return card;
    }

    private String currentName() {
        if (AppState.I.userName != null && !AppState.I.userName.isEmpty()) return AppState.I.userName;
        return Prefs.cookie(getContext()).isEmpty() ? "未登录" : "已导入 Cookie";
    }

    private void verifyCookie() {
        String ck = cookieField.getText().toString().trim();
        if (ck.isEmpty()) {
            act.toast("先粘贴 Cookie 再验证");
            return;
        }
        act.toast("正在验证…");
        new Thread(() -> {
            try {
                Api.refresh(getContext());
                Api.User u = Api.verifyCookie(ck);
                Prefs.setCookie(getContext(), ck);
                List<Prefs.Account> list = Prefs.accounts(getContext());
                String id = String.valueOf(u.mid == 0 ? u.name.hashCode() : u.mid);
                boolean found = false;
                for (Prefs.Account a : list) {
                    if (a.id.equals(id)) {
                        a.name = u.name;
                        a.face = u.face;
                        a.cookie = ck;
                        found = true;
                    }
                }
                if (!found) {
                    Prefs.Account a = new Prefs.Account();
                    a.id = id;
                    a.name = u.name;
                    a.face = u.face;
                    a.cookie = ck;
                    list.add(a);
                }
                Prefs.saveAccounts(getContext(), list);
                Prefs.setCurrentAccountId(getContext(), id);
                AppState.I.userName = u.name;
                AppState.I.userFace = u.face;
                final String nm = u.name;
                act.runOnUiThread(() -> {
                    build();
                    loadAvatar();
                    act.toast("登录成功：" + nm);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? "Cookie 失效" : e.getMessage();
                act.runOnUiThread(() -> act.toast(msg));
            }
        }).start();
    }

    private void loadAvatar() {
        final String face = AppState.I.userFace;
        if (face == null || face.isEmpty()) return;
        final ImageView target = avatar;
        new Thread(() -> {
            byte[] b = Api.bytes(face);
            if (b == null) return;
            Bitmap bm = BitmapFactory.decodeByteArray(b, 0, b.length);
            if (bm == null) return;
            act.runOnUiThread(() -> {
                if (target != null) target.setImageBitmap(bm);
            });
        }).start();
    }

    private LinearLayout downloadCard() {
        LinearLayout card = Ui.card(getContext(), m);
        card.addView(Ui.label(getContext(), m, "下载位置"));
        String dir = Prefs.dirUri(getContext());
        String shown = dir.isEmpty() ? "系统相册目录 Movies/BiliSave（音频为 Music/BiliSave）" : dir;
        TextView t = Ui.text(getContext(), shown, 12.5f, Ui.alpha(m.onSurfaceVar, 235), false);
        t.setLineSpacing(dp(3), 1f);
        card.addView(t, top(10));
        LinearLayout actions = Ui.row(getContext());
        actions.setPadding(0, dp(12), 0, 0);
        TextView pick = Ui.softButton(getContext(), m, "选择文件夹");
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            act.startActivityForResult(i, REQ_DIR);
        });
        TextView reset = Ui.softButton(getContext(), m, "用默认目录");
        reset.setOnClickListener(v -> {
            Prefs.setDirUri(getContext(), "");
            build();
            act.toast("已改回系统相册目录");
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.rightMargin = dp(10);
        actions.addView(pick, p);
        actions.addView(reset);
        card.addView(actions);
        return card;
    }

    private LinearLayout appearanceCard() {
        LinearLayout card = Ui.card(getContext(), m);
        card.addView(Ui.label(getContext(), m, "外观"));

        LinearLayout dyn = Ui.row(getContext());
        dyn.setPadding(0, dp(12), 0, 0);
        TextView dl = Ui.text(getContext(), "动态取色（跟随壁纸）", 14f, m.onSurface, false);
        dyn.addView(dl, new LinearLayout.LayoutParams(0, -2, 1f));
        Toggle tg = new Toggle(getContext(), m, Prefs.dynamic(getContext()));
        tg.setOnChange(() -> {
            Prefs.setDynamic(getContext(), tg.isChecked());
            act.reloadTheme();
        });
        dyn.addView(tg, new LinearLayout.LayoutParams(dp(48), dp(28)));
        card.addView(dyn);

        TextView tl = Ui.text(getContext(), "深色模式", 14f, m.onSurface, false);
        card.addView(tl, top(14));
        LinearLayout modes = Ui.row(getContext());
        modes.setPadding(0, dp(8), 0, 0);
        String[] names = {"跟随系统", "浅色", "深色"};
        int curMode = Prefs.themeMode(getContext());
        for (int i = 0; i < 3; i++) {
            final int mode = i;
            ChipView chip = new ChipView(getContext(), m, names[i]);
            chip.setActive(curMode == i);
            chip.setOnClickListener(v -> {
                Prefs.setThemeMode(getContext(), mode);
                act.reloadTheme();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            modes.addView(chip, lp);
        }
        card.addView(modes);
        return card;
    }

    private LinearLayout networkCard() {
        LinearLayout card = Ui.card(getContext(), m);
        card.addView(Ui.label(getContext(), m, "网络"));
        TextView t = Ui.text(getContext(), "所有请求已固定带 Referer 与桌面 UA，被风控时可在这里覆写 UA。", 12f,
                Ui.alpha(m.onSurfaceVar, 220), false);
        t.setLineSpacing(dp(3), 1f);
        card.addView(t, top(8));
        uaField = new EditText(getContext());
        uaField.setHint("留空使用内置桌面 UA");
        uaField.setHintTextColor(Ui.alpha(m.onSurfaceVar, 150));
        uaField.setTextColor(m.onSurface);
        uaField.setTextSize(12f);
        uaField.setSingleLine(false);
        uaField.setMaxLines(3);
        uaField.setInputType(InputType.TYPE_CLASS_TEXT);
        uaField.setBackground(Ui.round(Ui.alpha(m.surfaceVar, m.dark ? 150 : 190), dp(16), dp(1), Ui.alpha(m.outline, 50)));
        uaField.setPadding(dp(14), dp(12), dp(14), dp(12));
        uaField.setText(Prefs.ua(getContext()));
        card.addView(uaField, top(10));
        TextView save = Ui.softButton(getContext(), m, "保存 UA");
        save.setOnClickListener(v -> {
            Prefs.setUa(getContext(), uaField.getText().toString().trim());
            Api.refresh(getContext());
            act.toast("UA 已保存");
        });
        card.addView(save, top(10));
        return card;
    }

    private LinearLayout aboutCard() {
        LinearLayout card = Ui.card(getContext(), m);
        card.addView(Ui.label(getContext(), m, "关于与合规"));
        TextView t = Ui.text(getContext(),
                "BiliSave 是个人离线观看工具。请尊重 UP 主版权与平台协议：不要二次分发下载内容，不要批量抓取，不要用于绕过付费墙。\n\n"
                        + "App 不收集账号密码，不接入任何统计或崩溃上报；Cookie 只保存在本机，也不会写进日志。登录态由你自己提供，账号安全由你自行负责。\n\n"
                        + "解析走官方 web 接口（x/web-interface/view 与 x/player/wbi/playurl），需要一个有效登录态才能看到更高清晰度。",
                12f, Ui.alpha(m.onSurfaceVar, 225), false);
        t.setLineSpacing(dp(4), 1f);
        card.addView(t, top(10));
        TextView ver = Ui.text(getContext(), "版本 1.0 · 单文件离线构建 · Material You", 11f, Ui.alpha(m.onSurfaceVar, 170), false);
        card.addView(ver, top(10));
        return card;
    }

    public void refresh() {
        build();
    }

    public void refreshUser() {
        build();
        loadAvatar();
    }

    public void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_DIR && res == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContext().getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            Prefs.setDirUri(getContext(), uri.toString());
            build();
            act.toast("下载目录已更新");
        }
    }
}

