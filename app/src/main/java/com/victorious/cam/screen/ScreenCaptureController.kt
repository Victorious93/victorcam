package com.victorious.cam.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import com.victorious.cam.pipeline.FramePipeline

/**
 * Owns MediaProjection + VirtualDisplay + ImageReader crop into [FramePipeline].
 */
class ScreenCaptureController(
    context: Context,
    private val pipeline: FramePipeline
) {
    private val appContext = context.applicationContext
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var cropLeft = 0
    private var cropTop = 0
    private var cropW = pipeline.width
    private var cropH = pipeline.height

    fun start(resultCode: Int, data: Intent, left: Int, top: Int, w: Int, h: Int) {
        stop()
        cropLeft = left.coerceAtLeast(0)
        cropTop = top.coerceAtLeast(0)
        cropW = w.coerceAtLeast(1)
        cropH = h.coerceAtLeast(1)

        val mpm = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mpm.getMediaProjection(resultCode, data)
            ?: run {
                pipeline.pushPlaceholder("MediaProjection denied")
                return
            }
        projection = proj

        val t = HandlerThread("VictorCam-ScreenCapture").also { it.start() }
        thread = t
        handler = Handler(t.looper)

        val reader = ImageReader.newInstance(
            pipeline.width, pipeline.height, PixelFormat.RGBA_8888, 2
        )
        imageReader = reader
        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buf = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * image.width
                val full = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
                full.copyPixelsFromBuffer(buf)
                val safeW = cropW.coerceAtMost(full.width - cropLeft).coerceAtLeast(1)
                val safeH = cropH.coerceAtMost(full.height - cropTop).coerceAtLeast(1)
                val cropped = Bitmap.createBitmap(full, cropLeft.coerceAtMost(full.width - 1), cropTop.coerceAtMost(full.height - 1), safeW, safeH)
                val scaled = Bitmap.createScaledBitmap(cropped, pipeline.width, pipeline.height, true)
                pipeline.pushBitmap(scaled)
                if (scaled !== cropped) cropped.recycle()
                full.recycle()
                scaled.recycle()
            } finally {
                image.close()
            }
        }, handler)

        virtualDisplay = proj.createVirtualDisplay(
            "VictorCamScreen",
            pipeline.width,
            pipeline.height,
            appContext.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
    }

    fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection?.stop()
        projection = null
        thread?.quitSafely()
        thread = null
        handler = null
    }

    companion object {
        fun createConsentIntent(context: Context): Intent {
            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            return mpm.createScreenCaptureIntent()
        }

        const val REQUEST_MEDIA_PROJECTION = 4401
    }
}
