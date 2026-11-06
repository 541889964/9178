package com.superrouter.dytool;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

/**
 * 轻量前台服务：让进程获得前台优先级，避免老机型/深度定制 ROM 在后台把任务杀掉。
 * 不做任何实际工作，只维持存活，功耗几乎为零。
 */
public class KeepAliveService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(App.NOTIF_ID, buildNotification());
        // 被系统杀掉后自动重建
        return START_STICKY;
    }

    private Notification buildNotification() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, flag);

        return new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("抖音工具箱运行中")
                .setContentText("后台下载不会中断")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
