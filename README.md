# Calendar Widget App

A modern Android application featuring a beautiful monochromatic calendar widget that displays current time and current week information.

## Features

- **Monochromatic Calendar Widget**: Displays current time, day/date, and current week calendar
- **Time Display**: Shows current time in large, prominent format (e.g., "10:09")
- **Day/Date Information**: Displays day abbreviation and date (e.g., "Wed Thu 11")
- **Current Week Display**: Shows the current week with all 7 days and highlights today
- **Modern Design**: Clean monochromatic design with beige background and dark brown text
- **Auto-updating**: Widget updates every minute to show current time
- **Clickable**: Tapping the widget opens the main app
- **Resizable**: Widget can be resized horizontally and vertically
- **Clean Android project structure**
- **Modern Android Gradle Plugin (8.1.4)**
- **Gradle 8.4**
- **AndroidX support**
- **Material Design components**

## Widget Features

The calendar widget displays:
- **Current Time**: Large, prominent display of current time (e.g., "10:09")
- **Day and Date**: Abbreviated day name and date (e.g., "Wed Thu 11")
- **Week Header**: "THIS WEEK" label
- **Current Week**: All 7 days of the current week with days of the week labels
- **Current Day Highlight**: Today's date is highlighted with dark brown background
- **Monochromatic Design**: Beige background with dark brown text for clean, elegant look

## Build Requirements

- Android SDK 34
- Java 11
- Gradle 8.4

## Building the Project

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/calendar/tbt/
│   │   ├── MainActivity.java
│   │   └── CalendarWidget.java
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   └── calendar_widget.xml
│   │   ├── drawable/
│   │   │   ├── widget_background.xml
│   │   │   ├── widget_preview.xml
│   │   │   ├── current_day_background.xml
│   │   │   └── button_background.xml
│   │   ├── xml/
│   │   │   └── calendar_widget_info.xml
│   │   └── values/
│   │       └── strings.xml
│   └── AndroidManifest.xml
└── build.gradle
```

## How to Use

1. **Install the app** on your Android device
2. **Add the widget** to your home screen:
   - Long press on home screen
   - Select "Widgets"
   - Find "Calendar Widget" in the list
   - Drag and drop to your home screen
3. **The widget will display** current time and current week information
4. **Tap the widget** to open the main app
5. **Use the "Update Widget" button** in the app to manually refresh the widget

## Widget Specifications

- **Size**: 3x2 (180dp x 110dp minimum)
- **Update Frequency**: Every minute (for time display)
- **Resizable**: Yes (horizontal and vertical)
- **Clickable**: Yes (opens main app)
- **Design**: Monochromatic with beige background and dark brown text
- **Layout**: Time on left, current week on right
