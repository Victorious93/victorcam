package com.victorious.cam.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Software compositor: draws text / logo / timestamp onto a frame bitmap.
 */
class OverlayCompositor(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun compose(base: Bitmap, config: OverlayConfig, logo: Bitmap? = null): Bitmap {
        if (!config.anyEnabled()) {
            return base.copy(Bitmap.Config.ARGB_8888, false)
        }
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        if (config.textEnabled && config.text.isNotBlank()) {
            drawLabel(
                canvas, out.width, out.height,
                config.text, config.textPosition, config.textOpacity, 36f
            )
        }
        if (config.timestampEnabled) {
            val fmt = SimpleDateFormat(config.timestampPattern, Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            val stamp = fmt.format(Date(clock()))
            drawLabel(
                canvas, out.width, out.height,
                stamp, config.timestampPosition, config.timestampOpacity, 28f
            )
        }
        if (config.logoEnabled && logo != null && !logo.isRecycled) {
            drawLogo(canvas, out.width, out.height, logo, config.logoPosition, config.logoOpacity)
        }
        return out
    }

    private fun drawLabel(
        canvas: Canvas, w: Int, h: Int,
        text: String, pos: OverlayPosition, opacity: Float, textSize: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
        }
        val bg = Paint().apply {
            color = Color.BLACK
            alpha = (opacity * 140).toInt().coerceIn(0, 255)
        }
        val pad = 12f
        val tw = paint.measureText(text)
        val th = paint.fontMetrics.let { it.descent - it.ascent }
        val (x, y) = anchor(w, h, tw, th, pos, pad)
        canvas.drawRoundRect(RectF(x - pad / 2, y - th, x + tw + pad / 2, y + pad / 2), 8f, 8f, bg)
        canvas.drawText(text, x, y, paint)
    }

    private fun drawLogo(
        canvas: Canvas, w: Int, h: Int,
        logo: Bitmap, pos: OverlayPosition, opacity: Float
    ) {
        val maxW = w / 5f
        val scale = (maxW / logo.width).coerceAtMost(1f)
        val dw = logo.width * scale
        val dh = logo.height * scale
        val (x, y) = anchor(w, h, dw, dh, pos, 16f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(logo, null, RectF(x, y - dh, x + dw, y), paint)
    }

    private fun anchor(
        w: Int, h: Int, tw: Float, th: Float, pos: OverlayPosition, margin: Float
    ): Pair<Float, Float> = when (pos) {
        OverlayPosition.TOP_LEFT -> margin to (margin + th)
        OverlayPosition.TOP_RIGHT -> (w - tw - margin) to (margin + th)
        OverlayPosition.BOTTOM_LEFT -> margin to (h - margin)
        OverlayPosition.BOTTOM_RIGHT -> (w - tw - margin) to (h - margin)
        OverlayPosition.CENTER -> ((w - tw) / 2f) to ((h + th) / 2f)
    }
}
