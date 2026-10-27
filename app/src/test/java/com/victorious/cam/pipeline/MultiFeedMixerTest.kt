package com.victorious.cam.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import com.victorious.cam.overlay.OverlayConfig
import com.victorious.cam.overlay.OverlayPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MultiFeedMixerTest {

    @Test
    fun pipCornerSlots() {
        val cfg = PipConfig(PipPreset.PIP_CORNER, PipCorner.BOTTOM_RIGHT)
        val slots = cfg.slotsFor(listOf("a", "b"))
        assertEquals(2, slots.size)
        assertEquals(0f, slots[0].left)
        assertEquals(1f, slots[0].right)
        assertTrue(slots[1].left > 0.5f)
        assertTrue(slots[1].top > 0.5f)
    }

    @Test
    fun gridProducesFourSlots() {
        val slots = PipConfig(PipPreset.GRID_2X2).slotsFor(listOf("a", "b", "c", "d", "e"))
        assertEquals(4, slots.size)
    }

    @Test
    fun composeDrawsPrimaryAndInset() {
        val mixer = MultiFeedMixer(64, 48)
        val primary = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.RED)
        }
        val inset = Bitmap.createBitmap(16, 12, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.BLUE)
        }
        val out = mixer.compose(
            listOf(
                MultiFeedMixer.Layer("a", primary),
                MultiFeedMixer.Layer("b", inset)
            ),
            PipConfig(PipPreset.PIP_CORNER, PipCorner.BOTTOM_RIGHT, insetFraction = 0.25f)
        )
        assertEquals(64, out.width)
        assertEquals(48, out.height)
        // Primary fills most of the frame
        assertEquals(Color.RED, out.getPixel(1, 1))
        // Inset corner should be blue-ish
        assertEquals(Color.BLUE, out.getPixel(60, 44))
    }

    @Test
    fun composeToPipelineAndOverlay() {
        val pipe = FramePipeline(32, 32)
        pipe.start()
        Thread.sleep(50)
        val mixer = MultiFeedMixer(32, 32)
        val a = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.GREEN) }
        val b = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.YELLOW) }
        val overlay = OverlayConfig(textEnabled = true, text = "PiP", textPosition = OverlayPosition.TOP_LEFT)
        val frame = mixer.composeToPipeline(
            pipe,
            listOf(MultiFeedMixer.Layer("a", a), MultiFeedMixer.Layer("b", b)),
            PipConfig(PipPreset.SPLIT_VERTICAL),
            overlay
        )
        assertNotNull(pipe.latestFrame())
        assertEquals(32, frame.width)
        pipe.stop()
    }

    @Test
    fun splitVerticalRects() {
        val slots = PipConfig(PipPreset.SPLIT_VERTICAL).slotsFor(listOf("L", "R"))
        assertEquals(0.5f, slots[0].right)
        assertEquals(0.5f, slots[1].left)
    }
}
