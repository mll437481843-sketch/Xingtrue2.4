package com.bilisave.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Soft watercolour mist background — the Monet layer. */
public class MistView extends View {
    private final Monet m;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;
    private ValueAnimator anim;

    public MistView(Context c, Monet m) {
        super(c);
        this.m = m;

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(42000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public void stop() {
        if (anim != null) anim.cancel();
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        int top = m.dark ? Ui.alpha(m.surfaceHi, 255) : Ui.alpha(m.surface, 255);
        paint.setShader(new LinearGradient(0, 0, w * 0.6f, h,
                new int[]{top, Ui.alpha(m.surface, 255), Ui.alpha(m.surfaceVar, 255)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        float t = phase * (float) (Math.PI * 2);
        blob(cv, w * (0.18f + 0.05f * (float) Math.sin(t)), h * (0.12f + 0.03f * (float) Math.cos(t)),
                w * 0.62f, m.primary, m.dark ? 66 : 46);
        blob(cv, w * (0.92f + 0.04f * (float) Math.cos(t + 1.2f)), h * (0.34f + 0.05f * (float) Math.sin(t + 0.8f)),
                w * 0.55f, m.secondary, m.dark ? 58 : 40);
        blob(cv, w * (0.30f + 0.06f * (float) Math.sin(t + 2.1f)), h * (0.78f + 0.04f * (float) Math.cos(t + 1.7f)),
                w * 0.70f, m.tertiary, m.dark ? 44 : 34);
        blob(cv, w * (0.80f + 0.05f * (float) Math.sin(t + 3.3f)), h * (0.95f + 0.03f * (float) Math.cos(t + 2.4f)),
                w * 0.50f, m.primary, m.dark ? 40 : 26);
    }

    private void blob(Canvas cv, float cx, float cy, float r, int color, int a) {
        paint.setShader(new RadialGradient(cx, cy, r,
                new int[]{Ui.alpha(color, a), Ui.alpha(color, a / 3), Ui.alpha(color, 0)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, paint);
        paint.setShader(null);
    }
}

