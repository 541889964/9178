package com.xuanyin.app.util;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowManager;
public class IslandAdaptiveHelper {

    // 折叠宽度：屏幕百分比
    public static int getCollapsedWidth(Context c) {
        float p = Prefs.getFloat("collapsed_width_percent", 0.62f);
        return (int)(c.getResources().getDisplayMetrics().widthPixels * p);
    }
    // 折叠高度：dp
    public static int getCollapsedHeight(Context c) {
        int dp = Prefs.getInt("collapsed_height_dp", 44);
        return dpToPx(c, dp);
    }
    // 展开宽度：屏幕百分比
    public static int getExpandedWidth(Context c) {
        float p = Prefs.getFloat("expanded_width_percent", 0.94f);
        return (int)(c.getResources().getDisplayMetrics().widthPixels * p);
    }
    // 展开高度：dp
    public static int getExpandedHeight(Context c) {
        int dp = Prefs.getInt("expanded_height_dp", 260);
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int max = dm.heightPixels - getStatusBarHeight(c) - getNavBarHeight(c) - dpToPx(c, 40);
        return Math.min(dpToPx(c, dp), max);
    }
    // 圆角：折叠 = 高度/2（胶囊）；展开 = 44dp（iPhone 风格）
    public static float getCornerRadius(Context c, boolean expanded, int w, int h) {
        if (expanded) return dpToPx(c, 44f);
        return h / 2f;
    }
    public static int getSafeTop(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    WindowInsets ins = wm.getCurrentWindowMetrics().getWindowInsets();
                    int a = ins.getInsets(WindowInsets.Type.displayCutout()).top;
                    int b = ins.getInsets(WindowInsets.Type.statusBars()).top;
                    return Math.max(a, b);
                }
            } catch (Throwable ignored) {}
        }
        return getStatusBarHeight(c);
    }
    public static int getStatusBarHeight(Context c) {
        int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : dpToPx(c, 24);
    }
    public static int getNavBarHeight(Context c) {
        int id = c.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : dpToPx(c, 48);
    }
    public static int dpToPx(Context c, float dp) {
        return (int)(dp * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
