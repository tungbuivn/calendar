#!/bin/bash

set -e

./gradlew assembleRelease

device=$(adb devices | grep 97 | awk '{print $1}')

adb -s $device install -r app/build/outputs/apk/release/app-release.apk