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
| Network stream | RTSP / HTTP(S) / HLS via Media3; RTMP/SRT best-effort |

Overlays (text, logo, timestamp) composite onto frames. Feed presets persist in Room.

## Intended use

Privacy testing, streaming demos, **user-owned** content only.  
**Not** for fraud, KYC/biometric bypass, or impersonation. First-launch disclosure required.

## Camera intents

`CaptureHandlerActivity` handles `IMAGE_CAPTURE` / `VIDEO_CAPTURE` / `STILL_IMAGE_CAMERA`.

## Network / NDI

- **Supported:** `rtsp://`, `http(s)://`, HLS (`.m3u8`), DASH (`.mpd`); RTMP/SRT URLs are accepted and attempted via ExoPlayer (device/extension dependent).
- **NDI:** **TODO** — no permissively licensed NDI mobile SDK is vendored here. Do not expect `ndi://` to play.

## Optional advanced path: root / LSPosed (`:xposed`)

Separate Gradle module **`xposed`** (OFF by default):

- Explicit in-app opt-in screen with hard warnings
- Per-app allow list only
- **Hardcoded denylist** refuses banking, payment, government ID, and KYC/identity packages (exact names + category tokens). See `HookDenylist` and tests.
- No hook runs unless master gate is enabled **and** package is opted in **and** not denylisted

Denylist examples (non-exhaustive): PayPal, Venmo, Cash App, Chase, BoA, Wells Fargo, Citi, Amex, IRS/gov tokens, Onfido, Jumio, Veriff, Socure, Persona, Sumsub, packages containing `bank`, `kyc`, `wallet`, `payment`, etc.


## Network ingest (features-2)

| Scheme | Implementation | Certainty |
|--------|----------------|-----------|
| RTMP/RTMPS | Media3 `media3-datasource-rtmp` (`RtmpDataSource`) | High for progressive FLV/RTMP that Media3 supports |
| SRT | In-tree `SrtDataSource` (MPEG-TS over UDP caller path) | **Medium/Low** — no full libsrt handshake/encryption; lab TS feeds only |
| RTSP/HTTP/HLS/DASH | Media3 defaults | High |
| NDI | Not bundled | N/A |

## Virtual mic

Stock Android cannot register VictorCam as the system microphone. `AudioFeedController` plays ExoPlayer/AudioTrack audio for **preview** and for **our** capture-intent results. System-wide mic replacement remains a root/LSPosed advanced path (not default).

## Recording & trim

`FeedRecorder` encodes composed pipeline frames to MP4 (MediaCodec surface input). `FeedTrimmer` does keyframe-aligned remux trim via MediaExtractor/MediaMuxer.

## Scheduling / hotkeys

WorkManager + AlarmManager schedule preset switches; notification actions provide Next/Prev quick-switch (`FeedSwitchBus`).

## Backup

`PresetBackup` exports/imports JSON or zip (logos/) for SAF share/restore.

## Build

```bash
./gradlew assembleDebug test
```

Debug keystore: `keystore/victorcam-debug.jks` (password `android`, alias `victorcamdebug`). No release keys.
