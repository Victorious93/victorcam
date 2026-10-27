# SRT ingest — capability & certainty

## What we ship

VictorCam's `SrtDataSource` ingests **MPEG-TS over UDP** from `srt://host:port`
URLs (caller-oriented socket). Suitable for lab TS feeds that already speak
plain UDP/TS on the advertised port.

**Certainty: Medium** for bare TS-over-UDP lab feeds.
**Certainty: Low / Not implemented** for real SRT (libsrt handshake, AES
encryption / passphrase, latency/linger, rendezvous, streamid).

## Why not full libsrt?

As of this commit there is **no permissively redistributable, Maven-published
libsrt AAR / JNI wrapper** we can vendor without:

- building native libsrt (permissive upstream) + JNI ourselves for every ABI, or
- accepting a proprietary / non-Maven binary.

We therefore keep the TS-over-UDP path and parse SRT URL query options so a
future libsrt-backed `DataSource` can drop in without URL-format churn.

## Parsed URL options (forward-compatible)

```
srt://host:9000?mode=caller&latency=120&passphrase=secret&streamid=live/main
```

| Option | Parsed | Applied today |
|--------|--------|---------------|
| `mode` (`caller` / `listener` / `rendezvous`) | yes | caller UDP only |
| `latency` (ms) | yes | socket timeout hint only |
| `passphrase` / `pbkeylen` | yes | **ignored / rejected** — no encryption |
| `streamid` | yes | carried in `SrtOptions` |

## Swap path

Replace `SrtDataSource.Factory` in `StreamMediaSourceFactory.srtSource` with a
libsrt-backed factory when one is available under an acceptable license.
