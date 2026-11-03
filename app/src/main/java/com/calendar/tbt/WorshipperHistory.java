package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Lịch sử tên/nhóm người đứng lễ để lần sau chọn một chạm.
 * Chỉ lưu trên máy; không gửi lên mạng.
 */
public final class WorshipperHistory {

    private static final String PREFS = "prayer_worshipper_history";
    private static final String KEY_NAMES = "names";
    private static final String KEY_GROUPS = "groups";
    private static final int MAX_NAMES = 24;
    private static final int MAX_GROUPS = 8;

    private WorshipperHistory() {
    }

    /** Ghi tên lẻ và nhóm (nếu từ 2 người) sau khi người dùng lưu. */
    public static void record(Context context, String raw) {
        List<String> parts = Worshippers.split(raw);
        if (parts.isEmpty()) return;

        LinkedHashSet<String> names = read(context, KEY_NAMES);
        for (String part : parts) {
            String name = clean(part);
            if (name.isEmpty()) continue;
            removeIgnoreCase(names, name);
            names.add(name);
        }
        trim(names, MAX_NAMES);
        write(context, KEY_NAMES, names);

        if (parts.size() >= 2) {
            List<String> cleaned = new ArrayList<>();
            for (String part : parts) {
                String name = clean(part);
                if (!name.isEmpty()) cleaned.add(name);
            }
            if (cleaned.size() >= 2) {
                String group = TextUtils.join(", ", cleaned);
                LinkedHashSet<String> groups = read(context, KEY_GROUPS);
                removeIgnoreCase(groups, group);
                groups.add(group);
                trim(groups, MAX_GROUPS);
                write(context, KEY_GROUPS, groups);
            }
        }
    }

    /** Tên lẻ, mới nhất trước. */
    public static List<String> names(Context context) {
        return newestFirst(read(context, KEY_NAMES));
    }

    /** Nhóm đầy đủ, mới nhất trước. */
    public static List<String> groups(Context context) {
        return newestFirst(read(context, KEY_GROUPS));
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_NAMES).remove(KEY_GROUPS).apply();
    }

    private static List<String> newestFirst(LinkedHashSet<String> values) {
        List<String> result = new ArrayList<>(values);
        java.util.Collections.reverse(result);
        return result;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static void removeIgnoreCase(LinkedHashSet<String> values, String target) {
        for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
            if (iterator.next().equalsIgnoreCase(target)) {
                iterator.remove();
                return;
            }
        }
    }

    private static void trim(LinkedHashSet<String> values, int max) {
        while (values.size() > max) {
            Iterator<String> iterator = values.iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    private static LinkedHashSet<String> read(Context context, String key) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String raw = prefs(context).getString(key, "");
        if (raw == null || raw.isEmpty()) return values;
        for (String line : raw.split("\n")) {
            String value = line.trim();
            if (!value.isEmpty()) values.add(value);
        }
        return values;
    }

    private static void write(Context context, String key, LinkedHashSet<String> values) {
        prefs(context).edit().putString(key, TextUtils.join("\n", values)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
