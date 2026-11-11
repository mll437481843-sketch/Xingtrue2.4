package com.bilisave.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bilisave.app.data.Prefs;
import com.bilisave.app.pages.HomePage;
import com.bilisave.app.pages.SettingsPage;
import com.bilisave.app.pages.TasksPage;
import com.bilisave.app.ui.IconView;
import com.bilisave.app.ui.MistView;
import com.bilisave.app.ui.Monet;
import com.bilisave.app.ui.Snack;
import com.bilisave.app.ui.Ui;

public class MainActivity extends Activity implements AppState.Listener {
    public Monet monet;
    private FrameLayout root;
    private FrameLayout host;
    private FrameLayout overlay;
    private MistView mist;
    private LinearLayout nav;
    private LinearLayout header;
    private TextView title, subtitle;
    private HomePage home;
    private TasksPage tasksPage;
    private SettingsPage settingsPage;
    private int page = 0;
    private String themeKey = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppState.I.load(this);
        cleanCache();
        applyWindow();
        buildUi();
        AppState.I.add(this);
        if (Build.VERSION.SDK_INT >= 33) {
            java.util.List<String> need = new java.util.ArrayList<>();
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                need.add("android.permission.POST_NOTIFICATIONS");
            if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 11);
        }
    }

    /** Drop cache fragments that no task can resume (left over after a hard kill). */
    private void cleanCache() {
        try {
            java.io.File dir = new java.io.File(getCacheDir(), "dl");
            java.io.File[] files = dir.listFiles();
            if (files == null) return;
            for (java.io.File f : files) {
                boolean owned = false;
                for (com.bilisave.app.data.Task t : AppState.I.tasks) {
                    if (f.getName().startsWith(t.id)) {
                        owned = true;
                        break;
                    }
                }
                if (!owned) f.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private void applyWindow() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        View d = getWindow().getDecorView();
        int f = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (monet != null && !monet.dark) f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        d.setSystemUiVisibility(f);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
    }

    private void buildUi() {
        monet = Monet.of(this, Prefs.dynamic(this), Prefs.themeMode(this));
        themeKey = Prefs.dynamic(this) + "/" + Prefs.themeMode(this) + "/" + monet.dark;
        if (Build.VERSION.SDK_INT >= 23) {
            View d = getWindow().getDecorView();
            int f = d.getSystemUiVisibility();
            if (monet.dark) f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            d.setSystemUiVisibility(f);
        }
        setContentView(layout_none());
    }

    private View layout_none() {
        root = new FrameLayout(this);
        root.setBackgroundColor(monet.surface);

        mist = new MistView(this, monet);
        root.addView(mist, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 22);
        header.setPadding(pad, Ui.dp(this, 18), pad, Ui.dp(this, 6));
        title = Ui.text(this, "BiliSave", 30f, monet.onSurface, true);
        subtitle = Ui.text(this, "个人离线收藏 · 只需粘贴视频地址", 12.5f, Ui.alpha(monet.onSurfaceVar, 220), false);
        header.addView(title);
        LinearLayout.LayoutParams sp = Ui.lp(-2, -2);
        sp.topMargin = Ui.dp(this, 4);
        header.addView(subtitle, sp);
        column.addView(header, new LinearLayout.LayoutParams(-1, -2));

        host = new FrameLayout(this);
        column.addView(host, new LinearLayout.LayoutParams(-1, 0, 1f));

        nav = buildNav();
        column.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        overlay = new FrameLayout(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            header.setPadding(Ui.dp(this, 22), Ui.dp(this, 18) + top, Ui.dp(this, 22), Ui.dp(this, 6));
            nav.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8) + bottom);
            return insets;
        });

        home = new HomePage(this);
        tasksPage = new TasksPage(this);
        settingsPage = new SettingsPage(this);
        showPage(page);
        return root;
    }

    private LinearLayout buildNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Ui.alpha(monet.surfaceHi, 245), Ui.alpha(monet.surface, 250)});
        bg.setCornerRadii(new float[]{Ui.dp(this, 26), Ui.dp(this, 26), Ui.dp(this, 26), Ui.dp(this, 26), 0, 0, 0, 0});
        bar.setBackground(bg);
        String[] labels = {"首页", "下载中", "设置"};
        for (int i = 0; i < 3; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            final int idx = i;
            item.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
            IconView icon = new IconView(this, i == 0 ? IconView.HOME : i == 1 ? IconView.DOWNLOAD : IconView.SETTINGS,
                    idx == page ? monet.primary : Ui.alpha(monet.onSurfaceVar, 190));
            item.addView(icon, Ui.lp(Ui.dp(this, 24), Ui.dp(this, 24)));
            TextView label = Ui.text(this, labels[i], 11f, idx == page ? monet.primary : Ui.alpha(monet.onSurfaceVar, 190), idx == page);
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.topMargin = Ui.dp(this, 3);
            item.addView(label, lp);
            item.setTag(new View[]{icon, label});
            item.setOnClickListener(v -> setPage(idx));
            bar.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return bar;
    }

    private void paintNav() {
        for (int i = 0; i < nav.getChildCount(); i++) {
            View item = nav.getChildAt(i);
            View[] pair = (View[]) item.getTag();
            boolean on = i == page;
            ((IconView) pair[0]).setColor(on ? monet.primary : Ui.alpha(monet.onSurfaceVar, 190));
            ((TextView) pair[1]).setTextColor(on ? monet.primary : Ui.alpha(monet.onSurfaceVar, 190));
            ((TextView) pair[1]).setTypeface(on ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        }
    }

    public void setPage(int idx) {
        page = idx;
        paintNav();
        host.removeAllViews();
        View p = idx == 0 ? home : idx == 1 ? tasksPage : settingsPage;
        p.setAlpha(0f);
        host.addView(p, new FrameLayout.LayoutParams(-1, -1));
        p.animate().alpha(1f).setDuration(180).start();
        if (idx == 1) tasksPage.refresh();
        if (idx == 2) settingsPage.refresh();
    }

    public void toast(String msg) {
        Snack.show(overlay, monet, msg);
    }

    public void reloadTheme() {
        AppState.I.save(this);
        if (mist != null) mist.stop();
        int keep = page;
        View content = layout_none();
        themeKey = Prefs.dynamic(this) + "/" + Prefs.themeMode(this) + "/" + monet.dark;
        page = keep;
        setContentView(content);
        showPage(keep);
    }

    private void showPage(int idx) {
        page = idx;
        paintNav();
        host.removeAllViews();
        View p = idx == 0 ? home : idx == 1 ? tasksPage : settingsPage;
        host.addView(p, new FrameLayout.LayoutParams(-1, -1));
    }

    public Monet theme() {
        return monet;
    }

    @Override
    protected void onResume() {
        super.onResume();
        String key = Prefs.dynamic(this) + "/" + Prefs.themeMode(this) + "/"
                + ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        if (!themeKey.equals(key)) reloadTheme();
    }

    @Override
    protected void onDestroy() {
        AppState.I.remove(this);
        if (mist != null) mist.stop();
        super.onDestroy();
    }

    @Override
    public void onTasks() {
        if (page == 1) tasksPage.refresh();
    }

    @Override
    public void onVideo() {
        if (page == 0) home.refresh();
    }

    @Override
    public void onMsg(String msg) {
        toast(msg);
    }

    @Override
    public void onUser() {
        if (page == 2) settingsPage.refreshUser();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (settingsPage != null) settingsPage.onActivityResult(req, res, data);
    }
}

