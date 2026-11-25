#!/usr/bin/env bash
# VictorCam optional helper — phone camera or screen as a Linux v4l2 webcam.
#
# Uses a user-installed scrcpy 2.x (--video-source=camera|display) and
# v4l2loopback. scrcpy is not bundled; see https://github.com/Genymobile/scrcpy
# (Apache-2.0, copyright Genymobile and contributors) and ../NOTICE.
#
# Off by default: this script does nothing until you run it.
# Intended use: privacy testing, streaming demos, user-owned content only.
# Not for fraud, KYC/biometric bypass, or impersonation.
#
# Certainty: command flags match published scrcpy 2.x docs; this script has
# not been end-to-end tested against a physical phone in this change.
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: victorcam-scrcpy.sh [options]

  --camera             Phone camera via scrcpy (default). Needs Android 12+.
  --screen, --display  Phone screen-mirror via scrcpy.
  --list-cameras       Ask scrcpy to list cameras, then exit.
  --device /dev/videoN v4l2loopback node (default: /dev/video10)
  --facing front|back|external
  --camera-id ID       Mutually exclusive with --facing
  --camera-size WxH    e.g. 1920x1080
  --serial SERIAL      adb/scrcpy serial
  --show-window        Do not pass --no-video-playback
  --with-audio         Do not pass --no-audio
  --setup-loopback     Only load v4l2loopback for --device, then exit
  -h, --help           Show this help

Examples:
  sudo modprobe v4l2loopback devices=1 video_nr=10 card_label=VictorCam exclusive_caps=1
  scripts/victorcam-scrcpy.sh --camera
  scripts/victorcam-scrcpy.sh --screen
EOF
}

die() { echo "error: $*" >&2; exit 1; }

MODE="camera"
DEVICE="/dev/video10"
FACING=""
CAMERA_ID=""
CAMERA_SIZE=""
SERIAL=""
NO_PLAYBACK=1
NO_AUDIO=1
LIST_CAMERAS=0
SETUP_ONLY=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --camera) MODE="camera"; shift ;;
    --screen|--display) MODE="display"; shift ;;
    --list-cameras) LIST_CAMERAS=1; shift ;;
    --device)
      [[ $# -ge 2 ]] || die "--device needs a path"
      DEVICE="$2"
      shift 2
      ;;
    --facing)
      [[ $# -ge 2 ]] || die "--facing needs front|back|external"
      FACING="$2"
      shift 2
      ;;
    --camera-id)
      [[ $# -ge 2 ]] || die "--camera-id needs an id"
      CAMERA_ID="$2"
      shift 2
      ;;
    --camera-size)
      [[ $# -ge 2 ]] || die "--camera-size needs WIDTHxHEIGHT"
      CAMERA_SIZE="$2"
      shift 2
      ;;
    --serial)
      [[ $# -ge 2 ]] || die "--serial needs a serial"
      SERIAL="$2"
      shift 2
      ;;
    --show-window) NO_PLAYBACK=0; shift ;;
    --with-audio) NO_AUDIO=0; shift ;;
    --setup-loopback) SETUP_ONLY=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
done

if [[ "$(uname -s)" != "Linux" ]]; then
  die "v4l2loopback is Linux-only; this helper does not support $(uname -s)"
fi

if [[ ! "$DEVICE" =~ ^/dev/video[0-9]+$ ]]; then
  die "device must look like /dev/videoN, got: $DEVICE"
fi

if [[ -n "$FACING" && -n "$CAMERA_ID" ]]; then
  die "--facing and --camera-id cannot be combined"
fi
if [[ -n "$FACING" && "$FACING" != "front" && "$FACING" != "back" && "$FACING" != "external" ]]; then
  die "--facing must be front, back, or external"
fi
if [[ -n "$FACING" && "$MODE" != "camera" ]]; then
  die "--facing is only valid with --camera"
fi
if [[ -n "$CAMERA_ID" && "$MODE" != "camera" ]]; then
  die "--camera-id is only valid with --camera"
fi
if [[ -n "$CAMERA_SIZE" && "$MODE" != "camera" ]]; then
  die "--camera-size is only valid with --camera"
fi
if [[ -n "$CAMERA_SIZE" && ! "$CAMERA_SIZE" =~ ^[0-9]+x[0-9]+$ ]]; then
  die "--camera-size must look like WIDTHxHEIGHT"
fi

setup_loopback() {
  local nr="${DEVICE#/dev/video}"
  if [[ -e "$DEVICE" ]]; then
    echo "Using existing $DEVICE"
    return 0
  fi
  echo "Loading v4l2loopback for $DEVICE (may require sudo)"
  if ! command -v modprobe >/dev/null 2>&1; then
    die "modprobe not found; install v4l2loopback (e.g. v4l2loopback-dkms)"
  fi
  local args=(v4l2loopback devices=1 "video_nr=${nr}" card_label=VictorCam exclusive_caps=1)
  if [[ "$(id -u)" -eq 0 ]]; then
    modprobe "${args[@]}"
  else
    sudo modprobe "${args[@]}"
  fi
  [[ -e "$DEVICE" ]] || die "modprobe ran but $DEVICE did not appear. Try: v4l2-ctl --list-devices"
  echo "Created $DEVICE"
}

if [[ "$SETUP_ONLY" -eq 1 ]]; then
  setup_loopback
  exit 0
fi

command -v scrcpy >/dev/null 2>&1 || die "scrcpy not found on PATH. Install scrcpy 2.x: https://github.com/Genymobile/scrcpy"
command -v adb >/dev/null 2>&1 || die "adb not found on PATH"

ver_out="$(scrcpy --version 2>&1 || true)"
ver="$(printf '%s\n' "$ver_out" | grep -Eo '[0-9]+\.[0-9]+(\.[0-9]+)?' | head -n1 || true)"
if [[ -z "$ver" ]]; then
  die "could not parse scrcpy version from: $ver_out"
fi
major="${ver%%.*}"
if [[ "$major" -lt 2 ]]; then
  die "scrcpy 2.x is required for --video-source=camera (found $ver)"
fi
echo "Found scrcpy $ver"

if [[ "$LIST_CAMERAS" -eq 1 ]]; then
  args=(scrcpy)
  [[ -n "$SERIAL" ]] && args+=("--serial=$SERIAL")
  args+=(--list-cameras)
  echo "Running: ${args[*]}"
  exec "${args[@]}"
fi

setup_loopback

if ! adb ${SERIAL:+-s "$SERIAL"} get-state >/dev/null 2>&1; then
  echo "warning: adb did not report a device; scrcpy may still find one." >&2
fi

args=(scrcpy)
[[ -n "$SERIAL" ]] && args+=("--serial=$SERIAL")
args+=("--video-source=$MODE" "--v4l2-sink=$DEVICE")
[[ "$NO_PLAYBACK" -eq 1 ]] && args+=(--no-video-playback)
[[ "$NO_AUDIO" -eq 1 ]] && args+=(--no-audio)
[[ -n "$FACING" ]] && args+=("--camera-facing=$FACING")
[[ -n "$CAMERA_ID" ]] && args+=("--camera-id=$CAMERA_ID")
[[ -n "$CAMERA_SIZE" ]] && args+=("--camera-size=$CAMERA_SIZE")

echo "Starting phone $MODE via scrcpy -> $DEVICE"
echo "Running: ${args[*]}"
exec "${args[@]}"
