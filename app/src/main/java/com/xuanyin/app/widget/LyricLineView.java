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
    private float baseSize;
    private float density;
    public LyricLineView(Context c) { super(c); init(); }
    public LyricLineView(Context c, AttributeSet a) { super(c, a); init(); }
    public LyricLineView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        density = getResources().getDisplayMetrics().scaledDensity;
        baseSize = 15f * density;
        pBase.setColor(0xFFC9BFF5);
        pFill.setColor(0xFFFFFFFF);
        pBase.setTextSize(baseSize);
        pFill.setTextSize(baseSize);
        setClickable(false);
        setFocusable(false);
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
        if (w <= 0 || h <= 0) return;
        float size = baseSize;
        pBase.setTextSize(size);
        float tw = pBase.measureText(text);
        float maxW = w - 4f;
        if (tw > maxW && tw > 0) {
            size = size * maxW / tw;
            float minS = 10f * density;
            if (size < minS) size = minS;
            pBase.setTextSize(size);
            pFill.setTextSize(size);
            tw = pBase.measureText(text);
        } else {
            pFill.setTextSize(size);
        }
        float baseline = h / 2f - (pBase.descent() + pBase.ascent()) / 2f;
        cv.drawText(text, 0, baseline, pBase);
        if (!current || progress <= 0f) return;
        cv.save();
        cv.clipRect(0, 0, tw * Math.min(1f, progress), h);
        cv.drawText(text, 0, baseline, pFill);
        cv.restore();
    }
}
