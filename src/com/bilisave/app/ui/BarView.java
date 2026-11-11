package com.bilisave.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class BarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int trackColor, fillColor;
    private float progress;
    private int height;

    public BarView(Context c, Monet m) {
        super(c);
        trackColor = Ui.alpha(m.outline, 60);
        fillColor = m.primary;
        height = Ui.dp(c, 7);
    }

    public void setColors(int track, int fill) {
        trackColor = track;
        fillColor = fill;
        invalidate();
    }

    public void setProgress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        super.onMeasure(w, h);
        setMeasuredDimension(getMeasuredWidth(), height);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float rad = height / 2f;
        r.set(0, 0, getWidth(), height);
        paint.setColor(trackColor);
        cv.drawRoundRect(r, rad, rad, paint);
        if (progress > 0.001f) {
            r.set(0, 0, Math.max(height, getWidth() * progress), height);
            paint.setColor(fillColor);
            cv.drawRoundRect(r, rad, rad, paint);
        }
    }
}

