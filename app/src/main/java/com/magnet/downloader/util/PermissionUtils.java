package com.magnet.downloader.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {
    public static final int REQ_STORAGE = 1001;
    public static final int REQ_NOTIFICATION = 1002;

    public static boolean hasStorage(Activity a) {
        if (Build.VERSION.SDK_INT >= 33) return true;
        int r = ContextCompat.checkSelfPermission(a, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return r == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestStorage(Activity a) {
        if (Build.VERSION.SDK_INT >= 33) return;
        ActivityCompat.requestPermissions(a,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                REQ_STORAGE);
    }

    public static void requestNotification(Activity a) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(a,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATION);
            }
        }
    }
}
