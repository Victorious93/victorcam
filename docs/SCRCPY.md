# Phone camera (and screen) via scrcpy

Optional, **off by default**. VictorCam can point you at a Linux PC helper that
uses [scrcpy](https://github.com/Genymobile/scrcpy) 2.x to present this phone's
**camera** or **screen** as a v4l2loopback webcam.

This path is for privacy testing, streaming demos, and **user-owned** content
only. It is **not** for fraud, KYC/biometric bypass, or impersonation.

scrcpy is copyright Genymobile and contributors, licensed under Apache-2.0.
See [NOTICE](../NOTICE). scrcpy is **not** bundled in this app or repo.

## What this does — and what it does not

| Path | What happens | Certainty |
|------|----------------|-----------|
| `scripts/victorcam-scrcpy.sh --camera` | PC runs `scrcpy --video-source=camera --v4l2-sink=/dev/videoN` | **High** for the command line (unit-tested builder + published scrcpy flags). **Untested here** on a real phone/PC in this change. Camera mirroring needs **Android 12+** and scrcpy **2.x**. |
| `scripts/victorcam-scrcpy.sh --screen` | Same, but `--video-source=display` (screen-mirror). Useful if you want the phone screen, including a VictorCam preview, as the PC webcam. | **High** for the command line. **Untested here** end-to-end. |
| On-device virtual camera pipeline | These sources do **not** inject frames into VictorCam's `FramePipeline`. Capture happens on the PC. | **High** — by design, unit-tested. |

Linux only for `--v4l2-sink`. macOS and Windows are not covered by the helper.

## Prerequisites

1. Linux PC
2. [scrcpy 2.x](https://github.com/Genymobile/scrcpy) on `PATH` (`scrcpy --version` must report 2.0 or newer)
3. `adb`, with USB debugging authorized on the phone
4. `v4l2loopback` (for example `sudo apt install v4l2loopback-dkms v4l-utils`)

## Usage

```bash
# Create a loopback node if you do not already have one (example: /dev/video10)
sudo modprobe v4l2loopback devices=1 video_nr=10 card_label=VictorCam exclusive_caps=1

# Phone camera -> v4l2 webcam (default)
scripts/victorcam-scrcpy.sh --camera

# Phone screen-mirror -> v4l2 webcam
scripts/victorcam-scrcpy.sh --screen

# List cameras the phone reports (Android 12+)
scripts/victorcam-scrcpy.sh --list-cameras
```

Useful flags: `--device /dev/videoN`, `--facing front|back|external`,
`--camera-id ID`, `--serial SERIAL`, `--show-window`.

Then open `/dev/video10` (or the device you chose) in any v4l2-capable app
(ffplay, VLC, OBS, a conferencing tool).

Equivalent raw scrcpy commands (also built by `ScrcpyCommandOptions`):

```bash
scrcpy --video-source=camera --v4l2-sink=/dev/video10 --no-video-playback --no-audio
scrcpy --video-source=display --v4l2-sink=/dev/video10 --no-video-playback --no-audio
```

## In-app setup

**Phone camera via scrcpy (optional)** in VictorCam shows these steps and a
master switch that starts **OFF**. Turning it on only acknowledges the path so
the optional feed-source rows can be started; it does not launch scrcpy from
the phone.

## Limits

- Camera source requires Android 12+ (scrcpy's own requirement).
- v4l2loopback loading usually needs root on the PC (`modprobe`).
- WebRTC/Chrome sometimes needs `exclusive_caps=1` on the loopback module.
- Do not leave USB debugging enabled on an untrusted computer.
