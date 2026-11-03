package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Gia phả: mỗi người lưu tên, giới tính, năm sinh, còn sống hay đã khuất
 * và ba mối nối cấu trúc (cha, mẹ, vợ hoặc chồng). Quan hệ hiển thị không nhập tay mà suy ra
 * từ các nối này, nhờ vậy đổi người làm mốc là cả nhà tính lại được.
 */
public final class FamilyStore {

    public static final int MALE = 1;
    public static final int FEMALE = 2;
    public static final int UNKNOWN = 0;

    private static final String PREFS = "family_tree";
    private static final String KEY_PEOPLE = "people";
    private static final String KEY_ROOT = "root_id";
    private static final String KEY_RELATIONS = "relations";
    private static final String KEY_TAGS = "relation_tags";

    /** Chưa rõ người kia cách mấy bậc — sơ đồ sẽ xếp vào hàng "Quan hệ nhập tay". */
    public static final int GENERATION_UNKNOWN = Integer.MIN_VALUE;

    public static final class Person {
        public final String id;
        public String name;
        public int gender;
        public int birthYear;
        /** Người đã khuất vẽ nền xám, phân biệt với người còn sống trong sơ đồ. */
        public boolean deceased;
        public String fatherId;
        public String motherId;
        public String spouseId;

        Person(String id, String name, int gender, int birthYear, boolean deceased,
               String fatherId, String motherId, String spouseId) {
            this.id = id == null || id.isEmpty() ? UUID.randomUUID().toString() : id;
            this.name = name == null ? "" : name.trim();
            this.gender = gender;
            this.birthYear = birthYear;
            this.deceased = deceased;
            this.fatherId = clean(fatherId);
            this.motherId = clean(motherId);
            this.spouseId = clean(spouseId);
        }

        /** Chữ cái đầu của tên (tiếng Việt đã bỏ dấu) để vẽ avatar. */
        public String initial() {
            String[] parts = name.trim().split("\\s+");
            String word = parts.length == 0 ? "" : parts[parts.length - 1];
            String folded = PrayerCatalog.fold(word);
            return folded.isEmpty() ? "?" : folded.substring(0, 1).toUpperCase(Locale.ROOT);
        }

        public boolean isMale() {
            return gender == MALE;
        }

        public boolean isFemale() {
            return gender == FEMALE;
        }

        /** Với một người chưa khai giới tính thì suy ra từ vai vế đang gán. */
        public int genderOr(int fallback) {
            return gender == UNKNOWN ? fallback : gender;
        }
    }

    /**
     * Quan hệ nhập tay giữa đúng hai người, chỉ dùng khi gia phả chưa nối được
     * (ví dụ thông gia, con nuôi). Cách xưng hô ghi theo cả hai chiều vì tiếng Việt
     * không đối xứng: A gọi B là "thông gia", B gọi A là "cha của con dâu".
     */
    public static final class Relation {
        public final String id;
        public String aId;
        public String bId;
        public String aCallsB;
        public String bCallsA;
        /** Số bậc của B so với A: dương là B trên A, âm là B dưới A. */
        public int bGeneration;

        Relation(String id, String aId, String bId, String aCallsB, String bCallsA, int bGeneration) {
            this.id = id == null || id.isEmpty() ? UUID.randomUUID().toString() : id;
            this.aId = clean(aId);
            this.bId = clean(bId);
            this.aCallsB = aCallsB == null ? "" : aCallsB.trim();
            this.bCallsA = bCallsA == null ? "" : bCallsA.trim();
            this.bGeneration = bGeneration;
        }
    }

    /**
     * Tag xưng hô mà người dùng tự đặt, giữ lại giữa các lần nhập để không phải
     * gõ lại và để ô nhập gợi ý những cách gọi nhà đang dùng.
     * Trùng nhau theo {@link KinshipTags#normalize} nên "Chú" và "chú " chỉ là một.
     */
    public static final class Tag {
        public String term;
        public String reciprocal;
        public int generation;

        Tag(String term, String reciprocal, int generation) {
            this.term = term == null ? "" : term.trim();
            this.reciprocal = reciprocal == null ? "" : reciprocal.trim();
            this.generation = generation;
        }
    }

    private FamilyStore() {
    }

    public static List<Person> all(Context context) {
        List<Person> result = new ArrayList<>();
        String raw = prefs(context).getString(KEY_PEOPLE, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json == null) continue;
                String name = json.optString("name", "").trim();
                if (name.isEmpty()) continue;
                result.add(new Person(json.optString("id"), name,
                        json.optInt("gender", UNKNOWN), json.optInt("birthYear", 0),
                        json.optBoolean("deceased", false),
                        json.optString("fatherId", ""), json.optString("motherId", ""),
                        json.optString("spouseId", "")));
            }
        } catch (JSONException e) {
            // Danh mục hỏng thì coi như chưa có gia phả.
        }
        return result;
    }

    public static Person find(Context context, String id) {
        return find(all(context), id);
    }

    public static Person find(List<Person> people, String id) {
        if (id == null || id.isEmpty()) return null;
        for (Person person : people) {
            if (person.id.equals(id)) return person;
        }
        return null;
    }

    public static Person byName(List<Person> people, String name) {
        for (Person person : people) {
            if (person.name.equalsIgnoreCase(name)) return person;
        }
        return null;
    }

    public static List<Person> childrenOf(List<Person> people, String parentId) {
        List<Person> result = new ArrayList<>();
        if (parentId == null || parentId.isEmpty()) return result;
        for (Person person : people) {
            if (parentId.equals(person.fatherId) || parentId.equals(person.motherId)) result.add(person);
        }
        return result;
    }

    /**
     * Thứ tự sinh trong số các anh chị em cùng cha mẹ hoặc cùng cha (cùng mẹ).
     * Trả -1 khi không xác định được, khi đó bảng quan hệ sẽ dùng dạng gộp "bác/chú".
     */
    public static int birthRank(List<Person> people, Person person) {
        if (person == null) return -1;
        List<Person> siblings = siblingsOf(people, person);
        return siblings.indexOf(person);
    }

    public static List<Person> siblingsOf(List<Person> people, Person person) {
        List<Person> result = new ArrayList<>();
        if (person == null) return result;
        for (Person candidate : people) {
            if (candidate.id.equals(person.id)) continue;
            boolean sameFather = !person.fatherId.isEmpty() && person.fatherId.equals(candidate.fatherId);
            boolean sameMother = !person.motherId.isEmpty() && person.motherId.equals(candidate.motherId);
            if (sameFather || sameMother) result.add(candidate);
        }
        return result;
    }

    /** Người đang đứng khấn: màn gia phả lấy người này làm mốc để tính mọi vai vế. */
    public static String rootId(Context context) {
        List<Person> people = all(context);
        return rootId(context, people);
    }

    public static String rootId(Context context, List<Person> people) {
        String stored = prefs(context).getString(KEY_ROOT, "");
        for (Person person : people) {
            if (person.id.equals(stored)) return stored;
        }
        return people.isEmpty() ? "" : people.get(0).id;
    }

    public static void setRootId(Context context, String id) {
        prefs(context).edit().putString(KEY_ROOT, clean(id)).apply();
    }

    /** Thêm hoặc thay thế một người; quan hệ vợ chồng luôn được ghi cả hai phía. */
    public static void save(Context context, Person person) {
        List<Person> people = all(context);
        int index = indexOf(people, person.id);
        if (index < 0) people.add(person);
        else people.set(index, person);
        mirrorSpouse(people, person);
        write(context, people);
    }

    /**
     * Gán vai vế cho một người mà không phá dữ liệu của người khác.
     * Trả về lời báo lỗi, hoặc null nếu hợp lệ.
     */
    public static String saveChecked(Context context, Person person) {
        List<Person> people = all(context);
        String problem = validate(people, person);
        if (problem != null) return problem;
        int index = indexOf(people, person.id);
        List<Person> updated = new ArrayList<>(people);
        if (index < 0) updated.add(person);
        else updated.set(index, person);
        mirrorSpouse(updated, person);
        if (createsCycle(updated, person)) return "Người này đang nằm trong vòng lặp quan hệ (cha mẹ của chính mình).";
        write(context, updated);
        return null;
    }

    public static void remove(Context context, String id) {
        List<Person> people = all(context);
        int index = indexOf(people, id);
        if (index < 0) return;
        people.remove(index);
        for (Person person : people) {
            if (id.equals(person.fatherId)) person.fatherId = "";
            if (id.equals(person.motherId)) person.motherId = "";
            if (id.equals(person.spouseId)) person.spouseId = "";
        }
        if (id.equals(prefs(context).getString(KEY_ROOT, ""))) {
            prefs(context).edit().remove(KEY_ROOT).apply();
        }
        write(context, people);
        List<Relation> kept = new ArrayList<>();
        for (Relation relation : relations(context)) {
            if (!id.equals(relation.aId) && !id.equals(relation.bId)) kept.add(relation);
        }
        writeRelations(context, kept);
    }

    // ---- quan hệ nhập tay --------------------------------------------------

    public static List<Relation> relations(Context context) {
        List<Relation> result = new ArrayList<>();
        String raw = prefs(context).getString(KEY_RELATIONS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json == null) continue;
                result.add(new Relation(json.optString("id"), json.optString("aId", ""),
                        json.optString("bId", ""), json.optString("aCallsB", ""),
                        json.optString("bCallsA", ""),
                        json.optInt("bGeneration", GENERATION_UNKNOWN)));
            }
        } catch (JSONException e) {
            // Danh mục hỏng thì coi như chưa có quan hệ nhập tay nào.
        }
        return result;
    }

    public static Relation findRelation(List<Relation> relations, String id) {
        for (Relation relation : relations) {
            if (relation.id.equals(id)) return relation;
        }
        return null;
    }

    /** Quan hệ nhập tay đã có cho cặp này chưa, không kể phần tử đang sửa. */
    public static Relation relationFor(List<Relation> relations, String firstId, String secondId,
                                       String exceptId) {
        for (Relation relation : relations) {
            if (relation.id.equals(exceptId)) continue;
            boolean same = firstId.equals(relation.aId) && secondId.equals(relation.bId);
            boolean flipped = firstId.equals(relation.bId) && secondId.equals(relation.aId);
            if (same || flipped) return relation;
        }
        return null;
    }

    public static void saveRelation(Context context, Relation relation) {
        List<Relation> relations = relations(context);
        int index = -1;
        for (int i = 0; i < relations.size(); i++) {
            if (relations.get(i).id.equals(relation.id)) index = i;
        }
        if (index < 0) relations.add(relation);
        else relations.set(index, relation);
        writeRelations(context, relations);
        rememberTerms(context, relation);
    }

    /** Cách gọi vừa ghi được bổ sung vào kho tag để lần sau chỉ cần chọn, khỏi gõ lại. */
    private static void rememberTerms(Context context, Relation relation) {
        saveTag(context, relation.aCallsB, relation.bCallsA, relation.bGeneration);
        saveTag(context, relation.bCallsA, relation.aCallsB, -relation.bGeneration);
    }

    public static void removeRelation(Context context, String id) {
        List<Relation> relations = relations(context);
        for (int i = 0; i < relations.size(); i++) {
            if (relations.get(i).id.equals(id)) {
                relations.remove(i);
                break;
            }
        }
        writeRelations(context, relations);
    }

    /** Trả về lời báo lỗi, hoặc null nếu quan hệ nhập tay hợp lệ. */
    public static String validateRelation(List<Person> people, List<Relation> relations,
                                          Relation relation) {
        if (relation.aId.isEmpty() || relation.bId.isEmpty()) return "Hãy chọn đủ hai người.";
        if (relation.aId.equals(relation.bId)) return "Hai phía phải là hai người khác nhau.";
        Person a = find(people, relation.aId);
        Person b = find(people, relation.bId);
        if (a == null || b == null) return "Một trong hai người không còn trong gia phả.";
        if (relation.aCallsB.isEmpty() && relation.bCallsA.isEmpty()) {
            return "Hãy nhập cách xưng hô ít nhất một chiều.";
        }
        Relation existing = relationFor(relations, relation.aId, relation.bId, relation.id);
        if (existing != null) {
            return "Cặp " + a.name + " – " + b.name + " đã có quan hệ nhập tay rồi — hãy sửa bản đó.";
        }
        return null;
    }

    // ---- tag xưng hô -------------------------------------------------------

    public static List<Tag> tags(Context context) {
        List<Tag> result = new ArrayList<>();
        String raw = prefs(context).getString(KEY_TAGS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json == null) continue;
                String term = json.optString("term", "").trim();
                if (term.isEmpty()) continue;
                result.add(new Tag(term, json.optString("reciprocal", ""),
                        json.optInt("generation", GENERATION_UNKNOWN)));
            }
        } catch (JSONException e) {
            // Danh mục hỏng thì chỉ mất phần gợi ý, quan hệ đã ghi vẫn còn.
        }
        return result;
    }

    /** Tag người dùng đã ghi cho cách gọi này, hoặc null khi chỉ có từ vựng chuẩn. */
    public static Tag findTag(List<Tag> tags, String term) {
        if (term == null || term.trim().isEmpty()) return null;
        for (Tag tag : tags) {
            if (KinshipTags.sameTerm(tag.term, term)) return tag;
        }
        return null;
    }

    /** Ghi tag mới hoặc cập nhật tag trùng; tag đã có trong từ vựng chuẩn thì bỏ qua. */
    public static void saveTag(Context context, String term, String reciprocal, int generation) {
        String trimmed = term == null ? "" : term.trim();
        if (trimmed.isEmpty()) return;
        List<Tag> stored = tags(context);
        if (findTag(stored, trimmed) != null) return;
        if (KinshipTags.match(trimmed) != null) return;
        stored.add(new Tag(trimmed, reciprocal, generation));
        writeTags(context, stored);
    }

    public static void removeTag(Context context, String term) {
        List<Tag> stored = tags(context);
        List<Tag> kept = new ArrayList<>();
        for (Tag tag : stored) {
            if (!KinshipTags.sameTerm(tag.term, term)) kept.add(tag);
        }
        writeTags(context, kept);
    }

    private static void writeTags(Context context, List<Tag> tags) {
        JSONArray array = new JSONArray();
        try {
            for (Tag tag : tags) {
                JSONObject json = new JSONObject();
                json.put("term", tag.term);
                json.put("reciprocal", tag.reciprocal);
                json.put("generation", tag.generation);
                array.put(json);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs(context).edit().putString(KEY_TAGS, array.toString()).apply();
    }

    private static void writeRelations(Context context, List<Relation> relations) {
        JSONArray array = new JSONArray();
        try {
            for (Relation relation : relations) {
                JSONObject json = new JSONObject();
                json.put("id", relation.id);
                json.put("aId", relation.aId);
                json.put("bId", relation.bId);
                json.put("aCallsB", relation.aCallsB);
                json.put("bCallsA", relation.bCallsA);
                json.put("bGeneration", relation.bGeneration);
                array.put(json);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs(context).edit().putString(KEY_RELATIONS, array.toString()).apply();
    }

    public static String validate(List<Person> people, Person person) {
        if (person.name.isEmpty()) return "Hãy nhập tên người.";
        if (person.id.equals(person.fatherId)) return "Người này không thể là cha mẹ của chính mình.";
        if (person.id.equals(person.motherId)) return "Người này không thể là cha mẹ của chính mình.";
        if (person.id.equals(person.spouseId)) return "Người này không thể kết hôn với chính mình.";
        String father = "Cha";
        String problem = checkParent(people, person, person.fatherId, MALE, father);
        if (problem != null) return problem;
        problem = checkParent(people, person, person.motherId, FEMALE, "Mẹ");
        if (problem != null) return problem;
        Person spouse = find(people, person.spouseId);
        if (spouse != null && person.gender != UNKNOWN && spouse.gender != UNKNOWN
                && person.gender == spouse.gender) {
            return "Vợ hoặc chồng phải khác giới tính với người này (đã khai cả hai).";
        }
        return null;
    }

    private static String checkParent(List<Person> people, Person person, String parentId,
                                      int expectedGender, String role) {
        Person parent = find(people, parentId);
        if (parent == null) return parentId == null || parentId.isEmpty()
                ? null : role + " đã chọn không còn trong gia phả.";
        if (parent.id.equals(person.id)) return "Người này không thể là " + role.toLowerCase(Locale.ROOT)
                + " của chính mình.";
        if (parent.gender != UNKNOWN && parent.gender != expectedGender) {
            return role + " \"" + parent.name + "\" đang khai giới tính khác — hãy sửa giới tính trước.";
        }
        return null;
    }

    /** Cha mẹ chồng lên nhau quay về chính người đó nghĩa là dữ liệu bị vòng. */
    private static boolean createsCycle(List<Person> people, Person person) {
        String currentId = person.fatherId;
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (currentId != null && !currentId.isEmpty() && seen.add(currentId)) {
            if (currentId.equals(person.id)) return true;
            Person parent = find(people, currentId);
            currentId = parent == null ? "" : parent.fatherId;
        }
        currentId = person.motherId;
        seen.clear();
        while (currentId != null && !currentId.isEmpty() && seen.add(currentId)) {
            if (currentId.equals(person.id)) return true;
            Person parent = find(people, currentId);
            currentId = parent == null ? "" : parent.motherId;
        }
        return false;
    }

    private static void mirrorSpouse(List<Person> people, Person person) {
        if (person.spouseId.isEmpty()) return;
        Person spouse = find(people, person.spouseId);
        if (spouse != null && !person.id.equals(spouse.spouseId)) spouse.spouseId = person.id;
        // Xoá nối cũ của phía bên kia khi người này đổi vợ/chồng.
        for (Person other : people) {
            if (!other.id.equals(person.id) && !other.id.equals(person.spouseId)
                    && person.id.equals(other.spouseId)) {
                other.spouseId = "";
            }
        }
    }

    private static int indexOf(List<Person> people, String id) {
        for (int i = 0; i < people.size(); i++) {
            if (people.get(i).id.equals(id)) return i;
        }
        return -1;
    }

    private static String clean(String id) {
        return id == null ? "" : id.trim();
    }

    /** Thay toàn bộ gia phả (dùng cho dữ liệu mẫu); xóa quan hệ nhập tay cũ. */
    public static void replaceAll(Context context, List<Person> people, String rootId) {
        write(context, people);
        prefs(context).edit()
                .putString(KEY_RELATIONS, "[]")
                .putString(KEY_ROOT, clean(rootId))
                .apply();
    }

    private static void write(Context context, List<Person> people) {
        JSONArray array = new JSONArray();
        try {
            for (Person person : people) {
                JSONObject json = new JSONObject();
                json.put("id", person.id);
                json.put("name", person.name);
                json.put("gender", person.gender);
                json.put("birthYear", person.birthYear);
                json.put("deceased", person.deceased);
                json.put("fatherId", person.fatherId);
                json.put("motherId", person.motherId);
                json.put("spouseId", person.spouseId);
                array.put(json);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs(context).edit().putString(KEY_PEOPLE, array.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
