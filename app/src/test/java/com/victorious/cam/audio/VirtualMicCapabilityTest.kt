package com.victorious.cam.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualMicCapabilityTest {
    @Test
    fun stockIsPreviewOnly() {
        assertEquals(VirtualMicCapability.Level.PREVIEW_ONLY, VirtualMicCapability.stockLevel())
        assertTrue(VirtualMicCapability.description(VirtualMicCapability.stockLevel()).contains("Stock"))
    }

    @Test
    fun rootRaisesToSystemMic() {
        assertEquals(
            VirtualMicCapability.Level.SYSTEM_MIC_REPLACE,
            VirtualMicCapability.withRootHook()
        )
    }
}
