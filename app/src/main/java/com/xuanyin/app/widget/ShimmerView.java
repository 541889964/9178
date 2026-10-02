package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class ShimmerView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;
    public ShimmerView(Context c) { super(c); init(); }
    public ShimmerView(Context c, AttributeSet a) { super(c, a); init(); }
    public ShimmerView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() { setClickable(false); setFocusable(false); }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        phase += 0.008f;
        if (phase > 1.4f) phase = -0.2f;
        float bandW = w * 0.35f;
        float cx = (phase * (w + bandW * 2f)) - bandW;
        p.setShader(new LinearGradient(cx - bandW, 0, cx + bandW, 0,
            new int[]{0x00FFFFFF, 0x44FFFFFF, 0x00FFFFFF},
            new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        postInvalidateOnAnimation();
    }
}
