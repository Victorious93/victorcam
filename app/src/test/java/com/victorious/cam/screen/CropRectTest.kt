package com.victorious.cam.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CropRectTest {
    @Test
    fun summaryAndClamp() {
        val r = CropRect(10, 20, 640, 480)
        assertEquals("640x480 @ (10,20)", r.summary())
        val c = r.clampedTo(100, 100)
        assertEquals(90, c.width)
        assertEquals(80, c.height)
    }

    @Test
    fun parseCsv() {
        assertEquals(CropRect(0, 0, 100, 50), CropRect.parse("0,0,100,50"))
        assertNull(CropRect.parse("bad"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroSize() {
        CropRect(0, 0, 0, 10)
    }
}
