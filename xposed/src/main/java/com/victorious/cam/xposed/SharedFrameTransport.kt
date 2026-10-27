package com.victorious.cam.xposed

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Cross-process feed-frame transport contract (versioned + backpressure).
 *
 * **Shipped path (certainty: High for logic / Medium for IPC):** an in-process
 * ring that mirrors the on-wire layout of a SharedMemory / ashmem / MemoryFile
 * region. The host app writes; hooked apps read the latest committed version.
 *
 * **Production wiring:** bind the same byte layout to
 * `android.os.SharedMemory` (API 27+) or a `ContentProvider` + `ParcelFileDescriptor`
 * / MemoryFile. Layout is ABI-stable so the swap does not change callers.
 *
 * Keep OFF by default — [HookGate] + [HookDenylist] still gate consumption.
 */
object SharedFrameTransport {

    const val MAGIC = 0x56434652 // 'VCFR'
    const val HEADER_BYTES = 40
    const val PROTOCOL_VERSION = 1

    /** Max outstanding unread frames before publish drops (backpressure). */
    const val DEFAULT_MAX_PENDING = 2

    data class Header(
        val magic: Int,
        val protocolVersion: Int,
        val frameVersion: Long,
        val width: Int,
        val height: Int,
        val formatOrdinal: Int,
        val payloadBytes: Int,
        val timestampNs: Long
    )

    data class Stats(
        val published: Long,
        val droppedBackpressure: Long,
        val reads: Long,
        val latestVersion: Long
    )

    private val slot = AtomicReference<ByteArray?>(null)
    private val frameVersion = AtomicLong(0)
    private val pending = AtomicInteger(0)
    private val published = AtomicLong(0)
    private val dropped = AtomicLong(0)
    private val reads = AtomicLong(0)
    @Volatile var maxPending: Int = DEFAULT_MAX_PENDING

    fun reset() {
        slot.set(null)
        frameVersion.set(0)
        pending.set(0)
        published.set(0)
        dropped.set(0)
        reads.set(0)
        maxPending = DEFAULT_MAX_PENDING
    }

    /**
     * Publish [frame] into the shared slot. Returns false when backpressure
     * drops the frame (consumer lagging).
     */
    fun publish(frame: FeedFrame): Boolean {
        require(frame.width > 0 && frame.height > 0)
        require(frame.bytes.isNotEmpty())
        if (pending.get() >= maxPending) {
            dropped.incrementAndGet()
            // Still replace latest so consumer gets freshest when it catches up
            writeSlot(frame)
            return false
        }
        writeSlot(frame)
        pending.incrementAndGet()
        published.incrementAndGet()
        return true
    }

    private fun writeSlot(frame: FeedFrame) {
        val ver = frameVersion.incrementAndGet()
        val payload = frame.bytes
        val buf = ByteBuffer.allocate(HEADER_BYTES + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(MAGIC)
        buf.putInt(PROTOCOL_VERSION)
        buf.putLong(ver)
        buf.putInt(frame.width)
        buf.putInt(frame.height)
        buf.putInt(frame.format.ordinal)
        buf.putInt(payload.size)
        buf.putLong(frame.timestampNs)
        // pad to HEADER_BYTES if needed — current fields = 4+4+8+4+4+4+4+8 = 40? Wait recount
        // We declared HEADER_BYTES=32 — fix layout to fit 32:
        // Actually let's use HEADER_BYTES = 40 consistently. Adjusting in companion.
        buf.put(payload)
        slot.set(buf.array())
        // Mirror into legacy in-process buffer for existing hooks
        FeedFrameBuffer.publish(frame.copy(timestampNs = frame.timestampNs))
    }

    fun snapshot(): FeedFrame? {
        val bytes = slot.get() ?: return null
        if (bytes.size < HEADER_BYTES) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val header = readHeader(buf) ?: return null
        if (header.payloadBytes <= 0 || HEADER_BYTES + header.payloadBytes > bytes.size) return null
        val payload = ByteArray(header.payloadBytes)
        buf.position(HEADER_BYTES)
        buf.get(payload)
        reads.incrementAndGet()
        pending.updateAndGet { (it - 1).coerceAtLeast(0) }
        val format = FeedFrame.Format.entries.getOrElse(header.formatOrdinal) {
            FeedFrame.Format.RGBA_8888
        }
        return FeedFrame(
            width = header.width,
            height = header.height,
            format = format,
            bytes = payload,
            timestampNs = header.timestampNs
        )
    }

    fun latestVersion(): Long = frameVersion.get()

    fun hasFrame(): Boolean = slot.get() != null

    fun stats(): Stats = Stats(
        published = published.get(),
        droppedBackpressure = dropped.get(),
        reads = reads.get(),
        latestVersion = frameVersion.get()
    )

    fun encodeHeader(
        version: Long,
        width: Int,
        height: Int,
        formatOrdinal: Int,
        payloadBytes: Int,
        timestampNs: Long
    ): ByteArray {
        // Layout (40 bytes): magic(4)+proto(4)+version(8)+w(4)+h(4)+fmt(4)+payload(4)+ts(8)
        val buf = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(MAGIC)
        buf.putInt(PROTOCOL_VERSION)
        buf.putLong(version)
        buf.putInt(width)
        buf.putInt(height)
        buf.putInt(formatOrdinal)
        buf.putInt(payloadBytes)
        buf.putLong(timestampNs)
        return buf.array()
    }

    fun readHeader(buf: ByteBuffer): Header? {
        if (buf.remaining() < HEADER_BYTES) return null
        buf.order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.int
        if (magic != MAGIC) return null
        val proto = buf.int
        if (proto != PROTOCOL_VERSION) return null
        val ver = buf.long
        val w = buf.int
        val h = buf.int
        val fmt = buf.int
        val payload = buf.int
        val ts = buf.long
        return Header(magic, proto, ver, w, h, fmt, payload, ts)
    }
}
