package com.calendar.tbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.calendar.tbt.FamilyTreeLayout.Node;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Thuật toán xếp cây chỉ test mấy bất biến nhìn thấy trên sơ đồ: vợ chồng sát nhau,
 * cha mẹ nằm giữa đàn con, không hai ô chồng nhau, chuỗi nối méo vẫn xếp hết người.
 */
public class FamilyTreeLayoutTest {

    private static final float W = 100f;
    private static final float H = 54f;

    @Test
    public void nothingToPlaceYieldsNoRows() {
        assertTrue(FamilyTreeLayout.layout(Collections.<Node>emptyList()).isEmpty());
    }

    @Test
    public void spousesStandSideBySideInTheSameRow() {
        Node ong = node("ong", 2);
        Node ba = node("ba", 2);
        ong.spouseId = "ba";
        ba.spouseId = "ong";

        FamilyTreeLayout.Result result = FamilyTreeLayout.layout(Arrays.asList(ong, ba));

        assertEquals(ong.y, ba.y, 0.01f);
        assertEquals(1, result.rows.size());
        Node left = ong.x <= ba.x ? ong : ba;
        Node right = ong.x <= ba.x ? ba : ong;
        assertEquals(FamilyTreeLayout.SPOUSE_GAP,
                (right.x - right.width / 2) - (left.x + left.width / 2), 0.01f);
    }

    @Test
    public void parentsCenterOverTheSpanOfTheirChildren() {
        Node cha = node("cha", 1);
        Node me = node("me", 1);
        cha.spouseId = "me";
        me.spouseId = "cha";
        Node first = child("con0", cha, 2000);
        Node middle = child("con1", cha, 2001);
        Node last = child("con2", cha, 2002);

        FamilyTreeLayout.layout(Arrays.asList(cha, me, first, middle, last));

        assertEquals((first.x + last.x) / 2, (cha.x + me.x) / 2, 0.01f);
        // Cha mẹ đứng trên, không đè xuống dải con.
        assertTrue(cha.y + H / 2 < first.y - H / 2);
        assertEquals(middle.x, (cha.x + me.x) / 2, 0.01f);
    }

    @Test
    public void childrenKeepBirthOrderNotEntryOrder() {
        Node cha = node("cha", 1);
        Node anh = child("anh", cha, 1990);
        Node em = child("em", cha, 1995);
        anh.order = 1;
        em.order = 0;

        FamilyTreeLayout.layout(Arrays.asList(cha, anh, em));

        assertTrue(anh.x < em.x);
        assertEquals(anh.y, em.y, 0.01f);
    }

    @Test
    public void separateFamiliesInOneRowKeepTheirDistance() {
        Node a = node("a", 0);
        Node b = node("b", 0);
        Node c = node("c", 0);
        Node d = node("d", 0);
        a.spouseId = "b";
        b.spouseId = "a";
        c.spouseId = "d";
        d.spouseId = "c";

        FamilyTreeLayout.layout(Arrays.asList(a, b, c, d));

        List<Node> row = new ArrayList<>(Arrays.asList(a, b, c, d));
        Collections.sort(row, byCenterX());
        for (int i = 1; i < row.size(); i++) {
            Node previous = row.get(i - 1);
            Node current = row.get(i);
            float gap = (current.x - current.width / 2) - (previous.x + previous.width / 2);
            assertTrue("Hai ô chồng nhau trong cùng hàng", gap >= FamilyTreeLayout.SPOUSE_GAP - 0.01f);
        }
        // Hai gia đình riêng thì cách nhau bằng chỗ hở giữa các nhà.
        Node leftFamily = row.get(1);
        Node rightFamily = row.get(2);
        assertEquals(FamilyTreeLayout.FAMILY_GAP,
                (rightFamily.x - rightFamily.width / 2) - (leftFamily.x + leftFamily.width / 2), 0.01f);
    }

    @Test
    public void peopleWithNoKnownGenerationLandAtTheBottom() {
        Node ong = node("ong", 2);
        Node ban = node("ban", FamilyTreeLayout.GENERATION_UNPLACED);

        FamilyTreeLayout.Result result = FamilyTreeLayout.layout(Arrays.asList(ong, ban));

        assertEquals(2, result.rows.size());
        assertEquals(FamilyTreeLayout.GENERATION_UNPLACED,
                result.rows.get(result.rows.size() - 1).generation);
        assertTrue(ong.y < ban.y);
    }

    @Test
    public void reversedAndSelfLinksStillPlaceEveryone() {
        // Cha là con của con và người tự là cha mình: thuật toán phải dừng, vẫn đặt đủ ô.
        Node cha = node("cha", 1);
        Node con = node("con", 0);
        Node minh = node("minh", 0);
        cha.fatherId = "con";
        con.fatherId = "cha";
        minh.fatherId = "minh";

        FamilyTreeLayout.layout(Arrays.asList(cha, con, minh));

        for (Node node : Arrays.asList(cha, con, minh)) {
            assertFalse("Ô chưa có vị trí", Float.isNaN(node.x));
        }
        // Không có hai ô cùng hàng đè lên nhau kể cả khi chuỗi nối bị vòng.
        List<Node> sameRow = Arrays.asList(con, minh);
        Node left = con.x <= minh.x ? con : minh;
        Node right = con.x <= minh.x ? minh : con;
        assertTrue(right.x - right.width / 2 >= left.x + left.width / 2 - 0.01f);
    }

    @Test
    public void boundsCoverEveryCell() {
        Node ong = node("ong", 2);
        ong.width = 180f;
        Node cha = node("cha", 1);
        cha.fatherId = "ong";
        Node con = node("con", 0);
        con.fatherId = "cha";
        con.width = 220f;

        FamilyTreeLayout.Result result = FamilyTreeLayout.layout(Arrays.asList(ong, cha, con));

        for (Node node : Arrays.asList(ong, cha, con)) {
            assertTrue("minX cắt mất ô", result.minX <= node.x - node.width / 2 + 0.01f);
            assertTrue("maxX cắt mất ô", result.maxX >= node.x + node.width / 2 - 0.01f);
            assertTrue("maxY cắt mất ô", result.maxY >= node.y + node.height / 2 - 0.01f);
        }
    }

    @Test
    public void tallSpouseCardsGetMoreHorizontalRoom() {
        Node a = node("a", 0);
        Node b = node("b", 0);
        a.spouseId = b.id;
        b.spouseId = a.id;
        a.height = 200f;
        b.height = 200f;

        FamilyTreeLayout.layout(Arrays.asList(a, b));

        Node left = a.x <= b.x ? a : b;
        Node right = a.x <= b.x ? b : a;
        float gap = (right.x - right.width / 2f) - (left.x + left.width / 2f);
        assertTrue("Vợ/chồng vẫn quá sát: gap=" + gap + " (min=" + FamilyTreeLayout.SPOUSE_GAP + ")",
                gap > FamilyTreeLayout.SPOUSE_GAP);
    }

    @Test
    public void tallRowsKeepMoreVerticalBreathingRoom() {
        Node top = node("top", 1);
        Node bottom = node("bottom", 0);
        top.height = 200f;
        bottom.height = 200f;
        // Nối cha-con để hai người nằm ở hai hàng liên tiếp.
        bottom.fatherId = top.id;

        FamilyTreeLayout.layout(Arrays.asList(top, bottom));

        float gap = (bottom.y - bottom.height / 2f) - (top.y + top.height / 2f);
        assertTrue("Hai hàng dính sát: gap=" + gap, gap >= 0.7f * top.height);
    }

    @Test
    public void unrelatedPeopleInTheUnplacedRowNeverOverlap() {
        // Chọn một người không nối với mốc: cả nhóm rơi vào hàng "Chưa nối".
        // Cha/con trong nhóm này vẫn giữ link nhưng phải xếp cạnh nhau, không đè.
        Node parent = node("parent", FamilyTreeLayout.GENERATION_UNPLACED);
        Node child1 = node("child1", FamilyTreeLayout.GENERATION_UNPLACED);
        Node child2 = node("child2", FamilyTreeLayout.GENERATION_UNPLACED);
        child1.fatherId = parent.id;
        child2.fatherId = parent.id;

        FamilyTreeLayout.layout(Arrays.asList(parent, child1, child2));

        List<Node> row = Arrays.asList(parent, child1, child2);
        Collections.sort(row, byCenterX());
        for (int i = 1; i < row.size(); i++) {
            Node previous = row.get(i - 1);
            Node current = row.get(i);
            float gap = (current.x - current.width / 2) - (previous.x + previous.width / 2);
            assertTrue("Ô chưa nối bị đè nhau: " + gap, gap >= 0f);
        }
    }

    @Test
    public void treeFamilyUsesTidyLayout() {
        Node cha = node("cha", 1);
        Node me = node("me", 1);
        cha.spouseId = me.id;
        me.spouseId = cha.id;
        Node con = child("con", cha, 2000);
        con.motherId = me.id;

        FamilyTreeLayout.Result result =
                FamilyTreeLayout.layout(Arrays.asList(cha, me, con));

        assertTrue("Gia phả dạng cây phải dùng layout cây chuẩn", result.tidy);
    }

    @Test
    public void inLawCoupleFallsBackSafely() {
        Node pa = node("pa", 2);
        Node ma = node("ma", 2);
        pa.spouseId = ma.id;
        ma.spouseId = pa.id;
        Node pb = node("pb", 2);
        Node mb = node("mb", 2);
        pb.spouseId = mb.id;
        mb.spouseId = pb.id;
        Node chong = node("chong", 1);
        Node vo = node("vo", 1);
        chong.spouseId = vo.id;
        vo.spouseId = chong.id;
        chong.fatherId = pa.id;
        chong.motherId = ma.id;
        vo.fatherId = pb.id;
        vo.motherId = mb.id;

        FamilyTreeLayout.Result result = FamilyTreeLayout.layout(
                Arrays.asList(pa, ma, pb, mb, chong, vo));

        assertFalse("Hai bên thông gia phải dùng fallback", result.tidy);
        for (Node node : Arrays.asList(pa, ma, pb, mb, chong, vo)) {
            assertFalse("Ô chưa có vị trí", Float.isNaN(node.x));
        }
    }

    @Test
    public void tidySiblingBranchesKeepHorizontalSpansApart() {
        Node rootA = node("rootA", 2);
        Node rootB = node("rootB", 2);
        rootA.spouseId = rootB.id;
        rootB.spouseId = rootA.id;
        Node first = node("first", 1);
        Node firstSpouse = node("firstSpouse", 1);
        first.spouseId = firstSpouse.id;
        firstSpouse.spouseId = first.id;
        first.fatherId = rootA.id;
        Node second = node("second", 1);
        Node secondSpouse = node("secondSpouse", 1);
        second.spouseId = secondSpouse.id;
        secondSpouse.spouseId = second.id;
        second.fatherId = rootA.id;
        Node grand1 = child("grand1", first, 2000);
        Node grand2 = child("grand2", second, 2001);

        FamilyTreeLayout.layout(Arrays.asList(
                rootA, rootB, first, firstSpouse, second, secondSpouse, grand1, grand2));

        float firstLeft = Math.min((first.x + firstSpouse.x) / 2f, grand1.x);
        float firstRight = Math.max((first.x + firstSpouse.x) / 2f, grand1.x);
        float secondLeft = Math.min((second.x + secondSpouse.x) / 2f, grand2.x);
        float secondRight = Math.max((second.x + secondSpouse.x) / 2f, grand2.x);
        assertTrue("Hai nhánh ngang chồng nhau",
                firstRight <= secondLeft + 0.01f || secondRight <= firstLeft + 0.01f);
    }

    // ---- dựng dữ liệu -----------------------------------------------------

    private static Node node(String id, int generation) {
        Node node = new Node(id);
        node.generation = generation;
        node.width = W;
        node.height = H;
        return node;
    }

    private static Node child(String id, Node parent, int birthYear) {
        Node child = node(id, parent.generation - 1);
        child.fatherId = parent.id;
        child.birthYear = birthYear;
        return child;
    }

    private static Comparator<Node> byCenterX() {
        return new Comparator<Node>() {
            @Override
            public int compare(Node left, Node right) {
                return Float.compare(left.x, right.x);
            }
        };
    }
}
