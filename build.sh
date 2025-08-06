#!/bin/bash

# Set Java 17 for Android development
# export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

mode=$1
# Ensure debug key is in the app directory
if [ ! -f "app/debug-key.jks" ]; then
    echo "Copying debug key to app directory..."
    cp debug-key.jks app/
fi

# Build the project
echo "Building with Java 17..."
if [ "$mode" == "prod" ]; then
    ./gradlew assembleRelease -x lint 
    ./gradlew installRelease
else
    ./gradlew assembleDebug -x lint 
    ./gradlew installDebug
fi

echo "Build completed!"

#adb logcat -s CalendarWidget

