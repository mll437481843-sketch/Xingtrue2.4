package com.bilisave.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

public class ChipView extends TextView {
    private final Monet m;
    private final int padH, padV;
    private boolean active;

    public ChipView(Context c, Monet m, String label) {
        super(c);
        this.m = m;
        padH = Ui.dp(c, 14);
        padV = Ui.dp(c, 7);
        setText(label);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setGravity(Gravity.CENTER);
        setPadding(padH, padV, padH, padV);
        refresh();
    }

    public void setActive(boolean a) {
        active = a;
        refresh();
    }

    public boolean isActive() {
        return active;
    }

    private void refresh() {
        setTextColor(active ? m.onPrimaryContainer : m.onSurfaceVar);
        setBackground(Ui.ripple(Ui.round(active ? m.primaryContainer : Ui.alpha(m.surfaceVar, m.dark ? 160 : 190),
                Ui.dp(getContext(), 18), Ui.dp(getContext(), 1),
                active ? Ui.alpha(m.primary, 120) : Ui.alpha(m.outline, 60)), Ui.alpha(m.primary, 60)));
    }
}

