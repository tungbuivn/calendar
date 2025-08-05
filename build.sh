#!/bin/bash

# Set Java 17 for Android development
# export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Ensure debug key is in the app directory
if [ ! -f "app/debug-key.jks" ]; then
    echo "Copying debug key to app directory..."
    cp debug-key.jks app/
fi

# Build the project
echo "Building with Java 17..."
./gradlew assembleDebug -x lint 
./gradlew installDebug

echo "Build completed!"

#adb logcat -s CalendarWidget

