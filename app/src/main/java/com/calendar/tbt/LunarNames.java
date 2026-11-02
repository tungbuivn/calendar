package com.calendar.tbt;

/** Cách đọc ngày, tháng âm lịch bằng lời trong văn khấn. */
public final class LunarNames {

    private static final String[] MONTH = { "Giêng", "Hai", "Ba", "Tư", "Năm", "Sáu",
            "Bảy", "Tám", "Chín", "Mười", "Một", "Chạp" };
    private static final String[] UNIT = { "", "một", "hai", "ba", "bốn", "năm", "sáu",
            "bảy", "tám", "chín" };

    private LunarNames() {
    }

    /** "mồng một"… "mồng mười", "rằm", "hai mươi ba"… (không kèm chữ "ngày"). */
    public static String day(int day) {
        if (day < 1 || day > 30) return String.valueOf(day);
        if (day <= 10) return "mồng " + (day == 10 ? "mười" : UNIT[day]);
        if (day == 15) return "rằm";
        if (day < 20) return "mười " + UNIT[day - 10];
        if (day == 20) return "hai mươi";
        if (day == 21) return "hai mươi mốt";
        if (day < 30) return "hai mươi " + (day == 25 ? "lăm" : UNIT[day - 20]);
        return "ba mươi";
    }

    /** "Bảy" hoặc "Bảy nhuận" (không kèm chữ "tháng"). */
    public static String month(int month, boolean leap) {
        if (month < 1 || month > 12) return String.valueOf(month);
        return MONTH[month - 1] + (leap ? " nhuận" : "");
    }
}
