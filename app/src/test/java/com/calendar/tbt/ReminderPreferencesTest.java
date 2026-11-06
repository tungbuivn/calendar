package com.calendar.tbt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Kiểm tra việc bật/tắt nhắc trước chỉ mở khi người dùng chủ động bật,
 * và quyền gửi thông báo chỉ được xin đúng lúc đó.
 */
public class ReminderPreferencesTest {

    /** SharedPreferences giả, chỉ cần đúng phần đọc/ghi số và boolean. */
    private static final class FakePreferences implements SharedPreferences {

        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return values;
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            return value instanceof Set ? (Set<String>) value : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value instanceof Integer ? (Integer) value : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value instanceof Long ? (Long) value : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public SharedPreferences.Editor edit() {
            return new FakeEditor(this);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
        }
    }

    private static final class FakeEditor implements SharedPreferences.Editor {

        private final FakePreferences prefs;

        FakeEditor(FakePreferences prefs) {
            this.prefs = prefs;
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            prefs.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            prefs.values.put(key, values);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            prefs.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            prefs.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            prefs.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            prefs.values.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            prefs.values.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            prefs.values.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {
        }
    }

    private Context context;

    @Before
    public void setUp() {
        FakePreferences prefs = new FakePreferences();
        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        when(appContext.getSharedPreferences(any(String.class), any(Integer.class))).thenReturn(prefs);
        context = appContext;
    }

    @Test
    public void notificationsDisabledByDefault() {
        assertFalse("Thông báo phải mặc định tắt để không hỏi quyền lúc mở app",
                ReminderPreferences.areNotificationsEnabled(context));
    }

    @Test
    public void canToggleNotifications() {
        ReminderPreferences.setNotificationsEnabled(context, true);
        assertTrue(ReminderPreferences.areNotificationsEnabled(context));
        ReminderPreferences.setNotificationsEnabled(context, false);
        assertFalse(ReminderPreferences.areNotificationsEnabled(context));
    }

    @Test
    public void daysBeforeRoundTrip() {
        ReminderPreferences.setDaysBefore(context, 7);
        assertEquals(7, ReminderPreferences.getDaysBefore(context));
    }

    @Test
    public void daysBeforeIsClamped() {
        ReminderPreferences.setDaysBefore(context, ReminderPreferences.MAX_DAYS + 10);
        assertEquals(ReminderPreferences.MAX_DAYS, ReminderPreferences.getDaysBefore(context));
        ReminderPreferences.setDaysBefore(context, ReminderPreferences.MIN_DAYS - 5);
        assertEquals(ReminderPreferences.MIN_DAYS, ReminderPreferences.getDaysBefore(context));
    }
}
