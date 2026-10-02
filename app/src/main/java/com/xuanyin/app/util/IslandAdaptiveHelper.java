package com.xuanyin.app.util;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowManager;
public class IslandAdaptiveHelper {
    public static int getCollapsedWidth(Context c) {
        return (int)(c.getResources().getDisplayMetrics().widthPixels * Prefs.getFloat("collapsed_width_percent", 0.62f));
    }
    public static int getCollapsedHeight(Context c) { return dpToPx(c, Prefs.getInt("collapsed_height_dp", 44)); }
    public static int getExpandedWidth(Context c) {
        return (int)(c.getResources().getDisplayMetrics().widthPixels * Prefs.getFloat("expanded_width_percent", 0.94f));
    }
    public static int getExpandedHeight(Context c) {
        int dp = Prefs.getInt("expanded_height_dp", 260);
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int max = dm.heightPixels - getStatusBarHeight(c) - getNavBarHeight(c) - dpToPx(c, 40);
        return Math.min(dpToPx(c, dp), max);
    }
    public static float getCornerRadius(Context c, boolean exp, int w, int h) {
        return exp ? dpToPx(c, 44f) : h / 2f;
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
