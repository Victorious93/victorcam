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
            val dst = RectF(
                slot.left * outputWidth,
                slot.top * outputHeight,
                slot.right * outputWidth,
                slot.bottom * outputHeight
            )
            val src = Rect(0, 0, layer.bitmap.width, layer.bitmap.height)
            canvas.drawBitmap(layer.bitmap, src, dst, null)
            if (slot.zIndex > 0) canvas.drawRect(dst, border)
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
