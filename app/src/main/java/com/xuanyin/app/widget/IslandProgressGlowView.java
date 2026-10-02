package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class IslandProgressGlowView extends View {
    private final Paint tk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gl = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private long pos = 0, dur = 1;
    private int sc = 0xFFFF6B9D, ec = 0xFF9B6BFF;
    private float phase = 0f;
    private boolean run = false;
    public IslandProgressGlowView(Context c) { super(c); init(); }
    public IslandProgressGlowView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandProgressGlowView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() { tk.setColor(0x33FFFFFF); pg.setColor(sc); gl.setColor(0xFFFFFFFF);
        setClickable(false); setFocusable(false); }
    public void setProgress(long p, long d) { pos = Math.max(0, p); dur = Math.max(1, d); invalidate(); }
    public void setRunning(boolean b) { if (run != b) { run = b; if (b) postInvalidateOnAnimation(); else invalidate(); } }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float rad = h / 2f;
        r.set(0, 0, w, h);
        cv.drawRoundRect(r, rad, rad, tk);
        float ratio = Math.min(1f, pos / (float) dur);
        float pw = w * ratio;
        if (pw > 0.5f) {
            RectF pr = new RectF(0, 0, pw, h);
            pg.setShader(new LinearGradient(0, 0, w, 0, sc, ec, Shader.TileMode.CLAMP));
            cv.drawRoundRect(pr, rad, rad, pg);
            phase += 0.02f; if (phase > 1f) phase = 0f;
            float gx = pw * phase, gw = Math.max(pw * 0.12f, h * 1.6f);
            int a = (int)(90 * Math.sin(Math.PI * phase));
            if (a > 0) {
                gl.setAlpha(a);
                gl.setShader(new LinearGradient(gx - gw/2f, 0, gx + gw/2f, 0, 0x00FFFFFF, 0xFFFFFFFF, Shader.TileMode.CLAMP));
                RectF gr = new RectF(Math.max(0, gx - gw/2f), 0, Math.min(pw, gx + gw/2f), h);
                cv.drawRoundRect(gr, rad, rad, gl);
            }
            if (run) postInvalidateOnAnimation();
        }
    }
}
