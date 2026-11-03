package com.calendar.tbt;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bảng term hiển thị, đọc từ assets/kinship_terms.json.
 * Code chỉ dùng key; muốn đổi cách gọi thì sửa value trong JSON, không sửa Java.
 */
public final class KinshipTerms {

    private static final Pattern PAIR = Pattern.compile(
            "\"((?:\\\\.|[^\"\\\\])*)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private static final Map<String, String> VALUES = new HashMap<>();

    private KinshipTerms() {
    }

    public static void load(Context context) {
        try (InputStream input = context.getAssets().open("kinship_terms.json")) {
            load(input);
        } catch (IOException error) {
            // Giữ bảng cũ nếu có; app vẫn chạy bằng fallback key.
        }
    }

    public static void load(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            load(input);
        }
    }

    private static void load(InputStream input) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
        }
        synchronized (KinshipTerms.class) {
            VALUES.clear();
            Matcher matcher = PAIR.matcher(text.toString());
            while (matcher.find()) {
                VALUES.put(unescape(matcher.group(1)), unescape(matcher.group(2)));
            }
        }
    }

    public static String get(String key) {
        String value = VALUES.get(key);
        return value == null ? key : value;
    }

    /** Đổi term/band của kết quả theo bảng; key không có thì giữ nguyên. */
    static Kinship.Result localize(Kinship.Result result) {
        String term = get(result.term);
        String band = get(result.band);
        if (term.equals(result.term) && band.equals(result.band)) return result;
        return new Kinship.Result(term, result.generation, band, result.kind, result.sub,
                result.seniority, result.related, result.manual);
    }

    public static void clear() {
        synchronized (KinshipTerms.class) {
            VALUES.clear();
        }
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            if (next == 'n') out.append('\n');
            else if (next == 't') out.append('\t');
            else if (next == 'r') out.append('\r');
            else if (next == '"') out.append('"');
            else if (next == '\\') out.append('\\');
            else if (next == 'u' && i + 4 < text.length()) {
                try {
                    out.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
                    i += 4;
                } catch (NumberFormatException error) {
                    out.append("\\u");
                }
            } else {
                out.append(next);
            }
        }
        return out.toString();
    }
}
