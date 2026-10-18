#!/usr/bin/env bash
# Run only on the disposable CI emulator, after isolated instrumentation fixtures.
set -euo pipefail
candidate_apk="$1"
report_dir="$2"
test -f "$candidate_apk"
if [[ "${LIQUID_DISPOSABLE_EMULATOR:-}" != "1" ]]; then
    echo 'Refusing destructive smoke setup: a disposable emulator must be explicitly selected.' >&2
    exit 1
fi
emulator_serial="$(adb -e get-serialno)"
if [[ ! "$emulator_serial" =~ ^emulator-[0-9]+$ ]]; then
    echo 'Refusing smoke setup: expected a local emulator serial.' >&2
    exit 1
fi
emulator_adb=(adb -s "$emulator_serial")
if [[ "$("${emulator_adb[@]}" shell getprop ro.kernel.qemu | tr -d '\r\n')" != "1" ]]; then
    echo 'Refusing smoke setup: target is not a verified emulator.' >&2
    exit 1
fi
mkdir -p "$report_dir"
# Remove only disposable emulator fixtures so startup also covers a clean install
# of the same exact distributed build already exercised by instrumentation.
"${emulator_adb[@]}" uninstall com.pritam.liquidchat.test >/dev/null 2>&1 || true
"${emulator_adb[@]}" uninstall com.pritam.liquidchat >/dev/null 2>&1 || true
"${emulator_adb[@]}" install "$candidate_apk" > "$report_dir/install.txt"
"${emulator_adb[@]}" shell am start -W -n com.pritam.liquidchat/.MainActivity > "$report_dir/launch.txt"
sleep 3
if ! "${emulator_adb[@]}" shell pidof com.pritam.liquidchat > "$report_dir/process.txt"; then
    "${emulator_adb[@]}" logcat -d -b crash > "$report_dir/startup-crash.txt"
    exit 1
fi
"${emulator_adb[@]}" exec-out screencap -p > "$report_dir/startup.png"
"${emulator_adb[@]}" shell am force-stop com.pritam.liquidchat
