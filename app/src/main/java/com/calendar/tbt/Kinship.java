package com.calendar.tbt;

import com.calendar.tbt.FamilyStore.Person;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Suy ra cách xưng hô tiếng Việt của một người so với người làm mốc.
 * Chỉ cần các nối cấu trúc (cha, mẹ, vợ chồng) nên đổi mốc là cả nhà tính lại:
 * hàng "Cha mẹ" khi nhìn từ ông bà nội sẽ thành hàng "Con".
 */
public final class Kinship {

    public static final class Result {
        /** "chú", "bà ngoại", "cháu nội"…; dạng gộp "bác/chú" khi chưa xác định trên dưới. */
        public final String term;
        /** Số bậc so với mốc: dương là trên, âm là dưới, 0 là cùng hàng. */
        public final int generation;
        /** Nhãn hàng thế hệ cố định: "Cụ", "Ông bà", "Cha mẹ", "Cùng hàng", "Con"… */
        public final String band;
        /** Dạng tắt để bảng hôn thú tra tiếp: self/parent/child/sibling/uncle/cousin/nephew… */
        public final String kind;
        /** bac/chu/co/cau/di — cần cho "mợ", "thím", "dượng". */
        public final String sub;
        /** -1 là trên bậc mốc, 1 là dưới bậc, 0 chưa rõ. */
        public final int seniority;
        public final boolean related;
        /** true khi vai vế lấy từ quan hệ nhập tay, không suy ra được từ gia phả. */
        public final boolean manual;

        Result(String term, int generation, String band, String kind, String sub,
               int seniority, boolean related) {
            this(term, generation, band, kind, sub, seniority, related, false);
        }

        Result(String term, int generation, String band, String kind, String sub,
               int seniority, boolean related, boolean manual) {
            this.term = term;
            this.generation = generation;
            this.band = band;
            this.kind = kind;
            this.sub = sub;
            this.seniority = seniority;
            this.related = related;
            this.manual = manual;
        }

        static Result none() {
            return new Result("chưa nối", 0, "Chưa xác định", "none", "", 0, false);
        }
    }

    private static final String[] BANDS = {
            "Cao tổ", "Cụ", "Ông bà", "Cha mẹ", "Cùng hàng", "Con", "Cháu", "Chắt", "Chút", "Chít"
    };

    private Kinship() {
    }

    public static Result of(List<Person> people, String rootId, String targetId) {
        return of(people, Collections.<FamilyStore.Relation>emptyList(), rootId, targetId);
    }

    /**
     * Vai vế của người kia nhìn từ mốc. Gia phả nối được thì luôn ưu tiên suy ra;
     * quan hệ nhập tay chỉ dùng cho cặp chưa nối (thông gia, con nuôi…).
     */
    public static Result of(List<Person> people, List<FamilyStore.Relation> relations,
                            String rootId, String targetId) {
        Person root = FamilyStore.find(people, rootId);
        Person target = FamilyStore.find(people, targetId);
        if (root == null || target == null) return Result.none();
        if (root.id.equals(target.id)) {
            return new Result("chính tôi", 0, band(0), "self", "", 0, true);
        }
        Result blood = blood(people, root, target);
        if (blood != null) return blood;
        Result affine = affinity(people, root, target);
        if (affine != null) return affine;
        Result manual = manual(relations, root, target);
        return manual != null ? manual : Result.none();
    }

    /** true nếu cặp này đã suy ra được từ cấu trúc gia phả — không cần nhập tay. */
    public static boolean derivable(List<Person> people, String firstId, String secondId) {
        Result result = of(people, firstId, secondId);
        return result.related;
    }

    /**
     * Cách người đứng khấn gọi người được cúng giỗ — "ông", "bà", "cụ"… để điền vào bài khấn.
     * Rỗng khi chưa nối được: gia phả chưa biết người này thì bài khấn để trống chỗ đó.
     */
    public static String prayerTerm(List<Person> people, List<FamilyStore.Relation> relations,
                                    String rootId, String personId) {
        if (personId == null || personId.isEmpty()) return "";
        Result result = of(people, relations, rootId, personId);
        if (!result.related || "self".equals(result.kind) || "none".equals(result.kind)) return "";
        return result.term;
    }

    /** Tên hàng thế hệ cố định, dùng làm tiêu đề mỗi dải trong sơ đồ. */
    public static String band(int generation) {
        int index = 4 - generation;
        if (index < 0) return "Trên cao tổ";
        if (index >= BANDS.length) return "Dưới cháu nhiều đời";
        return BANDS[index];
    }

    private static Result manual(List<FamilyStore.Relation> relations, Person root, Person target) {
        for (FamilyStore.Relation relation : relations) {
            boolean forward = root.id.equals(relation.aId) && target.id.equals(relation.bId);
            boolean backward = root.id.equals(relation.bId) && target.id.equals(relation.aId);
            if (!forward && !backward) continue;
            String term = forward ? relation.aCallsB : relation.bCallsA;
            if (term == null || term.trim().isEmpty()) continue;
            int generation = relation.bGeneration == FamilyStore.GENERATION_UNKNOWN
                    ? FamilyStore.GENERATION_UNKNOWN
                    : (forward ? relation.bGeneration : -relation.bGeneration);
            String band = generation == FamilyStore.GENERATION_UNKNOWN
                    ? "Quan hệ nhập tay" : band(generation);
            return new Result(term.trim(), generation, band, "manual", "", 0, true, true);
        }
        return null;
    }

    // ---- huyết thống -------------------------------------------------------

    private static Result blood(List<Person> people, Person root, Person target) {
        Match match = commonAncestor(people, root, target);
        if (match == null) return null;
        int a = match.toRoot;
        int b = match.toTarget;
        int generation = a - b;

        if (b == 0) {
            boolean paternal = isMale(people, firstStep(match.rootIds, root.id));
            String line = paternal ? " nội" : " ngoại";
            if (a == 1) {
                return new Result(gender(target, "cha", "mẹ", "cha/mẹ"), 1, band(1), "parent", "", -1, true);
            }
            if (a == 2) {
                return new Result(gender(target, "ông" + line, "bà" + line, "ông/bà" + line),
                        a, band(a), "grandparent", "", -1, true);
            }
            String title = a == 3 ? "cụ" : a == 4 ? "kỵ" : "cụ tổ";
            return new Result(title + line, a, band(a), "ancestor", "", -1, true);
        }

        if (a == 0) {
            // Qua con trai thì là nội, qua con gái thì là ngoại.
            List<String> down = match.targetIds;
            String connectorId = down.size() >= 2 ? down.get(down.size() - 2) : target.id;
            boolean paternal = isMale(people, connectorId);
            String line = paternal ? " nội" : " ngoại";
            if (b == 1) {
                return new Result(gender(target, "con trai", "con gái", "con"), -1, band(-1),
                        "child", "", 1, true);
            }
            if (b == 2) {
                return new Result(gender(target, "cháu trai" + line, "cháu gái" + line, "cháu" + line),
                        -2, band(-2), "grandchild", "", 1, true);
            }
            String term = b == 3 ? "chắt" + line : b == 4 ? "chút" + line : b == 5 ? "chít" + line
                    : "cháu đời thứ " + b + line;
            return new Result(term, -b, band(-b), "descendant", "", 1, true);
        }

        if (a == 1 && b == 1) return sibling(people, root, target);

        if (a == 2 && b == 1) {
            Person connector = FamilyStore.find(people, firstStep(match.rootIds, root.id));
            if (connector == null) return null;
            int order = seniority(people, target, connector);
            String term;
            String sub;
            if (!connector.isFemale()) {
                if (target.isMale()) {
                    term = order < 0 ? "bác" : order > 0 ? "chú" : "bác/chú";
                    sub = order > 0 ? "chu" : "bac";
                } else if (target.isFemale()) {
                    term = order < 0 ? "bác" : order > 0 ? "cô" : "bác/cô";
                    sub = order > 0 ? "co" : "bac";
                } else {
                    term = "bác/chú/cô";
                    sub = "bac";
                }
            } else {
                term = gender(target, "cậu", "dì", "cậu/dì");
                sub = target.isFemale() ? "di" : "cau";
            }
            return new Result(term, generation, band(generation), "uncle", sub, order, true);
        }

        if (a == 1 && b == 2) {
            return new Result(gender(target, "cháu trai", "cháu gái", "cháu"), generation,
                    band(generation), "nephew", "", 1, true);
        }

        if (a == 2 && b == 2) {
            int order = seniority(people, target, root);
            String term;
            if (order < 0) term = target.isFemale() ? "chị họ" : "anh họ";
            else if (order > 0) term = "em họ";
            else term = target.isMale() ? "anh/em họ" : target.isFemale() ? "chị/em họ" : "anh/chị/em họ";
            return new Result(term, generation, band(generation), "cousin", "", order, true);
        }

        // Vai vế xa: mô tả qua người nối gần nhất, ví dụ "con của cụ".
        boolean paternal = isMale(people, firstStep(match.rootIds, root.id));
        String upTerm = ancestorTerm(a, paternal);
        String down = b == 1 ? "con" : b == 2 ? "cháu" : b == 3 ? "chắt" : "cháu đời thứ " + b;
        return new Result(down + " của " + upTerm, generation, band(generation),
                generation >= 0 ? "relative" : "junior", "", generation >= 0 ? -1 : 1, true);
    }

    private static Result sibling(List<Person> people, Person root, Person target) {
        int order = seniority(people, target, root);
        String term;
        if (target.isMale()) term = order < 0 ? "anh trai" : order > 0 ? "em trai" : "anh/em trai";
        else if (target.isFemale()) term = order < 0 ? "chị gái" : order > 0 ? "em gái" : "chị/em gái";
        else term = order < 0 ? "anh/chị" : order > 0 ? "em" : "anh/chị/em";
        return new Result(term, 0, band(0), "sibling", "", order, true);
    }

    /** Từ "con của cụ" tới "cháu của cụ" cần vai vế của người nối, không cần Match. */
    private static String ancestorTerm(int steps, boolean paternal) {
        String line = paternal ? " nội" : " ngoại";
        if (steps == 1) return "cha/mẹ";
        if (steps == 2) return "ông bà" + line;
        if (steps == 3) return "cụ" + line;
        if (steps == 4) return "kỵ" + line;
        return "cụ tổ" + line;
    }

    // ---- hôn thú -----------------------------------------------------------

    private static Result affinity(List<Person> people, Person root, Person target) {
        for (Person anchor : people) {
            if (anchor.id.equals(target.id)) continue;
            if (!isSpouseOf(anchor, target)) continue;
            if (anchor.id.equals(root.id)) {
                return new Result(gender(target, "chồng", "vợ", "vợ/chồng"), 0, band(0), "spouse",
                        "", 0, true);
            }
            Result anchorTerm = blood(people, root, anchor);
            if (anchorTerm == null) continue;
            Result mapped = inLaw(target, anchor, anchorTerm);
            if (mapped != null) return mapped;
            String who = target.isMale() ? "chồng của " : target.isFemale() ? "vợ của " : "vợ/chồng của ";
            return new Result(who + anchorTerm.term, anchorTerm.generation, anchorTerm.band,
                    "inlaw", anchorTerm.sub, anchorTerm.seniority, true);
        }
        return null;
    }

    private static Result inLaw(Person target, Person anchor, Result anchorTerm) {
        String kind = anchorTerm.kind;
        if ("parent".equals(kind) || "grandparent".equals(kind) || "ancestor".equals(kind)) {
            if (anchor.isMale() && target.isFemale()) return affine("mẹ kế", anchorTerm);
            if (anchor.isFemale() && target.isMale()) return affine("bố dượng", anchorTerm);
            return null;
        }
        if ("sibling".equals(kind)) {
            boolean older = anchorTerm.seniority < 0;
            if (anchor.isMale() && target.isFemale()) {
                return affine(older ? "chị dâu" : "em dâu", anchorTerm);
            }
            if (anchor.isFemale() && target.isMale()) {
                return affine(older ? "anh rể" : "em rể", anchorTerm);
            }
            return null;
        }
        if ("child".equals(kind)) {
            if (anchor.isMale() && target.isFemale()) return affine("con dâu", anchorTerm);
            if (anchor.isFemale() && target.isMale()) return affine("con rể", anchorTerm);
            return null;
        }
        if ("grandchild".equals(kind)) {
            if (anchor.isMale() && target.isFemale()) return affine("cháu dâu", anchorTerm);
            if (anchor.isFemale() && target.isMale()) return affine("cháu rể", anchorTerm);
            return null;
        }
        if ("uncle".equals(kind)) {
            String sub = anchorTerm.sub;
            if (target.isFemale()) {
                if ("cau".equals(sub)) return affine("mợ", anchorTerm);
                if ("chu".equals(sub)) return affine("thím", anchorTerm);
                if ("bac".equals(sub)) return affine("bác gái", anchorTerm);
            }
            if (target.isMale() && ("co".equals(sub) || "di".equals(sub))) {
                return affine("dượng", anchorTerm);
            }
            return null;
        }
        return null;
    }

    /** Vai vế của người nối được giữ nguyên, chỉ đổi sang phía hôn thú. */
    private static Result affine(String term, Result anchorTerm) {
        return new Result(term, anchorTerm.generation, anchorTerm.band, "inlaw", anchorTerm.sub,
                anchorTerm.seniority, true);
    }

    private static boolean isSpouseOf(Person a, Person b) {
        return a.spouseId.equals(b.id) || b.spouseId.equals(a.id);
    }

    // ---- tiện ích ----------------------------------------------------------

    /** Người gần mốc nhất trên chuỗi nối (cha hoặc mẹ của mốc). */
    private static String firstStep(List<String> upIds, String fallbackId) {
        return upIds.isEmpty() ? fallbackId : upIds.get(0);
    }

    private static boolean isMale(List<Person> people, String id) {
        Person person = FamilyStore.find(people, id);
        return person == null || !person.isFemale();
    }

    /** -1 là trên (lớn tuổi hơn), 1 là dưới, 0 chưa xác định. */
    private static int seniority(List<Person> people, Person x, Person y) {
        if (x.birthYear > 0 && y.birthYear > 0 && x.birthYear != y.birthYear) {
            return x.birthYear < y.birthYear ? -1 : 1;
        }
        String shared = sharedParent(people, x, y);
        if (shared != null) {
            int rx = FamilyStore.childrenOf(people, shared).indexOf(x);
            int ry = FamilyStore.childrenOf(people, shared).indexOf(y);
            if (rx >= 0 && ry >= 0 && rx != ry) return rx < ry ? -1 : 1;
        }
        return 0;
    }

    private static String sharedParent(List<Person> people, Person x, Person y) {
        if (!x.fatherId.isEmpty() && x.fatherId.equals(y.fatherId)) return x.fatherId;
        if (!x.motherId.isEmpty() && x.motherId.equals(y.motherId)) return x.motherId;
        return null;
    }

    private static String gender(Person person, String male, String female, String neutral) {
        if (person.isMale()) return male;
        if (person.isFemale()) return female;
        return neutral;
    }

    // ---- tổ tiên chung gần nhất --------------------------------------------

    private static final class Chain {
        final int dist;
        final String topId;
        final List<String> ids; // từ cha/mẹ của người xuất phát lên tới topId

        Chain(String startId, int dist, List<String> ids) {
            this.dist = dist;
            this.ids = ids;
            this.topId = dist == 0 ? startId : ids.get(ids.size() - 1);
        }
    }

    private static final class Match {
        final int toRoot;
        final int toTarget;
        final List<String> rootIds;
        final List<String> targetIds;

        Match(int toRoot, int toTarget, List<String> rootIds, List<String> targetIds) {
            this.toRoot = toRoot;
            this.toTarget = toTarget;
            this.rootIds = rootIds;
            this.targetIds = targetIds;
        }
    }

    private static Match commonAncestor(List<Person> people, Person root, Person target) {
        List<Chain> rootChains = chainsOf(people, root);
        List<Chain> targetChains = chainsOf(people, target);
        Match best = null;
        int bestScore = Integer.MAX_VALUE;
        for (Chain rc : rootChains) {
            for (Chain tc : targetChains) {
                if (!rc.topId.equals(tc.topId)) continue;
                int score = rc.dist + tc.dist;
                if (score > 0 && (score < bestScore || (score == bestScore && rc.dist < best.toRoot))) {
                    bestScore = score;
                    best = new Match(rc.dist, tc.dist, rc.ids, tc.ids);
                }
            }
        }
        return best;
    }

    private static List<Chain> chainsOf(List<Person> people, Person start) {
        List<Chain> out = new ArrayList<>();
        Chain origin = new Chain(start.id, 0, new ArrayList<String>());
        out.add(origin);
        ArrayDeque<Chain> queue = new ArrayDeque<>();
        queue.add(origin);
        Set<String> visited = new HashSet<>();
        visited.add(start.id);
        while (!queue.isEmpty()) {
            Chain chain = queue.poll();
            Person top = FamilyStore.find(people, chain.topId);
            if (top == null) continue;
            Chain viaFather = extend(people, visited, chain, top.fatherId);
            if (viaFather != null) {
                out.add(viaFather);
                queue.add(viaFather);
            }
            Chain viaMother = extend(people, visited, chain, top.motherId);
            if (viaMother != null) {
                out.add(viaMother);
                queue.add(viaMother);
            }
        }
        return out;
    }

    private static Chain extend(List<Person> people, Set<String> visited, Chain chain, String parentId) {
        if (parentId == null || parentId.isEmpty() || !visited.add(parentId)) return null;
        if (FamilyStore.find(people, parentId) == null) return null;
        List<String> ids = new ArrayList<>(chain.ids);
        ids.add(parentId);
        return new Chain(chain.topId, chain.dist + 1, ids);
    }
}
