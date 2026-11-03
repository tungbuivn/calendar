package com.calendar.tbt;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kho từ vựng xưng hô dùng chung cho mọi quan hệ nhập tay. Một tag là cách gọi
 * (A gọi B) kèm theo chiều ngược lại và số bậc của B so với A, nhờ vậy gõ một chiều
 * là suy ra được chiều kia, và hai người viết khác nhau không tạo ra tag trùng nghĩa:
 * "chú", "Chú", "ông chú" đều quy về một.
 *
 * <p>So khớp bỏ dấu thanh và khoảng trắng nên chỉ cần {@link #normalize} là đủ.
 */
public final class KinshipTags {

    /** Cách gọi, cách gọi lại và bậc của người được gọi so với người gọi. */
    public static final class Tag {
        public final String term;
        /** rỗng khi chiều ngược lại còn tuỳ giới tính người nói, phải nhập tay. */
        public final String reciprocal;
        /** dương là bậc trên, âm là bậc dưới, 0 ngang hàng, {@link FamilyStore#GENERATION_UNKNOWN} chưa rõ. */
        public final int generation;

        Tag(String term, String reciprocal, int generation) {
            this.term = term;
            this.reciprocal = reciprocal;
            this.generation = generation;
        }
    }

    /** {cách gọi, gọi lại, bậc} — bậc của người được gọi so với người gọi. */
    private static final String[][] VOCABULARY = {
            {"cha", "con", "1"},
            {"mẹ", "con", "1"},
            {"bố", "con", "1"},
            {"cha nuôi", "con nuôi", "1"},
            {"mẹ nuôi", "con nuôi", "1"},
            {"ông nội", "cháu nội", "2"},
            {"bà nội", "cháu nội", "2"},
            {"ông ngoại", "cháu ngoại", "2"},
            {"bà ngoại", "cháu ngoại", "2"},
            {"cụ nội", "chắt nội", "3"},
            {"cụ ngoại", "chắt ngoại", "3"},
            {"bác", "cháu", "1"},
            {"chú", "cháu", "1"},
            {"cô", "cháu", "1"},
            {"cậu", "cháu", "1"},
            {"dì", "cháu", "1"},
            {"thím", "bố chồng", "-1"},
            {"mợ", "bố vợ", "-1"},
            {"dượng", "bố vợ", "-1"},
            {"bố dượng", "con riêng", "1"},
            {"mẹ kế", "con riêng", "1"},
            {"anh trai", "em trai", "0"},
            {"chị gái", "em gái", "0"},
            {"em trai", "anh trai", "0"},
            {"em gái", "chị gái", "0"},
            {"anh", "em", "0"},
            {"chị", "em", "0"},
            {"em", "anh/chị", "0"},
            {"vợ", "chồng", "0"},
            {"chồng", "vợ", "0"},
            {"con", "cha/mẹ", "-1"},
            {"con trai", "cha/mẹ", "-1"},
            {"con gái", "cha/mẹ", "-1"},
            {"con dâu", "bố chồng/mẹ chồng", "-1"},
            {"con rể", "bố vợ/mẹ vợ", "-1"},
            {"cháu", "ông/bà", "-1"},
            {"cháu nội", "ông/bà nội", "-1"},
            {"cháu ngoại", "ông/bà ngoại", "-1"},
            {"chắt", "cụ", "-2"},
            {"chút", "cụ", "-3"},
            {"chít", "cụ", "-4"},
            {"anh rể", "em vợ", "0"},
            {"chị dâu", "em chồng", "0"},
            {"em rể", "anh vợ", "0"},
            {"em dâu", "chị chồng", "0"},
            {"anh vợ", "em rể", "0"},
            {"chị vợ", "em rể", "0"},
            {"em vợ", "anh rể", "0"},
            {"anh chồng", "em dâu", "0"},
            {"chị chồng", "em dâu", "0"},
            {"em chồng", "chị dâu", "0"},
            {"bố vợ", "con rể", "1"},
            {"mẹ vợ", "con rể", "1"},
            {"bố chồng", "con dâu", "1"},
            {"mẹ chồng", "con dâu", "1"},
            {"ông thông gia", "ông thông gia", "0"},
            {"bà thông gia", "bà thông gia", "0"},
            {"thông gia", "thông gia", "0"},
            {"anh họ", "em họ", "0"},
            {"chị họ", "em họ", "0"},
            {"em họ", "anh/chị họ", "0"},
    };

    private static volatile Map<String, Tag> index;

    private KinshipTags() {
    }

    /**
     * Bỏ dấu thanh và chữ hoa, gom khoảng trắng — hai cách viết cùng nghĩa khớp thành một tag.
     * Không dùng PrayerCatalog.fold: lớp đó khởi tạo LruCache của Android nên không chạy được
     * trong unit test thuần JVM.
     */
    public static String normalize(String term) {
        if (term == null) return "";
        String stripped = Normalizer.normalize(term, Normalizer.Form.NFD).replaceAll("\\p{Mn}", "");
        String folded = stripped.replace('đ', 'd').replace('Đ', 'd').toLowerCase(Locale.ROOT);
        return folded.replaceAll("\\s+", " ").trim();
    }

    private static List<Tag> builtIns() {
        return new ArrayList<>(index().values());
    }

    /**
     * Tag của từ vựng chuẩn cho cách gọi này, hoặc null khi chưa biết.
     * Chỉ khớp nguyên từ đã bỏ dấu để suy ra chiều ngược lại chắc chắn đúng;
     * cách gọi gần trùng ("ông chú") được bắt bằng {@link #suggest} lúc nhập.
     */
    public static Tag match(String term) {
        String key = normalize(term);
        return key.isEmpty() ? null : index().get(key);
    }

    /** Cách gọi lại theo từ vựng chuẩn, hoặc rỗng khi chưa biết từ này. */
    public static String reciprocalOf(String term) {
        Tag tag = match(term);
        return tag == null ? "" : tag.reciprocal;
    }

    /** Bậc của người được gọi so với người gọi, hoặc {@link FamilyStore#GENERATION_UNKNOWN}. */
    public static int generationOf(String term) {
        Tag tag = match(term);
        return tag == null ? FamilyStore.GENERATION_UNKNOWN : tag.generation;
    }

    /**
     * Danh sách cho ô nhập: tag người dùng đã ghi xếp trước vì đó là cách gọi nhà đang dùng,
     * kế đến là từ vựng chuẩn. Trùng sau khi bỏ dấu chỉ giữ một.
     */
    public static List<String> vocabulary(List<String> userTerms) {
        List<String> result = new ArrayList<>();
        appendAll(result, userTerms);
        for (Tag tag : builtIns()) appendAll(result, Collections.singletonList(tag.term));
        return result;
    }

    /**
     * Gợi ý cho phần đang gõ: tag trùng trước, rồi đến tag có chứa hoặc được chứa.
     * Trả về rỗng khi chuỗi gõ quá ngắn để khỏi gợi ý cả kho từ.
     */
    public static List<String> suggest(String query, List<String> userTerms, int limit) {
        List<String> result = new ArrayList<>();
        String needle = normalize(query);
        if (needle.length() < 2) return result;
        List<String> partial = new ArrayList<>();
        for (String term : vocabulary(userTerms)) {
            String key = normalize(term);
            if (key.isEmpty()) continue;
            if (key.equals(needle)) {
                if (!result.contains(term)) result.add(term);
            } else if (key.contains(needle) || needle.contains(key)) {
                if (!partial.contains(term)) partial.add(term);
            }
            if (result.size() + partial.size() >= limit * 3) break;
        }
        for (String term : partial) {
            if (result.size() >= limit) break;
            result.add(term);
        }
        return result;
    }

    /** true khi hai cách gọi được coi là một tag. */
    public static boolean sameTerm(String first, String second) {
        String key = normalize(first);
        return !key.isEmpty() && key.equals(normalize(second));
    }

    private static void appendAll(List<String> target, List<String> terms) {
        if (terms == null) return;
        for (String term : terms) {
            if (term == null) continue;
            String trimmed = term.trim();
            if (!trimmed.isEmpty() && !containsTerm(target, trimmed)) target.add(trimmed);
        }
    }

    private static boolean containsTerm(List<String> terms, String term) {
        for (String existing : terms) {
            if (sameTerm(existing, term)) return true;
        }
        return false;
    }

    private static Map<String, Tag> index() {
        Map<String, Tag> local = index;
        if (local == null) {
            synchronized (KinshipTags.class) {
                local = index;
                if (local == null) {
                    Map<String, Tag> built = new LinkedHashMap<>();
                    for (String[] row : VOCABULARY) {
                        built.put(normalize(row[0]), new Tag(row[0], row[1], parseGeneration(row[2])));
                    }
                    index = local = built;
                }
            }
        }
        return local;
    }

    private static int parseGeneration(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return FamilyStore.GENERATION_UNKNOWN;
        }
    }
}
