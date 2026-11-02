package com.calendar.tbt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

/**
 * Regression tests for the Monday-first week calculation used by both the main
 * app and the home-screen widget.
 *
 * <p>The widget used to call {@code calendar.set(Calendar.DAY_OF_WEEK,
 * Calendar.MONDAY)}. On a Sunday-first locale (e.g. {@code en-US}) that call
 * jumps to the following Monday, so on Sundays every date in the widget row was
 * shifted forward by a week and the highlight landed on the wrong cell, while
 * the main app stayed correct. These tests pin down the locale-independent
 * behaviour.</p>
 */
public class CalendarWeekUtilsTest {

    private static Calendar date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.clear();
        calendar.set(year, month - 1, day, 12, 0, 0);
        return calendar;
    }

    @Test
    public void mapsEveryDayOfWeekToMondayFirstIndex() {
        Calendar monday = date(2024, 1, 8);
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) monday.clone();
            day.add(Calendar.DAY_OF_YEAR, i);
            assertEquals("index for " + day.getTime(), i, CalendarWeekUtils.indexFromMonday(day));
        }
    }

    @Test
    public void sundayStaysInTheSameWeek() {
        Calendar sunday = date(2025, 9, 14);
        assertEquals(6, CalendarWeekUtils.indexFromMonday(sunday));

        Calendar start = CalendarWeekUtils.startOfWeekMonday(sunday);
        assertEquals(8, start.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.SEPTEMBER, start.get(Calendar.MONTH));
        assertEquals(2025, start.get(Calendar.YEAR));
    }

    @Test
    public void weekIsTheSameForAllDaysRegardlessOfFirstDayOfWeekLocale() {
        // The helper must not depend on the device locale's firstDayOfWeek.
        Calendar reference = date(2024, 1, 8); // Monday
        for (Locale locale : new Locale[] { new Locale("en", "US"), new Locale("vi", "VN"),
                Locale.UK }) {
            Calendar day = Calendar.getInstance(locale);
            day.clear();
            day.set(2024, Calendar.JANUARY, 14, 12, 0, 0); // Sunday
            Calendar start = CalendarWeekUtils.startOfWeekMonday(day);
            assertEquals("locale " + locale, reference.get(Calendar.DAY_OF_MONTH),
                    start.get(Calendar.DAY_OF_MONTH));
            assertEquals("locale " + locale, reference.get(Calendar.MONTH),
                    start.get(Calendar.MONTH));
        }
    }

    @Test
    public void weekCanStartInThePreviousMonth() {
        Calendar wednesday = date(2025, 10, 1);
        Calendar start = CalendarWeekUtils.startOfWeekMonday(wednesday);
        assertEquals(29, start.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.SEPTEMBER, start.get(Calendar.MONTH));
        assertEquals(2025, start.get(Calendar.YEAR));
    }

    @Test
    public void weekCanStartInThePreviousYear() {
        Calendar thursday = date(2026, 1, 1);
        Calendar start = CalendarWeekUtils.startOfWeekMonday(thursday);
        assertEquals(29, start.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.DECEMBER, start.get(Calendar.MONTH));
        assertEquals(2025, start.get(Calendar.YEAR));
    }

    @Test
    public void helperDoesNotMutateInputCalendar() {
        Calendar sunday = date(2025, 9, 14);
        CalendarWeekUtils.startOfWeekMonday(sunday);
        assertEquals(14, sunday.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.SEPTEMBER, sunday.get(Calendar.MONTH));
    }
}
