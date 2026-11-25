package com.victorious.cam.scrcpy

import android.content.Context

/**
 * Optional scrcpy path gate. Always starts **OFF**.
 *
 * Enabling only records that the user opened the in-app setup and opted in.
 * It does not launch scrcpy or inject frames into the on-device pipeline.
 * The PC helper (`scripts/victorcam-scrcpy.sh`) is what actually runs scrcpy.
 */
object ScrcpyGate {
    @Volatile
    var enabled: Boolean = false
}

class ScrcpyPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        ScrcpyGate.enabled = value
    }

    fun restoreToGate() {
        ScrcpyGate.enabled = isEnabled()
    }

    companion object {
        const val PREFS_NAME = "victorcam_scrcpy"
        const val KEY_ENABLED = "enabled"
    }
}
