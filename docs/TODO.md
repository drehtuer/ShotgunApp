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
  - [ ] Wire the write into the draw surface once that screen exists
- [x] Persist settings - DataStore. Theme preference now survives process
      death; the remaining toggles are stored but not yet surfaced by the
      settings screen.

## Screens

- [ ] **Home** — mode cards with their four-dot motifs, the team stepper
      (floor 2, no ceiling), and the footer link.
- [ ] **Draw surface** — the core of the app, and the hardest part:
  - [ ] one ring per pointer, tracked by pointer id
  - [ ] countdown arming on the 2nd finger, **+1 s per further finger**
  - [ ] edge glow and frame driven by countdown progress
  - [ ] drag to reposition without counting as a new player
  - [ ] double-tap to lift a player
  - [ ] the "N fingers can't fill M teams" guard
  - [ ] suspense churn, then reveal per mode
  - [ ] haptics: a `12 ms` tick per finger down (not on drag, not on lift),
        then `[90]` / `[90, 60, 90]` on the result
  - [ ] write the draw to the history database
  - [x] keep the screen awake while the draw surface is open
- [ ] **Result** — the fairness field on a Canvas (density kernel, edge
      mirroring, ramp interpolation), fed from the history database, with the
      last draw plotted over it.
- [ ] **Settings** — the two toggles, the countdown stepper and the reveal
      timing picker. Appearance is already wired.
  - [ ] DIM MODE — lower `WindowManager.LayoutParams.screenBrightness` for the
        app's own window only, restoring it on leave. Must not touch the
        system-wide setting, and must not darken the palette.

## Testing

- [ ] Unit-test the draw logic once it exists: shuffle fairness, team
      assignment, countdown extension arithmetic, heatmap density, and position
      normalisation.
- [ ] Instrumented tests for navigation and the Compose UI.
- [ ] Decide whether CI should run instrumented tests on an emulator - it is
      slow, and it still cannot cover multi-touch.

## Infrastructure

- [x] Branch protection on `main` - a ruleset now requires a PR.
- [ ] Add the required status checks to the `main` ruleset. Protection requires
      a PR but does not yet require the PR checks to pass.
- [ ] Consider enabling the **merge queue** on the `main` ruleset - stacked PRs
      are currently chained by hand.
- [x] Signing keys configured as repository secrets - both dev and release.
- [ ] **Back the release keystore up off this machine.** It is gitignored, and
      a GitHub secret is write-only, so the local file is the only readable
      copy. Losing it means no future build can ever update an installed app.
- [ ] Cut a first release once there is something to release - `v*` tag - which
      is also the first real exercise of the release and docs workflows.
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
