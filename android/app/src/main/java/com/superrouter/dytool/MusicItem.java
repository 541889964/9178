package com.superrouter.dytool;

/** 音乐条目 */
public class MusicItem {
    public String id = "";
    public String name = "";
    public String artist = "";
    public String album = "";
    public String cover = "";
    public long durationMs;
    public String source = "netease";

    public String subtitle() {
        StringBuilder sb = new StringBuilder();
        if (artist != null && !artist.isEmpty()) sb.append(artist);
        if (album != null && !album.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(album);
        }
        return sb.toString();
    }

    public String safeName() {
        String n = (name == null ? "" : name) + " - " + (artist == null ? "" : artist);
        n = n.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        n = n.replaceAll("_+", "_");
        if (n.length() > 70) n = n.substring(0, 70);
        n = n.replaceAll("^[_. ]+|[_. ]+$", "");
        if (n.isEmpty()) n = "music_" + id;
        return n;
    }
}
