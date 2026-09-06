# Shotgun!

An Android app for settling *who goes first*. Everyone puts a finger on the
screen, a countdown runs, and the app draws — a starting player, a full player
order, or teams.

The mark is the game itself: four fingers on the glass, one of them called -
specified in [`design/Shotgun Logo.dc.html`](design/Shotgun%20Logo.dc.html),
with rendered assets in [`assets/logo/`](assets/logo/).

The design lives in [`design/Shotgun.dc.html`](design/Shotgun.dc.html),
exported from a Claude Design project. It is the spec for this implementation:
screens, copy, palette and interaction behaviour all come from there.
`design/_ds/` is the bound copy of the **Modernist** design system the export
was built against.

## Status

**Skeleton + theme.** The project builds and runs, navigation works end to end,
and the design tokens are in place. The screens themselves are stubs.

| Piece | State |
| --- | --- |
| Gradle project, Compose setup | done |
| Modernist theme: colour, type, dimensions | done |
| Navigation shell across all four screens | done |
| Appearance setting (system / light / dark) | done |
| Home: mode cards, team stepper | done |
| Draw surface: multi-touch, countdown, reveal | done, unverified on real fingers |
| Result: fairness heatmap | done |
| Settings: haptics, dim, countdown, timing | stub |
| Draw history database | done |
| Identity: name, logo, launcher icon | done |
| Settings persistence | done |

## Design tokens

The `--pp-*` custom properties from the export are transcribed into
[`ui/theme/Color.kt`](app/src/main/java/de/drehtuer/shotgun/ui/theme/Color.kt)
as a `PPColors` palette, with light and dark variants, the team ring fills and
the heatmap ramp. Type roles from the design (wordmark, display, card title, micro
label…) are in [`Type.kt`](app/src/main/java/de/drehtuer/shotgun/ui/theme/Type.kt);
Archivo ships bundled in `res/font`.

Read them through `PPTheme`:

```kotlin
Text(
    text = "SHOTGUN!",
    style = PPTheme.typography.wordmark,
    color = PPTheme.colors.ink,
)
```

Modernist rules that the theme encodes: **zero corner radius**, **2px rules**
(`PPTheme.dimens.rule`) doing the organising, and everything set in Archivo.

## Development

The repo ships a devcontainer with the Android SDK, an emulator and
`platform-tools`. Open the folder in VS Code and *Reopen in Container*; the
first create downloads the SDK packages (~1–2 GB) into a named volume, so later
rebuilds are fast.

```bash
./gradlew assembleDebug          # debug build: debuggable, unminified
./gradlew assembleRelease        # release build: R8, shrunk, symbols stripped
./gradlew testDebugUnitTest      # unit tests
./gradlew lint                   # static analysis
./gradlew installDebug           # install on the connected device
```

Signing keys are **never committed** - this repository is public. See
[`docs/build-environment.md`](docs/build-environment.md#signing).

Contributor workflow, testing rules and the emulator/device recipes are in
[`.claude/CLAUDE.md`](.claude/CLAUDE.md).

### Emulator

```bash
./.devcontainer/emulator-start.sh        # boots pixel9a-api37 headless
```

The AVD targets **API 37 (Android 17)** on a `pixel_9a` profile, matching the
target phone's OS version. The Pixel 10a has no profile in the emulator's device
catalogue yet, so `pixel_9a` is the closest hardware match.

The container gets `/dev/kvm` for hardware acceleration.

### A real device over Wi-Fi

The emulator cannot test multi-touch, so the draw surface needs a real phone.
The container uses bridge networking, and `adb connect` is outbound, so this
works without host networking.

```bash
# Android 11+: Developer options > Wireless debugging > Pair device with code
./.devcontainer/connect-device.sh pair 192.168.1.42:41234 123456
./.devcontainer/connect-device.sh connect 192.168.1.42:5555

./gradlew installDebug
```

Run `./.devcontainer/connect-device.sh` with no arguments for the full notes,
including the older `adb tcpip 5555` route.

## Documentation

| Document | What it covers |
| --- | --- |
| [`docs/design.md`](docs/design.md) | The design in Markdown: tokens, screens, state model, behaviour |
| [`docs/build-environment.md`](docs/build-environment.md) | Building, testing, emulator and device, CI, troubleshooting |
| [`docs/TODO.md`](docs/TODO.md) | Open work and the decisions that block it |
| [`docs/STATUS.md`](docs/STATUS.md) | What has been done and how it turned out |
| [`.claude/CLAUDE.md`](.claude/CLAUDE.md) | Working rules: branching, testing, keeping docs in sync |

The design sources these are derived from:

| Source | What it is |
| --- | --- |
| [`design/Shotgun.dc.html`](design/Shotgun.dc.html) | The app design and its state machine - the spec |
| [`design/Shotgun Logo.dc.html`](design/Shotgun%20Logo.dc.html) | The identity: mark geometry, wordmark, brand don'ts |
| [`design/_ds/…/readme.md`](design/_ds/modernist-f7022762-4cb9-409e-a6ce-7116795bae5b/readme.md) | Modernist's own guide, from the bound design system |

Documentation is kept in sync with the implementation - see
[`.claude/CLAUDE.md`](.claude/CLAUDE.md).

## Continuous integration

| Workflow | Runs on | Does |
| --- | --- | --- |
| [`pr.yml`](.github/workflows/pr.yml) | any PR, push to `main` | builds debug, then unit tests and Android Lint |
| [`release.yml`](.github/workflows/release.yml) | a `v*` tag only | builds the release APK and AAB, attaches them to the release |
| [`docs.yml`](.github/workflows/docs.yml) | `release: published`, manual | builds the GitHub Pages site (Dokka API docs + these documents) |

Reports are uploaded as artifacts, including on failure.

## Layout

```
app/           the Android app
assets/logo/   rendered logo assets
design/        the Claude Design export (the spec) + bound Modernist system
docs/          documentation
.devcontainer/ Android SDK, emulator and adb helpers

app/src/main/java/de/drehtuer/shotgun/
  MainActivity.kt
  ui/
    theme/       PPColors, PPTypography, PPDimens, ShotgunTheme
    navigation/  Destination, DrawMode, ShotgunNavHost
    components/  Rule, ScreenHeader, NotBuiltYet, ShotgunWordmark
    screens/     Home, Draw, Result, Settings
    util/        KeepScreenOn, findActivity

app/src/test/    JVM unit tests
```

## Requirements

- minSdk 26, compileSdk / targetSdk 37 (Android 17)
- AGP 9.4.0 on Gradle 9.7.1, JDK 21. AGP 9 has built-in Kotlin support, so
  there is deliberately **no `kotlin-android` plugin** - applying it fails.
- Portrait only, and a touchscreen with distinct multi-touch

Target device: **Pixel 10a running Android 17** (API 37), confirmed - so the
app targets the device's own API level exactly. Verified building and running on
an API 37 emulator; multi-touch still needs the real phone.
