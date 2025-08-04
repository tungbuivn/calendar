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

public class MainActivity extends AppCompatActivity {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

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
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            params.topMargin = statusBarHeight;
            toolbar.setLayoutParams(params);
        }
    }
    
    private void populateCalendarInfo() {
        // Get current date
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        int month = calendar.get(java.util.Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
        int year = calendar.get(java.util.Calendar.YEAR);
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        int weekOfYear = calendar.get(java.util.Calendar.WEEK_OF_YEAR);
        
        // Update today's date header
        TextView todayDate = findViewById(R.id.today_date);
        String[] monthNames = {"THÁNG MỘT", "THÁNG HAI", "THÁNG BA", "THÁNG TƯ", "THÁNG NĂM", "THÁNG SÁU",
                              "THÁNG BẢY", "THÁNG TÁM", "THÁNG CHÍN", "THÁNG MƯỜI", "THÁNG MƯỜI MỘT", "THÁNG MƯỜI HAI"};
        String[] englishMonths = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
                                 "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};
        String monthText = monthNames[month - 1] + " " + year + " " + englishMonths[month - 1];
        todayDate.setText(monthText);
        
        // Update day number
        TextView dayNumber = findViewById(R.id.day_number);
        dayNumber.setText(String.valueOf(day));
        
        // Update day name
        TextView dayName = findViewById(R.id.day_name);
        String[] dayNames = {"", "CHỦ NHẬT - SUNDAY", "THỨ HAI - MONDAY", "THỨ BA - TUESDAY", 
                           "THỨ TƯ - WEDNESDAY", "THỨ NĂM - THURSDAY", "THỨ SÁU - FRIDAY", "THỨ BẢY - SATURDAY"};
        dayName.setText(dayNames[dayOfWeek]);
        
        // Update week number
        TextView weekNumber = findViewById(R.id.week_number);
        weekNumber.setText(String.valueOf(weekOfYear));
        
        // Update lunar information (simplified - in real app you'd use a lunar calendar library)
        TextView lunarDay = findViewById(R.id.lunar_day);
        TextView lunarMonth = findViewById(R.id.lunar_month);
        TextView lunarDayName = findViewById(R.id.lunar_day_name);
        TextView lunarHour = findViewById(R.id.lunar_hour);
        
        // For demo purposes, using simplified lunar data
        lunarDay.setText("9"); // This would be calculated from actual lunar calendar
        lunarMonth.setText("THÁNG QUÝ MÙI");
        lunarDayName.setText("NGÀY QUÝ MÃO");
        lunarHour.setText("GIỜ NHÂM TÝ");
        
        // Update week days
        TextView weekDays = findViewById(R.id.week_days);
        StringBuilder weekDaysText = new StringBuilder();
        java.util.Calendar weekStart = (java.util.Calendar) calendar.clone();
        weekStart.add(java.util.Calendar.DAY_OF_WEEK, -(dayOfWeek - 1)); // Start from Monday
        
        String[] shortDayNames = {"", "mon", "tue", "wed", "thu", "fri", "sat", "sun"};
        
        for (int i = 1; i <= 7; i++) {
            java.util.Calendar currentDay = (java.util.Calendar) weekStart.clone();
            currentDay.add(java.util.Calendar.DAY_OF_WEEK, i - 1);
            
            int currentDayOfMonth = currentDay.get(java.util.Calendar.DAY_OF_MONTH);
            int currentMonth = currentDay.get(java.util.Calendar.MONTH) + 1;
            int currentDayOfWeek = currentDay.get(java.util.Calendar.DAY_OF_WEEK);
            
            weekDaysText.append(shortDayNames[currentDayOfWeek]);
            weekDaysText.append(" ").append(currentDayOfMonth);
            if (currentMonth != month) {
                weekDaysText.append("/").append(currentMonth);
            }
            weekDaysText.append(" ").append(i).append("\n");
        }
        weekDays.setText(weekDaysText.toString().trim());
        
        // Update auspicious activities (this would come from lunar calendar calculations)
        TextView auspiciousActivities = findViewById(R.id.auspicious_activities);
        auspiciousActivities.setText("Cưới hỏi, khai trương, giao dịch, an táng, cải táng, xuất hành, chuyển về nhà mới.");
        
        // Update solar term and astrological info
        TextView solarTerm = findViewById(R.id.solar_term);
        TextView astrologicalInfo = findViewById(R.id.astrological_info);
        solarTerm.setText("Đại thử: ngày 22/07 lúc 20g30'");
        astrologicalInfo.setText("Hành Kim - Sao Nữ - Trực Thành");
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
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
}
