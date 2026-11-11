package com.bilisave.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class IconView extends View {
    public static final int HOME = 0, DOWNLOAD = 1, SETTINGS = 2, SEARCH = 3, PLAY = 4,
            TRASH = 5, FOLDER = 6, CHECK = 7, AUDIO = 8, USER = 9, CLOSE = 10, LINK = 11;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int type;
    private int color;
    private final float sw;

    public IconView(Context c, int type, int color) {
        super(c);
        this.type = type;
        this.color = color;
        this.sw = Ui.dp(c, 1.8f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float s = Math.min(getWidth(), getHeight());
        float ox = (getWidth() - s) / 2f, oy = (getHeight() - s) / 2f;
        float u = s / 24f;
        cv.save();
        cv.translate(ox, oy);
        cv.scale(u, u);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.8f);
        path.reset();
        switch (type) {
            case HOME:
                path.moveTo(3.5f, 10.5f);
                path.lineTo(12f, 3.5f);
                path.lineTo(20.5f, 10.5f);
                path.moveTo(5.5f, 9.5f);
                path.lineTo(5.5f, 20f);
                path.lineTo(18.5f, 20f);
                path.lineTo(18.5f, 9.5f);
                cv.drawPath(path, p);
                break;
            case DOWNLOAD:
                path.moveTo(12f, 3.5f);
                path.lineTo(12f, 14.5f);
                path.moveTo(7.5f, 10.5f);
                path.lineTo(12f, 15f);
                path.lineTo(16.5f, 10.5f);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(4f, 17f);
                path.lineTo(4f, 20f);
                path.lineTo(20f, 20f);
                path.lineTo(20f, 17f);
                cv.drawPath(path, p);
                break;
            case SETTINGS:
                cv.drawCircle(12f, 12f, 3.2f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45 + 22.5);
                    float x1 = (float) (12 + Math.cos(a) * 6.2), y1 = (float) (12 + Math.sin(a) * 6.2);
                    float x2 = (float) (12 + Math.cos(a) * 8.4), y2 = (float) (12 + Math.sin(a) * 8.4);
                    cv.drawLine(x1, y1, x2, y2, p);
                }
                break;
            case SEARCH:
                cv.drawCircle(10.5f, 10.5f, 6.2f, p);
                cv.drawLine(15.2f, 15.2f, 20f, 20f, p);
                break;
            case PLAY:
                path.moveTo(8f, 5.5f);
                path.lineTo(18.5f, 12f);
                path.lineTo(8f, 18.5f);
                path.close();
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(path, p);
                break;
            case TRASH:
                path.moveTo(4.5f, 7f);
                path.lineTo(19.5f, 7f);
                path.moveTo(9.5f, 7f);
                path.lineTo(9.5f, 4.5f);
                path.lineTo(14.5f, 4.5f);
                path.lineTo(14.5f, 7f);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(6.5f, 7f);
                path.lineTo(7.6f, 20f);
                path.lineTo(16.4f, 20f);
                path.lineTo(17.5f, 7f);
                cv.drawPath(path, p);
                break;
            case FOLDER:
                path.moveTo(3.5f, 7f);
                path.lineTo(10f, 7f);
                path.lineTo(11.8f, 9.2f);
                path.lineTo(20.5f, 9.2f);
                path.lineTo(20.5f, 19f);
                path.lineTo(3.5f, 19f);
                path.close();
                cv.drawPath(path, p);
                break;
            case CHECK:
                path.moveTo(5f, 12.5f);
                path.lineTo(10f, 17.5f);
                path.lineTo(19.5f, 6.5f);
                cv.drawPath(path, p);
                break;
            case AUDIO:
                path.moveTo(4f, 10f);
                path.lineTo(8f, 10f);
                path.lineTo(13f, 5.5f);
                path.lineTo(13f, 18.5f);
                path.lineTo(8f, 14f);
                path.lineTo(4f, 14f);
                path.close();
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(16.5f, 9f);
                path.quadTo(19f, 12f, 16.5f, 15f);
                cv.drawPath(path, p);
                break;
            case USER:
                cv.drawCircle(12f, 8.6f, 3.8f, p);
                path.moveTo(4.8f, 20f);
                path.quadTo(12f, 13.4f, 19.2f, 20f);
                cv.drawPath(path, p);
                break;
            case CLOSE:
                cv.drawLine(6f, 6f, 18f, 18f, p);
                cv.drawLine(18f, 6f, 6f, 18f, p);
                break;
            case LINK:
                path.addRoundRect(new RectF(3.5f, 8.5f, 20.5f, 15.5f), 3.5f, 3.5f, Path.Direction.CW);
                cv.drawPath(path, p);
                cv.drawCircle(9f, 12f, 1.2f, p);
                cv.drawCircle(12.6f, 12f, 1.2f, p);
                break;
        }
        cv.restore();
    }
}

