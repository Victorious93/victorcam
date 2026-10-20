package com.victorious.cam.feed

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import com.victorious.cam.pipeline.FramePipeline
import com.victorious.cam.screen.ScreenCaptureController

/**
 * Screen-region capture via MediaProjection → VirtualDisplay → [FramePipeline].
 * Requires user consent Intent result before [start] succeeds with live capture;
 * without projection, pushes a labeled placeholder for the configured crop rect.
 */
class ScreenRegionFeedSource(
    private val context: Context? = null,
    private var left: Int = 0,
    private var top: Int = 0,
    private var width: Int = 0,
    private var height: Int = 0
) : FeedSource {
    override val id: String = FeedSourceKind.SCREEN_REGION.toDefaultId()
    override val displayName: String = "Screen region"
    override val description: String =
        "Capture a selected screen rectangle as the virtual camera feed."

    private var pipeline: FramePipeline? = null
    private var controller: ScreenCaptureController? = null
    private var active = false
    private var projectionData: Intent? = null
    private var projectionResultCode: Int = 0

    fun setRegion(l: Int, t: Int, w: Int, h: Int) {
        require(w > 0 && h > 0) { "Region size must be positive" }
        left = l; top = t; width = w; height = h
    }

    fun regionSummary(): String = "${width}x${height} @ ($left,$top)"
    fun cropRect(): IntArray = intArrayOf(left, top, width, height)

    fun setProjectionResult(resultCode: Int, data: Intent?) {
        projectionResultCode = resultCode
        projectionData = data
    }

    override fun bindPipeline(pipeline: FramePipeline) {
        this.pipeline = pipeline
        val ctx = context
        if (ctx != null) {
            controller = ScreenCaptureController(ctx, pipeline)
        }
    }

    override suspend fun start(): Result<Unit> {
        if (width <= 0 || height <= 0) {
            return Result.failure(IllegalStateException("Screen region not configured"))
        }
        val pipe = pipeline ?: return Result.failure(IllegalStateException("Pipeline not bound"))
        val ctrl = controller
        val data = projectionData
        if (ctrl != null && data != null) {
            pipe.start()
            ctrl.start(projectionResultCode, data, left, top, width, height)
        } else if (ctrl != null) {
            pipe.start()
            pipe.pushPlaceholder("Screen ${regionSummary()} (awaiting consent)")
        }
        // null controller = JVM unit-test path
        active = true
        return Result.success(Unit)
    }

    override suspend fun stop() {
        controller?.stop()
        active = false
    }

    override fun isActive(): Boolean = active
    override fun statusLine(): String =
        if (active) {
            if (projectionData != null) "Capturing region ${regionSummary()}"
            else "Region ${regionSummary()} — consent needed for live capture"
        } else "Idle — select a screen region"
}
