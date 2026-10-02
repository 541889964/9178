package com.xuanyin.app.util;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import com.xuanyin.app.model.Song;
import java.io.File;
import java.util.*;
public class MusicScanner {
    private static final Set<String> EXT = new HashSet<>(Arrays.asList(
        "mp3","flac","wav","m4a","aac","ogg","ape","wma","opus","mp4","3gp"));
    private static final Set<String> SKIP_DIR = new HashSet<>(Arrays.asList(
        "Android","data","obb",".thumbnails","cache","temp"));

    public static List<Song> scan(Context ctx) {
        List<Song> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // 通道 1：MediaStore（快）
        try { queryMediaStore(ctx, list, seen); } catch (Throwable ignored) {}
        // 通道 2：递归文件系统
        for (File root : getRoots()) {
            try { recursive(root, list, seen, 0); } catch (Throwable ignored) {}
        }
        Collections.sort(list, (a,b) -> {
            String x = a.title == null ? "" : a.title;
            String y = b.title == null ? "" : b.title;
            return x.compareToIgnoreCase(y);
        });
        return list;
    }

    private static File[] getRoots() {
        List<File> roots = new ArrayList<>();
        roots.add(new File("/storage/emulated/0"));
        File sd = new File("/storage");
        File[] subs = sd.listFiles();
        if (subs != null) for (File f : subs) {
            String n = f.getName();
            if (f.isDirectory() && !n.equals("emulated") && !n.equals("self")) roots.add(f);
        }
        return roots.toArray(new File[0]);
    }

    private static void recursive(File dir, List<Song> out, Set<String> seen, int depth) {
        if (dir == null || depth > 12) return;
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) return;
        String name = dir.getName();
        if (SKIP_DIR.contains(name) || name.startsWith(".")) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                recursive(f, out, seen, depth + 1);
            } else {
                String fn = f.getName();
                int dot = fn.lastIndexOf('.');
                if (dot < 0) continue;
                String ext = fn.substring(dot+1).toLowerCase(Locale.ROOT);
                if (!EXT.contains(ext)) continue;
                String path = f.getAbsolutePath();
                if (seen.contains(path)) continue;
                seen.add(path);
                Song s = new Song();
                s.path = path;
                s.title = fn.substring(0, dot);
                s.duration = 0;
                try {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(path);
                    String t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    String a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    String al = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    String d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (t != null && !t.isEmpty()) s.title = t;
                    if (a != null) s.artist = a;
                    if (al != null) s.album = al;
                    if (d != null) try { s.duration = Long.parseLong(d); } catch (Throwable ignored) {}
                    mmr.release();
                } catch (Throwable ignored) {}
                s.id = path.hashCode() & 0x7FFFFFFF;
                out.add(s);
            }
        }
    }

    private static void queryMediaStore(Context ctx, List<Song> out, Set<String> seen) {
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null,
                MediaStore.Audio.Media.TITLE + " ASC");
        if (c == null) return;
        int iId = c.getColumnIndex(MediaStore.Audio.Media._ID);
        int iT = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int iA = c.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int iAl = c.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int iD = c.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int iP = c.getColumnIndex(MediaStore.Audio.Media.DATA);
        int iAB = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        while (c.moveToNext()) {
            String path = iP >= 0 ? c.getString(iP) : null;
            if (path != null && seen.contains(path)) continue;
            Song s = new Song();
            s.id = c.getLong(iId);
            s.title = c.getString(iT);
            s.artist = c.getString(iA);
            s.album = c.getString(iAl);
            s.duration = c.getLong(iD);
            s.path = path;
            s.albumId = c.getLong(iAB);
            if (s.title != null) {
                out.add(s);
                if (path != null) seen.add(path);
            }
        }
        c.close();
    }
}
