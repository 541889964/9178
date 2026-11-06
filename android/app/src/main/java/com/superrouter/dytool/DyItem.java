package com.superrouter.dytool;

import java.util.ArrayList;
import java.util.List;

/** 解析结果 */
public class DyItem {
    public String awemeId = "";
    public String title = "";
    public String author = "";
    public String cover = "";
    public String webUrl = "";
    public List<String> tags = new ArrayList<>();
    public List<String> videoUrls = new ArrayList<>();
    public int width;
    public int height;
    public long sizeBytes;
    public double durationSec;
    public boolean watermark;

    public String safeName() {
        String n = title == null ? "" : title;
        n = n.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        n = n.replaceAll("_+", "_");
        if (n.length() > 60) n = n.substring(0, 60);
        n = n.replaceAll("^[_. ]+|[_. ]+$", "");
        if (n.isEmpty()) n = "douyin_" + awemeId;
        return n;
    }
}
