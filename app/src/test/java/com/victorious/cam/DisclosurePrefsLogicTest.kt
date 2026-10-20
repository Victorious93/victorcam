package com.victorious.cam

import com.victorious.cam.data.DisclosurePrefs
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lightweight check of disclosure preference key constants
 * (Android SharedPreferences itself needs instrumentation).
 */
class DisclosurePrefsLogicTest {
    @Test
    fun preferenceKeysAreStable() {
        assertEquals("victorcam_disclosure", DisclosurePrefs.PREFS_NAME)
        assertEquals("accepted", DisclosurePrefs.KEY_ACCEPTED)
    }
}
