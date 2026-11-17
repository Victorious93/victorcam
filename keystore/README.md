# VictorCam debug keystore

`victorcam-debug.jks` is VictorCam's fixed **debug** signing key. It is committed on purpose so every debug build, local or CI, is signed with the same certificate:

- Alias: `victorcamdebug`
- Certificate SHA-256: `8B:56:F8:18:93:B2:4F:02:BC:6C:93:39:39:36:32:3E:C5:74:86:D9:34:E2:9B:73:7C:E1:A7:B4:24:C5:B6:0D`

Its passwords are **not** in git. Provide them one of two ways:

- `local.properties`: `victorcam.debug.storePassword=...` and `victorcam.debug.keyPassword=...`
- Environment: `VICTORCAM_DEBUG_STORE_PASSWORD` and `VICTORCAM_DEBUG_KEY_PASSWORD` (CI reads these from repository secrets of the same names)

Without them, Gradle falls back to the default Android debug key, which produces a different certificate that fails the fleet theme pin and won't install over fleet builds.

**Release keys are never committed to this repo.**
