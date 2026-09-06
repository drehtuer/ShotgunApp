# TODO

Open work, roughly in the order it makes sense to do it. Move an item to
[`STATUS.md`](STATUS.md) when it lands, with what actually happened.

Behaviour for everything below is specified in [`design.md`](design.md) and,
authoritatively, in the design export itself.

## Decisions needed

None open. The three that were blocking were answered on 2026-09-05 and are now
specified in [`design.md`](design.md):

- **SOUND** — not needed, removed from the design and the docs.
- **DIM MODE** — lowers screen brightness, like an alarm clock.
- **Draw history** — recorded to a database so the heatmap shows real history.

## Persistence and data

- [x] **Draw history database** - Room, schema v1 exported to `app/schemas/`.
  - [x] Schema and DAO, with the schema committed so a migration can be written
        against a real diff
  - [x] `DrawHistory` repository - the draw writes, the Result screen reads
  - [x] Pruning on write, capped at `MAX_RETAINED_DRAWS`
  - [x] Unit-tested normalisation
- [x] Persist settings - DataStore. Theme preference now survives process
      death; the remaining toggles are stored but not yet surfaced by the
      settings screen.

## Screens

- [ ] **COLOURS mode** — a fourth draw mode. Like player order, but the colours
      are defined first and each finger is given one in the order they were
      defined.
  - [ ] A colour-definition screen: a vertical list with a `+` to add
  - [ ] Preset player colours - blue, red, black, green, yellow and so on
  - [ ] A colour picker for anything finer
  - [ ] Decide where the list lives: edited once and persisted, or set up per
        draw. That choice decides the navigation and whether it is stored at all.
  - [ ] The rings and the result dots take the chosen colours rather than the
        palette's team fills

- [x] **Home** — mode cards with their four-dot motifs, the team stepper
      (floor 2, no ceiling), and the footer link.
- [x] **Draw surface** — built.
  - [x] one ring per pointer, tracked by pointer id
  - [x] countdown arming on the 2nd finger, restarting in full whenever the
        finger count changes - a settling time, not an additive one
  - [x] edge glow and frame driven by countdown progress
  - [x] drag to reposition without counting as a new player
  - [x] the "N fingers can't fill M teams" guard
  - [x] reveal per mode
  - [x] haptics: a `12 ms` tick per finger down (not on drag, not on lift),
        then `[90]` / `[90, 60, 90]` on the result
  - [x] write the draw to the history database
  - [x] keep the screen awake while the draw surface is open
  - [x] **Verified on the Pixel 10a** over seven rounds of feedback. Feel,
        timing and haptic strength are judged; the countdown default landed on
        3.5 s as a result.
  - [ ] Whether the rank labels clear the player's actual hand is still a
        heuristic - they are drawn above the ring, flipping below near the top
        edge. It holds for the hands it has been tried with, not by
        construction.
  - [ ] The suspense churn animation is not implemented; the pause happens but
        the rings do not pulse during it.
  - [ ] Lifting a finger removes its ring, so the design's double-tap-to-lift
        has no purpose on a touchscreen and is deliberately absent.
- [x] **Result** — the fairness field, fed from the history database, with the
      last draw plotted over it.
  - [ ] The field is recomputed on the main thread when history changes. It is
        fast at the sizes involved, but belongs off the main thread before the
        history gets large.
- [x] **Settings** — the two toggles, the countdown stepper and the reveal
      timing picker, all persisted.
  - [x] DIM MODE — lowers `WindowManager.LayoutParams.screenBrightness` for the
        app's own window only, restored on leave.
  - [x] The dim level halves the current brightness, floored just above off.
        Two earlier attempts were wrong on the phone: a fixed 0.25 made an
        already-dim screen *brighter*, and capping at the current value made
        the setting do nothing.

## Testing

- [x] Unit-test the draw logic: shuffle fairness, team assignment, countdown
      extension arithmetic and position normalisation. Heatmap density follows
      with the Result screen.
- [x] Instrumented tests for multi-touch and the Room DAO.
- [x] Instrumented tests for navigation - `NavigationStateTest`, which
      reproduced the blank screen before it was fixed and now guards it.
- [ ] Decide whether CI should run instrumented tests on an emulator. They pass
      locally but nothing runs them automatically, so they will rot.

## Infrastructure

- [x] Branch protection on `main` - a ruleset now requires a PR.
- [x] Required status checks on the `main` ruleset - Build, Unit tests and
      Static analysis must pass before a merge into `main`.
- [ ] **Merge queue is not available for this repository.** GitHub offers it
      only for **organization-owned** repositories; this one is owned by a user
      account, so the setting is absent from the UI and the API rejects the
      rule outright (`Invalid rule 'merge_queue'`). To get one, transfer the
      repository to an organisation. Until then, stacked PRs are chained by
      hand: each branches from the one below it, and GitHub retargets them to
      `main` as they merge.
- [x] Signing keys configured as repository secrets - both dev and release. All
      eight are set; nothing further needs adding.
- [x] The documentation site is rendered by GitHub's own Jekyll, from the
      Markdown in place rather than a copy.
- [ ] **Back the release keystore up off this machine.** It is gitignored, and
      a GitHub secret is write-only, so the local file is the only readable
      copy. Losing it means no future build can ever update an installed app.
- [x] Cut a first release - **`v0.1.0`**, signed, with the APK and AAB attached.
      The release workflow worked first time.
- [ ] Publish the documentation site. The first run failed on a permission
      error; fixed, but the site has not been deployed yet.
- [ ] `v0.1.0` carries `app-release.apk` / `app-release.aab`, from before the
      artifacts were named. Releases are immutable, so it keeps them; the next
      release gets `Shotgun-<version>.apk`.
- [ ] Consider `actions/attest-build-provenance` on the release artifacts. It
      pairs with immutable releases - the release cannot change, and the
      attestation says which workflow and commit produced it - but it is not
      needed until something is actually distributed.
- [ ] **Behaviour decisions live in this repo, but re-exports come from Claude
      Design and overwrite them** (it has happened twice). Either mirror the
      haptics and toggle-copy changes upstream, or accept the export as
      visuals-only and stop hand-editing it.

## Polish

- [ ] The in-app mark is drawn from the logo geometry; check it optically
      against `assets/logo/` on a real screen at small sizes.
- [ ] No app-level tests for accessibility: the design leans on colour and
      scale, and the draw surface has no text alternative.
- [ ] Decide what happens on very large finger counts - the design says the
      screen is the limit, which is not a limit.
- [ ] Five informational Lint findings, none failing the build and all
      predating the documentation work: `NewerVersionAvailable`,
      `ModifierParameter` (Wordmark), `AutoboxingStateCreation` (DrawScreen),
      `UnusedResources` (`ic_launcher_round.xml`) and
      `UseOfNonLambdaOffsetOverload` (SettingsScreen). Worth a pass, or a
      narrow suppression each with a reason.
