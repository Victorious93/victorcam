package com.victorious.cam.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtOptionsTest {
    @Test
    fun parsesCallerDefaults() {
        val o = SrtOptions.parse("srt://10.0.0.5:9000")
        assertNotNull(o)
        assertEquals("10.0.0.5", o!!.host)
        assertEquals(9000, o.port)
        assertEquals(SrtOptions.Mode.CALLER, o.mode)
        assertFalse(o.requiresNativeLibsrt())
    }

    @Test
    fun parsesQueryOptions() {
        val o = SrtOptions.parse(
            "srt://live.example:9001?mode=rendezvous&latency=200&passphrase=sekrit&streamid=live/a"
        )!!
        assertEquals(SrtOptions.Mode.RENDEZVOUS, o.mode)
        assertEquals(200, o.latencyMs)
        assertEquals("sekrit", o.passphrase)
        assertEquals("live/a", o.streamId)
        assertTrue(o.requiresNativeLibsrt())
    }

    @Test
    fun rejectsNonSrt() {
        assertNull(SrtOptions.parse("rtmp://x/app"))
    }

    @Test
    fun capabilityLabelsHonest() {
        assertEquals(SrtCapability.Level.TS_OVER_UDP, SrtCapability.shippedLevel())
        assertTrue(SrtCapability.certaintyLabel().contains("Medium"))
        assertTrue(SrtCapability.summary().contains("libsrt"))
    }

    @Test
    fun parseEndpointDelegates() {
        assertEquals("a" to 9000, SrtDataSource.parseEndpoint("srt://a"))
        assertNull(SrtDataSource.parseEndpoint("http://a"))
    }
}
