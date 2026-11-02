package com.calendar.tbt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Nhận báo thức mỗi sáng và đặt lại báo thức sau khi mở máy hoặc đổi ngày/giờ. */
public final class PrayerReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) return;
        if (PrayerReminder.ACTION_REMIND.equals(action) || Intent.ACTION_DATE_CHANGED.equals(action)) {
            // Đúng giờ nhắc, hoặc vừa sang ngày mới: thông báo phải theo ngày hiện tại.
            run(context, true);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            run(context, false);
        }
    }

    /** Việc đọc danh mục và tính âm lịch chạy ở tuyến nền để không chặn máy thu. */
    private void run(final Context context, final boolean notify) {
        final PendingResult result = goAsync();
        new Thread(() -> {
            try {
                Context app = context.getApplicationContext();
                if (notify) PrayerReminder.notifyToday(app);
                PrayerReminder.scheduleNextMorning(app);
            } catch (Exception e) {
                android.util.Log.e("PrayerReminder", "Không đặt được thông báo nhắc khấn", e);
            } finally {
                result.finish();
            }
        }, "prayer-reminder").start();
    }
}
