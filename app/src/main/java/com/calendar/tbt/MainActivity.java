package com.calendar.tbt;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.app.job.JobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobService;
import android.app.job.JobParameters;
import android.app.job.JobInfo.Builder;
import android.app.job.JobInfo.Builder;
import android.provider.Settings;
import android.os.PowerManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import com.calendar.tbt.CalendarWidget;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // Current date for navigation
    private java.util.Calendar currentDate;
    private static final float SWIPE_THRESHOLD = 100;
    private static final float SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize navigation drawer
        setupNavigationDrawer();

        // Initialize bottom toolbar buttons
        setupBottomToolbar();

        // Request location permissions
        requestLocationPermissions();

        // Request battery optimization permission for Android 6.0+
        requestBatteryOptimizationPermission();

        // Force update all widgets to ensure they show current data
        forceUpdateAllWidgets();

        // Populate calendar information
        populateCalendarInfo();

        // Initialize current date and set up swipe gestures
        currentDate = java.util.Calendar.getInstance();
        setupSwipeGestures();

        // Set fixed widths for side columns and make main_date fill remaining space
        // setFixedWidths();

        CalendarWidget.schedulePeriodicUpdates(this);
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Handle status bar height for notch devices
        handleStatusBarHeight();

        // Set up navigation item selection listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_widget_settings) {
                Intent intent = new Intent(this, WidgetSettingsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            }

            return false;
        });

        // Set up action bar drawer toggle
        androidx.appcompat.app.ActionBarDrawerToggle toggle = new androidx.appcompat.app.ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupBottomToolbar() {
        Button bottomButton1 = findViewById(R.id.bottom_button_1);
        Button bottomButton2 = findViewById(R.id.bottom_button_2);

        bottomButton1.setOnClickListener(v -> {
            // Handle button 1 click
            Toast.makeText(this, "Button 1 clicked!", Toast.LENGTH_SHORT).show();
        });

        bottomButton2.setOnClickListener(v -> {
            // Handle button 2 click
            Toast.makeText(this, "Button 2 clicked!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            updateWidget();
            Toast.makeText(this, "Widget refreshed!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleStatusBarHeight() {
        // Get the status bar height
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // Add top margin to toolbar to account for status bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) toolbar
                    .getLayoutParams();
            params.topMargin = statusBarHeight;
            toolbar.setLayoutParams(params);
        }
    }

    private void populateCalendarInfo() {
        // Get current date and update calendar
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        updateCalendarForDate(calendar, true);
    }

    /**
     * Updates the Center Column - Main Date section with current date information
     */
    private void updateMainDateSection(int day, int dayOfWeek) {
        // Update day number with enhanced formatting
        TextView dayNumber = findViewById(R.id.day_number);
        dayNumber.setText(String.valueOf(day));

        // Add visual emphasis for current day
        if (dayNumber != null) {
            // You can add additional styling here if needed
            // For example, changing text color based on day of week
            int textColor = getDayColor(dayOfWeek);
            dayNumber.setTextColor(textColor);
        }

        // Update day name with enhanced formatting
        TextView dayName = findViewById(R.id.day_name);
        String dayNameText = getDayName(dayOfWeek);
        dayName.setText(dayNameText);
    }

    /**
     * Returns the appropriate color for the day number based on day of week
     */
    private int getDayColor(int dayOfWeek) {
        switch (dayOfWeek) {
            case java.util.Calendar.SUNDAY:
                return 0xFFFF6B9D; // Pink for Sunday
            case java.util.Calendar.SATURDAY:
                return 0xFFFF6B9D; // Pink for Saturday
            default:
                return 0xFFFF6B9D; // Default pink color
        }
    }

    /**
     * Returns the formatted day name in Vietnamese only
     */
    private String getDayName(int dayOfWeek) {
        String[] dayNames = { "", "CHỦ NHẬT", "THỨ HAI", "THỨ BA",
                "THỨ TƯ", "THỨ NĂM", "THỨ SÁU", "THỨ BẢY" };
        return dayNames[dayOfWeek];
    }

    private void forceUpdateAllWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName componentName = new ComponentName(this, CalendarWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        if (appWidgetIds.length > 0) {
            android.util.Log.d("MainActivity", "Force updating " + appWidgetIds.length + " widgets on app start");
            Intent intent = new Intent(this, CalendarWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            sendBroadcast(intent);
        }
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName componentName = new ComponentName(this, CalendarWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        Intent intent = new Intent(this, CalendarWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(intent);
    }

    /**
     * Request battery optimization permission for Android 6.0+
     * This is important for widgets and background services to work properly
     */
    private void requestBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                } catch (Exception e) {
                    // Fallback: open battery optimization settings
                    try {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e2) {
                        // If both fail, just log the error
                        android.util.Log.e("MainActivity", "Could not open battery optimization settings", e2);
                    }
                }
            }
        }
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted!", Toast.LENGTH_SHORT).show();
                // Force weather refresh with new location
                CalendarWidget.refreshWeather(this);
            } else {
                Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Sets fixed widths for side columns and makes main_date fill remaining space
     */
    private void setFixedWidths() {
        // Use post to ensure layout is complete
        findViewById(android.R.id.content).post(() -> {
            try {
                // Find the components
                android.view.View weekInfo = findViewById(R.id.week_info);
                android.view.View imageView = findViewById(R.id.image_view);
                android.view.View mainDate = findViewById(R.id.main_date);

                if (weekInfo != null && imageView != null && mainDate != null) {
                    // Calculate fixed width for side columns (e.g., 120dp)
                    int fixedWidth = (int) (120 * getResources().getDisplayMetrics().density);

                    // Set fixed width for week_info
                    android.view.ViewGroup.LayoutParams weekInfoParams = weekInfo.getLayoutParams();
                    weekInfoParams.width = fixedWidth;
                    weekInfo.setLayoutParams(weekInfoParams);

                    // Set fixed width for image_view
                    android.view.ViewGroup.LayoutParams imageViewParams = imageView.getLayoutParams();
                    imageViewParams.width = fixedWidth;
                    imageView.setLayoutParams(imageViewParams);

                    // Make main_date fill remaining space
                    android.view.ViewGroup.LayoutParams mainDateParams = mainDate.getLayoutParams();
                    mainDateParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    mainDate.setLayoutParams(mainDateParams);

                    // Log the new widths
                    String logMessage = String.format(
                            "Fixed Widths Set - week_info: %dpx, image_view: %dpx, main_date: MATCH_PARENT",
                            fixedWidth, fixedWidth);
                    android.util.Log.d("MainActivity", logMessage);

                    // Show a toast with the new widths
                    String toastMessage = String.format("Fixed widths set: %dpx each", fixedWidth);
                    Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();

                } else {
                    android.util.Log.e("MainActivity", "One or more components not found");
                    if (weekInfo == null) {
                        android.util.Log.e("MainActivity", "week_info is null");
                    }
                    if (imageView == null) {
                        android.util.Log.e("MainActivity", "image_view is null");
                    }
                    if (mainDate == null) {
                        android.util.Log.e("MainActivity", "main_date is null");
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error setting fixed widths", e);
            }
        });
    }

    /**
     * Logs the width of week_info and image_view components
     */
    private void logComponentWidths() {
        // Use post to ensure layout is complete
        findViewById(android.R.id.content).post(() -> {
            try {
                // Find the components
                android.view.View weekInfo = findViewById(R.id.week_info);
                android.view.View imageView = findViewById(R.id.image_view);

                if (weekInfo != null && imageView != null) {
                    // Get the widths
                    int weekInfoWidth = weekInfo.getWidth();
                    int imageViewWidth = imageView.getWidth();

                    // Log the widths
                    String logMessage = String.format("Component Widths - week_info: %dpx, image_view: %dpx",
                            weekInfoWidth, imageViewWidth);
                    android.util.Log.d("MainActivity", logMessage);

                    // Also print to console for easy viewing
                    System.out.println("=== COMPONENT WIDTHS ===");
                    System.out.println(logMessage);
                    System.out.println("========================");

                    // Show a toast with the widths
                    String toastMessage = String.format("Widths - Week: %dpx, Image: %dpx",
                            weekInfoWidth, imageViewWidth);
                    Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();

                } else {
                    android.util.Log.e("MainActivity", "One or both components not found");
                    if (weekInfo == null) {
                        android.util.Log.e("MainActivity", "week_info is null");
                    }
                    if (imageView == null) {
                        android.util.Log.e("MainActivity", "image_view is null");
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error logging component widths", e);
            }
        });
    }

    /**
     * Set up swipe gestures for day navigation
     */
    private void setupSwipeGestures() {
        // Set up gesture detection on the entire main layout
        View mainLayout = findViewById(android.R.id.content);
        if (mainLayout != null) {
            mainLayout.setOnTouchListener(new View.OnTouchListener() {
                private GestureDetector gestureDetector = new GestureDetector(MainActivity.this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                                try {
                                    float diffX = e2.getX() - e1.getX();
                                    float diffY = e2.getY() - e1.getY();

                                    android.util.Log.d("MainActivity",
                                            String.format("Swipe detected - diffX: %.2f, diffY: %.2f, velocityX: %.2f",
                                                    diffX, diffY, velocityX));

                                    if (Math.abs(diffX) > Math.abs(diffY)) {
                                        if (Math.abs(diffX) > SWIPE_THRESHOLD
                                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                            if (diffX > 0) {
                                                // Swipe from left to right - Previous day
                                                android.util.Log.d("MainActivity", "Previous day swipe detected");
                                                goToPreviousDay();
                                                return true;
                                            } else {
                                                // Swipe from right to left - Next day
                                                android.util.Log.d("MainActivity", "Next day swipe detected");
                                                goToNextDay();
                                                return true;
                                            }
                                        } else {
                                            android.util.Log.d("MainActivity", "Swipe threshold not met");
                                        }
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("MainActivity", "Error in swipe gesture", e);
                                }
                                return false;
                            }
                        });

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gestureDetector.onTouchEvent(event);
                }
            });
        }
    }

    /**
     * Navigate to the next day
     */
    private void goToNextDay() {
        currentDate.add(java.util.Calendar.DAY_OF_YEAR, 1);
        updateCalendarForCurrentDate();
        Toast.makeText(this, "Next day", Toast.LENGTH_SHORT).show();
        android.util.Log.d("MainActivity", "Next day called - new date: " + currentDate.getTime());
    }

    /**
     * Test method to simulate next day (for testing purposes)
     */
    public void testNextDay() {
        goToNextDay();
    }

    /**
     * Test method to simulate previous day (for testing purposes)
     */
    public void testPreviousDay() {
        goToPreviousDay();
    }

    /**
     * Navigate to the previous day
     */
    private void goToPreviousDay() {
        currentDate.add(java.util.Calendar.DAY_OF_YEAR, -1);
        updateCalendarForCurrentDate();
        Toast.makeText(this, "Previous day", Toast.LENGTH_SHORT).show();
        android.util.Log.d("MainActivity", "Previous day called - new date: " + currentDate.getTime());
    }

    /**
     * Update calendar display for the current selected date
     */
    private void updateCalendarForCurrentDate() {
        updateCalendarForDate(currentDate, false);
    }

    /**
     * Shared method to update calendar display for a specific date
     * 
     * @param targetDate    The date to display
     * @param isInitialLoad Whether this is the initial load (affects lunar hour
     *                      display)
     */
    private void updateCalendarForDate(java.util.Calendar targetDate, boolean isInitialLoad) {
        int day = targetDate.get(java.util.Calendar.DAY_OF_MONTH);
        int month = targetDate.get(java.util.Calendar.MONTH) + 1;
        int year = targetDate.get(java.util.Calendar.YEAR);
        int dayOfWeek = targetDate.get(java.util.Calendar.DAY_OF_WEEK);
        int weekOfYear = targetDate.get(java.util.Calendar.WEEK_OF_YEAR);

        // Update today's date header
        TextView todayDate = findViewById(R.id.today_date);
        String[] monthNames = { "THÁNG MỘT", "THÁNG HAI", "THÁNG BA", "THÁNG TƯ", "THÁNG NĂM", "THÁNG SÁU",
                "THÁNG BẢY", "THÁNG TÁM", "THÁNG CHÍN", "THÁNG MƯỜI", "THÁNG MƯỜI MỘT", "THÁNG MƯỜI HAI" };
        String[] englishMonths = { "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
                "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };
        String monthText = monthNames[month - 1] + " " + year + " " + englishMonths[month - 1];
        todayDate.setText(monthText);

        // Update Center Column - Main Date
        updateMainDateSection(day, dayOfWeek);

        // Update week number
        TextView weekNumber = findViewById(R.id.week_number);
        weekNumber.setText(String.valueOf(weekOfYear));

        // Update lunar information
        TextView lunarDay = findViewById(R.id.lunar_day);
        TextView lunarMonth = findViewById(R.id.lunar_month);
        TextView lunarDayName = findViewById(R.id.lunar_day_name);
        TextView lunarHour = findViewById(R.id.lunar_hour);
        TextView solarTerm = findViewById(R.id.solar_term);

        // Get lunar date for the target date
        LunarCalendar.LunarDate lunarDate = LunarCalendar.getLunarDate(day, month, year);
        TextView yearText = findViewById(R.id.year_text);
        yearText.setText(lunarDate.getYearName());

        lunarDay.setText(String.format("%d", lunarDate.day));
        lunarMonth.setText("Tháng " + lunarDate.getMonthName());
        lunarDayName.setText("Ngày " + lunarDate.getDayName());
        TextView thangNum = findViewById(R.id.thang_num);
        thangNum.setText(String.format("Tháng %d", lunarDate.month));

        // Update solar term
        solarTerm.setText(lunarDate.getSolarTerm());
        lunarHour.setText("Giờ " + lunarDate.getHourName());
        // Update lunar hour based on whether it's initial load or navigation
        TextView astrologicalInfo = findViewById(R.id.gio_tot);
        astrologicalInfo.setText("Giờ hoàng đạo: " + lunarDate.getLuckyHours());

        // Update week info for the week containing the target date
        updateWeekInfoForDate(targetDate);
    }

    /**
     * Update the week info component with current week's solar and lunar dates
     */
    private void updateWeekInfo() {
        updateWeekInfoForDate(java.util.Calendar.getInstance());
    }

    /**
     * Update the week info component for a specific date
     */
    private void updateWeekInfoForDate(java.util.Calendar targetDate) {
        // Find Monday of the week containing the target date
        java.util.Calendar monday = (java.util.Calendar) targetDate.clone();
        int dayOfWeek = monday.get(java.util.Calendar.DAY_OF_WEEK);
        int daysToMonday = (dayOfWeek + 5) % 7; // Convert to Monday = 0
        monday.add(java.util.Calendar.DAY_OF_YEAR, -daysToMonday);

        // Update each day of the week
        String[] dayIds = { "t2", "t3", "t4", "t5", "t6", "t7", "cn" };

        for (int i = 0; i < 7; i++) {
            java.util.Calendar currentDay = (java.util.Calendar) monday.clone();
            currentDay.add(java.util.Calendar.DAY_OF_YEAR, i);

            int day = currentDay.get(java.util.Calendar.DAY_OF_MONTH);
            int month = currentDay.get(java.util.Calendar.MONTH) + 1;
            int year = currentDay.get(java.util.Calendar.YEAR);

            // Get lunar date for this day
            LunarCalendar.LunarDate lunarDate = LunarCalendar.getLunarDate(day, month, year);

            // Update solar day TextView
            String solarDayId = "week_day_" + dayIds[i] + "s";
            int solarDayResId = getResources().getIdentifier(solarDayId, "id", getPackageName());
            if (solarDayResId != 0) {
                TextView solarDayView = findViewById(solarDayResId);
                if (solarDayView != null) {
                    // Format as 'day/month' if day is 1, otherwise just show the day
                    String solarDayText;
                    if (day == 1) {
                        solarDayText = String.format("%d/%d", day, month);
                    } else {
                        solarDayText = String.valueOf(day);
                    }
                    solarDayView.setText(solarDayText);
                }
            }

            // Update lunar day TextView
            String lunarDayId = "week_day_" + dayIds[i] + "l";
            int lunarDayResId = getResources().getIdentifier(lunarDayId, "id", getPackageName());
            if (lunarDayResId != 0) {
                TextView lunarDayView = findViewById(lunarDayResId);
                if (lunarDayView != null) {
                    lunarDayView.setText(String.valueOf(lunarDate.day));
                }
            }
        }
    }
}
