# Build environment

How to build, test and run Shotgun!. The short version lives in
[`README.md`](../README.md); the working rules live in
[`.claude/CLAUDE.md`](../.claude/CLAUDE.md).

## Toolchain

| | Version | Note |
| --- | --- | --- |
| AGP | 9.4.0 | |
| Gradle | 9.7.1 | via the wrapper |
| Kotlin / Compose plugin | 2.4.10 | |
| JDK | 21 | |
| compileSdk / targetSdk | 37 (Android 17) | matches the target phone exactly |
| minSdk | 26 | |
| Compose BOM | 2026.08.00 | |

**AGP 9 has built-in Kotlin support, so there is deliberately no
`kotlin-android` plugin.** Adding it fails the build with *"no longer required
since AGP 9.0"*. This is the single most likely thing to trip someone up.

The move to AGP 9 was not cosmetic: current AndroidX (`navigation-compose`
2.10.0) requires compileSdk 37 and AGP 9.1+, so AGP 8.x cannot build this
project at all.

Versions are pinned in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml).
Keep `.devcontainer/post-create.sh` in step with it - it installs the matching
SDK packages.

## Devcontainer (recommended)

The repo ships a devcontainer with the SDK, an emulator and `platform-tools`.
Open the folder in VS Code and **Reopen in Container**.

The first create downloads ~1–2 GB of SDK packages. They land in named volumes,
so later rebuilds are fast:

| Volume | Holds | Why it is a volume |
| --- | --- | --- |
| `shotgun-android-sdk` | SDK packages + AVDs | avoids re-downloading |
| `shotgun-gradle` | Gradle cache | avoids re-resolving |
| `shotgun-dotandroid` | `~/.android` | **the debug keystore** |

That last one matters more than it looks: a regenerated debug keystore makes
`installDebug` fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` until you
uninstall the app from the device by hand.

`/dev/kvm` is passed through for emulator acceleration, and `post-create.sh`
opens its permissions - it arrives owned by a host group that does not exist
inside the container, and without write access the emulator refuses to start
x86_64 images at all.

Networking is left on the default **bridge**, not host. `adb connect` is an
outbound connection, so Wi-Fi debugging to a phone on the LAN works through NAT
without the complications host networking brings.

### Building without the devcontainer

You need JDK 21 and an Android SDK with `platforms;android-37.0` and
`build-tools;37.0.0`. Point Gradle at it with either `ANDROID_HOME` or a
`local.properties` containing `sdk.dir=/path/to/sdk`. `local.properties` is
gitignored.

## Building

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK
./gradlew bundleRelease          # release AAB
./gradlew installDebug           # build and install on the connected device
```

Output lands in `app/build/outputs/apk/` and `app/build/outputs/bundle/`.

### The two variants

| | `debug` | `release` |
| --- | --- | --- |
| Debuggable | yes | no |
| Minified (R8) | no | yes |
| Resource shrinking | no | yes |
| Debug symbols | kept | stripped (`debugSymbolLevel = NONE`) |
| Version name | `0.1.0-debug` | `0.1.0` |
| Signed with | dev key | release key, when present |

`debug` is the build to test on a device: unminified, with symbols, so a stack
trace means something.

Two deliberate omissions in `debug`:

- **No `applicationIdSuffix`.** It would rename the package for debug builds
  and break every `adb` command in this document.
- **No `enableAndroidTestCoverage`.** It instruments the APK itself, and this
  app is judged on touch and countdown timing - a slowed build would misreport
  how it feels. Unit-test coverage is on; it does not touch the APK.

## Signing

**This repository is public. No signing material is ever committed to it.**
`.gitignore` covers `*.keystore`, `*.jks`, `keystore/` and `keystore.properties`.

There are two keys:

| Key | Lives | Used for |
| --- | --- | --- |
| **dev** | your machine, and a GitHub Actions secret | debug builds, CI |
| **release** | your machine only - **never uploaded anywhere** | release builds |

### Local setup

Gradle reads `keystore.properties` from the repository root:

```properties
debug.storeFile=keystore/debug.keystore
debug.storePassword=…
debug.keyAlias=shotgun-debug
debug.keyPassword=…

release.storeFile=keystore/release.keystore
release.storePassword=…
release.keyAlias=shotgun-release
release.keyPassword=…
```

Both the file and the keystores are gitignored. If they are missing the build
still works: `debug` falls back to the SDK's own debug key, and `release` is
produced **unsigned**.

### Generating a key

```bash
keytool -genkeypair -v \
  -keystore keystore/release.keystore -alias shotgun-release \
  -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=Shotgun!, OU=Release, O=drehtuer, C=DE"
```

**Back the release key up somewhere off this machine.** Losing it means no
future build can update an installed app - Android identifies an app by its
signature, and there is no recovery.

### CI

The dev key reaches CI as base64 in a secret:

```bash
base64 -w0 keystore/debug.keystore    # paste into the DEBUG_KEYSTORE_BASE64 secret
```

| Secret | Used by |
| --- | --- |
| `DEBUG_KEYSTORE_BASE64`, `DEBUG_KEYSTORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD` | `pr.yml` |
| `RELEASE_KEYSTORE_BASE64`, … | `release.yml`, **only if you choose to add them** |

**The release key is deliberately not on GitHub**, and the consequence is
explicit: the release workflow produces **unsigned** artifacts and warns in the
job log. Sign them locally before distributing:

```bash
./gradlew assembleRelease bundleRelease    # signed, because the key is local
```

Every workflow deletes the restored keystore in an `always()` step, so it never
survives into a later step or an uploaded artifact.

## Testing

Two layers run without a device, one needs one.

```bash
./gradlew lint                   # static analysis
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew connectedDebugAndroidTest   # instrumented, needs emulator or phone
```

Unit tests live in `app/src/test/`. Reports:

- Unit tests — `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint — `app/build/reports/lint-results-debug.html`

Lint must pass clean. Suppress a genuine false positive *narrowly*, with
`tools:ignore` or `@Suppress` plus a comment - never disable a check globally.

There is one standing false positive: `R.mipmap.ic_launcher_round` is reported
unused, but it is referenced from the manifest's `android:roundIcon`.

## Emulator

```bash
./.devcontainer/emulator-start.sh        # boots pixel9a-api37 headless
adb devices                              # confirm it attached
```

The AVD is a **`pixel_9a` profile on an API 37 image**. The target phone is a
Pixel 10a, which has no profile in the emulator's device catalogue yet, so
`pixel_9a` is the closest hardware match - the API level matches exactly.

Useful while checking a change:

```bash
adb shell am start -n de.drehtuer.shotgun/.MainActivity
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png
adb logcat -d -b crash | tail -20

adb shell cmd uimode night yes    # dark palette
adb shell cmd uimode night no     # light palette
```

Always check both palettes. Theme bugs routinely show in only one - the app can
override the system theme from Settings, so the two can disagree.

## Real device over Wi-Fi

**The emulator cannot test multi-touch**, which is the entire draw surface. Any
change to touch handling, haptics or countdown timing has to be checked on the
phone.

```bash
# Phone: Developer options > Wireless debugging > Pair device with pairing code
./.devcontainer/connect-device.sh pair <ip>:<pairingPort> <code>
./.devcontainer/connect-device.sh connect <ip>:5555
./gradlew installDebug
```

The pairing port and the connect port are **different** - the pairing dialog
shows one, the Wireless debugging screen shows the other. Run the script with
no arguments for the full notes, including the older `adb tcpip 5555` route for
pre-Android-11 devices.

With both an emulator and a phone attached, target one explicitly:

```bash
adb devices
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## CI

| Workflow | Trigger | Does |
| --- | --- | --- |
| [`pr.yml`](../.github/workflows/pr.yml) | PR to `main`, push to `main` | builds debug, then unit tests and lint |
| [`release.yml`](../.github/workflows/release.yml) | **a `v*` tag only** | builds the release APK and AAB and attaches them to the release |
| [`docs.yml`](../.github/workflows/docs.yml) | `release: published`, manual | builds and deploys the Pages site |

Releases are cut by tagging. Nothing in `release.yml` runs for ordinary pushes
or pull requests:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

Test and lint reports are uploaded as artifacts, including on failure, so a red
run can be diagnosed without reproducing it locally.

The docs workflow renders Markdown with pandoc and generates API docs with
Dokka. Pages serves the uploaded artifact **as-is** - there is no Jekyll step in
the Actions-based flow - which is why the Markdown is rendered in the workflow
rather than shipped raw.

**GitHub Pages must be set to Source: GitHub Actions** in the repository
settings, or the deploy step fails.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `The 'org.jetbrains.kotlin.android' plugin is no longer required` | Someone re-added the plugin. Remove it. |
| `NoClassDefFoundError: ProjectTypeBinding` on any task | Gradle is older than 9. Use the wrapper. |
| `requires ... compile against version 37 or later` | `compileSdk` was lowered, or AndroidX was bumped past the toolchain. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Debug keystore changed. `adb uninstall de.drehtuer.shotgun`, then reinstall. |
| `x86_64 emulation currently requires hardware acceleration` | `/dev/kvm` missing or not writable. |
| Emulator says `Unknown AVD name` | `ANDROID_AVD_HOME` is not set; the AVD lives in the SDK volume. |
