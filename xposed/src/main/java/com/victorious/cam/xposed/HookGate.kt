package com.victorious.cam.xposed

/**
 * Master gate for Camera2 substitution. Default OFF.
 * Requires explicit opt-in flag (persisted by the host app) AND per-app allow list,
 * AND must pass [HookDenylist].
 */
object HookGate {
    @Volatile
    var masterEnabled: Boolean = false

    /** Packages the user explicitly opted into (never includes denylisted ones). */
    @Volatile
    var optedInPackages: Set<String> = emptySet()

    fun canHook(packageName: String): Boolean {
        if (!masterEnabled) return false
        if (HookDenylist.isDenied(packageName)) return false
        return packageName in optedInPackages
    }

    fun resetToSafeDefaults() {
        masterEnabled = false
        optedInPackages = emptySet()
    }
}
