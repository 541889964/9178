package com.magnet.downloader.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.magnet.downloader.model.DownloadTask;
import com.magnet.downloader.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 真·下载引擎
 *
 * 关键设计：
 * 1. OkHttp + byteStream（不是 string()，二进制安全）
 * 2. 64KB buffer，边读边写
 * 3. 临时文件 .part，完成后 fsync + rename
 * 4. 完成后校验大小，不符则报错
 * 5. 支持并发多任务（每个任务独立线程）
 * 6. 支持取消（AtomicBoolean）
 * 7. 支持重定向（OkHttp 默认）
 * 8. 支持 Content-Disposition 文件名解析
 */
public final class DownloadEngine {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<Long, AtomicBoolean> CANCELS = new ConcurrentHashMap<>();

    private DownloadEngine() {}

    public static void cancel(long taskId) {
        AtomicBoolean f = CANCELS.get(taskId);
        if (f != null) f.set(true);
    }

    public static void start(final Context ctx, final DownloadTask task, final DownloadListener listener) {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        CANCELS.put(task.id, cancelled);

        Thread t = new Thread(() -> doDownload(ctx, task, listener, cancelled),
                "download-" + task.id);
        t.start();
    }

    private static void doDownload(Context ctx, DownloadTask task,
                                   DownloadListener listener,
                                   AtomicBoolean cancelled) {
        FileOutputStream fos = null;
        InputStream in = null;
        File tempFile = null;
        Response response = null;

        try {
            // 1. 目录
            File saveDir = FileUtils.getSaveDir(ctx);
            if (!saveDir.exists() && !saveDir.mkdirs()) {
                throw new IOException("无法创建目录: " + saveDir.getAbsolutePath());
            }

            // 2. 请求
            Request req = new Request.Builder()
                    .url(task.url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; MagnetDownloader)")
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .build();

            response = CLIENT.newCall(req).execute();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) throw new IOException("响应体为空");

            // 3. 文件名 + 大小
            long total = body.contentLength();
            String name = FileUtils.resolveFileName(task.url, response.headers());

            task.fileName = name;
            task.totalBytes = total;

            // 去重
            File target = new File(saveDir, name);
            int idx = 1;
            String base = name, ext = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0) { base = name.substring(0, dot); ext = name.substring(dot); }
            while (target.exists()) {
                target = new File(saveDir, base + "(" + (idx++) + ")" + ext);
            }
            task.savePath = target.getAbsolutePath();
            tempFile = new File(saveDir, target.getName() + ".part");

            final long ftotal = total;
            final String fname = target.getName();
            MAIN.post(() -> listener.onStart(task, fname, ftotal));

            // 4. 写
            in = body.byteStream();
            fos = new FileOutputStream(tempFile);
            byte[] buf = new byte[64 * 1024];
            long got = 0;
            long lastReport = 0, lastBytes = 0, lastTime = System.currentTimeMillis();

            int n;
            while ((n = in.read(buf)) != -1) {
                if (cancelled.get()) throw new IOException("已取消");
                fos.write(buf, 0, n);
                got += n;

                long now = System.currentTimeMillis();
                if (now - lastReport >= 400) {
                    long dt = now - lastTime;
                    long speed = dt > 0 ? (got - lastBytes) * 1000 / dt : 0;
                    lastReport = now; lastTime = now; lastBytes = got;
                    final long fgot = got, fspeed = speed;
                    final int pct = ftotal > 0 ? (int)(fgot * 100 / ftotal) : -1;
                    MAIN.post(() -> listener.onProgress(task, fgot, ftotal, pct, fspeed));
                }
            }

            // 5. 落盘
            fos.flush();
            try { fos.getFD().sync(); } catch (Throwable ignored) {}
            fos.close(); fos = null;
            in.close(); in = null;

            // 6. 校验大小
            if (total > 0 && tempFile.length() != total) {
                throw new IOException("大小不符: 实际 " + tempFile.length() + " 期望 " + total);
            }

            // 7. rename
            File finalFile = new File(task.savePath);
            if (finalFile.exists()) finalFile.delete();
            if (!tempFile.renameTo(finalFile)) {
                FileUtils.copyFile(tempFile, finalFile);
                tempFile.delete();
            }

            final File result = finalFile;
            MAIN.post(() -> listener.onDone(task, result));

        } catch (Throwable t) {
            try { if (fos != null) fos.close(); } catch (Throwable ignored) {}
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (tempFile != null) tempFile.delete(); } catch (Throwable ignored) {}
            try { if (response != null) response.close(); } catch (Throwable ignored) {}

            final String msg = t.getMessage() == null ? "未知错误" : t.getMessage();
            final boolean wasCancelled = cancelled.get();
            MAIN.post(() -> {
                if (wasCancelled) listener.onCancel(task);
                else listener.onError(task, msg);
            });
        } finally {
            CANCELS.remove(task.id);
        }
    }
}
