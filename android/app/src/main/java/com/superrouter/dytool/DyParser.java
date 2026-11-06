package com.superrouter.dytool;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 抖音解析：走 dtk 数据 API。
 * demo 账号自动登录 + 会话保活 + 401 自动重登。
 */
public class DyParser {

    private static final String BASE = "https://api.douyin.wtf";
    private static final String DEMO_USER = "demo";
    private static final String DEMO_PASS = "kelp-kelp-delta-3690";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static volatile boolean loggedIn = false;

    private final OkHttpClient client;

    public DyParser() {
        client = App.http().newBuilder()
                .cookieJar(new SimpleCookieJar())
                .callTimeout(70, TimeUnit.SECONDS)
                .build();
    }

    /** 从任意文本中提取首个 http(s) 链接 */
    public static String extractUrl(String raw) {
        if (raw == null) return null;
        int i = raw.indexOf("http");
        while (i >= 0) {
            int end = i;
            while (end < raw.length()) {
                char c = raw.charAt(end);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '\u3000'
                        || c == '，' || c == '。' || c == '“' || c == '”') break;
                end++;
            }
            String u = raw.substring(i, end);
            if (u.startsWith("http://") || u.startsWith("https://")) return u;
            i = raw.indexOf("http", i + 4);
        }
        return null;
    }

    public static boolean isDouyin(String url) {
        if (url == null) return false;
        String h;
        try {
            h = HttpUrl.parse(url).host();
        } catch (Exception e) {
            return false;
        }
        if (h == null) return false;
        h = h.toLowerCase();
        return h.endsWith("douyin.com") || h.endsWith("iesdouyin.com");
    }

    // ---------------- 登录 ----------------
    private synchronized boolean login() {
        try {
            JSONObject body = new JSONObject();
            body.put("username", DEMO_USER);
            body.put("password", DEMO_PASS);
            Request req = new Request.Builder()
                    .url(BASE + "/api/v1/auth/login")
                    .header("User-Agent", App.UA)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
            try (Response r = client.newCall(req).execute()) {
                ResponseBody rb = r.body();
                String s = rb == null ? "" : rb.string();
                if (r.isSuccessful() && s.contains("\"success\":true")) {
                    loggedIn = true;
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        loggedIn = false;
        return false;
    }

    private synchronized boolean ensureLogin() {
        if (!loggedIn) return login();
        try {
            Request req = new Request.Builder()
                    .url(BASE + "/api/v1/auth/me")
                    .header("User-Agent", App.UA)
                    .get().build();
            try (Response r = client.newCall(req).execute()) {
                if (r.code() == 200) return true;
            }
        } catch (Exception ignored) {
        }
        return login();
    }

    // ---------------- 解析 ----------------
    public DyItem parse(String shareUrl) throws Exception {
        ensureLogin();

        JSONObject payload = new JSONObject();
        payload.put("url", shareUrl);
        RequestBody rb = RequestBody.create(payload.toString(), JSON);

        String respText = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpUrl url = HttpUrl.parse(BASE + "/api/v1/parse").newBuilder()
                    .addQueryParameter("wait", "30")
                    .build();
            Request req = new Request.Builder()
                    .url(url)
                    .header("User-Agent", App.UA)
                    .header("Accept", "application/json")
                    .post(rb)
                    .build();
            try (Response r = client.newCall(req).execute()) {
                ResponseBody body = r.body();
                respText = body == null ? "" : body.string();
                if (r.code() == 401 && attempt == 0) {
                    login();
                    continue;
                }
                if (r.code() == 400 && respText.contains("\"wait\"")) {
                    // wait 超上限，退化为不带 wait
                    continue;
                }
                break;
            }
        }

        if (respText == null || respText.isEmpty()) throw new IOException("解析服务无响应");

        JSONObject root = new JSONObject(respText);
        if (!root.optBoolean("success", false)) {
            JSONObject err = root.optJSONObject("error");
            String msg = err != null ? err.optString("message", "解析失败") : "解析失败";
            throw new IOException(msg);
        }

        JSONObject data = root.optJSONObject("data");
        if (data == null) throw new IOException("解析结果为空");
        return normalize(data);
    }

    private DyItem normalize(JSONObject d) throws Exception {
        DyItem it = new DyItem();
        it.awemeId = d.optString("content_id", d.optString("aweme_id", ""));
        it.webUrl = d.optString("web_url", "");

        String title = d.optString("title", "");
        if (title.isEmpty()) title = d.optString("description", "");
        it.title = title.replaceAll("\\s+", " ").trim();

        JSONArray tags = d.optJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.length() && i < 8; i++) {
                String t = tags.optString(i, "");
                if (!t.isEmpty()) it.tags.add(t);
            }
        }

        Object au = d.opt("author");
        if (au instanceof JSONObject) {
            JSONObject ao = (JSONObject) au;
            it.author = ao.optString("nickname", ao.optString("name", ""));
        } else if (au != null) {
            it.author = String.valueOf(au);
        }

        double durMs = d.optDouble("duration_ms", 0);
        it.durationSec = durMs > 0 ? durMs / 1000.0 : d.optDouble("duration", 0);

        // 抖音原声音频
        JSONObject music = d.optJSONObject("music");
        if (music != null) {
            it.musicUrl = music.optString("play_url", "");
            it.musicTitle = music.optString("title", "");
            it.musicAuthor = music.optString("author", "");
        }

        JSONObject media = d.optJSONObject("media");
        if (media == null) throw new IOException("没有媒体数据");

        // 图集作品
        JSONArray images = media.optJSONArray("images");
        if (images != null && images.length() > 0) {
            it.type = DyItem.TYPE_IMAGE;
            for (int i = 0; i < images.length(); i++) {
                JSONObject im = images.optJSONObject(i);
                if (im == null) continue;
                DyItem.Pic pic = new DyItem.Pic();
                pic.imageUrl = im.optString("url", "");
                if (pic.imageUrl.isEmpty()) {
                    JSONArray us = im.optJSONArray("urls");
                    if (us != null && us.length() > 0) pic.imageUrl = us.optString(0, "");
                }
                pic.videoUrl = im.optString("video_url", "");
                pic.width = im.optInt("width", 0);
                pic.height = im.optInt("height", 0);
                if (!pic.imageUrl.isEmpty()) it.pics.add(pic);
            }
            JSONArray covers2 = media.optJSONArray("covers");
            if (covers2 != null && covers2.length() > 0) {
                JSONObject c = covers2.optJSONObject(0);
                if (c != null) it.cover = c.optString("url", "");
            }
            if (it.pics.isEmpty()) throw new IOException("没有拿到图片");
            return it;
        }

        // 视频作品
        it.type = DyItem.TYPE_VIDEO;
        JSONObject video = media.optJSONObject("video");
        if (video != null) {
            it.width = video.optInt("width", 0);
            it.height = video.optInt("height", 0);
            it.sizeBytes = video.optLong("size_bytes", 0);
            it.watermark = video.optBoolean("watermark", false);

            JSONArray urls = video.optJSONArray("urls");
            if (urls != null) {
                for (int i = 0; i < urls.length(); i++) {
                    String u = urls.optString(i, "");
                    if (!u.isEmpty()) it.videoUrls.add(u);
                }
            }
            if (it.videoUrls.isEmpty()) {
                String u = video.optString("url", "");
                if (!u.isEmpty()) it.videoUrls.add(u);
            }
        }

        // streams 兜底
        if (it.videoUrls.isEmpty()) {
            JSONArray streams = media.optJSONArray("streams");
            if (streams != null) {
                for (int i = 0; i < streams.length(); i++) {
                    JSONObject st = streams.optJSONObject(i);
                    if (st == null) continue;
                    String u = st.optString("url", "");
                    if (!u.isEmpty()) it.videoUrls.add(u);
                }
            }
        }

        JSONArray covers = media.optJSONArray("covers");
        if (covers != null && covers.length() > 0) {
            JSONObject c = covers.optJSONObject(0);
            if (c != null) it.cover = c.optString("url", "");
        }
        if (it.cover.isEmpty() && video != null) it.cover = video.optString("cover", "");

        if (it.videoUrls.isEmpty()) throw new IOException("没有拿到视频直链");
        return it;
    }

    /** 极简 CookieJar：内存保存 dtk_session */
    private static class SimpleCookieJar implements okhttp3.CookieJar {
        private final Map<String, java.util.List<okhttp3.Cookie>> store = new LinkedHashMap<>();

        @Override
        public synchronized void saveFromResponse(HttpUrl url, java.util.List<okhttp3.Cookie> cookies) {
            store.put(url.host(), cookies);
        }

        @Override
        public synchronized java.util.List<okhttp3.Cookie> loadForRequest(HttpUrl url) {
            java.util.List<okhttp3.Cookie> list = store.get(url.host());
            return list != null ? list : new java.util.ArrayList<okhttp3.Cookie>();
        }
    }
}
