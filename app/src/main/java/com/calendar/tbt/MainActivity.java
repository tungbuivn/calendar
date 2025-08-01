package com.calendar.tbt;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.app.job.JobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobService;
import android.app.job.JobParameters;
import android.app.job.JobInfo.Builder;
import android.app.job.JobInfo.Builder;
import com.calendar.tbt.CalendarWidget;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
}
