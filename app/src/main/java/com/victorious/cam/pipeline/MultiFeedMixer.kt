package com.victorious.cam.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.victorious.cam.overlay.OverlayCompositor
import com.victorious.cam.overlay.OverlayConfig

/**
 * Composes 2+ feed bitmaps into one output frame according to [PipConfig],
 * optionally runs [OverlayCompositor], and pushes into [FramePipeline].
 *
 * Wire order: sources → mixer.compose → overlays → pipeline / recorder / publisher.
 */
class MultiFeedMixer(
    private val outputWidth: Int = FramePipeline.DEFAULT_WIDTH,
    private val outputHeight: Int = FramePipeline.DEFAULT_HEIGHT,
    private val overlayCompositor: OverlayCompositor = OverlayCompositor()
) {
    data class Layer(
        val sourceId: String,
        val bitmap: Bitmap
    )

    fun compose(
        layers: List<Layer>,
        config: PipConfig = PipConfig(),
        overlay: OverlayConfig? = null,
        logo: Bitmap? = null
    ): Bitmap {
        require(outputWidth > 0 && outputHeight > 0)
        val out = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)
        if (layers.isEmpty()) return out

        val ids = layers.map { it.sourceId }
        val byId = layers.associateBy { it.sourceId }
        val slots = config.slotsFor(ids).sortedBy { it.zIndex }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 2f
            alpha = 160
        }
        for (slot in slots) {
            val layer = byId[slot.sourceId] ?: continue
            if (layer.bitmap.isRecycled) continue
            val dst = Rect(
                (slot.left * outputWidth).toInt(),
                (slot.top * outputHeight).toInt(),
                (slot.right * outputWidth).toInt(),
                (slot.bottom * outputHeight).toInt()
            )
            if (dst.width() == outputWidth && dst.height() == outputHeight &&
                layer.bitmap.width == outputWidth && layer.bitmap.height == outputHeight
            ) {
                val pixels = IntArray(outputWidth * outputHeight)
                layer.bitmap.getPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
                out.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
            } else {
                val src = Rect(0, 0, layer.bitmap.width, layer.bitmap.height)
                // Scale via createScaledBitmap for Robolectric-safe blit
                val dw = dst.width().coerceAtLeast(1)
                val dh = dst.height().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(layer.bitmap, dw, dh, true)
                val pixels = IntArray(dw * dh)
                scaled.getPixels(pixels, 0, dw, 0, 0, dw, dh)
                out.setPixels(pixels, 0, dw, dst.left, dst.top, dw, dh)
                if (scaled !== layer.bitmap && !scaled.isRecycled) scaled.recycle()
            }
            if (slot.zIndex > 0) {
                canvas.drawRect(RectF(dst), border)
            }
        }
        if (overlay != null && overlay.anyEnabled()) {
            val composed = overlayCompositor.compose(out, overlay, logo)
            if (composed !== out) {
                out.recycle()
                return composed
            }
        }
        return out
    }

    /** Compose and push into [pipeline] (and return the frame). */
    fun composeToPipeline(
        pipeline: FramePipeline,
        layers: List<Layer>,
        config: PipConfig = PipConfig(),
        overlay: OverlayConfig? = null,
        logo: Bitmap? = null
    ): Bitmap {
        val frame = compose(layers, config, overlay, logo)
        pipeline.pushBitmap(frame)
        return frame
    }
}
