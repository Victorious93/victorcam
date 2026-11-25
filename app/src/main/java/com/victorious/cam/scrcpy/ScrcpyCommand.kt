package com.victorious.cam.scrcpy

/**
 * Builds a scrcpy 2.x command line for the optional Linux PC helper path.
 *
 * Camera capture uses `--video-source=camera` (Android 12+). Screen-mirror uses
 * `--video-source=display` (scrcpy default). Frames are written to a
 * v4l2loopback device with `--v4l2-sink`. scrcpy itself is not bundled.
 *
 * @see <a href="https://github.com/Genymobile/scrcpy">Genymobile/scrcpy</a> (Apache-2.0)
 */
enum class ScrcpyVideoSource(val cliValue: String) {
    CAMERA("camera"),
    DISPLAY("display")
}

data class ScrcpyCommandOptions(
    val binary: String = "scrcpy",
    val videoSource: ScrcpyVideoSource = ScrcpyVideoSource.CAMERA,
    val v4l2Sink: String = DEFAULT_V4L2_SINK,
    val noVideoPlayback: Boolean = true,
    val noAudio: Boolean = true,
    val cameraFacing: String? = null,
    val cameraId: String? = null,
    val cameraSize: String? = null,
    val serial: String? = null,
    val listCameras: Boolean = false
) {
    /**
     * Validate and return argv (including the binary). Throws
     * [IllegalArgumentException] when options conflict or are malformed.
     */
    fun toArgs(): List<String> {
        require(binary.isNotBlank()) { "scrcpy binary name is blank" }
        if (listCameras) {
            require(videoSource == ScrcpyVideoSource.CAMERA || videoSource == ScrcpyVideoSource.DISPLAY) {
                "unknown video source"
            }
            return listOf(binary) + serialArgs() + listOf("--list-cameras")
        }
        require(V4L2_SINK_PATTERN.matches(v4l2Sink)) {
            "v4l2 sink must look like /dev/videoN, got: $v4l2Sink"
        }
        val facing = cameraFacing?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val id = cameraId?.trim()?.takeIf { it.isNotEmpty() }
        val size = cameraSize?.trim()?.takeIf { it.isNotEmpty() }
        if (facing != null) {
            require(facing in ALLOWED_FACING) {
                "camera facing must be one of ${ALLOWED_FACING.joinToString()}, got: $facing"
            }
            require(videoSource == ScrcpyVideoSource.CAMERA) {
                "--camera-facing is only valid with --video-source=camera"
            }
        }
        if (id != null) {
            require(videoSource == ScrcpyVideoSource.CAMERA) {
                "--camera-id is only valid with --video-source=camera"
            }
            require(facing == null) {
                "--camera-id and --camera-facing cannot be combined"
            }
        }
        if (size != null) {
            require(CAMERA_SIZE_PATTERN.matches(size)) {
                "camera size must look like WIDTHxHEIGHT, got: $size"
            }
            require(videoSource == ScrcpyVideoSource.CAMERA) {
                "--camera-size is only valid with --video-source=camera"
            }
        }

        val args = mutableListOf(
            binary,
            *serialArgs().toTypedArray(),
            "--video-source=${videoSource.cliValue}",
            "--v4l2-sink=$v4l2Sink"
        )
        if (noVideoPlayback) args += "--no-video-playback"
        if (noAudio) args += "--no-audio"
        if (facing != null) args += "--camera-facing=$facing"
        if (id != null) args += "--camera-id=$id"
        if (size != null) args += "--camera-size=$size"
        return args
    }

    fun toCommandLine(): String = toArgs().joinToString(" ")

    private fun serialArgs(): List<String> {
        val s = serial?.trim().orEmpty()
        return if (s.isEmpty()) emptyList() else listOf("--serial=$s")
    }

    companion object {
        const val DEFAULT_V4L2_SINK = "/dev/video10"
        val ALLOWED_FACING = setOf("front", "back", "external")
        private val V4L2_SINK_PATTERN = Regex("""^/dev/video\d+$""")
        private val CAMERA_SIZE_PATTERN = Regex("""^\d+x\d+$""")
    }
}

object ScrcpyVersion {
    const val MIN_MAJOR = 2

    data class Parsed(val major: Int, val minor: Int, val patch: Int = 0) {
        fun isAtLeast(major: Int, minor: Int = 0, patch: Int = 0): Boolean {
            if (this.major != major) return this.major > major
            if (this.minor != minor) return this.minor > minor
            return this.patch >= patch
        }
    }

    /**
     * Parse `scrcpy --version` text such as `scrcpy 2.7` or
     * `scrcpy 2.4.0 <https://github.com/Genymobile/scrcpy>`.
     */
    fun parse(versionOutput: String): Parsed? {
        val match = VERSION_PATTERN.find(versionOutput) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: 0
        val patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: 0
        return Parsed(major, minor, patch)
    }

    fun isSupported(versionOutput: String): Boolean {
        val parsed = parse(versionOutput) ?: return false
        return parsed.isAtLeast(MIN_MAJOR)
    }

    private val VERSION_PATTERN = Regex(
        """(?:scrcpy\s+)?(\d+)\.(\d+)(?:\.(\d+))?""",
        RegexOption.IGNORE_CASE
    )
}
