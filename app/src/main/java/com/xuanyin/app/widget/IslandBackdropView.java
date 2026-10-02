package com.xuanyin.app.widget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
public class IslandBackdropView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF inner = new RectF();
    private float radius = 20f;
    private int accent = 0xFF9B6BFF;
    private float phase = 0f;
    private boolean breath = false;
    private int mode = 0;
    public IslandBackdropView(Context c) { super(c); init(); }
    public IslandBackdropView(Context c, AttributeSet a) { super(c, a); init(); }
    public IslandBackdropView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }
    private void init() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(0.8f));
        setClickable(false);
        setFocusable(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }
    public void setCornerRadius(float r) {
        if (Math.abs(radius - r) > 0.3f) { radius = r; invalidate(); }
    }
    public void setMode(int m) {
        mode = m;
        // iPhone 灵动岛：所有模式都是黑色，只有内部微光颜色不同
        if (m == 1) accent = 0xFFFF6B9D;
        else if (m == 2) accent = 0xFF30D158;
        else if (m == 3) accent = 0xFFFFCC00;
        else if (m == 4) accent = 0xFF5AC8FA;
        else accent = 0xFF9B6BFF;
        invalidate();
    }
    public void setChargingBreath(boolean b) {
        if (breath != b) { breath = b; invalidate(); }
    }
    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = Math.min(radius, h / 2f);
        rect.set(0, 0, w, h);

        // 1. 纯黑底色（iPhone 灵动岛基色）
        fill.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF000000, 0xFF0A0A0F, 0xFF05050A},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, fill);

        // 2. 顶部微妙高光（液态反光）
        fill.setShader(new LinearGradient(0, 0, 0, h * 0.5f,
                new int[]{0x22FFFFFF, 0x08FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        rect.set(0, 0, w, h * 0.5f);
        cv.drawRoundRect(rect, r, r, fill);
        rect.set(0, 0, w, h);

        // 3. 内部微光（模式色，非常淡）
        if (mode > 0) {
            phase += 0.015f;
            if (phase > 1f) phase -= 1f;
            int baseA = breath ? (int)(15 + 25 * (0.5f + 0.5f * Math.sin(phase * Math.PI * 2))) : 18;
            int col = (accent & 0x00FFFFFF) | (baseA << 24);
            fill.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.5f,
                    new int[]{col, 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            cv.drawRoundRect(rect, r, r, fill);
        }

        // 4. 边缘白色描边（iPhone 独有的玻璃感）
        stroke.setShader(null);
        stroke.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        stroke.setColor(0x2EFFFFFF);
        stroke.setStrokeWidth(dp(0.8f));
        cv.drawRoundRect(rect, r, r, stroke);

        // 5. 内部 1px 微光边
        inner.set(dp(0.8f), dp(0.8f), w - dp(0.8f), h - dp(0.8f));
        stroke.setColor(0x14FFFFFF);
        stroke.setStrokeWidth(dp(0.5f));
        cv.drawRoundRect(inner, r - dp(0.8f), r - dp(0.8f), stroke);

        if (breath) postInvalidateOnAnimation();
    }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}
