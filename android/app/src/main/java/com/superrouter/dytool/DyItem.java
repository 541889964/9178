package com.superrouter.dytool;

import java.util.ArrayList;
import java.util.List;

/** 抖音解析结果，同时支持视频作品与图集作品 */
public class DyItem {

    public static final int TYPE_VIDEO = 0;
    public static final int TYPE_IMAGE = 1;

    public int type = TYPE_VIDEO;

    public String awemeId = "";
    public String title = "";
    public String author = "";
    public String cover = "";
    public String webUrl = "";

    public List<String> tags = new ArrayList<>();
    public List<String> videoUrls = new ArrayList<>();

    /** 图集：每一项是一张图，动图项自带 videoUrl 可直接转视频 */
    public List<Pic> pics = new ArrayList<>();

    /** 抖音原声音频直链 */
    public String musicUrl = "";
    public String musicTitle = "";
    public String musicAuthor = "";

    public int width;
    public int height;
    public long sizeBytes;
    public double durationSec;
    public boolean watermark;

    public boolean isImagePost() {
        return type == TYPE_IMAGE;
    }

    public String safeName() {
        String n = title == null ? "" : title;
        n = sanitize(n);
        if (n.length() > 60) n = n.substring(0, 60);
        n = n.replaceAll("^[_. ]+|[_. ]+$", "");
        if (n.isEmpty()) n = "douyin_" + awemeId;
        return n;
    }

    public static String sanitize(String n) {
        StringBuilder bad = new StringBuilder();
        for (int c : new int[]{47, 92, 58, 42, 63, 34, 60, 62, 124, 13, 10, 9}) {
            bad.append((char) c);
        }
        n = n.replaceAll("[" + java.util.regex.Pattern.quote(bad.toString()) + "]", "_").trim();
        return n.replaceAll("__+", "_");
    }

    public static class Pic {
        public String imageUrl = "";
        /** 动图对应的视频地址，非空即为动图 */
        public String videoUrl = "";
        public int width;
        public int height;

        public boolean isAnimated() {
            return videoUrl != null && !videoUrl.isEmpty();
        }
    }
}
