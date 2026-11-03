package com.calendar.tbt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Kiểm tra xưng hô số nhiều: hai người đứng lễ trở lên thì phải nói "chúng con". */
public class WorshippersTest {

    @Test
    public void oneWorshipperKeepsTheSingularPronoun() {
        assertEquals(1, Worshippers.count("Nguyễn Văn A"));
        String raw = "Tín chủ con là {{ho_ten}}, ngụ tại {{dia_chi}}.";
        assertEquals(raw, Worshippers.agree(raw, "Nguyễn Văn A"));
    }

    @Test
    public void emptyNamesAreNotTreatedAsAGroup() {
        assertEquals(1, Worshippers.count(null));
        assertEquals(1, Worshippers.count(""));
        assertEquals(1, Worshippers.count("   "));
        String raw = "tín chủ con là {{ho_ten}}.";
        assertEquals(raw, Worshippers.agree(raw, ""));
    }

    @Test
    public void separatedNamesSwitchToChungCon() {
        String raw = "Tín chủ con là {{ho_ten}}, ngụ tại {{dia_chi}}.";
        assertEquals("Tín chủ chúng con là {{ho_ten}}, ngụ tại {{dia_chi}}.",
                Worshippers.agree(raw, "Nguyễn Văn A, Trần Thị B"));
        assertEquals("Tín chủ chúng con là {{ho_ten}}, ngụ tại {{dia_chi}}.",
                Worshippers.agree(raw, "Nguyễn Văn A; Trần Thị B"));
        assertEquals(3, Worshippers.count("Nguyễn Văn A, Trần Thị B và Lê Văn C"));
    }

    @Test
    public void vaAndCungAlsoMakeTwoWorshippers() {
        assertEquals(2, Worshippers.count("Nguyễn Văn A và Trần Thị B"));
        assertEquals(2, Worshippers.count("Nguyễn Văn A cùng Trần Thị B"));
        assertEquals("tín chủ chúng con là {{ho_ten}}",
                Worshippers.agree("tín chủ con là {{ho_ten}}", "A và B"));
    }

    @Test
    public void capitalisationSurvivesTheSwap() {
        assertEquals("Tín chủ chúng con là {{ho_ten}}",
                Worshippers.agree("Tín chủ con là {{ho_ten}}", "A, B"));
        assertEquals("tín chủ chúng con là {{ho_ten}}",
                Worshippers.agree("tín chủ con là {{ho_ten}}", "A, B"));
    }

    @Test
    public void otherUsesOfConStayUntouched() {
        String raw = "gia đình con làm lễ cho {{ten_nguoi_than}}, có con là {{ten_nguoi_than}}, "
                + "tín chủ con là {{ho_ten}}.";
        assertEquals("gia đình con làm lễ cho {{ten_nguoi_than}}, có con là {{ten_nguoi_than}}, "
                        + "tín chủ chúng con là {{ho_ten}}.",
                Worshippers.agree(raw, "A, B"));
    }

    @Test
    public void everyOccurrenceInThePrayerIsFixed() {
        String raw = "Tín chủ con là {{ho_ten}}.\n\nLại nữa, tín chủ con là {{ho_ten}}.";
        String fixed = Worshippers.agree(raw, "A, B");
        assertEquals(-1, fixed.indexOf("chủ con là"));
        assertEquals(2, fixed.split("chúng con là", -1).length - 1);
    }

    @Test
    public void nameThatEndsWithCungIsStillOneWorshipper() {
        assertEquals(1, Worshippers.count("Nguyễn Văn Cùng"));
        assertEquals(1, Worshippers.count("Trần Thị Và"));
    }
}
