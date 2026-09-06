# Status

What has been done and how it turned out. Newest first. Entries keep the parts
that went wrong - a bug someone hit and fixed is worth more to the next person
than a clean summary that hides it.

See [`TODO.md`](TODO.md) for what is still open.

## Current state

**Built, verified on the phone, and released.** All four screens work, the draw
history and settings persist, and seven rounds of device feedback settled the
timing, the reveal, the dim level and the haptics. `v0.1.0` is published and the
documentation site is live. What is left is a fourth draw mode and polish.

| Area | State |
| --- | --- |
| Gradle project, Compose setup, Modernist theme | done |
| Navigation across all four screens | done |
| Home, Draw surface, Result, Settings | done, verified on the phone |
| Draw history database, settings persistence | done |
| Identity: name, logo, launcher icon | done |
| Devcontainer: SDK, emulator, adb over Wi-Fi | done |
| CI: build, unit tests, lint | done |
| Release: signed artifacts on a `v*` tag | `v0.1.0` published |
| Documentation site | live at [drehtuer.github.io/ShotgunApp](https://drehtuer.github.io/ShotgunApp/) |
| COLOURS draw mode | not started - blocked on one decision |

---

## 2026-09-06 — First release, and the documentation site

**Outcome: `v0.1.0` published and the site live - after both workflows turned
out to be broken in ways only building their output could show.**

Neither had ever run. Building the site locally, with the same container image
CI uses, found what reading them had not.

**Every link on the site was broken.** All 25 cross-document links pointed at
`.md` files that do not exist on the site; a `sed` pass rewrote a few prefixes
and left the rest. Now GitHub's own Jekyll against a root `_config.yml`, which
fixes it at the source - `jekyll-relative-links` resolves them as a matter of
course. Links to files Jekyll does not publish are absolute GitHub URLs, so they
work in the repository *and* on the site.

**The Mermaid diagrams would have been served as source code.** GitHub renders
Mermaid in its *repository* Markdown view - which is where they had been checked
- but **Pages does not**. All five would have appeared as walls of `graph TD`
text. The layout now loads Mermaid and unwraps Kramdown's markup, with blocks
hidden until it has run.

**A release would have been published unsigned, permanently.** `release.yml`
treated a missing key as normal and published unsigned artifacts with a warning
- written back when the key was deliberately not on GitHub. Since a published
release is immutable, that would have burned the version number for good. A
missing key now fails the run, `apksigner verify` confirms the signature on the
artifact rather than inferring it from the secret existing, and artifacts are
attached to a **draft** which is published only once the upload succeeds.

**The docs workflow would never have fired.** It triggered on
`release: published`, and `release.yml` publishes the release itself using the
default `GITHUB_TOKEN` - and **an event raised by that token does not start
another workflow run**. The site would silently never have published. Both
workflows now watch the `v*` tag, which a person pushes.

### The one thing local testing hid

The release worked first time: signed APK and AAB, `apksigner` confirming
`CN=Shotgun!, OU=Release, O=drehtuer`. The docs run failed on
`mkdir: cannot create directory '_site/api': Permission denied`.
`jekyll-build-pages` is a **container action running as root**, so `_site`
belongs to root while the job does not.

Local testing had hidden this precisely: the container had been run with
`--user "$(id -u)"` to keep the output writable, which made the local run
*differ from CI in the one way that mattered*. **A convenience added to avoid
cleaning up root-owned files removed the failure being tested for.** The site is
now assembled in a fresh directory - copying *out* of `_site` needs only read -
and the fix was verified by reproducing the failure first.

### Named release artifacts

`app-release.apk` says neither what it is nor which version. The builds now
produce `Shotgun-<version>.apk` and `Shotgun-debug-<version>.apk`, and the
release workflow renames the bundle to match. `outputFileName` is not on the
public `VariantOutput` interface, so this goes through `VariantOutputImpl`; the
cast is checked, not forced.

That introduced a new way to publish something unfixable - the name comes from
`appVersion` in the build file, not the tag, so a `v0.2.0` tag on an unchanged
`appVersion` would put `Shotgun-0.1.0.apk` inside a `v0.2.0` release. The
workflow now refuses to run when the two disagree. **`v0.1.0` keeps the old
names**; it is immutable.

### Documentation that had drifted

Found by checking claims against the code rather than reading for sense:
`STATUS.md` still opened with "skeleton + theme … the four screens are stubs";
`TODO.md` carried the removed "+1 s per further finger" countdown and a dim
level that had been wrong twice; `design.md` gave the old 2 s countdown default
and a 600 ms suspense; `build.gradle.kts` and `release.yml` both still said the
release key was not on GitHub.

## 2026-09-06 — Device testing, and the blank screen

**Outcome: a run of real bugs, all found by hands on glass rather than by
tests.** Installed on the target Pixel 10a over Wi-Fi.

The blank screen is worth recording in full, because three plausible
explanations were wrong before the right one.

Reported as: pick a mode, press back immediately, and the app draws nothing
until it is restarted. It could not be reproduced synthetically - six back-press
timings, rapid double taps, tap-plus-two-backs and the edge-swipe gesture all
behaved. Two suspects were ruled out with evidence rather than argument: the
glow uses `BlurMaskFilter`, historically unsupported under hardware
acceleration, but forcing a countdown produced no render errors; and the
brightness override was measured, not guessed.

What settled it was catching the app *in* the state. `dumpsys` showed the
activity resumed with no crash, and an accessibility dump showed **three nodes**
- bare window chrome, no Compose content. The navigation graph had lost its
destination.

Reproduced in a test: **popping more often than the stack is deep empties the
graph**, and an empty graph renders nothing. Two pops racing each other is
enough - a button tapped twice, or a tap arriving with the back gesture. Every
exit now goes through `popSafely()`, and back handling lives in one place.

**Lesson: an unreproducible bug is usually a wrong model of the failure, not a
rare one.** Catching the app in the broken state and dumping what it actually
contained took minutes; guessing at causes took much longer.

Also fixed from device testing: labels drawn under the very fingers they
belonged to; the countdown appearing to ignore its setting, which was the
stepper losing taps to an asynchronous read; both timers restarted forever by
the jitter of a resting hand; and dim mode, set absolutely, making a dim screen
*brighter*.

## 2026-09-06 — The four screens

**Outcome: done, across five PRs.** Each screen's logic was pushed out of the
composables into plain functions, so the parts that can be tested without
hardware are.

**Settings and dim mode.** Dim lowers `screenBrightness` on the app's own window
and restores it on the way out - deliberately not the system-wide setting, which
would leave a phone dimmed after the app closed. Applied in `MainActivity`,
because brightness is an app-wide property, not a property of the screen that
happens to toggle it. Verified by changing three settings, force-stopping and
reopening.

**Result and the fairness field.** The density maths lives in a plain
`HeatField` with no Android types, so it is unit-tested. That mattered more than
usual: the screen makes a claim about fairness, and "it looks about right" is
not a check of a claim. The test worth keeping is **a corner win registering as
hot as a centre win** - without edge mirroring, corners read as permanently
cold and the screen would libel the draw as unfair. Verified by seeding 241
draws into the real on-device database and looking at it.

Two things that cost time: `gradle connectedAndroidTest` **uninstalls the app
afterwards**, deleting its database, so seeding through it shows an empty screen
- `adb shell am instrument` leaves the data in place; and `--tests` is not a
valid filter there, it is
`-Pandroid.testInstrumentationRunnerArguments.class=`.

**Draw surface.** The rules live in a plain `DrawEngine` with no Android in it -
time passed in, randomness injected - so the countdown, the teams guard, the
reveal rules and who wins are unit-tested. The one thing that cannot be tested
without hardware is multi-touch, so everything else was made testable without
it. **Multi-touch turned out partly testable anyway**: the emulator cannot
inject a genuine multi-pointer gesture, but Compose's own pointer injection can.

**Home.** The team count lives in the ViewModel rather than in settings,
because the design keeps it beside the draw. Two bugs only visible on a device:
the vertical SETTINGS tab wrapped mid-word - "SETTIN / GS", since Compose has no
`writing-mode` and the first fix measured the rotated label against the tab's
width; and the stepper's minus stayed at full contrast at the floor, looking
live while doing nothing.

**Signing, build variants and persistence.** `debug` carries full debug data,
`release` is R8-minified and symbol-stripped - 1.2 MB against 12 MB. Two
deliberate omissions in `debug`, both easy to add by reflex and both wrong here:
no `applicationIdSuffix` (it would break every documented adb command) and no
`enableAndroidTestCoverage` (it instruments the APK, and this app is judged on
touch and countdown timing).

The integration risk was KSP: it versions independently and AGP 9 supplies its
own Kotlin, so the two could easily have disagreed. KSP 2.3.11 against Kotlin
2.4.10 under AGP 9 was checked before anything was built on top of it.

Positions are normalised to 0..1 **at write time**. That is the one thing in
this layer that can corrupt data silently - raw pixels would skew the fairness
field, and the damage stays invisible until a screen size changes.
`fallbackToDestructiveMigration` is deliberately **not** set: history is the
whole point of the fairness field, and wiping it on a schema change would make
that screen lie.

## 2026-09-06 — Rename to Shotgun!, and a documentation audit

**Outcome: done.**

The app became **Shotgun!**: re-exported design, a logo in `assets/logo/`,
package and `applicationId` moved to `de.drehtuer.shotgun`. The `PP*` prefix on
the theme types was **kept deliberately** - it mirrors the `--pp-*` token names
the design still uses, so a colour can be traced between design and code by
name.

**The re-export had been branched from the original design rather than the
edited copy here, so it silently reverted decisions made the day before**:
haptics went back to a single buzz, and the toggle copy to its old text. Those
decisions still stand, so they were re-applied. Second time hand-edits have been
at risk - see `TODO.md`.

The audit that followed found more wrong than missing. `.claude/CLAUDE.md`
opened by pointing at `design/Player Picker.dc.html`, **deleted in the rename**;
it survived because the path was line-wrapped across two lines, so the search
and replace never matched. `README.md`'s layout tree still named
`PlayerPickerTheme` and `PlayerPickerNavHost`, for the same class of reason -
inside a fenced code block.

**Lesson: a line-wrapped path and a fenced code block both defeat a naive search
and replace.** After a rename, grep for the old *words*, not the old path.

## 2026-09-05 — Keep the screen awake during a draw

**Outcome: done and verified on device.**

The screen could dim or lock mid-draw. Android resets its idle timer on touch
*events*, and a finger held still through the countdown produces none - so the
one moment the screen must stay on is exactly the moment the system counts as
idle. Fixed with `FLAG_KEEP_SCREEN_ON`, scoped to the draw surface so it cannot
leak into the other screens.

Verified with `dumpsys window` across the whole navigation graph. The first
attempt reported a **false negative**: `grep -A3` after the window line stopped
short of the `fl=` line it needed, about five lines in.

## 2026-09-05 — Specification decisions

**Outcome: done. All three open questions answered; no code changed yet.**

SOUND removed as not needed. DIM MODE defined as lowering screen brightness like
an alarm clock, explicitly *not* a palette change. Haptics redefined from a
single buzz to a `12 ms` tick per finger down plus a stronger result buzz - the
tick deliberately does **not** fire on drag or lift, because dragging is
repositioning, not joining, and buzzing on it would contradict the rule the hint
text teaches. Draw history moved to a database so the heatmap plots real draws.

Dim mode and the database cannot be expressed in a browser prototype, which
forced a question about which document wins. Resolved by splitting authority:
**the export owns visuals, `docs/design.md` owns behaviour.**

## 2026-09-05 — Toolchain, documentation set and CI

**Outcome: done, and retargeting forced a toolchain jump.**

The target device was confirmed as a **Pixel 10a on Android 17 (API 37)**.
Raising `compileSdk` to 36 failed immediately - current AndroidX requires
compileSdk 37 and AGP 9.1+ - so the whole toolchain moved: AGP 8.7.3 → 9.4.0,
Gradle 8.11.1 → 9.7.1, Kotlin 2.0.21 → 2.4.10, compileSdk/targetSdk 35 → 37.

**AGP 9 has built-in Kotlin support: the `kotlin-android` plugin now fails the
build outright and had to be removed.** Recorded in several places because it is
easy to re-add by reflex.

The emulator has no `pixel_10a` profile; `pixel_9a` on an API 37 image is the
closest match, and reported `ro.build.version.release=17`.

Also added: `docs/design.md` as a translation of the export - not a replacement,
since the `.dc.html` holds the executable state machine - `build-environment.md`,
this file, `TODO.md`, the first 11 unit tests, and the two workflows. Neither
workflow could run on its own PR, since workflows must exist on `main` first.

Translating the design surfaced two gaps: **`sound` and `dim` appeared in the
settings with no behaviour defined anywhere.** Both were answered the same day.

## 2026-09-05 — Android skeleton and Modernist theme

**Outcome: done.**

The Gradle project, the theme transcribed from the design's `--pp-*` tokens, the
navigation shell and stub screens, and the devcontainer.

**Two bugs found only by running it:** system bar icons followed `uiMode` rather
than the app's palette, so the clock was unreadable when the theme was
overridden; and the settings dividers used a hardcoded height instead of the
row's intrinsic height.

Several devcontainer problems also only appeared under use: `/dev/kvm` arrives
owned by a nonexistent group, and AVDs and the debug keystore vanished on
rebuild until they were moved into named volumes.

## 2026-09-05 — Design export imported

**Outcome: done.**

The repository began as an export from a Claude Design project, with no commits
on `main`. `/design-sync` does not apply - this repo is a *consumer* of a design
system, not the source of one.
