package com.superrouter.dytool;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 音乐 API：网易云公开接口
 *  - 搜索：/api/cloudsearch/pc
 *  - 详情（补封面）：/api/song/detail
 *  - 播放直链：/song/media/outer/url （302 跳 CDN，免登录、免版权费曲目可用）
 */
public class MusicApi {

    private static final String HOST = "https://music.163.com";
    private static final String REFERER = "https://music.163.com/";
    private static final OkHttpClient CLIENT = App.http().newBuilder()
            .callTimeout(25, TimeUnit.SECONDS)
            .build();

    /** 搜索歌曲 */
    public static List<MusicItem> search(String keyword, int limit) throws IOException {
        List<MusicItem> out = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return out;

        HttpUrl url = HttpUrl.parse(HOST + "/api/cloudsearch/pc").newBuilder()
                .addQueryParameter("s", keyword.trim())
                .addQueryParameter("type", "1")
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("offset", "0")
                .build();

        String body = get(url);
        try {
            JSONObject root = new JSONObject(body);
            JSONObject result = root.optJSONObject("result");
            if (result == null) return out;
            JSONArray songs = result.optJSONArray("songs");
            if (songs == null) return out;

            for (int i = 0; i < songs.length(); i++) {
                JSONObject s = songs.optJSONObject(i);
                if (s == null) continue;
                MusicItem m = new MusicItem();
                m.id = s.optString("id", "");
                m.name = s.optString("name", "");
                m.durationMs = s.optLong("dt", s.optLong("duration", 0));

                JSONArray ar = s.optJSONArray("ar");
                if (ar == null) ar = s.optJSONArray("artists");
                if (ar != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < ar.length(); j++) {
                        JSONObject a = ar.optJSONObject(j);
                        if (a == null) continue;
                        String nm = a.optString("name", "");
                        if (nm.isEmpty()) continue;
                        if (sb.length() > 0) sb.append(" / ");
                        sb.append(nm);
                    }
                    m.artist = sb.toString();
                }

                JSONObject al = s.optJSONObject("al");
                if (al == null) al = s.optJSONObject("album");
                if (al != null) {
                    m.album = al.optString("name", "");
                    m.cover = al.optString("picUrl", "");
                }
                if (m.cover.isEmpty()) m.cover = s.optString("picUrl", "");

                if (!m.id.isEmpty()) out.add(m);
            }

            // 补齐缺失封面
            fillCovers(out);
            return out;
        } catch (Exception e) {
            throw new IOException("搜索结果解析失败");
        }
    }

    /** 批量查详情补封面 */
    private static void fillCovers(List<MusicItem> items) {
        List<String> need = new ArrayList<>();
        for (MusicItem m : items) if (m.cover == null || m.cover.isEmpty()) need.add(m.id);
        if (need.isEmpty()) return;

        try {
            StringBuilder ids = new StringBuilder("[");
            for (int i = 0; i < need.size(); i++) {
                if (i > 0) ids.append(',');
                ids.append(need.get(i));
            }
            ids.append(']');

            HttpUrl url = HttpUrl.parse(HOST + "/api/song/detail").newBuilder()
                    .addQueryParameter("ids", ids.toString())
                    .build();
            String body = get(url);
            JSONObject root = new JSONObject(body);
            JSONArray songs = root.optJSONArray("songs");
            if (songs == null) return;

            for (int i = 0; i < songs.length(); i++) {
                JSONObject s = songs.optJSONObject(i);
                if (s == null) continue;
                String id = s.optString("id", "");
                JSONObject al = s.optJSONObject("album");
                String pic = al == null ? "" : al.optString("picUrl", "");
                if (pic.isEmpty()) continue;
                for (MusicItem m : items) {
                    if (m.id.equals(id) && (m.cover == null || m.cover.isEmpty())) {
                        m.cover = pic;
                        if (m.album.isEmpty() && al != null) m.album = al.optString("name", "");
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** 取播放/下载直链（跟随 302 得到 CDN 真实地址） */
    public static String resolveAudioUrl(String songId) throws IOException {
        HttpUrl url = HttpUrl.parse(HOST + "/song/media/outer/url")
                .newBuilder()
                .addQueryParameter("id", songId + ".mp3")
                .build();

        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", App.UA)
                .header("Referer", REFERER)
                .header("Accept", "*/*")
                .head()
                .build();

        try (Response r = CLIENT.newCall(req).execute()) {
            String loc = r.header("Location");
            if (loc != null && (loc.contains("music.126.net") || loc.contains(".mp3"))) {
                return loc;
            }
            String finalUrl = r.request().url().toString();
            if (finalUrl.contains("music.126.net") || finalUrl.contains(".mp3")) {
                return finalUrl;
            }
            return null;
        }
    }

    private static String get(HttpUrl url) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", App.UA)
                .header("Referer", REFERER)
                .header("Accept", "application/json")
                .get()
                .build();
        try (Response r = CLIENT.newCall(req).execute()) {
            ResponseBody b = r.body();
            if (!r.isSuccessful() || b == null) throw new IOException("HTTP " + r.code());
            return b.string();
        }
    }
}
