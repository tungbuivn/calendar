package com.calendar.tbt;

import static org.junit.Assert.assertEquals;

import com.calendar.tbt.FamilyStore.Person;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/** Chứng minh term hiển thị lấy từ bảng JSON, không phải hard-code trong code. */
public class KinshipTermsTest {

    @Test
    public void jsonOverridesDisplayTerm() throws Exception {
        File file = File.createTempFile("kinship_terms", ".json");
        Files.write(file.toPath(), "{\"cha\":\"bố\"}".getBytes(StandardCharsets.UTF_8));
        KinshipTerms.load(file);
        try {
            Person father = new Person("cha", "Cha", FamilyStore.MALE, 1960, false,
                    "", "", "");
            Person root = new Person("toi", "Tôi", FamilyStore.MALE, 1985, false,
                    "cha", "", "");
            assertEquals("bố", Kinship.of(Arrays.asList(father, root), "toi", "cha").term);
        } finally {
            KinshipTerms.clear();
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        Person father = new Person("cha", "Cha", FamilyStore.MALE, 1960, false,
                "", "", "");
        Person root = new Person("toi", "Tôi", FamilyStore.MALE, 1985, false,
                "cha", "", "");
        assertEquals("cha", Kinship.of(Arrays.asList(father, root), "toi", "cha").term);
    }
}
