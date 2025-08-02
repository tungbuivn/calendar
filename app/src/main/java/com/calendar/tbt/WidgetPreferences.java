package com.calendar.tbt;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

public class WidgetPreferences {
    private static final String PREF_NAME = "widget_preferences";
    private static final String KEY_WEEK_FONT_SIZE = "week_font_size";
    private static final String KEY_TIME_FONT_SIZE = "time_font_size";
    private static final String KEY_DAY_FONT_SIZE = "day_font_size";
    private static final String KEY_WEATHER_FONT_SIZE = "weather_font_size";
    private static final String KEY_LANGUAGE = "language";
    
    private SharedPreferences preferences;
    
    public WidgetPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void setWeekFontSize(int size) {
        preferences.edit().putInt(KEY_WEEK_FONT_SIZE, size).apply();
    }
    
    public int getWeekFontSize() {
        return preferences.getInt(KEY_WEEK_FONT_SIZE, 24); // Default 24sp
    }
    
    public void setTimeFontSize(int size) {
        preferences.edit().putInt(KEY_TIME_FONT_SIZE, size).apply();
    }
    
    public int getTimeFontSize() {
        return preferences.getInt(KEY_TIME_FONT_SIZE, 48); // Default 48sp
    }
    
    public void setDayFontSize(int size) {
        preferences.edit().putInt(KEY_DAY_FONT_SIZE, size).apply();
    }
    
    public int getDayFontSize() {
        return preferences.getInt(KEY_DAY_FONT_SIZE, 24); // Default 24sp
    }
    
    public void setWeatherFontSize(int size) {
        preferences.edit().putInt(KEY_WEATHER_FONT_SIZE, size).apply();
    }
    
    public int getWeatherFontSize() {
        return preferences.getInt(KEY_WEATHER_FONT_SIZE, 14); // Default 14sp
    }
    
    public void setLanguage(String language) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply();
    }
    
    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, "vi"); // Default English
    }
    
    public Locale getLocale() {
        String language = getLanguage();
        switch (language) {
            case "vi":
                return new Locale("vi", "VN");
            case "en":
            default:
                return new Locale("en", "US");
        }
    }
} 