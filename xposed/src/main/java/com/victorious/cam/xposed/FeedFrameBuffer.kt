package com.victorious.cam.xposed

import java.util.concurrent.atomic.AtomicReference

/**
 * Latest virtual-camera frame bytes shared with hooked processes.
 *
 * The host app writes RGBA (or NV21) frames here via [publish]; the Xposed hook
 * reads them in [snapshot] to rewrite ImageReader planes.
 *
 * Cross-process: prefer a MemoryFile / ContentProvider in production; this
 * in-process buffer is the substitution core and is fully unit-testable.
 */
data class FeedFrame(
    val width: Int,
    val height: Int,
    val format: Format,
    val bytes: ByteArray,
    val timestampNs: Long = System.nanoTime()
) {
    enum class Format { RGBA_8888, NV21 }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeedFrame) return false
        return width == other.width && height == other.height &&
            format == other.format && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int =
        31 * (31 * (31 * width + height) + format.hashCode()) + bytes.contentHashCode()
}

object FeedFrameBuffer {
    private val latest = AtomicReference<FeedFrame?>(null)

    fun publish(frame: FeedFrame) {
        require(frame.width > 0 && frame.height > 0)
        require(frame.bytes.isNotEmpty())
        latest.set(frame)
    }

    fun clear() {
        latest.set(null)
    }

    fun snapshot(): FeedFrame? = latest.get()

    fun hasFrame(): Boolean = latest.get() != null
}
