package com.calendar.tbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.calendar.tbt.FamilyStore.Person;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Graph path engine: ưu tiên huyết thống, chọn đúng nhánh nội/ngoại. */
public class KinshipGraphTest {

    private static Person person(String id, String name, int gender, int birth,
                                 Person father, Person mother) {
        return new Person(id, name, gender, birth, false,
                father == null ? "" : father.id, mother == null ? "" : mother.id, "");
    }

    private static void marry(Person a, Person b) {
        a.spouseId = b.id;
        b.spouseId = a.id;
    }

    /** Đúng cây phone: Bách → Tung → Ngai → Vượng. */
    private static List<Person> phoneTree() {
        List<Person> people = new ArrayList<>();
        Person vuong = person("vuong", "Vượng", FamilyStore.MALE, 0, null, null);
        Person chat = person("chat", "Chắt", FamilyStore.MALE, 1945, null, null);
        Person ngai = person("ngai", "Ngải", FamilyStore.FEMALE, 1950, vuong, null);
        Person khoat = person("khoat", "Khoát", FamilyStore.MALE, 1940, null, null);
        Person xuyen = person("xuyen", "Xuyến", FamilyStore.FEMALE, 0, null, null);
        Person tung = person("tung", "Tùng", FamilyStore.MALE, 1980, chat, ngai);
        Person nhung = person("nhung", "Nhung", FamilyStore.FEMALE, 1977, khoat, xuyen);
        Person bach = person("bach", "Bách", FamilyStore.MALE, 2016, tung, nhung);
        Person ngan = person("ngan", "Ngạn", FamilyStore.MALE, 0, vuong, null);
        marry(chat, ngai);
        marry(khoat, xuyen);
        marry(tung, nhung);
        people.add(vuong);
        people.add(chat);
        people.add(ngai);
        people.add(khoat);
        people.add(xuyen);
        people.add(tung);
        people.add(nhung);
        people.add(bach);
        people.add(ngan);
        return people;
    }

    @Test
    public void bachToVuongIsBloodPaternalPath() {
        List<Person> people = phoneTree();
        KinshipGraph.Path path = KinshipGraph.bestPath(people, "bach", "vuong");
        assertNotNull(path);
        assertFalse(path.hasSpouse());
        assertEquals("father mother father", path.signature());
        assertEquals("cụ nội", Kinship.of(people, "bach", "vuong").term);
    }

    @Test
    public void nhungToVuongGoesThroughSpouse() {
        List<Person> people = phoneTree();
        KinshipGraph.Path path = KinshipGraph.bestPath(people, "nhung", "vuong");
        assertNotNull(path);
        assertTrue(path.hasSpouse());
        assertEquals("spouse mother father", path.signature());
        assertEquals("ông ngoại bên chồng",
                Kinship.of(people, "nhung", "vuong").term);
    }

    @Test
    public void bachToMaternalGrandfatherUsesMotherEdge() {
        List<Person> people = phoneTree();
        KinshipGraph.Path path = KinshipGraph.bestPath(people, "bach", "khoat");
        assertNotNull(path);
        assertEquals("mother father", path.signature());
        assertEquals("ông ngoại", Kinship.of(people, "bach", "khoat").term);
    }

    @Test
    public void bloodPathBeatsSpousePath() {
        Person father = person("f", "Cha", FamilyStore.MALE, 1950, null, null);
        Person root = person("r", "Mốc", FamilyStore.MALE, 1980, father, null);
        Person wife = person("w", "Vợ", FamilyStore.FEMALE, 1982, father, null);
        marry(root, wife);
        List<Person> people = new ArrayList<>();
        people.add(father);
        people.add(root);
        people.add(wife);

        KinshipGraph.Path path = KinshipGraph.bestPath(people, "r", "f");
        assertNotNull(path);
        assertFalse("Đường huyết thống phải thắng đường vợ", path.hasSpouse());
        assertEquals("father", path.signature());
    }
}
