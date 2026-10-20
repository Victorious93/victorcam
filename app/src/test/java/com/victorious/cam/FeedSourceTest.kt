package com.victorious.cam

import com.victorious.cam.feed.FeedSourceFactory
import com.victorious.cam.feed.FeedSourceKind
import com.victorious.cam.feed.ImageFeedSource
import com.victorious.cam.feed.LoopingClipFeedSource
import com.victorious.cam.feed.ScreenRegionFeedSource
import com.victorious.cam.feed.VideoFileFeedSource
import com.victorious.cam.network.NetworkStreamFeedSource
import com.victorious.cam.pipeline.FramePipeline
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedSourceTest {

    @Test
    fun factoryCreatesFiveSources() {
        val all = FeedSourceFactory.all(context = null)
        assertEquals(5, all.size)
        assertEquals(
            setOf("video_file", "image", "looping_clip", "screen_region", "network_stream"),
            all.map { it.id }.toSet()
        )
    }

    @Test
    fun videoFileRequiresUri() = runTest {
        val source = VideoFileFeedSource()
        source.bindPipeline(FramePipeline(16, 16))
        assertTrue(source.start().isFailure)
        source.setUri("content://demo/v.mp4")
        assertTrue(source.start().isSuccess)
        assertTrue(source.isActive())
        source.stop()
        assertFalse(source.isActive())
    }

    @Test
    fun imageFeedRequiresUri() = runTest {
        val source = ImageFeedSource()
        source.bindPipeline(FramePipeline(16, 16))
        assertTrue(source.start().isFailure)
        source.setUri("content://demo/i.jpg")
        assertTrue(source.start().isSuccess)
        source.stop()
    }

    @Test
    fun loopingClipRejectsNonPositiveLoopCount() {
        val source = LoopingClipFeedSource()
        try {
            source.setLoopCount(0)
            org.junit.Assert.fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
        source.setLoopCount(3)
        assertEquals(3, source.getLoopCount())
    }

    @Test
    fun screenRegionRequiresPositiveSize() = runTest {
        val source = ScreenRegionFeedSource()
        source.bindPipeline(FramePipeline(16, 16))
        assertTrue(source.start().isFailure)
        source.setRegion(10, 20, 640, 480)
        assertEquals("640x480 @ (10,20)", source.regionSummary())
        assertTrue(source.start().isSuccess)
        source.stop()
    }

    @Test
    fun networkRequiresUrl() = runTest {
        val source = NetworkStreamFeedSource()
        source.bindPipeline(FramePipeline(16, 16))
        assertTrue(source.start().isFailure)
        source.setUrl("rtsp://demo/stream")
        assertTrue(source.start().isSuccess)
        source.stop()
    }

    @Test
    fun createByKind() {
        assertTrue(FeedSourceFactory.create(FeedSourceKind.VIDEO_FILE) is VideoFileFeedSource)
        assertTrue(FeedSourceFactory.create(FeedSourceKind.IMAGE) is ImageFeedSource)
        assertTrue(FeedSourceFactory.create(FeedSourceKind.LOOPING_CLIP) is LoopingClipFeedSource)
        assertTrue(FeedSourceFactory.create(FeedSourceKind.SCREEN_REGION) is ScreenRegionFeedSource)
        assertTrue(FeedSourceFactory.create(FeedSourceKind.NETWORK_STREAM) is NetworkStreamFeedSource)
    }
}
