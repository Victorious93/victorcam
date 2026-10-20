package com.victorious.cam.feed

import android.content.Context
import android.net.Uri
import com.victorious.cam.pipeline.FeedPlayer
import com.victorious.cam.pipeline.FramePipeline

/** Still image decoded to bitmap and pushed into [FramePipeline]. */
class ImageFeedSource(
    private val context: Context? = null,
    private var uri: String? = null
) : FeedSource {
    override val id: String = FeedSourceKind.IMAGE.toDefaultId()
    override val displayName: String = "Still image"
    override val description: String =
        "Present a still image as a frozen virtual camera frame."

    private var pipeline: FramePipeline? = null
    private var player: FeedPlayer? = null
    private var active = false

    fun setUri(value: String?) { uri = value }
    fun getUri(): String? = uri

    override fun bindPipeline(pipeline: FramePipeline) {
        this.pipeline = pipeline
        val ctx = context
        if (ctx != null) player = FeedPlayer(ctx, pipeline)
    }

    override suspend fun start(): Result<Unit> {
        val u = uri
        if (u.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No image URI selected"))
        }
        val pipe = pipeline ?: return Result.failure(IllegalStateException("Pipeline not bound"))
        val p = player
        if (p != null) {
            pipe.start()
            p.playBitmapFromUri(Uri.parse(u))
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
        if (active) "Showing image: ${uri ?: "(none)"}" else "Idle — pick an image"
}
