package com.bilisave.app.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class Snack {
    private static TextView current;

    public static void show(FrameLayout root, Monet m, String msg) {
        if (current != null) {
            root.removeView(current);
            current = null;
        }
        Context c = root.getContext();
        TextView t = Ui.text(c, msg, 14f, m.onPrimaryContainer, false);
        t.setBackground(Ui.round(m.primaryContainer, Ui.dp(c, 18)));
        t.setPadding(Ui.dp(c, 18), Ui.dp(c, 12), Ui.dp(c, 18), Ui.dp(c, 12));
        t.setMaxLines(3);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = Ui.dp(c, 96);
        lp.leftMargin = Ui.dp(c, 20);
        lp.rightMargin = Ui.dp(c, 20);
        root.addView(t, lp);
        current = t;
        t.setAlpha(0f);
        t.animate().alpha(1f).setDuration(180).start();
        t.postDelayed(() -> {
            if (t.getParent() != null) {
                t.animate().alpha(0f).setDuration(220).withEndAction(() -> root.removeView(t)).start();
            }
        }, 2600);
    }
}

