package com.victorious.cam.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayConfigTest {
    @Test
    fun anyEnabled_flags() {
        assertFalse(OverlayConfig().anyEnabled())
        assertTrue(OverlayConfig(textEnabled = true, text = "x").anyEnabled())
        assertTrue(OverlayConfig(logoEnabled = true).anyEnabled())
        assertTrue(OverlayConfig(timestampEnabled = true).anyEnabled())
    }

    @Test
    fun positionsExist() {
        assertTrue(OverlayPosition.entries.size >= 5)
    }
}
