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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Request location permissions
        requestLocationPermissions();
        
        // Request battery optimization permission for Android 6.0+
        requestBatteryOptimizationPermission();
        
        // Start the widget update service
        // WidgetUpdateJobService.scheduleWidgetJob(this);
        
        // Force update all widgets to ensure they show current data
        forceUpdateAllWidgets();
        
        // Force immediate time update
        // WidgetUpdateJobService.scheduleWidgetJob(this);
        
        // Add a button to manually update the widget
        Button updateWidgetButton = findViewById(R.id.update_widget_button);
        if (updateWidgetButton != null) {
            updateWidgetButton.setOnClickListener(v -> {
                updateWidget();
                Toast.makeText(this, "Widget updated!", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Add a button to open widget settings
        Button settingsButton = findViewById(R.id.widget_settings_button);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, WidgetSettingsActivity.class);
                startActivity(intent);
            });
        }
        
        // Add a test button to manually trigger time update
        Button testTimeButton = findViewById(R.id.test_time_button);
        if (testTimeButton != null) {
            testTimeButton.setOnClickListener(v -> {
                // Manually trigger the job service
                // WidgetUpdateJobService.scheduleWidgetJob(this);
                
                // Also directly update the widget time
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                ComponentName componentName = new ComponentName(this, CalendarWidget.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
                
                if (appWidgetIds.length > 0) {
                    // Test both methods
                    CalendarWidget.updateWidgetTime(this, appWidgetManager, appWidgetIds);
                    
                    // Also force a full widget update
                    Intent intent = new Intent(this, CalendarWidget.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                    sendBroadcast(intent);
                    
                    Toast.makeText(this, "Time updated directly!", Toast.LENGTH_SHORT).show();
            } else {
                    Toast.makeText(this, "No widgets found!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Add a button to refresh weather
        Button refreshWeatherButton = findViewById(R.id.refresh_weather_button);
        if (refreshWeatherButton != null) {
            refreshWeatherButton.setOnClickListener(v -> {
                // Force refresh weather data
                CalendarWidget.refreshWeather(this);
                Toast.makeText(this, "Weather refreshed!", Toast.LENGTH_SHORT).show();
            });
        }
        CalendarWidget.schedulePeriodicUpdates(this);
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
