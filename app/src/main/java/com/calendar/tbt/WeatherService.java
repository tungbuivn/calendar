package com.calendar.tbt;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Thời tiết lấy trực tiếp từ Open-Meteo theo thành phố người dùng chọn.
 *
 * <p>Không dùng quyền vị trí: không FINE/COARSE_LOCATION, không Geocoder,
 * không đọc GPS. Nhờ vậy không cần prominent disclosure cho location và
 * giảm rủi ro chính sách Google Play.
 */
public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String WEATHER_API_BASE_URL = "https://api.open-meteo.com/v1/forecast";

    public static final String[] CITY_NAMES = {
            "Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "Huế", "Nha Trang", "Đà Lạt", "Vũng Tàu", "Quy Nhơn",
            "Buôn Ma Thuột", "Thái Nguyên", "Nam Định", "Việt Trì", "Hạ Long",
            "Sa Pa", "Lạng Sơn", "Cao Bằng", "Điện Biên Phủ", "Tuy Hòa",
            "Phan Thiết", "Rạch Giá", "Cà Mau", "Hà Giang"
    };

    private static final double[] CITY_LATITUDE = {
            21.0285, 10.8231, 16.0544, 20.8449, 10.0452,
            16.4637, 12.2388, 11.9465, 10.3460, 13.7829,
            12.6667, 21.5942, 20.4388, 21.3227, 20.9101,
            22.3364, 21.8537, 22.6657, 21.3860, 13.0882,
            10.9289, 10.0125, 9.1768, 22.8233
    };

    private static final double[] CITY_LONGITUDE = {
            105.8542, 106.6297, 108.2022, 106.6881, 105.7469,
            107.5909, 109.1967, 108.4419, 107.0843, 109.2196,
            108.0500, 105.8482, 106.1621, 105.4019, 107.1839,
            103.8438, 106.7615, 106.2522, 103.0230, 109.3130,
            108.1021, 105.0809, 105.1524, 104.9836
    };

    private static String cachedWeatherInfo = "~";
    private static String cachedLocationName = CITY_NAMES[0];
    private static long lastWeatherUpdate = 0;
    private static final long WEATHER_CACHE_DURATION = 30 * 60 * 1000L;
    private static boolean isFirstUpdate = true;
    private static WeatherUpdateCallback weatherCallback = null;
    private static Context appContext = null;

    public interface WeatherUpdateCallback {
        void onWeatherUpdated(String weatherInfo);
    }

    public static void setWeatherCallback(WeatherUpdateCallback callback) {
        weatherCallback = callback;
    }

    public static void setAppContext(Context context) {
        if (appContext == null && context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static String[] getCityNames() {
        return CITY_NAMES.clone();
    }

    public static int getCityIndex(Context context) {
        int index = new WidgetPreferences(context).getWeatherCityIndex();
        if (index < 0 || index >= CITY_NAMES.length) return 0;
        return index;
    }

    public static String getLocationName(Context context) {
        return CITY_NAMES[getCityIndex(context)];
    }

    private static void applyCity(Context context) {
        int index = getCityIndex(context);
        cachedLocationName = CITY_NAMES[index];
    }

    public static String getWeatherInfo(Context context) {
        setAppContext(context);
        applyCity(context);

        long currentTime = System.currentTimeMillis();
        if (isFirstUpdate) {
            isFirstUpdate = false;
            lastWeatherUpdate = 0;
        }

        if (currentTime - lastWeatherUpdate < WEATHER_CACHE_DURATION && !isFirstUpdate) {
            return cachedWeatherInfo;
        }

        new FetchWeatherTask().execute();
        return cachedWeatherInfo;
    }

    private static String getWeatherApiUrl(Context context) {
        int index = getCityIndex(context);
        double latitude = CITY_LATITUDE[index];
        double longitude = CITY_LONGITUDE[index];
        return WEATHER_API_BASE_URL
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current_weather=true";
    }

    private static class FetchWeatherTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                if (appContext == null) return null;
                String apiUrl = getWeatherApiUrl(appContext);
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    return parseWeatherResponse(response.toString());
                }
                Log.e(TAG, "Weather API error: " + responseCode);
                return null;
            } catch (IOException error) {
                Log.e(TAG, "Error fetching weather: " + error.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String weatherInfo) {
            if (weatherInfo == null) return;
            cachedWeatherInfo = weatherInfo;
            lastWeatherUpdate = System.currentTimeMillis();
            if (weatherCallback != null) weatherCallback.onWeatherUpdated(weatherInfo);
            updateWidgetWithWeather();
        }
    }

    private static String parseWeatherResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject currentWeather = json.getJSONObject("current_weather");
            double temperature = currentWeather.getDouble("temperature");
            int weatherCode = currentWeather.getInt("weathercode");
            String tempString = String.format(Locale.US, "%.0f°C", Math.ceil(temperature));
            String weatherIcon = getWeatherIcon(weatherCode);
            return tempString + " " + weatherIcon;
        } catch (JSONException error) {
            Log.e(TAG, "Error parsing weather JSON: " + error.getMessage());
            return "~";
        }
    }

    private static String getWeatherIcon(int weatherCode) {
        switch (weatherCode) {
            case 0: return "☀️";
            case 1: case 2: case 3: return "🌤️";
            case 45: case 48: return "🌫️";
            case 51: case 53: case 55: return "🌦️";
            case 56: case 57: return "🌧️";
            case 61: case 63: case 65: return "🌧️";
            case 66: case 67: return "🌨️";
            case 71: case 73: case 75: return "🌨️";
            case 77: return "❄️";
            case 80: case 81: case 82: return "🌦️";
            case 85: case 86: return "🌨️";
            case 95: return "⛈️";
            case 96: case 99: return "⛈️";
            default: return "🌤️";
        }
    }

    private static void updateWidgetWithWeather() {
        if (appContext != null) CalendarWidget.updateAllWidgets(appContext);
    }

    public static void clearCache() {
        lastWeatherUpdate = 0;
        isFirstUpdate = true;
    }

    public static String getLastUpdateInfo() {
        if (lastWeatherUpdate == 0) return "Never updated";
        long timeSinceUpdate = (System.currentTimeMillis() - lastWeatherUpdate) / 60000;
        return "Updated " + timeSinceUpdate + " minutes ago";
    }
}