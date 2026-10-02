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
    public static final String A_START = "com.xuanyin.app.DL_START";
    public static final String A_PROG = "com.xuanyin.app.DL_PROGRESS";
    public static final String A_DONE = "com.xuanyin.app.DL_DONE";
    public static final String A_ERR = "com.xuanyin.app.DL_ERROR";
    private static final String CH_ID = "download_ch";
    private static final int BASE_ID = 2000;
    private static int idCounter = 0;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS).build();
    private static final Handler main = new Handler(Looper.getMainLooper());
    public interface Listener { void onStart(); void onProgress(int p); void onDone(File f); void onError(String m); }
    public static void download(Context ctx, Song song, Listener l) {
        if (song == null || song.onlineUrl == null) { if (l != null) l.onError("无效"); return; }
        final int id = BASE_ID + (++idCounter);
        final NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        ensure(ctx, nm);
        notifyProg(ctx, nm, id, song.title, 0);
        bcast(ctx, A_START, song.title, 0);
        if (l != null) l.onStart();
        new Thread(() -> {
            File out = null;
            try {
                Request r = new Request.Builder().url(song.onlineUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                        .header("Referer", "https://music.163.com/").build();
                Response resp = client.newCall(r).execute();
                if (!resp.isSuccessful() || resp.body() == null) { fail(ctx, nm, id, l, "HTTP " + resp.code(), song.title); return; }
                long total = resp.body().contentLength();
                InputStream in = resp.body().byteStream();
                File dir = getDir(ctx);
                if (!dir.exists() && !dir.mkdirs()) { fail(ctx, nm, id, l, "无法创建目录", song.title); return; }
                String safe = sanitize(song.title);
                out = new File(dir, safe + ".mp3");
                int n2 = 1;
                while (out.exists()) { out = new File(dir, safe + " (" + (n2++) + ").mp3"); }
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buf = new byte[16384];
                long got = 0; int n, last = -1;
                while ((n = in.read(buf)) > 0) {
                    fos.write(buf, 0, n); got += n;
                    if (total > 0) {
                        int pct = (int)(got * 100 / total);
                        if (pct != last) {
                            last = pct;
                            notifyProg(ctx, nm, id, song.title, pct);
                            bcast(ctx, A_PROG, song.title, pct);
                            if (l != null) { final int fp = pct; main.post(() -> l.onProgress(fp)); }
                        }
                    }
                }
                fos.flush(); fos.close(); in.close();
                Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan.setData(Uri.fromFile(out)); ctx.sendBroadcast(scan);
                nm.cancel(id);
                bcast(ctx, A_DONE, song.title, 100);
                final File f = out;
                if (l != null) main.post(() -> l.onDone(f));
                toast(ctx, "已保存: " + f.getName());
            } catch (Throwable t) {
                if (out != null && out.exists()) out.delete();
                fail(ctx, nm, id, l, t.getMessage() == null ? "下载失败" : t.getMessage(), song.title);
            }
        }).start();
    }
    private static void bcast(Context c, String action, String title, int pct) {
        try { Intent i = new Intent(action); i.setPackage(c.getPackageName());
            i.putExtra("title", title); i.putExtra("percent", pct);
            c.sendBroadcast(i); } catch (Throwable ignored) {}
    }
    private static File getDir(Context ctx) {
        try {
            File m = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (m != null) { File d = new File(m, "玄音"); if (!d.exists()) d.mkdirs(); if (d.canWrite()) return d; }
        } catch (Throwable ignored) {}
        File e = ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (e != null) { File d = new File(e, "玄音"); if (!d.exists()) d.mkdirs(); return d; }
        File in = new File(ctx.getFilesDir(), "Music"); if (!in.exists()) in.mkdirs();
        return in;
    }
    private static String sanitize(String s) { return s == null ? "未知" : s.replaceAll("[\\\\/:*?\"<>|]", "_").trim(); }
    private static void ensure(Context ctx, NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            if (nm.getNotificationChannel(CH_ID) == null) {
                NotificationChannel ch = new NotificationChannel(CH_ID, "下载", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false); nm.createNotificationChannel(ch);
            }
        }
    }
    private static void notifyProg(Context ctx, NotificationManager nm, int id, String t, int p) {
        if (nm == null) return;
        Notification n = new NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("正在下载").setContentText(t + "  " + p + "%")
            .setSmallIcon(R.drawable.ic_launcher).setProgress(100, p, false)
            .setOngoing(true).setOnlyAlertOnce(true).build();
        try { nm.notify(id, n); } catch (Throwable ignored) {}
    }
    private static void fail(Context ctx, NotificationManager nm, int id, Listener l, String m, String title) {
        if (nm != null) nm.cancel(id);
        bcast(ctx, A_ERR, title, 0);
        toast(ctx, "下载失败: " + m);
        if (l != null) main.post(() -> l.onError(m));
    }
    private static void toast(Context ctx, String m) {
        main.post(() -> { try { android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {} });
    }
}
