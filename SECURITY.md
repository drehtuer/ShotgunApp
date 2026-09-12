# Security policy

Shotgun! is an Android app maintained by one person in the open. This document
says what to report, where to send it, and what to expect back.

## Reporting a vulnerability

**Use GitHub's private vulnerability reporting:**
[report a vulnerability](https://github.com/drehtuer/ShotgunApp/security/advisories/new).

That keeps the report private until there is a fix, and gives us one place to
discuss it. **Please do not open a public issue for a vulnerability** - a public
issue is a disclosure, whatever it is titled.

What helps, in rough order of usefulness:

- What an attacker gains. "Reads the draw history of another app" is a finding;
  "uses a deprecated API" is not, on its own.
- The steps to reproduce it, and the Android version and device you saw it on.
- The commit or release it was found in.

You will get an acknowledgement within **7 days**. This is a hobby project, so
that is a genuine estimate rather than a service level - if it matters and you
have heard nothing, say so on the same advisory.

## What is in scope

- The app in [`app/`](app/) - anything it stores, displays or exposes to other
  apps on the device.
- The build and release pipeline in [`.github/workflows/`](.github/workflows/),
  including how signing material is handled.
- Anything published to the [GitHub Pages site](https://drehtuer.github.io/ShotgunApp/).

### Signing material, specifically

**No signing key is ever committed to this repository - it is public.** Both the
dev and the release key are held as encrypted GitHub Actions secrets, restored
during a workflow run and deleted in an `always()` step. Everything local lives
in `keystore/` - the keystores and the `keystore.properties` that points at
them - and `*.keystore`, `*.jks`, `keystore/` and `keystore.properties` are all
gitignored.

**If you find a keystore, a key password or a private key anywhere in this
repository or in a published artifact, that is a vulnerability and we want to
know immediately.** It is the highest-severity thing that can go wrong here:
Android identifies an app by its signature, so a leaked release key lets someone
else ship an "update" to anyone who installed this app.

## What is not in scope

- **The design export** in [`design/`](design/). It is a specification rendered
  as a static HTML prototype, not something that is deployed or executed as part
  of the product.
- **The development container** in [`.devcontainer/`](.devcontainer/). It is a
  local tool for building the app, running on a developer's own machine.
- Findings that require an already-compromised device - root access, a malicious
  app with system privileges, or physical access to an unlocked phone.
- Reports from automated scanners with no demonstrated impact.

## What the app can actually do

Worth stating plainly, because it bounds most of what could go wrong:

| | |
| --- | --- |
| Permissions | `VIBRATE`, and nothing else |
| Network access | **None.** The app holds no `INTERNET` permission. The only link on the settings screen is handed to the browser. |
| Data collected | None. Nothing leaves the device, because nothing can. |
| Data stored | Finger positions from past draws, in a local database, to draw the fairness field. Positions only - no identity, no timestamps beyond ordering. |

## Supported versions

The most recent release is the supported one. Releases are immutable, so a fix
arrives as a new version rather than as a replaced artifact.

| Version | Supported |
| --- | --- |
| Latest release | yes |
| Anything older | no - upgrade |
