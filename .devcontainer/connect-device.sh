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
#       ./.devcontainer/connect-device.sh pair 192.168.1.42:37somePort 123456
#     Then connect on the (different) port listed under Wireless debugging:
#       ./.devcontainer/connect-device.sh connect 192.168.1.42:5555
#
#   Older devices (needs one USB connection to the HOST first):
#     On the host: adb tcpip 5555
#     Then:  ./.devcontainer/connect-device.sh connect <phone-ip>:5555
set -euo pipefail

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
  *)
    sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac
