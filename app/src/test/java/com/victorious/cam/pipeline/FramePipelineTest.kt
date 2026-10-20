package com.victorious.cam.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FramePipelineTest {

    @Test
    fun pushBitmap_latestFrameReturnsCopy() {
        val pipe = FramePipeline(64, 48)
        pipe.start()
        Thread.sleep(100)
        val src = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.RED)
        pipe.pushBitmap(src)
        val out = pipe.latestFrame()
        assertNotNull(out)
        assertEquals(64, out!!.width)
        assertEquals(48, out.height)
        assertTrue(pipe.isRunning())
        pipe.stop()
        assertFalse(pipe.isRunning())
    }

    @Test
    fun placeholder_producesFrame() {
        val pipe = FramePipeline(32, 32)
        pipe.start()
        Thread.sleep(80)
        pipe.pushPlaceholder("test")
        assertNotNull(pipe.latestFrame())
        pipe.stop()
    }
}
