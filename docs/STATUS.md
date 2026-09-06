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
| Identity: name, logo, launcher icon | done |
| Settings persistence | not started |

---

## 2026-09-06 — Result screen and the fairness field

**Outcome: done, and the field is real history rather than a picture of one.**

The density maths lives in a plain `HeatField` object with no Android types, so
it is unit-tested - eleven tests. That mattered more here than usual: the screen
makes a claim about fairness, and "it looks about right" is not a check of a
claim.

The test worth keeping is **a corner win registering as hot as a centre win**.
Without edge mirroring a win in the corner spreads into a quarter of the
kernel's area instead of all of it, so the corners read as permanently cold -
the screen would libel the draw as unfair when it is not.

The field is computed at 160px wide and scaled up. It is a blur either way, and
this keeps a large history cheap to draw.

**Verified by seeding 241 draws into the real on-device database** and looking
at it: the field covers the panel with no cold corners, the last draw's dots sit
on top with the winner marked, and the mode line and caption read correctly.

Two things that cost time and are worth remembering:

- `gradle connectedAndroidTest` **uninstalls the app afterwards**, which deletes
  its database. Seeding through it and then looking at the app shows an empty
  screen. Running the instrumentation directly with `adb shell am instrument`
  leaves the data in place.
- `--tests` is not a valid option for `connectedAndroidTest`; the filter is
  `-Pandroid.testInstrumentationRunnerArguments.class=…`.

50 unit tests and 16 instrumented tests pass.

## 2026-09-06 — Draw surface

**Outcome: built and tested as far as this hardware allows. Not yet touched by
real fingers.**

The rules live in a plain `DrawEngine` with no Android in it - time is passed in
and randomness injected - so the countdown extension, the teams guard, the
reveal rules and who wins are all unit-tested. That split was the point: the one
thing that cannot be tested without hardware is the multi-touch itself, so
everything else was made testable without it.

**Multi-touch turned out to be testable after all, partially.** The emulator
exposes one input device per contact and so cannot inject a genuine
multi-pointer gesture, but Compose's own pointer injection can. Six instrumented
tests now cover several pointers alive at once, lifting one of them, and moving
without joining - the tracking that unit tests cannot reach.

Also closed the gap flagged in the persistence PR: the Room DAO now has six
instrumented tests of its own, including that pruning cascades to points rather
than orphaning them.

Two deliberate divergences from the export, both recorded in `design.md`:

- **Lifting removes a ring.** The prototype was mouse-driven and could not lift,
  so it removed players on double-tap and left rings on screen. On a touchscreen
  a ring belongs to a finger. "LIFT ALL FINGERS TO CLEAR" already assumes this,
  which makes double-tap redundant.
- The suspense **churn animation** is not implemented - the pause happens, the
  rings do not pulse yet.

Verified on an API 37 emulator: the hint reads correctly, and a single finger
held for two and a half seconds never draws and never writes to the database.
40 unit tests and 12 instrumented tests pass.

**What is unverified:** feel. Timing, haptic strength, and whether the countdown
is long enough to get a hand down are all judgements that need the Pixel 10a.

## 2026-09-06 — Home screen

**Outcome: done.**

Mode cards with their four-dot motifs, the team stepper and the footer link.
The motifs are drawn from the palette rather than hard-coded, so team colours
stay in step with `PPColors.teamFills`.

The team count deliberately lives in the ViewModel rather than in settings: the
design keeps it beside the draw, not among the preferences, so it resets with
the app the way the mode does. Its clamp is a pure function so the floor is
unit-tested rather than only observed.

**Two bugs found by looking at it on a device**, neither visible from the code:

- The vertical SETTINGS tab wrapped mid-word - "SETTIN / GS". Compose has no
  `writing-mode`, and my first attempt measured the rotated label against the
  tab's 60dp width. Fixed by measuring it unbounded and rotating only for
  drawing.
- The stepper's minus stayed at full contrast at the floor, so it looked live
  while doing nothing. It now dims.

Verified on an API 37 emulator in both palettes: motifs, stepper floor (3 → 2,
then held), and navigation from each card into the right draw mode.

## 2026-09-06 — Signing, build variants, persistence

**Outcome: done, in two stacked PRs.**

**Signing and build variants.** `debug` carries full debug data; `release` is
R8-minified, resource-shrunk and symbol-stripped - 1.2 MB against 12 MB. Both
keys are generated locally and held as encrypted Actions secrets; nothing
signing-related is ever committed, because this repository is public. A new
`release.yml` fires only on a `v*` tag.

Two deliberate omissions in `debug`, both easy to add by reflex and both wrong
here: no `applicationIdSuffix` (it would break every documented adb command) and
no `enableAndroidTestCoverage` (it instruments the APK, and this app is judged
on touch and countdown timing).

**Persistence.** Room for draw history, DataStore for settings.

The integration risk was KSP: it has moved to independent versioning, and AGP 9
supplies its own Kotlin, so the two could easily have disagreed. KSP 2.3.11
works against Kotlin 2.4.10 under AGP 9 - checked before building anything on
top of it.

Positions are normalised to 0..1 **at write time**, not stored as pixels. That
is the one thing in this layer that can corrupt data silently: raw pixels would
skew the fairness field, and the damage is invisible until a screen size
changes. It has tests, including the degenerate zero-sized surface that would
otherwise divide by zero.

`fallbackToDestructiveMigration` is deliberately **not** set: history is the
whole point of the fairness field, and wiping it on a schema change would make
that screen lie.

Verified on an API 37 emulator: settings picked in the app survive a
`force-stop` - the system was in light mode and the app relaunched dark, so the
value came from disk rather than memory - and `settings.preferences_pb` exists
on disk.

## 2026-09-06 — Documentation reference audit

**Outcome: done. Found more wrong than missing.**

Checked whether `README.md` referenced every document in the repo. It did not -
but the omissions mattered less than three stale references, two of which this
project's own docs-stay-in-sync rule was written to prevent.

Wrong, and now fixed:

- `.claude/CLAUDE.md` opened by pointing at `design/Player Picker.dc.html`, a
  file **deleted** in the rename. It survived the rename because the path was
  line-wrapped across `design/Player` / `Picker.dc.html`, so the search and
  replace never matched it. The worst of the three: it is the first instruction
  anyone reads, and it named a file that no longer exists.
- `README.md`'s layout tree still said `PlayerPickerTheme` and
  `PlayerPickerNavHost`; both were renamed in the same PR. They survived for the
  same class of reason - inside a fenced code block.
- That tree also omitted `ui/util/` and `ShotgunWordmark`, added in the two
  preceding PRs.

Structural: `### Emulator` and `### A real device over Wi-Fi` had ended up under
`## Documentation` rather than `## Development`, because the Documentation
section was inserted between Development and its own subsections. Moved.

Missing, and now referenced: the identity spec `design/Shotgun Logo.dc.html`
(never mentioned, despite the README opening with a line about the mark),
Modernist's own readme in `design/_ds/`, and both CI workflows - the README had
no mention of CI at all.

`docs/github.md` was deleted rather than fixed: it was Claude Design's sync note
and carried almost nothing beyond a repo pointer and a stale screen map.

**Lesson worth keeping:** a line-wrapped path and a fenced code block both
defeat a naive search and replace. After a rename, grep for the old *words*
(`Player`, `PlayerPicker`) rather than the old path.

## 2026-09-06 — Renamed to Shotgun!, new identity

**Outcome: done.**

The app is now **Shotgun!**. The design was re-exported as
`design/Shotgun.dc.html` (the old `Player Picker.dc.html` is removed), a logo
was added in `assets/logo/`, and SOUND is gone from the settings screen.

Code: package and `applicationId` moved from `de.drehtuer.playerpicker` to
`de.drehtuer.shotgun`, `PlayerPickerTheme` -> `ShotgunTheme`,
`PlayerPickerNavHost` -> `ShotgunNavHost`, launcher label "Shotgun!".

The `PP*` prefix on the theme types was **kept deliberately**: it mirrors the
`--pp-*` token names the design still uses, so a colour can be traced between
design and code by name. Renaming it would have broken that link for no gain.

Home's accent eyebrow is replaced by the mark plus wordmark, so the `kicker`
type role became `wordmark`. The mark and the launcher icon are both generated
from the same normalised dot geometry in the logo spec, so they cannot drift.

**The re-export had been branched from the original design, not from the edited
copy in this repo, so it silently reverted decisions made on 2026-09-05:**
haptics went back to a single buzz on fire, and the HAPTICS/DIM MODE copy went
back to the old text. Those decisions still stand, so they were re-applied to
the new export and re-verified. This is the second time hand-edits have been at
risk; the durable fix is to make behaviour changes in the Claude Design project
itself.

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
