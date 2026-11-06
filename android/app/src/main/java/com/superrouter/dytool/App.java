package com.superrouter.dytool;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class App extends Application {

    public static final String CHANNEL_ID = "dytool_bg";
    public static final int NOTIF_ID = 1001;

    public static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    private static App instance;
    private static OkHttpClient http;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createChannel();
    }

    public static App get() {
        return instance;
    }

    /** 全局复用的 OkHttp，连接池与线程池避免重复创建 */
    public static synchronized OkHttpClient http() {
        if (http == null) {
            http = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();
        }
        return http;
    }

    /** 输出目录：外部私有目录，免存储权限 */
    public static File outDir() {
        File d = new File(instance.getExternalFilesDir(null), "DyTool");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File cacheDirCustom() {
        File d = new File(instance.getCacheDir(), "dy");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "后台任务", NotificationManager.IMPORTANCE_MIN);
            ch.setDescription("保持下载与解析不被系统中断");
            ch.setShowBadge(false);
            ch.enableLights(false);
            ch.enableVibration(false);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
    }
}
