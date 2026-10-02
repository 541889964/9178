package com.xuanyin.app.util;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
public class LyricsFetcher {
    public interface CB { void onResult(String lrc); }
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final Handler main = new Handler(Looper.getMainLooper());
    public static void fetch(long songId, CB cb) {
        try {
            String url = "https://music.163.com/api/song/lyric?id=" + songId + "&lv=1&kv=1&tv=-1";
            Request req = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://music.163.com/")
                .build();
            client.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {
                    main.post(() -> cb.onResult(""));
                }
                @Override public void onResponse(Call c, Response r) {
                    String lrc = "";
                    try {
                        String body = r.body() != null ? r.body().string() : "";
                        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                        if (root.has("lrc")) {
                            JsonObject obj = root.getAsJsonObject("lrc");
                            if (obj.has("lyric")) lrc = obj.get("lyric").getAsString();
                        }
                    } catch (Throwable ignored) {}
                    final String out = lrc;
                    main.post(() -> cb.onResult(out));
                }
            });
        } catch (Throwable t) { cb.onResult(""); }
    }
}
