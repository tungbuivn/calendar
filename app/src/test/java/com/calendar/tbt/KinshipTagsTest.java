package com.calendar.tbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Tag xưng hô: một từ vựng dùng chung, trùng sau khi bỏ dấu, và suy ra chiều ngược lại. */
public class KinshipTagsTest {

    @Test
    public void sameWordDifferentSpellingIsOneTag() {
        assertTrue(KinshipTags.sameTerm("Chú", "  chú "));
        assertTrue(KinshipTags.sameTerm("ông nội", "  Ông  Nội "));
        assertFalse(KinshipTags.sameTerm("chú", "ông chú"));
    }

    @Test
    public void longFormSuggestsTheTagAlreadyInUse() {
        List<String> hits = KinshipTags.suggest("ông chú", Collections.<String>emptyList(), 8);
        assertTrue(hits.contains("chú"));
    }

    @Test
    public void unmarkedTypingMatchesTheMarkedTag() {
        List<String> hits = KinshipTags.suggest("cha nuoi", Collections.<String>emptyList(), 8);
        assertEquals("cha nuôi", hits.get(0));
    }

    @Test
    public void reciprocalAndGenerationComeFromTheTag() {
        assertEquals("cháu", KinshipTags.reciprocalOf("chú"));
        assertEquals("vợ", KinshipTags.reciprocalOf("chồng"));
        assertEquals("bà thông gia", KinshipTags.reciprocalOf("bà thông gia"));
        assertEquals(2, KinshipTags.generationOf("bà nội"));
        assertEquals(-1, KinshipTags.generationOf("con dâu"));
    }

    @Test
    public void unknownWordStaysUnknown() {
        assertEquals("", KinshipTags.reciprocalOf("người cùng họ"));
        assertEquals(FamilyStore.GENERATION_UNKNOWN, KinshipTags.generationOf("người cùng họ"));
        assertTrue(KinshipTags.suggest("a", Collections.<String>emptyList(), 8).isEmpty());
    }

    @Test
    public void userTagsLeadThePickerAndAreNotDuplicated() {
        List<String> vocabulary = KinshipTags.vocabulary(Arrays.asList("chú", "ông vải"));
        assertEquals("chú", vocabulary.get(0));
        assertEquals("ông vải", vocabulary.get(1));
        assertEquals(1, countOf(vocabulary, "chu"));
        assertTrue(countOf(vocabulary, "bo vo") > 0);
    }

    @Test
    public void duongIsTheMothersHusbandOnly() {
        // "Dượng" là chồng của mẹ (không sinh ra mình) nên gọi lại là "con riêng".
        assertEquals("con riêng", KinshipTags.reciprocalOf("dượng"));
        assertEquals(1, KinshipTags.generationOf("dượng"));
        assertEquals("con riêng", KinshipTags.reciprocalOf("bố dượng"));
    }

    private static int countOf(List<String> terms, String normalized) {
        int count = 0;
        for (String term : terms) {
            if (KinshipTags.normalize(term).equals(normalized)) count++;
        }
        return count;
    }
}
