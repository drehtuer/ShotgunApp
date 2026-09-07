#!/usr/bin/env bash
# Connects to a physical phone over Wi-Fi from inside the container.
#
# The container uses bridge networking, so an outbound connection to a phone on
# the host LAN works without host networking. Multi-touch cannot be tested on
# the emulator, so this is the path for real gesture testing.
#
#   Android 11+ (recommended, no cable):
#     On the phone: Developer options > Wireless debugging > Pair device with
#     pairing code. Then, using the PAIRING port and code shown there:
#       ./.devcontainer/connect-device.sh pair 192.168.1.42:37123 123456
#     Pairing and connecting use DIFFERENT ports, and the connect port is
#     random - it is the one under "Wireless debugging", not the pairing one,
#     and it is not 5555. If the phone is not showing it, find it:
#       ./.devcontainer/connect-device.sh discover 192.168.1.42
#     Then:
#       ./.devcontainer/connect-device.sh connect 192.168.1.42:43227
#
#   Older devices (needs one USB connection to the HOST first):
#     On the host: adb tcpip 5555
#     Then:  ./.devcontainer/connect-device.sh connect <phone-ip>:5555
#
# Note: `adb mdns services` finds nothing under bridge networking, because
# mDNS is link-local and does not cross the NAT - hence `discover`.
set -euo pipefail

# Wireless debugging picks an ephemeral port. Scanning the range is crude but
# reliable, and it is the only option left once mDNS is ruled out.
SCAN_LO=30000
SCAN_HI=50000
SCAN_CHUNKS=8

discover() {
  local ip="$1" span chunk lo hi tmp
  command -v nc >/dev/null || { echo "discover needs 'nc' (netcat)" >&2; exit 3; }
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN

  span=$(( SCAN_HI - SCAN_LO + 1 ))
  chunk=$(( (span + SCAN_CHUNKS - 1) / SCAN_CHUNKS ))
  echo "Scanning ${ip} ports ${SCAN_LO}-${SCAN_HI} for an adb listener..." >&2

  for (( lo = SCAN_LO; lo <= SCAN_HI; lo += chunk )); do
    hi=$(( lo + chunk - 1 )); [ "$hi" -gt "$SCAN_HI" ] && hi=$SCAN_HI
    nc -z -w 1 "$ip" "$lo-$hi" 2>&1 | grep -oE '[0-9]+ port' > "$tmp/$lo" &
  done
  wait

  local ports
  ports="$(cat "$tmp"/* 2>/dev/null | grep -oE '^[0-9]+' | sort -un)"
  [ -n "$ports" ] || { echo "No open ports found. Is Wireless debugging still on?" >&2; exit 1; }

  echo "Open ports: $(echo "$ports" | tr '\n' ' ')" >&2
  # The pairing port also stays open, so try each and keep the one that talks
  # adb. A port that pairs but does not connect reports the device 'offline'.
  local p
  for p in $ports; do
    if adb connect "${ip}:${p}" 2>&1 | grep -q '^connected'; then
      echo "Connected on ${ip}:${p}"
      adb devices -l
      return 0
    fi
    adb disconnect "${ip}:${p}" >/dev/null 2>&1 || true
  done
  echo "None of the open ports accepted an adb connection." >&2
  echo "Pair first: $0 pair ${ip}:<pairingPort> <code>" >&2
  exit 1
}

cmd="${1:-}"
case "$cmd" in
  pair)
    [ $# -eq 3 ] || { echo "usage: $0 pair <ip:pairingPort> <code>" >&2; exit 2; }
    adb pair "$2" "$3"
    ;;
  connect)
    [ $# -eq 2 ] || { echo "usage: $0 connect <ip:port>" >&2; exit 2; }
    adb connect "$2"
    adb devices
    ;;
  discover)
    [ $# -eq 2 ] || { echo "usage: $0 discover <ip>" >&2; exit 2; }
    discover "$2"
    ;;
  *)
    sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac
