package com.xuanyin.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.Random;
public class ChargingEffectView extends View {
    public static class P { float x, y, r, a, speed; }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<P> parts = new ArrayList<>();
    private final Random rnd = new Random();
    private ValueAnimator anim;
    private boolean active = false;
    private int color = 0xFF30D158;
    public ChargingEffectView(Context c) { super(c); init(); }
    public ChargingEffectView(Context c, AttributeSet a) { super(c, a); init(); }
    public ChargingEffectView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        p.setStyle(Paint.Style.FILL);
        setClickable(false); setFocusable(false);
    }
    public void setColor(int c) { color = c; invalidate(); }
    public void setActive(boolean b) {
        if (active == b) return;
        active = b;
        if (b) {
            parts.clear();
            for (int i = 0; i < 24; i++) {
                P part = new P();
                part.x = rnd.nextFloat();
                part.y = rnd.nextFloat();
                part.r = 2f + rnd.nextFloat() * 3f;
                part.a = 0.4f + rnd.nextFloat() * 0.6f;
                part.speed = 0.3f + rnd.nextFloat() * 0.7f;
                parts.add(part);
            }
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1400);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(a -> {
                for (P part : parts) {
                    part.y -= 0.01f * part.speed;
                    if (part.y < -0.1f) { part.y = 1.1f; part.x = rnd.nextFloat(); }
                }
                invalidate();
            });
            anim.start();
        } else {
            if (anim != null) { anim.cancel(); anim = null; }
            invalidate();
        }
    }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (!active) return;
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        int baseAlpha = (int)(160 * (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 600.0)));
        for (P part : parts) {
            p.setColor(color);
            p.setAlpha(Math.min(255, (int)(baseAlpha * part.a)));
            cv.drawCircle(part.x * w, part.y * h, part.r, p);
        }
        postInvalidateOnAnimation();
    }
}
