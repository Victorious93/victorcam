package com.victorious.cam.scrcpy

import com.victorious.cam.feed.FeedSource
import com.victorious.cam.feed.FeedSourceKind
import com.victorious.cam.feed.toDefaultId
import com.victorious.cam.pipeline.FramePipeline

/**
 * Optional listing for the PC-side scrcpy helper path.
 *
 * Starting this source does **not** push frames into [FramePipeline]. Capture
 * happens on a Linux PC via scrcpy 2.x and v4l2loopback. See docs/SCRCPY.md.
 * Off by default until [ScrcpyGate.enabled] is set from the setup screen.
 */
class ScrcpyFeedSource(
    val mode: ScrcpyVideoSource,
    private val gate: () -> Boolean = { ScrcpyGate.enabled }
) : FeedSource {
    override val id: String = kind.toDefaultId()
    override val displayName: String = when (mode) {
        ScrcpyVideoSource.CAMERA -> "Phone camera via scrcpy"
        ScrcpyVideoSource.DISPLAY -> "Phone screen via scrcpy"
    }
    override val description: String = when (mode) {
        ScrcpyVideoSource.CAMERA ->
            "Optional, off by default. Send this phone's camera to a Linux v4l2 webcam with scrcpy 2.x."
        ScrcpyVideoSource.DISPLAY ->
            "Optional, off by default. Mirror this phone's screen to a Linux v4l2 webcam with scrcpy."
    }

    val kind: FeedSourceKind
        get() = when (mode) {
            ScrcpyVideoSource.CAMERA -> FeedSourceKind.SCRCPY_CAMERA
            ScrcpyVideoSource.DISPLAY -> FeedSourceKind.SCRCPY_SCREEN_MIRROR
        }

    private var pipeline: FramePipeline? = null
    private var active = false

    fun recommendedCommand(): ScrcpyCommandOptions =
        ScrcpyCommandOptions(videoSource = mode)

    override fun bindPipeline(pipeline: FramePipeline) {
        this.pipeline = pipeline
    }

    override suspend fun start(): Result<Unit> {
        if (pipeline == null) {
            return Result.failure(IllegalStateException("Pipeline not bound"))
        }
        if (!gate()) {
            return Result.failure(
                IllegalStateException(
                    "scrcpy path is OFF by default. Enable it in Phone camera via scrcpy setup, " +
                        "then run scripts/victorcam-scrcpy.sh on a Linux PC. " +
                        "This source does not feed the on-device virtual camera pipeline."
                )
            )
        }
        // Intentionally no FramePipeline.start() / frame publish: capture is PC-side.
        active = true
        return Result.success(Unit)
    }

    override suspend fun stop() {
        active = false
    }

    override fun isActive(): Boolean = active

    override fun statusLine(): String = when {
        active ->
            "Acknowledged — run ${recommendedCommand().toCommandLine()} on a Linux PC. " +
                "No frames are injected into the on-device pipeline."
        gate() ->
            "Enabled, idle — run scripts/victorcam-scrcpy.sh on a Linux PC (see docs/SCRCPY.md)."
        else ->
            "Off by default — open Phone camera via scrcpy setup to enable this optional path."
    }
}
