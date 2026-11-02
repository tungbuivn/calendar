package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Danh sách ngày giỗ do người dùng cài; ngày mất luôn được quy đổi về âm lịch. */
public final class GioStore {

    private static final String PREFS = "gio_memorials";
    private static final String KEY_LIST = "list";
    private static final double TZ = 7.0;

    public static final class Memorial {
        public final String id;
        public final String name;
        public final int lunarDay;
        public final int lunarMonth;
        public final int lunarYear;
        public final boolean lunarLeap;
        public final int solarDay;
        public final int solarMonth;
        public final int solarYear;
        public final String burialPlace;
        /** Người tương ứng trong gia phả; rỗng khi chưa nối. Quan hệ khi khấn suy ra từ đây. */
        public final String personId;

        Memorial(String id, String name, int lunarDay, int lunarMonth, int lunarYear, boolean lunarLeap,
                 int solarDay, int solarMonth, int solarYear, String burialPlace, String personId) {
            this.id = id;
            this.name = name;
            this.lunarDay = lunarDay;
            this.lunarMonth = lunarMonth;
            this.lunarYear = lunarYear;
            this.lunarLeap = lunarLeap;
            this.solarDay = solarDay;
            this.solarMonth = solarMonth;
            this.solarYear = solarYear;
            this.burialPlace = burialPlace;
            this.personId = personId == null ? "" : personId.trim();
        }

        /** Ngày mất ở dạng dương lịch để tính 49 ngày, 100 ngày. */
        public int deathJulianDay() {
            return LunarCalendar.jdFromDate(solarDay, solarMonth, solarYear);
        }

        /** Giỗ trùng đúng ngày âm lịch đang xem (không phân biệt tháng nhuận). */
        public boolean fallsOn(int lunarDay, int lunarMonth) {
            return this.lunarDay == lunarDay && this.lunarMonth == lunarMonth;
        }

        public String lunarLabel() {
            return String.format("Âm lịch %d/%d/%d%s", lunarDay, lunarMonth, lunarYear,
                    lunarLeap ? " (tháng nhuận)" : "");
        }

        public String solarLabel() {
            return String.format("Dương lịch %d/%d/%d", solarDay, solarMonth, solarYear);
        }

        /** Năm chẵn lễ tính từ năm mất, ví dụ 1 = giỗ đầu, 2 = giỗ hết. */
        public int anniversary(int lunarYearOfVisit) {
            return lunarYearOfVisit - lunarYear;
        }
    }

    private GioStore() {
    }

    public static List<Memorial> all(Context context) {
        List<Memorial> result = new ArrayList<>();
        String raw = prefs(context).getString(KEY_LIST, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json == null || json.optString("name").isEmpty()) continue;
                result.add(new Memorial(json.optString("id"), json.optString("name"),
                        json.optInt("lunarDay"), json.optInt("lunarMonth"), json.optInt("lunarYear"),
                        json.optBoolean("lunarLeap"), json.optInt("solarDay"), json.optInt("solarMonth"),
                        json.optInt("solarYear"), json.optString("burialPlace", ""),
                        json.optString("personId", "")));
            }
        } catch (JSONException e) {
            // Danh mục hỏng thì coi như chưa cài ngày giỗ nào.
        }
        return result;
    }

    /** Đọc sẵn danh sách giỗ từ đĩa ở tuyến nền. */
    public static void preload(Context context) {
        all(context);
    }

    public static Memorial find(Context context, String id) {
        if (id == null) return null;
        for (Memorial memorial : all(context)) {
            if (memorial.id.equals(id)) return memorial;
        }
        return null;
    }

    public static void save(Context context, Memorial memorial) {
        List<Memorial> list = all(context);
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(memorial.id)) {
                list.set(i, memorial);
                replaced = true;
                break;
            }
        }
        if (!replaced) list.add(memorial);
        write(context, list);
    }

    public static void remove(Context context, String id) {
        List<Memorial> list = all(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                break;
            }
        }
        write(context, list);
    }

    /** Nhập ngày dương, quy đổi ra âm lịch để lưu. */
    public static Memorial fromSolar(String id, String name, int day, int month, int year,
                                     String burial, String personId) {
        LunarCalendar.LunarDate lunar = LunarCalendar.getLunarDate(day, month, year);
        return new Memorial(id == null ? UUID.randomUUID().toString() : id, name.trim(),
                lunar.day, lunar.month, lunar.year, lunar.isLeap, day, month, year, safe(burial),
                safe(personId));
    }

    /** Nhập ngày âm, quy đổi ra dương để tiện tính 49 ngày, 100 ngày. */
    public static Memorial fromLunar(String id, String name, int day, int month, int year,
                                     boolean leap, String burial, String personId) {
        int[] solar = LunarCalendar.convertLunar2Solar(day, month, year, leap ? 1 : 0, TZ);
        if (solar[0] == 0) return null; // ngày âm không tồn tại (tháng nhuận ảo)
        return new Memorial(id == null ? UUID.randomUUID().toString() : id, name.trim(),
                day, month, year, leap, solar[0], solar[1], solar[2], safe(burial), safe(personId));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static void write(Context context, List<Memorial> list) {
        JSONArray array = new JSONArray();
        try {
            for (Memorial memorial : list) {
                JSONObject json = new JSONObject();
                json.put("id", memorial.id);
                json.put("name", memorial.name);
                json.put("lunarDay", memorial.lunarDay);
                json.put("lunarMonth", memorial.lunarMonth);
                json.put("lunarYear", memorial.lunarYear);
                json.put("lunarLeap", memorial.lunarLeap);
                json.put("solarDay", memorial.solarDay);
                json.put("solarMonth", memorial.solarMonth);
                json.put("solarYear", memorial.solarYear);
                json.put("burialPlace", memorial.burialPlace);
                json.put("personId", memorial.personId);
                array.put(json);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs(context).edit().putString(KEY_LIST, array.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
