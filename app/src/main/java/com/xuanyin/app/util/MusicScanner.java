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
    private static final Set<String> SKIP = new HashSet<>(Arrays.asList(
        "Android","data","obb",".thumbnails","cache","temp"));
    public static List<Song> scan(Context ctx) {
        List<Song> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try { query(ctx, list, seen); } catch (Throwable ignored) {}
        for (File r : roots()) try { walk(r, list, seen, 0); } catch (Throwable ignored) {}
        Collections.sort(list, (a,b) -> {
            String x = a.title == null ? "" : a.title;
            String y = b.title == null ? "" : b.title;
            return x.compareToIgnoreCase(y);
        });
        return list;
    }
    private static File[] roots() {
        List<File> r = new ArrayList<>();
        r.add(new File("/storage/emulated/0"));
        File[] s = new File("/storage").listFiles();
        if (s != null) for (File f : s) {
            String n = f.getName();
            if (f.isDirectory() && !n.equals("emulated") && !n.equals("self")) r.add(f);
        }
        return r.toArray(new File[0]);
    }
    private static void walk(File d, List<Song> out, Set<String> seen, int depth) {
        if (d == null || depth > 12 || !d.exists() || !d.isDirectory() || !d.canRead()) return;
        String n = d.getName();
        if (SKIP.contains(n) || n.startsWith(".")) return;
        File[] fs = d.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) walk(f, out, seen, depth + 1);
            else {
                String fn = f.getName();
                int dot = fn.lastIndexOf('.');
                if (dot < 0) continue;
                String ext = fn.substring(dot+1).toLowerCase(Locale.ROOT);
                if (!EXT.contains(ext)) continue;
                String p = f.getAbsolutePath();
                if (seen.contains(p)) continue;
                seen.add(p);
                Song s = new Song();
                s.path = p;
                s.title = fn.substring(0, dot);
                try {
                    MediaMetadataRetriever m = new MediaMetadataRetriever();
                    m.setDataSource(p);
                    String t = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    String a = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    String al = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    String du = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (t != null && !t.isEmpty()) s.title = t;
                    if (a != null) s.artist = a;
                    if (al != null) s.album = al;
                    if (du != null) try { s.duration = Long.parseLong(du); } catch (Throwable ignored) {}
                    m.release();
                } catch (Throwable ignored) {}
                s.id = p.hashCode() & 0x7FFFFFFF;
                out.add(s);
            }
        }
    }
    private static void query(Context ctx, List<Song> out, Set<String> seen) {
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
            String p = iP >= 0 ? c.getString(iP) : null;
            if (p != null && seen.contains(p)) continue;
            Song s = new Song();
            s.id = c.getLong(iId);
            s.title = c.getString(iT);
            s.artist = c.getString(iA);
            s.album = c.getString(iAl);
            s.duration = c.getLong(iD);
            s.path = p;
            s.albumId = c.getLong(iAB);
            if (s.title != null) { out.add(s); if (p != null) seen.add(p); }
        }
        c.close();
    }
}
