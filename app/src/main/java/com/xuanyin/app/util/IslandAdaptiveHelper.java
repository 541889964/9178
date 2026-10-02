package com.xuanyin.app.util;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowManager;
public class IslandAdaptiveHelper {
    public static int getCollapsedWidth(Context ctx, int mw) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int min = (int)(dm.widthPixels * 0.20f), max = (int)(dm.widthPixels * 0.95f);
        if (mw <= 0) return min;
        return Math.max(min, Math.min(max, mw));
    }
    public static int getCollapsedHeight(Context ctx, int dp) {
        return Math.max(dpToPx(ctx, 40), Math.min(dpToPx(ctx, 90), dpToPx(ctx, dp)));
    }
    public static int getExpandedMaxWidth(Context ctx) {
        return (int)(ctx.getResources().getDisplayMetrics().widthPixels * 0.96f);
    }
    public static int getExpandedMaxHeight(Context ctx) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int h = dm.heightPixels - getStatusBarHeight(ctx) - getNavBarHeight(ctx) - dpToPx(ctx, 24);
        return Math.max(h, dpToPx(ctx, 200));
    }
    public static float getCornerRadius(Context ctx, boolean expanded, int width) {
        float base = expanded ? 32f : 20f;
        float ratio = width / (float) Math.max(ctx.getResources().getDisplayMetrics().widthPixels, 1);
        return dpToPx(ctx, base + ratio * 4f);
    }
    public static int getSafeTop(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    WindowInsets ins = wm.getCurrentWindowMetrics().getWindowInsets();
                    int a = ins.getInsets(WindowInsets.Type.displayCutout()).top;
                    int b = ins.getInsets(WindowInsets.Type.statusBars()).top;
                    return Math.max(a, b);
                }
            } catch (Throwable ignored) {}
        }
        return getStatusBarHeight(ctx);
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
