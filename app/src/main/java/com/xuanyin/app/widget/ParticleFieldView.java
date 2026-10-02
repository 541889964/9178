package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.*;
public class ParticleFieldView extends View {
    public static class P { float x, y, r, vy, vx, a; }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<P> ps = new ArrayList<>();
    private final Random rnd = new Random();
    private long last = 0;
    public ParticleFieldView(Context c) { super(c); init(); }
    public ParticleFieldView(Context c, AttributeSet a) { super(c, a); init(); }
    public ParticleFieldView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        setClickable(false); setFocusable(false);
        for (int i = 0; i < 32; i++) ps.add(newP());
    }
    private P newP() {
        P part = new P();
        part.x = rnd.nextFloat(); part.y = rnd.nextFloat();
        part.r = 1.2f + rnd.nextFloat() * 2.6f;
        part.vy = -0.00025f - rnd.nextFloat() * 0.0006f;
        part.vx = (rnd.nextFloat() - 0.5f) * 0.0003f;
        part.a = 0.25f + rnd.nextFloat() * 0.5f;
        return part;
    }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        long now = System.currentTimeMillis();
        if (last == 0) last = now;
        float dt = Math.min(48f, now - last) / 16.6f;
        last = now;
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        for (P part : ps) {
            part.x += part.vx * dt;
            part.y += part.vy * dt;
            if (part.y < -0.05f) { part.y = 1.05f; part.x = rnd.nextFloat(); }
            if (part.x < -0.05f) part.x = 1.05f;
            if (part.x > 1.05f) part.x = -0.05f;
            p.setAlpha((int)(part.a * 255));
            cv.drawCircle(part.x * w, part.y * h, part.r, p);
        }
        postInvalidateOnAnimation();
    }
}
