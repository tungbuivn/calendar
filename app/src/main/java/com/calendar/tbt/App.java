package com.calendar.tbt;

import android.app.Application;
import android.content.Context;

/** Khởi động sớm phần dữ liệu văn khấn để các màn hình mở ra là có ngay. */
public final class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        final Context appContext = getApplicationContext();
        Thread warmup = new Thread(() -> {
            ProfileStore.preload(appContext);
            GioStore.preload(appContext);
        GioStore.ensureFamilyLinks(appContext);
            PrayerVault.preload(appContext);
            PrayerReminder.scheduleNextMorning(appContext);
        }, "calendar-warmup");
        warmup.setPriority(Thread.MIN_PRIORITY);
        warmup.start();
    }
}
