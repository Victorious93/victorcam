package com.victorious.cam.record

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.victorious.cam.pipeline.FramePipeline
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Records the composed feed (bitmaps from [FramePipeline]) to a local MP4
 * via MediaCodec + MediaMuxer (software path suitable for overlay-composited frames).
 */
class FeedRecorder(private val context: Context) {
    private var muxer: MediaMuxer? = null
    private var codec: MediaCodec? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private val recording = AtomicBoolean(false)
    private var outputFile: File? = null
    private var frameIndex = 0L
    private var width = FramePipeline.DEFAULT_WIDTH
    private var height = FramePipeline.DEFAULT_HEIGHT

    fun isRecording(): Boolean = recording.get()
    fun output(): File? = outputFile

    fun start(output: File = defaultOutput(), width: Int = this.width, height: Int = this.height) {
        stop()
        this.width = width
        this.height = height
        output.parentFile?.mkdirs()
        outputFile = output
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface: Surface = c.createInputSurface()
        c.start()
        codec = c
        muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Keep surface for draw — stored on codec input
        inputSurfaceHolder = inputSurface
        recording.set(true)
        frameIndex = 0
        drainEncoder(false)
    }

    @Volatile private var inputSurfaceHolder: Surface? = null

    fun pushFrame(bitmap: Bitmap) {
        if (!recording.get()) return
        val surface = inputSurfaceHolder ?: return
        val canvas = surface.lockCanvas(null)
        try {
            val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            if (scaled !== bitmap && !scaled.isRecycled) scaled.recycle()
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
        frameIndex++
        drainEncoder(false)
    }

    fun stop(): File? {
        if (!recording.getAndSet(false)) return outputFile
        try {
            codec?.signalEndOfInputStream()
            drainEncoder(true)
        } catch (_: Exception) {
        }
        try {
            if (muxerStarted) muxer?.stop()
        } catch (_: Exception) {
        }
        muxer?.release()
        muxer = null
        codec?.stop()
        codec?.release()
        codec = null
        inputSurfaceHolder?.release()
        inputSurfaceHolder = null
        muxerStarted = false
        trackIndex = -1
        return outputFile
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val c = codec ?: return
        val m = muxer ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outIndex = c.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) throw RuntimeException("format changed twice")
                    trackIndex = m.addTrack(c.outputFormat)
                    m.start()
                    muxerStarted = true
                }
                outIndex >= 0 -> {
                    val encoded = c.getOutputBuffer(outIndex) ?: continue
                    if (bufferInfo.size > 0 && muxerStarted) {
                        encoded.position(bufferInfo.offset)
                        encoded.limit(bufferInfo.offset + bufferInfo.size)
                        m.writeSampleData(trackIndex, encoded, bufferInfo)
                    }
                    c.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
                else -> break
            }
            if (!endOfStream) break
        }
    }

    private fun defaultOutput(): File {
        val dir = File(context.filesDir, "recordings").also { it.mkdirs() }
        return File(dir, "feed_${System.currentTimeMillis()}.mp4")
    }
}
