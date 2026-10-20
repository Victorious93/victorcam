package com.victorious.cam.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

class PipelineDefaultsTest {
    @Test
    fun defaultSize() {
        assertEquals(1280, FramePipeline.DEFAULT_WIDTH)
        assertEquals(720, FramePipeline.DEFAULT_HEIGHT)
    }
}
