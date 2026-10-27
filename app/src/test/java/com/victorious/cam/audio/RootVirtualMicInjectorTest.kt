package com.victorious.cam.audio

import com.victorious.cam.xposed.HookGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RootVirtualMicInjectorTest {
    private lateinit var gate: FakeGate
    private lateinit var injector: RootVirtualMicInjector

    @Before
    fun setUp() {
        HookGate.resetToSafeDefaults()
        gate = FakeGate()
        injector = RootVirtualMicInjector(gate)
    }

    @Test
    fun blockedWhenMasterOff() {
        val d = injector.evaluate("com.example.cam")
        assertFalse(d.allowed)
        assertEquals("master_gate_off", d.reason)
        assertEquals(VirtualMicCapability.Level.PREVIEW_ONLY, d.level)
    }

    @Test
    fun denylistBlocksBankingEvenWithRoot() {
        gate.masterEnabled = true
        gate.rootAvailable = true
        gate.optedIn = setOf("com.chase.sig.android")
        val d = injector.evaluate("com.chase.sig.android")
        assertFalse(d.allowed)
        assertTrue(d.reason.startsWith("denylist"))
    }

    @Test
    fun requiresOptInAndRoot() {
        gate.masterEnabled = true
        gate.rootAvailable = false
        gate.optedIn = setOf("com.example.cam")
        assertEquals("root_or_magisk_unavailable", injector.evaluate("com.example.cam").reason)

        gate.rootAvailable = true
        gate.optedIn = emptySet()
        assertEquals("not_opted_in", injector.evaluate("com.example.cam").reason)

        gate.optedIn = setOf("com.example.cam")
        val ok = injector.evaluate("com.example.cam")
        assertTrue(ok.allowed)
        assertEquals(VirtualMicCapability.Level.SYSTEM_MIC_REPLACE, ok.level)
    }

    @Test
    fun armAndPullPcm() {
        gate.masterEnabled = true
        gate.rootAvailable = true
        gate.optedIn = setOf("com.example.cam")
        assertTrue(injector.arm("com.example.cam") { byteArrayOf(9, 8, 7, 6) })
        assertTrue(injector.isArmedFor("com.example.cam"))
        val pcm = injector.pullPcm("com.example.cam", 2)
        assertNotNull(pcm)
        assertEquals(2, pcm!!.size)
        assertEquals(9.toByte(), pcm[0])
        injector.disarm()
        assertNull(injector.pullPcm("com.example.cam", 2))
    }

    @Test
    fun rootDetectorHeuristic() {
        assertFalse(RootDetector.isRootLikely(whichSu = { false }, magiskMarker = { false }))
        assertTrue(RootDetector.isRootLikely(whichSu = { true }, magiskMarker = { false }))
    }

    class FakeGate : HookGateAccess {
        override var masterEnabled: Boolean = false
        override var rootAvailable: Boolean = false
        var optedIn: Set<String> = emptySet()
        override fun isOptedIn(packageName: String) = packageName in optedIn
    }
}
