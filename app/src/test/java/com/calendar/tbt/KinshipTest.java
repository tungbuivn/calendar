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
    private static final Person DI = person("di", "Dì", FamilyStore.FEMALE, 1961);
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
        // Con của bác thì là "chị họ" của mình: vai theo hàng bố mẹ, không theo tuổi.
        assertEquals("chị họ", Kinship.of(people, "r", "c").term);
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
    public void coParentsAcrossAMarriageAreThongGia() {
        List<Person> people = family();
        // Hai nhà cùng là cha mẹ của Cha (nhà nội) và của Mẹ (nhà ngoại) — nối được rồi.
        assertEquals("ông thông gia", Kinship.of(people, "ong_noi", "ngoai_o").term);
        assertEquals("bà thông gia", Kinship.of(people, "ong_noi", "ngoai_b").term);
        // Đóng theo cặp: ai trong nhà này cũng thông gia với cả hai người nhà kia.
        assertEquals("ông thông gia", Kinship.of(people, "ba_noi", "ngoai_o").term);
        assertEquals("bà thông gia", Kinship.of(people, "ba_noi", "ngoai_b").term);
        assertEquals("ông thông gia", Kinship.of(people, "ngoai_o", "ong_noi").term);
        Kinship.Result result = Kinship.of(people, "ong_noi", "ngoai_o");
        assertEquals(0, result.generation);
        assertEquals("Cùng hàng", result.band);
        assertFalse(result.manual);
    }

    @Test
    public void relativesOfTheSpouseGetInLawWords() {
        Person chong = person("c", "Chồng", FamilyStore.MALE, 1990);
        Person vo = person("v", "Vợ", FamilyStore.FEMALE, 1992);
        Person boVo = person("bv", "Bố vợ", FamilyStore.MALE, 1962);
        Person meVo = person("mv", "Mẹ vợ", FamilyStore.FEMALE, 1964);
        Person anhVo = person("av", "Anh vợ", FamilyStore.MALE, 1989);
        Person boChong = person("bc", "Bố chồng", FamilyStore.MALE, 1960);
        spouse(chong, vo);
        parent(vo, boVo, meVo);
        parent(anhVo, boVo, meVo);
        parent(chong, boChong, null);
        List<Person> people = listOf(chong, vo, boVo, meVo, anhVo, boChong);
        assertEquals("bố vợ", Kinship.of(people, "c", "bv").term);
        assertEquals("mẹ vợ", Kinship.of(people, "c", "mv").term);
        assertEquals("anh vợ", Kinship.of(people, "c", "av").term);
        assertEquals("bố chồng", Kinship.of(people, "v", "bc").term);
        // Cha mẹ đẻ thì vẫn gọi theo huyết thống, không lẫn với bên vợ.
        assertEquals("cha", Kinship.of(people, "v", "bv").term);
    }

    @Test
    public void manualRelationCoversPairsTheTreeCannot() {
        List<Person> people = family();
        Person banCuaCha = person("ban", "Bạn của cha", FamilyStore.MALE, 1955);
        people.add(banCuaCha);
        assertFalse(Kinship.of(people, "toi", "ban").related);
        List<FamilyStore.Relation> relations = relationList(relation("r1", "toi", "ban",
                "bạn của cha", "con của bạn", FamilyStore.GENERATION_UNKNOWN));
        assertEquals("bạn của cha", Kinship.of(people, relations, "toi", "ban").term);
        Kinship.Result reversed = Kinship.of(people, relations, "ban", "toi");
        assertEquals("con của bạn", reversed.term);
        assertTrue(reversed.manual);
        assertEquals("Quan hệ nhập tay", reversed.band);
    }

    @Test
    public void oneSidedManualRelationFlipsThroughTheTag() {
        Person nguoiA = person("pa", "Người A", FamilyStore.MALE, 1970);
        Person nguoiB = person("pb", "Người B", FamilyStore.MALE, 2000);
        List<Person> people = listOf(nguoiA, nguoiB);
        // Chỉ mới ghi một chiều: A gọi B là "chú" thì chiều ngược lại tự ra "cháu".
        List<FamilyStore.Relation> relations = relationList(
                relation("r1", "pa", "pb", "chú", "", 1));
        assertEquals("chú", Kinship.of(people, relations, "pa", "pb").term);
        Kinship.Result back = Kinship.of(people, relations, "pb", "pa");
        assertEquals("cháu", back.term);
        assertEquals(-1, back.generation);
        assertEquals("Con", back.band);
        assertTrue(back.manual);
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
        // Cặp này giờ đã nối được nên khỏi nhập tay.
        assertEquals("ông thông gia",
                Kinship.prayerTerm(people, relationList(), "ong_noi", "ngoai_o"));
        Person lan = person("lan_vang", "Bà Lan", FamilyStore.FEMALE, 1950);
        people.add(lan);
        assertEquals("", Kinship.prayerTerm(people, relationList(), "ong_noi", "lan_vang"));
        assertEquals("bà Lan", Kinship.prayerTerm(people,
                relationList(relation("r1", "ong_noi", "lan_vang", "bà Lan", "ông", 0)),
                "ong_noi", "lan_vang"));
    }

    /** Nhà bác Tùng: em gái của chồng thì cả vợ chồng đều là "cô", chồng của cô là "chú". */
    private static List<Person> nhaBacTung() {
        Person ong = person("chat", "Ông Nội", FamilyStore.MALE, 1945);
        Person ba = person("ngai", "Bà Nội", FamilyStore.FEMALE, 1950);
        Person conTrai = person("tung", "Bùi Thanh Tùng", FamilyStore.MALE, 1980);
        Person conGai = person("huyen", "Bùi Thanh Huyền", FamilyStore.FEMALE, 1987);
        Person vo = person("nhung", "Nguyễn Thị Hồng Nhung", FamilyStore.FEMALE, 1977);
        Person chongCuaEmGai = person("manh", "Bùi Văn Mạnh", FamilyStore.MALE, 0);
        spouse(ong, ba);
        parent(conTrai, ong, ba);
        parent(conGai, ong, ba);
        spouse(conTrai, vo);
        spouse(conGai, chongCuaEmGai);
        return new ArrayList<>(listOf(ong, ba, conTrai, conGai, vo, chongCuaEmGai));
    }

    @Test
    public void siblingInLawsRankWithTheirOwnFamilyNotTheirOwnAge() {
        List<Person> people = nhaBacTung();
        // Mạnh cưới em gái của Tùng nên là "em rể" của Nhung: vai lấy theo thứ tự sinh
        // của hai chị em nhà chồng, không theo tuổi của chính hai người.
        Kinship.Result result = Kinship.of(people, "nhung", "manh");
        assertTrue(result.related);
        assertEquals("em rể", result.term);
        assertEquals(0, result.generation);
        assertEquals("Cùng hàng", result.band);
        assertEquals(1, result.seniority);
        // Chiều ngược lại: vợ của anh chồng là "chị dâu".
        assertEquals("chị dâu", Kinship.of(people, "manh", "nhung").term);
        // Chị/em ruột của chồng vẫn gọi theo phía chồng, không bị hôn thú lấn.
        assertEquals("em chồng", Kinship.of(people, "nhung", "huyen").term);
        // Vai hôn thú của bố mẹ chồng phải đứng trên vai "dượng/dì" của người nối.
        assertEquals("bố chồng", Kinship.of(people, "nhung", "chat").term);
        assertEquals("mẹ chồng", Kinship.of(people, "nhung", "ngai").term);
        assertEquals("bố vợ", Kinship.of(people, "manh", "chat").term);
    }

    @Test
    public void husbandsOfPaternalAuntsAreUncleWordsNotDuong() {
        List<Person> people = nhaBacTung();
        Person tram = person("tram", "Bùi Ngọc Trâm", FamilyStore.FEMALE, 2006);
        parent(tram, people.get(2), people.get(4));
        people.add(tram);
        assertEquals("cô", Kinship.of(people, "tram", "huyen").term);
        // Em của bố là cô thì chồng của cô là chú, chỉ nhà ngoại (dì) mới là dượng.
        Kinship.Result chu = Kinship.of(people, "tram", "manh");
        assertEquals("chú", chu.term);
        assertEquals(1, chu.generation);
        assertEquals("Cha mẹ", chu.band);
    }

    @Test
    public void spousesShareTheRankOfTheirOwnSiblingSet() {
        Person ong = person("ong", "Ông Nội", FamilyStore.MALE, 1940);
        Person ba = person("ba", "Bà Nội", FamilyStore.FEMALE, 1942);
        Person chi = person("chi", "Chị chồng", FamilyStore.FEMALE, 1970);
        Person cuChong = person("chu", "Em chồng", FamilyStore.MALE, 1975);
        Person vo = person("vo", "Vợ", FamilyStore.FEMALE, 1976);
        Person chongChi = person("chong_chi", "Chồng của chị", FamilyStore.MALE, 1968);
        spouse(ong, ba);
        parent(chi, ong, ba);
        parent(cuChong, ong, ba);
        spouse(cuChong, vo);
        spouse(chi, chongChi);
        List<Person> people = listOf(ong, ba, chi, cuChong, vo, chongChi);
        // Vợ đứng đúng vai của chồng: chị gái chồng là "chị chồng", chồng của chị ấy là "anh rể".
        assertEquals("chị chồng", Kinship.of(people, "vo", "chi").term);
        assertEquals("anh rể", Kinship.of(people, "vo", "chong_chi").term);
        Person chau = person("chau", "Con của vợ chồng", FamilyStore.MALE, 2000);
        parent(chau, cuChong, vo);
        people.add(chau);
        // Hàng con thì bác gái hay bác trai đều gọi là "bác", chỉ khác giới tính người được gọi.
        assertEquals("bác", Kinship.of(people, "chau", "chi").term);
        assertEquals("bác", Kinship.of(people, "chau", "chong_chi").term);
        Kinship.Result reverse = Kinship.of(people, "chong_chi", "chau");
        assertTrue(reverse.related);
        assertEquals(-1, reverse.generation);
    }

    @Test
    public void mothersElderSiblingsAreBacNotCau() {
        Person ngoaiO = person("no", "Ông Ngoại", FamilyStore.MALE, 1929);
        Person ngoaiB = person("nb", "Bà Ngoại", FamilyStore.FEMALE, 1931);
        Person chiCuaMe = person("bac_ngoai", "Chị của mẹ", FamilyStore.FEMALE, 1950);
        Person chongCuaChi = person("chong_bac", "Chồng của bác", FamilyStore.MALE, 1948);
        Person me = person("me2", "Mẹ", FamilyStore.FEMALE, 1958);
        Person emTraiCuaMe = person("cau2", "Em trai của mẹ", FamilyStore.MALE, 1960);
        Person con = person("con2", "Con", FamilyStore.MALE, 1985);
        spouse(ngoaiO, ngoaiB);
        spouse(chiCuaMe, chongCuaChi);
        parent(chiCuaMe, ngoaiO, ngoaiB);
        parent(me, ngoaiO, ngoaiB);
        parent(emTraiCuaMe, ngoaiO, ngoaiB);
        parent(con, null, me);
        List<Person> people = listOf(ngoaiO, ngoaiB, chiCuaMe, chongCuaChi, me, emTraiCuaMe, con);
        // Bên ngoại cũng chia trên dưới như bên nội: chị của mẹ là "bác", em mới là "dì".
        assertEquals("bác", Kinship.of(people, "con2", "bac_ngoai").term);
        assertEquals("bác", Kinship.of(people, "con2", "chong_bac").term);
        assertEquals("cậu", Kinship.of(people, "con2", "cau2").term);
    }

    /** Nhà bác Tùng có thêm Chuối là con của Mạnh – Huyền, đúng như gia phả trên điện thoại. */
    @Test
    public void childOfTheYoungerSisterCallsTheElderBrotherBac() {
        List<Person> people = nhaBacTung();
        Person chuoi = person("chuoi", "Chuối", FamilyStore.MALE, 1985);
        parent(chuoi, people.get(5), people.get(3)); // cha = Mạnh, mẹ = Huyền
        people.add(chuoi);
        // Tùng (1980) là anh của Huyền (1987) nên Chuối gọi Tùng là "bác", không phải "cậu".
        assertEquals("bác", Kinship.of(people, "chuoi", "tung").term);
        assertEquals("bác gái", Kinship.of(people, "chuoi", "nhung").term);
        assertEquals("mẹ", Kinship.of(people, "chuoi", "huyen").term);
        assertEquals("cha", Kinship.of(people, "chuoi", "manh").term);
        assertEquals("ông ngoại", Kinship.of(people, "chuoi", "chat").term);
        assertEquals("bà ngoại", Kinship.of(people, "chuoi", "ngai").term);
        // Con của bác thì ngang hàng Chuối về thế hệ, nhưng vai theo bố mẹ: Huyền là em của
        // Tùng nên Chuối là "em họ" của Trâm và phải gọi Trâm là "chị", bất kể tuổi tác.
        Person tram = person("tram2", "Bùi Ngọc Trâm", FamilyStore.FEMALE, 1990);
        parent(tram, people.get(2), people.get(4)); // cha = Tùng, mẹ = Nhung
        people.add(tram);
        Kinship.Result chuoiGoi = Kinship.of(people, "chuoi", "tram2");
        assertTrue(chuoiGoi.related);
        assertEquals("Cùng hàng", chuoiGoi.band);
        assertEquals("chị họ", chuoiGoi.term);
        assertEquals("em họ", Kinship.of(people, "tram2", "chuoi").term);
    }

    /** Nhà ngoại của Chuối: hàng ông bà thì gọi ông bà, nhưng "nội/ngoại" chỉ người sinh ra bố mẹ. */
    @Test
    public void everyoneInTheGrandparentBandIsCalledOngBa() {
        Person vuong = person("nv", "Nguyễn Thành Vượng", FamilyStore.MALE, 0);
        Person ngai = person("ng", "Nguyễn Thị Ngải", FamilyStore.FEMALE, 1950);
        Person ngan = person("nn", "Nguyễn thành ngạn", FamilyStore.MALE, 0);
        Person dung = person("dg", "Dung", FamilyStore.FEMALE, 0);
        Person chat = person("bc", "Bùi Văn Chắt", FamilyStore.MALE, 1945);
        Person huyen = person("ht", "Bùi Thanh Huyền", FamilyStore.FEMALE, 1987);
        Person manh = person("bv", "Bùi Văn Mạnh", FamilyStore.MALE, 0);
        Person chuoi = person("ch", "Chuối", FamilyStore.MALE, 1985);
        Person tung = person("tt", "Bui thanh tung", FamilyStore.MALE, 1980);
        Person nhung = person("nh", "nguyen thi hong nhung", FamilyStore.FEMALE, 1977);
        Person bach = person("bb", "bui viet bach", FamilyStore.MALE, 2016);
        spouse(ngai, chat);
        spouse(ngan, dung);
        spouse(huyen, manh);
        spouse(tung, nhung);
        parent(ngai, vuong, null);
        parent(ngan, vuong, null);
        parent(huyen, chat, ngai);
        parent(tung, chat, ngai);
        parent(chuoi, manh, huyen);
        parent(bach, tung, nhung);
        List<Person> people = listOf(vuong, ngai, ngan, dung, chat, huyen, manh, chuoi, tung,
                nhung, bach);
        // Nhìn từ Chuối: bố mẹ của mẹ mới là "ông bà ngoại", bác của mẹ thì là "ông bên ngoại".
        assertEquals("mẹ", Kinship.of(people, "ch", "ht").term);
        assertEquals("bà ngoại", Kinship.of(people, "ch", "ng").term);
        assertEquals("ông ngoại", Kinship.of(people, "ch", "bc").term);
        assertEquals("ông bên ngoại", Kinship.of(people, "ch", "nn").term);
        assertEquals("bà bên ngoại", Kinship.of(people, "ch", "dg").term);
        assertEquals("cụ ngoại", Kinship.of(people, "ch", "nv").term);
        assertEquals("Ông bà", Kinship.of(people, "ch", "nn").band);
        // Con của ông ngạn thì ngang hàng bố mẹ Chuối nên vẫn gọi là "bác" theo hàng.
        Person conCuaBac = person("cc", "Con của bác", FamilyStore.MALE, 1975);
        parent(conCuaBac, ngan, dung);
        people.add(conCuaBac);
        assertEquals("bác", Kinship.of(people, "ch", "cc").term);
        assertEquals("Cha mẹ", Kinship.of(people, "ch", "cc").band);
        // Nhìn từ Bách: cùng những người đó nhưng là bên nội, và Chuối là "em họ" dù hơn 31 tuổi.
        assertEquals("cha", Kinship.of(people, "bb", "tt").term);
        assertEquals("bà nội", Kinship.of(people, "bb", "ng").term);
        assertEquals("ông nội", Kinship.of(people, "bb", "bc").term);
        assertEquals("ông bên nội", Kinship.of(people, "bb", "nn").term);
        assertEquals("bà bên nội", Kinship.of(people, "bb", "dg").term);
        assertEquals("cụ nội", Kinship.of(people, "bb", "nv").term);
        assertEquals("em họ", Kinship.of(people, "bb", "ch").term);
        assertEquals("anh họ", Kinship.of(people, "ch", "bb").term);
    }

    @Test
    public void parentsOfTheEldersInLawFamilyAreOngBaOfThatBranch() {
        List<Person> people = nhaBacTung();
        // Chuối là con của Huyền và Mạnh; Nhung (vợ của bác Tùng) cưới vào nên bố mẹ đẻ của
        // Nhung không huyết thống với Chuối, nhưng vẫn ngồi hàng ông bà.
        Person chuoi = person("chuoi", "Chuối", FamilyStore.MALE, 1985);
        parent(chuoi, people.get(5), people.get(3));
        Person khoat = person("khoat", "Nguyễn Văn Khoát", FamilyStore.MALE, 1940);
        Person xuyen = person("xuyen", "Nguyễn Thị Xuyến", FamilyStore.FEMALE, 1942);
        spouse(khoat, xuyen);
        people.get(4).fatherId = khoat.id;
        people.get(4).motherId = xuyen.id;
        // Mẹ của ông Khoát thì lên một bậc nữa.
        Person cu = person("cu", "Cụ bên bác", FamilyStore.FEMALE, 1915);
        parent(khoat, null, cu);
        people.add(chuoi);
        people.add(khoat);
        people.add(xuyen);
        people.add(cu);
        assertEquals("bác", Kinship.of(people, "chuoi", "tung").term);
        assertEquals("bác gái", Kinship.of(people, "chuoi", "nhung").term);
        Kinship.Result ong = Kinship.of(people, "chuoi", "khoat");
        assertTrue(ong.related);
        assertEquals("ông bên bác", ong.term);
        assertEquals(2, ong.generation);
        assertEquals("Ông bà", ong.band);
        assertEquals("bà bên bác", Kinship.of(people, "chuoi", "xuyen").term);
        assertEquals("cụ bên bác", Kinship.of(people, "chuoi", "cu").term);
        assertEquals("Cụ", Kinship.of(people, "chuoi", "cu").band);
        // Người nhà chồng không vì hôn thú mà mất hàng: ông bà nội của Chuối vẫn là "ngoại".
        assertEquals("ông ngoại", Kinship.of(people, "chuoi", "chat").term);
        assertEquals("bà ngoại", Kinship.of(people, "chuoi", "ngai").term);
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
