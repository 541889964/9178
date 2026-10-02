package com.xuanyin.app.service;
import android.app.*;
import android.content.*;
import android.os.*;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.xuanyin.app.R;
import com.xuanyin.app.model.Song;
import com.xuanyin.app.util.LyricsFetcher;
import java.util.ArrayList;
import java.util.List;
public class MusicService extends Service {
    public static final String ACTION_PLAY = "com.xuanyin.app.PLAY";
    public static final String ACTION_TOGGLE = "com.xuanyin.app.TOGGLE";
    public static final String ACTION_NEXT = "com.xuanyin.app.NEXT";
    public static final String ACTION_PREV = "com.xuanyin.app.PREV";
    public static final String ACTION_CYCLE_MODE = "com.xuanyin.app.CYCLE_MODE";
    public static final String A_SONG_CHANGED = "com.xuanyin.app.SONG_CHANGED";
    public static final String A_PLAY_STATE = "com.xuanyin.app.PLAY_STATE";
    public static final String A_LYRICS = "com.xuanyin.app.LYRICS";
    public static final String A_MODE_CHANGED = "com.xuanyin.app.MODE_CHANGED";
    private static final String CH = "music_ch";
    private static final int NID = 1002;
    private static ExoPlayer player;
    private static Song currentSong;
    private static List<Song> playlist = new ArrayList<>();
    private static int currentIndex = -1;
    private static int mode = 0; // 0=顺序 1=单曲 2=随机
    public static void playList(Context c, List<Song> list, int index) {
        if (list == null || list.isEmpty()) return;
        playlist = new ArrayList<>(list);
        currentIndex = Math.max(0, Math.min(index, playlist.size() - 1));
        playIndex(c, currentIndex);
    }
    public static void playOnline(Context c, Song s) {
        List<Song> one = new ArrayList<>(); one.add(s); playList(c, one, 0);
    }
    public static void next(Context c) {
        if (playlist.isEmpty()) return;
        if (mode == 1) { playIndex(c, currentIndex); return; }
        if (mode == 2) {
            int rnd = currentIndex;
            if (playlist.size() > 1) { while (rnd == currentIndex) rnd = (int)(Math.random() * playlist.size()); }
            currentIndex = rnd;
        } else {
            currentIndex = (currentIndex + 1) % playlist.size();
        }
        playIndex(c, currentIndex);
    }
    public static void prev(Context c) {
        if (playlist.isEmpty()) return;
        if (mode == 1) { playIndex(c, currentIndex); return; }
        if (mode == 2) { next(c); return; }
        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        playIndex(c, currentIndex);
    }
    public static void cycleMode(Context c) {
        mode = (mode + 1) % 3;
        sendMode(c);
    }
    private static void sendMode(Context c) {
        try {
            Intent i = new Intent(A_MODE_CHANGED);
            i.putExtra("mode", mode);
            i.setPackage(c.getPackageName());
            c.sendBroadcast(i);
        } catch (Throwable ignored) {}
    }
    private static void playIndex(Context c, int i) {
        if (i < 0 || i >= playlist.size()) return;
        Song s = playlist.get(i);
        currentSong = s;
        Intent intent = new Intent(c, MusicService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra("song", s);
        c.startService(intent);
    }
    public static Song getCurrent() { return currentSong; }
    public static ExoPlayer getPlayer() { return player; }
    public static int getMode() { return mode; }
    @Override public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean p) {
                sendBroadcast(new Intent(A_PLAY_STATE).putExtra("playing", p));
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) next(MusicService.this);
            }
        });
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
                        try { player.stop(); player.clearMediaItems(); } catch (Throwable ignored) {}
                        player.setMediaItem(MediaItem.fromUri(url));
                        player.prepare(); player.play();
                        updateNotification();
                        sendBroadcast(new Intent(A_SONG_CHANGED).putExtra("song", s));
                        if (s.isOnline) {
                            LyricsFetcher.fetch(s.id, lrc -> {
                                Intent li = new Intent(A_LYRICS);
                                li.putExtra("lrc", lrc);
                                sendBroadcast(li);
                            });
                        }
                    }
                    break;
                }
                case ACTION_TOGGLE: player.setPlayWhenReady(!player.getPlayWhenReady()); break;
                case ACTION_NEXT: next(this); break;
                case ACTION_PREV: prev(this); break;
                case ACTION_CYCLE_MODE: cycleMode(this); break;
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
