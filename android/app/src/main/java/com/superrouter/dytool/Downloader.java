package com.superrouter.dytool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 高速下载器
 * 核心优化：多直链并发探测，谁先给出有效响应就用谁，避免串行等待失效链接超时。
 */
public class Downloader {

    public interface Progress {
        void onProgress(int pct, long done, long total);
    }

    private static final OkHttpClient CLIENT = App.http().newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build();

    private static final ExecutorService PROBE = Executors.newCachedThreadPool();

    /** 并发探测：返回最先可用的链接（已确认 HTTP 200 且是媒体） */
    private static String pickFastest(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        if (urls.size() == 1) return urls.get(0);

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(urls.size(), 4));
        List<Future<String>> fs = new java.util.ArrayList<>();

        for (final String u : urls) {
            if (u == null || u.isEmpty()) continue;
            fs.add(pool.submit(() -> {
                try {
                    Request req = new Request.Builder()
                            .url(u)
                            .header("User-Agent", App.UA)
                            .header("Referer", "https://www.douyin.com/")
                            .header("Range", "bytes=0-2047")
                            .build();
                    try (Response r = CLIENT.newCall(req).execute()) {
                        if (!r.isSuccessful() && r.code() != 206) return null;
                        String ct = r.header("Content-Type", "");
                        if (ct != null && (ct.contains("text/") || ct.contains("json"))) return null;
                        ResponseBody b = r.body();
                        if (b == null) return null;
                        byte[] head = b.bytes();
                        if (head == null || head.length < 200) return null;
                        return u;
                    }
                } catch (Exception e) {
                    return null;
                }
            }));
        }

        String winner = null;
        try {
            for (Future<String> f : fs) {
                try {
                    String r = f.get(9, TimeUnit.SECONDS);
                    if (r != null) {
                        winner = r;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return winner != null ? winner : urls.get(0);
    }

    public static File download(List<String> urls, File dest,
                                Progress cb, boolean isImage) throws IOException {
        if (urls == null || urls.isEmpty()) throw new IOException("没有可用链接");

        String best = pickFastest(urls);
        if (best == null) throw new IOException("所有直链均不可用");

        try {
            return tryOne(best, dest, cb);
        } catch (Exception e) {
            // 失败则退回串行兜底
            String lastErr = e.getMessage() == null ? "下载失败" : e.getMessage();
            for (String u : urls) {
                if (u == null || u.equals(best)) continue;
                try {
                    return tryOne(u, dest, cb);
                } catch (Exception ex) {
                    lastErr = ex.getMessage() == null ? lastErr : ex.getMessage();
                }
            }
            throw new IOException(lastErr);
        }
    }

    private static File tryOne(String url, File dest, Progress cb) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", App.UA)
                .header("Referer", "https://www.douyin.com/")
                .header("Accept", "*/*")
                .build();

        Response resp = CLIENT.newCall(req).execute();
        try {
            if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
            ResponseBody body = resp.body();
            if (body == null) throw new IOException("响应为空");

            String ctype = resp.header("Content-Type", "");
            if (ctype != null && (ctype.contains("text/") || ctype.contains("json"))) {
                throw new IOException("CDN 返回非媒体内容");
            }

            long total = body.contentLength();
            InputStream in = body.byteStream();

            File tmp = new File(dest.getAbsolutePath() + ".part");
            FileOutputStream out = new FileOutputStream(tmp);
            byte[] buf = new byte[256 * 1024];
            long done = 0;
            int lastPct = -2;
            long lastNotify = 0;

            try {
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    done += n;
                    int pct = total > 0 ? (int) (done * 100 / total) : -1;
                    long now = System.currentTimeMillis();
                    if (pct != lastPct || now - lastNotify > 300) {
                        lastPct = pct;
                        lastNotify = now;
                        if (cb != null) cb.onProgress(pct, done, total);
                    }
                }
                out.flush();
            } finally {
                try { out.close(); } catch (Exception ignored) {}
                try { in.close(); } catch (Exception ignored) {}
            }

            if (done < 1024) {
                tmp.delete();
                throw new IOException("文件过小，可能被拦截");
            }

            if (dest.exists()) dest.delete();
            if (!tmp.renameTo(dest)) {
                java.io.FileInputStream fin = new java.io.FileInputStream(tmp);
                FileOutputStream fout = new FileOutputStream(dest);
                try {
                    byte[] b = new byte[128 * 1024];
                    int n;
                    while ((n = fin.read(b)) != -1) fout.write(b, 0, n);
                    fout.flush();
                } finally {
                    try { fin.close(); } catch (Exception ignored) {}
                    try { fout.close(); } catch (Exception ignored) {}
                }
                tmp.delete();
            }

            if (cb != null) cb.onProgress(100, done, done);
            return dest;
        } finally {
            try { resp.close(); } catch (Exception ignored) {}
        }
    }
}