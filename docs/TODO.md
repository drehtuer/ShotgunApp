# TODO

Open work, roughly in the order it makes sense to do it. Move an item to
[`STATUS.md`](STATUS.md) when it lands, with what actually happened.

Behaviour for everything below is specified in [`design.md`](design.md) and,
authoritatively, in the design export itself.

## Decisions needed

These block implementation and are not mine to make.

- [ ] **What does SOUND do?** The toggle is in the design with the copy *"Off by
      default — most tables are loud"*, but no sound is defined anywhere in the
      export. Needs a decision: a click on reveal? on each finger down? drop the
      toggle?
- [ ] **What does DIM MODE do?** Described as *"Caps brightness for dark rooms"*.
      Needs a target: reduce window brightness, or tone down the palette?
- [ ] **Should results be persisted?** The fairness heatmap currently plots 320
      seeded sample points, not real history. Real logged winners would make the
      screen mean what it claims.

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
  - [ ] haptics on fire (40 ms starter, `[20,40,20]` otherwise)
- [ ] **Result** — the fairness field on a Canvas (density kernel, edge
      mirroring, ramp interpolation) with the last draw plotted over it.
- [ ] **Settings** — the three toggles, the countdown stepper and the reveal
      timing picker. Appearance is already wired.

## Persistence

- [ ] Persist settings. Theme preference is currently hoisted state in
      `MainActivity` and resets on process death; the rest are not stored at
      all. DataStore is the obvious fit.

## Testing

- [ ] Unit-test the draw logic once it exists: shuffle fairness, team
      assignment, countdown extension arithmetic, heatmap density.
- [ ] Instrumented tests for navigation and the Compose UI.
- [ ] Decide whether CI should run instrumented tests on an emulator - it is
      slow, and it still cannot cover multi-touch.

## Infrastructure

- [ ] **Enable GitHub Pages** (Settings → Pages → Source: GitHub Actions), or
      the docs workflow fails at the deploy step.
- [ ] Consider branch protection on `main` requiring the PR checks.
- [ ] Cut a first release once there is something to release, which is also the
      first real exercise of the docs workflow.
- [ ] `docs/github.md` is Claude Design's repo-sync note and was moved from the
      root. If that sync expects it at the root, it needs repointing.

## Polish

- [ ] App icon is a placeholder built from the ring motif.
- [ ] No app-level tests for accessibility: the design leans on colour and
      scale, and the draw surface has no text alternative.
- [ ] Decide what happens on very large finger counts - the design says the
      screen is the limit, which is not a limit.
