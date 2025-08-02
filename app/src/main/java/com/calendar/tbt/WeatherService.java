package com.calendar.tbt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String WEATHER_API_BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static String cachedWeatherInfo = "~";
    private static String cachedLocationName = "Hanoi";
    private static long lastWeatherUpdate = 0;
    private static final long WEATHER_CACHE_DURATION = 3600000/2; // 1 hour cache (API updates every hour)
    private static boolean isFirstUpdate = true; // Track if this is the first update
    private static WeatherUpdateCallback weatherCallback = null;
    private static Context appContext = null; // Store application context
    private static double lastLatitude = -1; // Initialize to -1
    private static double lastLongitude = -1; // Initialize to -1
    private static String locationName = "Hanoi"; // Default location name

    // Interface for weather update callbacks
    public interface WeatherUpdateCallback {
        void onWeatherUpdated(String weatherInfo);
    }

    public static void setWeatherCallback(WeatherUpdateCallback callback) {
        weatherCallback = callback;
    }

    public static void setAppContext(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    public static String getWeatherInfo(Context context) {
        Log.d(TAG, "=== getWeatherInfo called ===");
        
        // Store context for later use
        setAppContext(context);
        
        // Get current location
        updateLocation(context);
        
        long currentTime = System.currentTimeMillis();
        
        // On first update, always fetch fresh weather data
        if (isFirstUpdate) {
            Log.d(TAG, "=== First update detected, forcing fresh weather fetch ===");
            isFirstUpdate = false;
            lastWeatherUpdate = 0; // Force cache to be considered expired
        }
        
        // Return cached weather if it's still fresh (API updates every hour)
        if (currentTime - lastWeatherUpdate < WEATHER_CACHE_DURATION && !isFirstUpdate) {
            long timeSinceUpdate = (currentTime - lastWeatherUpdate) / 60000; // minutes
            Log.d(TAG, "=== Returning cached weather: " + cachedWeatherInfo + " (updated " + timeSinceUpdate + " minutes ago) ===");
            return cachedWeatherInfo;
        }

        // Fetch fresh weather data (API updates every hour)
        Log.d(TAG, "=== Cache expired or first update, fetching fresh weather data from API ===");
        Log.d(TAG, "=== Current cached weather: " + cachedWeatherInfo + " ===");
        new FetchWeatherTask().execute();
        
        // Return cached weather while fetching new data
        Log.d(TAG, "=== Returning cached weather while fetching: " + cachedWeatherInfo + " ===");
        return cachedWeatherInfo;
    }

    public static String getLocationName(Context context) {
        Log.d(TAG, "=== getLocationName called ===");
        
        // Store context for later use
        setAppContext(context);
        
        // Get current location
        updateLocation(context);
        
        return cachedLocationName;
    }

    private static void updateLocation(Context context) {
        Log.d(TAG, "=== updateLocation: Getting device location ===");
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "=== updateLocation: Location permissions not granted, using default coordinates ===");
            // Set default coordinates if permissions not granted
            if (lastLatitude == -1 || lastLongitude == -1) {
                lastLatitude = 21.0285; // Default to Hanoi
                lastLongitude = 105.8542; // Default to Hanoi
                Log.d(TAG, "=== updateLocation: Set default coordinates - Lat: " + lastLatitude + ", Long: " + lastLongitude + " ===");
            }
            return;
        }

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                
                if (location != null) {
                    lastLatitude = location.getLatitude();
                    lastLongitude = location.getLongitude();
                    Log.d(TAG, "=== updateLocation: Got location - Lat: " + lastLatitude + ", Long: " + lastLongitude + " ===");
                    
                    // Get location name using reverse geocoding
                    updateLocationName(context, lastLatitude, lastLongitude);
                } else {
                    Log.w(TAG, "=== updateLocation: No location available, using default coordinates ===");
                    // Set default coordinates if no location available
                    if (lastLatitude == -1 || lastLongitude == -1) {
                        lastLatitude = 21.0285; // Default to Hanoi
                        lastLongitude = 105.8542; // Default to Hanoi
                        Log.d(TAG, "=== updateLocation: Set default coordinates - Lat: " + lastLatitude + ", Long: " + lastLongitude + " ===");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "=== updateLocation: Error getting location: " + e.getMessage() + " ===");
            // Set default coordinates if error occurs
            if (lastLatitude == -1 || lastLongitude == -1) {
                lastLatitude = 21.0285; // Default to Hanoi
                lastLongitude = 105.8542; // Default to Hanoi
                Log.d(TAG, "=== updateLocation: Set default coordinates after error - Lat: " + lastLatitude + ", Long: " + lastLongitude + " ===");
            }
        }
    }

    private static void updateLocationName(Context context, double latitude, double longitude) {
        Log.d(TAG, "=== updateLocationName: Getting location name for coordinates ===");
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);
                String cityName = address.getLocality();
                if (cityName == null || cityName.isEmpty()) {
                    cityName = address.getAdminArea(); // Use state/province if city is not available
                }
                if (cityName == null || cityName.isEmpty()) {
                    cityName = address.getCountryName(); // Use country if state is not available
                }
                
                if (cityName != null && !cityName.isEmpty()) {
                    locationName = cityName;
                    cachedLocationName = cityName;
                    Log.d(TAG, "=== updateLocationName: Location name updated to: " + locationName + " ===");
                } else {
                    Log.w(TAG, "=== updateLocationName: Could not get location name, using default ===");
                }
            } else {
                Log.w(TAG, "=== updateLocationName: No addresses found for coordinates ===");
            }
        } catch (Exception e) {
            Log.e(TAG, "=== updateLocationName: Error getting location name: " + e.getMessage() + " ===");
        }
    }

    private static String getWeatherApiUrl() {
        // Ensure location is obtained before calling API
        if (appContext != null) {
            updateLocation(appContext);
        }
        
        // Use default coordinates if still not set
        if (lastLatitude == -1 || lastLongitude == -1) {
            // lastLatitude = 10.762622; // Default to Ho Chi Minh City
            // lastLongitude = 106.660172; // Default to Ho Chi Minh City
            Log.d(TAG, "=== getWeatherApiUrl: Using default coordinates - Lat: " + lastLatitude + ", Long: " + lastLongitude + " ===");
            return null;
        }
        
        String url = WEATHER_API_BASE_URL + "?latitude=" + lastLatitude + "&longitude=" + lastLongitude + "&current_weather=true";
        Log.d(TAG, "=== getWeatherApiUrl: Generated URL: " + url + " ===");
        return url;
    }

    private static class FetchWeatherTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String apiUrl = getWeatherApiUrl();
                if (apiUrl == null) {
                    Log.e(TAG, "=== FetchWeatherTask: No API URL available, returning null ===");
                    return null;
                }
                Log.d(TAG, "=== FetchWeatherTask: Starting HTTP request to API ===");
                Log.d(TAG, "Fetching weather from API: " + apiUrl);
                
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                Log.d(TAG, "=== FetchWeatherTask: Making HTTP connection ===");
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "=== FetchWeatherTask: HTTP response code: " + responseCode + " ===");
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "=== FetchWeatherTask: HTTP OK, reading response ===");
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Log.d(TAG, "Weather API response: " + response.toString());
                    String parsedWeather = parseWeatherResponse(response.toString());
                    Log.d(TAG, "=== FetchWeatherTask: Parsed weather result: " + parsedWeather + " ===");
                    return parsedWeather;
                } else {
                    Log.e(TAG, "Weather API error: " + responseCode);
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error fetching weather: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String weatherInfo) {
            Log.d(TAG, "=== FetchWeatherTask: onPostExecute called with weatherInfo: " + weatherInfo + " ===");
            if (weatherInfo != null) {
                Log.d(TAG, "=== FetchWeatherTask: Updating cached weather from '" + cachedWeatherInfo + "' to '" + weatherInfo + "' ===");
                cachedWeatherInfo = weatherInfo;
                lastWeatherUpdate = System.currentTimeMillis();
                Log.d(TAG, "Weather updated: " + weatherInfo);
                
                // Notify callback if available
                if (weatherCallback != null) {
                    Log.d(TAG, "=== FetchWeatherTask: Notifying callback with weather: " + weatherInfo + " ===");
                    weatherCallback.onWeatherUpdated(weatherInfo);
                }
                
                // Update widget with new weather immediately
                Log.d(TAG, "=== FetchWeatherTask: Calling updateWidgetWithWeather ===");
                updateWidgetWithWeather();
            } else {
                Log.e(TAG, "Failed to fetch weather, keeping cached value: " + cachedWeatherInfo);
            }
        }
    }

    private static String parseWeatherResponse(String jsonResponse) {
        Log.d(TAG, "=== parseWeatherResponse: Starting to parse JSON response ===");
        try {
            JSONObject json = new JSONObject(jsonResponse);
            Log.d(TAG, "=== parseWeatherResponse: JSON object created successfully ===");
            
            JSONObject currentWeather = json.getJSONObject("current_weather");
            Log.d(TAG, "=== parseWeatherResponse: Found current_weather object ===");
            
            double temperature = currentWeather.getDouble("temperature");
            int weatherCode = currentWeather.getInt("weathercode");
            Log.d(TAG, "=== parseWeatherResponse: Raw temperature: " + temperature + ", weather code: " + weatherCode + " ===");
            
            // Format temperature with round up value and zero decimal place
            String tempString = String.format(Locale.US, "%.0f°C", Math.ceil(temperature));
            
            // Add weather icon based on weather code
            String weatherIcon = getWeatherIcon(weatherCode);
            Log.d(TAG, "=== parseWeatherResponse: Formatted temp: " + tempString + ", weather icon: " + weatherIcon + " ===");
            
            // Return only weather info (location name is handled separately)
            String weatherInfo = tempString + " " + weatherIcon;
            Log.d(TAG, "Parsed weather: " + weatherInfo + " (temp: " + temperature + ", code: " + weatherCode + ", location: " + locationName + ")");
            Log.d(TAG, "=== parseWeatherResponse: Final weather info: " + weatherInfo + " ===");
            
            return weatherInfo;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing weather JSON: " + e.getMessage());
            Log.d(TAG, "=== parseWeatherResponse: JSON parsing failed, returning fallback ===");
            return "~"; // Fallback to indicate no weather data
        }
    }

    private static String getWeatherIcon(int weatherCode) {
        // Weather codes from Open-Meteo API - using emoji icons
        switch (weatherCode) {
            case 0: return "☀️"; // Clear sky
            case 1: case 2: case 3: return "🌤️"; // Partly cloudy
            case 45: case 48: return "🌫️"; // Foggy
            case 51: case 53: case 55: return "🌦️"; // Drizzle
            case 56: case 57: return "🌧️"; // Freezing drizzle
            case 61: case 63: case 65: return "🌧️"; // Rain
            case 66: case 67: return "🌨️"; // Freezing rain
            case 71: case 73: case 75: return "🌨️"; // Snow
            case 77: return "❄️"; // Snow grains
            case 80: case 81: case 82: return "🌦️"; // Rain showers
            case 85: case 86: return "🌨️"; // Snow showers
            case 95: return "⛈️"; // Thunderstorm
            case 96: case 99: return "⛈️"; // Thunderstorm with hail
            default: return "🌤️"; // Default partly cloudy
        }
    }

    private static void updateWidgetWithWeather() {
        // Update widget immediately when weather is received
        Log.d(TAG, "=== updateWidgetWithWeather: Starting widget update ===");
        if (appContext != null) {
            Log.d(TAG, "Updating widget with new weather: " + cachedWeatherInfo);
            Log.d(TAG, "=== updateWidgetWithWeather: Calling CalendarWidget.updateAllWidgets ===");
            CalendarWidget.updateAllWidgets(appContext);
            Log.d(TAG, "=== updateWidgetWithWeather: Widget update call completed ===");
        } else {
            Log.e(TAG, "Cannot update widget - no context available");
            Log.d(TAG, "=== updateWidgetWithWeather: No context available for widget update ===");
        }
    }

    // Method to clear cache and force fresh weather fetch
    public static void clearCache() {
        lastWeatherUpdate = 0; // Reset cache timestamp
        isFirstUpdate = true; // Reset first update flag
        Log.d(TAG, "Weather cache cleared and first update flag reset");
    }

    // Method to get last update time for debugging
    public static String getLastUpdateInfo() {
        if (lastWeatherUpdate == 0) {
            return "Never updated";
        }
        long timeSinceUpdate = (System.currentTimeMillis() - lastWeatherUpdate) / 60000; // minutes
        return "Updated " + timeSinceUpdate + " minutes ago";
    }
} 