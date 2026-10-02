package com.xuanyin.app.util;
import android.os.*;
import com.google.gson.*;
import com.xuanyin.app.model.Song;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import okhttp3.*;
public class NeteaseApi {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler main = new Handler(Looper.getMainLooper());
    public interface CB { void onResult(List<Song> list); }
    public static void search(String keyword, CB cb) {
        try {
            String url = "https://music.163.com/api/search/get/web?type=1&offset=0&limit=30&s=" + URLEncoder.encode(keyword, "UTF-8");
            Request req = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();
            client.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) { main.post(() -> cb.onResult(new ArrayList<>())); }
                @Override public void onResponse(Call c, Response r) {
                    List<Song> result = new ArrayList<>();
                    try {
                        String body = r.body() != null ? r.body().string() : "";
                        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                        JsonArray songs = root.getAsJsonObject("result").getAsJsonArray("songs");
                        for (JsonElement el : songs) {
                            JsonObject o = el.getAsJsonObject();
                            Song s = new Song();
                            s.id = o.get("id").getAsLong();
                            s.title = o.get("name").getAsString();
                            if (o.has("artists")) {
                                JsonArray ar = o.getAsJsonArray("artists");
                                if (ar.size() > 0) s.artist = ar.get(0).getAsJsonObject().get("name").getAsString();
                            }
                            s.isOnline = true;
                            s.onlineUrl = "http://music.163.com/song/media/outer/url?id=" + s.id + ".mp3";
                            result.add(s);
                        }
                    } catch (Throwable ignored) {}
                    main.post(() -> cb.onResult(result));
                }
            });
        } catch (Throwable t) { cb.onResult(new ArrayList<>()); }
    }
}
