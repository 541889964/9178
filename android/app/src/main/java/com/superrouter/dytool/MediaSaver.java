package com.superrouter.dytool;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * 保存到系统媒体库。
 * API 29+ 走 MediaStore（无需权限）；
 * API 27/28 直接写公共目录 + 通知媒体扫描（依赖 WRITE_EXTERNAL_STORAGE）。
 */
public class MediaSaver {

    public static String save(Context ctx, File src, int kind) throws Exception {
        // kind: 0=视频 1=音频 2=图片
        String name = src.getName();
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) ext = name.substring(dot + 1).toLowerCase();

        String mime;
        String dirName;
        if (kind == 0) {
            mime = "video/mp4";
            dirName = Environment.DIRECTORY_MOVIES;
        } else if (kind == 1) {
            mime = ext.equals("aac") ? "audio/aac" : "audio/mp4";
            dirName = Environment.DIRECTORY_MUSIC;
        } else {
            mime = ext.equals("png") ? "image/png" : "image/jpeg";
            dirName = Environment.DIRECTORY_PICTURES;
        }

        if (Build.VERSION.SDK_INT >= 29) {
            return saveModern(ctx, src, name, mime, dirName, kind);
        } else {
            return saveLegacy(ctx, src, name, mime, dirName);
        }
    }

    /** Android 10+ ：MediaStore 沙盒写入 */
    private static String saveModern(Context ctx, File src, String name, String mime,
                                     String dirName, int kind) throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, dirName + "/抖音工具箱");

        Uri collection;
        if (kind == 0) collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        else if (kind == 1) collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        else collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Uri item = cr.insert(collection, cv);
        if (item == null) throw new Exception("无法创建媒体条目");

        try (OutputStream out = cr.openOutputStream(item)) {
            if (out == null) throw new Exception("无法写入媒体库");
            copy(src, out);
        }
        return dirName + "/抖音工具箱/" + name;
    }

    /** Android 8.1 / 9 ：直接写公共目录并触发媒体扫描 */
    private static String saveLegacy(Context ctx, File src, String name, String mime,
                                     String dirName) throws Exception {
        File pub = Environment.getExternalStoragePublicDirectory(dirName);
        if (pub == null) throw new Exception("外部存储不可用");
        File dir = new File(pub, "抖音工具箱");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("无法创建目录");

        File dst = new File(dir, name);
        FileOutputStream out = new FileOutputStream(dst);
        try {
            copy(src, out);
            out.flush();
        } finally {
            try { out.close(); } catch (Exception ignored) {}
        }

        MediaScannerConnection.scanFile(ctx,
                new String[]{dst.getAbsolutePath()},
                new String[]{mime}, null);
        return dirName + "/抖音工具箱/" + name;
    }

    private static void copy(File src, OutputStream out) throws Exception {
        FileInputStream in = new FileInputStream(src);
        try {
            byte[] buf = new byte[128 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        } finally {
            try { in.close(); } catch (Exception ignored) {}
        }
    }
}
