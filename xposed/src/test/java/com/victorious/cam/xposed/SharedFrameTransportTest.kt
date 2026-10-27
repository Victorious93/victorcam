package com.victorious.cam.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SharedFrameTransportTest {
    @Before
    fun reset() {
        SharedFrameTransport.reset()
        FeedFrameBuffer.clear()
        HookGate.resetToSafeDefaults()
    }

    @Test
    fun publishSnapshotRoundTrip() {
        val frame = FeedFrame(2, 1, FeedFrame.Format.RGBA_8888, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        assertTrue(SharedFrameTransport.publish(frame))
        assertTrue(SharedFrameTransport.hasFrame())
        val out = SharedFrameTransport.snapshot()
        assertNotNull(out)
        assertEquals(2, out!!.width)
        assertEquals(1, out.height)
        assertTrue(out.bytes.contentEquals(frame.bytes))
        assertEquals(1L, SharedFrameTransport.latestVersion())
    }

    @Test
    fun backpressureDropsWhenPendingFull() {
        SharedFrameTransport.maxPending = 1
        val a = FeedFrame(1, 1, FeedFrame.Format.RGBA_8888, byteArrayOf(1, 0, 0, 255.toByte()))
        val b = FeedFrame(1, 1, FeedFrame.Format.RGBA_8888, byteArrayOf(2, 0, 0, 255.toByte()))
        assertTrue(SharedFrameTransport.publish(a))
        // pending==1, next publish should drop (return false) but still update latest
        assertFalse(SharedFrameTransport.publish(b))
        val stats = SharedFrameTransport.stats()
        assertEquals(1L, stats.droppedBackpressure)
        assertEquals(2.toByte(), SharedFrameTransport.snapshot()!!.bytes[0])
    }

    @Test
    fun headerMagicAndVersion() {
        val encoded = SharedFrameTransport.encodeHeader(
            version = 7, width = 8, height = 4,
            formatOrdinal = 0, payloadBytes = 128, timestampNs = 99L
        )
        assertEquals(SharedFrameTransport.HEADER_BYTES, encoded.size)
        val header = SharedFrameTransport.readHeader(
            ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN)
        )
        assertNotNull(header)
        assertEquals(SharedFrameTransport.MAGIC, header!!.magic)
        assertEquals(SharedFrameTransport.PROTOCOL_VERSION, header.protocolVersion)
        assertEquals(7L, header.frameVersion)
        assertEquals(8, header.width)
        assertEquals(99L, header.timestampNs)
    }

    @Test
    fun stillGatedByHookGate() {
        SharedFrameTransport.publish(
            FeedFrame(1, 1, FeedFrame.Format.RGBA_8888, byteArrayOf(1, 2, 3, 4))
        )
        val blocked = FrameSubstitutionEngine.shouldSubstitute("com.example.cam", true)
        assertFalse(blocked.substituted)
        HookGate.masterEnabled = true
        HookGate.optedInPackages = setOf("com.example.cam")
        assertTrue(FrameSubstitutionEngine.shouldSubstitute("com.example.cam", true).substituted)
        HookGate.optedInPackages = setOf("com.chase.sig.android")
        assertFalse(FrameSubstitutionEngine.shouldSubstitute("com.chase.sig.android", true).substituted)
    }
}
