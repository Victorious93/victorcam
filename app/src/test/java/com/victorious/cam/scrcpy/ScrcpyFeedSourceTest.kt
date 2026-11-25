package com.victorious.cam.scrcpy

import com.victorious.cam.feed.FeedSourceFactory
import com.victorious.cam.feed.FeedSourceKind
import com.victorious.cam.pipeline.FramePipeline
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrcpyFeedSourceTest {

    @Test
    fun gateDefaultsOffAndStartFails() = runTest {
        val previous = ScrcpyGate.enabled
        try {
            ScrcpyGate.enabled = false
            val source = ScrcpyFeedSource(ScrcpyVideoSource.CAMERA)
            source.bindPipeline(FramePipeline(16, 16))
            assertFalse(source.isActive())
            assertTrue(source.statusLine().contains("Off by default"))
            assertTrue(source.start().isFailure)
            assertFalse(source.isActive())
        } finally {
            ScrcpyGate.enabled = previous
        }
    }

    @Test
    fun startSucceedsWhenEnabledButDoesNotClaimOnDeviceFrames() = runTest {
        val source = ScrcpyFeedSource(ScrcpyVideoSource.DISPLAY, gate = { true })
        source.bindPipeline(FramePipeline(16, 16))
        assertTrue(source.start().isSuccess)
        assertTrue(source.isActive())
        assertTrue(source.statusLine().contains("on-device pipeline"))
        assertTrue(source.recommendedCommand().toCommandLine().contains("--video-source=display"))
        source.stop()
        assertFalse(source.isActive())
    }

    @Test
    fun factoryExposesOptionalScrcpySources() {
        val all = FeedSourceFactory.all(context = null)
        assertEquals(7, all.size)
        assertTrue(all.any { it is ScrcpyFeedSource && it.mode == ScrcpyVideoSource.CAMERA })
        assertTrue(all.any { it is ScrcpyFeedSource && it.mode == ScrcpyVideoSource.DISPLAY })
        val camera = FeedSourceFactory.create(FeedSourceKind.SCRCPY_CAMERA)
        val screen = FeedSourceFactory.create(FeedSourceKind.SCRCPY_SCREEN_MIRROR)
        assertTrue(camera is ScrcpyFeedSource && camera.kind == FeedSourceKind.SCRCPY_CAMERA)
        assertTrue(screen is ScrcpyFeedSource && screen.kind == FeedSourceKind.SCRCPY_SCREEN_MIRROR)
        assertEquals("scrcpy_camera", camera.id)
        assertEquals("scrcpy_screen_mirror", screen.id)
    }
}
