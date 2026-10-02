package com.xuanyin.app.util;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import com.xuanyin.app.model.Song;
import java.util.ArrayList;
import java.util.List;
public class MusicScanner {
    public static List<Song> scan(Context ctx) {
        List<Song> list = new ArrayList<>();
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null,
                MediaStore.Audio.Media.TITLE + " ASC");
        if (c == null) return list;
        int iId = c.getColumnIndex(MediaStore.Audio.Media._ID);
        int iT = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int iA = c.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int iAl = c.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int iD = c.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int iP = c.getColumnIndex(MediaStore.Audio.Media.DATA);
        int iAB = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        while (c.moveToNext()) {
            Song s = new Song();
            s.id = c.getLong(iId);
            s.title = c.getString(iT);
            s.artist = c.getString(iA);
            s.album = c.getString(iAl);
            s.duration = c.getLong(iD);
            s.path = c.getString(iP);
            s.albumId = c.getLong(iAB);
            if (s.title != null) list.add(s);
        }
        c.close();
        return list;
    }
}
