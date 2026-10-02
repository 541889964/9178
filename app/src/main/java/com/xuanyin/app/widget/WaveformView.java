package com.xuanyin.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.Random;
public class WaveformView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Random rnd = new Random();
    private final float[] hs = new float[5];
    private final float[] ts = new float[5];
    private ValueAnimator anim;
    private boolean active = false;
    private final int c1 = 0xFFFF6B9D, c2 = 0xFF9B6BFF;
    private final int bars = 5;
    private final float barW = 3f, gap = 3f;
    public WaveformView(Context c) { super(c); init(); }
    public WaveformView(Context c, AttributeSet a) { super(c, a); init(); }
    public WaveformView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        p.setStyle(Paint.Style.FILL);
        setClickable(false);
        setFocusable(false);
        for (int i = 0; i < bars; i++) { hs[i] = 0.35f; ts[i] = 0.35f; }
    }
    public void setActive(boolean b) {
        if (active == b) return;
        active = b;
        if (b) {
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(180);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(a -> {
                for (int i = 0; i < bars; i++) {
                    if (rnd.nextFloat() < 0.35f) ts[i] = 0.2f + rnd.nextFloat() * 0.8f;
                    hs[i] += (ts[i] - hs[i]) * 0.25f;
                }
                invalidate();
            });
            anim.start();
        } else {
            if (anim != null) { anim.cancel(); anim = null; }
            for (int i = 0; i < bars; i++) ts[i] = 0.2f;
        }
    }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float totalW = bars * barW + (bars - 1) * gap;
        float startX = (w - totalW) / 2f;
        float cy = h / 2f;
        if (p.getShader() == null) p.setShader(new LinearGradient(0, 0, w, 0, c1, c2, Shader.TileMode.CLAMP));
        for (int i = 0; i < bars; i++) {
            float bh = h * hs[i];
            if (bh < 3f) bh = 3f;
            float left = startX + i * (barW + gap);
            r.set(left, cy - bh / 2f, left + barW, cy + bh / 2f);
            cv.drawRoundRect(r, barW / 2f, barW / 2f, p);
        }
        if (active) postInvalidateOnAnimation();
    }
}
