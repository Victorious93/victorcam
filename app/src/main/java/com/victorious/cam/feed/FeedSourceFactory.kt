package com.victorious.cam.feed

import android.content.Context
import com.victorious.cam.network.NetworkStreamFeedSource
import com.victorious.cam.pipeline.FramePipeline
import com.victorious.cam.scrcpy.ScrcpyFeedSource
import com.victorious.cam.scrcpy.ScrcpyVideoSource

object FeedSourceFactory {
    fun all(context: Context? = null): List<FeedSource> = listOf(
        VideoFileFeedSource(context),
        ImageFeedSource(context),
        LoopingClipFeedSource(context),
        ScreenRegionFeedSource(context),
        NetworkStreamFeedSource(context),
        ScrcpyFeedSource(ScrcpyVideoSource.CAMERA),
        ScrcpyFeedSource(ScrcpyVideoSource.DISPLAY)
    )

    fun create(kind: FeedSourceKind, context: Context? = null): FeedSource = when (kind) {
        FeedSourceKind.VIDEO_FILE -> VideoFileFeedSource(context)
        FeedSourceKind.IMAGE -> ImageFeedSource(context)
        FeedSourceKind.LOOPING_CLIP -> LoopingClipFeedSource(context)
        FeedSourceKind.SCREEN_REGION -> ScreenRegionFeedSource(context)
        FeedSourceKind.NETWORK_STREAM -> NetworkStreamFeedSource(context)
        FeedSourceKind.SCRCPY_CAMERA -> ScrcpyFeedSource(ScrcpyVideoSource.CAMERA)
        FeedSourceKind.SCRCPY_SCREEN_MIRROR -> ScrcpyFeedSource(ScrcpyVideoSource.DISPLAY)
        FeedSourceKind.MULTI_FEED_MIX ->
            error("MULTI_FEED_MIX is produced by MultiFeedMixer, not FeedSourceFactory")
    }

    fun bindAll(sources: List<FeedSource>, pipeline: FramePipeline) {
        sources.forEach { it.bindPipeline(pipeline) }
    }
}
