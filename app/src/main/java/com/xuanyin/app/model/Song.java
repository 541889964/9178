package com.xuanyin.app.model;
import java.io.Serializable;
public class Song implements Serializable {
    public long id, albumId, duration;
    public String title, artist, album, path, onlineUrl;
    public boolean isOnline;
    public String displayArtist() { return artist == null || artist.equals("<unknown>") ? "未知歌手" : artist; }
    public String displayAlbum() { return album == null || album.equals("<unknown>") ? "未知专辑" : album; }
    public String durationText() {
        if (duration <= 0) return "0:00";
        long s = duration / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
