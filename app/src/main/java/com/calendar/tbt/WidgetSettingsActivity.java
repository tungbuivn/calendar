package com.calendar.tbt;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.CheckBox;
import androidx.appcompat.widget.Toolbar;

public class WidgetSettingsActivity extends Activity {
    
    private WidgetPreferences preferences;
    private SeekBar weekFontSeekBar;
    private SeekBar timeFontSeekBar;
    private SeekBar dayFontSeekBar;
    private SeekBar weatherFontSeekBar;
    private TextView weekFontSizeText;
    private TextView timeFontSizeText;
    private TextView dayFontSizeText;
    private TextView weatherFontSizeText;
    private RadioGroup languageRadioGroup;
    private CheckBox hideTopSectionCheckBox;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_settings);
        
        preferences = new WidgetPreferences(this);
        
        setupToolbar();
        initializeViews();
        setupSeekBars();
        setupLanguageSelection();
        setupHideTopSection();
        setupButtons();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Handle status bar height for notch devices
            handleStatusBarHeight();
            
            // Set up back button
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
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
    
    private void initializeViews() {
        weekFontSeekBar = findViewById(R.id.week_font_seekbar);
        timeFontSeekBar = findViewById(R.id.time_font_seekbar);
        dayFontSeekBar = findViewById(R.id.day_font_seekbar);
        weatherFontSeekBar = findViewById(R.id.weather_font_seekbar);
        
        weekFontSizeText = findViewById(R.id.week_font_size_text);
        timeFontSizeText = findViewById(R.id.time_font_size_text);
        dayFontSizeText = findViewById(R.id.day_font_size_text);
        weatherFontSizeText = findViewById(R.id.weather_font_size_text);
        
        languageRadioGroup = findViewById(R.id.language_radio_group);
        hideTopSectionCheckBox = findViewById(R.id.hide_top_section_checkbox);
    }
    
    private void setupSeekBars() {
        // Week font size (12-36sp)
        weekFontSeekBar.setMax(24); // 36 - 12 = 24
        weekFontSeekBar.setProgress(preferences.getWeekFontSize() - 12);
        weekFontSizeText.setText("Week Font Size: " + preferences.getWeekFontSize() + "sp");
        
        weekFontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int fontSize = progress + 12;
                weekFontSizeText.setText("Week Font Size: " + fontSize + "sp");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int fontSize = seekBar.getProgress() + 12;
                preferences.setWeekFontSize(fontSize);
            }
        });
        
        // Time font size (24-72sp)
        timeFontSeekBar.setMax(48); // 72 - 24 = 48
        timeFontSeekBar.setProgress(preferences.getTimeFontSize() - 24);
        timeFontSizeText.setText("Time Font Size: " + preferences.getTimeFontSize() + "sp");
        
        timeFontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int fontSize = progress + 24;
                timeFontSizeText.setText("Time Font Size: " + fontSize + "sp");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int fontSize = seekBar.getProgress() + 24;
                preferences.setTimeFontSize(fontSize);
            }
        });
        
        // Day font size (16-48sp)
        dayFontSeekBar.setMax(32); // 48 - 16 = 32
        dayFontSeekBar.setProgress(preferences.getDayFontSize() - 16);
        dayFontSizeText.setText("Day Font Size: " + preferences.getDayFontSize() + "sp");
        
        dayFontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int fontSize = progress + 16;
                dayFontSizeText.setText("Day Font Size: " + fontSize + "sp");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int fontSize = seekBar.getProgress() + 16;
                preferences.setDayFontSize(fontSize);
            }
        });
        
        // Weather font size (8-32sp)
        weatherFontSeekBar.setMax(24); // 32 - 8 = 24
        weatherFontSeekBar.setProgress(preferences.getWeatherFontSize() - 8);
        weatherFontSizeText.setText("Weather Font Size: " + preferences.getWeatherFontSize() + "sp");
        
        weatherFontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int fontSize = progress + 8;
                weatherFontSizeText.setText("Weather Font Size: " + fontSize + "sp");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int fontSize = seekBar.getProgress() + 8;
                preferences.setWeatherFontSize(fontSize);
            }
        });
    }
    
    private void setupLanguageSelection() {
        String currentLanguage = preferences.getLanguage();
        
        if ("vi".equals(currentLanguage)) {
            languageRadioGroup.check(R.id.radio_vietnamese);
        } else {
            languageRadioGroup.check(R.id.radio_english);
        }
        
        languageRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                final String language;
                if (checkedId == R.id.radio_english) {
                    preferences.setLanguage("en");
                    language = "English";
                } else if (checkedId == R.id.radio_vietnamese) {
                    preferences.setLanguage("vi");
                    language = "Vietnamese";
                } else {
                    language = "English";
                }
                // Immediately update the widget when language changes
                // Add a small delay to ensure preferences are saved
                group.post(new Runnable() {
                    @Override
                    public void run() {
                        updateWidget();
                        Toast.makeText(WidgetSettingsActivity.this, 
                            "Language changed to " + language, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    private void setupHideTopSection() {
        // Set initial state based on preferences
        hideTopSectionCheckBox.setChecked(preferences.getHideTopSection());
        
        hideTopSectionCheckBox.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                preferences.setHideTopSection(isChecked);
                // Immediately update the widget when this setting changes
                updateWidget();
                                       String message = isChecked ? "Day labels and week dates hidden" : "Day labels and week dates shown";
                Toast.makeText(WidgetSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupButtons() {
        Button applyButton = findViewById(R.id.apply_button);
        Button resetButton = findViewById(R.id.reset_button);
        
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWidget();
                Toast.makeText(WidgetSettingsActivity.this, "Settings applied!", Toast.LENGTH_SHORT).show();
            }
        });
        
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset to defaults
                preferences.setWeekFontSize(24);
                preferences.setTimeFontSize(48);
                preferences.setDayFontSize(24);
                preferences.setLanguage("vi");
                preferences.setHideTopSection(false);
                
                // Update seekbars
                weekFontSeekBar.setProgress(16);
                timeFontSeekBar.setProgress(24);
                dayFontSeekBar.setProgress(8);
                
                // Update text
                weekFontSizeText.setText("Week Font Size: "+preferences.getWeekFontSize()+"sp");
                timeFontSizeText.setText("Time Font Size: "+preferences.getTimeFontSize()+"sp");
                dayFontSizeText.setText("Day Font Size: "+preferences.getDayFontSize()+"sp");
                
                // Update language selection
                languageRadioGroup.check(R.id.radio_vietnamese);
                
                // Update checkbox
                hideTopSectionCheckBox.setChecked(false);
                
                updateWidget();
                Toast.makeText(WidgetSettingsActivity.this, "Settings reset to defaults!", Toast.LENGTH_SHORT).show();
            }
        });
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