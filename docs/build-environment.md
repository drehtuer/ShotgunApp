# Build environment

How to build, test and run Shotgun!. The short version lives in
[`README.md`](../README.md); the working rules live in
[`.claude/CLAUDE.md`](https://github.com/drehtuer/ShotgunApp/blob/main/.claude/CLAUDE.md).

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

Versions are pinned in [`gradle/libs.versions.toml`](https://github.com/drehtuer/ShotgunApp/blob/main/gradle/libs.versions.toml).
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

Output lands in `app/build/outputs/apk/` and `app/build/outputs/bundle/`,
named for people rather than for Gradle:

| Build | File |
| --- | --- |
| release | `Shotgun-<version>.apk` |
| debug | `Shotgun-debug-<version>.apk` |

The version comes from `appVersion` in `app/build.gradle.kts`, which is also
what `versionName` is set from - one value, so the file name and the manifest
cannot disagree. The release workflow **fails if the tag and `appVersion`
disagree**: the artifacts are named from the build file, so a `v0.2.0` tag on
an unchanged `appVersion` would publish files called `Shotgun-0.1.0.apk`, and a
published release cannot be corrected.

The bundle keeps Gradle's own name until the release workflow renames it to
`Shotgun-<version>.aab` alongside the APK.

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
| **release** | your machine, and a GitHub Actions secret | release builds |

Both keys are held as **encrypted Actions secrets**, never as files in the
repository. That distinction is the whole point: this repository is public, so a
committed key would be world-readable, while a secret is encrypted at rest and
is not exposed to pull requests from forks.

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
| `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | `release.yml` |

All eight are configured, so CI signs both variants end to end, and **nothing
further needs to be added**. If the release secrets are ever removed,
`release.yml` fails on the spot rather than publishing unsigned artifacts: a
published release is immutable, so an unsigned APK would burn that version
number permanently. It also runs `apksigner verify` on the built APK, because
the Gradle config falls back to an unsigned build when the keystore does not
load - the signature is confirmed on the artifact, not inferred from the
secret being present.

Every workflow deletes the restored keystore in an `always()` step, so it never
survives into a later step or an uploaded artifact.

**Back the release key up somewhere off this machine and off GitHub.** A secret
can be read back by no one, including you - it is write-only once set. If the
local copy is lost, the key is gone, and no future build can update an
installed app.

## Persistence

Two stores, both under `app/src/main/java/de/drehtuer/shotgun/data/`:

| Store | Backed by | Holds |
| --- | --- | --- |
| `DrawHistory` | Room (`shotgun.db`) | every completed draw, for the fairness field |
| `SettingsRepository` | DataStore (`settings.preferences_pb`) | theme, haptics, dim, countdown, reveal timing |

**Room schemas are committed** to `app/schemas/`. Bump `version` in
`ShotgunDatabase` and add a `Migration` for every schema change; the exported
JSON gives a real before-and-after to write it against.

`fallbackToDestructiveMigration` is deliberately not set. History is what makes
the fairness field evidence rather than decoration, so a schema change must
migrate it, not discard it.

Inspecting either on a device:

```bash
adb shell run-as de.drehtuer.shotgun ls -l files/datastore databases
adb exec-out run-as de.drehtuer.shotgun cat databases/shotgun.db > shotgun.db
```

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
adb -s <serial> install -r app/build/outputs/apk/debug/Shotgun-debug-*.apk
```

## CI

| Workflow | Trigger | Does |
| --- | --- | --- |
| [`pr.yml`](https://github.com/drehtuer/ShotgunApp/blob/main/.github/workflows/pr.yml) | **any** pull request, push to `main` | builds debug, then unit tests and lint |
| [`release.yml`](https://github.com/drehtuer/ShotgunApp/blob/main/.github/workflows/release.yml) | **a `v*` tag only** | builds and signs the release APK and AAB, drafts the release with them, then publishes it |
| [`docs.yml`](https://github.com/drehtuer/ShotgunApp/blob/main/.github/workflows/docs.yml) | **a `v*` tag only**, manual | builds the Pages site with Jekyll plus Dokka, and deploys it |

Releases are cut by tagging. Nothing in `release.yml` runs for ordinary pushes
or pull requests:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

### Why the release is drafted first

**A published release is immutable: its assets cannot be added to, replaced or
removed.** So the workflow attaches the APK and AAB to a *draft*, and publishes
only once the upload has finished. Attaching to an already-published release
would fail, and a release published empty could never be corrected - the tag
would have to be abandoned.

### Why the docs workflow watches the tag, not the release

It would read better as `release: published` - the site describes a release, so
it should follow one. It would also never run. **A release published by a
workflow using the default `GITHUB_TOKEN` does not trigger further workflow
runs**, which is GitHub's guard against workflows setting each other off in a
loop. The documentation would simply never publish, with nothing in any log to
say why.

So both workflows watch the same `v*` tag and run independently. The tag is
pushed by a person, so it triggers as expected.

Building in CI rather than locally is what makes the immutability worth
anything. The artifacts come from a clean checkout of the tag, so what is
published is exactly what the tag contains - a local build cannot promise that,
and with an immutable release there is no way to correct one after the fact.

Test and lint reports are uploaded as artifacts, including on failure, so a red
run can be diagnosed without reproducing it locally.

`pr.yml` deliberately has **no base-branch filter**. Stacked pull requests are
based on the branch below them rather than on `main`, and a filter of
`branches: [main]` would leave every PR in a stack unverified.

### The documentation site

The site is built by `actions/jekyll-build-pages` - **GitHub's own Pages
toolchain** - from `_config.yml` at the repository root. It renders the Markdown
that already lives here, `README.md` and `docs/`, rather than a copy, so the
published site cannot drift from what a reader sees in the repository.

| Piece | Where |
| --- | --- |
| Site configuration | [`_config.yml`](https://github.com/drehtuer/ShotgunApp/blob/main/_config.yml) |
| Page layout, navigation, Mermaid | [`_layouts/default.html`](https://github.com/drehtuer/ShotgunApp/blob/main/_layouts/default.html) |
| Stylesheet, from the app palette | [`assets/css/site.css`](https://github.com/drehtuer/ShotgunApp/blob/main/assets/css/site.css) |
| API documentation | Dokka, copied to `/api/` after the Jekyll build |

Four things about it are easy to get wrong:

- **`baseurl` must be `/ShotgunApp`.** It is a project site, served from a
  subdirectory, so every internal link goes through Jekyll's `relative_url`
  filter. Without it the stylesheet and navigation 404.
- **Links between documents work because of `jekyll-relative-links`**, which
  turns `docs/design.md` into the rendered page. It only knows about files
  Jekyll publishes, so links to source, workflows or the design export are
  written as absolute GitHub URLs in the Markdown - they resolve both in the
  repository and on the site.
- **Mermaid is not rendered by Pages.** GitHub renders Mermaid in its *repository*
  Markdown view, not on Pages, so the layout loads Mermaid from a CDN and
  unwraps Kramdown's `<pre><code class="language-mermaid">` into the element
  Mermaid expects. Diagrams are hidden until it has run, so a reader never sees
  raw diagram source.
- **Dokka's output is copied in after Jekyll**, not before: it contains
  underscore-prefixed files, which Jekyll would silently drop.

**GitHub Pages must be set to Source: GitHub Actions** in the repository
settings, or the deploy step fails.

To preview the site exactly as CI builds it, run the same container image:

```bash
docker run --rm --user "$(id -u):$(id -g)" -v "$PWD":/w -e GITHUB_WORKSPACE=/w \
  -e INPUT_SOURCE=./ -e INPUT_DESTINATION=./_site \
  ghcr.io/actions/jekyll-build-pages:latest
```

`_site/` is gitignored. Note that links are absolute under `/ShotgunApp/`, so
serve it from that path rather than opening the files directly.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `The 'org.jetbrains.kotlin.android' plugin is no longer required` | Someone re-added the plugin. Remove it. |
| `NoClassDefFoundError: ProjectTypeBinding` on any task | Gradle is older than 9. Use the wrapper. |
| `requires ... compile against version 37 or later` | `compileSdk` was lowered, or AndroidX was bumped past the toolchain. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Debug keystore changed. `adb uninstall de.drehtuer.shotgun`, then reinstall. |
| `x86_64 emulation currently requires hardware acceleration` | `/dev/kvm` missing or not writable. |
| Emulator says `Unknown AVD name` | `ANDROID_AVD_HOME` is not set; the AVD lives in the SDK volume. |
