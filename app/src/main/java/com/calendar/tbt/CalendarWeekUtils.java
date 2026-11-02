package com.calendar.tbt;

import java.util.Calendar;

/**
 * Locale-independent helpers for the Monday-first week displayed by the app and
 * the widget.
 *
 * <p>Do not use {@link Calendar#set(int, int)} with
 * {@link Calendar#DAY_OF_WEEK} to find Monday: on locales whose first day of
 * week is Sunday (e.g. {@code en-US}) that call jumps to the following week
 * when "today" is Sunday, and it can also cross week/month boundaries. The
 * arithmetic below always moves the date by a fixed number of days, so it
 * produces the same Monday regardless of the device locale.</p>
 */
final class CalendarWeekUtils {

    private CalendarWeekUtils() {
    }

    /**
     * Index of the supplied date inside a Monday-first week,
     * where Monday is 0 and Sunday is 6.
     */
    static int indexFromMonday(Calendar date) {
        return (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    /**
     * Returns a copy of {@code date} moved back to Monday of the same
     * Monday-first week. The supplied calendar is left untouched.
     */
    static Calendar startOfWeekMonday(Calendar date) {
        Calendar monday = (Calendar) date.clone();
        monday.add(Calendar.DAY_OF_YEAR, -indexFromMonday(date));
        return monday;
    }
}
