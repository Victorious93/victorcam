package com.victorious.cam.feed

import android.content.Context
import android.net.Uri
import com.victorious.cam.pipeline.FeedPlayer
import com.victorious.cam.pipeline.FramePipeline

/** Video file via Media3 ExoPlayer → shared [FramePipeline]. */
class VideoFileFeedSource(
    private val context: Context? = null,
    private var uri: String? = null
) : FeedSource {
    override val id: String = FeedSourceKind.VIDEO_FILE.toDefaultId()
    override val displayName: String = "Video file"
    override val description: String =
        "Play a video file as the virtual camera feed (user-owned content)."

    private var pipeline: FramePipeline? = null
    private var player: FeedPlayer? = null
    private var active = false

    fun setUri(value: String?) { uri = value }
    fun getUri(): String? = uri

    override fun bindPipeline(pipeline: FramePipeline) {
        this.pipeline = pipeline
        val ctx = context
        if (ctx != null) {
            player = FeedPlayer(ctx, pipeline)
        }
    }

    override suspend fun start(): Result<Unit> {
        val u = uri
        if (u.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No video URI selected"))
        }
        val pipe = pipeline ?: return Result.failure(IllegalStateException("Pipeline not bound"))
        val p = player
        if (p != null) {
            pipe.start()
            p.playUri(Uri.parse(u), loop = false)
        }
        // null player = JVM unit-test path (no Android graphics)
        active = true
        return Result.success(Unit)
    }

    override suspend fun stop() {
        player?.stop()
        active = false
    }

    override fun isActive(): Boolean = active
    override fun statusLine(): String =
        if (active) "Playing video: ${uri ?: "(none)"}" else "Idle — pick a video"
}
