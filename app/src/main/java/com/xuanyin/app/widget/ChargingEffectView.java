package com.xuanyin.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.Random;
public class ChargingEffectView extends View {
    public static class P { float x, y, r, a, sp; }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<P> parts = new ArrayList<P>();
    private final Random rnd = new Random();
    private ValueAnimator anim;
    private boolean active = false;
    public ChargingEffectView(Context c) { super(c); init(); }
    public ChargingEffectView(Context c, AttributeSet a) { super(c, a); init(); }
    public ChargingEffectView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        p.setStyle(Paint.Style.FILL);
        glow.setStyle(Paint.Style.FILL);
        setClickable(false);
        setFocusable(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }
    public void setActive(boolean b) {
        if (active == b) return;
        active = b;
        if (b) {
            parts.clear();
            for (int i = 0; i < 20; i++) {
                P part = new P();
                part.x = rnd.nextFloat();
                part.y = 0.6f + rnd.nextFloat() * 0.4f;
                part.r = 1.2f + rnd.nextFloat() * 2.5f;
                part.a = 0.4f + rnd.nextFloat() * 0.6f;
                part.sp = 0.5f + rnd.nextFloat() * 0.8f;
                parts.add(part);
            }
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1500);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    for (int i = 0; i < parts.size(); i++) {
                        P part = parts.get(i);
                        part.y -= 0.008f * part.sp;
                        if (part.y < -0.05f) {
                            part.y = 1.05f;
                            part.x = rnd.nextFloat();
                            part.r = 1.2f + rnd.nextFloat() * 2.5f;
                        }
                    }
                    invalidate();
                }
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
        long now = System.currentTimeMillis();
        float pulse = 0.5f + 0.5f * (float)Math.sin(now / 500.0);
        int glowA = (int)(20 + 40 * pulse);
        glow.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.6f,
                new int[]{(0x30D158 & 0x00FFFFFF) | (glowA << 24), 0x00000000},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glow);
        for (int i = 0; i < parts.size(); i++) {
            P part = parts.get(i);
            int a = (int)(200 * part.a * (0.7f + 0.3f * pulse));
            p.setColor((0x30D158 & 0x00FFFFFF) | (a << 24));
            p.setShadowLayer(5f, 0, 0, 0x30D158);
            cv.drawCircle(part.x * w, part.y * h, part.r, p);
        }
        p.setShadowLayer(0, 0, 0, 0);
        float sweep = (now % 2000) / 2000f;
        float sx = sweep * (w + 100) - 50;
        glow.setShader(new LinearGradient(sx - 30, 0, sx + 30, 0,
                new int[]{0x0030D158, 0x6630D158, 0x0030D158},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glow);
        postInvalidateOnAnimation();
    }
}
