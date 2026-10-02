package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class IslandBackdropView extends View {
    public static final int LIFE=0, MUSIC=1, CHARGING=2, NOTIFICATION=3, DOWNLOAD=4;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float corner = 0f;
    private int accent = 0xFF5AC8FA;
    private boolean breath = false;
    public IslandBackdropView(Context c) { super(c); init(); }
    public IslandBackdropView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandBackdropView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1f)); stroke.setColor(0x33FFFFFF);
        setClickable(false); setFocusable(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }
    public void setCornerRadius(float v) { if (Math.abs(corner - v) > 0.5f) { corner = v; invalidate(); } }
    public void setMode(int m) {
        switch (m) {
            case MUSIC: accent = 0xFFFF6B9D; break;
            case CHARGING: accent = 0xFF30D158; break;
            case NOTIFICATION: accent = 0xFFFFCC00; break;
            case DOWNLOAD: accent = 0xFF5AC8FA; break;
            default: accent = 0xFF5AC8FA;
        }
        invalidate();
    }
    public void setChargingBreath(boolean b) { if (breath != b) { breath = b; invalidate(); } }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float rad = corner > 0 ? corner : h / 2f;
        r.set(0, 0, w, h);
        fill.setShader(new LinearGradient(0, 0, w, h, 0xF0000000, 0xD00D0B1F, Shader.TileMode.CLAMP));
        cv.drawRoundRect(r, rad, rad, fill);
        int c1 = (accent & 0x00FFFFFF) | 0x40000000;
        int c2 = (accent & 0x00FFFFFF) | 0x10000000;
        fill.setShader(new LinearGradient(0, 0, w, h, c1, c2, Shader.TileMode.CLAMP));
        cv.drawRoundRect(r, rad, rad, fill);
        float ph = breath ? (float)(0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 700.0)) : 0.5f;
        int base = breath ? 90 : 60;
        int a = Math.min((int)(base + 55 * ph), 220);
        int col = (a << 24) | (accent & 0x00FFFFFF);
        stroke.setShadowLayer(dp(12f), 0, 0, accent);
        stroke.setColor(col);
        if (breath) postInvalidateOnAnimation();
        cv.drawRoundRect(r, rad, rad, stroke);
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
