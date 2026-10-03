package com.magnet.downloader.widget;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class AuroraBackgroundView extends View {
    private final Paint p1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint p3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;

    public AuroraBackgroundView(Context c) { super(c); init(); }
    public AuroraBackgroundView(Context c, AttributeSet a) { super(c, a); init(); }
    public AuroraBackgroundView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setClickable(false);
        setFocusable(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        phase += 0.0025f;
        if (phase > 1f) phase -= 1f;

        float r = Math.max(w, h) * 0.6f;

        float x1 = w * (0.25f + 0.15f * (float) Math.sin(phase * 6.28f));
        float y1 = h * (0.20f + 0.12f * (float) Math.cos(phase * 6.28f));
        p1.setShader(new RadialGradient(x1, y1, r,
                new int[]{0x55FF6B9D, 0}, null, Shader.TileMode.CLAMP));
        cv.drawCircle(x1, y1, r, p1);

        float x2 = w * (0.75f + 0.12f * (float) Math.cos(phase * 6.28f + 1.5f));
        float y2 = h * (0.55f + 0.15f * (float) Math.sin(phase * 6.28f + 1.5f));
        p2.setShader(new RadialGradient(x2, y2, r,
                new int[]{0x559B6BFF, 0}, null, Shader.TileMode.CLAMP));
        cv.drawCircle(x2, y2, r, p2);

        float x3 = w * (0.40f + 0.18f * (float) Math.sin(phase * 6.28f + 3f));
        float y3 = h * (0.85f + 0.12f * (float) Math.cos(phase * 6.28f + 3f));
        p3.setShader(new RadialGradient(x3, y3, r,
                new int[]{0x445AC8FA, 0}, null, Shader.TileMode.CLAMP));
        cv.drawCircle(x3, y3, r, p3);

        postInvalidateOnAnimation();
    }
}
