package com.victorious.cam.network

import android.content.Context
import android.net.Uri
import com.victorious.cam.feed.FeedSource
import com.victorious.cam.feed.FeedSourceKind
import com.victorious.cam.feed.toDefaultId
import com.victorious.cam.pipeline.FeedPlayer
import com.victorious.cam.pipeline.FramePipeline

/**
 * Network stream input (OBS-compatible URLs) into [FramePipeline] via Media3.
 *
 * - RTSP / HTTP(S) / HLS / DASH — Media3 defaults
 * - RTMP / RTMPS — Media3 [RtmpDataSource]
 * - SRT — [SrtDataSource] (MPEG-TS UDP path; see gaps in that class)
 * - NDI — proprietary SDK not bundled; [NdiRtpBridge] maps ndi:// / ndi+rtp://
 */
class NetworkStreamFeedSource(
    private val context: Context? = null,
    private var url: String? = null,
    private var ndiGateway: Map<String, String> = emptyMap()
) : FeedSource {
    override val id: String = FeedSourceKind.NETWORK_STREAM.toDefaultId()
    override val displayName: String = "Network stream"
    override val description: String =
        "RTSP / HTTP(S) / HLS / RTMP / SRT as OBS-style input."

    private var pipeline: FramePipeline? = null
    private var player: FeedPlayer? = null
    private var active = false

    fun setUrl(value: String?) { url = value }
    fun getUrl(): String? = url
    fun setNdiGateway(map: Map<String, String>) { ndiGateway = map }

    override fun bindPipeline(pipeline: FramePipeline) {
        this.pipeline = pipeline
        val ctx = context
        if (ctx != null) player = FeedPlayer(ctx, pipeline)
    }

    override suspend fun start(): Result<Unit> {
        val u = url?.trim().orEmpty()
        if (u.isEmpty()) {
            return Result.failure(IllegalStateException("No stream URL"))
        }
        var playUrl = u
        val kind = StreamUrlClassifier.classify(u)
        if (kind == StreamUrlClassifier.Kind.UNSUPPORTED) {
            return Result.failure(IllegalArgumentException("Unsupported URL scheme: $u"))
        }
        if (NdiRtpBridge.isNdiFamily(u)) {
            val resolved = NdiRtpBridge.resolve(u, ndiGateway)
                ?: return Result.failure(
                    IllegalStateException(
                        "NDI source not resolvable. Configure a gateway map or use ndi+rtp://host:port. See docs/NDI.md"
                    )
                )
            playUrl = resolved.playableUrl
        }
        val pipe = pipeline ?: return Result.failure(IllegalStateException("Pipeline not bound"))
        val p = player
        if (p != null) {
            pipe.start()
            p.playUri(Uri.parse(playUrl), loop = false)
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
        if (active) "Streaming: ${url ?: ""}" else "Idle — enter RTSP/HTTP/RTMP/SRT/NDI-RTP URL"
}
