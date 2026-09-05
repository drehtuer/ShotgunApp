#!/usr/bin/env bash
# Boots the AVD headless and waits until it is ready.
set -euo pipefail
AVD_NAME="${1:-pixel9a-api37}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-${ANDROID_HOME}/.avd}"

"${ANDROID_HOME}/emulator/emulator" -avd "$AVD_NAME" \
  -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect \
  >/tmp/emulator.log 2>&1 &

echo "==> Booting ${AVD_NAME} (log: /tmp/emulator.log)"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done
echo "==> Ready."
adb devices
