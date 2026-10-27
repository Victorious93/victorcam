# NDI input — licensing & fallback

## Official NDI SDK

The NewTek / NDI SDK is **royalty-free but proprietary** (not an OSI-permissive
open-source license). We **do not vendor** the NDI SDK binaries or headers in
this repository.

**Certainty: N/A** for native NDI discovery / NDI|HX in-tree.

## What we ship instead: NDI-over-RTP compatible fallback

Many NDI-adjacent workflows and gateways can bridge to **RTP/MPEG-TS** or
**RTSP**. VictorCam accepts:

| URL form | Behavior |
|----------|----------|
| `ndi://source-name` | Resolves via optional local gateway map → RTP/RTSP URL |
| `ndi+rtp://host:port` | Direct RTP/MPEG-TS UDP ingest (same path as lab TS) |
| `rtp://host:port` | Same TS-over-UDP path |

`NdiRtpBridge` performs the mapping. Without a configured gateway, `ndi://`
fails with a clear error pointing here.

**Certainty: Medium** for RTP/TS fallback when a gateway exposes RTP/RTSP.
**Certainty: Low** for full NDI discovery / codec without the proprietary SDK.

## Enabling a gateway map

Write `ndi.gateway.properties` under the app files dir (or pass a map to
`NdiRtpBridge`) for example:

```
Camera1=rtsp://192.168.1.20:8554/cam1
Desktop=rtp://192.168.1.20:5004
```
