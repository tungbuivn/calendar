package com.calendar.tbt;

import java.util.Calendar;
import java.util.ArrayList;

class LuckyHour {
    String name;
    ArrayList<Integer> time = new ArrayList<>();
    
    @Override
    public String toString() {
        if (time.size() >= 2) {
            return name + " (" + time.get(0) + "-" + time.get(1) + ")";
        } else if (time.size() == 1) {
            return name + " (" + time.get(0) + ")";
        } else {
            return name;
        }
    }
}

/**
 * Lunar Calendar Calculator
 * Ported from C# code based on
 * http://www.informatik.uni-leipzig.de/~duc/amlich/VietCalendar.java
 * Original C# author: nghiaht (nghiaht.github.io)
 */
public class LunarCalendar {

    private static final double TIMEZONE = 7.0; // Vietnam timezone

    private static final String[] WEEKDAYS = { "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật" };
    private static final String[] HEAVENLY_STEMS = { "Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm",
            "Quý" };
    private static final String[] EARTHLY_BRANCHES = { "Tí", "Sửu", "Dần", "Mão", "Thìn", "Tị", "Ngọ", "Mùi", "Thân",
            "Dậu", "Tuất", "Hợi" };
    private static final String[] SOLAR_TERMS = {
            "Xuân phân", "Thanh minh", "Cốc vũ", "Lập hạ", "Tiểu mãn", "Mang chủng",
            "Hạ chí", "Tiểu thử", "Đại thử", "Lập thu", "Xử thử", "Bạch lộ",
            "Thu phân", "Hàn lộ", "Sương giáng", "Lập đông", "Tiểu tuyết", "Đại tuyết",
            "Đông chí", "Tiểu hàn", "Đại hàn", "Lập xuân", "Vũ Thủy", "Kinh trập"
    };

    /**
     * Convert date to Julian Day Number
     * 
     * @param dd day
     * @param mm month
     * @param yy year
     * @return Julian Day Number
     */
    public static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        }
        return jd;
    }

    // TODO: Tên giờ và thêm cả giờ hắc đạo
    public static ArrayList<LuckyHour> getLuckyHours(int jd) {
        // Temporarily commented out to fix build
        return new ArrayList<>();
    }

    /**
     * Convert Julian Day Number to date
     * 
     * @param jd Julian Day Number
     * @return array [day, month, year]
     */
    public static int[] jdToDate(int jd) {
        int a, b, c;
        if (jd > 2299160) { // After 5/10/1582, Gregorian calendar
            a = jd + 32044;
            b = (4 * a + 3) / 146097;
            c = a - (b * 146097) / 4;
        } else {
            b = 0;
            c = jd + 32082;
        }
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        int day = e - (153 * m + 2) / 5 + 1;
        int month = m + 3 - 12 * (m / 10);
        int year = b * 100 + d - 4800 + m / 10;
        return new int[] { day, month, year };
    }

    /**
     * Sun longitude in degrees
     * Algorithm from: Astronomical Algorithms, by Jean Meeus, 1998
     * 
     * @param jdn Julian Day Number
     * @return sun longitude in degrees
     */
    public static double SunLongitude(double jdn) {
        return SunLongitudeAA98(jdn);
    }

    public static double SunLongitudeAA98(double jdn) {
        double T = (jdn - 2451545.0) / 36525; // Time in Julian centuries from 2000-01-01 12:00:00 GMT
        double T2 = T * T;
        double dr = Math.PI / 180; // degree to radian
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2; // mean anomaly, degree
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2; // mean longitude, degree
        double DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
        DL = DL + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.000290 * Math.sin(dr * 3 * M);
        double L = L0 + DL; // true longitude, degree
        L = L - 360 * (int) (L / 360); // Normalize to (0, 360)
        return L;
    }

    public static double NewMoon(int k) {
        return NewMoonAA98(k);
    }

    /**
     * Julian day number of the kth new moon after (or before) the New Moon of
     * 1900-01-01 13:51 GMT.
     * Accuracy: 2 minutes
     * Algorithm from: Astronomical Algorithms, by Jean Meeus, 1998
     * 
     * @param k
     * @return the Julian date number of the New Moon
     */
    public static double NewMoonAA98(int k) {
        double T = k / 1236.85; // Time in Julian centuries from 1900 January 0.5
        double T2 = T * T;
        double T3 = T2 * T;
        double dr = Math.PI / 180;
        double Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
        Jd1 = Jd1 + 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr); // Mean new moon
        double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3; // Sun's mean anomaly
        double Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3; // Moon's mean anomaly
        double F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3; // Moon's argument of latitude
        double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
        C1 = C1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
        C1 = C1 - 0.0004 * Math.sin(dr * 3 * Mpr);
        C1 = C1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mpr));
        C1 = C1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
        C1 = C1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
        C1 = C1 + 0.0010 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
        double deltat;
        if (T < -11) {
            deltat = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3;
        } else {
            deltat = -0.000278 + 0.000265 * T + 0.000262 * T2;
        }
        double JdNew = Jd1 + C1 - deltat;
        return JdNew;
    }

    public static int INT(double d) {
        return (int) Math.floor(d);
    }

    public static double getSunLongitude(int dayNumber, double timeZone) {
        return SunLongitude(dayNumber - 0.5 - timeZone / 24);
    }

    public static int getNewMoonDay(int k, double timeZone) {
        double jd = NewMoon(k);
        return INT(jd + 0.5 + timeZone / 24);
    }

    public static int getLunarMonth11(int yy, double timeZone) {
        double off = jdFromDate(31, 12, yy) - 2415021.076998695;
        int k = INT(off / 29.530588853);
        int nm = getNewMoonDay(k, timeZone);
        int sunLong = INT(getSunLongitude(nm, timeZone) / 30);
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone);
        }
        return nm;
    }

    public static int getLeapMonthOffset(int a11, double timeZone) {
        int k = INT(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int last; // Month 11 contains point of sun longitude 3*PI/2 (December solstice)
        int i = 1; // We start with the month following lunar month 11
        int arc = INT(getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone) / 30);
        do {
            last = arc;
            i++;
            arc = INT(getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone) / 30);
        } while (arc != last && i < 14);
        return i - 1;
    }

    /**
     * Convert solar date to lunar date
     * 
     * @param dd       day
     * @param mm       month
     * @param yy       year
     * @param timeZone timezone
     * @return array [lunarDay, lunarMonth, lunarYear, leapOrNot]
     */
    public static int[] convertSolar2Lunar(int dd, int mm, int yy, double timeZone) {
        int lunarDay, lunarMonth, lunarYear, lunarLeap;
        int dayNumber = jdFromDate(dd, mm, yy);
        int k = INT((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = getNewMoonDay(k + 1, timeZone);
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone);
        }
        int a11 = getLunarMonth11(yy, timeZone);
        int b11 = a11;
        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = getLunarMonth11(yy - 1, timeZone);
        } else {
            lunarYear = yy + 1;
            b11 = getLunarMonth11(yy + 1, timeZone);
        }
        lunarDay = dayNumber - monthStart + 1;
        int diff = INT((monthStart - a11) / 29);
        lunarLeap = 0;
        lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapMonthDiff = getLeapMonthOffset(a11, timeZone);
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10;
                if (diff == leapMonthDiff) {
                    lunarLeap = 1;
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth = lunarMonth - 12;
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1;
        }
        return new int[] { lunarDay, lunarMonth, lunarYear, lunarLeap };
    }

    public static int[] convertLunar2Solar(int lunarDay, int lunarMonth, int lunarYear, int lunarLeap,
            double timeZone) {
        int a11, b11;
        if (lunarMonth < 11) {
            a11 = getLunarMonth11(lunarYear - 1, timeZone);
            b11 = getLunarMonth11(lunarYear, timeZone);
        } else {
            a11 = getLunarMonth11(lunarYear, timeZone);
            b11 = getLunarMonth11(lunarYear + 1, timeZone);
        }
        int k = INT(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int off = lunarMonth - 11;
        if (off < 0) {
            off += 12;
        }
        if (b11 - a11 > 365) {
            int leapOff = getLeapMonthOffset(a11, timeZone);
            int leapMonth = leapOff - 2;
            if (leapMonth < 0) {
                leapMonth += 12;
            }
            if (lunarLeap != 0 && lunarMonth != leapMonth) {
                return new int[] { 0, 0, 0 };
            } else if (lunarLeap != 0 || off >= leapOff) {
                off += 1;
            }
        }
        int monthStart = getNewMoonDay(k + off, timeZone);
        return jdToDate(monthStart + lunarDay - 1);
    }

    /**
     * Get lunar date for given solar date
     * 
     * @param day   solar day
     * @param month solar month
     * @param year  solar year
     * @return LunarDate object
     */
    public static LunarDate getLunarDate(int day, int month, int year) {
        if (day < 1 || day > 31 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid date values");
        }
        if (year < 1900) {
            throw new IllegalArgumentException("Year must be >= 1900");
        }

        // Kiểm tra ngày hợp lệ cho từng tháng
        if (month == 2) {
            boolean isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
            if (day > (isLeap ? 29 : 28)) {
                throw new IllegalArgumentException("Invalid date: " + day + "/" + month + "/" + year);
            }
        } else if (month == 4 || month == 6 || month == 9 || month == 11) {
            if (day > 30) {
                throw new IllegalArgumentException("Invalid date: " + day + "/" + month + "/" + year);
            }
        }

        int[] lunar = convertSolar2Lunar(day, month, year, TIMEZONE);
        return new LunarDate(lunar[2], lunar[1], lunar[0], lunar[3] == 1, jdFromDate(day, month, year));
    }

    /**
     * Get current lunar date
     * 
     * @return LunarDate object for current date
     */
    public static LunarDate getCurrentLunarDate() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        return getLunarDate(day, month, year);
    }

    /**
     * Lunar date class
     */
    public static class LunarDate {
        public final int year;
        public final int month;
        public final int day;
        public final boolean isLeap;
        public final int jd;

        public LunarDate(int year, int month, int day, boolean isLeap, int jd) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.isLeap = isLeap;
            this.jd = jd;
        }

        @Override
        public String toString() {
            return String.format("%d / %d", day, month);
        }

        /**
         * Return month's name in Sexagenary cycle (Can Chi).
         * 
         * @returns month's name in Sexagenary cycle
         */
        public String getMonthName() {
            return Constants.CAN[(this.year * 12 + this.month + 3) % 10] + " "
                    + Constants.CHI[(this.month + 1) % 12]
                    + (this.isLeap ? " (nhuận)" : "");
        }

        /**
         * Return day's name in Sexagenary cycle (Can Chi).
         * 
         * @returns day's name in Sexagenary cycle
         */
        public String getDayName() {
            return Constants.CAN[(this.jd + 9) % 10] + " "
                    + Constants.CHI[(this.jd + 1) % 12];
        }

        /**
         * Return year's name in Sexagenary cycle (Can Chi).
         * 
         * @returns year's name in Sexagenary cycle
         */
        public String getYearName() {
            return Constants.CAN[(this.year + 6) % 10] + " "
                    + Constants.CHI[(this.year + 8) % 12];
        }

        /**
         * Return hour's name in Sexagenary cycle (Can Chi). Heavenly stem is set to
         * 'Ty'.
         * 
         * @returns hour's name in Sexagenary cycle
         */
        public String getHourName() {
            return Constants.CAN[(this.jd - 1) * 2 % 10] + " " + Constants.CHI[0];
        }

        /**
         * Get Solar Term (Tiết Khí).
         * 
         * @returns solar term
         */
        public String getSolarTerm() {
            
            int jd1=this.jd+1;
            double mjd= (jd1 - 0.5 - TIMEZONE / 24.0) ;
            double sunLongitude = SunLongitude(mjd);    
            // Convert sun longitude (0-360 degrees) to solar term index (0-23)
            // Each solar term covers 15 degrees (360/24 = 15)
            int termIndex = (int)(sunLongitude / 15.0) % 24;
            android.util.Log.d("===========",String.format("Solar Term jd: %d, mjd: %f, sunLong: %.2f, termIndex: %d", this.jd, mjd, sunLongitude, termIndex));
            return Constants.SOLAR_TERMS[termIndex];
        }
         /**
     * Get lucky hours of the day.
     * @returns luck hours
     */
    // TODO: Tên giờ và thêm cả giờ hắc đạo
    public String getLuckyHours() {
        // const jd = this.jd;
        int chiOfDay = (jd + 1) % 12;
        String gioHD = Constants.LUCKY_HOURS[chiOfDay % 6];

        ArrayList<LuckyHour> zodiacHours = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            if (gioHD.charAt(i) == '1') {
                LuckyHour zodiac = new LuckyHour();
                zodiac.name = Constants.CHI[i];
                // Calculate time range for each zodiac hour
                // Each zodiac hour covers 2 hours
                int startHour = (i * 2 + 23) % 24;  // Start hour (23, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21)
                int endHour = (i * 2 + 1) % 24;     // End hour (1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23)
                zodiac.time.add(startHour);
                zodiac.time.add(endHour);
                zodiacHours.add(zodiac);
            }
        }
        for (LuckyHour zodiac : zodiacHours) {
            android.util.Log.d("===========",String.format("Lucky Hour: %s, %s", zodiac.name, zodiac.time));
        }
        // join zodiacHours with comma
        String result = String.join(", ", zodiacHours.stream().map(LuckyHour::toString).toArray(String[]::new));
        return result;
    }

        /**
         * Get Auspicious Hours (Giờ Hoàng Đạo).
         * 
         * @returns auspicious hours in format: <hour_name> (start-end)
         */
        public String getAuspiciousHours() {
            // Get the day's earthly branch (Chi) to determine auspicious hours
            int dayChi = (this.jd + 1) % 12;
            
            // Auspicious hours mapping based on the day's earthly branch
            // Format: <hour_name> (start-end)
            String[] auspiciousHours = {
                "Tí (23-1)",      // For days with Tí (Rat)
                "Sửu (1-3)",      // For days with Sửu (Ox)
                "Dần (3-5)",      // For days with Dần (Tiger)
                "Mão (5-7)",      // For days with Mão (Rabbit)
                "Thìn (7-9)",     // For days with Thìn (Dragon)
                "Tị (9-11)",      // For days with Tị (Snake)
                "Ngọ (11-13)",    // For days with Ngọ (Horse)
                "Mùi (13-15)",    // For days with Mùi (Goat)
                "Thân (15-17)",   // For days with Thân (Monkey)
                "Dậu (17-19)",    // For days with Dậu (Rooster)
                "Tuất (19-21)",   // For days with Tuất (Dog)
                "Hợi (21-23)"     // For days with Hợi (Pig)
            };
            
            return auspiciousHours[dayChi];
        }
    }
}