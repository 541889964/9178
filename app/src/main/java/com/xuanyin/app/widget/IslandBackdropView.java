package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class IslandBackdropView extends View {
    public static final int MODE_LIFE = 0, MODE_MUSIC = 1, MODE_CHARGING = 2, MODE_NOTIFICATION = 3;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float cornerRadius = 0f;
    private int accent = 0xFF5AC8FA;
    private boolean glow = true, charging = false;
    public IslandBackdropView(Context c) { super(c); init(); }
    public IslandBackdropView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandBackdropView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1f));
        stroke.setColor(0x33FFFFFF);
        setClickable(false); setFocusable(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }
    public void setCornerRadius(float r) { if (Math.abs(cornerRadius - r) > 0.5f) { cornerRadius = r; invalidate(); } }
    public void setMode(int m) {
        switch (m) {
            case MODE_MUSIC: accent = 0xFFFF6B9D; break;
            case MODE_CHARGING: accent = 0xFF30D158; break;
            case MODE_NOTIFICATION: accent = 0xFFFFCC00; break;
            default: accent = 0xFF5AC8FA;
        }
        invalidate();
    }
    public void setGlowEnabled(boolean b) { glow = b; setLayerType(b ? LAYER_TYPE_SOFTWARE : LAYER_TYPE_NONE, null); invalidate(); }
    public void setChargingBreath(boolean b) { if (charging != b) { charging = b; invalidate(); } }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = cornerRadius > 0 ? cornerRadius : h / 2f;
        rect.set(0, 0, w, h);
        fill.setShader(new LinearGradient(0, 0, w, h, 0xE6000000, 0xCC0D0B1F, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, r, r, fill);
        int c1 = (accent & 0x00FFFFFF) | 0x33000000, c2 = (accent & 0x00FFFFFF) | 0x0D000000;
        fill.setShader(new LinearGradient(0, 0, w, h, c1, c2, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, r, r, fill);
        if (glow) {
            float phase = charging ? (float)(0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 700.0)) : 0.5f;
            int base = charging ? 70 : 45;
            int alpha = Math.min((int)(base + 55 * phase), 200);
            int col = (alpha << 24) | (accent & 0x00FFFFFF);
            stroke.setShadowLayer(dp(10f), 0, 0, accent);
            stroke.setColor(col);
            if (charging) postInvalidateOnAnimation();
        } else {
            stroke.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
            stroke.setColor(0x33FFFFFF);
        }
        canvas.drawRoundRect(rect, r, r, stroke);
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
