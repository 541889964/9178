package com.magnet.downloader.util;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
public class Haptic {
    private static Vibrator vb(Context c){if(c==null)return null;try{return(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);}catch(Throwable t){return null;}}
    private static void one(Context c,long ms,int amp){Vibrator v=vb(c);if(v==null||!v.hasVibrator())return;try{if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,amp));else v.vibrate(ms);}catch(Throwable ignored){}}
    public static void tap(View v){if(v!=null)try{v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}catch(Throwable ignored){}if(v!=null)one(v.getContext(),12,60);}
    public static void light(Context c){one(c,8,40);}
    public static void mid(Context c){one(c,20,90);}
    public static void longPress(View v){if(v!=null)try{v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);}catch(Throwable ignored){}if(v!=null)one(v.getContext(),34,110);}
    public static void done(Context c){one(c,30,120);}
}
