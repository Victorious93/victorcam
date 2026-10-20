package com.victorious.cam.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FrameSubstitutionEngineTest {
    @Before
    fun reset() {
        HookGate.resetToSafeDefaults()
        FeedFrameBuffer.clear()
    }

    @Test
    fun gateBlocksSubstitution() {
        val r = FrameSubstitutionEngine.shouldSubstitute("com.example.cam", hasFrame = true)
        assertFalse(r.substituted)
        assertEquals("gate_closed_or_denied", r.reason)
    }

    @Test
    fun denylistBlocksEvenWhenEnabled() {
        HookGate.masterEnabled = true
        HookGate.optedInPackages = setOf("com.chase.sig.android")
        val r = FrameSubstitutionEngine.shouldSubstitute("com.chase.sig.android", true)
        assertFalse(r.substituted)
    }

    @Test
    fun optedInAllowsWhenFramePresent() {
        HookGate.masterEnabled = true
        HookGate.optedInPackages = setOf("com.example.cam")
        assertFalse(FrameSubstitutionEngine.shouldSubstitute("com.example.cam", false).substituted)
        val ok = FrameSubstitutionEngine.shouldSubstitute("com.example.cam", true)
        assertTrue(ok.substituted)
    }

    @Test
    fun materializeScalesRgba() {
        val src = ByteArray(2 * 2 * 4) { 0x7F.toByte() }
        val frame = FeedFrame(2, 2, FeedFrame.Format.RGBA_8888, src)
        val out = FrameSubstitutionEngine.materializeRgba(frame, 4, 4)
        assertEquals(4 * 4 * 4, out.size)
    }

    @Test
    fun writeIntoPlanesRespectsRowStride() {
        val rgba = ByteArray(8 * 2 * 4) { 1 }
        val planes = FrameSubstitutionEngine.writeIntoPlanes(rgba, width = 8, rowStride = 40)
        assertEquals(1, planes.size)
        assertEquals(40, planes[0].rowStride)
        assertTrue(planes[0].bytes.size >= 40 * 2)
    }

    @Test
    fun feedFrameBufferPublishSnapshot() {
        val frame = FeedFrame(1, 1, FeedFrame.Format.RGBA_8888, byteArrayOf(1, 2, 3, 4))
        FeedFrameBuffer.publish(frame)
        assertTrue(FeedFrameBuffer.hasFrame())
        assertEquals(frame, FeedFrameBuffer.snapshot())
        FeedFrameBuffer.clear()
        assertFalse(FeedFrameBuffer.hasFrame())
    }
}
