#!/usr/bin/env bash
# Installs the Android SDK packages into $ANDROID_HOME (a persistent volume),
# then creates an AVD. Safe to re-run: sdkmanager skips what is already there.
set -euo pipefail

: "${ANDROID_HOME:=/opt/android-sdk}"
SDKMANAGER="${CMDLINE_TOOLS:-/opt/android-cmdline-tools}/latest/bin/sdkmanager"
AVDMANAGER="${CMDLINE_TOOLS:-/opt/android-cmdline-tools}/latest/bin/avdmanager"

# Keep these in step with app/build.gradle.kts and gradle/libs.versions.toml.
# API 37 platforms use Android's minor-version scheme (37.0, 37.1, ...), so the
# platform and system-image ids carry the minor while compileSdk stays 37.
COMPILE_SDK=37.0
BUILD_TOOLS=37.0.0
SYSTEM_IMAGE="system-images;android-${COMPILE_SDK};google_apis;x86_64"
# The target device is a Pixel 10a on Android 17 (API 37). It has no profile in
# the emulator's device catalogue yet, so pixel_9a is the closest hardware
# match; the API level matches the phone exactly.
AVD_DEVICE=pixel_9a
AVD_NAME=pixel9a-api37

# Keep AVDs in the SDK volume rather than $HOME, so they survive a container
# rebuild the same way the downloaded packages do.
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-${ANDROID_HOME}/.avd}"
mkdir -p "$ANDROID_AVD_HOME"

echo "==> Accepting SDK licences"
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null 2>&1 || true

echo "==> Installing SDK packages (this is the slow part on first create)"
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" \
  "cmdline-tools;latest" \
  "platform-tools" \
  "platforms;android-${COMPILE_SDK}" \
  "build-tools;${BUILD_TOOLS}" \
  "emulator" \
  "$SYSTEM_IMAGE"

# Prefer the copy sdkmanager just installed inside the SDK: avdmanager infers
# the SDK root from its own location, so running it from $ANDROID_HOME avoids
# "inconsistent location" warnings (and matches what Android Studio expects).
if [ -x "${ANDROID_HOME}/cmdline-tools/latest/bin/avdmanager" ]; then
  AVDMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/avdmanager"
fi

echo "==> Creating AVD '${AVD_NAME}' (if missing)"
if ! "$AVDMANAGER" list avd 2>/dev/null | grep -q "Name: ${AVD_NAME}"; then
  echo "no" | "$AVDMANAGER" create avd \
    --name "$AVD_NAME" \
    --package "$SYSTEM_IMAGE" \
    --device "$AVD_DEVICE" \
    --force
fi

# local.properties is gitignored; Android Studio reads sdk.dir from it. Gradle
# falls back to $ANDROID_HOME, so failing to write it is a warning, not an error.
if ! echo "sdk.dir=${ANDROID_HOME}" > "$(dirname "$0")/../local.properties" 2>/dev/null; then
  echo "NOTE: could not write local.properties (workspace not writable by $(id -un))."
  echo "      Gradle will use ANDROID_HOME=${ANDROID_HOME} instead."
fi

# /dev/kvm is passed through from the host, so it carries the host's group id,
# which usually maps to no group in here. Without write access the emulator
# refuses to start x86_64 images at all.
if [ ! -e /dev/kvm ]; then
  echo "WARNING: /dev/kvm not present - the emulator cannot run x86_64 images."
  echo "         Check the --device=/dev/kvm runArg and that KVM works on the host."
elif [ ! -w /dev/kvm ]; then
  if sudo -n chmod 666 /dev/kvm 2>/dev/null; then
    echo "==> Opened /dev/kvm for $(id -un)"
  else
    echo "WARNING: /dev/kvm is not writable by $(id -un) and it could not be fixed."
    echo "         Run: sudo chmod 666 /dev/kvm"
  fi
fi

echo "==> Done. 'adb devices' to check, './gradlew assembleDebug' to build."
