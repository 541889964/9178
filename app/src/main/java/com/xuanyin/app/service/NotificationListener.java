package com.xuanyin.app.service;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
public class NotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        Bundle ex = sbn.getNotification().extras;
        Intent i = new Intent("com.xuanyin.app.NOTIFY");
        i.setPackage(getPackageName());
        i.putExtra("title", ex.getString(Notification.EXTRA_TITLE));
        i.putExtra("text", ex.getString(Notification.EXTRA_TEXT));
        sendBroadcast(i);
    }
}
