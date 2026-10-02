package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class IslandBackdropView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF inner = new RectF();
    private float radius = 22f;
    private int accent = 0xFF9B6BFF;
    private float phase = 0f;
    private boolean breath = false;
    public IslandBackdropView(Context c) { super(c); init(); }
    public IslandBackdropView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandBackdropView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1.2f));
        glow.setStyle(Paint.Style.FILL);
        setClickable(false); setFocusable(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }
    public void setCornerRadius(float r) { if (Math.abs(radius - r) > 0.5f) { radius = r; invalidate(); } }
    public void setMode(int m) {
        if (m == 1) accent = 0xFFFF6B9D;
        else if (m == 2) accent = 0xFF30D158;
        else if (m == 3) accent = 0xFFFFCC00;
        else if (m == 4) accent = 0xFF5AC8FA;
        else accent = 0xFF9B6BFF;
        invalidate();
    }
    public void setChargingBreath(boolean b) { if (breath != b) { breath = b; invalidate(); } }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = Math.min(radius, h / 2f);
        phase += 0.02f; if (phase > 1f) phase -= 1f;
        float pulse = breath ? (0.5f + 0.5f * (float)Math.sin(phase * Math.PI * 2)) : 0.5f;
        rect.set(0, 0, w, h);
        glow.setShadowLayer(dp(breath ? 18f : 10f), 0, dp(2f), (accent & 0x00FFFFFF) | (breath ? 0x66000000 : 0x33000000));
        glow.setColor(0x00000000);
        cv.drawRoundRect(rect, r, r, glow);
        fill.setShader(new LinearGradient(0, 0, w, h, new int[]{0xF0100E1E, 0xEE1A1830, 0xF00A0815}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, fill);
        fill.setShader(new LinearGradient(0, 0, 0, h * 0.55f, new int[]{0x66FFFFFF, 0x11FFFFFF, 0x00FFFFFF}, new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        rect.set(dp(1), dp(1), w - dp(1), h * 0.55f);
        cv.drawRoundRect(rect, r - dp(1), r - dp(1), fill);
        rect.set(0, 0, w, h);
        fill.setShader(new RadialGradient(w * 0.15f, h * 0.5f, Math.max(w, h) * 0.8f, new int[]{(accent & 0x00FFFFFF) | 0x33, 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, fill);
        if (breath) {
            int a = (int)(40 + 60 * pulse);
            fill.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.6f, new int[]{(accent & 0x00FFFFFF) | (a << 24), 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            cv.drawRoundRect(rect, r, r, fill);
        }
        stroke.setShader(null);
        stroke.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        stroke.setColor(0x55FFFFFF);
        stroke.setStrokeWidth(dp(1.2f));
        cv.drawRoundRect(rect, r, r, stroke);
        inner.set(dp(1.5f), dp(1.5f), w - dp(1.5f), h - dp(1.5f));
        stroke.setColor(0x1AFFFFFF);
        stroke.setStrokeWidth(dp(0.8f));
        cv.drawRoundRect(inner, r - dp(1.5f), r - dp(1.5f), stroke);
        if (breath) postInvalidateOnAnimation();
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
