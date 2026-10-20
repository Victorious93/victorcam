package com.victorious.cam.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared Surface / SurfaceTexture pipeline for all feed sources.
 *
 * Producers render into [producerSurface] (ExoPlayer) or push bitmaps via [pushBitmap].
 * Consumers (Compose preview, overlay compositor, future Camera2 inject) read the latest frame.
 */
class FramePipeline(
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT
) {
    private val running = AtomicBoolean(false)
    private val latestBitmap = AtomicReference<Bitmap?>(null)
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    private var surfaceTexture: SurfaceTexture? = null
    private var _producerSurface: Surface? = null

    /** Surface for ExoPlayer / MediaProjection VirtualDisplay. */
    val producerSurface: Surface?
        get() = _producerSurface

    fun start() {
        if (!running.compareAndSet(false, true)) return
        val t = HandlerThread("VictorCam-FramePipeline").also { it.start() }
        thread = t
        handler = Handler(t.looper)
        handler?.post {
            try {
                val st = SurfaceTexture(0).also {
                    it.setDefaultBufferSize(width, height)
                }
                surfaceTexture = st
                _producerSurface = Surface(st)
            } catch (_: Throwable) {
                // Emulators / unit tests may lack a GL producer — bitmap path still works
            }
            try {
                val blank = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                blank.eraseColor(Color.BLACK)
                pushBitmap(blank)
                blank.recycle()
            } catch (_: Throwable) {
                // ignore on stubbed Bitmap
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        handler?.post {
            _producerSurface?.release()
            _producerSurface = null
            surfaceTexture?.release()
            surfaceTexture = null
        }
        thread?.quitSafely()
        thread = null
        handler = null
        latestBitmap.getAndSet(null)?.takeIf { !it.isRecycled }?.recycle()
    }

    fun isRunning(): Boolean = running.get()

    /** Push a software-composited / still-image frame for preview and overlay. */
    fun pushBitmap(bitmap: Bitmap) {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val previous = latestBitmap.getAndSet(copy)
        if (previous != null && previous !== copy && !previous.isRecycled) {
            previous.recycle()
        }
    }

    /** Snapshot of the latest frame for Compose preview / overlay input. */
    fun latestFrame(): Bitmap? = latestBitmap.get()?.let {
        if (it.isRecycled) null else it.copy(Bitmap.Config.ARGB_8888, false)
    }

    /** Draw a labeled placeholder when a source has no media yet. */
    fun pushPlaceholder(label: String) {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(13, 17, 23))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 194, 168)
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, width / 2f, height / 2f, paint)
        pushBitmap(bmp)
        if (!bmp.isRecycled) bmp.recycle()
    }

    companion object {
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 720
    }
}
