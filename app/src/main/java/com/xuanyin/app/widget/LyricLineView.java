package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
public class LyricLineView extends View {
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text = "";
    private float progress = 0f;
    private boolean current = false;
    public LyricLineView(Context c) { super(c); init(); }
    public LyricLineView(Context c, AttributeSet a) { super(c, a); init(); }
    public LyricLineView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        float sp = getResources().getDisplayMetrics().scaledDensity;
        float size = 15f * sp;
        pBase.setColor(0xFFC9BFF5);
        pFill.setColor(0xFFFFFFFF);
        pBase.setTextSize(size);
        pFill.setTextSize(size);
        setClickable(false); setFocusable(false);
    }
    public void setText(String t) {
        if (t == null) t = "";
        if (t.equals(text)) return;
        text = t;
        invalidate();
    }
    public void setCurrent(boolean c) {
        if (current == c) return;
        current = c;
        if (current) {
            pFill.setShadowLayer(10f, 0, 0, 0xFFFF6B9D);
            setAlpha(1f);
        } else {
            pFill.setShadowLayer(0, 0, 0, 0);
            setAlpha(0.55f);
        }
        invalidate();
    }
    public void setProgress(float p) {
        if (Math.abs(progress - p) < 0.005f) return;
        progress = p;
        invalidate();
    }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (text.isEmpty()) return;
        float w = getWidth(), h = getHeight();
        float baseline = h / 2f - (pBase.descent() + pBase.ascent()) / 2f;
        cv.drawText(text, 0, baseline, pBase);
        if (!current) return;
        if (progress <= 0f) return;
        float tw = pBase.measureText(text);
        cv.save();
        cv.clipRect(0, 0, tw * Math.min(1f, progress), h);
        cv.drawText(text, 0, baseline, pFill);
        cv.restore();
    }
}
