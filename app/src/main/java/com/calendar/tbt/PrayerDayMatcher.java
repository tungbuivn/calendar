package com.calendar.tbt;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tìm bài khấn áp dụng cho một ngày dương lịch: nhãn ngày đã gán trong mục lục
 * cộng với các lễ giỗ của người đã khuất mà người dùng đã cài.
 */
public final class PrayerDayMatcher {

    public static final class Suggestion {
        public final PrayerCatalog.Entry entry;
        public final String label;
        public final GioStore.Memorial memorial;

        Suggestion(PrayerCatalog.Entry entry, String label, GioStore.Memorial memorial) {
            this.entry = entry;
            this.label = label;
            this.memorial = memorial;
        }

        public boolean isMemorial() {
            return memorial != null;
        }
    }

    private PrayerDayMatcher() {
    }

    public static List<Suggestion> forDate(Context context, PrayerCatalog catalog,
                                           int solarDay, int solarMonth, int solarYear) {
        List<Suggestion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        LunarCalendar.LunarDate lunar = safeLunar(solarDay, solarMonth, solarYear);
        if (lunar == null) return result;

        java.util.Calendar tomorrow = java.util.Calendar.getInstance();
        tomorrow.clear();
        tomorrow.set(solarYear, solarMonth - 1, solarDay);
        tomorrow.add(java.util.Calendar.DAY_OF_MONTH, 1);
        LunarCalendar.LunarDate tomorrowLunar = safeLunar(
                tomorrow.get(java.util.Calendar.DAY_OF_MONTH),
                tomorrow.get(java.util.Calendar.MONTH) + 1,
                tomorrow.get(java.util.Calendar.YEAR));

        for (GioStore.Memorial memorial : GioStore.all(context)) {
            addMemorialRites(result, catalog, seen, lunar, memorial);
            addMemorialEve(result, catalog, seen, tomorrowLunar, memorial);
            addMourningMilestones(result, catalog, seen, lunar, memorial);
        }

        String term = PrayerCatalog.fold(lunar.getSolarTerm());
        for (PrayerCatalog.Entry entry : catalog.entries()) {
            for (PrayerCatalog.Rule rule : entry.when) {
                if (rule.matches(lunar, term)) {
                    add(result, catalog, seen, entry.key,
                            rule.label.isEmpty() ? "Lễ trong ngày" : rule.label, null);
                    break;
                }
            }
        }
        return result;
    }

    /** Đúng ngày giỗ: chọn bài theo số năm chẵn kể từ năm mất. */
    private static void addMemorialRites(List<Suggestion> result, PrayerCatalog catalog, Set<String> seen,
                                         LunarCalendar.LunarDate lunar, GioStore.Memorial memorial) {
        if (!memorial.fallsOn(lunar.day, lunar.month)) return;
        int years = memorial.anniversary(lunar.year);
        if (years == 1) {
            add(result, catalog, seen, "van-khan-ngay-gio-dau", "Giỗ đầu — " + memorial.name, memorial);
            add(result, catalog, seen, "van-khan-le-tieu-tuong-dai-tuong", "Lễ tiểu tường — " + memorial.name, memorial);
        } else if (years == 2) {
            add(result, catalog, seen, "van-khan-ngay-gio-het-dai-tuong", "Giỗ hết — " + memorial.name, memorial);
            add(result, catalog, seen, "van-khan-le-tieu-tuong-dai-tuong", "Lễ đại tường — " + memorial.name, memorial);
        } else if (years >= 3) {
            add(result, catalog, seen, "van-khan-khi-cung-gio", "Giỗ thường lần thứ " + years + " — " + memorial.name, memorial);
            add(result, catalog, seen, "van-khan-gia-tien-chinh-ngay-gio-thuong", "Khấn gia tiên ngày giỗ — " + memorial.name, memorial);
            add(result, catalog, seen, "van-khan-tho-than-chinh-gio-thuong",
                    "Khấn Thần linh đúng ngày giỗ — " + memorial.name, memorial);
        }
    }

    /** Hôm trước ngày giỗ một ngày: lễ Tiên Thường, sửa soạn cỗ bàn. */
    private static void addMemorialEve(List<Suggestion> result, PrayerCatalog catalog, Set<String> seen,
                                       LunarCalendar.LunarDate tomorrowLunar, GioStore.Memorial memorial) {
        if (tomorrowLunar == null || !memorial.fallsOn(tomorrowLunar.day, tomorrowLunar.month)) return;
        int years = memorial.anniversary(tomorrowLunar.year);
        if (years == 1) {
            add(result, catalog, seen, "van-khan-tho-than-truoc-gio-dau", "Trước giỗ đầu một ngày — " + memorial.name, memorial);
        } else if (years == 2) {
            add(result, catalog, seen, "van-khan-tho-than-truoc-gio-het", "Trước giỗ hết một ngày — " + memorial.name, memorial);
        } else if (years >= 3) {
            add(result, catalog, seen, "van-khan-gia-tien-ngay-tien-thuong", "Tiên thường — " + memorial.name, memorial);
            add(result, catalog, seen, "van-khan-tho-than-ngay-tien-thuong",
                    "Khấn Thần linh ngày Tiên thường — " + memorial.name, memorial);
        }
    }

    /** Bốn mươi chín ngày và một trăm ngày tính theo ngày mất đã lưu. */
    private static void addMourningMilestones(List<Suggestion> result, PrayerCatalog catalog, Set<String> seen,
                                               LunarCalendar.LunarDate lunar, GioStore.Memorial memorial) {
        int days = lunar.jd - memorial.deathJulianDay();
        if (days == 49) {
            add(result, catalog, seen, "van-khan-le-chung-that-tot-khoc", "Lễ chung thất (49 ngày) — " + memorial.name, memorial);
        } else if (days == 100) {
            add(result, catalog, seen, "van-khan-le-chung-that-tot-khoc", "Lễ tốt khóc (100 ngày) — " + memorial.name, memorial);
        }
    }

    private static void add(List<Suggestion> result, PrayerCatalog catalog, Set<String> seen,
                            String key, String label, GioStore.Memorial memorial) {
        if (key == null || seen.contains(key)) return;
        PrayerCatalog.Entry entry = catalog.findEntry(key);
        if (entry == null) return;
        seen.add(key);
        result.add(new Suggestion(entry, label, memorial));
    }

    private static LunarCalendar.LunarDate safeLunar(int day, int month, int year) {
        try {
            return LunarCalendar.getLunarDate(day, month, year);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
