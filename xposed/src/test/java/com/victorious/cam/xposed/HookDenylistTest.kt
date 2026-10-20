package com.victorious.cam.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HookDenylistTest {
    @Before
    fun reset() {
        HookGate.resetToSafeDefaults()
    }

    @Test
    fun deniesBanksAndKyc() {
        assertTrue(HookDenylist.isDenied("com.chase.sig.android"))
        assertTrue(HookDenylist.isDenied("com.mybank.mobile"))
        assertTrue(HookDenylist.isDenied("com.vendor.kyc.sdk"))
        assertTrue(HookDenylist.isDenied("com.onfido.demo"))
        assertTrue(HookDenylist.isDenied("gov.irs.mobile"))
    }

    @Test
    fun allowsOrdinaryApps() {
        assertFalse(HookDenylist.isDenied("com.example.camera.demo"))
        assertFalse(HookDenylist.isDenied("com.victorious.labpreview"))
    }

    @Test
    fun gateOffByDefault() {
        assertFalse(HookGate.masterEnabled)
        assertFalse(HookGate.canHook("com.example.camera.demo"))
    }

    @Test
    fun gateRequiresOptInAndNotDenied() {
        HookGate.masterEnabled = true
        HookGate.optedInPackages = setOf("com.example.camera.demo", "com.chase.sig.android")
        assertTrue(HookGate.canHook("com.example.camera.demo"))
        assertFalse(HookGate.canHook("com.chase.sig.android")) // denylist wins
        assertFalse(HookGate.canHook("com.other.app"))
    }
}
