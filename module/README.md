# Optional VictorCam root module

Magisk / KernelSU / APatch module that can install VictorCam
(`com.victorious.cam`) as a privileged system app and pre-grant the
runtime permissions the APK already declares.

**The app works fully without this module.** It is off by default: you
only get a priv-app install if you flash the zip in a compatible manager
and reboot.

Intended use is the same as the app: privacy testing, streaming demos,
and **user-owned** content only. **Not** for fraud, KYC/biometric bypass,
or impersonation.

## Hard warnings

- Rooting a device and installing a privileged app has real security
  risk. You can break the system, void a warranty, or expose data.
- This is **not** a hide-root / Shamiko / Play Integrity fix. Banking
  and similar apps may still detect root. That is expected.
- The existing **HookDenylist** (banking, payment, government-ID, and
  KYC packages) is unchanged. This module does not disable, shrink, or
  bypass it.
- Privileged install does **not** turn on LSPosed / Camera2 rewrite /
  virtual-mic inject. Those stay behind the in-app opt-in and gates.
- MediaProjection screen capture still requires the system consent
  dialog.

## What it does

- Places `VictorCam.apk` at `/system/priv-app/VictorCam/` via a
  systemless overlay (Magisk-format module).
- Ships `system/etc/permissions/privapp-permissions-com.victorious.cam.xml`
  so privileged-permission enforcement has an allowlist for this
  package.
- After reboot, `service.sh` runs `pm grant` for the dangerous runtime
  permissions declared in the current manifest (`CAMERA`,
  `RECORD_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES`,
  `POST_NOTIFICATIONS`) and allows `SCHEDULE_EXACT_ALARM` via appops.
- If you already had a user-installed copy, the installer copies that
  APK into the module and then uninstalls the user copy (data kept)
  so the two do not conflict after reboot.

## What it does not do

- It does **not** grant `signature|privileged` permissions the APK
  does not request. The current manifest (versionName
  `0.4.0-features-3`) declares none, so the privapp-permissions
  allowlist is intentionally empty. [Certain] for the checked-in
  manifest; update the XML if a future APK adds privileged requests.
- It does **not** hide root, spoof attestations, change the denylist,
  or add fraud / KYC / impersonation features.
- It does **not** replace a user-debug or release signing key. The CI
  artifact bundles the **debug** APK. For a release build, pack your
  own APK with `module/pack-module.sh --apk <file>`.
- It does **not** make VictorCam a requirement for boot. Removing the
  module in the manager and rebooting returns the device to a normal
  user install (reinstall the APK if you still want the app).

## Compatibility

The zip is Magisk module format (`module.prop` at the zip root).

| Manager | Install path | Certainty |
|---------|--------------|-----------|
| Magisk 20.4+ | Manager or recovery (uses official `util_functions.sh`) | **High** for format; **untested** on a device in this change |
| KernelSU | Install the zip from the KernelSU manager app (not recovery) | **Likely** — KSU accepts Magisk-format modules; **untested** here |
| APatch | Install the zip from the APatch manager app (not recovery) | **Likely** — APatch accepts Magisk-format modules; **untested** here |

## Install

1. Build or download a VictorCam APK, **or** use the GitHub Actions
   artifact `victorcam-root-module` (includes the debug APK).
2. If you are packing locally:

   ```bash
   ./module/pack-module.sh --apk app/build/outputs/apk/debug/app-debug.apk
   ```

   A zip without `--apk` is still valid: on a booted rooted device the
   installer will copy an already-installed `com.victorious.cam`.
3. Flash the zip in Magisk, KernelSU, or APatch. Read the warnings in
   the installer output.
4. Reboot.

`META-INF/com/google/android/update-binary` follows the official Magisk
module installer (Magisk project, GPL-3.0), with a fallback message
telling KernelSU/APatch users to install from the manager app.

## CI

The `module-zip` job depends on `build-test`, packs this module with
the debug APK, and uploads `victorcam-root-module`.
