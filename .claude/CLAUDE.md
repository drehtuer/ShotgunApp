# Working in this repo

Shotgun! is an Android app (Kotlin + Jetpack Compose) built from a Claude
Design export. The design is the spec, not a mood board:
`design/Shotgun.dc.html` carries the real state machine in its embedded
`<script type="text/x-dc">` block. **Read [`docs/design.md`](../docs/design.md)
and the export before implementing any screen behaviour** - countdown timing,
reveal rules, haptics and the fairness field are all defined between them.

## Layout

```
app/            the Android app
design/         the Claude Design export (spec), the identity spec and the
                bound Modernist system
docs/           all documentation except README.md
.devcontainer/  Android SDK, emulator, adb helpers
```

| Document | What it is for |
| --- | --- |
| [`docs/design.md`](../docs/design.md) | The design, in Markdown: tokens, screens, state model, behaviour |
| [`docs/architecture.md`](../docs/architecture.md) | How the app is put together: layers, screen graph, state machines |
| [`docs/build-environment.md`](../docs/build-environment.md) | Building, testing, emulator and device, CI, troubleshooting |
| [`docs/TODO.md`](../docs/TODO.md) | Open work and the decisions that block it |
| [`docs/STATUS.md`](../docs/STATUS.md) | What has been done and how it turned out |

Two more references worth knowing, outside `docs/`:

| Source | What it is for |
| --- | --- |
| [`design/Shotgun Logo.dc.html`](../design/Shotgun%20Logo.dc.html) | The identity spec: mark geometry, wordmark, brand don'ts |
| [`design/_ds/…/readme.md`](../design/_ds/modernist-f7022762-4cb9-409e-a6ce-7116795bae5b/readme.md) | Modernist's own guide, from the bound design system |

## Non-negotiables

- **AGP 9 has built-in Kotlin support.** There is deliberately no
  `kotlin-android` plugin. Applying it fails the build outright. Do not add it.
- **The design owns the visuals.** Take colours, type and spacing from
  `PPTheme.colors` / `.typography` / `.dimens` - never hard-code a hex, a sp
  value or a corner radius. Modernist is flat: zero radius, 2px rules.
- **Portrait only.** The draw surface is a fixed field players reach across;
  rotating mid-draw would move every finger's ring.
- **Documentation stays in sync with the implementation.** Docs are part of the
  change, not a follow-up - see below.

## Task workflow

Every task follows the same loop. Do not skip the middle step.

1. **Branch first.** Never commit to `main`.
   ```bash
   git checkout main && git pull --ff-only
   git checkout -b <type>/<short-name>      # feat/, fix/, chore/, docs/
   ```
2. **Implement, then test before committing** - see *Testing* below. Both the
   static checks and the unit tests must be green.
3. **Update the documentation in the same commit** - see *Documentation* below.
4. **Commit and open a PR** once the task is done and tests pass.
   ```bash
   git push -u origin <branch>
   gh pr create --base main --fill
   ```
   Say in the PR body what was verified and what was left out.

CI runs the same checks on the PR. A red PR does not get merged - fix it rather
than merging around it.

## Documentation

**Documentation is kept in sync with the implementation, always.** A change that
makes a document wrong is not finished. Update it in the *same* commit as the
code - never as a follow-up, because follow-ups do not happen and stale docs are
worse than none: they get trusted.

Work out which of these your change touches, and update it:

| If you changed... | Update |
| --- | --- |
| Screen behaviour, tokens, copy, state | [`docs/design.md`](../docs/design.md) |
| Layers, navigation, state ownership, a new screen | [`docs/architecture.md`](../docs/architecture.md) |
| Toolchain, Gradle tasks, CI, devcontainer, emulator/device setup | [`docs/build-environment.md`](../docs/build-environment.md) |
| Anything on the open list, or found new work | [`docs/TODO.md`](../docs/TODO.md) |
| Finished a task | [`docs/STATUS.md`](../docs/STATUS.md) - what you did **and how it turned out**, including what went wrong |
| Project layout, commands, status table | [`README.md`](../README.md) |
| The rules themselves | this file |

Two specific rules:

- **Know which source wins.** `design/Shotgun.dc.html` is the authority on
  *visuals* - layout, tokens, copy, the interaction model. `docs/design.md` is
  the specification of record for *behaviour*, including the parts a browser
  prototype cannot express (dim mode, the draw database). Disagree on visuals ->
  fix `docs/design.md`. New behaviour decided -> specify it in `docs/design.md`,
  and update the export too if it can express it.
- **Record outcomes honestly in `STATUS.md`.** A bug you hit and fixed is worth
  more to the next person than a clean summary that hides it.

## Testing

**Every function gets tested before it is committed.** Two layers, both
required:

### Static checks

```bash
./gradlew assembleDebug lint
```

`lint` is the static analysis gate. It must pass clean. If a warning is a
genuine false positive, suppress it *narrowly* with `tools:ignore` or
`@Suppress` plus a comment saying why - never disable the check globally.

### Unit tests (JVM, no device)

```bash
./gradlew testDebugUnitTest
```

Put them in `app/src/test/`. This is where pure logic belongs: draw and shuffle
rules, countdown arithmetic, route building, palette invariants, heatmap
density maths. **Prefer moving logic out of composables into plain functions so
it can be tested here** - it is fast and runs in CI.

### Instrumented tests (emulator or device)

```bash
./gradlew connectedDebugAndroidTest
```

For anything needing the Android framework or Compose UI. Needs a running
emulator or a connected phone (below).

## Running on the emulator

```bash
./.devcontainer/emulator-start.sh          # boots pixel9a-api37 headless
adb devices                                # confirm it attached
./gradlew installDebug
adb shell am start -n de.drehtuer.shotgun/.MainActivity
```

Screenshot what you changed, and check for crashes:

```bash
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png
adb logcat -d -b crash | tail -20
```

Check **both palettes** - a lot of theme bugs only show in one:

```bash
adb shell cmd uimode night yes    # dark
adb shell cmd uimode night no     # light
```

The AVD uses a `pixel_9a` profile because the target phone, a **Pixel 10a on
Android 17 (API 37)**, has no profile in the emulator's device catalogue yet.
The API level matches the phone exactly.

## Running on the real device

**The emulator cannot test multi-touch.** The draw surface is the whole point
of the app and needs several real fingers, so anything touching it must be
checked on the phone before the PR is called done.

The container uses bridge networking and `adb connect` is outbound, so Wi-Fi
debugging works without host networking:

```bash
# Phone: Developer options > Wireless debugging > Pair device with pairing code
./.devcontainer/connect-device.sh pair <ip>:<pairingPort> <code>
./.devcontainer/connect-device.sh connect <ip>:5555
./gradlew installDebug
```

Run the script with no arguments for the full notes, including the older
`adb tcpip 5555` route.

If both an emulator and a phone are attached, target one explicitly:

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Before opening the PR

- [ ] `./gradlew assembleDebug lint` clean
- [ ] `./gradlew testDebugUnitTest` green, with tests for the code you added
- [ ] Ran on the emulator; checked light and dark
- [ ] Ran on the phone if you touched multi-touch, haptics or timing
- [ ] `./gradlew assembleRelease` still builds (R8 is on for release)
- [ ] Documentation updated in the same commit (see *Documentation*)
- [ ] `docs/STATUS.md` has an entry, and `docs/TODO.md` reflects reality
