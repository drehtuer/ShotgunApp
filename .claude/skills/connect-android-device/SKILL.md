---
name: connect-android-device
description: Connect to the physical Android phone (Pixel 10a) over Wi-Fi debugging and drive it with adb - pair, discover the random connect port, install, launch, screenshot, read logcat and pull the draw database. Use whenever a change needs checking on real hardware rather than the emulator, in particular anything touching multi-touch, haptics or timing, or when adb reports no devices and the phone must be reattached.
---

# Connecting to the Android device

The emulator cannot test multi-touch, and the draw surface is the whole point of
the app - so anything touching it must be checked on the phone before a PR is
called done. See `.claude/CLAUDE.md`.

The container uses bridge networking and `adb connect` is outbound, so Wi-Fi
debugging works without host networking.

## Before anything: is it already attached?

```bash
adb devices -l
```

A line ending `device` is ready. `offline` means a stale entry - `adb disconnect
<ip:port>` and reconnect. Empty means start at *Pair*.

## 1. Pair (once per phone, or after "Forget")

On the phone: **Developer options > Wireless debugging > Pair device with
pairing code**. That dialog shows an `ip:port` and a six-digit code.

```bash
./.devcontainer/connect-device.sh pair <ip>:<pairingPort> <code>
```

Expect `Successfully paired`. The pairing dialog times out - if it fails,
reopen it and read the fresh port and code.

## 2. Connect

**The connect port is not the pairing port, and it is not 5555.** Wireless
debugging picks a random ephemeral port, shown on the main *Wireless debugging*
screen (the pairing dialog shows a different one). If the phone is in reach,
read it and use it:

```bash
./.devcontainer/connect-device.sh connect <ip>:<port>
```

Otherwise find it - the script scans 30000-50000 and tries each open port until
one talks adb:

```bash
./.devcontainer/connect-device.sh discover <ip>
```

`adb mdns services` returns nothing here: mDNS is link-local and does not cross
the container's NAT. Do not rely on it.

### If pairing succeeded but connecting fails

The pairing port stays open afterwards and looks like a candidate; connecting to
it yields a device stuck at `offline`. Drop it and use the other open port:

```bash
adb disconnect <ip>:<pairingPort>
```

## 3. Target the right device

If an emulator is also running, every command is ambiguous. Pin it:

```bash
export ANDROID_SERIAL=<ip>:<port>     # or pass -s <serial> per command
adb $ANDROID_SERIAL shell getprop ro.product.model
```

## 4. Build, install, run

```bash
./gradlew installDebug
adb shell am start -n de.drehtuer.shotgun/.MainActivity
```

If Gradle fails with *"does not provide the required capabilities:
[JAVA_COMPILER]"*, there is a JRE but no JDK - you are probably outside the
devcontainer. See `docs/build-environment.md`. A previously built APK is a
usable fallback when the tree has not changed since:

```bash
find app/build/outputs -name "*.apk"
# confirm nothing is newer than it before trusting it:
find app/src/main -name "*.kt" -newer <apk>
adb install -r <apk>
```

## 5. Check what happened

```bash
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png .
adb logcat -d -b crash | tail -20
```

Check **both palettes** - many theme bugs show in only one:

```bash
adb shell cmd uimode night yes    # dark
adb shell cmd uimode night no     # light
```

## 6. Read a draw back out of the database

The debug build is debuggable, so `run-as` reaches the app's files. Room runs in
**WAL mode**, so the `.db` alone is nearly empty - pull all three files or the
tables look missing:

```bash
for f in shotgun.db shotgun.db-shm shotgun.db-wal; do
  adb exec-out run-as de.drehtuer.shotgun cat "databases/$f" > "t.db${f#shotgun.db}"
done
```

There is no `sqlite3` on the device; query locally with Python:

```bash
python3 -c "
import sqlite3
c = sqlite3.connect('t.db')
for d in c.execute('select id,mode,team_count,timestamp from draws order by id'):
    pts = c.execute('select x,y,won,assignment from draw_points where draw_id=?', (d[0],)).fetchall()
    print(d, 'fingers:', len(pts), 'ranks:', sorted(p[3] for p in pts))
"
```

This is the reliable way to count how many fingers the app actually tracked -
one `draw_points` row per finger. PLAYER ORDER mode gives each a distinct rank.

## Hardware notes

Reading touchscreen limits needs adb; the app itself is sandboxed out of
`/proc/bus/input/devices` and `/dev/input/*`.

```bash
adb shell getevent -pl | grep -A2 ABS_MT_SLOT     # panel slots
adb shell dumpsys input | grep -A1 "Touch Input Mapper"
```

`ABS_MT_SLOT max` is the highest slot **index** and is zero-based: `max 9` means
**ten** simultaneous contacts. The Pixel 10a reports exactly that - see
`docs/design.md`, *How many fingers*.
