# VictorCam

OBS-style **virtual camera feed** for Android (Camera2 / CameraX consumers).

**applicationId / namespace:** `com.victorious.cam`  
**Stack:** Kotlin, Jetpack Compose, Media3 ExoPlayer, Room, optional `:xposed` module

![VictorCam logo](assets/victorcam-logo.jpg)

## Purpose

| Source | Implementation |
|--------|----------------|
| Video file | Media3 ExoPlayer → shared `FramePipeline` |
| Still image | Bitmap decode → pipeline |
| Looping clip | ExoPlayer `REPEAT_MODE_ONE` |
| Screen region | MediaProjection + foreground service + crop rect |
| Network stream | RTSP / HTTP(S) / HLS / RTMP; SRT TS-over-UDP; NDI-over-RTP fallback |
| Multi-feed PiP | `MultiFeedMixer` composes 2+ sources → pipeline / overlays / recorder |
| Phone camera / screen via scrcpy (optional, off by default) | Linux PC helper: scrcpy 2.x `--video-source=camera` or `display` + v4l2loopback. Does **not** feed the on-device pipeline. See [docs/SCRCPY.md](docs/SCRCPY.md). |

Overlays (text, logo, timestamp) composite onto frames. Feed presets persist in Room.

## Intended use

Privacy testing, streaming demos, **user-owned** content only.  
**Not** for fraud, KYC/biometric bypass, or impersonation. First-launch disclosure required.

## Camera intents

`CaptureHandlerActivity` handles `IMAGE_CAPTURE` / `VIDEO_CAPTURE` / `STILL_IMAGE_CAMERA`.

## Network / SRT / NDI

| Scheme | Implementation | Certainty |
|--------|----------------|-----------|
| RTMP/RTMPS | Media3 `RtmpDataSource` | High |
| SRT | `SrtDataSource` MPEG-TS over UDP (caller); options parsed | **Medium** lab TS; **Low** full libsrt (not vendored) — [docs/SRT.md](docs/SRT.md) |
| RTSP/HTTP/HLS/DASH | Media3 defaults | High |
| NDI | Proprietary SDK **not** bundled; `ndi+rtp://` / gateway map | **Medium** RTP/gateway; N/A native — [docs/NDI.md](docs/NDI.md) |

## Optional advanced path: root / LSPosed (`:xposed`)

OFF by default. Cross-process frames use versioned `SharedFrameTransport` (backpressure).

- Explicit in-app opt-in + hard warnings
- Per-app allow list only
- **Hardcoded denylist** refuses banking, payment, government ID, and KYC packages
- Virtual mic system inject: [docs/VIRTUAL_MIC.md](docs/VIRTUAL_MIC.md) (stock limitation unchanged without root)

## Build

```bash
./gradlew assembleDebug test
```

Debug signing: optional local keystore via `local.properties` /
`VICTORCAM_DEBUG_*` env vars (see `keystore/README.md`). Keystores and
`local.properties` are gitignored — never commit passwords or `*.jks`.

## Optional path: phone camera via scrcpy

OFF by default. A Linux PC helper (`scripts/victorcam-scrcpy.sh`) can present this phone's camera (scrcpy 2.x `--video-source=camera`, Android 12+) or screen (`--video-source=display`) as a v4l2loopback webcam. In-app setup: **Phone camera via scrcpy (optional)**.

This does not inject frames into VictorCam's on-device virtual camera pipeline. scrcpy is not bundled.

Credit: [scrcpy](https://github.com/Genymobile/scrcpy) by Genymobile and contributors, licensed under the Apache License 2.0. See [NOTICE](NOTICE).
