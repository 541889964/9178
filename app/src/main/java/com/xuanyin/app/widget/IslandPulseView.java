package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class IslandPulseView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int accent = 0xFFFF6B9D;
    private boolean pulsing = false;
    private float phase = 0f;
    public IslandPulseView(Context c) { super(c); init(); }
    public IslandPulseView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandPulseView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3f));
        paint.setColor(accent);
        setClickable(false); setFocusable(false);
    }
    public void setAccentColor(int c) { accent = c; paint.setColor(c); invalidate(); }
    public void setPulsing(boolean p) {
        if (pulsing != p) {
            pulsing = p;
            if (p) { phase = 0f; postInvalidateOnAnimation(); } else invalidate();
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!pulsing) return;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float base = Math.min(cx, cy);
        if (base <= 0) return;
        phase += 0.035f; if (phase > 1f) phase = 0f;
        float radius = base * (0.70f + 0.45f * phase);
        int alpha = Math.max(0, (int)((1f - phase) * 120f));
        paint.setColor(accent); paint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, radius, paint);
        postInvalidateOnAnimation();
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
