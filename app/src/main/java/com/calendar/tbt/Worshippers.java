package com.calendar.tbt;

import java.util.regex.Pattern;

/**
 * Một bài khấn có thể do một người, cũng có thể do hai vợ chồng hoặc cả gia đình đứng lễ.
 * Từ hai người trở lên thì phải xưng "chúng con", nên câu "tín chủ con là …" tự đổi thành
 * "tín chủ chúng con là …" ngay lúc điền họ tên — người dùng chỉ cần gõ nhiều tên, cách
 * nhau bằng dấu phẩy hoặc chữ "và".
 */
public final class Worshippers {

    /** Trường chứa họ tên người đứng lễ. */
    public static final String NAME_FIELD = "ho_ten";

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    /** Tách từng người: dấu phẩy, chấm phẩy, gạch chéo, xuống dòng, hoặc chữ "và", "cùng". */
    private static final Pattern SEPARATORS =
            Pattern.compile("[,;/\\n]|\\s+(?:và|cùng)\\s+", FLAGS);

    /** Chỉ đổi đúng câu này; "có con là …" vẫn nguyên vì đó là con cháu, không phải người khấn. */
    private static final Pattern LEAD_IN = Pattern.compile("(tín chủ) con là", FLAGS);

    private Worshippers() {
    }

    /** Bao nhiêu người đứng lễ; chưa điền thì tính một cho khỏi đổi xưng hô bừa. */
    public static int count(String names) {
        if (names == null) return 1;
        int total = 0;
        for (String part : SEPARATORS.split(names)) {
            if (!part.trim().isEmpty()) total++;
        }
        return Math.max(1, total);
    }

    /** Chốt số nhiều cho toàn văn; một người thì giữ nguyên từng chữ. */
    public static String agree(String raw, String names) {
        if (raw == null || count(names) < 2) return raw;
        return LEAD_IN.matcher(raw).replaceAll("$1 chúng con là");
    }
}
