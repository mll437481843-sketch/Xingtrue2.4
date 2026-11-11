package com.bilisave.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int alpha(int color, int a) {
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable round(int color, float radiusPx, int strokeW, int strokeColor) {
        GradientDrawable d = round(color, radiusPx);
        if (strokeW > 0) d.setStroke(strokeW, strokeColor);
        return d;
    }

    public static Drawable ripple(Drawable content, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.weight = weight;
        return p;
    }

    public static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout card(Context c, Monet m) {
        LinearLayout card = column(c);
        card.setBackground(round(alpha(m.surface, m.dark ? 190 : 216), dp(c, 26), dp(c, 1),
                alpha(m.outline, 70)));
        card.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16));
        return card;
    }

    public static void margin(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) p).setMargins(l, t, r, b);
            v.setLayoutParams(p);
        }
    }

    public static TextView primaryButton(Context c, Monet m, String label) {
        TextView t = text(c, label, 15f, m.onPrimary, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(c, 20), Ui.dp(c, 13), Ui.dp(c, 20), Ui.dp(c, 13));
        t.setBackground(ripple(round(m.primary, Ui.dp(c, 22)), alpha(m.onPrimary, 60)));
        return t;
    }

    public static TextView softButton(Context c, Monet m, String label) {
        TextView t = text(c, label, 14f, m.onSurfaceVar, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(c, 16), Ui.dp(c, 11), Ui.dp(c, 16), Ui.dp(c, 11));
        t.setBackground(ripple(round(alpha(m.surfaceVar, m.dark ? 150 : 170), Ui.dp(c, 20),
                Ui.dp(c, 1), alpha(m.outline, 70)), alpha(m.primary, 50)));
        return t;
    }

    public static TextView label(Context c, Monet m, String s) {
        TextView t = text(c, s, 12f, alpha(m.onSurfaceVar, 230), true);
        t.setLetterSpacing(0.04f);
        return t;
    }

}
