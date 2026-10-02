package com.xuanyin.app;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import com.xuanyin.app.service.IslandService;
import com.xuanyin.app.util.Prefs;
public class MusicApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Prefs.init(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            try { startService(new Intent(this, IslandService.class)); } catch (Throwable ignored) {}
        }
    }
}
