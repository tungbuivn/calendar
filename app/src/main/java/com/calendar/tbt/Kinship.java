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
     * quan hệ nhập tay chỉ dùng cho cặp chưa nối (bạn của cha, con nuôi…).
     * Sáu tầng suy ra, theo thứ tự ưu tiên: huyết thống → hôn thú trực tiếp (vợ/chồng của
     * người trong gia phả) → họ hàng bên vợ/chồng → hai người cùng cưới vào một nhà →
     * thông gia (hai nhà cùng là cha mẹ của một đứa trẻ) → bề trên của nhà bạn đời.
     * Vợ chồng luôn chung một vai, nên tầng bốn bắc qua bạn đời của cả hai phía: em gái của
     * chồng là "em chồng" thì chồng của cô ấy là "em rể", không cần hai người quen nhau.
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
        Result affine = affinity(people, root, target, false);
        if (affine != null) return affine;
        Result spouseSide = viaSpouse(people, root, target);
        if (spouseSide != null) return spouseSide;
        // Cả hai đều là người cưới vào cùng một gia đình, không ai nối huyết thống với ai:
        // vai vế lấy theo cặp vợ chồng đứng trong gia phả.
        Result bothInLaws = affinity(people, root, target, true);
        if (bothInLaws != null) return bothInLaws;
        Result coInLaw = coInLaw(people, root, target);
        if (coInLaw != null) return coInLaw;
        // Bề trên trong nhà đẻ của người cưới vào: ông bà thông gia bên bác, bên chú…
        Result eldersOfInLaws = inLawElders(people, root, target);
        if (eldersOfInLaws != null) return eldersOfInLaws;
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
            String wanted = forward ? relation.aCallsB : relation.bCallsA;
            String other = forward ? relation.bCallsA : relation.aCallsB;
            String term = wanted == null ? "" : wanted.trim();
            // Chỉ mới nhập một chiều thì chiều còn lại lấy theo tag: A gọi B là "chú"
            // thì B tự hiểu là gọi lại "cháu".
            if (term.isEmpty()) term = KinshipTags.reciprocalOf(other).trim();
            if (term.isEmpty()) continue;
            int generation = relation.bGeneration == FamilyStore.GENERATION_UNKNOWN
                    ? KinshipTags.generationOf(term)
                    : (forward ? relation.bGeneration : -relation.bGeneration);
            String band = generation == FamilyStore.GENERATION_UNKNOWN
                    ? "Quan hệ nhập tay" : band(generation);
            return new Result(term, generation, band, "manual", "", 0, true, true);
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
            return new Result(elderTitle(a) + line, a, band(a), "ancestor", "", -1, true);
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
            return new Result(youngerTitle(b) + line, -b, band(-b), "descendant", "", 1, true);
        }

        if (a == 1 && b == 1) return sibling(people, root, target);

        if (a == 2 && b == 1) return uncle(people, match, root, target, generation);

        if (a == 1 && b == 2) {
            return new Result(gender(target, "cháu trai", "cháu gái", "cháu"), generation,
                    band(generation), "nephew", "", 1, true);
        }

        if (a == 2 && b == 2) return cousin(people, match, root, target, generation);

        // Xa hơn nữa thì vẫn gọi theo hàng thế hệ, không mô tả vòng qua người nối:
        // ngang hàng ông bà thì là "ông/bà", ngang hàng cha mẹ thì là "bác/chú/cô/cậu/dì",
        // ngang hàng với mình thì là "anh/chị/em họ". "nội/ngoại" chỉ dành cho người sinh ra
        // bố mẹ hoặc ông bà mình; người cùng hàng khác thì phải nói rõ "bên nội/bên ngoại".
        boolean paternal = isMale(people, firstStep(match.rootIds, root.id));
        String side = paternal ? " bên nội" : " bên ngoại";
        if (generation >= 3) {
            return new Result(elderTitle(generation) + side, generation, band(generation),
                    "ancestor", "", -1, true);
        }
        if (generation == 2) {
            return new Result(gender(target, "ông", "bà", "ông/bà") + side, generation,
                    band(generation), "grandparent", "", -1, true);
        }
        if (generation == 1) return uncle(people, match, root, target, generation);
        if (generation == 0) return cousin(people, match, root, target, generation);
        if (generation == -1) {
            return new Result(gender(target, "cháu trai", "cháu gái", "cháu"), generation,
                    band(generation), "nephew", "", 1, true);
        }
        String term = generation == -2 ? gender(target, "cháu trai", "cháu gái", "cháu") + side
                : youngerTitle(-generation) + side;
        return new Result(term, generation, band(generation),
                generation == -2 ? "grandchild" : "descendant", "", 1, true);
    }

    /** Bác/chú/cô bên nội, bác/cậu/dì bên ngoại — trên dưới so với bố mẹ của mốc. */
    private static Result uncle(List<Person> people, Match match, Person root, Person target,
                               int generation) {
        Person connector = FamilyStore.find(people, firstStep(match.rootIds, root.id));
        if (connector == null) return null;
        int order = seniority(people, target, connector);
        // Anh chị em của bố hay của mẹ đều chia trên dưới như nhau: người trên bậc bố mẹ
        // thì gọi là "bác", người dưới mới là "chú/cô" bên nội và "cậu/dì" bên ngoại.
        boolean paternal = !connector.isFemale();
        String juniorMale = paternal ? "chú" : "cậu";
        String juniorFemale = paternal ? "cô" : "dì";
        String term;
        String sub;
        if (target.isMale()) {
            term = order < 0 ? "bác" : order > 0 ? juniorMale : "bác/" + juniorMale;
            sub = order > 0 ? (paternal ? "chu" : "cau") : "bac";
        } else if (target.isFemale()) {
            term = order < 0 ? "bác" : order > 0 ? juniorFemale : "bác/" + juniorFemale;
            sub = order > 0 ? (paternal ? "co" : "di") : "bac";
        } else {
            term = "bác/" + juniorMale + "/" + juniorFemale;
            sub = "bac";
        }
        return new Result(term, generation, band(generation), "uncle", sub, order, true);
    }

    /** Anh/chị/em họ: vai lấy từ hàng bố mẹ, không lấy từ tuổi của chính hai người. */
    private static Result cousin(List<Person> people, Match match, Person root, Person target,
                                int generation) {
        int order = parentSeniority(people, match);
        if (order == 0) order = seniority(people, target, root);
        String term;
        if (order < 0) term = target.isFemale() ? "chị họ" : "anh họ";
        else if (order > 0) term = "em họ";
        else term = target.isMale() ? "anh/em họ" : target.isFemale() ? "chị/em họ" : "anh/chị/em họ";
        return new Result(term, generation, band(generation), "cousin", "", order, true);
    }

    /** Cách gọi người trên bậc mốc: cụ, kỵ, rồi chung là cụ tổ cho các bậc xa hơn. */
    private static String elderTitle(int steps) {
        return steps == 3 ? "cụ" : steps == 4 ? "kỵ" : "cụ tổ";
    }

    /** Cách gọi người dưới bậc mốc: chắt, chút, chít rồi cháu đời thứ n. */
    private static String youngerTitle(int steps) {
        return steps == 3 ? "chắt" : steps == 4 ? "chút" : steps == 5 ? "chít" : "cháu đời thứ " + steps;
    }

    private static Result sibling(List<Person> people, Person root, Person target) {
        int order = seniority(people, target, root);
        String term;
        if (target.isMale()) term = order < 0 ? "anh trai" : order > 0 ? "em trai" : "anh/em trai";
        else if (target.isFemale()) term = order < 0 ? "chị gái" : order > 0 ? "em gái" : "chị/em gái";
        else term = order < 0 ? "anh/chị" : order > 0 ? "em" : "anh/chị/em";
        return new Result(term, 0, band(0), "sibling", "", order, true);
    }

    // ---- hôn thú -----------------------------------------------------------

    /**
     * Vợ/chồng của một người trong gia phả thì mang vai vế của người đó.
     * {@code throughOwnSpouse} bật ở lượt cuối, khi bản thân mốc cũng là người cưới vào:
     * lúc đó vai của người nối lấy từ bạn đời của mốc vì hai vợ chồng chung một hàng.
     */
    private static Result affinity(List<Person> people, Person root, Person target,
                                   boolean throughOwnSpouse) {
        for (Person anchor : people) {
            if (anchor.id.equals(target.id)) continue;
            if (!isSpouseOf(anchor, target)) continue;
            if (anchor.id.equals(root.id)) {
                return new Result(gender(target, "chồng", "vợ", "vợ/chồng"), 0, band(0), "spouse",
                        "", 0, true);
            }
            Result anchorTerm = blood(people, root, anchor);
            if (anchorTerm == null && throughOwnSpouse) {
                Person rootSpouse = FamilyStore.find(people, root.spouseId);
                if (rootSpouse != null) anchorTerm = blood(people, rootSpouse, anchor);
            }
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
        if ("parent".equals(kind)) {
            // Vợ/chồng mới của bố mẹ đẻ thì thành bố dượng/mẹ kế.
            if (anchor.isMale() && target.isFemale()) return affine("mẹ kế", anchorTerm);
            if (anchor.isFemale() && target.isMale()) return affine("bố dượng", anchorTerm);
            return null;
        }
        if ("grandparent".equals(kind)) {
            // Vợ/chồng của ông bà ngồi cùng hàng ông bà, chỉ khác giới tính người gọi.
            return affine(gender(target, "ông", "bà", "ông/bà") + lineOf(anchorTerm.term), anchorTerm);
        }
        if ("ancestor".equals(kind)) {
            // "cụ", "kỵ" không chia giống nên hai vợ chồng dùng cùng một cách gọi.
            return affine(anchorTerm.term, anchorTerm);
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
        if ("grandchild".equals(kind) || "nephew".equals(kind) || "descendant".equals(kind)) {
            if (anchor.isMale() && target.isFemale()) return affine("cháu dâu", anchorTerm);
            if (anchor.isFemale() && target.isMale()) return affine("cháu rể", anchorTerm);
            return null;
        }
        if ("uncle".equals(kind)) {
            // Vai của người bạn đời giữ nguyên nên chỉ cần đổi sang cách gọi khác giới:
            // vợ của chú là "thím" nhưng chồng của cô lại là "chú", chồng của dì mới là "dượng".
            String sub = anchorTerm.sub;
            if (anchor.isMale()) {
                if (!target.isFemale()) return null;
                if ("cau".equals(sub)) return affine("mợ", anchorTerm);
                if ("chu".equals(sub)) return affine("thím", anchorTerm);
                return affine("bác gái", anchorTerm);
            }
            if (anchor.isFemale() && target.isMale()) {
                if ("co".equals(sub)) return affine("chú", anchorTerm);
                if ("bac".equals(sub)) return affine("bác", anchorTerm);
                if ("di".equals(sub)) return affine("dượng", anchorTerm);
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

    /**
     * Nhánh đứng sau một cách gọi, để vợ/chồng cùng hàng mang cùng nhánh.
     * "bên nội/bên ngoại" phải thử trước, nếu không "ông bên ngoại" sẽ bị cắt thành
     * "ông ngoại" — trong khi "ngoại" chỉ dành cho người sinh ra bố hoặc mẹ mình.
     */
    private static String lineOf(String term) {
        for (String suffix : new String[]{" bên nội", " bên ngoại", " nội", " ngoại"}) {
            if (term.endsWith(suffix)) return suffix;
        }
        return "";
    }

    private static boolean isSpouseOf(Person a, Person b) {
        return a.spouseId.equals(b.id) || b.spouseId.equals(a.id);
    }

    // ---- họ hàng bên vợ chồng ----------------------------------------------

    /**
     * Lấy vợ/chồng làm cầu: anh chị em ruột của vợ thì mình gọi là "anh vợ",
     * cha mẹ của chồng thì gọi là "bố chồng/mẹ chồng". Vợ chồng ngang hàng nên
     * bậc của người kia giữ nguyên như bậc tính từ người bạn đời.
     */
    private static Result viaSpouse(List<Person> people, Person root, Person target) {
        Person spouse = FamilyStore.find(people, root.spouseId);
        if (spouse == null || spouse.id.equals(target.id)) return null;
        Result anchor = blood(people, spouse, target);
        if (anchor == null) return null;
        String side = root.isFemale() ? "chồng" : root.isMale() ? "vợ" : "vợ/chồng";
        String kind = anchor.kind;
        String term;
        if ("parent".equals(kind)) {
            term = gender(target, "bố " + side, "mẹ " + side, "bố/mẹ " + side);
        } else if ("sibling".equals(kind)) {
            String own = anchor.seniority < 0 ? (target.isFemale() ? "chị" : "anh")
                    : anchor.seniority > 0 ? "em" : (target.isFemale() ? "chị/em" : "anh/em");
            term = own + " " + side;
        } else if ("child".equals(kind)) {
            // Con riêng của bạn đời: mình là mẹ kế/bố dượng, không phải cha mẹ đẻ.
            term = "con riêng";
        } else {
            term = anchor.term + " bên " + side;
        }
        return new Result(term, anchor.generation, band(anchor.generation), "inlaw",
                anchor.sub, anchor.seniority, true);
    }

    // ---- thông gia ---------------------------------------------------------

    /**
     * Hai người cùng là cha mẹ của một đứa trẻ — tính cả cha mẹ đẻ của vợ/chồng đứa trẻ —
     * mà không phải vợ chồng nhau thì là thông gia. Vì xét theo cả cặp nên một người
     * đã là thông gia thì bạn đời của người đó cũng là thông gia với cả hai bên nhà kia.
     */
    private static Result coInLaw(List<Person> people, Person root, Person target) {
        if (isSpouseOf(root, target)) return null;
        for (Person child : people) {
            if (child.id.equals(root.id) || child.id.equals(target.id)) continue;
            boolean rootReal = isRealParent(root, child);
            boolean targetReal = isRealParent(target, child);
            // Cả hai cùng đẻ ra đứa trẻ thì là cha mẹ của nhau, không phải thông gia.
            if (rootReal == targetReal) continue;
            boolean link = rootReal ? isRealParent(target, spouseOf(people, child))
                    : isRealParent(root, spouseOf(people, child));
            if (!link) continue;
            String term = gender(target, "ông thông gia", "bà thông gia", "thông gia");
            return new Result(term, 0, band(0), "inlaw", "", 0, true);
        }
        return null;
    }

    /**
     * Bề trên trong nhà đẻ của một người cưới vào gia phả. Vợ chồng chung một hàng nên
     * bậc của người trên tính bằng bậc của người nối trừ nhà mình cộng với bậc trong nhà
     * người ấy; nhánh thì giữ theo vai của người nối — "ông bên bác", "bà bên chú" —
     * vì "nội/ngoại" chỉ dành cho người sinh ra bố hoặc mẹ của mốc.
     */
    private static Result inLawElders(List<Person> people, Person root, Person target) {
        for (Person bridge : people) {
            if (bridge.id.equals(root.id) || bridge.id.equals(target.id)) continue;
            Person kin = FamilyStore.find(people, bridge.spouseId);
            if (kin == null || kin.id.equals(root.id) || kin.id.equals(target.id)) continue;
            Result kinTerm = blood(people, root, kin);
            if (kinTerm == null) continue;
            Result up = blood(people, bridge, target);
            if (up == null || up.generation <= 0) continue;
            int generation = kinTerm.generation + up.generation;
            // Chỉ nhận hàng ông bà trở lên; các hàng khác chưa có cách gọi gọn thì để trống.
            if (generation < 2) continue;
            String side = " bên " + firstWord(kinTerm.term);
            String term = generation == 2
                    ? gender(target, "ông", "bà", "ông/bà") + side
                    : elderTitle(generation) + side;
            return new Result(term, generation, band(generation),
                    generation == 2 ? "grandparent" : "ancestor", "", -1, true);
        }
        return null;
    }

    /** Chữ đầu của một cách gọi, đủ để chỉ nhánh: "bác gái" → "bác". */
    private static String firstWord(String term) {
        int space = term.indexOf(' ');
        return space < 0 ? term : term.substring(0, space);
    }

    private static boolean isRealParent(Person parent, Person child) {
        return child != null && (parent.id.equals(child.fatherId) || parent.id.equals(child.motherId));
    }

    private static Person spouseOf(List<Person> people, Person person) {
        return person == null ? null : FamilyStore.find(people, person.spouseId);
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

    /**
     * Trên dưới của hai người nối ở hàng bố mẹ: -1 khi bố/mẹ của người được gọi là
     * anh chị của bố/mẹ mốc, 1 khi là em, 0 khi gia phả chưa cho biết.
     */
    private static int parentSeniority(List<Person> people, Match match) {
        if (match.rootIds.isEmpty() || match.targetIds.isEmpty()) return 0;
        Person rootParent = FamilyStore.find(people, match.rootIds.get(0));
        Person targetParent = FamilyStore.find(people, match.targetIds.get(0));
        if (rootParent == null || targetParent == null) return 0;
        return seniority(people, targetParent, rootParent);
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
