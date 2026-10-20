package com.victorious.cam.data

import android.content.Context

/**
 * First-launch disclosure acceptance. Persisted in shared preferences.
 */
class DisclosurePrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasAccepted(): Boolean = prefs.getBoolean(KEY_ACCEPTED, false)

    fun accept() {
        prefs.edit().putBoolean(KEY_ACCEPTED, true).apply()
    }

    companion object {
        const val PREFS_NAME = "victorcam_disclosure"
        const val KEY_ACCEPTED = "accepted"
    }
}
