package com.magnet.downloader.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import okhttp3.Headers;

public class FileUtils {

    public static File getSaveDir(Context ctx) {
        File pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (pub != null && (pub.exists() || pub.mkdirs())) return pub;
        File ext = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (ext != null && (ext.exists() || ext.mkdirs())) return ext;
        File in = new File(ctx.getFilesDir(), "downloads");
        if (!in.exists()) in.mkdirs();
        return in;
    }

    /**
     * 从 URL + 响应头里解析文件名
     */
    public static String resolveFileName(String url, Headers headers) {
        // 优先 Content-Disposition
        if (headers != null) {
            String cd = headers.get("Content-Disposition");
            if (cd != null) {
                // filename*=UTF-8''xxx  或  filename="xxx"
                String name = null;
                int idxStar = cd.indexOf("filename*=");
                if (idxStar >= 0) {
                    String part = cd.substring(idxStar + 10).trim();
                    int semi = part.indexOf(';');
                    if (semi > 0) part = part.substring(0, semi);
                    part = part.trim();
                    int q1 = part.indexOf('\''), q2 = part.indexOf('\'', q1 + 1);
                    if (q1 >= 0 && q2 > q1) {
                        try { name = URLDecoder.decode(part.substring(q2 + 1), "UTF-8"); }
                        catch (Throwable ignored) {}
                    }
                }
                if (name == null) {
                    int idx = cd.indexOf("filename=");
                    if (idx >= 0) {
                        String part = cd.substring(idx + 9).trim();
                        int semi = part.indexOf(';');
                        if (semi > 0) part = part.substring(0, semi);
                        part = part.trim();
                        if (part.startsWith("\"") && part.endsWith("\"") && part.length() > 1) {
                            part = part.substring(1, part.length() - 1);
                        }
                        try { name = URLDecoder.decode(part, "UTF-8"); }
                        catch (Throwable ignored) { name = part; }
                    }
                }
                if (name != null && !name.isEmpty()) {
                    name = sanitize(name);
                    if (!name.isEmpty()) return name;
                }
            }
        }

        // 从 URL 路径
        try {
            String path = new URL(url).getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.contains("?")) name = name.substring(0, name.indexOf('?'));
            if (!name.isEmpty()) {
                try { name = URLDecoder.decode(name, "UTF-8"); } catch (Throwable ignored) {}
                name = sanitize(name);
                if (!name.isEmpty()) return name;
            }
        } catch (Throwable ignored) {}

        // 兜底
        String ext = guessExt(url);
        return "download_" + System.currentTimeMillis() + ext;
    }

    private static String guessExt(String url) {
        try {
            String path = new URL(url).getPath().toLowerCase();
            int dot = path.lastIndexOf('.');
            if (dot > 0 && dot > path.length() - 6) return path.substring(dot);
        } catch (Throwable ignored) {}
        return "";
    }

    private static String sanitize(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        out.flush();
        try { out.getFD().sync(); } catch (Throwable ignored) {}
        out.close();
        in.close();
    }
}
