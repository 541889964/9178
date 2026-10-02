package com.xuanyin.app.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.xuanyin.app.R;
import com.xuanyin.app.model.Song;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {

    private static final String CH_ID = "download_ch";
    private static final int BASE_ID = 2000;
    private static int idCounter = 0;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build();
    private static final Handler main = new Handler(Looper.getMainLooper());

    public interface Listener {
        void onStart();
        void onProgress(int percent);
        void onDone(File file);
        void onError(String msg);
    }

    public static void download(Context ctx, Song song, Listener listener) {
        if (song == null) { if (listener != null) listener.onError("无效歌曲"); return; }
        String url = song.onlineUrl;
        if (url == null || url.isEmpty()) { if (listener != null) listener.onError("无下载地址"); return; }

        final int id = BASE_ID + (++idCounter);
        final NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        ensureChannel(ctx, nm);
        notifyProgress(ctx, nm, id, song.title, 0);
        if (listener != null) listener.onStart();

        new Thread(() -> {
            File out = null;
            try {
                Request req = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                        .header("Referer", "https://music.163.com/")
                        .build();
                Response resp = client.newCall(req).execute();
                if (!resp.isSuccessful() || resp.body() == null) {
                    fail(ctx, nm, id, listener, "HTTP " + resp.code());
                    return;
                }
                long total = resp.body().contentLength();
                InputStream in = resp.body().byteStream();

                File dir = getDownloadDir(ctx);
                if (!dir.exists() && !dir.mkdirs()) {
                    fail(ctx, nm, id, listener, "无法创建目录");
                    return;
                }
                String safe = sanitize(song.title);
                out = new File(dir, safe + ".mp3");
                int n2 = 1;
                while (out.exists()) { out = new File(dir, safe + " (" + (n2++) + ").mp3"); }

                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[8192];
                long got = 0;
                int n;
                int lastPct = -1;
                while ((n = in.read(buf)) > 0) {
                    fos.write(buf, 0, n);
                    got += n;
                    if (total > 0) {
                        int pct = (int)(got * 100 / total);
                        if (pct != lastPct) {
                            lastPct = pct;
                            final int fp = pct;
                            notifyProgress(ctx, nm, id, song.title, fp);
                            if (listener != null) main.post(() -> listener.onProgress(fp));
                        }
                    }
                }
                fos.flush(); fos.close(); in.close();

                Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan.setData(Uri.fromFile(out));
                ctx.sendBroadcast(scan);

                nm.cancel(id);
                final File fout = out;
                if (listener != null) main.post(() -> listener.onDone(fout));
                toast(ctx, "已保存: " + fout.getName());
            } catch (Throwable t) {
                if (out != null && out.exists()) out.delete();
                fail(ctx, nm, id, listener, t.getMessage() == null ? "下载失败" : t.getMessage());
            }
        }).start();
    }

    private static File getDownloadDir(Context ctx) {
        try {
            File music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (music != null) {
                File dir = new File(music, "玄音");
                if (!dir.exists()) dir.mkdirs();
                if (dir.canWrite()) return dir;
            }
        } catch (Throwable ignored) {}
        File ext = ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (ext != null) { File d = new File(ext, "玄音"); if (!d.exists()) d.mkdirs(); return d; }
        File in = new File(ctx.getFilesDir(), "Music");
        if (!in.exists()) in.mkdirs();
        return in;
    }

    private static String sanitize(String s) {
        if (s == null) return "未知歌曲";
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private static void ensureChannel(Context ctx, NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            if (nm.getNotificationChannel(CH_ID) == null) {
                NotificationChannel ch = new NotificationChannel(CH_ID, "下载", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private static void notifyProgress(Context ctx, NotificationManager nm, int id, String title, int pct) {
        if (nm == null) return;
        Notification n = new NotificationCompat.Builder(ctx, CH_ID)
                .setContentTitle("正在下载")
                .setContentText(title + "  " + pct + "%")
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(100, pct, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        try { nm.notify(id, n); } catch (Throwable ignored) {}
    }

    private static void fail(Context ctx, NotificationManager nm, int id, Listener l, String msg) {
        if (nm != null) nm.cancel(id);
        toast(ctx, "下载失败: " + msg);
        if (l != null) main.post(() -> l.onError(msg));
    }

    private static void toast(Context ctx, String msg) {
        main.post(() -> {
            try {
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {}
        });
    }
}
