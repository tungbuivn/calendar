package com.calendar.tbt;

import com.calendar.tbt.FamilyStore.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * Gia phả mẫu phủ các nhánh trong quy-tac.md: nội – ngoại – ông bà – cụ – bác/chú/cô/cậu/dì –
 * vợ/chồng của các vai đó – dâu/rể – thông gia – đồng hao – chắt/chít.
 * Chỉ dùng cho mục đích xem thử; không tự nạp nếu người dùng không chọn.
 */
public final class DemoFamily {

    public static final String ROOT_ID = "toi";

    private DemoFamily() {
    }

    public static List<Person> people() {
        List<Person> p = new ArrayList<>();

        // ---- cụ kỵ bên nội: nguồn của các cụ bác/chú/cô ----
        Person kyO = add(p, "ky_o", "Kỵ Ông", FamilyStore.MALE, 1870, null, null);
        Person kyB = add(p, "ky_b", "Kỵ Bà", FamilyStore.FEMALE, 1872, null, null);
        marry(kyO, kyB);

        Person cuO = add(p, "cu_o", "Cụ Ông Nội", FamilyStore.MALE, 1900, kyO, kyB);
        Person cuB = add(p, "cu_b", "Cụ Bà Nội", FamilyStore.FEMALE, 1903, null, null);
        marry(cuO, cuB);

        Person cuBac = add(p, "cu_bac", "Cụ Bác", FamilyStore.MALE, 1895, kyO, kyB);
        Person cuBaBac = add(p, "cu_ba_bac", "Cụ Bà Bác", FamilyStore.FEMALE, 1897, null, null);
        marry(cuBac, cuBaBac);

        Person cuChu = add(p, "cu_chu", "Cụ Chú", FamilyStore.MALE, 1905, kyO, kyB);
        Person cuBaThim = add(p, "cu_ba_thim", "Cụ Bà Thím", FamilyStore.FEMALE, 1907, null, null);
        marry(cuChu, cuBaThim);

        Person cuCo = add(p, "cu_co", "Cụ Cô", FamilyStore.FEMALE, 1908, kyO, kyB);
        Person ongDuongCu = add(p, "ong_duong_cu", "Ông Dượng Cụ", FamilyStore.MALE, 1906, null, null);
        marry(cuCo, ongDuongCu);

        // ---- ông bà nội và anh chị em của ông bà ----
        Person ongNoi = add(p, "ong_noi", "Ông Nội", FamilyStore.MALE, 1930, cuO, cuB);
        Person baNoi = add(p, "ba_noi", "Bà Nội", FamilyStore.FEMALE, 1932, null, null);
        marry(ongNoi, baNoi);

        Person ongBac = add(p, "ong_bac", "Ông Bác", FamilyStore.MALE, 1925, cuO, cuB);
        Person baBac = add(p, "ba_bac", "Bà Bác", FamilyStore.FEMALE, 1927, null, null);
        marry(ongBac, baBac);

        Person baCo = add(p, "ba_co", "Bà Cô", FamilyStore.FEMALE, 1928, cuO, cuB);
        Person ongDuong = add(p, "ong_duong", "Ông Dượng", FamilyStore.MALE, 1926, null, null);
        marry(baCo, ongDuong);

        Person ongChu = add(p, "ong_chu", "Ông Chú", FamilyStore.MALE, 1935, cuO, cuB);
        Person baThim = add(p, "ba_thim", "Bà Thím", FamilyStore.FEMALE, 1937, null, null);
        marry(ongChu, baThim);

        // bà nội có bố mẹ riêng để anh chị em bà nội không bị coi là dòng ông nội
        Person cuBaNoi = add(p, "cu_ba_noi", "Cụ Ông Bên Bà Nội", FamilyStore.MALE, 1905, null, null);
        Person cuBaNoiB = add(p, "cu_ba_noi_b", "Cụ Bà Bên Bà Nội", FamilyStore.FEMALE, 1907, null, null);
        marry(cuBaNoi, cuBaNoiB);
        baNoi.fatherId = cuBaNoi.id;
        baNoi.motherId = cuBaNoiB.id;

        Person ongBaNoiBac = add(p, "ong_ba_noi_bac", "Ông Bác Bên Bà Nội",
                FamilyStore.MALE, 1926, cuBaNoi, cuBaNoiB);
        Person baBaNoiBacVo = add(p, "ba_ba_noi_bac_vo", "Bà Bác Bên Bà Nội",
                FamilyStore.FEMALE, 1928, null, null);
        marry(ongBaNoiBac, baBaNoiBacVo);

        Person ongBaNoiChu = add(p, "ong_ba_noi_chu", "Ông Chú Bên Bà Nội",
                FamilyStore.MALE, 1936, cuBaNoi, cuBaNoiB);
        Person baBaNoiChuVo = add(p, "ba_ba_noi_chu_vo", "Bà Thím Bên Bà Nội",
                FamilyStore.FEMALE, 1938, null, null);
        marry(ongBaNoiChu, baBaNoiChuVo);

        Person baBaNoiBac = add(p, "ba_ba_noi_bac", "Bà Bác Bên Bà Nội",
                FamilyStore.FEMALE, 1928, cuBaNoi, cuBaNoiB);
        Person ongBaNoiBacChong = add(p, "ong_ba_noi_bac_chong", "Ông Bác Bên Bà Nội",
                FamilyStore.MALE, 1926, null, null);
        marry(baBaNoiBac, ongBaNoiBacChong);

        Person baBaNoiCo = add(p, "ba_ba_noi_co", "Bà Cô Bên Bà Nội",
                FamilyStore.FEMALE, 1938, cuBaNoi, cuBaNoiB);

        // ---- bố mẹ và anh chị em của bố ----
        Person bo = add(p, "bo", "Bố", FamilyStore.MALE, 1960, ongNoi, baNoi);
        Person me = add(p, "me", "Mẹ", FamilyStore.FEMALE, 1962, null, null);
        marry(bo, me);

        Person bac = add(p, "bac", "Bác Trai", FamilyStore.MALE, 1955, ongNoi, baNoi);
        Person bacGai = add(p, "bac_gai", "Bác Gái", FamilyStore.FEMALE, 1957, null, null);
        marry(bac, bacGai);

        Person bacGai2 = add(p, "bac_gai_2", "Bác Gái Hai", FamilyStore.FEMALE, 1958,
                ongNoi, baNoi);
        Person bacGai2Chong = add(p, "bac_gai_2_chong", "Bác Trai Hai", FamilyStore.MALE,
                1956, null, null);
        marry(bacGai2, bacGai2Chong);

        Person chu = add(p, "chu", "Chú", FamilyStore.MALE, 1965, ongNoi, baNoi);
        Person thim = add(p, "thim", "Thím", FamilyStore.FEMALE, 1967, null, null);
        marry(chu, thim);

        Person co = add(p, "co", "Cô", FamilyStore.FEMALE, 1968, ongNoi, baNoi);
        Person chuCuaCo = add(p, "chu_cua_co", "Chú Của Cô", FamilyStore.MALE, 1966, null, null);
        marry(co, chuCuaCo);

        // ---- ông bà ngoại và anh chị em ----
        Person cuNgoai = add(p, "cu_ngoai", "Cụ Ông Ngoại", FamilyStore.MALE, 1905, null, null);
        Person cuNgoaiB = add(p, "cu_ngoai_b", "Cụ Bà Ngoại", FamilyStore.FEMALE, 1907, null, null);
        marry(cuNgoai, cuNgoaiB);

        Person ongNgoai = add(p, "ong_ngoai", "Ông Ngoại", FamilyStore.MALE, 1933,
                cuNgoai, cuNgoaiB);
        Person baNgoai = add(p, "ba_ngoai", "Bà Ngoại", FamilyStore.FEMALE, 1935, null, null);
        marry(ongNgoai, baNgoai);
        me.fatherId = ongNgoai.id;
        me.motherId = baNgoai.id;

        Person cuBaNgoai = add(p, "cu_ba_ngoai", "Cụ Ông Bên Bà Ngoại", FamilyStore.MALE,
                1906, null, null);
        Person cuBaNgoaiB = add(p, "cu_ba_ngoai_b", "Cụ Bà Bên Bà Ngoại", FamilyStore.FEMALE,
                1908, null, null);
        marry(cuBaNgoai, cuBaNgoaiB);
        baNgoai.fatherId = cuBaNgoai.id;
        baNgoai.motherId = cuBaNgoaiB.id;

        Person ongNgoaiBac = add(p, "ong_ngoai_bac", "Ông Bác Ngoại", FamilyStore.MALE, 1928,
                cuNgoai, cuNgoaiB);
        Person baBacNgoai = add(p, "ba_bac_ngoai", "Bà Bác Ngoại", FamilyStore.FEMALE, 1930,
                null, null);
        marry(ongNgoaiBac, baBacNgoai);

        Person ongNgoaiCau = add(p, "ong_ngoai_cau", "Ông Cậu Ngoại", FamilyStore.MALE, 1938,
                cuNgoai, cuNgoaiB);
        Person baMo = add(p, "ba_mo", "Bà Mợ", FamilyStore.FEMALE, 1940, null, null);
        marry(ongNgoaiCau, baMo);

        Person baNgoaiBac = add(p, "ba_ngoai_bac", "Bà Bác Ngoại", FamilyStore.FEMALE, 1930,
                cuBaNgoai, cuBaNgoaiB);
        Person ongBacNgoaiChong = add(p, "ong_bac_ngoai_chong", "Ông Bác Ngoại",
                FamilyStore.MALE, 1928, null, null);
        marry(baNgoaiBac, ongBacNgoaiChong);

        Person baNgoaiDi = add(p, "ba_ngoai_di", "Bà Dì Ngoại", FamilyStore.FEMALE, 1940,
                cuBaNgoai, cuBaNgoaiB);
        Person ongDuongNgoai = add(p, "ong_duong_ngoai", "Ông Dượng Ngoại",
                FamilyStore.MALE, 1938, null, null);
        marry(baNgoaiDi, ongDuongNgoai);

        // ---- anh chị em của mẹ ----
        Person bacNgoai = add(p, "bac_ngoai", "Bác Gái Ngoại", FamilyStore.FEMALE, 1955,
                ongNgoai, baNgoai);
        Person bacNgoaiChong = add(p, "bac_ngoai_chong", "Bác Trai Ngoại", FamilyStore.MALE,
                1953, null, null);
        marry(bacNgoai, bacNgoaiChong);

        Person cau = add(p, "cau", "Cậu", FamilyStore.MALE, 1965, ongNgoai, baNgoai);
        Person mo = add(p, "mo", "Mợ", FamilyStore.FEMALE, 1960, null, null);
        marry(cau, mo);

        Person di = add(p, "di", "Dì", FamilyStore.FEMALE, 1967, ongNgoai, baNgoai);
        Person duongDi = add(p, "duong_di", "Dượng", FamilyStore.MALE, 1965, null, null);
        marry(di, duongDi);

        // ---- anh chị em ruột và vợ/chồng ----
        Person toi = add(p, "toi", "Tôi", FamilyStore.MALE, 1985, bo, me);
        Person anh = add(p, "anh", "Anh Trai", FamilyStore.MALE, 1983, bo, me);
        Person chiDau = add(p, "chi_dau", "Chị Dâu", FamilyStore.FEMALE, 1985, null, null);
        marry(anh, chiDau);
        Person chi = add(p, "chi", "Chị Gái", FamilyStore.FEMALE, 1981, bo, me);
        Person anhRe = add(p, "anh_re", "Anh Rể", FamilyStore.MALE, 1979, null, null);
        marry(chi, anhRe);
        Person em = add(p, "em", "Em Trai", FamilyStore.MALE, 1990, bo, me);
        Person emDau = add(p, "em_dau", "Em Dâu", FamilyStore.FEMALE, 1992, null, null);
        marry(em, emDau);
        Person emGai = add(p, "em_gai", "Em Gái", FamilyStore.FEMALE, 1992, bo, me);
        Person emRe = add(p, "em_re", "Em Rể", FamilyStore.MALE, 1990, null, null);
        marry(emGai, emRe);

        // ---- vợ và nhà vợ ----
        Person vo = add(p, "vo", "Vợ", FamilyStore.FEMALE, 1990, null, null);
        marry(toi, vo);

        Person cuVo = add(p, "cu_vo", "Cụ Ông Bên Vợ", FamilyStore.MALE, 1940, null, null);
        Person cuVoB = add(p, "cu_vo_b", "Cụ Bà Bên Vợ", FamilyStore.FEMALE, 1942, null, null);
        marry(cuVo, cuVoB);

        Person boVo = add(p, "bo_vo", "Bố Vợ", FamilyStore.MALE, 1965, cuVo, cuVoB);
        Person meVo = add(p, "me_vo", "Mẹ Vợ", FamilyStore.FEMALE, 1967, null, null);
        marry(boVo, meVo);
        vo.fatherId = boVo.id;
        vo.motherId = meVo.id;

        Person bacVo = add(p, "bac_vo", "Bác Bên Vợ", FamilyStore.MALE, 1960, cuVo, cuVoB);
        Person anhVo = add(p, "anh_vo", "Anh Vợ", FamilyStore.MALE, 1988, boVo, meVo);
        Person chiVo = add(p, "chi_vo", "Chị Vợ", FamilyStore.FEMALE, 1989, boVo, meVo);
        Person emVo = add(p, "em_vo", "Em Vợ", FamilyStore.FEMALE, 1994, boVo, meVo);

        Person dongHao = add(p, "dong_hao", "Đồng Hao", FamilyStore.MALE, 1989, null, null);
        marry(chiVo, dongHao);
        Person conChiVo = add(p, "con_chi_vo", "Cháu Bên Vợ", FamilyStore.FEMALE, 2015,
                chiVo, dongHao);

        // ---- con, dâu/rể, cháu, chắt, chít ----
        Person conTrai = add(p, "con_trai", "Con Trai", FamilyStore.MALE, 2010, toi, vo);
        Person conDau = add(p, "con_dau", "Con Dâu", FamilyStore.FEMALE, 2012, null, null);
        marry(conTrai, conDau);
        Person thongGiaDau = add(p, "thong_gia_dau", "Ông Thông Gia Dâu",
                FamilyStore.MALE, 1980, null, null);
        Person thongGiaDauVo = add(p, "thong_gia_dau_vo", "Bà Thông Gia Dâu",
                FamilyStore.FEMALE, 1982, null, null);
        marry(thongGiaDau, thongGiaDauVo);
        conDau.fatherId = thongGiaDau.id;
        conDau.motherId = thongGiaDauVo.id;

        Person conGai = add(p, "con_gai", "Con Gái", FamilyStore.FEMALE, 2012, toi, vo);
        Person conRe = add(p, "con_re", "Con Rể", FamilyStore.MALE, 2010, null, null);
        marry(conGai, conRe);
        Person thongGiaRe = add(p, "thong_gia_re", "Ông Thông Gia Rể",
                FamilyStore.MALE, 1980, null, null);
        Person thongGiaReVo = add(p, "thong_gia_re_vo", "Bà Thông Gia Rể",
                FamilyStore.FEMALE, 1982, null, null);
        marry(thongGiaRe, thongGiaReVo);
        conRe.fatherId = thongGiaRe.id;
        conRe.motherId = thongGiaReVo.id;

        Person chauNoi = add(p, "chau_noi", "Cháu Nội", FamilyStore.FEMALE, 2035,
                conTrai, conDau);
        Person chauNgoai = add(p, "chau_ngoai", "Cháu Ngoại", FamilyStore.MALE, 2038,
                conGai, conRe);
        Person chat = add(p, "chat", "Chắt", FamilyStore.MALE, 2060, chauNoi, null);
        add(p, "chit", "Chít", FamilyStore.FEMALE, 2085, chat, null);

        // ---- anh chị em họ và cháu họ ----
        Person conBac = add(p, "con_bac", "Con Bác", FamilyStore.MALE, 1985, bac, bacGai);
        add(p, "chau_ho", "Cháu Họ", FamilyStore.FEMALE, 2010, conBac, null);
        add(p, "con_chu", "Con Chú", FamilyStore.MALE, 1990, chu, thim);
        add(p, "con_co", "Con Cô", FamilyStore.FEMALE, 1992, co, chuCuaCo);
        add(p, "con_cau", "Con Cậu", FamilyStore.MALE, 1988, cau, mo);
        add(p, "con_di", "Con Dì", FamilyStore.FEMALE, 1992, di, duongDi);
        add(p, "con_bac_ngoai", "Con Bác Ngoại", FamilyStore.FEMALE, 1985,
                bacNgoai, bacNgoaiChong);

        return p;
    }

    private static Person add(List<Person> people, String id, String name, int gender,
                              int birthYear, Person father, Person mother) {
        Person person = new Person(id, name, gender, birthYear, false,
                father == null ? "" : father.id, mother == null ? "" : mother.id, "");
        people.add(person);
        return person;
    }

    private static void marry(Person first, Person second) {
        first.spouseId = second.id;
        second.spouseId = first.id;
    }
}
