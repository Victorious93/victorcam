package com.victorious.cam.feed

import com.victorious.cam.pipeline.FramePipeline

/**
 * Contract for an OBS-style virtual camera feed source.
 * Sources render into a shared [FramePipeline] Surface / bitmap path.
 */
interface FeedSource {
    val id: String
    val displayName: String
    val description: String

    fun bindPipeline(pipeline: FramePipeline)

    /** Prepare the source (open file, acquire projection, etc.). */
    suspend fun start(): Result<Unit>

    /** Tear down resources. */
    suspend fun stop()

    /** Whether the source is currently producing frames. */
    fun isActive(): Boolean

    /** Human-readable status for the preview UI. */
    fun statusLine(): String
}

enum class FeedSourceKind {
    VIDEO_FILE,
    IMAGE,
    LOOPING_CLIP,
    SCREEN_REGION,
    NETWORK_STREAM
}

fun FeedSourceKind.toDefaultId(): String = when (this) {
    FeedSourceKind.VIDEO_FILE -> "video_file"
    FeedSourceKind.IMAGE -> "image"
    FeedSourceKind.LOOPING_CLIP -> "looping_clip"
    FeedSourceKind.SCREEN_REGION -> "screen_region"
    FeedSourceKind.NETWORK_STREAM -> "network_stream"
}
