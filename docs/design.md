# Design

The specification for the app, derived from
[`design/Shotgun.dc.html`](https://github.com/drehtuer/ShotgunApp/blob/main/design/Shotgun.dc.html), the Claude
Design export.

**This document is the specification of record.** The export remains the
authority on *visuals* - layout, tokens, copy, and the interaction model in its
embedded `<script type="text/x-dc">` block - and it is still the thing to read
for how a screen should look. But some behaviour has since been decided that a
browser prototype cannot express, and it is specified here:

| Behaviour | Where it lives |
| --- | --- |
| Layout, tokens, copy, state machine, reveal | the export |
| Haptic patterns | the export, and *Haptics* below |
| Dim mode | here - the prototype has the toggle but no behaviour |
| Keeping the screen awake | here - a browser prototype cannot express it |
| Recording draws to a database | here - the prototype uses a fixed sample set |

If the export and this file disagree on **visuals**, the export wins and this
file is what gets fixed. Where this file specifies behaviour the export does
not implement, that is deliberate and marked as such.

## The app in one paragraph

Everyone puts a finger on the screen. Once at least two fingers are down a
countdown runs, shown as a glow around the screen edge. When it expires the app
draws: a starting player, a full running order, or teams. The result lands on
the fingers themselves - no list, no names, nothing typed in.

## Identity

The app is **Shotgun!** — the exclamation mark is part of the name and is never
dropped. Specified in [`design/Shotgun Logo.dc.html`](https://github.com/drehtuer/ShotgunApp/blob/main/design/Shotgun%20Logo.dc.html),
with rendered assets in [`assets/logo/`](https://github.com/drehtuer/ShotgunApp/tree/main/assets/logo).

**The mark is the game itself:** four fingers on the glass, one of them called.
One filled dot in accent, three outlined ones that missed, placed where fingers
actually land — not a grid, and deliberately not a firearm.

| | |
| --- | --- |
| Wordmark | Archivo 800, `-0.03em`, all caps, `!` in accent |
| Icon — dark | `#201E1D` plate, `#EC3013` claimed dot, `#605D5D` rings |
| Icon — red | `#EC3013` plate, `#F3F2F2` claimed dot, `#7C1405` rings |
| Tagline | FINGERS DECIDE |
| Don't | No rounded icon corners, no firearm imagery, no third colour, no drop shadow |

Dot geometry, normalised to the plate — centre, relative size, and which one is
claimed:

| | x | y | size | |
| --- | --- | --- | --- | --- |
| 1 | 0.19 | 0.44 | 1.00 | **claimed** |
| 2 | 0.44 | 0.19 | 0.90 | missed |
| 3 | 0.79 | 0.33 | 0.84 | missed |
| 4 | 0.56 | 0.74 | 0.95 | missed |

Dot diameter is `0.25 × plate × size`; ring stroke is `0.0365 × plate`, drawn
*inside* the diameter. The launcher icon and the in-app mark are both generated
from exactly these numbers, so they cannot drift apart.

## Design system

Built on **Modernist**: flat, architectural, set entirely in **Archivo**. Zero
corner radius, strong 2px rules, nothing floats and nothing is decorated -
alignment and the strength of the dividers do the organising.

The app layers its own `--pp-*` palette on top.

### Colour tokens

| Token | Dark | Light | Role |
| --- | --- | --- | --- |
| `bg` | `#201E1D` | `#F3F2F2` | Page ground |
| `surface` | `#2D2B2B` | `#EAE9E9` | Mode cards, settings rows, the fairness field |
| `surface2` | `#444141` | `#D7D3D3` | Pressed / hovered surface |
| `ink` | `#F3F2F2` | `#201E1D` | Primary text |
| `dim` | `#9B9797` | `#7D7979` | Secondary text, inactive marks |
| `line` | `#605D5D` | `#BAB6B6` | The 2px rules |
| `accent` | `#FF563C` | `#EC3013` | The single accent |
| `accentInk` | `#201E1D` | `#F3F2F2` | Text on an accent fill - always the ground |
| `accentSoft` | accent @ 14% | accent @ 12% | Fill behind a winning ring |

The palettes are inversions of each other: dark `ink` is light `bg`, and back.

### Team ring fills

Indexed by `team % 6`. Each pairs a fill with a label colour that stays legible
on it.

| # | Fill | Label |
| --- | --- | --- |
| 0 | `accent` | `accentInk` |
| 1 | `ink` | `bg` |
| 2 | `dim` | `bg` |
| 3 | `#AE1800` | `#F3F2F2` |
| 4 | `#FFC4B8` | `#201E1D` |
| 5 | `surface2` | `ink` |

### Heatmap ramp

Cold to hot, interpolated in RGB.

| Stop | Dark | Light |
| --- | --- | --- |
| 0.00 | `#2D2B2B` | `#EAE9E9` |
| 0.30 | `#4D170E` | `#FFC4B8` |
| 0.58 | `#AE1800` | `#FF563C` |
| 0.82 | `#FF563C` | `#DD2B0F` |
| 1.00 | `#FFC4B8` | `#7C1405` |

### Type roles

All Archivo. The design has its own vocabulary rather than Material's slots.

| Role | Size | Weight | Tracking | Used for |
| --- | --- | --- | --- | --- |
| `wordmark` | 20 | 800 | `-.02em` | The "SHOTGUN!" wordmark on Home |
| `display` | 30 | 800 | `-.01em` | Home headline |
| `screenTitle` | 27 | 800 | `-.01em` | RESULT, SETTINGS |
| `cardTitle` | 25 | 800 | `-.01em` | Mode card titles |
| `hintTitle` | 29 | 800 | `-.01em` | Draw surface hint |
| `sectionTitle` | 19 | 800 | — | FAIRNESS |
| `settingTitle` | 17 | 700 | — | Settings row titles |
| `body` | 15 | 400 | — | Supporting copy |
| `bodySmall` | 14 | 400 | — | Denser supporting copy |
| `micro` | 13 | 700 | `.14em` | "← MODES", "CLOSE", "DONE" |
| `microWide` | 12 | 700 | `.18em` | "TEAMS", "SECONDS" |

## Screens

Four screens. Mode lives on **Home**, deliberately, so the draw surface stays
bare.

### Home

```
┌────────────────────────────────┬─────┐
│ ●∘∘∘ SHOTGUN!          (wordmark)│  S  │
│ PICK A MODE.                     │  E  │  ← vertical tab,
│ HANDS ON GLASS.        (display) │  T  │    opens Settings
├──────────────────────────────────┴─────┤
│ ● ○ ○ ○                                │
│ STARTING PLAYER                        │  flex 1
│ One finger wins the draw               │
├────────────────────────────────────────┤
│ ① ② ③ ④                                │
│ PLAYER ORDER                           │  flex 1
│ Every finger gets a number             │
├────────────────────────────────────────┤
│ Ⓐ Ⓑ Ⓐ Ⓒ                                │
│ TEAMS                                  │  flex 1.2
│ Uneven sizes allowed                   │
│ ┌──────────────┬────┬────┬────┐        │
│ │ TEAMS        │ −  │  3 │ +  │        │
├─┴──────────────┴────┴────┴────┴────────┤
│ LAST RESULT & FAIRNESS HEATMAP →       │
└────────────────────────────────────────┘
```

The header carries the mark and the SHOTGUN! wordmark. Each mode card carries a
four-dot motif previewing what that mode does. The team stepper has a **floor of
2 and no ceiling** - "the screen is the limit".

### Draw surface

Full-bleed and deliberately bare. The top chrome (`← MODES`, mode label) fades
to `opacity: 0` the moment the first finger lands.

Layers, back to front:

1. **Glow** - inset shadow in accent, opacity driven by countdown progress
2. **Frame** - 2px accent border, opacity driven by countdown progress
3. **Chrome** - back link and mode label, hidden while fingers are down
4. **Rings** - one per finger, 112px default
5. **Hint** - shown only when fewer than two fingers are down and idle
6. **Error banner** - accent bar along the bottom
7. **Reveal bar** - "LIFT ALL FINGERS TO CLEAR" + `DETAILS`, once revealed

Copy for the hint:

| Fingers | Title | Subtitle |
| --- | --- | --- |
| 0 | EVERYONE, ONE FINGER DOWN. | The edge glow is the countdown, and it restarts whenever someone joins or leaves. Drag to reposition; a moving finger is not a new player. |
| 1 | ONE MORE FINGER. | At least two players are needed to draw. |

### Result

```
┌──────────────────────────┬─────────────┐
│ RESULT                   │       CLOSE │
├──────────────────────────┼─────────────┤
│ Starting player          │  5 PLAYERS  │   mode + summary
├──────────────────────────┼─────────────┤
│ FAIRNESS                 │   N WINNERS │
├──────────────────────────┴─────────────┤
│                                        │
│      the fairness field (canvas),      │
│      last draw's dots plotted over     │
│                                        │
│  COLD ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ HOT   │
├────────────────────────────────────────┤
│ caption                                │
└────────────────────────────────────────┘
```

The point of this screen is trust: it shows that **where you put your finger
changes nothing**. With no draws yet it reads "NO DRAWS YET / Run a draw — it
lands on top of this field."

### Settings

| Setting | Type | Default |
| --- | --- | --- |
| HAPTICS — *A tick per finger, a stronger buzz on the result* | toggle | on |
| DIM MODE — *Lowers screen brightness, like an alarm clock* | toggle | on |
| APPEARANCE — *Follows the system theme unless you pick one* | System / Light / Dark | System |
| COUNTDOWN — *How long hands must settle before the draw runs* | stepper, 0.5s steps, min 0.5s | 3.5s |
| REVEAL — *Show order and teams at once, or after a beat* | Suspense / Instant | **Instant** |

Footer note: *"Mode lives on the home screen so the draw surface stays bare.
Moving a finger never counts as a new player. Team count has no ceiling — the
screen is the limit."*

### How many fingers

**There is no cap on players, and there is deliberately no arbitrary one.** The
limit is the touchscreen: Android reports a device-dependent maximum number of
simultaneous pointers - commonly ten - and beyond that a finger produces no
pointer for the app to see at all. A cap in the app would only be a second,
lower limit that had to be explained.

Ten is also the practical ceiling for the thing itself: it is two hands, and the
draw exists to settle an argument around one phone. Tests pin the behaviour
there - ten fingers each get a distinct rank, ten across three teams are dealt
round robin with sizes differing by at most one, and lifting one of ten leaves
the other nine undisturbed.

The teams guard still applies: fewer fingers than teams is refused, whatever the
numbers are.

#### The hardware sets the real limit

**The ceiling is the digitizer, and it varies by device.** A panel tracks a
fixed number of simultaneous contacts; past that, a finger generates no pointer
at all, so neither the app nor Android ever learns it was there. It is not a
number the app can raise.

The **Pixel 10a** - the target device - is the worked example. Its touchscreen
reports, over `adb shell getevent -pl`:

```
name: "focal_ts"
ABS_MT_SLOT : value 0, min 0, max 9
```

and Android's own input stack agrees, in `adb shell dumpsys input`:

```
Touch Input Mapper (mode - DIRECT):
    Slot: min=0, max=9
```

**`max 9` is the highest slot *index*, and slots are zero-based - so that is ten
simultaneous contacts, not nine.** The off-by-one is easy to misread, and it is
the reason to write the number down here rather than re-derive it.

Verified on the phone: a ten-finger PLAYER ORDER draw recorded ten `draw_points`
rows with ranks 1-10 and exactly one winner, with nothing in the crash buffer.

Other devices will differ - five and ten are both common - so treat ten as this
phone's number, not a constant.

#### Why this is not shown in the app

**The app cannot read it, so it does not claim it.** Both `/proc/bus/input/devices`
and `/dev/input/event*` are `Permission denied` under the app's uid; the numbers
above come from `adb`, which is not available at runtime. The only public API is
the `PackageManager` feature tier, and its top tier -
`android.hardware.touchscreen.multitouch.jazzhand` - means only *"5 or more
points tracked distinctly"*. That is a floor, not a maximum: on the Pixel 10a it
would report five for a panel that does ten.

So there is deliberately no finger-count row in [Settings](#settings). Showing
the tier would understate the hardware, and showing a running high-water mark
would dress an observation up as a hardware fact. An app whose fairness field
exists to show its work should not guess here.

## State model

```
screen      home | draw | details | settings
mode        starter | order | teams
phase       idle → counting → (suspense) → revealed
fingers     [{ id, x, y }]
result      { mode, teamCount, assign, winnerId, fingers, w, h } | null
settings    { haptics, dim, countdown, timing, themePref }
```

### Phases

| Phase | Entered when | Leaves when |
| --- | --- | --- |
| `idle` | start, or all fingers lifted | a 2nd finger lands |
| `counting` | ≥2 fingers down | countdown expires, or count drops below 2 |
| `suspense` | draw fires, if not instant | the last player has been revealed |
| `revealed` | draw fires (instant) or suspense ends | all fingers lifted |

## Behaviour

### The countdown

**Divergence from the export.** The design armed a fixed countdown on the second
finger and added a second for each finger after it. In use that was hard to
predict - three latecomers trebled the wait - and it existed only because the
countdown itself was not adjustable. Now that it is, the countdown is a
**settling time** instead:

- Arms when the **second** finger lands, for the configured time
  (default **3.5 s**, adjustable in half-second steps from 0.5 s). The default
  was set on the phone: shorter rushed a group still reaching in.
- **Any change in the number of fingers restarts it in full** - someone joining
  or leaving. Nobody is caught by a draw firing as they reach in.
- **Moving a finger does not restart it.** Repositioning is not a change in the
  count.
- Dropping below two fingers cancels it and returns to `idle`.
- Progress drives two things at once: the edge glow (`0.1 → 0.5` opacity) and
  the frame (`0.2 → 1.0`).

### Touch rules

| Gesture | Effect |
| --- | --- |
| Press on empty surface | Adds a player |
| Drag an existing ring | **Repositions it — never counts as a new player** |
| Lift a finger | Removes that player |
| Press during suspense/revealed | Ignored |

**Divergence from the export.** The prototype was driven by a mouse, which
cannot lift, so it removed a player on double-tap and kept rings on screen after
the pointer went up. On a touchscreen a ring belongs to a finger: it exists
while that finger is down and goes when it lifts. That is what "LIFT ALL FINGERS
TO CLEAR" already assumes, so double-tap has no purpose and is not implemented.

Every press that adds a player fires a short haptic tick — see *Haptics*.

That second rule is the one users notice: a hand that shifts on the glass must
not silently join the draw twice.

### The draw

1. Guard: in `teams` mode, fewer fingers than teams flashes
   *"N fingers can't fill M teams"* for 2.4 s and returns to `idle`.
2. Fisher-Yates shuffle of the finger ids.
3. Assign — `teams`: `index % teamCount`; otherwise rank `index + 1`.
4. Winner is the first of the shuffled order.
5. Haptics, if enabled: a **stronger** buzz than the per-finger tick —
   `[90]` for starter, `[90, 60, 90]` otherwise (see *Haptics*).
6. Reveal immediately if timing is `instant` **or** mode is `starter`;
   otherwise reveal **one finger at a time**, 500 ms apart, along the draw
   order. That order is the rank order, and because teams are dealt round
   robin it steps between teams on every reveal.

Starter mode always reveals instantly - there is nothing to stagger.

### Reveal, per mode

| | Winner / first | Others |
| --- | --- | --- |
| **starter** | accent ring, `accentSoft` fill, scale 1.14, bloom animation | `line` ring, opacity 0.2, scale 0.88 |
| **order** | rank number at 46px, scale 1.12, `accentSoft` fill | rank at 32px, opacity fading `1 → 0.3` by rank |
| **teams** | — | team letter on the team fill, "TEAM" beneath |

During `suspense` every ring churns (`scale .94 ↔ 1.06`) in accent at 85%
opacity — the tell that something is being decided. **Specified but not yet
built**: the staged reveal happens, the churn between steps does not. Tracked
in [`TODO.md`](TODO.md).

## Keeping the screen awake

**The screen must not dim, sleep or lock while the draw surface is open.**

This is not covered by Android's normal behaviour. The idle timer is reset by
touch *events*, and players hold their fingers still through the countdown,
which produces none - so a draw can be interrupted by the screen dimming or the
lock screen appearing at the worst possible moment.

- Implemented with `FLAG_KEEP_SCREEN_ON`, held for as long as the draw surface
  is shown and released when leaving it.
- Scoped to that screen, not the whole app: Home, Result and Settings should
  time out normally.
- It holds for the *whole* draw screen, not only while fingers are down. A
  group gathering around the phone before the first finger lands should not
  have the screen go dark on them either.
- No wake lock permission is needed - the flag is a window attribute, and the
  system releases it if the app leaves the foreground.

## Haptics

Two distinct strengths, so the phone tells you what happened without looking:

| Event | Pattern | Feel |
| --- | --- | --- |
| A finger lands | `12 ms` | A keyboard-style tick, once per player added |
| Result revealed — starter | `[90]` | One firm buzz |
| Result revealed — order / teams | `[90, 60, 90]` | A heavier double buzz |

The tick fires on **finger down only** — never on drag, and never on lift.
Dragging is repositioning, not joining, and buzzing on it would contradict
that. Both are suppressed entirely when HAPTICS is off.

On Android this is `VibratorManager` / `VibrationEffect`; the app already
declares `android.permission.VIBRATE`.

## Dim mode

**Lowers the screen brightness, the way an alarm clock does.** The app is used
on a table in a dark room and a phone at full brightness is unpleasant there.

- Applies to the app's own window only, by lowering
  `WindowManager.LayoutParams.screenBrightness` — it must not change the
  system-wide setting, and the previous value must return on leaving the app.
- **Halves whatever the screen is currently at**, with a floor just above off.
  It is deliberately *relative*: a fixed target was tried first and made a dim
  screen brighter, which is the one thing the setting must never do. The
  current level is read from `Settings.System.SCREEN_BRIGHTNESS`, because a
  window that has never overridden brightness reports `BRIGHTNESS_OVERRIDE_NONE`
  rather than a value.
- On by default.
- It is a *brightness* change, not a palette change: the theme is chosen
  separately under APPEARANCE, and dim must not silently darken the colours.

## The fairness field

A density heatmap of **real recorded winner positions**, drawn on a canvas.
Every draw appends its finger positions to a local database, and this screen
plots the accumulated history.

The point of the screen is trust, and it only earns that if the data is real:
a seeded sample set would be a picture of fairness rather than evidence of it.

### What gets recorded

Every completed draw records, for each finger:

- its position, **normalised to 0..1** against the surface it was captured on,
  so records stay comparable across devices and orientations
- whether it won (starter) or its assigned rank / team
- the mode, and when the draw happened

Normalising at write time is what makes old records still usable after a screen
size change. Storing raw pixels would silently skew the field.

### Rendering

- Density kernel: radius `max(18, min(W,H) × 0.13)`, quartic falloff
  `(1 − d²/r²)²`.
- **Edge mirroring** — points near a border are reflected outward, so corners
  do not read as artificially cold.
- Normalised against the peak, gamma `0.9`, then mapped through the ramp.
- `heatSamples` caps how many records are drawn, newest first, so the field
  stays cheap to render as history grows.

The last draw's positions are plotted on top as dots, re-projected from their
normalised coordinates. Labels flip to the left of the dot past 55% width so
they never run off screen.

Copy adapts to the real count — with a result: *"Your last draw sits on top of
N logged winners. The field stays flat edge to edge — where you put your finger
changes nothing."* With no history at all, the field reads "NO DRAWS YET".

## Tunable parameters

Exposed as editor props in the design:

| Prop | Default | Range |
| --- | --- | --- |
| `ringDiameter` | 112 px | 88–140, step 4 |
| `heatSamples` | 320 | 40–320, step 20 - caps how many records are drawn |

## Implementation notes

- The design is authored for a **428 × 908** frame; the target device is a
  Pixel 10a.
- Result positions are stored with the surface dimensions they were captured
  on (`w`, `h`) so the Result screen can re-project them proportionally.
- Recorded positions are normalised to 0..1 at write time, so history survives
  a device or surface-size change. The export instead keeps raw pixels with the
  captured `w`/`h`, which works for a single session but not for a stored
  history.
- The export's fairness field draws a fixed, seeded sample set. That is
  scaffolding for the visual only - the implementation reads the database.
- A SOUND toggle was in the original design and has been removed: it is not
  needed.
