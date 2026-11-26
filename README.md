<div align="center">

<img src="assets/victorcam-logo.jpg" width="260" alt="VictorCam logo"/>

# VictorCam

**An OBS-style virtual camera feed for Android.**

<p>
  <a href="https://github.com/Victorious93/victorcam/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/Victorious93/victorcam/ci.yml?branch=main&style=flat-square&labelColor=0B0F1A&logo=githubactions&logoColor=00E5FF&label=CI" alt="CI"/></a>
  <img src="https://img.shields.io/badge/platform-Android-00E5FF?style=flat-square&labelColor=0B0F1A" alt="Platform"/>
  <a href="https://victorious-framework.pages.dev"><img src="https://img.shields.io/badge/Victorious-Framework-FF2BD6?style=flat-square&labelColor=0B0F1A" alt="Victorious Framework"/></a>
</p>

<p><a href="https://victorious-framework.pages.dev">Victorious Framework</a></p>

</div>

---

An OBS-style **virtual camera feed** for Android (Camera2 / CameraX consumers). Kotlin, Jetpack Compose, Media3 ExoPlayer, Room, with an optional `:xposed` module. Package id: `com.victorious.cam`, version 0.4.0 (`features-3`).

> **Intended use:** privacy testing and streaming demos with **user-owned** content only. **Not** for fraud, KYC or biometric bypass, or impersonation. A first-launch disclosure is required.
>
> **Status: early development.** CI builds the app; the README makes no device-testing claims.

## Features

| Source | Implementation |
|--------|----------------|
| Video file | Media3 ExoPlayer feeding the shared `FramePipeline` |
| Still image | Bitmap decode into the pipeline |
| Looping clip | ExoPlayer `REPEAT_MODE_ONE` |
| Screen region | MediaProjection + foreground service + crop rectangle |
| Network stream | RTSP / HTTP(S) / HLS / RTMP; SRT as MPEG-TS over UDP; NDI-over-RTP fallback |
| Multi-feed picture-in-picture | `MultiFeedMixer` composes two or more sources into the pipeline, overlays and recorder |
| Phone camera / screen via scrcpy (optional, off by default) | Linux PC helper: scrcpy 2.x `--video-source=camera` or `display` plus v4l2loopback. Does **not** feed the on-device pipeline. See [docs/SCRCPY.md](docs/SCRCPY.md). |

Overlays (text, logo, timestamp) are composited onto frames. Feed presets persist in Room. `CaptureHandlerActivity` handles the `IMAGE_CAPTURE`, `VIDEO_CAPTURE` and `STILL_IMAGE_CAMERA` camera intents. The code also contains recording, scheduling, backup and a read-only VictorLaunch fleet theme client.

### Network, SRT and NDI: what is real

| Scheme | Implementation | Confidence |
|--------|----------------|------------|
| RTMP / RTMPS | Media3 `RtmpDataSource` | High |
| SRT | `SrtDataSource`, MPEG-TS over UDP (caller); options parsed | **Medium** with lab TS; **Low** for full libsrt (not vendored). See [docs/SRT.md](docs/SRT.md) |
| RTSP / HTTP / HLS / DASH | Media3 defaults | High |
| NDI | Proprietary SDK is **not** bundled; `ndi+rtp://` / gateway mapping | **Medium** for RTP/gateway; native NDI not available. See [docs/NDI.md](docs/NDI.md) |

### Optional advanced path: root / LSPosed (`:xposed`)

Off by default. Cross-process frames use a versioned `SharedFrameTransport` with backpressure.

- Explicit in-app opt-in with hard warnings.
- Per-app allow list only.
- A **hardcoded denylist** refuses banking, payment, government-ID and KYC packages.
- System-wide virtual microphone injection: see [docs/VIRTUAL_MIC.md](docs/VIRTUAL_MIC.md) (the stock limitation is unchanged without root).

### Optional path: phone camera via scrcpy

Off by default. A Linux PC helper (`scripts/victorcam-scrcpy.sh`) can present this phone's camera (scrcpy 2.x `--video-source=camera`, Android 12+) or screen (`--video-source=display`) as a v4l2loopback webcam. In-app setup: **Phone camera via scrcpy (optional)**.

This does not inject frames into VictorCam's on-device virtual camera pipeline. scrcpy is not bundled.

Credit: [scrcpy](https://github.com/Genymobile/scrcpy) by Genymobile and contributors, licensed under the Apache License 2.0. See [NOTICE](NOTICE).

## Optional root module (Magisk / KernelSU / APatch)

OFF by default, and **the app works fully without it**. A Magisk-format
module under [`module/`](module/) can install VictorCam as a privileged
system app (`/system/priv-app`) and pre-grant the runtime permissions it
already declares.

It does **not** weaken the banking / payment / government-ID / KYC
denylist, hide root, spoof device integrity, or add fraud/KYC features.
Hard warnings and a full does / does not list:
[module/README.md](module/README.md).

CI packs a flashable zip (with the debug APK) as the
`victorcam-root-module` artifact.

## Install

No release has been published yet. Debug APKs are built by [CI](https://github.com/Victorious93/victorcam/actions/workflows/ci.yml). minSdk 26.

## Build from source

JDK 17 and the Android SDK.

```bash
git clone https://github.com/Victorious93/victorcam.git
cd victorcam
./gradlew assembleDebug test
```

### Debug signing

`keystore/victorcam-debug.jks` is the project's fixed **debug** key, committed on purpose so every debug build has the same certificate. Its passwords are **not** in git: provide them in `local.properties` (`victorcam.debug.storePassword`, `victorcam.debug.keyPassword`) or the `VICTORCAM_DEBUG_STORE_PASSWORD` / `VICTORCAM_DEBUG_KEY_PASSWORD` environment variables (CI reads repository secrets of the same names). Without them Gradle falls back to the default Android debug key, which fails the fleet theme pin. Release keys are never committed. See [keystore/README.md](keystore/README.md).

## Permissions

`CAMERA`, `RECORD_AUDIO`, `INTERNET`, `READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES`, `FOREGROUND_SERVICE` (media projection), `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED`. The `:xposed` module needs root and LSPosed and is optional.

---

## Victorious fleet

VictorCam is part of the [Victorious Framework](https://victorious-framework.pages.dev), a family of Android and PC tools by Don Victor. **Every app works standalone; fleet integrations are optional** and never required.

Related: [droidcommand-AI](https://github.com/Victorious93/droidcommand-AI) (fleet controller), [VictorLaunch](https://github.com/Victorious93/victorlaunch) (fleet launcher and theme provider), [victorious-fleet](https://github.com/Victorious93/victorious-fleet) (index of all fleet apps). The full roster lives in [victorious-fleet](https://github.com/Victorious93/victorious-fleet), and live status is in [fleet-manifest](https://github.com/Victorious93/fleet-manifest).

## Contributing

Issues and pull requests are welcome. Keep changes focused, match the existing code style, never commit secrets or release keys, and keep any fleet integration optional so the app still works on its own. Open a PR against the default branch and wait for CI to pass.

## License

This repository does not include a LICENSE file yet, so no license is granted beyond what the author states elsewhere. Please open an issue before reusing the code.
