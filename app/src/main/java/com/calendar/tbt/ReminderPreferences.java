package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;

/** Cấu hình nhắc trước ngày giỗ; mặc định 1 ngày. */
public final class ReminderPreferences {

    private static final String PREFS = "reminder_preferences";
    private static final String KEY_DAYS_BEFORE = "days_before";
    public static final int DEFAULT_DAYS = 1;
    public static final int MIN_DAYS = 0;
    public static final int MAX_DAYS = 30;

    private ReminderPreferences() {
    }

    public static int getDaysBefore(Context context) {
        int value = prefs(context).getInt(KEY_DAYS_BEFORE, DEFAULT_DAYS);
        return Math.max(MIN_DAYS, Math.min(MAX_DAYS, value));
    }

    public static void setDaysBefore(Context context, int days) {
        int value = Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        prefs(context).edit().putInt(KEY_DAYS_BEFORE, value).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}