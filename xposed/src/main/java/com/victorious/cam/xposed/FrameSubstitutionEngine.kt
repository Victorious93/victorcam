package com.victorious.cam.xposed

/**
 * Pure logic for substituting camera ImageReader plane bytes with a [FeedFrame].
 *
 * Returns false if substitution must be skipped (gate, missing frame, size mismatch
 * that cannot be handled). Does not touch Android Camera2 APIs — safe for JVM tests.
 */
object FrameSubstitutionEngine {

    data class Plane(
        val bytes: ByteArray,
        val pixelStride: Int,
        val rowStride: Int
    )

    data class Result(
        val substituted: Boolean,
        val reason: String,
        val planes: List<Plane> = emptyList()
    )

    fun shouldSubstitute(packageName: String, hasFrame: Boolean): Result {
        if (!HookGate.canHook(packageName)) {
            return Result(false, "gate_closed_or_denied")
        }
        if (!hasFrame) {
            return Result(false, "no_feed_frame")
        }
        return Result(true, "ok")
    }

    /**
     * Builds RGBA plane bytes sized to [targetW]x[targetH] from [frame].
     * Nearest-neighbor scale when dimensions differ.
     */
    fun materializeRgba(frame: FeedFrame, targetW: Int, targetH: Int): ByteArray {
        require(targetW > 0 && targetH > 0)
        val src = when (frame.format) {
            FeedFrame.Format.RGBA_8888 -> frame.bytes
            FeedFrame.Format.NV21 -> nv21ToRgba(frame.bytes, frame.width, frame.height)
        }
        if (frame.width == targetW && frame.height == targetH && frame.format == FeedFrame.Format.RGBA_8888) {
            return src.copyOf()
        }
        val out = ByteArray(targetW * targetH * 4)
        for (y in 0 until targetH) {
            val sy = y * frame.height / targetH
            for (x in 0 until targetW) {
                val sx = x * frame.width / targetW
                val si = (sy * frame.width + sx) * 4
                val di = (y * targetW + x) * 4
                if (si + 3 < src.size) {
                    out[di] = src[si]
                    out[di + 1] = src[si + 1]
                    out[di + 2] = src[si + 2]
                    out[di + 3] = src[si + 3]
                }
            }
        }
        return out
    }

    /**
     * Writes [rgba] into a mock ImageReader-style plane list (single RGBA plane).
     */
    fun writeIntoPlanes(rgba: ByteArray, width: Int, rowStride: Int): List<Plane> {
        val plane = ByteArray(rowStride * (rgba.size / (width * 4)).coerceAtLeast(1))
        val height = rgba.size / (width * 4)
        for (y in 0 until height) {
            System.arraycopy(rgba, y * width * 4, plane, y * rowStride, width * 4)
        }
        return listOf(Plane(plane, pixelStride = 4, rowStride = rowStride))
    }

    /** Very small NV21→RGBA (approx) for tests / YUV camera buffers. */
    fun nv21ToRgba(nv21: ByteArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height * 4)
        val frameSize = width * height
        var tip = 0
        for (j in 0 until height) {
            var yp = j * width
            for (i in 0 until width) {
                val y = (nv21[yp].toInt() and 0xff)
                val uvp = frameSize + (j shr 1) * width + (i and 1.inv())
                val v = if (uvp < nv21.size) (nv21[uvp].toInt() and 0xff) - 128 else 0
                val u = if (uvp + 1 < nv21.size) (nv21[uvp + 1].toInt() and 0xff) - 128 else 0
                var r = (y + 1.370705f * v).toInt()
                var g = (y - 0.337633f * u - 0.698001f * v).toInt()
                var b = (y + 1.732446f * u).toInt()
                r = r.coerceIn(0, 255); g = g.coerceIn(0, 255); b = b.coerceIn(0, 255)
                out[tip++] = r.toByte()
                out[tip++] = g.toByte()
                out[tip++] = b.toByte()
                out[tip++] = 0xFF.toByte()
                yp++
            }
        }
        return out
    }
}
