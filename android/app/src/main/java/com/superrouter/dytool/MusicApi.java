package com.superrouter.dytool;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 音乐 API（多源兜底）
 * 搜索：网易云 cloudsearch
 * 播放：网易云 outer/url + qijieya + injahow + kuwo 多源并发探测
 */
public class MusicApi {

    private static final String HOST = "https://music.163.com";
    private static final String REFERER = "https://music.163.com/";
    private static final OkHttpClient CLIENT = App.http().newBuilder()
            .callTimeout(20, TimeUnit.SECONDS)
            .build();

    public static List<MusicItem> search(String keyword, int limit) throws IOException {
        List<MusicItem> out = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return out;

        HttpUrl url = HttpUrl.parse(HOST + "/api/cloudsearch/pc").newBuilder()
                .addQueryParameter("s", keyword.trim())
                .addQueryParameter("type", "1")
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("offset", "0")
                .build();

        String body = get(url, REFERER);
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

                JSONObject priv = s.optJSONObject("privilege");
                if (priv != null) {
                    m.fee = priv.optInt("fee", 0);
                    m.playable = priv.optInt("st", 0) < 0 ? false : true;
                    int pl = priv.optInt("pl", 0);
                    m.maxBr = priv.optInt("maxbr", 0);
                }

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
                if (m.cover.isEmpty()) {
                    String picId = s.optString("picId", "");
                    if (!picId.isEmpty()) {
                        try {
                            long pid = Long.parseLong(picId);
                            m.cover = "https://p2.music.126.net/" + pid + ".jpg";
                        } catch (Exception ignored) {}
                    }
                }

                if (!m.id.isEmpty()) out.add(m);
            }

            // 免费可播的排前面
            out.sort((a, b) -> {
                if (a.playable != b.playable) return a.playable ? -1 : 1;
                return 0;
            });

            fillCovers(out);
            return out;
        } catch (Exception e) {
            throw new IOException("搜索结果解析失败");
        }
    }

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
            String body = get(url, REFERER);
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

    /** 多源返回候选播放直链（并发探测选最快可用） */
    public static List<String> resolveAudioUrls(String songId) throws IOException {
        List<String> urls = new ArrayList<>();

        // 源1：网易云官方外链（302 → CDN）
        try {
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
                if (loc != null && !loc.contains("/404")) {
                    urls.add(loc);
                }
            }
        } catch (Exception ignored) {
        }

        // 源2：qijieya meting（302 → 直链）
        String q = followHead("https://api.qijieya.cn/meting/?type=url&id=" + songId);
        if (q != null) urls.add(q);

        // 源3：injahow meting
        String i = followHead("https://api.injahow.cn/meting/?type=url&id=" + songId);
        if (i != null) urls.add(i);

        // 源4：网易云外链（直接作为下载地址，不解析 302）
        urls.add(HOST + "/song/media/outer/url?id=" + songId + ".mp3");

        if (urls.isEmpty()) throw new IOException("无可用音源");
        return urls;
    }

    private static String followHead(String api) {
        try {
            Request req = new Request.Builder()
                    .url(api)
                    .header("User-Agent", App.UA)
                    .header("Accept", "*/*")
                    .head()
                    .build();
            try (Response r = CLIENT.newCall(req).execute()) {
                String loc = r.header("Location");
                if (loc != null && loc.startsWith("http")) return loc;
                String finalUrl = r.request().url().toString();
                if (finalUrl.contains(".mp3") || finalUrl.contains("music.126.net")) return finalUrl;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String get(HttpUrl url, String referer) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", App.UA)
                .header("Referer", referer)
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