package com.victorious.cam.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FeedTrimmerTest {
    @Test
    fun clampRange() {
        assertEquals(0L to 1000L, FeedTrimmer.clampRange(0, 5000, 1000))
        assertEquals(100L to 500L, FeedTrimmer.clampRange(100, 500, 10_000))
    }

    @Test
    fun trimRequestValidation() {
        val f = File("in.mp4")
        val out = File("out.mp4")
        val ok = FeedTrimmer.TrimRequest(f, out, 0, 1000)
        assertEquals(1000L, ok.durationMs())
        try {
            FeedTrimmer.TrimRequest(f, out, 10, 10)
            org.junit.Assert.fail()
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }
}
