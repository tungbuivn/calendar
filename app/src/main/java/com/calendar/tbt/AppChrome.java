package com.calendar.tbt;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Header + bottom nav dùng chung cho các màn con; body do từng màn tự chứa. */
public final class AppChrome {

    private AppChrome() {
    }

    public static void setup(Activity activity, int selectedItemId) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(titleFor(activity));
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(v -> activity.finish());
        }

        BottomNavigationView bottom = activity.findViewById(R.id.bottom_navigation);
        if (bottom == null) return;
        if (bottom.getMenu().findItem(selectedItemId) != null) {
            bottom.setSelectedItemId(selectedItemId);
        }
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;
            open(activity, id);
            return true;
        });

        final int baseBottom = bottom.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bottom, (view, insets) -> {
            int navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), baseBottom + navBar);
            return insets;
        });
        ViewCompat.requestApplyInsets(bottom);
    }

    private static void open(Activity activity, int id) {
        Class<?> target;
        if (id == R.id.month_calendar) {
            target = MonthCalendarActivity.class;
        } else if (id == R.id.van_khan) {
            target = VanKhanActivity.class;
        } else if (id == R.id.gia_pha) {
            target = GiaPhaActivity.class;
        } else if (id == R.id.today) {
            target = MainActivity.class;
        } else {
            return;
        }
        Intent intent = new Intent(activity, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static String titleFor(Activity activity) {
        if (activity instanceof GiaPhaActivity) return "Gia phả";
        if (activity instanceof GioSettingsActivity) return "Ngày giỗ";
        if (activity instanceof MonthCalendarActivity) return "Lịch tháng";
        if (activity instanceof PrayerFieldActivity) return "Trường điền";
        if (activity instanceof PrayerProfileActivity) return "Thông tin điền sẵn";
        if (activity instanceof TtsSettingsActivity) return "Giọng đọc";
        if (activity instanceof VanKhanActivity) return "Văn khấn";
        if (activity instanceof VanKhanDetailActivity) return "Văn khấn";
        if (activity instanceof WidgetSettingsActivity) return "Widget Settings";
        return activity.getString(R.string.app_name);
    }
}
