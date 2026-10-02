package com.xuanyin.app.service;
import android.app.*;
import android.content.*;
import android.os.*;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import com.xuanyin.app.R;
import com.xuanyin.app.model.Song;
public class MusicService extends Service {
    public static final String ACTION_PLAY = "com.xuanyin.app.PLAY";
    public static final String ACTION_TOGGLE = "com.xuanyin.app.TOGGLE";
    public static final String ACTION_NEXT = "com.xuanyin.app.NEXT";
    public static final String ACTION_PREV = "com.xuanyin.app.PREV";
    private static final String CH = "music_ch";
    private static final int NID = 1002;
    private static ExoPlayer player;
    private static Song currentSong;
    public static void playOnline(Context c, Song s) {
        Intent i = new Intent(c, MusicService.class);
        i.setAction(ACTION_PLAY); i.putExtra("song", s); c.startService(i);
    }
    public static Song getCurrent() { return currentSong; }
    public static ExoPlayer getPlayer() { return player; }
    @Override public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        ensureChannel();
        startForeground(NID, buildNotification());
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY: {
                    Song s = (Song) intent.getSerializableExtra("song");
                    if (s != null) {
                        currentSong = s;
                        String url = s.isOnline ? s.onlineUrl : s.path;
                        player.setMediaItem(MediaItem.fromUri(url));
                        player.prepare(); player.play();
                        updateNotification();
                        sendBroadcast(new Intent("com.xuanyin.app.SONG_CHANGED").putExtra("song", s));
                    }
                    break;
                }
                case ACTION_TOGGLE: player.setPlayWhenReady(!player.getPlayWhenReady()); break;
                case ACTION_NEXT: player.seekToNextMediaItem(); break;
                case ACTION_PREV: player.seekToPreviousMediaItem(); break;
            }
        }
        return START_STICKY;
    }
    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "播放控制", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
            }
        }
    }
    private Notification buildNotification() {
        String t = currentSong == null ? "玄音" : currentSong.title;
        String s = currentSong == null ? "未在播放" : currentSong.displayArtist();
        return new NotificationCompat.Builder(this, CH)
            .setContentTitle(t).setContentText(s).setSmallIcon(R.drawable.ic_launcher).setOngoing(true).build();
    }
    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NID, buildNotification());
    }
    @Override public void onDestroy() { if (player != null) player.release(); player = null; super.onDestroy(); }
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
