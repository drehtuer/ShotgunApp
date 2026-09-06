# TODO

Open work only. When something lands it moves to [`STATUS.md`](STATUS.md),
with what actually happened - so this file stays a list of what is left, not a
record of what was done.

Behaviour for everything below is specified in [`design.md`](design.md) and,
authoritatively, in the design export itself.

## Decisions needed

- **Where does the COLOURS list live** - edited once and persisted, or set up
  per draw? It decides the navigation and whether it is stored at all, so the
  mode cannot be built until it is answered.

## Screens

- [ ] **COLOURS mode** - a fourth draw mode. Like player order, but the colours
      are defined first and each finger is given one in the order they were
      defined. Blocked on the decision above.
  - [ ] A colour-definition screen: a vertical list with a `+` to add
  - [ ] Preset player colours - blue, red, black, green, yellow and so on
  - [ ] A colour picker for anything finer
  - [ ] Rings and result dots take the chosen colours rather than the palette's
        team fills
- [ ] The **suspense churn animation** is not implemented. The staged reveal
      happens; the rings do not pulse between steps.
- [ ] Whether the **rank labels clear the player's own hand** is a heuristic -
      drawn above the ring, flipping below near the top edge. True for the hands
      it has been tried with, not by construction.
- [ ] The **fairness field is recomputed on the main thread** when history
      changes. Fast at the sizes involved, but it belongs off the main thread
      before the history gets large.

## Infrastructure

- [ ] **Back the release keystore up off this machine.** It is gitignored, and a
      GitHub secret is write-only, so the local file is the only readable copy.
      Losing it means no future build can ever update an installed app. This is
      the one item here that cannot be recovered from.
- [ ] **Merge queue.** Reported as available for this repository, but the
      rulesets API still rejects the rule outright - `Invalid rule
      'merge_queue'`, at the rule-type level, with or without parameters - and
      the `main` ruleset carries no merge-queue rule. If the option is visible
      in the UI, ticking it there will settle it; the earlier claim that it is
      organization-only is no longer something this repo can substantiate.
- [ ] **Two secret-scanning options would not enable.**
      `secret_scanning_non_provider_patterns` (generic secrets, which is what a
      keystore password looks like) and `secret_scanning_validity_checks` both
      stay `disabled` after a `PATCH` that the API accepts without error.
      Probably a UI toggle or an Advanced Security requirement. They matter here
      more than most repositories, because a leaked signing key is the one
      unrecoverable failure - see
      [`SECURITY.md`](https://github.com/drehtuer/ShotgunApp/blob/main/SECURITY.md).
- [ ] **Re-exports from Claude Design overwrite behaviour decided here** - it
      has happened twice. Either mirror the haptics and toggle-copy changes
      upstream, or accept the export as visuals-only and stop hand-editing it.
- [ ] Consider `actions/attest-build-provenance` on release artifacts. It pairs
      with immutable releases - the release cannot change, and the attestation
      says which workflow and commit produced it - but it is not needed until
      the app is actually distributed.

## Polish

- [ ] Check the in-app mark optically against
      [`assets/logo/`](https://github.com/drehtuer/ShotgunApp/tree/main/assets/logo)
      on a real screen at small sizes.
- [ ] **No accessibility tests.** The design leans on colour and scale, and the
      draw surface has no text alternative.

## Settled, so they are not open

Recorded here because each was a real question, and the answer is easy to
re-litigate otherwise.

| Question | Answer |
| --- | --- |
| Should CI run the instrumented tests? | **No.** They run locally, against the phone. An emulator cannot test multi-touch, so a green CI run would say nothing about the one thing they exist to check. |
| SOUND | Not needed. Removed from the design and the docs. |
| DIM MODE | Lowers screen brightness like an alarm clock - halves the current level, app window only. |
| Draw history | Recorded to a database, so the fairness field plots real draws. |
| Double-tap to lift a player | Deliberately absent. Lifting a finger removes its ring, so it has no purpose on a touchscreen. |
