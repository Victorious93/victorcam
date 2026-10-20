package com.victorious.cam.capture

import android.net.Uri

/**
 * Decides how a camera-intent capture should return media to the calling app.
 *
 * Pure routing logic — I/O stays in CaptureHandlerActivity — so helpers are unit-testable.
 */
object CaptureOutputRouter {

    enum class Mode {
        /** Caller provided MediaStore.EXTRA_OUTPUT — copy bytes to that Uri. */
        WRITE_TO_EXTRA_OUTPUT,
        /** No EXTRA_OUTPUT — return a content Uri (or thumbnail) in the result Intent. */
        RETURN_IN_RESULT
    }

    data class Plan(
        val mode: Mode,
        val extraOutput: Uri?,
        val isVideo: Boolean,
        val action: String
    )

    fun plan(action: String?, extraOutput: Uri?): Plan {
        val video = isVideoAction(action)
        val mode = if (extraOutput != null) Mode.WRITE_TO_EXTRA_OUTPUT else Mode.RETURN_IN_RESULT
        return Plan(
            mode = mode,
            extraOutput = extraOutput,
            isVideo = video,
            action = action.orEmpty()
        )
    }

    /**
     * Test/helper entry that avoids constructing [Uri] on the JVM unit-test classpath.
     */
    fun planForTest(action: String?, extraOutputPresent: Boolean): Plan {
        val video = isVideoAction(action)
        val mode = if (extraOutputPresent) Mode.WRITE_TO_EXTRA_OUTPUT else Mode.RETURN_IN_RESULT
        return Plan(
            mode = mode,
            extraOutput = null,
            isVideo = video,
            action = action.orEmpty()
        )
    }

    fun isVideoAction(action: String?): Boolean =
        action == "android.media.action.VIDEO_CAPTURE"

    fun isImageAction(action: String?): Boolean =
        action == "android.media.action.IMAGE_CAPTURE" ||
            action == "android.media.action.STILL_IMAGE_CAMERA"

    fun acceptedMimeTypes(isVideo: Boolean): String =
        if (isVideo) "video/*" else "image/*"

    fun isAcceptableSource(uri: Uri?, isVideo: Boolean, mimeHint: String? = null): Boolean =
        isAcceptableSourcePath(uri?.toString(), isVideo, mimeHint)

    fun isAcceptableSourcePath(
        uriString: String?,
        isVideo: Boolean,
        mimeHint: String? = null
    ): Boolean {
        if (uriString.isNullOrBlank()) return false
        val path = uriString.lowercase()
        val mime = (mimeHint ?: "").lowercase()
        return if (isVideo) {
            mime.startsWith("video/") ||
                path.endsWith(".mp4") ||
                path.endsWith(".webm") ||
                path.endsWith(".mkv") ||
                path.contains("video") ||
                path.contains("clip") ||
                path.contains("loop")
        } else {
            mime.startsWith("image/") ||
                path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".png") ||
                path.endsWith(".webp") ||
                path.contains("image") ||
                path.contains("still") ||
                path.contains("photo")
        }
    }
}
