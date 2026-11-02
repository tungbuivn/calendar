package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

/** Thông tin người cúng điền một lần, dùng chung cho mọi bài khấn. */
public final class ProfileStore {

    private static final String PREFS = "prayer_profile";

    private ProfileStore() {
    }

    public static String get(Context context, String fieldId) {
        return prefs(context).getString(fieldId, "").trim();
    }

    /** Kéo tệp hồ sơ từ đĩa vào bộ nhớ trước khi màn hình cần tới. */
    public static void preload(Context context) {
        prefs(context).getString("_", "");
    }

    public static void set(Context context, String fieldId, String value) {
        prefs(context).edit().putString(fieldId, value == null ? "" : value.trim()).apply();
    }

    public static Map<String, String> all(Context context) {
        Map<String, String> result = new LinkedHashMap<>();
        for (PrayerCatalog.Field field : PrayerVault.shared(context).fields()) {
            result.put(field.id, get(context, field.id));
        }
        return result;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
