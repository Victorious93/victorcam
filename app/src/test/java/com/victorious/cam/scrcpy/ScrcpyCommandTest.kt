package com.victorious.cam.scrcpy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrcpyCommandTest {

    @Test
    fun cameraDefaultBuildsV4l2Command() {
        val cmd = ScrcpyCommandOptions().toArgs()
        assertEquals(
            listOf(
                "scrcpy",
                "--video-source=camera",
                "--v4l2-sink=/dev/video10",
                "--no-video-playback",
                "--no-audio"
            ),
            cmd
        )
    }

    @Test
    fun screenMirrorUsesDisplaySource() {
        val line = ScrcpyCommandOptions(videoSource = ScrcpyVideoSource.DISPLAY).toCommandLine()
        assertTrue(line.contains("--video-source=display"))
        assertTrue(line.contains("--v4l2-sink=/dev/video10"))
        assertFalse(line.contains("--camera-facing"))
    }

    @Test
    fun facingAndSizeAllowedOnCamera() {
        val cmd = ScrcpyCommandOptions(
            cameraFacing = "back",
            cameraSize = "1920x1080",
            v4l2Sink = "/dev/video2"
        ).toArgs()
        assertTrue(cmd.contains("--camera-facing=back"))
        assertTrue(cmd.contains("--camera-size=1920x1080"))
        assertTrue(cmd.contains("--v4l2-sink=/dev/video2"))
    }

    @Test
    fun cameraIdAndFacingAreExclusive() {
        try {
            ScrcpyCommandOptions(cameraId = "0", cameraFacing = "front").toArgs()
            org.junit.Assert.fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun cameraOptionsRejectedOnDisplaySource() {
        try {
            ScrcpyCommandOptions(
                videoSource = ScrcpyVideoSource.DISPLAY,
                cameraFacing = "front"
            ).toArgs()
            org.junit.Assert.fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun invalidSinkRejected() {
        try {
            ScrcpyCommandOptions(v4l2Sink = "video10").toArgs()
            org.junit.Assert.fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun listCamerasOmitsV4l2() {
        val cmd = ScrcpyCommandOptions(listCameras = true, serial = "ABC123").toArgs()
        assertEquals(listOf("scrcpy", "--serial=ABC123", "--list-cameras"), cmd)
    }
}

class ScrcpyVersionTest {

    @Test
    fun acceptsScrcpy2() {
        assertTrue(ScrcpyVersion.isSupported("scrcpy 2.7"))
        assertTrue(ScrcpyVersion.isSupported("scrcpy 2.4.0 <https://github.com/Genymobile/scrcpy>"))
        assertTrue(ScrcpyVersion.isSupported("3.0"))
        val parsed = ScrcpyVersion.parse("scrcpy 2.4.0")
        assertEquals(2, parsed?.major)
        assertEquals(4, parsed?.minor)
        assertEquals(0, parsed?.patch)
    }

    @Test
    fun rejectsLegacy1xAndGarbage() {
        assertFalse(ScrcpyVersion.isSupported("scrcpy 1.25"))
        assertFalse(ScrcpyVersion.isSupported("not a version"))
        assertEquals(null, ScrcpyVersion.parse(""))
    }
}
