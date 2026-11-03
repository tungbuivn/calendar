package com.calendar.tbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Kiểm tra cây mẫu phủ các nhánh trong quy-tac.md. */
public class DemoFamilyTest {

    private static String term(String id) {
        return Kinship.of(DemoFamily.people(), DemoFamily.ROOT_ID, id).term;
    }

    @Test
    public void directAndGrandparents() {
        assertEquals("cha", term("bo"));
        assertEquals("mẹ", term("me"));
        assertEquals("ông nội", term("ong_noi"));
        assertEquals("bà nội", term("ba_noi"));
        assertEquals("ông ngoại", term("ong_ngoai"));
        assertEquals("bà ngoại", term("ba_ngoai"));
        assertEquals("cụ nội", term("cu_o"));
    }

    @Test
    public void grandUnclesAndTheirSpouses() {
        assertEquals("ông bác", term("ong_bac"));
        assertEquals("bà bác", term("ba_bac"));
        assertEquals("ông chú", term("ong_chu"));
        assertEquals("bà thím", term("ba_thim"));
        assertEquals("bà cô", term("ba_co"));
        assertEquals("ông dượng", term("ong_duong"));

        assertEquals("ông bác", term("ong_ba_noi_bac"));
        assertEquals("bà bác", term("ba_ba_noi_bac_vo"));
        assertEquals("ông chú", term("ong_ba_noi_chu"));
        assertEquals("bà thím", term("ba_ba_noi_chu_vo"));
        assertEquals("bà bác", term("ba_ba_noi_bac"));
        assertEquals("bà cô", term("ba_ba_noi_co"));
        assertEquals("ông bác", term("ong_ba_noi_bac_chong"));
    }

    @Test
    public void maternalGrandUnclesAndTheirSpouses() {
        assertEquals("ông bác", term("ong_ngoai_bac"));
        assertEquals("bà bác", term("ba_bac_ngoai"));
        assertEquals("ông cậu", term("ong_ngoai_cau"));
        assertEquals("bà mợ", term("ba_mo"));
        assertEquals("bà bác", term("ba_ngoai_bac"));
        assertEquals("ông bác", term("ong_bac_ngoai_chong"));
        assertEquals("bà dì", term("ba_ngoai_di"));
        assertEquals("ông dượng", term("ong_duong_ngoai"));
    }

    @Test
    public void greatGrandUnclesAndTheirSpouses() {
        assertEquals("cụ bác", term("cu_bac"));
        assertEquals("cụ bà", term("cu_ba_bac"));
        assertEquals("cụ chú", term("cu_chu"));
        assertEquals("cụ cô", term("cu_co"));
        assertEquals("cụ ông", term("ong_duong_cu"));
    }

    @Test
    public void parentsSiblingsAndSpouses() {
        assertEquals("bác", term("bac"));
        assertEquals("bác gái", term("bac_gai"));
        assertEquals("chú", term("chu"));
        assertEquals("thím", term("thim"));
        assertEquals("cô", term("co"));
        assertEquals("chú", term("chu_cua_co"));
        assertEquals("bác", term("bac_gai_2"));
        assertEquals("bác", term("bac_gai_2_chong"));

        assertEquals("cậu", term("cau"));
        assertEquals("mợ", term("mo"));
        assertEquals("dì", term("di"));
        assertEquals("dượng", term("duong_di"));
        assertEquals("bác", term("bac_ngoai"));
        assertEquals("bác", term("bac_ngoai_chong"));
    }

    @Test
    public void siblingsAndTheirSpouses() {
        assertEquals("anh trai", term("anh"));
        assertEquals("chị dâu", term("chi_dau"));
        assertEquals("chị gái", term("chi"));
        assertEquals("anh rể", term("anh_re"));
        assertEquals("em trai", term("em"));
        assertEquals("em dâu", term("em_dau"));
        assertEquals("em gái", term("em_gai"));
        assertEquals("em rể", term("em_re"));
    }

    @Test
    public void spouseSideAndDongHao() {
        assertEquals("vợ", term("vo"));
        assertEquals("bố vợ", term("bo_vo"));
        assertEquals("mẹ vợ", term("me_vo"));
        assertEquals("anh vợ", term("anh_vo"));
        assertEquals("chị vợ", term("chi_vo"));
        assertEquals("em vợ", term("em_vo"));
        assertEquals("em đồng hao", term("dong_hao"));
        // Chú/bác của vợ cũng gọi đúng vai, không thêm "bên vợ".
        assertEquals("bác", term("bac_vo"));
    }

    @Test
    public void childrenGrandchildrenAndInLaws() {
        assertEquals("con trai", term("con_trai"));
        assertEquals("con dâu", term("con_dau"));
        assertEquals("con gái", term("con_gai"));
        assertEquals("con rể", term("con_re"));
        assertEquals("cháu gái nội", term("chau_noi"));
        assertEquals("cháu trai ngoại", term("chau_ngoai"));
        assertEquals("chắt nội", term("chat"));
        assertEquals("chít nội", term("chit"));

        assertEquals("ông thông gia", term("thong_gia_dau"));
        assertEquals("bà thông gia", term("thong_gia_dau_vo"));
        assertEquals("ông thông gia", term("thong_gia_re"));
        assertEquals("bà thông gia", term("thong_gia_re_vo"));
    }

    @Test
    public void cousinsAndCousinsChildren() {
        assertEquals("anh họ", term("con_bac"));
        assertEquals("cháu họ", term("chau_ho"));
        assertEquals("em họ", term("con_chu"));
        assertEquals("chị họ", term("con_bac_ngoai"));
    }

    @Test
    public void everyDemoPersonHasATerm() {
        for (FamilyStore.Person person : DemoFamily.people()) {
            Kinship.Result result = Kinship.of(DemoFamily.people(), DemoFamily.ROOT_ID, person.id);
            assertTrue("Chưa có quan hệ: " + person.name + " (" + person.id + ")", result.related);
        }
    }
}
