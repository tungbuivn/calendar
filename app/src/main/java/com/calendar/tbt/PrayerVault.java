package com.calendar.tbt;

import android.content.Context;

/** Kho văn khấn đọc một lần từ assets và giữ lại cho các màn hình. */
public final class PrayerVault {

    private static PrayerCatalog catalog;

    private PrayerVault() {
    }

    public static synchronized PrayerCatalog shared(Context context) {
        if (catalog == null) {
            try {
                catalog = PrayerCatalog.load(context.getApplicationContext());
            } catch (Exception e) {
                catalog = PrayerCatalog.empty();
            }
        }
        return catalog;
    }

    /**
     * Đọc danh mục để sẵn vào bộ nhớ — gọi ở tuyến nền khi tiến trình khởi động,
     * đến lúc mở màn hình Văn khấn thì mục lục đã biên dịch xong.
     */
    public static void preload(Context context) {
        shared(context);
    }

    /** Cho phép đọc lại assets sau khi thử nghiệm. */
    public static synchronized void reset() {
        catalog = null;
    }
}
