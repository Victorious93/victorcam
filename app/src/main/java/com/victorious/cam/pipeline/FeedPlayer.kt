package com.victorious.cam.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.victorious.cam.network.StreamMediaSourceFactory
import com.victorious.cam.network.StreamUrlClassifier
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Media3 ExoPlayer helper for video / looping clip / network URIs into [FramePipeline].
 */
@UnstableApi
class FeedPlayer(
    context: Context,
    private val pipeline: FramePipeline
) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private val started = AtomicBoolean(false)

    fun playUri(uri: Uri, loop: Boolean) {
        stop()
        pipeline.start()
        val surface = waitForSurface()
        val exo = ExoPlayer.Builder(appContext).build().also { p ->
            val kind = StreamUrlClassifier.classify(uri.toString())
            if (isNetworkKind(kind)) {
                p.setMediaSource(StreamMediaSourceFactory.create(appContext, uri))
            } else {
                p.setMediaItem(MediaItem.fromUri(uri))
            }
            p.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            if (surface != null) {
                p.setVideoSurface(surface)
            }
            p.prepare()
            p.playWhenReady = true
        }
        player = exo
        started.set(true)
        if (surface == null) {
            pipeline.pushPlaceholder("Video (no surface yet)")
        }
    }

    fun playBitmapFromUri(uri: Uri) {
        stop()
        pipeline.start()
        started.set(true)
        val bmp = decodeBitmap(uri)
        if (bmp != null) {
            pipeline.pushBitmap(bmp)
            bmp.recycle()
        } else {
            pipeline.pushPlaceholder("Image decode failed")
        }
    }

    fun playStillBitmap(bitmap: Bitmap) {
        stop()
        pipeline.start()
        started.set(true)
        pipeline.pushBitmap(bitmap)
    }

    fun isPlaying(): Boolean = started.get() && (player?.isPlaying == true || pipeline.latestFrame() != null)

    fun stop() {
        started.set(false)
        player?.release()
        player = null
    }

    fun exoPlayer(): ExoPlayer? = player

    private fun isNetworkKind(kind: StreamUrlClassifier.Kind): Boolean =
        kind == StreamUrlClassifier.Kind.RTMP ||
            kind == StreamUrlClassifier.Kind.RTSP ||
            kind == StreamUrlClassifier.Kind.SRT ||
            kind == StreamUrlClassifier.Kind.HTTP ||
            kind == StreamUrlClassifier.Kind.HLS ||
            kind == StreamUrlClassifier.Kind.DASH

    private fun waitForSurface(): android.view.Surface? {
        repeat(20) {
            pipeline.producerSurface?.let { return it }
            Thread.sleep(25)
        }
        return pipeline.producerSurface
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: BitmapFactory.decodeFile(uri.path)
        } catch (_: Exception) {
            null
        }
    }
}
