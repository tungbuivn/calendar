package com.calendar.tbt;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.job.JobScheduler;

public class CalendarWidget extends AppWidgetProvider {

    private static long lastUpdateTime = 0;
    private static final long MIN_UPDATE_INTERVAL = 60000; // 60 seconds minimum between updates

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        android.util.Log.d("CalendarWidget", "onUpdate called with " + appWidgetIds.length + " widgets");

        // Force fresh time calculation for each update
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis()); // Ensure fresh time

        // Always update when system calls onUpdate (no throttling for system calls)
        for (int appWidgetId : appWidgetIds) {
            android.util.Log.d("CalendarWidget", "Updating widget " + appWidgetId + " with fresh time data");
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        // Start job service for periodic updates only if app is running
        schedulePeriodicUpdates(context);
        // if (isAppRunning(context)) {
        // WidgetUpdateJobService.scheduleWidgetJob(context);
        // } else {
        // android.util.Log.d("CalendarWidget", "App not running - relying on system
        // updates every 5 seconds");
        // }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        android.util.Log.d("CalendarWidget", "Received intent: " + action);

        if (action != null) {
            switch (action) {

                case "android.appwidget.action.APPWIDGET_UPDATE":
                case "android.intent.action.BOOT_COMPLETED":
                case "android.intent.action.MY_PACKAGE_REPLACED":
                    // Force immediate update
                    android.util.Log.d("CalendarWidget", "Forcing immediate update due to: " + action);
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    ComponentName componentName = new ComponentName(context, CalendarWidget.class);
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

                    if (appWidgetIds.length > 0) {
                        // Update all widgets immediately
                        for (int appWidgetId : appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, appWidgetId);
                        }

                        // Start service for continuous updates only if app is running
                        // if (isAppRunning(context)) {
                        //     WidgetUpdateJobService.scheduleWidgetJob(context);
                        // }
                    }
                    schedulePeriodicUpdates(context);
                    break;
            }
        }
    }

    private boolean isAppRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals("com.calendar.tbt")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        android.util.Log.d("CalendarWidget", "Widget enabled - starting update services");

        // Force an immediate update to ensure widgets show current data
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, CalendarWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        if (appWidgetIds.length > 0) {
            android.util.Log.d("CalendarWidget", "Forcing immediate update for " + appWidgetIds.length + " widgets");
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }

        // Start both JobService and AlarmManager for maximum reliability
        // WidgetUpdateJobService.scheduleWidgetJob(context);
        schedulePeriodicUpdates(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        android.util.Log.d("CalendarWidget", "Widget disabled - stopping all update services");
        // Stop all update services when all widgets are removed
        // WidgetUpdateJobService.cancelWidgetJob(context);
        cancelPeriodicUpdates(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
            Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        android.util.Log.d("CalendarWidget", "Widget options changed - updating widget");

        // Ensure the update job service is running
        // WidgetUpdateJobService.scheduleWidgetJob(context);

        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
        android.util.Log.d("CalendarWidget", "Widget restored - updating widgets");

        // Ensure the update job service is running
        // WidgetUpdateJobService.scheduleWidgetJob(context);
        schedulePeriodicUpdates(context);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        // widget already on screen, so no need to update
        for (int appWidgetId : newWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // Method to update only the time component
    public static void updateWidgetTime(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        android.util.Log.d("CalendarWidget", "updateWidgetTime called with " + appWidgetIds.length + " widgets");

        WidgetPreferences preferences = new WidgetPreferences(context);
        Locale locale = preferences.getLocale();

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", locale);
        String currentTime = timeFormat.format(now);

        // Add detailed time logging (reduced frequency)
        if (System.currentTimeMillis() % 30000 < 5000) { // Only log every 30 seconds
            android.util.Log.d("CalendarWidget", "System time: " + System.currentTimeMillis() +
                    ", Calendar time: " + calendar.getTimeInMillis() +
                    ", Formatted time: " + currentTime);
        }

        // Calculate lunar date
        String lunarDate = calculateLunarDate();

        // Get weather information
        String weatherInfo = getWeatherInfo(context);

        android.util.Log.d("CalendarWidget",
                "Current time: " + currentTime + ", Lunar date: " + lunarDate + ", Weather: " + weatherInfo);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.calendar_widget);

            // Update time, lunar calendar, and weather
            views.setTextViewText(R.id.widget_time, currentTime);
            views.setTextViewTextSize(R.id.widget_time, android.util.TypedValue.COMPLEX_UNIT_SP,
                    preferences.getTimeFontSize());
            views.setTextViewText(R.id.widget_moon_calendar, lunarDate);
            views.setTextViewText(R.id.widget_weather, weatherInfo);
            views.setTextViewTextSize(R.id.widget_weather, android.util.TypedValue.COMPLEX_UNIT_SP,
                    preferences.getTimeFontSize());

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
            android.util.Log.d("CalendarWidget", "Updated widget " + appWidgetId + " with time: " + currentTime
                    + ", lunar: " + lunarDate + ", weather: " + weatherInfo);
        }
    }

    private static String calculateLunarDate() {
        // Sử dụng class LunarCalendar mới với thuật toán thiên văn chính xác
        LunarCalendar.LunarDate lunarDate = LunarCalendar.getCurrentLunarDate();
        return lunarDate.toString();
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Get preferences
        WidgetPreferences preferences = new WidgetPreferences(context);
        Locale locale = preferences.getLocale();
        String language = preferences.getLanguage();

        // Get current date and time - ALWAYS fresh
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis()); // Force fresh time
        Date now = calendar.getTime();

        // Log the current time being used
        android.util.Log.d("CalendarWidget", "Main update - System time: " + System.currentTimeMillis() +
                ", Calendar time: " + calendar.getTimeInMillis() +
                ", Formatted time: " + new SimpleDateFormat("HH:mm", locale).format(now));

        // Format the time and date components with selected locale
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", locale);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", locale);
        SimpleDateFormat dayNumberFormat = new SimpleDateFormat("d", locale);
        SimpleDateFormat monthNumberFormat = new SimpleDateFormat("M", locale);

        String currentTime = timeFormat.format(now);
        String dayName = dayFormat.format(now);
        String dayNumber = dayNumberFormat.format(now);
        String monthNumber = monthNumberFormat.format(now);

        // Add detailed time logging (reduced frequency)
        if (System.currentTimeMillis() % 30000 < 5000) { // Only log every 30 seconds
            android.util.Log.d("CalendarWidget", "Main update - System time: " + System.currentTimeMillis() +
                    ", Calendar time: " + calendar.getTimeInMillis() +
                    ", Formatted time: " + currentTime);
        }

        // Create day and date string like "Wed 1/8"
        // change dayName to vietnamese if language is vi
        String dayDateString = "";
        if ("vi".equals(language)) {
            // Get the day of week and map to Vietnamese names
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            switch (dayOfWeek) {
                case Calendar.MONDAY:
                    dayName = "Thứ Hai";
                    break;
                case Calendar.TUESDAY:
                    dayName = "Thứ Ba";
                    break;
                case Calendar.WEDNESDAY:
                    dayName = "Thứ Tư";
                    break;
                case Calendar.THURSDAY:
                    dayName = "Thứ Năm";
                    break;
                case Calendar.FRIDAY:
                    dayName = "Thứ Sáu";
                    break;
                case Calendar.SATURDAY:
                    dayName = "Thứ Bảy";
                    break;
                case Calendar.SUNDAY:
                    dayName = "Chủ Nhật";
                    break;
            }
        }
        dayDateString = dayName + " " + dayNumber + "/" + monthNumber;

        // Calculate current week dates
        Calendar weekCalendar = Calendar.getInstance();
        // Force week to start on Monday
        weekCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        int[] weekDates = new int[7];
        for (int i = 0; i < 7; i++) {
            weekDates[i] = weekCalendar.get(Calendar.DAY_OF_MONTH);
            weekCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        android.util.Log.d("CalendarWidget", "Week dates: " + weekDates[0] + ", " + weekDates[1] + ", " + weekDates[2]
                + ", " + weekDates[3] + ", " + weekDates[4] + ", " + weekDates[5] + ", " + weekDates[6]);

        // Create RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.calendar_widget);

        // Calculate lunar date
        String lunarDate = calculateLunarDate();
        android.util.Log.d("CalendarWidget", "Lunar date: " + lunarDate);

        // Get weather information
        String weatherInfo = getWeatherInfo(context);
        android.util.Log.d("CalendarWidget", "Weather info: " + weatherInfo);

        // Set the text views with custom font sizes
        views.setTextViewText(R.id.widget_time, currentTime);
        views.setTextViewText(R.id.widget_day_date, dayDateString);
        views.setTextViewText(R.id.widget_moon_calendar, lunarDate);
        views.setTextViewText(R.id.widget_weather, weatherInfo);

        // Set font sizes from preferences
        views.setTextViewTextSize(R.id.widget_time, android.util.TypedValue.COMPLEX_UNIT_SP,
                preferences.getTimeFontSize());
        views.setTextViewTextSize(R.id.widget_day_date, android.util.TypedValue.COMPLEX_UNIT_SP,
                preferences.getDayFontSize());
        views.setTextViewTextSize(R.id.widget_weather, android.util.TypedValue.COMPLEX_UNIT_SP,
                preferences.getWeatherFontSize());

        // Set day labels based on language
        if ("vi".equals(language)) {
            // Vietnamese day labels
            views.setTextViewText(R.id.label_day_mon, "T2");
            views.setTextViewText(R.id.label_day_tue, "T3");
            views.setTextViewText(R.id.label_day_wed, "T4");
            views.setTextViewText(R.id.label_day_thu, "T5");
            views.setTextViewText(R.id.label_day_fri, "T6");
            views.setTextViewText(R.id.label_day_sat, "T7");
            views.setTextViewText(R.id.label_day_sun, "CN");
        } else {
            // English day labels
            views.setTextViewText(R.id.label_day_mon, "M");
            views.setTextViewText(R.id.label_day_tue, "T");
            views.setTextViewText(R.id.label_day_wed, "W");
            views.setTextViewText(R.id.label_day_thu, "T");
            views.setTextViewText(R.id.label_day_fri, "F");
            views.setTextViewText(R.id.label_day_sat, "S");
            views.setTextViewText(R.id.label_day_sun, "S");
        }

        // Set week dates with custom font size
        views.setTextViewText(R.id.day_mon, String.valueOf(weekDates[0]));
        views.setTextViewText(R.id.day_tue, String.valueOf(weekDates[1]));
        views.setTextViewText(R.id.day_wed, String.valueOf(weekDates[2]));
        views.setTextViewText(R.id.day_thu, String.valueOf(weekDates[3]));
        views.setTextViewText(R.id.day_fri, String.valueOf(weekDates[4]));
        views.setTextViewText(R.id.day_sat, String.valueOf(weekDates[5]));
        views.setTextViewText(R.id.day_sun, String.valueOf(weekDates[6]));

        android.util.Log.d("CalendarWidget", "Set week dates to views");

        // Ensure the top section is visible by setting a minimum height
        views.setInt(R.id.widget_container, "setMinimumHeight", 200);

        // Set font sizes for week days
        int weekFontSize = preferences.getWeekFontSize();
        views.setTextViewTextSize(R.id.day_mon, android.util.TypedValue.COMPLEX_UNIT_SP, weekFontSize);
        views.setTextViewTextSize(R.id.day_tue, android.util.TypedValue.COMPLEX_UNIT_SP, weekFontSize);
        views.setTextViewTextSize(R.id.day_wed, android.util.TypedValue.COMPLEX_UNIT_SP, weekFontSize);
        views.setTextViewTextSize(R.id.day_thu, android.util.TypedValue.COMPLEX_UNIT_SP, weekFontSize);
        views.setTextViewTextSize(R.id.day_fri, android.util.TypedValue.COMPLEX_UNIT_SP, weekFontSize);
        views.setTextViewTextSize(R.id.day_sat, android.util.TypedValue.COMPLEX_UNIT_SP, weekFontSize);
        views.setTextViewTextSize(R.id.day_sun, android.util.TypedValue.COMPLEX_UNIT_SP, weekFontSize);

        // Highlight current day - Fixed calculation
        Calendar currentCalendar = Calendar.getInstance();
        int currentDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);

        // Map Calendar.DAY_OF_WEEK to our 0-6 index (Monday=0, Sunday=6)
        int currentIndex;
        switch (currentDayOfWeek) {
            case Calendar.MONDAY:
                currentIndex = 0;
                break;
            case Calendar.TUESDAY:
                currentIndex = 1;
                break;
            case Calendar.WEDNESDAY:
                currentIndex = 2;
                break;
            case Calendar.THURSDAY:
                currentIndex = 3;
                break;
            case Calendar.FRIDAY:
                currentIndex = 4;
                break;
            case Calendar.SATURDAY:
                currentIndex = 5;
                break;
            case Calendar.SUNDAY:
                currentIndex = 6;
                break;
            default:
                currentIndex = 0;
                break;
        }

        android.util.Log.d("CalendarWidget",
                "Current day of week: " + currentDayOfWeek + ", Current index: " + currentIndex);

        // Clear all backgrounds first
        views.setInt(R.id.day_mon, "setBackgroundResource", 0);
        views.setInt(R.id.day_tue, "setBackgroundResource", 0);
        views.setInt(R.id.day_wed, "setBackgroundResource", 0);
        views.setInt(R.id.day_thu, "setBackgroundResource", 0);
        views.setInt(R.id.day_fri, "setBackgroundResource", 0);
        views.setInt(R.id.day_sat, "setBackgroundResource", 0);
        views.setInt(R.id.day_sun, "setBackgroundResource", 0);

        // Set current day background and text color
        switch (currentIndex) {
            case 0: // Monday
                views.setInt(R.id.day_mon, "setBackgroundResource", R.drawable.current_day_background);
                views.setTextColor(R.id.day_mon, 0xFFFFFFFF);
                break;
            case 1: // Tuesday
                views.setInt(R.id.day_tue, "setBackgroundResource", R.drawable.current_day_background);
                views.setTextColor(R.id.day_tue, 0xFFFFFFFF);
                break;
            case 2: // Wednesday
                views.setInt(R.id.day_wed, "setBackgroundResource", R.drawable.current_day_background);
                views.setTextColor(R.id.day_wed, 0xFFFFFFFF);
                break;
            case 3: // Thursday
                views.setInt(R.id.day_thu, "setBackgroundResource", R.drawable.current_day_background);
                views.setTextColor(R.id.day_thu, 0xFFFFFFFF);
                break;
            case 4: // Friday
                views.setInt(R.id.day_fri, "setBackgroundResource", R.drawable.current_day_background);
                views.setTextColor(R.id.day_fri, 0xFFFFFFFF);
                break;
            case 5: // Saturday
                views.setInt(R.id.day_sat, "setBackgroundResource", R.drawable.current_day_background);
                views.setTextColor(R.id.day_sat, 0xFFFFFFFF);
                break;
            case 6: // Sunday
                views.setInt(R.id.day_sun, "setBackgroundResource", R.drawable.current_day_background);
                views.setTextColor(R.id.day_sun, 0xFFFFFFFF);
                break;
        }

        // Create an intent to launch the main activity when widget is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // Static method to update all widgets
    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, CalendarWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        if (appWidgetIds.length > 0) {
            android.util.Log.d("CalendarWidget", "Updating " + appWidgetIds.length + " widgets via JobService");
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        } else {
            android.util.Log.d("CalendarWidget", "No widgets found to update");
        }
    }

    private static String getWeatherInfo(Context context) {
        // For now, return a placeholder weather info
        // In a real implementation, you would fetch weather data from a weather API
        // For example, using OpenWeatherMap API or similar service

        // Simulate weather data based on time of day
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String weatherInfo;
        if (hour >= 6 && hour < 12) {
            weatherInfo = "22°C ☀️";
        } else if (hour >= 12 && hour < 18) {
            weatherInfo = "28°C 🌤️";
        } else if (hour >= 18 && hour < 22) {
            weatherInfo = "24°C 🌅";
        } else {
            weatherInfo = "20°C 🌙";
        }

        return weatherInfo;
    }

    public static void schedulePeriodicUpdates(Context context) {
        android.util.Log.d("CalendarWidget", "Scheduling periodic updates with AlarmManager");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CalendarWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // AlarmManager alarmManager = (AlarmManager)
        // context.getSystemService(Context.ALARM_SERVICE);
        // Intent intent = new Intent(context, WidgetUpdateReceiver.class); // A
        // BroadcastReceiver or Service
        // PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
        // PendingIntent.FLAG_IMMUTABLE);
        long currentTime = System.currentTimeMillis();
        long nextMinute = ((currentTime / 60000) + 1) * 60000;
        // long triggerAtMillis = nextMinuteBoundary;

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMinute,
                pendingIntent);
        // Schedule updates every 5 seconds
        // long currentTime = System.currentTimeMillis();
        // long nextMinute = ((currentTime / 60000) + 1) * 60000;
        // long interval = 5000; // 5 seconds
        // long firstRun = System.currentTimeMillis() + interval;

        // if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        // alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstRun,
        // pendingIntent);
        // } else {
        // alarmManager.setExact(AlarmManager.RTC_WAKEUP, firstRun, pendingIntent);
        // }

        android.util.Log.d("CalendarWidget", "AlarmManager scheduled for " + nextMinute + "ms intervals");
    }

    private void cancelPeriodicUpdates(Context context) {
        android.util.Log.d("CalendarWidget", "Canceling periodic updates");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CalendarWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
    }
}