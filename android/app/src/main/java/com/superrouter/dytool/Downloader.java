package com.superrouter.dytool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** 带进度、多直链故障转移的文件下载器 */
public class Downloader {

    public interface Progress {
        /** @param pct 0~100，未知时为 -1 */
        void onProgress(int pct, long done, long total);
    }

    private static final OkHttpClient CLIENT = App.http();

    /**
     * 依次尝试 urls，第一个成功的即返回。
     *
     * @return 下载完成的文件
     */
    public static File download(List<String> urls, File dest,
                                Progress cb, boolean isImage) throws IOException {
        if (urls == null || urls.isEmpty()) throw new IOException("没有可用链接");

        String lastErr = "下载失败";
        for (String u : urls) {
            if (u == null || u.isEmpty()) continue;
            try {
                File f = tryOne(u, dest, cb, isImage);
                if (f != null) return f;
            } catch (Exception e) {
                lastErr = e.getMessage() == null ? e.toString() : e.getMessage();
            }
        }
        throw new IOException(lastErr);
    }

    private static File tryOne(String url, File dest, Progress cb,
                               boolean isImage) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", App.UA)
                .header("Referer", "https://www.douyin.com/")
                .header("Accept", "*/*")
                .build();

        Response resp = CLIENT.newCall(req).execute();
        try {
            if (!resp.isSuccessful()) {
                throw new IOException("HTTP " + resp.code());
            }
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
            byte[] buf = new byte[128 * 1024];
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
                    if (pct != lastPct || now - lastNotify > 400) {
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
                // rename 失败则拷贝
                copy(tmp, dest);
                tmp.delete();
            }

            if (cb != null) cb.onProgress(100, done, done);
            return dest;
        } finally {
            try { resp.close(); } catch (Exception ignored) {}
        }
    }

    private static void copy(File src, File dst) throws IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        try {
            byte[] b = new byte[64 * 1024];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            out.flush();
        } finally {
            try { in.close(); } catch (Exception ignored) {}
            try { out.close(); } catch (Exception ignored) {}
        }
    }
}
