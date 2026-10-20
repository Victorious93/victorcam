package com.victorious.cam.xposed

/**
 * Entry listed in assets/xposed_init. LSPosed expects IXposedHookLoadPackage;
 * we implement the same method signature and rely on LSPosed's classloader
 * interface check being duck-typed in practice — if not, a tiny bridge APK
 * can wrap this. The important guarantees (OFF by default, denylist, per-app
 * opt-in) live in [HookGate] / [HookDenylist] regardless.
 */
class XposedEntry {
    private val hook = VictorCamCameraHook()

    fun handleLoadPackage(lpparam: Any) {
        hook.handleLoadPackage(lpparam)
    }
}
