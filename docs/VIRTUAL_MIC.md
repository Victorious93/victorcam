# Virtual microphone — stock vs root

## Stock (default)

`AudioFeedController` plays ExoPlayer / AudioTrack audio for **preview** and can
mux into **VictorCam's own** capture-intent outputs.

**Certainty: High** — this is all a normal app can do.
Android does **not** allow registering as the system microphone without
privileged roles or hooks.

## Root / Magisk path (`RootVirtualMicInjector`)

When Magisk/root + an LSPosed-style AudioRecord hook module are present, VictorCam
may feed PCM as the mic **to opted-in apps only**.

Gates (identical to camera substitution):

1. `HookGate.masterEnabled` — OFF by default
2. Per-app opt-in
3. Hardcoded `HookDenylist` (banking / payment / gov-ID / KYC)
4. Explicit hard-warning acknowledgement in the host UI

**Certainty: Medium** for the inject path itself (requires a loaded module);
**High** for the gating logic (unit-tested).

Stock limitation is **unchanged** when root is absent.
