package com.xuanyin.app.util;
import java.util.Calendar;
import java.util.Random;
public class WarmGreeting {
    public static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 6) return "凌晨好呀 🌙";
        if (h < 12) return "早上好呀 ☀️";
        if (h < 14) return "中午好呀 🍜";
        if (h < 18) return "下午好呀 ☕";
        if (h < 22) return "晚上好呀 🌆";
        return "夜深了 🌌";
    }
    public static String subGreeting() {
        String[] a = {"今天也要开心呀","愿今天被温柔以待","音乐陪你度过","慢慢来，一切都好","记得照顾好自己"};
        return a[new Random().nextInt(a.length)];
    }
    public static String quote() {
        String[] a = {"♡ 每一天都是新的开始","♡ 保持热爱，奔赴山海","♡ 愿你被世界温柔以待","♡ 慢慢走，别着急","♡ 生活明朗，万物可爱","♡ 做自己的太阳"};
        return a[new Random().nextInt(a.length)];
    }
}
