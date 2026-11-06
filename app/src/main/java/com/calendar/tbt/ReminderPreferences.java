package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cấu hình nhắc trước ngày giỗ / sự kiện.
 *
 * <p>Thông báo mặc định tắt. Người dùng phải bật trong cấu hình, khi đó ứng dụng mới
 * xin quyền gửi thông báo (POST_NOTIFICATIONS). Việc hỏi quyền đúng lúc người dùng
 * chủ động bật tính năng giúp tỉ lệ đồng ý cao hơn so với hỏi ngay khi mở app.
 */
public final class ReminderPreferences {

    private static final String PREFS = "reminder_preferences";
    private static final String KEY_DAYS_BEFORE = "days_before";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_PERMISSION_REQUESTED = "notification_permission_requested";
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

    /** Người dùng đã bật thông báo nhắc trước hay chưa. */
    public static boolean areNotificationsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    /**
     * Đã từng hiện hộp thoại xin quyền gửi thông báo hay chưa.
     *
     * <p>Cần cờ này vì {@code shouldShowRequestPermissionRationale} trả false cho cả hai
     * trường hợp: chưa hỏi lần nào, và người dùng đã chọn "không hỏi lại". Chỉ khi đã hỏi
     * rồi mới biết chắc là không hỏi lại được nữa.
     */
    public static boolean wasPermissionRequested(Context context) {
        return prefs(context).getBoolean(KEY_PERMISSION_REQUESTED, false);
    }

    public static void setPermissionRequested(Context context) {
        prefs(context).edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
