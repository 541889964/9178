package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class IslandPulseView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int accent = 0xFFFF6B9D;
    private boolean pulsing = false;
    private float phase = 0f;
    public IslandPulseView(Context c) { super(c); init(); }
    public IslandPulseView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandPulseView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3f)); p.setColor(accent);
        setClickable(false); setFocusable(false); }
    public void setAccentColor(int c) { accent = c; p.setColor(c); invalidate(); }
    public void setPulsing(boolean b) {
        if (pulsing != b) { pulsing = b; if (b) { phase = 0f; postInvalidateOnAnimation(); } else invalidate(); }
    }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (!pulsing) return;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float base = Math.min(cx, cy);
        if (base <= 0) return;
        phase += 0.03f; if (phase > 1f) phase = 0f;
        float rad = base * (0.65f + 0.5f * phase);
        int a = Math.max(0, (int)((1f - phase) * 140f));
        p.setColor(accent); p.setAlpha(a);
        cv.drawCircle(cx, cy, rad, p);
        postInvalidateOnAnimation();
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
