package com.calendar.tbt;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

/**
 * Nhắc mỗi sáng: nếu hôm nay có nghi thức cần khấn thì báo một thông báo chung,
 * không ghi tên bài khấn hay tên người mất trong nội dung.
 */
public final class PrayerReminder {

    public static final String ACTION_REMIND = "com.calendar.tbt.action.PRAYER_REMIND";

    private static final String CHANNEL_ID = "prayer_reminder";
    private static final int NOTIFICATION_ID = 7101;
    private static final int REQUEST_CODE = 7102;
    private static final int HOUR_OF_DAY = 7;

    private PrayerReminder() {
    }

    /** Đặt báo thức cho 7 giờ sáng gần nhất và tự nối tiếp nhau mỗi ngày. */
    public static void scheduleNextMorning(Context context) {
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

    /** Đăng thông báo theo hôm nay rồi đặt báo thức cho lần nhắc kế tiếp. */
    public static void refreshNow(Context context) {
        notifyToday(context);
        scheduleNextMorning(context);
    }

    /** Hiển thị (hoặc gỡ) thông báo theo đúng nội dung nghi thức của hôm nay. */
    public static void notifyToday(Context context) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) return;
        createChannel(context);
        if (hasRitesToday(context)) {
            manager.notify(NOTIFICATION_ID, buildNotification(context));
        } else {
            manager.cancel(NOTIFICATION_ID);
        }
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
