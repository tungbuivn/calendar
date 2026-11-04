package com.calendar.tbt;

import java.util.ArrayList;
import java.util.List;

/**
 * Chặt toàn văn thành từng mảng ngắn cho máy đọc theo từng hơi, kèm vị trí của mỗi mảng
 * trong chuỗi đang hiển thị để tô sáng đúng chỗ đang đọc.
 *
 * <p>Toàn văn trong sách đã bỏ chú thích trong ngoặc rồi, nên ở đây chỉ cần xử lý
 * gạch đầu dòng và dấu chấm lửng của chỗ trống.
 */
public final class SpeechText {

    /** Một hơi đọc: ngắn để theo kịp bằng mắt, đủ dài cho một câu văn khấn. */
    public static final int MAX_PIECE = 160;

    /** Chữ đưa ra loa + phạm vi nó chiếm trong toàn văn hiển thị. */
    public static final class Piece {
        public final String spoken;
        public final int start;
        public final int end;

        Piece(String spoken, int start, int end) {
            this.spoken = spoken;
            this.start = start;
            this.end = end;
        }
    }

    private static final String TERMINATORS = ".;!?…";

    private SpeechText() {
    }

    /** Mảng đọc theo thứ tự xuất hiện; dòng trống và gạch đầu dòng không thành mảng. */
    public static List<Piece> split(String display) {
        List<Piece> pieces = new ArrayList<>();
        if (display == null || display.isEmpty()) return pieces;
        List<int[]> pending = new ArrayList<>();
        int position = 0;
        while (true) {
            int newline = display.indexOf('\n', position);
            int lineEnd = newline < 0 ? display.length() : newline;
            collect(display, position, lineEnd, pending);
            // Hết dòng là chốt một hơi để chỗ tô sáng bám đúng dòng đang đọc.
            flush(pieces, pending, display);
            if (newline < 0) break;
            position = newline + 1;
        }
        return pieces;
    }

    /** Gom các câu trong một dòng; dòng trống không thêm câu nào. */
    private static void collect(String display, int from, int to, List<int[]> pending) {
        int sentenceStart = contentStart(display, from, to);
        int cursor = sentenceStart;
        while (cursor < to) {
            char terminator = display.charAt(cursor);
            if (terminator == '…') {
                // Dấu chấm lửng là chỗ trống/pause trong cùng đoạn, không phải điểm kết đoạn.
                cursor++;
                while (cursor < to && display.charAt(cursor) == '…') cursor++;
                continue;
            }
            if (isTerminator(terminator)) {
                int run = cursor;
                while (run < to && isTerminator(display.charAt(run))) run++;
                add(display, sentenceStart, run, pending);
                cursor = skipSpace(display, run, to);
                sentenceStart = cursor;
                continue;
            }
            cursor++;
        }
        add(display, sentenceStart, to, pending);
    }

    /** Cắt câu quá dài theo dấu phẩy, hết dấu phẩy thì theo khoảng trắng. */
    private static void add(String display, int from, int to, List<int[]> pending) {
        int start = skipSpace(display, from, to);
        int end = trimEnd(display, start, to);
        while (end - start > MAX_PIECE) {
            int cut = breakBefore(display, start + MAX_PIECE, start);
            if (cut <= start) cut = start + MAX_PIECE;
            int pieceEnd = trimEnd(display, start, cut);
            if (pieceEnd > start) pending.add(new int[]{start, pieceEnd});
            start = skipSpace(display, cut, end);
        }
        if (end > start) pending.add(new int[]{start, end});
    }

    /** Dán các câu lại thành hơi đọc không quá MAX_PIECE, bỏ mảng không có chữ. */
    private static void flush(List<Piece> pieces, List<int[]> pending, CharSequence display) {
        for (int[] range : pending) {
            emit(pieces, display.subSequence(range[0], range[1]).toString(), range[0], range[1]);
        }
        pending.clear();
    }

    private static void emit(List<Piece> pieces, String raw, int start, int end) {
        String spoken = spoken(raw);
        if (spoken.isEmpty() || !hasWords(spoken)) return;
        pieces.add(new Piece(spoken, start, end));
    }

    /**
     * Chữ đưa ra loa: dấu chấm lửng của chỗ trống biến thành một lần ngừng,
     * khoảng trắng thừa gom lại một.
     */
    public static String spoken(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder(raw.length());
        boolean inDots = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '…') {
                inDots = true;
                continue;
            }
            if (inDots) {
                inDots = false;
                pause(out);
            }
            if (Character.isWhitespace(c)) {
                if (out.length() > 0 && out.charAt(out.length() - 1) == ' ') continue;
                out.append(' ');
                continue;
            }
            if (c == ',' && endsWithPause(out)) continue;
            out.append(c);
        }
        if (inDots) pause(out);
        return out.toString().trim();
    }

    /** Thêm một lần ngừng, không đếm hai lần liền. */
    private static void pause(StringBuilder out) {
        if (!endsWithPause(out)) out.append(',');
    }

    private static boolean endsWithPause(StringBuilder out) {
        for (int i = out.length() - 1; i >= 0; i--) {
            char c = out.charAt(i);
            if (c == ' ') continue;
            return c == ',';
        }
        return false;
    }

    // ---- đo chỉ mục -------------------------------------------------------

    /** Bỏ gạch đầu dòng "- " ở đầu dòng vì máy đọc sẽ gọi tên dấu. */
    private static int contentStart(String display, int from, int to) {
        int i = from;
        while (i < to && (display.charAt(i) == '-' || display.charAt(i) == '*')) i++;
        return skipSpace(display, i, to);
    }

    private static int skipSpace(String display, int from, int to) {
        int i = from;
        while (i < to && isIgnorable(display.charAt(i))) i++;
        return i;
    }

    private static int trimEnd(String display, int from, int to) {
        int i = to;
        while (i > from && isIgnorable(display.charAt(i - 1))) i--;
        return i;
    }

    private static boolean isIgnorable(char c) {
        return Character.isWhitespace(c) || c == '-' || c == '*';
    }

    private static boolean isTerminator(char c) {
        return TERMINATORS.indexOf(c) >= 0;
    }

    /** Chỗ ngắt gần nhất không quá {@code limit}, ưu tiên dấu phẩy rồi khoảng trắng. */
    private static int breakBefore(String display, int limit, int from) {
        int index = Math.min(limit, display.length() - 1);
        for (int i = index; i > from; i--) {
            char c = display.charAt(i);
            if (c == ',' || c == ';' || c == ':') return i + 1;
        }
        for (int i = index; i > from; i--) {
            if (Character.isWhitespace(display.charAt(i))) return i + 1;
        }
        return 0;
    }

    private static boolean hasWords(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) return true;
        }
        return false;
    }
}
