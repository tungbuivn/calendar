package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cấu hình giọng đọc văn khấn: tốc độ, cao độ, giọng đọc nào.
 * Giá trị luôn được kẹp trong vùng TextToSpeech chấp nhận nên máy nào cũng chạy được.
 */
public final class SpeechSettings {

    public static final float MIN_RATE = 0.5f;
    public static final float MAX_RATE = 2f;
    public static final float MIN_PITCH = 0.5f;
    public static final float MAX_PITCH = 2f;
    public static final float DEFAULT_RATE = 1f;
    public static final float DEFAULT_PITCH = 1f;

    private static final String PREFS = "tts_settings";
    private static final String KEY_RATE = "rate";
    private static final String KEY_PITCH = "pitch";
    private static final String KEY_VOICE = "voice";
    private static final String KEY_VOICE_LOCALE = "voice_locale";

    private SpeechSettings() {
    }

    /** Tốc độ đọc; 1 là bình thường, máy đọc nhân vào tốc độ gốc của giọng. */
    public static float rate(Context context) {
        return clamp(prefs(context).getFloat(KEY_RATE, DEFAULT_RATE), MIN_RATE, MAX_RATE);
    }

    public static void setRate(Context context, float value) {
        prefs(context).edit().putFloat(KEY_RATE, clamp(value, MIN_RATE, MAX_RATE)).apply();
    }

    public static float pitch(Context context) {
        return clamp(prefs(context).getFloat(KEY_PITCH, DEFAULT_PITCH), MIN_PITCH, MAX_PITCH);
    }

    public static void setPitch(Context context, float value) {
        prefs(context).edit().putFloat(KEY_PITCH, clamp(value, MIN_PITCH, MAX_PITCH)).apply();
    }

    /** Tên giọng đã chọn; để trống thì máy đọc tự lấy giọng theo ngôn ngữ. */
    public static String voice(Context context) {
        return prefs(context).getString(KEY_VOICE, "").trim();
    }

    public static String voiceLocale(Context context) {
        return prefs(context).getString(KEY_VOICE_LOCALE, "").trim();
    }

    public static void setVoice(Context context, String name, String locale) {
        prefs(context).edit()
                .putString(KEY_VOICE, name == null ? "" : name)
                .putString(KEY_VOICE_LOCALE, locale == null ? "" : locale)
                .apply();
    }

    public static void reset(Context context) {
        prefs(context).edit().clear().apply();
    }

    /** Ngoài vùng thì kéo về biên gần nhất, máy đọc không nhận giá trị vượt khoảng cho phép. */
    public static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) return min;
        return value < min ? min : (value > max ? max : value);
    }

    /** Thanh trượt 0..100 đổi thành tỉ lệ đọc. */
    public static float toScale(int progress, float min, float max) {
        return clamp(min + (max - min) * progress / 100f, min, max);
    }

    /** Chiều ngược lại để đặt lại thanh trượt theo giá trị đã lưu. */
    public static int toProgress(float value, float min, float max) {
        if (max <= min) return 0;
        return Math.round((clamp(value, min, max) - min) / (max - min) * 100f);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
