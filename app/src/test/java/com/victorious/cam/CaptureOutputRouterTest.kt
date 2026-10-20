package com.victorious.cam

import com.victorious.cam.capture.CaptureOutputRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for capture output routing without Android Uri / Robolectric.
 * Uses string-based helpers that mirror the Uri API used by CaptureHandlerActivity.
 */
class CaptureOutputRouterTest {

    @Test
    fun withExtraOutput_writesToUri() {
        val plan = CaptureOutputRouter.planForTest(
            action = "android.media.action.IMAGE_CAPTURE",
            extraOutputPresent = true
        )
        assertEquals(CaptureOutputRouter.Mode.WRITE_TO_EXTRA_OUTPUT, plan.mode)
        assertFalse(plan.isVideo)
    }

    @Test
    fun withoutExtraOutput_returnsInResult() {
        val plan = CaptureOutputRouter.planForTest(
            action = "android.media.action.IMAGE_CAPTURE",
            extraOutputPresent = false
        )
        assertEquals(CaptureOutputRouter.Mode.RETURN_IN_RESULT, plan.mode)
        assertNull(plan.extraOutput)
    }

    @Test
    fun videoCapture_flagsVideo() {
        val plan = CaptureOutputRouter.planForTest(
            action = "android.media.action.VIDEO_CAPTURE",
            extraOutputPresent = false
        )
        assertTrue(plan.isVideo)
        assertEquals("video/*", CaptureOutputRouter.acceptedMimeTypes(true))
        assertEquals("image/*", CaptureOutputRouter.acceptedMimeTypes(false))
    }

    @Test
    fun stillImageCamera_isImageAction() {
        assertTrue(CaptureOutputRouter.isImageAction("android.media.action.STILL_IMAGE_CAMERA"))
        assertTrue(CaptureOutputRouter.isImageAction("android.media.action.IMAGE_CAPTURE"))
        assertFalse(CaptureOutputRouter.isImageAction("android.media.action.VIDEO_CAPTURE"))
        assertTrue(CaptureOutputRouter.isVideoAction("android.media.action.VIDEO_CAPTURE"))
    }

    @Test
    fun acceptableSource_matchesType() {
        assertTrue(
            CaptureOutputRouter.isAcceptableSourcePath(
                "content://demo/photo.jpg",
                isVideo = false
            )
        )
        assertTrue(
            CaptureOutputRouter.isAcceptableSourcePath(
                "content://demo/clip.mp4",
                isVideo = true
            )
        )
        assertFalse(
            CaptureOutputRouter.isAcceptableSourcePath(
                "content://demo/photo.jpg",
                isVideo = true
            )
        )
        assertFalse(CaptureOutputRouter.isAcceptableSourcePath(null, isVideo = false))
        assertTrue(
            CaptureOutputRouter.isAcceptableSourcePath(
                "content://demo/blob",
                isVideo = false,
                mimeHint = "image/png"
            )
        )
    }

    @Test
    fun videoWithExtraOutput_plan() {
        val plan = CaptureOutputRouter.planForTest(
            action = "android.media.action.VIDEO_CAPTURE",
            extraOutputPresent = true
        )
        assertEquals(CaptureOutputRouter.Mode.WRITE_TO_EXTRA_OUTPUT, plan.mode)
        assertTrue(plan.isVideo)
    }
}
