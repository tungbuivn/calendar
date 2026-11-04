package com.calendar.tbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/** Kiểm tra bộ cắt toàn văn thành hơi đọc: phạm vi đúng, hơi ngắn, dấu câu vừa tai. */
public class SpeechTextTest {

    private static String textOf(List<SpeechText.Piece> pieces) {
        StringBuilder out = new StringBuilder();
        for (SpeechText.Piece piece : pieces) out.append(piece.spoken).append(" | ");
        return out.toString();
    }

    @Test
    public void nothingToReadYieldsNoPieces() {
        assertTrue(SpeechText.split(null).isEmpty());
        assertTrue(SpeechText.split("").isEmpty());
        // Dòng chỉ có dấu chấm lửng của chỗ trống thì không có chữ để đọc.
        assertTrue(SpeechText.split("…………").isEmpty());
    }

    @Test
    public void shortLinesBecomeOnePiecePerParagraph() {
        List<SpeechText.Piece> pieces = SpeechText.split("Con tên là Nguyễn Văn A.\nNgụ tại Hà Nội.");
        assertEquals(textOf(pieces), 2, pieces.size());
        assertEquals("Con tên là Nguyễn Văn A.", pieces.get(0).spoken);
        assertEquals("Ngụ tại Hà Nội.", pieces.get(1).spoken);
    }

    @Test
    public void bulletMarksAreNotReadOutLoud() {
        List<SpeechText.Piece> pieces = SpeechText.split("- Hương, hoa, trầu cau.\n* Nước sạch.");
        assertEquals(2, pieces.size());
        assertEquals("Hương, hoa, trầu cau.", pieces.get(0).spoken);
        assertEquals("Nước sạch.", pieces.get(1).spoken);
    }

    @Test
    public void semicolonAlsoEndsAPiece() {
        List<SpeechText.Piece> pieces = SpeechText.split("Câu một; câu hai.\nCâu ba");
        assertEquals(3, pieces.size());
        assertEquals("Câu một;", pieces.get(0).spoken);
        assertEquals("câu hai.", pieces.get(1).spoken);
        assertEquals("Câu ba", pieces.get(2).spoken);
    }

    @Test
    public void blankBecomesASinglePause() {
        List<SpeechText.Piece> pieces = SpeechText.split("Tín chủ con là…………, ngụ tại………… thôi.");
        assertEquals(1, pieces.size());
        // Mỗi chỗ trống chỉ ngừng một lần, không đọc "chấm chấm chấm".
        assertEquals("Tín chủ con là, ngụ tại, thôi.", pieces.get(0).spoken);
        assertFalse(pieces.get(0).spoken.contains("…"));
    }

    @Test
    public void rangesCoverTheDisplayedTextInOrder() {
        String display = "Hôm nay là ngày mùng 1 tháng Giêng năm Bính Ngọ.\n"
                + "Con kính lạy Đức Phật, kính lạy Tổ tiên, kính lạy thần linh.\n";
        List<SpeechText.Piece> pieces = SpeechText.split(display);
        assertFalse(pieces.isEmpty());
        int previousEnd = -1;
        for (SpeechText.Piece piece : pieces) {
            assertTrue(piece.start >= 0);
            assertTrue(piece.end > piece.start);
            assertTrue(piece.end <= display.length());
            // Phạm vi sau phải nối tiếp phạm vi trước, không chồng và không nhảy lùi.
            assertTrue(piece.start > previousEnd);
            previousEnd = piece.end;
            String shown = display.substring(piece.start, piece.end);
            // Chữ đọc được lấy nguyên từ đúng phạm vi đó, chỉ khác dấu ngừng và khoảng trắng.
            assertEquals(countLetters(shown), countLetters(piece.spoken));
        }
    }

    @Test
    public void longParagraphIsCutIntoBreaths() {
        StringBuilder line = new StringBuilder("Lạy Đức Phật Thích Ca Mâu Ni, con xin kính lạy người");
        while (line.length() <= SpeechText.MAX_PIECE * 3) {
            line.append(", cầu xin gia đình an lành, công việc hanh thông, mọi sự như ý");
        }
        List<SpeechText.Piece> pieces = SpeechText.split(line.toString());
        assertTrue(pieces.size() >= 3);
        for (SpeechText.Piece piece : pieces) {
            assertTrue("Hơi đọc quá dài: " + piece.spoken, piece.spoken.length() <= SpeechText.MAX_PIECE);
        }
    }

    private static int countLetters(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) total++;
        }
        return total;
    }
}
