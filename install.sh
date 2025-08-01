#!/bin/bash
adb install app/build/outputs/apk/debug/app-debug.apk 
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.calendar.tbt/.MainActivity