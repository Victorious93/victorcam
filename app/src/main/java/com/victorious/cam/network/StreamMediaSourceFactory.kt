package com.victorious.cam.network

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource

/**
 * Builds Media3 [MediaSource]s for OBS-style ingest URLs.
 *
 * - RTMP/RTMPS: official Media3 [RtmpDataSource]
 * - RTSP: [RtspMediaSource]
 * - HTTP/HLS/DASH: [DefaultMediaSourceFactory]
 * - SRT: [SrtDataSource] (MPEG-TS UDP path; see class docs for handshake/encryption gaps)
 */
@UnstableApi
object StreamMediaSourceFactory {

    fun create(context: Context, uri: Uri): MediaSource {
        val url = uri.toString()
        return when (StreamUrlClassifier.classify(url)) {
            StreamUrlClassifier.Kind.RTMP -> rtmpSource(uri)
            StreamUrlClassifier.Kind.RTSP ->
                RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(uri))
            StreamUrlClassifier.Kind.SRT -> srtSource(uri)
            StreamUrlClassifier.Kind.NDI_RTP -> rtpSource(uri)
            StreamUrlClassifier.Kind.HTTP,
            StreamUrlClassifier.Kind.HLS,
            StreamUrlClassifier.Kind.DASH ->
                DefaultMediaSourceFactory(context).createMediaSource(MediaItem.fromUri(uri))
            StreamUrlClassifier.Kind.NDI,
            StreamUrlClassifier.Kind.UNSUPPORTED ->
                throw IllegalArgumentException(
                    "Unsupported stream: $url (native NDI SDK not bundled — use ndi+rtp:// or a gateway; see docs/NDI.md)"
                )
        }
    }

    fun supportsFully(kind: StreamUrlClassifier.Kind): Boolean =
        StreamUrlClassifier.isPlayable(kind)

    private fun rtmpSource(uri: Uri): MediaSource {
        val dataSourceFactory = RtmpDataSource.Factory()
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
    }

    private fun srtSource(uri: Uri): MediaSource {
        return ProgressiveMediaSource.Factory(SrtDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(uri))
    }

    /** RTP / NDI-over-RTP: reuse TS-over-UDP DataSource with rtp host/port. */
    private fun rtpSource(uri: Uri): MediaSource {
        val host = uri.host ?: throw IllegalArgumentException("RTP URL missing host")
        val port = if (uri.port > 0) uri.port else 5004
        val srtStyle = android.net.Uri.parse("srt://$host:$port")
        return ProgressiveMediaSource.Factory(SrtDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(srtStyle))
    }
}
