package com.calendar.tbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.calendar.tbt.FamilyStore.Person;

import java.util.ArrayList;
import java.util.List;

/** Bảng xưng hô tiếng Việt, tính trên một gia phả mẫu đủ cả nội, ngoại và hôn thú. */
public class KinshipTest {

    private static final Person CU_O = person("cu_o", "Cụ Ông", FamilyStore.MALE, 1900);
    private static final Person CU_B = person("cu_b", "Cụ Bà", FamilyStore.FEMALE, 1903);
    private static final Person ONG_NOI = person("ong_noi", "Ông Nội", FamilyStore.MALE, 1930);
    private static final Person BA_NOI = person("ba_noi", "Bà Nội", FamilyStore.FEMALE, 1932);
    private static final Person BAC_GAI = person("bac_gai", "Bác Gái", FamilyStore.FEMALE, 1950);
    private static final Person CHU = person("chu", "Chú", FamilyStore.MALE, 1960);
    private static final Person THIM = person("thim", "Thím", FamilyStore.FEMALE, 1962);
    private static final Person CO = person("co", "Cô", FamilyStore.FEMALE, 1965);
    private static final Person NGOAI_O = person("ngoai_o", "Ông Ngoại", FamilyStore.MALE, 1929);
    private static final Person NGOAI_B = person("ngoai_b", "Bà Ngoại", FamilyStore.FEMALE, 1931);
    private static final Person CHA = person("cha", "Cha", FamilyStore.MALE, 1956);
    private static final Person ME = person("me", "Mẹ", FamilyStore.FEMALE, 1958);
    private static final Person CAU = person("cau", "Cậu", FamilyStore.MALE, 1960);
    private static final Person MO = person("mo", "Mợ", FamilyStore.FEMALE, 1962);
    private static final Person DI = person("di", "Dì", FamilyStore.FEMALE, 1955);
    private static final Person DUONG = person("duong", "Dượng", FamilyStore.MALE, 1954);
    private static final Person ANH = person("anh", "Anh Trai", FamilyStore.MALE, 1980);
    private static final Person DAU = person("dau", "Chị Dâu", FamilyStore.FEMALE, 1981);
    private static final Person TOI = person("toi", "Tôi", FamilyStore.MALE, 1983);
    private static final Person EM = person("em", "Em Gái", FamilyStore.FEMALE, 1986);
    private static final Person RE = person("re", "Em Rể", FamilyStore.MALE, 1985);
    private static final Person CON_BAC = person("con_bac", "Anh Họ", FamilyStore.MALE, 1979);
    private static final Person CHAU_GAI = person("chau_gai", "Cháu Gái", FamilyStore.FEMALE, 1990);
    private static final Person CON_TRAI = person("con_trai", "Con Trai", FamilyStore.MALE, 2010);
    private static final Person CHAU_NOI = person("chau_noi", "Cháu Nội", FamilyStore.FEMALE, 2035);

    private static final Person[] ALL = {CU_O, CU_B, ONG_NOI, BA_NOI, BAC_GAI, CHU, THIM, CO,
            NGOAI_O, NGOAI_B, CHA, ME, CAU, MO, DI, DUONG, ANH, DAU, TOI, EM, RE, CON_BAC,
            CHAU_GAI, CON_TRAI, CHAU_NOI};

    private static List<Person> family() {
        spouse(CU_O, CU_B);
        spouse(ONG_NOI, BA_NOI);
        spouse(CHU, THIM);
        spouse(CAU, MO);
        spouse(DI, DUONG);
        spouse(ANH, DAU);
        spouse(CHA, ME);
        spouse(EM, RE);
        parent(ONG_NOI, CU_O, CU_B);
        parent(CHA, ONG_NOI, BA_NOI);
        parent(BAC_GAI, ONG_NOI, BA_NOI);
        parent(CHU, ONG_NOI, BA_NOI);
        parent(CO, ONG_NOI, BA_NOI);
        parent(CON_BAC, BAC_GAI, null);
        parent(ME, NGOAI_O, NGOAI_B);
        parent(CAU, NGOAI_O, NGOAI_B);
        parent(DI, NGOAI_O, NGOAI_B);
        parent(ANH, CHA, ME);
        parent(TOI, CHA, ME);
        parent(EM, CHA, ME);
        parent(CHAU_GAI, ANH, DAU);
        parent(CON_TRAI, TOI, null);
        parent(CHAU_NOI, CON_TRAI, null);
        List<Person> people = new ArrayList<>();
        for (Person person : ALL) people.add(person);
        return people;
    }

    private static String term(String rootId, String targetId) {
        return Kinship.of(family(), rootId, targetId).term;
    }

    @Test
    public void selfAndParents() {
        assertEquals("chính tôi", term("toi", "toi"));
        assertEquals("cha", term("toi", "cha"));
        assertEquals("mẹ", term("toi", "me"));
    }

    @Test
    public void ancestorsKeepNoiNgoai() {
        assertEquals("ông nội", term("toi", "ong_noi"));
        assertEquals("bà nội", term("toi", "ba_noi"));
        assertEquals("ông ngoại", term("toi", "ngoai_o"));
        assertEquals("bà ngoại", term("toi", "ngoai_b"));
        assertEquals("cụ nội", term("toi", "cu_o"));
        assertEquals("cụ nội", term("toi", "cu_b"));
    }

    @Test
    public void unclesAndAuntsSplitBySideAndAge() {
        assertEquals("bác", term("toi", "bac_gai"));
        assertEquals("chú", term("toi", "chu"));
        assertEquals("cô", term("toi", "co"));
        assertEquals("cậu", term("toi", "cau"));
        assertEquals("dì", term("toi", "di"));
    }

    @Test
    public void inLawTerms() {
        assertEquals("thím", term("toi", "thim"));
        assertEquals("mợ", term("toi", "mo"));
        assertEquals("dượng", term("toi", "duong"));
        assertEquals("chị dâu", term("toi", "dau"));
        assertEquals("em rể", term("toi", "re"));
        assertEquals("cháu dâu", term("ong_noi", "dau"));
        assertEquals("cháu rể", term("ong_noi", "re"));
    }

    @Test
    public void siblingsCousinsAndNieces() {
        assertEquals("anh trai", term("toi", "anh"));
        assertEquals("em gái", term("toi", "em"));
        assertEquals("anh họ", term("toi", "con_bac"));
        assertEquals("cháu gái", term("toi", "chau_gai"));
    }

    @Test
    public void descendantsCountGenerations() {
        assertEquals("con trai", term("toi", "con_trai"));
        assertEquals("cháu gái nội", term("toi", "chau_noi"));
        assertEquals("chít nội", term("cu_o", "chau_noi"));
    }

    @Test
    public void rerootShiftsEveryRelation() {
        assertEquals("con trai", term("ong_noi", "cha"));
        assertEquals("cháu trai nội", term("ong_noi", "toi"));
        assertEquals("con dâu", term("ong_noi", "me"));
        assertEquals(-2, Kinship.of(family(), "ong_noi", "toi").generation);
        assertEquals("con trai", term("cu_o", "ong_noi"));
        assertEquals("cháu trai nội", term("cu_o", "cha"));
        assertEquals("Cha mẹ", Kinship.band(1));
        assertEquals("Con", Kinship.band(-1));
        assertEquals("Cụ", Kinship.band(3));
    }

    @Test
    public void cousinTermStaysCombinedWhenAgeUnknown() {
        Person grandparent = person("g", "Cụ", FamilyStore.FEMALE, 0);
        Person father = person("f", "Bố", FamilyStore.MALE, 0);
        Person aunt = person("s", "Chị của bố", FamilyStore.FEMALE, 0);
        Person cousin = person("c", "Con của cô", FamilyStore.FEMALE, 0);
        Person root = person("r", "Tôi", FamilyStore.MALE, 0);
        parent(father, null, grandparent);
        parent(aunt, null, grandparent);
        parent(cousin, null, aunt);
        parent(root, null, father);
        List<Person> people = listOf(grandparent, aunt, father, cousin, root);
        // Cùng mẹ nên thứ tự nhập được coi là thứ tự sinh: cô đứng trước bố thành "bác".
        assertEquals("bác", Kinship.of(people, "r", "s").term);
        // Họ thì khác nhánh, không năm sinh nên phải để dạng gộp.
        assertEquals("chị/em họ", Kinship.of(people, "r", "c").term);
        assertTrue(Kinship.of(people, "r", "c").related);
    }

    @Test
    public void stepParentsUseAffinity() {
        Person father = person("sf", "Bố đẻ", FamilyStore.MALE, 1950);
        Person mother = person("sm", "Mẹ đẻ", FamilyStore.FEMALE, 1952);
        Person child = person("sc", "Con", FamilyStore.MALE, 1980);
        Person husband = person("sh", "Chồng mới của mẹ", FamilyStore.MALE, 1948);
        spouse(father, mother);
        parent(child, father, mother);
        mother.spouseId = husband.id;
        List<Person> people = listOf(father, mother, child, husband);
        assertEquals("bố dượng", Kinship.of(people, "sc", "sh").term);
        assertFalse(Kinship.of(people, "sc", "zzz").related);
    }

    @Test
    public void peopleWithoutAnyLinkAreUnrelated() {
        List<Person> people = listOf(person("a", "Người lạ", FamilyStore.MALE, 0),
                person("b", "Khác", FamilyStore.FEMALE, 0));
        Kinship.Result result = Kinship.of(people, "a", "b");
        assertFalse(result.related);
        assertEquals("Chưa xác định", result.band);
    }

    @Test
    public void manualRelationCoversPairsTheTreeCannot() {
        List<Person> people = family();
        // Thông gia: ông nội và ông ngoại không nối được qua cha mẹ – vợ chồng.
        assertFalse(Kinship.of(people, "ong_noi", "ngoai_o").related);
        List<FamilyStore.Relation> relations = relationList(
                relation("r1", "ong_noi", "ngoai_o", "ông thông gia", "cha của con dâu", 0));
        assertEquals("ông thông gia", Kinship.of(people, relations, "ong_noi", "ngoai_o").term);
        Kinship.Result reversed = Kinship.of(people, relations, "ngoai_o", "ong_noi");
        assertEquals("cha của con dâu", reversed.term);
        assertTrue(reversed.manual);
        assertEquals("Cùng hàng", reversed.band);
    }

    @Test
    public void manualRelationFlipsGenerationWithTheRoot() {
        Person chaNuoi = person("cha_nuoi", "Cha nuôi", FamilyStore.MALE, 1955);
        List<Person> people = family();
        people.add(chaNuoi);
        List<FamilyStore.Relation> relations = relationList(
                relation("r1", "toi", "cha_nuoi", "cha nuôi", "con nuôi", 1));
        Kinship.Result child = Kinship.of(people, relations, "toi", "cha_nuoi");
        assertEquals("cha nuôi", child.term);
        assertEquals(1, child.generation);
        assertEquals("Cha mẹ", child.band);
        Kinship.Result father = Kinship.of(people, relations, "cha_nuoi", "toi");
        assertEquals("con nuôi", father.term);
        assertEquals(-1, father.generation);
        assertEquals("Con", father.band);
    }

    @Test
    public void derivedRelationAlwaysBeatsManualEntry() {
        List<Person> people = family();
        List<FamilyStore.Relation> relations = relationList(
                relation("r1", "toi", "cha", "cha nuôi", "con nuôi", 1));
        Kinship.Result result = Kinship.of(people, relations, "toi", "cha");
        assertEquals("cha", result.term);
        assertFalse(result.manual);
    }

    @Test
    public void unknownGenerationGoesToItsOwnBand() {
        Person lan = person("lan", "Cô Lan", FamilyStore.FEMALE, 1950);
        List<Person> people = family();
        people.add(lan);
        List<FamilyStore.Relation> relations = relationList(relation("r1", "toi", "lan", "bạn của cha",
                "con của bạn", FamilyStore.GENERATION_UNKNOWN));
        Kinship.Result result = Kinship.of(people, relations, "toi", "lan");
        assertTrue(result.related);
        assertEquals("Quan hệ nhập tay", result.band);
        assertEquals(FamilyStore.GENERATION_UNKNOWN, result.generation);
    }

    @Test
    public void prayerTermComesFromThePersonStandingAtTheAltar() {
        List<Person> people = family();
        List<FamilyStore.Relation> none = relationList();
        assertEquals("cha", Kinship.prayerTerm(people, none, "toi", "cha"));
        assertEquals("cụ nội", Kinship.prayerTerm(people, none, "toi", "cu_o"));
        // Đổi sang Cụ Ông đứng khấn thì tôi là lớp cháu ba bậc dưới, tức "chắt nội".
        assertEquals("chắt nội", Kinship.prayerTerm(people, none, "cu_o", "toi"));
        assertEquals("", Kinship.prayerTerm(people, none, "toi", ""));
        assertEquals("", Kinship.prayerTerm(people, none, "toi", "toi"));
        assertEquals("", Kinship.prayerTerm(people, none, "toi", "lan_vang"));
    }

    @Test
    public void prayerTermUsesManualRelationWhenTheTreeCannot() {
        List<Person> people = family();
        assertEquals("", Kinship.prayerTerm(people, relationList(), "ong_noi", "ngoai_o"));
        assertEquals("ông thông gia", Kinship.prayerTerm(people,
                relationList(relation("r1", "ong_noi", "ngoai_o", "ông thông gia",
                        "ông thông gia", 0)), "ong_noi", "ngoai_o"));
    }

    private static FamilyStore.Relation relation(String id, String aId, String bId, String aCallsB,
                                                 String bCallsA, int bGeneration) {
        return new FamilyStore.Relation(id, aId, bId, aCallsB, bCallsA, bGeneration);
    }

    private static List<FamilyStore.Relation> relationList(FamilyStore.Relation... relations) {
        List<FamilyStore.Relation> result = new ArrayList<>();
        for (FamilyStore.Relation relation : relations) result.add(relation);
        return result;
    }

    private static List<Person> listOf(Person... people) {
        List<Person> result = new ArrayList<>();
        for (Person person : people) result.add(person);
        return result;
    }

    private static Person person(String id, String name, int gender, int birthYear) {
        return new Person(id, name, gender, birthYear, false, "", "", "");
    }

    private static void spouse(Person a, Person b) {
        a.spouseId = b.id;
        b.spouseId = a.id;
    }

    private static void parent(Person child, Person father, Person mother) {
        child.fatherId = father == null ? "" : father.id;
        child.motherId = mother == null ? "" : mother.id;
    }
}
