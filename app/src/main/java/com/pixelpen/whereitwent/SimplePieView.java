package com.pixelpen.whereitwent;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SimplePieView extends View {
    private float[] values = new float[]{0f,0f,0f};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public SimplePieView(Context c) { super(c); }
    public SimplePieView(Context c, AttributeSet a) { super(c, a); }

    public void setValues(float fixed, float basic, float disc) {
        values = new float[]{Math.max(0,fixed), Math.max(0,basic), Math.max(0,disc)};
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float pad = Math.min(w, h) * 0.06f;
        oval.set(pad, pad, w - pad, h - pad);

        float sum = values[0] + values[1] + values[2];
        if (sum <= 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(4f, Math.min(w,h)*0.02f));
            paint.setColor(0xFFCCCCCC);
            canvas.drawOval(oval, paint);
            return;
        }

        float start = -90f;
        int[] colors = new int[]{0xFF024265, 0xFFFFDDC1, 0xFFBFCBD3};
        for (int i = 0; i < 3; i++) {
            float sweep = (values[i] / sum) * 360f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[i]);
            canvas.drawArc(oval, start, sweep, true, paint);
            start += sweep;
        }
    }
}
