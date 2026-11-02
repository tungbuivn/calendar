package com.calendar.tbt;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Điền thông tin vào toàn văn: marker {{field}} thay bằng giá trị đã cấu hình,
 * chỗ chưa có giá trị giữ dạng dấu chấm và được tô khác màu.
 */
public final class PrayerFiller {

    private static final Pattern MARKER = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    private static final String BLANK = "……………………";

    public static final class Segment {
        public final String text;
        public final String field;
        public final boolean filled;

        Segment(String text, String field, boolean filled) {
            this.text = text;
            this.field = field;
            this.filled = filled;
        }
    }

    public static final class Rendered {
        public final CharSequence text;
        public final int missing;

        Rendered(CharSequence text, int missing) {
            this.text = text;
            this.missing = missing;
        }
    }

    private PrayerFiller() {
    }

    /** Giá trị hiện có cho từng trường: tự động theo ngày, theo ngày giỗ, và hồ sơ người cúng. */
    public static Map<String, String> values(Context context, LunarCalendar.LunarDate lunar,
                                             GioStore.Memorial memorial) {
        Map<String, String> values = new HashMap<>();
        for (PrayerCatalog.Field field : PrayerVault.shared(context).fields()) {
            String value = ProfileStore.get(context, field.id);
            if (!value.isEmpty()) values.put(field.id, value);
        }
        if (!values.containsKey("tuoi")) {
            String birth = values.get("nam_sinh");
            if (birth != null && lunar != null) {
                try {
                    int year = Integer.parseInt(birth.trim());
                    int age = lunar.year - year + 1;
                    if (age > 0 && age < 120) values.put("tuoi", String.valueOf(age));
                } catch (NumberFormatException ignored) {
                    // Năm sinh không hợp lệ thì bỏ trống tuổi.
                }
            }
        }
        if (lunar != null) {
            values.put("ngay_am", LunarNames.day(lunar.day));
            values.put("thang_am", LunarNames.month(lunar.month, lunar.isLeap));
            values.put("nam_am", lunar.getYearName());
        }
        if (memorial != null) {
            values.put("huong_linh", memorial.name);
            values.put("ngay_mat", LunarNames.day(memorial.lunarDay));
            values.put("thang_mat", LunarNames.month(memorial.lunarMonth, memorial.lunarLeap));
            values.put("nam_mat", new LunarCalendar.LunarDate(memorial.lunarYear, memorial.lunarMonth,
                    memorial.lunarDay, memorial.lunarLeap, 0).getYearName());
            if (!memorial.burialPlace.isEmpty()) values.put("an_tang_tai", memorial.burialPlace);
            // Người đứng khấn đã có trong gia phả nên vai vế tự tính, không cần nhập.
            String prayerTerm = Kinship.prayerTerm(FamilyStore.all(context),
                    FamilyStore.relations(context), FamilyStore.rootId(context), memorial.personId);
            if (!prayerTerm.isEmpty()) values.put("quan_he", prayerTerm);
        }
        return values;
    }

    private static List<Segment> segments(String raw, Map<String, String> values) {
        List<Segment> result = new ArrayList<>();
        Matcher matcher = MARKER.matcher(raw);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                result.add(new Segment(raw.substring(cursor, matcher.start()), null, true));
            }
            String field = matcher.group(1);
            String value = values.get(field);
            boolean filled = value != null && !value.trim().isEmpty();
            result.add(new Segment(filled ? value.trim() : BLANK, field, filled));
            cursor = matcher.end();
        }
        if (cursor < raw.length()) result.add(new Segment(raw.substring(cursor), null, true));
        return result;
    }

    /**
     * Toàn văn đã điền, có tô sáng, kèm số chỗ còn trống — một lần duyệt duy nhất.
     * focusField được tô đậm hơn để người dùng thấy trường mình đang cấu hình xuất hiện ở đâu.
     */
    public static Rendered render(Context context, String raw, Map<String, String> values, String focusField) {
        int filledColor = ContextCompat.getColor(context, R.color.prayerFilledBackground);
        int missingColor = ContextCompat.getColor(context, R.color.prayerMissingBackground);
        int focusColor = ContextCompat.getColor(context, R.color.prayerFocusBackground);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int missing = 0;
        for (Segment segment : segments(raw, values)) {
            if (segment.field == null) {
                builder.append(segment.text);
                continue;
            }
            int start = builder.length();
            builder.append(segment.text);
            int color;
            if (segment.field.equals(focusField)) {
                color = focusColor;
                builder.setSpan(new UnderlineSpan(), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (segment.filled) {
                color = filledColor;
            } else {
                color = missingColor;
                missing++;
            }
            builder.setSpan(new BackgroundColorSpan(color), start, builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return new Rendered(builder, missing);
    }
}
