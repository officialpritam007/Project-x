#!/usr/bin/env bash
# Run only on the disposable CI emulator, after isolated instrumentation fixtures.
set -euo pipefail
candidate_apk="$1"
report_dir="$2"
test -f "$candidate_apk"
mkdir -p "$report_dir"
# Instrumentation rebuilds have a separate development certificate. Remove only
# disposable emulator fixtures, then install the exact distributed build artifact.
adb uninstall com.pritam.liquidchat.test >/dev/null 2>&1 || true
adb uninstall com.pritam.liquidchat >/dev/null 2>&1 || true
adb install "$candidate_apk" > "$report_dir/install.txt"
adb shell am start -W -n com.pritam.liquidchat/.MainActivity > "$report_dir/launch.txt"
sleep 3
if ! adb shell pidof com.pritam.liquidchat > "$report_dir/process.txt"; then
    adb logcat -d -b crash > "$report_dir/startup-crash.txt"
    exit 1
fi
adb exec-out screencap -p > "$report_dir/startup.png"
adb shell am force-stop com.pritam.liquidchat
