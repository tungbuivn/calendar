package com.calendar.tbt;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Calendar;
import java.util.Locale;

/**
 * Month view with the solar and Vietnamese lunar calendars.
 *
 * <p>A compact month grid sits on top; the selected day's lunar details are
 * shown in a card below instead of a modal dialog. Tapping a day updates the
 * details, and "use this date" returns the picked date to {@link MainActivity}.</p>
 */
public class MonthCalendarActivity extends AppCompatActivity {

    public static final String EXTRA_YEAR = "com.calendar.tbt.extra.YEAR";
    public static final String EXTRA_MONTH = "com.calendar.tbt.extra.MONTH"; // 1..12
    public static final String EXTRA_DAY = "com.calendar.tbt.extra.DAY";

    private static final int WEEKS = 6;
    private static final int DAYS = 7;
    private static final int ROW_HEIGHT_DP = 50;

    private final Calendar displayed = Calendar.getInstance();
    private Calendar selected = Calendar.getInstance();
    private boolean vietnamese;

    private LinearLayout grid;
    private LinearLayout weekHeader;
    private TextView monthTitle;
    private TextView monthSubtitle;
    private TextView selectedDateTitle;
    private TextView selectedLunarLine;
    private TextView selectedSolarTerm;
    private TextView selectedLuckyHours;
    private TextView todayButton;
    private TextView useDateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_calendar);

        vietnamese = !"en".equalsIgnoreCase(new WidgetPreferences(this).getLanguage());

        int year = getIntent().getIntExtra(EXTRA_YEAR, -1);
        int month = getIntent().getIntExtra(EXTRA_MONTH, -1);
        int day = getIntent().getIntExtra(EXTRA_DAY, -1);
        if (year > 0 && month > 0 && day > 0) {
            selected.set(year, month - 1, day, 12, 0, 0);
            displayed.set(year, month - 1, 1, 12, 0, 0);
        } else {
            selected = Calendar.getInstance();
            displayed.setTimeInMillis(selected.getTimeInMillis());
            displayed.set(Calendar.DAY_OF_MONTH, 1);
        }

        // The content is light; use dark status-bar icons so they stay visible.
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        setupToolbar();
        bindViews();
        bindActions();

        render();
        updateDetails();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(vietnamese ? "Lịch tháng" : "Month calendar");

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        params.topMargin = statusBarHeight;
        toolbar.setLayoutParams(params);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        grid = findViewById(R.id.month_grid);
        weekHeader = findViewById(R.id.week_header);
        monthTitle = findViewById(R.id.month_year_title);
        monthSubtitle = findViewById(R.id.month_subtitle);
        selectedDateTitle = findViewById(R.id.selected_date_title);
        selectedLunarLine = findViewById(R.id.selected_lunar_line);
        selectedSolarTerm = findViewById(R.id.selected_solar_term);
        selectedLuckyHours = findViewById(R.id.selected_lucky_hours);
        todayButton = findViewById(R.id.today_button);
        useDateButton = findViewById(R.id.use_date_button);
    }

    private void bindActions() {
        findViewById(R.id.prev_month).setOnClickListener(v -> {
            displayed.add(Calendar.MONTH, -1);
            render();
        });
        findViewById(R.id.next_month).setOnClickListener(v -> {
            displayed.add(Calendar.MONTH, 1);
            render();
        });
        todayButton.setOnClickListener(v -> {
            selected = Calendar.getInstance();
            displayed.setTimeInMillis(selected.getTimeInMillis());
            displayed.set(Calendar.DAY_OF_MONTH, 1);
            render();
            updateDetails();
        });
        useDateButton.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra(EXTRA_YEAR, selected.get(Calendar.YEAR));
            result.putExtra(EXTRA_MONTH, selected.get(Calendar.MONTH) + 1);
            result.putExtra(EXTRA_DAY, selected.get(Calendar.DAY_OF_MONTH));
            setResult(RESULT_OK, result);
            finish();
        });

        todayButton.setText(vietnamese ? "Hôm nay" : "Today");
        useDateButton.setText(vietnamese ? "Chọn ngày này" : "Use this date");
    }

    private void render() {
        int year = displayed.get(Calendar.YEAR);
        int month = displayed.get(Calendar.MONTH); // 0-based
        monthTitle.setText(monthTitle(month + 1, year));

        LunarCalendar.LunarDate firstLunar = LunarCalendar.getLunarDate(1, month + 1, year);
        monthSubtitle.setText(vietnamese
                ? "Âm lịch: tháng " + firstLunar.month + (firstLunar.isLeap ? " (nhuận)" : "")
                : "Lunar month: " + firstLunar.month + (firstLunar.isLeap ? " (leap)" : ""));

        int weekendColor = ContextCompat.getColor(this, R.color.weekendColor);
        int primaryColor = ContextCompat.getColor(this, R.color.textPrimary);
        int secondaryColor = ContextCompat.getColor(this, R.color.textSecondary);

        renderWeekHeader(weekendColor, secondaryColor);
        grid.removeAllViews();

        Calendar first = Calendar.getInstance();
        first.clear();
        first.set(year, month, 1, 12, 0, 0);
        Calendar cursor = CalendarWeekUtils.startOfWeekMonday(first);
        Calendar today = Calendar.getInstance();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int week = 0; week < WEEKS; week++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(ROW_HEIGHT_DP)));

            for (int day = 0; day < DAYS; day++) {
                int cellYear = cursor.get(Calendar.YEAR);
                int cellMonth = cursor.get(Calendar.MONTH) + 1;
                int cellDay = cursor.get(Calendar.DAY_OF_MONTH);
                boolean inMonth = cellYear == year && (cellMonth - 1) == month;
                boolean isToday = sameDay(cursor, today);
                boolean isSelected = sameDay(cursor, selected);
                boolean isWeekend = cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                        || cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

                View cell = inflater.inflate(R.layout.item_month_day, row, false);
                TextView solar = cell.findViewById(R.id.cell_solar);
                TextView lunar = cell.findViewById(R.id.cell_lunar);

                solar.setText(String.valueOf(cellDay));
                LunarCalendar.LunarDate lunarDate = LunarCalendar.getLunarDate(cellDay, cellMonth, cellYear);
                if (lunarDate.day == 1) {
                    lunar.setText(String.format(Locale.US, "1/%d", lunarDate.month));
                } else {
                    lunar.setText(String.valueOf(lunarDate.day));
                }

                if (isToday) {
                    solar.setBackgroundResource(R.drawable.today_circle_background);
                    solar.setTextColor(0xFFFFFFFF);
                    lunar.setTextColor(isWeekend ? weekendColor : secondaryColor);
                    cell.setAlpha(1f);
                } else if (isSelected) {
                    solar.setBackgroundResource(R.drawable.selected_circle_background);
                    solar.setTextColor(isWeekend ? weekendColor : primaryColor);
                    lunar.setTextColor(isWeekend ? weekendColor : secondaryColor);
                    cell.setAlpha(inMonth ? 1f : 0.5f);
                } else {
                    solar.setBackgroundResource(0);
                    solar.setTextColor(isWeekend ? weekendColor : primaryColor);
                    lunar.setTextColor(isWeekend ? weekendColor : secondaryColor);
                    cell.setAlpha(inMonth ? 1f : 0.35f);
                }

                final int selectedYear = cellYear;
                final int selectedMonth = cellMonth;
                final int selectedDay = cellDay;
                cell.setOnClickListener(v -> selectDate(selectedYear, selectedMonth, selectedDay));

                row.addView(cell);
                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }
            grid.addView(row);
        }
    }

    private void renderWeekHeader(int weekendColor, int secondaryColor) {
        weekHeader.removeAllViews();
        String[] labels = vietnamese
                ? new String[] { "T2", "T3", "T4", "T5", "T6", "T7", "CN" }
                : new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        for (int i = 0; i < labels.length; i++) {
            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            label.setGravity(Gravity.CENTER);
            label.setText(labels[i]);
            label.setTextSize(12);
            label.setTypeface(null, Typeface.BOLD);
            label.setLetterSpacing(0.08f);
            label.setPadding(0, dp(4), 0, dp(4));
            label.setTextColor(i >= 5 ? weekendColor : secondaryColor);
            weekHeader.addView(label);
        }
    }

    private void selectDate(int year, int month, int day) {
        selected.set(year, month - 1, day, 12, 0, 0);
        render();
        updateDetails();
    }

    private void updateDetails() {
        int year = selected.get(Calendar.YEAR);
        int month = selected.get(Calendar.MONTH) + 1;
        int day = selected.get(Calendar.DAY_OF_MONTH);
        LunarCalendar.LunarDate lunar = LunarCalendar.getLunarDate(day, month, year);
        String weekday = weekdayName(dayOfWeek(year, month, day));

        selectedDateTitle.setText(weekday + ", "
                + String.format(Locale.US, "%02d/%02d/%04d", day, month, year));

        String lunarLine;
        if (vietnamese) {
            lunarLine = "Âm lịch: ngày " + lunar.day + " tháng " + lunar.month
                    + (lunar.isLeap ? " (nhuận)" : "") + ", năm " + lunar.year
                    + "\nNgày " + lunar.getDayName() + " • Tháng " + lunar.getMonthName()
                    + " • Năm " + lunar.getYearName();
        } else {
            lunarLine = "Lunar: day " + lunar.day + " month " + lunar.month
                    + (lunar.isLeap ? " (leap)" : "") + ", year " + lunar.year
                    + "\nDay " + lunar.getDayName() + " • Month " + lunar.getMonthName()
                    + " • Year " + lunar.getYearName();
        }
        selectedLunarLine.setText(lunarLine);

        selectedSolarTerm.setText((vietnamese ? "Tiết khí: " : "Solar term: ")
                + lunar.getSolarTerm());

        String lucky = lunar.getLuckyHours();
        selectedLuckyHours.setText((vietnamese ? "Giờ hoàng đạo: " : "Auspicious hours: ")
                + (lucky == null || lucky.isEmpty() ? "—" : lucky));
    }

    private String monthTitle(int month, int year) {
        if (vietnamese) {
            return "Tháng " + month + " / " + year;
        }
        String[] months = { "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December" };
        return months[month - 1] + " " + year;
    }

    private String weekdayName(int dayOfWeek) {
        String[] vi = { "", "CHỦ NHẬT", "THỨ HAI", "THỨ BA", "THỨ TƯ", "THỨ NĂM", "THỨ SÁU", "THỨ BẢY" };
        String[] en = { "", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY" };
        return (vietnamese ? vi : en)[dayOfWeek];
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int dayOfWeek(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, day, 12, 0, 0);
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    private static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }
}