package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class IslandProgressGlowView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint prog = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private long position = 0L, duration = 1L;
    private int sc = 0xFFFF6B9D, ec = 0xFF9B6BFF;
    private float phase = 0f;
    private boolean running = false;
    public IslandProgressGlowView(Context c) { super(c); init(); }
    public IslandProgressGlowView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandProgressGlowView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        track.setColor(0x33FFFFFF); prog.setColor(sc); glow.setColor(0xFFFFFFFF);
        setClickable(false); setFocusable(false);
    }
    public void setProgress(long p, long d) { position = Math.max(0, p); duration = Math.max(1, d); invalidate(); }
    public void setRunning(boolean r) { if (running != r) { running = r; if (r) postInvalidateOnAnimation(); else invalidate(); } }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = h / 2f;
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, r, r, track);
        float ratio = Math.min(1f, position / (float) duration);
        float pw = w * ratio;
        if (pw > 0.5f) {
            RectF pr = new RectF(0, 0, pw, h);
            prog.setShader(new LinearGradient(0, 0, w, 0, sc, ec, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(pr, r, r, prog);
            phase += 0.02f; if (phase > 1f) phase = 0f;
            float gx = pw * phase, gw = Math.max(pw * 0.12f, h * 1.6f);
            int alpha = (int)(90 * Math.sin(Math.PI * phase));
            if (alpha > 0) {
                glow.setAlpha(alpha);
                glow.setShader(new LinearGradient(gx - gw/2f, 0, gx + gw/2f, 0,
                    0x00FFFFFF, 0xFFFFFFFF, Shader.TileMode.CLAMP));
                RectF gr = new RectF(Math.max(0, gx - gw/2f), 0, Math.min(pw, gx + gw/2f), h);
                canvas.drawRoundRect(gr, r, r, glow);
            }
            if (running) postInvalidateOnAnimation();
        }
    }
}
