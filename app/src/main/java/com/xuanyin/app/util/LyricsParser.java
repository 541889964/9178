package com.xuanyin.app.util;
import java.util.*;
import java.util.regex.*;
public class LyricsParser {
    public static class Line { public long time; public String text; public Line(long t, String s) { time = t; text = s; } }
    private static final Pattern P = Pattern.compile("\\[(\\d+):(\\d+)(?:\\.(\\d+))?\\](.*)");
    public static List<Line> parse(String lrc) {
        List<Line> list = new ArrayList<>();
        if (lrc == null) return list;
        for (String raw : lrc.split("\n")) {
            Matcher m = P.matcher(raw.trim());
            if (m.find()) {
                try {
                    long min = Long.parseLong(m.group(1));
                    long sec = Long.parseLong(m.group(2));
                    long ms = m.group(3) == null ? 0 : Long.parseLong((m.group(3) + "00").substring(0, 3));
                    long t = min * 60000 + sec * 1000 + ms;
                    String txt = m.group(4).trim();
                    if (!txt.isEmpty()) list.add(new Line(t, txt));
                } catch (Throwable ignored) {}
            }
        }
        Collections.sort(list, Comparator.comparingLong(o -> o.time));
        return list;
    }
    public static int findIndex(List<Line> lines, long pos) {
        if (lines == null || lines.isEmpty()) return -1;
        int lo = 0, hi = lines.size() - 1, res = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).time <= pos) { res = mid; lo = mid + 1; } else hi = mid - 1;
        }
        return res;
    }
}
