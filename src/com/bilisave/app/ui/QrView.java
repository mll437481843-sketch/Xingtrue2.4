package com.bilisave.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import io.nayuki.qrcodegen.QrCode;

/** Draws a QR symbol with a quiet zone — always dark-on-white so any scanner can read it. */
public class QrView extends View {
    private QrCode qr;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public QrView(Context c, String text) {
        super(c);
        try {
            qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
        } catch (Exception e) {
            qr = null;
        }
    }

    @Override
    protected void onMeasure(int w, int h) {
        int size = Math.min(MeasureSpec.getSize(w), MeasureSpec.getSize(h));
        if (size <= 0) size = Ui.dp(getContext(), 220);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (qr == null) return;
        int w = getWidth(), h = getHeight();
        int side = Math.min(w, h);
        int quiet = 2;
        float cell = side / (float) (qr.size + quiet * 2);
        float ox = (w - side) / 2f, oy = (h - side) / 2f;
        paint.setColor(0xFFFFFFFF);
        cv.drawRoundRect(ox, oy, ox + side, oy + side, cell * 2, cell * 2, paint);
        paint.setColor(0xFF141018);
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (!qr.getModule(x, y)) continue;
                float l = ox + (x + quiet) * cell;
                float t = oy + (y + quiet) * cell;
                cv.drawRect(l, t, l + cell + 0.6f, t + cell + 0.6f, paint);
            }
        }
    }
}

