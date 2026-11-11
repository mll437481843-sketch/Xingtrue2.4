package com.bilisave.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class Toggle extends View {
    private final Monet m;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private boolean checked;
    private float pos;
    private Runnable onChange;

    public Toggle(Context c, Monet m, boolean checked) {
        super(c);
        this.m = m;
        this.checked = checked;
        this.pos = checked ? 1f : 0f;
        setOnClickListener(v -> set(!Toggle.this.checked, true));
    }

    public void setOnChange(Runnable r) {
        onChange = r;
    }

    public boolean isChecked() {
        return checked;
    }

    public void set(boolean v, boolean animate) {
        checked = v;
        if (animate) {
            ValueAnimator a = ValueAnimator.ofFloat(pos, v ? 1f : 0f);
            a.setDuration(160);
            a.addUpdateListener(x -> {
                pos = (float) x.getAnimatedValue();
                invalidate();
            });
            a.start();
        } else {
            pos = v ? 1f : 0f;
            invalidate();
        }
        if (onChange != null) onChange.run();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(Ui.dp(getContext(), 48), Ui.dp(getContext(), 28));
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight(), rad = h / 2f;
        paint.setColor(checked ? m.primary : Ui.alpha(m.outline, 120));
        r.set(0, 0, w, h);
        cv.drawRoundRect(r, rad, rad, paint);
        float kn = h - Ui.dp(getContext(), 6);
        float left = Ui.dp(getContext(), 3) + (w - kn - Ui.dp(getContext(), 6)) * pos;
        paint.setColor(checked ? m.onPrimary : Ui.alpha(m.surface, 255));
        cv.drawCircle(left + kn / 2f, h / 2f, kn / 2f, paint);
    }
}

