# Design

The specification for the app, derived from
[`design/Player Picker.dc.html`](../design/Player%20Picker.dc.html), the Claude
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
| Recording draws to a database | here - the prototype uses a fixed sample set |

If the export and this file disagree on **visuals**, the export wins and this
file is what gets fixed. Where this file specifies behaviour the export does
not implement, that is deliberate and marked as such.

## The app in one paragraph

Everyone puts a finger on the screen. Once at least two fingers are down a
countdown runs, shown as a glow around the screen edge. When it expires the app
draws: a starting player, a full running order, or teams. The result lands on
the fingers themselves - no list, no names, nothing typed in.

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
| `kicker` | 12 | 700 | `.2em` | "PLAYER PICKER" eyebrow, in accent |
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
│ PLAYER PICKER            (kicker)│  S  │
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

Each mode card carries a four-dot motif previewing what that mode does. The
team stepper has a **floor of 2 and no ceiling** - "the screen is the limit".

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
| 0 | EVERYONE, ONE FINGER DOWN. | Hold still — the edge glow is the countdown. Drag to reposition; a moving finger is not a new player. |
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
| COUNTDOWN — *Each new finger adds one second* | stepper, min 1 | 3s |
| REVEAL — *Show order and teams at once, or after a beat* | Suspense / Instant | Suspense |

Footer note: *"Mode lives on the home screen so the draw surface stays bare.
Moving a finger never counts as a new player. Team count has no ceiling — the
screen is the limit."*

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
| `suspense` | draw fires, if not instant | after 600 ms |
| `revealed` | draw fires (instant) or suspense ends | all fingers lifted |

## Behaviour

### The countdown

- Arms when the **second** finger lands, for `countdown` seconds (default 3).
- **Every further finger extends the deadline by exactly 1 second** - so late
  joiners never lose their chance.
- Dropping below two fingers cancels it and returns to `idle`.
- Progress drives two things at once: the edge glow (`0.1 → 0.5` opacity) and
  the frame (`0.2 → 1.0`).

### Touch rules

| Gesture | Effect |
| --- | --- |
| Press on empty surface | Adds a player |
| Drag an existing ring | **Repositions it — never counts as a new player** |
| Double-tap a ring | Lifts that player |
| Press during suspense/revealed | Ignored |

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
   otherwise 600 ms of `suspense` first.

Starter mode always reveals instantly - there is nothing to stagger.

### Reveal, per mode

| | Winner / first | Others |
| --- | --- | --- |
| **starter** | accent ring, `accentSoft` fill, scale 1.14, bloom animation | `line` ring, opacity 0.2, scale 0.88 |
| **order** | rank number at 46px, scale 1.12, `accentSoft` fill | rank at 32px, opacity fading `1 → 0.3` by rank |
| **teams** | — | team letter on the team fill, "TEAM" beneath |

During `suspense` every ring churns (`scale .94 ↔ 1.06`) in accent at 85%
opacity — the tell that something is being decided.

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
