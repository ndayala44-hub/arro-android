#!/bin/bash
# Runs the screenshot tour on a booted emulator and copies the PNGs to <out>/screenshots.tar (default out/).
# Used by .github/workflows/release-assets.yml; also works against any device connected with adb.
set -x
OUT=${1:-out}
mkdir -p "$OUT"
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done'
sleep 10
# A tidy status bar: 9:41, full battery and signal, no notification icons.
adb shell settings put global sysui_demo_allowed 1
sleep 2
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb install -r out/app-debug.apk
adb install -r out/app-debug-androidTest.apk
adb shell getprop ro.build.version.release | tee "$OUT/android-version.txt"
adb shell am instrument -w -e class app.arro.ScreenshotTour app.arro.debug.test/androidx.test.runner.AndroidJUnitRunner | tee "$OUT/instrument.txt"
adb exec-out run-as app.arro.debug tar c files/screenshots > "$OUT/screenshots.tar"
ls -la "$OUT"
grep -q "OK (1 test)" "$OUT/instrument.txt"
