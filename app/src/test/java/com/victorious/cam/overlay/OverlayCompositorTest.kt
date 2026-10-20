package com.victorious.cam.overlay

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class OverlayCompositorTest {
    @Test
    fun compose_textAndTimestamp() {
        val base = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        base.eraseColor(Color.BLUE)
        val cfg = OverlayConfig(
            textEnabled = true,
            text = "VictorCam",
            timestampEnabled = true,
            timestampPattern = "HH:mm:ss"
        )
        val out = OverlayCompositor(clock = { 0L }).compose(base, cfg)
        assertNotNull(out)
        assertEquals(200, out.width)
        assertTrue(cfg.anyEnabled())
    }

    @Test
    fun opacityValidation() {
        try {
            OverlayConfig(textOpacity = 1.5f)
            org.junit.Assert.fail()
        } catch (_: IllegalArgumentException) {
        }
    }
}
