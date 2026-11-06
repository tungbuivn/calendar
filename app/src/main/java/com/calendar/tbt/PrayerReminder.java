package com.calendar.tbt;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Nhắc mỗi sáng: nếu hôm nay có nghi thức cần khấn thì báo một thông báo chung,
 * không ghi tên bài khấn hay tên người mất trong nội dung.
 */
public final class PrayerReminder {

    public static final String ACTION_REMIND = "com.calendar.tbt.action.PRAYER_REMIND";

    private static final String CHANNEL_ID = "prayer_reminder";
    private static final int NOTIFICATION_ID = 7101;
    private static final int REQUEST_CODE = 7102;
    private static final String MEMORIAL_CHANNEL_ID = "memorial_reminder";
    private static final int MEMORIAL_NOTIFICATION_ID = 7103;
    private static final int MEMORIAL_REQUEST_CODE = 7104;
    private static final String EVENT_CHANNEL_ID = "event_reminder";
    private static final int EVENT_NOTIFICATION_ID = 7105;
    private static final int EVENT_REQUEST_CODE = 7106;
    private static final int HOUR_OF_DAY = 7;

    private PrayerReminder() {
    }

    /** Đặt báo thức cho 7 giờ sáng gần nhất và tự nối tiếp nhau mỗi ngày. */
    public static void scheduleNextMorning(Context context) {
        if (!ReminderPreferences.areNotificationsEnabled(context)) {
            cancelAlarm(context);
            return;
        }
        AlarmManager alarm = (AlarmManager) context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, HOUR_OF_DAY);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_MONTH, 1);

        PendingIntent pending = remindIntent(context);
        if (canSetExactToTheMinute(alarm)) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending);
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending);
        }
    }

    /** Đăng thông báo theo tôm nay rồi đặt báo thức cho lần nhắc kế tiếp. */
    public static void refreshNow(Context context) {
        if (!ReminderPreferences.areNotificationsEnabled(context)) {
            cancelAll(context);
            return;
        }
        notifyToday(context);
        scheduleNextMorning(context);
    }

    /** Gỡ toàn bộ thông báo đã hiển thị (dùng khi người dùng tắt tính năng). */
    public static void cancelAll(Context context) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.cancel(NOTIFICATION_ID);
        manager.cancel(MEMORIAL_NOTIFICATION_ID);
        manager.cancel(EVENT_NOTIFICATION_ID);
        cancelAlarm(context);
    }

    /** Hủy báo thức đã đặt, tránh đánh thức máy khi người dùng đã tắt nhắc. */
    private static void cancelAlarm(Context context) {
        AlarmManager alarm = (AlarmManager) context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        alarm.cancel(remindIntent(context));
    }

    /** Hiển thị (hoặc gỡ) thông báo theo đúng nội dung nghi thức của tôm nay. */
    public static void notifyToday(Context context) {
        if (!ReminderPreferences.areNotificationsEnabled(context)) return;
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) return;
        createChannel(context);
        if (hasRitesToday(context)) {
            manager.notify(NOTIFICATION_ID, buildNotification(context));
        } else {
            manager.cancel(NOTIFICATION_ID);
        }
        notifyUpcomingMemorial(context);
        notifyUpcomingRites(context);
    }

    /** Báo trước N ngày nếu sắp tới có ngày giỗ; N lấy từ ReminderPreferences. */
    private static void notifyUpcomingMemorial(Context context) {
        int daysBefore = ReminderPreferences.getDaysBefore(context);
        Calendar target = Calendar.getInstance();
        target.add(Calendar.DAY_OF_MONTH, daysBefore);
        LunarCalendar.LunarDate lunar = LunarCalendar.getLunarDate(
                target.get(Calendar.DAY_OF_MONTH),
                target.get(Calendar.MONTH) + 1,
                target.get(Calendar.YEAR));
        List<GioStore.Memorial> matches = new ArrayList<>();
        for (GioStore.Memorial memorial : GioStore.all(context)) {
            if (memorial.lunarDay == lunar.day && memorial.lunarMonth == lunar.month) {
                matches.add(memorial);
            }
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (matches.isEmpty()) {
            manager.cancel(MEMORIAL_NOTIFICATION_ID);
            return;
        }
        createMemorialChannel(context);
        String when = daysBefore == 0 ? "Hôm nay"
                : daysBefore == 1 ? "Ngày mai"
                : "Còn " + daysBefore + " ngày";
        List<String> labels = new ArrayList<>();
        for (GioStore.Memorial memorial : matches) {
            labels.add(memorial.name + " (âm lịch "
                    + memorial.lunarDay + "/" + memorial.lunarMonth + ")");
        }
        String text = when + ": giỗ " + TextUtils.join(", ", labels) + ".";
        manager.notify(MEMORIAL_NOTIFICATION_ID, buildMemorialNotification(context, text));
    }

    private static boolean hasRitesToday(Context context) {
        Calendar now = Calendar.getInstance();
        return !PrayerDayMatcher.forDate(context.getApplicationContext(), PrayerVault.shared(context),
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR)).isEmpty();
    }

    private static android.app.Notification buildNotification(Context context) {
        Intent open = new Intent(context, VanKhanActivity.class);
        Calendar now = Calendar.getInstance();
        open.putExtra(VanKhanActivity.EXTRA_YEAR, now.get(Calendar.YEAR));
        open.putExtra(VanKhanActivity.EXTRA_MONTH, now.get(Calendar.MONTH) + 1);
        open.putExtra(VanKhanActivity.EXTRA_DAY, now.get(Calendar.DAY_OF_MONTH));
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(context.getString(R.string.prayer_reminder_title))
                .setContentText(context.getString(R.string.prayer_reminder_text))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.prayer_reminder_text)))
                .setContentIntent(PendingIntent.getActivity(context, REQUEST_CODE, open, flags()))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();
    }

    /** Báo trước N ngày nếu sắp tới có lễ tiết trong danh mục văn khấn. */
    private static void notifyUpcomingRites(Context context) {
        int daysBefore = ReminderPreferences.getDaysBefore(context);
        if (daysBefore == 0) {
            NotificationManagerCompat.from(context).cancel(EVENT_NOTIFICATION_ID);
            return;
        }
        Calendar target = Calendar.getInstance();
        target.add(Calendar.DAY_OF_MONTH, daysBefore);
        int day = target.get(Calendar.DAY_OF_MONTH);
        int month = target.get(Calendar.MONTH) + 1;
        int year = target.get(Calendar.YEAR);
        List<PrayerDayMatcher.Suggestion> entries = PrayerDayMatcher.forDate(
                context.getApplicationContext(), PrayerVault.shared(context), day, month, year);
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        List<String> titles = new ArrayList<>();
        // Chỉ lấy lễ tiết; ngày giỗ đã có thông báo riêng ở trên.
        for (PrayerDayMatcher.Suggestion suggestion : entries) {
            if (suggestion.memorial == null) titles.add(suggestion.entry.title);
        }
        if (titles.isEmpty()) {
            manager.cancel(EVENT_NOTIFICATION_ID);
            return;
        }
        createEventChannel(context);
        String when = daysBefore == 1 ? "Ngày mai" : "Còn " + daysBefore + " ngày";
        String text = when + ": " + TextUtils.join(", ", titles) + ".";
        Intent open = new Intent(context, VanKhanActivity.class);
        open.putExtra(VanKhanActivity.EXTRA_YEAR, year);
        open.putExtra(VanKhanActivity.EXTRA_MONTH, month);
        open.putExtra(VanKhanActivity.EXTRA_DAY, day);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        android.app.Notification notification = new NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("Sắp tới ngày lễ")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(PendingIntent.getActivity(context, EVENT_REQUEST_CODE, open, flags()))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();
        manager.notify(EVENT_NOTIFICATION_ID, notification);
    }

    private static void createEventChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(EVENT_CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(EVENT_CHANNEL_ID,
                "Nhắc ngày lễ", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Thông báo trước ngày lễ tiết");
        channel.setShowBadge(true);
        manager.createNotificationChannel(channel);
    }
    private static android.app.Notification buildMemorialNotification(Context context, String text) {
        Intent open = new Intent(context, GioSettingsActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return new NotificationCompat.Builder(context, MEMORIAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("Sắp tới ngày giỗ")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(PendingIntent.getActivity(context, MEMORIAL_REQUEST_CODE,
                        open, flags()))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();
    }

    private static void createMemorialChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(MEMORIAL_CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(MEMORIAL_CHANNEL_ID,
                "Nhắc ngày giỗ", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Thông báo trước ngày giỗ");
        channel.setShowBadge(true);
        manager.createNotificationChannel(channel);
    }
    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.prayer_reminder_channel), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.prayer_reminder_text));
        channel.setShowBadge(true);
        manager.createNotificationChannel(channel);
    }

    private static PendingIntent remindIntent(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), PrayerReminderReceiver.class)
                .setAction(ACTION_REMIND);
        return PendingIntent.getBroadcast(context.getApplicationContext(), REQUEST_CODE, intent, flags());
    }

    private static boolean canSetExactToTheMinute(AlarmManager alarm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return alarm.canScheduleExactAlarms();
        return true;
    }

    private static int flags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return flags;
    }
}
