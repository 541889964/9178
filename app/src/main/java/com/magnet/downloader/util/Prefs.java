package com.magnet.downloader.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static SharedPreferences sp;

    public static void init(Context c) {
        if (sp == null) sp = c.getApplicationContext()
                .getSharedPreferences("magnet_prefs", Context.MODE_PRIVATE);
    }

    public static boolean getBoolean(String k, boolean d) { return sp.getBoolean(k, d); }
    public static int getInt(String k, int d) { return sp.getInt(k, d); }
    public static String getString(String k, String d) { return sp.getString(k, d); }

    public static void put(String k, Object v) {
        SharedPreferences.Editor e = sp.edit();
        if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
        else if (v instanceof Integer) e.putInt(k, (Integer) v);
        else if (v instanceof String) e.putString(k, (String) v);
        e.apply();
    }
}
