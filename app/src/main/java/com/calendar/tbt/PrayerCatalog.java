package com.calendar.tbt;

import android.content.Context;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Danh mục văn khấn đọc từ assets/vankhan: index.json (mục lục) và items/&lt;key&gt;.txt (toàn văn).
 * Toàn bộ chi phí phân tích trả một lần khi load: nhãn ngày được biên dịch thành số,
 * tra theo key/trường dùng bảng hash, và toàn văn đã đọc được cache lại.
 */
public final class PrayerCatalog {

    private static final String INDEX_PATH = "vankhan/index.json";
    private static final String ITEM_PATH = "vankhan/items/";
    private static final LruCache<String, String> BODY_CACHE = new LruCache<String, String>(96);

    /** Nhãn ngày đã biên dịch: so sánh theo số, không cắt chuỗi khi gợi ý bài cho một ngày. */
    public static final class Rule {
        private static final int ANY = 0, LUNAR = 1, LUNAR_DAY = 2, LUNAR_MONTH = 3, TERM = 4, NONE = 5;
        private static final Pattern PAIR = Pattern.compile("(\\d+)-(\\d+)");

        private final int kind;
        private final int[] numbers;
        private final String[] terms;
        public final String label;

        private Rule(int kind, int[] numbers, String[] terms, String label) {
            this.kind = kind;
            this.numbers = numbers;
            this.terms = terms;
            this.label = label;
        }

        static Rule compile(String spec, String label) {
            int equals = spec.indexOf('=');
            String kind = equals < 0 ? spec : spec.substring(0, equals);
            String argument = equals < 0 ? "" : spec.substring(equals + 1);
            if ("any".equals(kind)) return new Rule(ANY, EMPTY, null, label);
            String[] tokens = argument.split(",");
            if ("term".equals(kind)) {
                String[] folded = new String[tokens.length];
                for (int i = 0; i < tokens.length; i++) folded[i] = fold(tokens[i]);
                return new Rule(TERM, EMPTY, folded, label);
            }
            int[] values = new int[tokens.length];
            int parsed = 0;
            boolean lunarPair = "lunar".equals(kind);
            for (String token : tokens) {
                if (lunarPair) {
                    Matcher matcher = PAIR.matcher(token.trim());
                    if (matcher.matches()) {
                        values[parsed++] = parseInt(matcher.group(1)) * 100 + parseInt(matcher.group(2));
                    }
                } else {
                    values[parsed++] = parseInt(token);
                }
            }
            int[] trimmed = new int[parsed];
            System.arraycopy(values, 0, trimmed, 0, parsed);
            int type = lunarPair ? LUNAR : "lunarDay".equals(kind) ? LUNAR_DAY
                    : "lunarMonth".equals(kind) ? LUNAR_MONTH : NONE;
            return new Rule(type, trimmed, null, label);
        }

        boolean matches(LunarCalendar.LunarDate lunar, String foldedTerm) {
            if (kind == ANY) return true;
            // Tháng nhuận của cùng ngày/tháng không lặp lại lễ tiết cố định, trừ ngày rằm/mồng một.
            if (lunar.isLeap && kind != LUNAR_DAY) return false;
            switch (kind) {
                case LUNAR: {
                    int packed = lunar.month * 100 + lunar.day;
                    for (int value : numbers) if (value == packed) return true;
                    return false;
                }
                case LUNAR_DAY:
                    for (int value : numbers) if (value == lunar.day) return true;
                    return false;
                case LUNAR_MONTH:
                    for (int value : numbers) if (value == lunar.month) return true;
                    return false;
                case TERM:
                    for (String candidate : terms) if (candidate.equals(foldedTerm)) return true;
                    return false;
                default:
                    return false;
            }
        }

        private static final int[] EMPTY = new int[0];
    }

    public static final class Entry {
        public final String title;
        public final String key;
        public final String path;
        public final List<String> slots;
        public final List<Rule> when;

        Entry(String title, String key, String path, List<String> slots, List<Rule> when) {
            this.title = title;
            this.key = key;
            this.path = path;
            this.slots = slots;
            this.when = when;
        }
    }

    public static final class Field {
        public final String id;
        public final String label;
        public final String group;
        public final String source;
        public final String hint;

        Field(String id, String label, String group, String source, String hint) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.source = source;
            this.hint = hint;
        }
    }

    public static final class Group {
        public final String title;
        public final List<Entry> entries;

        Group(String title, List<Entry> entries) {
            this.title = title;
            this.entries = entries;
        }
    }

    public static final class Part {
        public final String title;
        public final List<Group> groups;

        Part(String title, List<Group> groups) {
            this.title = title;
            this.groups = groups;
        }

        public int entryCount() {
            int total = 0;
            for (Group group : groups) total += group.entries.size();
            return total;
        }
    }

    private final List<Part> parts;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();
    private final Map<String, String> groupTitles = new HashMap<>();
    private final Map<String, Entry> byKey = new HashMap<>();
    private final Map<String, List<Entry>> byField = new HashMap<>();
    private final Map<String, Field> byFieldId = new HashMap<>();

    private PrayerCatalog(List<Part> parts) {
        this.parts = parts;
    }

    public List<Part> parts() {
        return parts;
    }

    /** Toàn bộ bài theo đúng thứ tự mục lục. */
    public List<Entry> entries() {
        return entries;
    }

    public List<Field> fields() {
        return fields;
    }

    public String groupTitle(String groupId) {
        String title = groupTitles.get(groupId);
        return title == null ? groupId : title;
    }

    public Entry findEntry(String key) {
        return byKey.get(key);
    }

    /** Trường điền theo id; null nếu không có. */
    public Field field(String fieldId) {
        return byFieldId.get(fieldId);
    }

    public String fieldLabel(String fieldId) {
        Field field = byFieldId.get(fieldId);
        return field == null ? fieldId : field.label;
    }

    /** Những bài có chỗ điền thuộc về một trường. */
    public List<Entry> entriesUsing(String fieldId) {
        List<Entry> result = byField.get(fieldId);
        return result == null ? Collections.<Entry>emptyList() : result;
    }

    public static PrayerCatalog empty() {
        return new PrayerCatalog(new ArrayList<Part>());
    }

    public static PrayerCatalog load(Context context) throws IOException, JSONException {
        String json = readAsset(context, INDEX_PATH);
        JSONObject root = new JSONObject(json);
        List<Part> parts = new ArrayList<>();
        JSONArray partsJson = root.optJSONArray("parts");
        if (partsJson == null) return new PrayerCatalog(Collections.<Part>emptyList());
        for (int i = 0; i < partsJson.length(); i++) {
            JSONObject partJson = partsJson.optJSONObject(i);
            if (partJson == null) continue;
            String partTitle = partJson.optString("title");
            List<Group> groups = new ArrayList<>();
            JSONArray groupsJson = partJson.optJSONArray("groups");
            if (groupsJson != null) {
                for (int j = 0; j < groupsJson.length(); j++) {
                    JSONObject groupJson = groupsJson.optJSONObject(j);
                    if (groupJson == null) continue;
                    String groupTitle = groupJson.optString("title");
                    List<Entry> entries = new ArrayList<>();
                    JSONArray itemsJson = groupJson.optJSONArray("items");
                    if (itemsJson != null) {
                        for (int k = 0; k < itemsJson.length(); k++) {
                            JSONObject itemJson = itemsJson.optJSONObject(k);
                            if (itemJson == null) continue;
                            String key = itemJson.optString("key");
                            if (key.isEmpty()) continue;
                            entries.add(new Entry(itemJson.optString("title"), key,
                                    partTitle + " · " + groupTitle,
                                    readSlots(itemJson), readRules(itemJson)));
                        }
                    }
                    if (!entries.isEmpty()) groups.add(new Group(groupTitle, entries));
                }
            }
            if (!groups.isEmpty()) parts.add(new Part(partTitle, groups));
        }
        PrayerCatalog catalog = new PrayerCatalog(parts);
        for (Part part : parts) {
            for (Group group : part.groups) {
                for (Entry entry : group.entries) {
                    catalog.entries.add(entry);
                    catalog.byKey.put(entry.key, entry);
                    for (String slot : entry.slots) {
                        List<Entry> users = catalog.byField.get(slot);
                        if (users == null) {
                            users = new ArrayList<>();
                            catalog.byField.put(slot, users);
                        }
                        users.add(entry);
                    }
                }
            }
        }
        JSONObject fieldsJson = root.optJSONObject("fields");
        if (fieldsJson != null) {
            JSONArray groupsJson = fieldsJson.optJSONArray("groups");
            if (groupsJson != null) {
                for (int i = 0; i < groupsJson.length(); i++) {
                    JSONObject group = groupsJson.optJSONObject(i);
                    if (group != null) catalog.groupTitles.put(group.optString("id"), group.optString("title"));
                }
            }
            JSONArray list = fieldsJson.optJSONArray("fields");
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    JSONObject field = list.optJSONObject(i);
                    if (field == null) continue;
                    Field parsed = new Field(field.optString("id"), field.optString("label"),
                            field.optString("group"), field.optString("source"), field.optString("hint"));
                    catalog.fields.add(parsed);
                    catalog.byFieldId.put(parsed.id, parsed);
                }
            }
        }
        return catalog;
    }

    private static List<String> readSlots(JSONObject itemJson) {
        List<String> slots = new ArrayList<>();
        JSONArray array = itemJson.optJSONArray("slots");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String slot = array.optString(i);
                if (!slot.isEmpty() && !slots.contains(slot)) slots.add(slot);
            }
        }
        return slots;
    }

    private static List<Rule> readRules(JSONObject itemJson) {
        List<Rule> rules = new ArrayList<>();
        JSONArray array = itemJson.optJSONArray("when");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject rule = array.optJSONObject(i);
                if (rule == null) continue;
                String spec = rule.optString("when");
                if (spec.isEmpty()) continue;
                rules.add(Rule.compile(spec, rule.optString("label")));
            }
        }
        return rules;
    }

    /** Toàn văn của một bài, đã bỏ dòng tiêu đề markdown ở đầu file và được cache lại. */
    public static String readItem(Context context, String key) throws IOException {
        String cached = BODY_CACHE.get(key);
        if (cached != null) return cached;
        String text = readAsset(context, ITEM_PATH + key + ".txt");
        if (text.startsWith("# ")) {
            int newline = text.indexOf('\n');
            text = newline < 0 ? "" : text.substring(newline + 1);
        }
        text = trimLeading(text);
        BODY_CACHE.put(key, text);
        return text;
    }

    /** stripLeading() chỉ có từ API 24, app còn hỗ trợ API 21. */
    private static String trimLeading(String s) {
        int start = 0;
        while (start < s.length() && Character.isWhitespace(s.charAt(start))) start++;
        return s.substring(start);
    }

    private static String readAsset(Context context, String path) throws IOException {
        try (InputStream in = context.getAssets().open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            return new String(out.toByteArray(), "UTF-8");
        }
    }

    static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Bỏ dấu thanh và khoảng trắng để so tên tiết khí. */
    static String fold(String input) {
        if (input == null) return "";
        String stripped = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{Mn}", "");
        return stripped.replace('đ', 'd').replace('Đ', 'd').toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
