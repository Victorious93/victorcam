package com.victorious.cam.feed

import android.content.Context
import android.net.Uri
import com.victorious.cam.pipeline.FeedPlayer
import com.victorious.cam.pipeline.FramePipeline

/** Looping short clip via ExoPlayer REPEAT_MODE_ONE. */
class LoopingClipFeedSource(
    private val context: Context? = null,
    private var uri: String? = null,
    private var loopCount: Int = Int.MAX_VALUE
) : FeedSource {
    override val id: String = FeedSourceKind.LOOPING_CLIP.toDefaultId()
    override val displayName: String = "Looping clip"
    override val description: String =
        "Loop a short video clip as a continuous virtual camera feed."

    private var pipeline: FramePipeline? = null
    private var player: FeedPlayer? = null
    private var active = false

    fun setUri(value: String?) { uri = value }
    fun getUri(): String? = uri
    fun setLoopCount(count: Int) {
        require(count > 0) { "loopCount must be positive" }
        loopCount = count
    }
    fun getLoopCount(): Int = loopCount

    override fun bindPipeline(pipeline: FramePipeline) {
        this.pipeline = pipeline
        val ctx = context
        if (ctx != null) player = FeedPlayer(ctx, pipeline)
    }

    override suspend fun start(): Result<Unit> {
        val u = uri
        if (u.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No clip URI selected"))
        }
        val pipe = pipeline ?: return Result.failure(IllegalStateException("Pipeline not bound"))
        val p = player
        if (p != null) {
            pipe.start()
            p.playUri(Uri.parse(u), loop = true)
        }
        active = true
        return Result.success(Unit)
    }

    override suspend fun stop() {
        player?.stop()
        active = false
    }

    override fun isActive(): Boolean = active
    override fun statusLine(): String =
        if (active) "Looping x$loopCount: ${uri ?: "(none)"}"
        else "Idle — pick a clip"
}
