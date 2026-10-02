package com.xuanyin.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.Random;
public class ChargingEffectView extends View {
    public static class P { float x, y, r, a, speed, hue; }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<P> parts = new ArrayList<P>();
    private final Random rnd = new Random();
    private ValueAnimator anim;
    private boolean active = false;
    public ChargingEffectView(Context c) { super(c); init(); }
    public ChargingEffectView(Context c, AttributeSet a) { super(c, a); init(); }
    public ChargingEffectView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() { p.setStyle(Paint.Style.FILL); glowP.setStyle(Paint.Style.FILL); setClickable(false); setFocusable(false); setLayerType(LAYER_TYPE_SOFTWARE, null); }
    public void setActive(boolean b) {
        if (active == b) return;
        active = b;
        if (b) {
            parts.clear();
            for (int i = 0; i < 32; i++) { P part = new P(); part.x = rnd.nextFloat(); part.y = 0.4f + rnd.nextFloat() * 0.6f; part.r = 1.5f + rnd.nextFloat() * 3.5f; part.a = 0.35f + rnd.nextFloat() * 0.65f; part.speed = 0.4f + rnd.nextFloat() * 1.0f; part.hue = rnd.nextFloat(); parts.add(part); }
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1200); anim.setRepeatCount(ValueAnimator.INFINITE); anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { @Override public void onAnimationUpdate(ValueAnimator a) {
                for (int i = 0; i < parts.size(); i++) { P part = parts.get(i); part.y -= 0.012f * part.speed; part.x += (float)(Math.sin((part.y + part.hue) * 12f) * 0.002f); if (part.y < -0.08f) { part.y = 1.05f; part.x = rnd.nextFloat(); part.r = 1.5f + rnd.nextFloat() * 3.5f; } }
                invalidate();
            }});
            anim.start();
        } else { if (anim != null) { anim.cancel(); anim = null; } invalidate(); }
    }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (!active) return;
        float w = getWidth(), h = getHeight(); if (w <= 0 || h <= 0) return;
        long now = System.currentTimeMillis();
        float basePulse = 0.5f + 0.5f * (float)Math.sin(now / 400.0);
        int glowA = (int)(60 + 80 * basePulse);
        glowP.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.55f, new int[]{0x00FFFFFF, (0x30D158 & 0x00FFFFFF) | (glowA << 24), 0x00000000}, new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowP);
        for (int i = 0; i < parts.size(); i++) { P part = parts.get(i); int a = (int)(180 * part.a * (0.6f + 0.4f * basePulse)); int c = (part.hue < 0.6f) ? 0x30D158 : 0x5AC8FA; p.setColor((c & 0x00FFFFFF) | (a << 24)); p.setShadowLayer(6f, 0, 0, c); cv.drawCircle(part.x * w, part.y * h, part.r, p); }
        p.setShadowLayer(0, 0, 0, 0);
        float sweep = (now % 2400) / 2400f; float sx = sweep * (w + 200) - 100;
        glowP.setShader(new LinearGradient(sx - 40, 0, sx + 40, h, new int[]{0x0030D158, 0x8830D158, 0x0030D158}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowP);
        postInvalidateOnAnimation();
    }
}
