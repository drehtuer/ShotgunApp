# Status

A running record of work done and how it turned out. Newest first. Add an entry
when a task lands; keep it factual, including the parts that went wrong.

See [`TODO.md`](TODO.md) for what is still open.

## Current state

**Skeleton + theme.** The project builds, runs on device and in CI, navigation
works end to end, and the design tokens are in place. The four screens are
stubs.

| Area | State |
| --- | --- |
| Gradle project, Compose setup | done |
| Modernist theme: colour, type, dimensions | done |
| Navigation shell across all four screens | done |
| Appearance setting (system / light / dark) | done, not persisted |
| Devcontainer: SDK, emulator, adb over Wi-Fi | done |
| CI: build, unit tests, lint | done |
| Docs: Pages on release | done, never run yet |
| Specification: all open questions answered | done |
| Home: mode cards, team stepper | stub |
| Draw surface: multi-touch, countdown, reveal | stub |
| Draw surface: keeps the screen awake | done |
| Result: fairness heatmap | stub |
| Settings: haptics, dim, countdown, timing | stub |
| Draw history database | not started |
| Settings persistence | not started |

---

## 2026-09-05 — Keep the screen awake during a draw

**Outcome: done and verified on device.**

The screen could dim or lock mid-draw. Android resets its idle timer on touch
*events*, and a finger held still through the countdown produces none - so the
one moment the screen must stay on is exactly the moment the system counts as
idle.

Fixed with `FLAG_KEEP_SCREEN_ON`, held by a `KeepScreenOn()` composable for as
long as the draw surface is in the composition and released on the way out.
Scoped to that screen so the flag cannot leak into Home, Result or Settings.

Also extracted the `Context.findActivity()` helper that `Theme.kt` already had
into `ui/util/`, rather than writing it a second time.

Verified with `dumpsys window` on the emulator, walking the whole navigation
graph: absent on Home, **present on the draw surface**, released on navigating
back, re-acquired on entering it again, released again on Result, and
re-acquired on returning to the draw surface. No crashes.

The first attempt to verify this reported a false negative - `grep -A3` after
the window line stopped short of the `fl=` line it needed. Worth remembering:
the flag shows up as `fl=KEEP_SCREEN_ON ...` about five lines into the window
block.

## 2026-09-05 — Specification decisions

**Outcome: done. All three open questions answered; no code changed yet.**

- **SOUND removed.** Not needed. Taken out of the design export (state field and
  settings row) and out of the documentation.
- **DIM MODE defined** as lowering screen brightness like an alarm clock -
  the app's own window only, restored on leave, and explicitly *not* a palette
  change.
- **Haptics redefined.** Was a single buzz when the draw fired. Now a `12 ms`
  keyboard-style tick per finger down, and a stronger `[90]` / `[90, 60, 90]`
  on the result. The tick deliberately does **not** fire on drag or lift -
  dragging is repositioning, not joining, and buzzing on it would contradict
  the rule the hint text teaches.
- **Draw history to a database.** The heatmap plots real recorded positions
  rather than the export's seeded sample set. Positions are normalised to 0..1
  at write time so history survives a device or surface-size change.

The export was edited directly for sound and haptics; its script was
syntax-checked after each change. Dim mode and the database cannot be expressed
in a browser prototype, which forced a question about which document wins.
Resolved by splitting authority: **the export owns visuals, `docs/design.md`
owns behaviour**, recorded in both files and in `.claude/CLAUDE.md`.

GitHub Pages has been enabled with Source: GitHub Actions, so the docs workflow
can now deploy. It has still never run - the first release will be its first
real exercise.

## 2026-09-05 — Documentation set

**Outcome: done.**

Added `docs/design.md` (the design export translated to Markdown),
`docs/build-environment.md`, `TODO.md` and this file. Added the
documentation-stays-in-sync rule to `.claude/CLAUDE.md`.

`design.md` is a translation, not a replacement: the `.dc.html` export stays the
source of truth because it holds the executable state machine.

Two gaps surfaced while translating: **`sound` and `dim` appear in the design's
settings but have no behaviour defined anywhere in the export.** Both were
answered the same day - see the entry above.

## 2026-09-05 — Restructure, contributor guide and CI (PR #2)

**Outcome: done.**

Moved the design export to `design/` and documentation to `docs/`; the
`.dc.html` references its siblings relatively, so they moved together and the
links still resolve. Added `.claude/CLAUDE.md`, the first 11 unit tests, and the
two workflows.

Verified: `assembleDebug`, `testDebugUnitTest` (11 passed) and `lintDebug` all
green; `dokkaGenerate` produces `app/build/dokka/html`; the Pages site assembly
was dry-run end to end.

**Found and fixed while building it:** the first version of the docs workflow
would have served the landing page as raw Markdown. Pages serves the uploaded
artifact as-is - there is no Jekyll step in the Actions-based flow - so the
workflow now renders Markdown with pandoc and rewrites repo-file links to
GitHub URLs.

Neither workflow could run on its own PR, since workflows must exist on `main`
first. Their first real exercise is the following PR.

## 2026-09-05 — Retarget to the Pixel 10a

**Outcome: done, and it forced a toolchain jump.**

The target device was confirmed as a **Pixel 10a on Android 17 (API 37)**.

Raising `compileSdk` to 36 failed immediately: current AndroidX
(`navigation-compose` 2.10.0) requires **compileSdk 37 and AGP 9.1+**. So the
whole toolchain moved - AGP 8.7.3 → 9.4.0, Gradle 8.11.1 → 9.7.1, Kotlin
2.0.21 → 2.4.10, compileSdk/targetSdk 35 → 37.

**AGP 9 has built-in Kotlin support: the `kotlin-android` plugin now fails the
build and had to be removed.** This is recorded in several places because it is
easy to re-add by reflex.

The emulator has no `pixel_10a` device profile; `pixel_9a` on an API 37 image is
the closest match. Verified on that emulator, which reported
`ro.build.version.release=17`.

## 2026-09-05 — Android skeleton and Modernist theme (PR #1)

**Outcome: done.**

Set up the Gradle project, the theme transcribed from the design's `--pp-*`
tokens, the navigation shell and stub screens, and the devcontainer.

Verified on an emulator: renders correctly in both palettes, navigation works,
no crashes.

**Two bugs found only by running it**, both fixed:

1. System bar icons followed `uiMode` rather than the app's palette, so the
   clock was unreadable when the app's theme was overridden.
2. The settings option dividers used a hardcoded height instead of the row's
   intrinsic height.

Several devcontainer problems also only appeared under use: `/dev/kvm` arrives
owned by a nonexistent group; AVDs and the debug keystore vanished on rebuild
until they were moved into named volumes.

## 2026-09-05 — Design export imported

**Outcome: done.**

The repository began as an export from a Claude Design project, with no commits
on `main`. `/design-sync` does not apply here - the repo is a *consumer* of a
design system, not the source of one, and there was nothing to convert.
