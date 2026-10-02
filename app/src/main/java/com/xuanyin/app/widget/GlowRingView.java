package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
public class GlowRingView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = 0xFFFF6B9D;
    private float phase = 0f;
    public GlowRingView(Context c) { super(c); init(); }
    public GlowRingView(Context c, AttributeSet a) { super(c, a); init(); }
    public GlowRingView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        setClickable(false);
        setFocusable(false);
    }
    public void setColor(int c) { color = c; invalidate(); }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float cx = w / 2f, cy = h / 2f;
        float baseR = Math.min(cx, cy);
        phase += 0.012f;
        if (phase > 1f) phase = 0f;
        float pulse = 0.85f + 0.15f * (float)Math.sin(phase * Math.PI * 2);
        float radius = baseR * pulse;
        int a = (int)(90 + 60 * pulse);
        int cIn = (a << 24) | (color & 0x00FFFFFF);
        int cMid = (color & 0x00FFFFFF) | 0x22000000;
        p.setShader(new RadialGradient(cx, cy, radius,
            new int[]{cIn, cMid, 0}, new float[]{0.55f, 0.85f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, radius, p);
        postInvalidateOnAnimation();
    }
}
