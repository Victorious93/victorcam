package com.victorious.cam.network

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SRT-oriented [DataSource] for Media3 ProgressiveMediaSource (MPEG-TS payloads).
 *
 * **Shipped path (certainty: Medium):** caller-mode UDP ingest of MPEG-TS from
 * `srt://host:port[?options]`. Suitable for lab TS feeds.
 *
 * **Not shipped (certainty: Low / N/A):** full libsrt handshake, AES encryption,
 * rendezvous/listener modes. Options are parsed via [SrtOptions] for a future
 * libsrt drop-in — see docs/SRT.md. No free Maven-published libsrt AAR found.
 */
@UnstableApi
class SrtDataSource : BaseDataSource(/* isNetwork= */ true) {

    private var socket: DatagramSocket? = null
    private var uri: Uri? = null
    private val open = AtomicBoolean(false)
    private val buffer = ByteArray(2048)
    private var pending: ByteArray? = null
    private var pendingOffset = 0

    private var options: SrtOptions? = null

    override fun open(dataSpec: DataSpec): Long {
        val u = dataSpec.uri
        uri = u
        val opts = SrtOptions.parse(u.toString())
            ?: run {
                val host = u.host ?: throw IOException("SRT URL missing host")
                val port = if (u.port > 0) u.port else 9000
                SrtOptions(host, port)
            }
        options = opts
        if (opts.mode != SrtOptions.Mode.CALLER) {
            throw IOException(
                "SRT mode=${opts.mode} requires native libsrt (not bundled). " +
                    "Use mode=caller with a plain MPEG-TS UDP peer, or see docs/SRT.md."
            )
        }
        if (!opts.passphrase.isNullOrEmpty()) {
            // Parsed but not applied — surface clearly rather than silently drop security.
            throw IOException(
                "SRT passphrase set but encryption is not implemented without libsrt. " +
                    "Remove passphrase for the TS-over-UDP path, or see docs/SRT.md."
            )
        }
        val sock = DatagramSocket()
        // latency option used only as a receive timeout hint on the UDP path
        sock.soTimeout = (opts.latencyMs * 10).coerceIn(1000, 15000)
        sock.connect(InetSocketAddress(InetAddress.getByName(opts.host), opts.port))
        // Send a minimal probe so caller-mode sources that wait for traffic wake up.
        try {
            sock.send(DatagramPacket(ByteArray(1), 1))
        } catch (_: Exception) {
            // ignore — some servers are push-only
        }
        socket = sock
        open.set(true)
        transferStarted(dataSpec)
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(bufferOut: ByteArray, offset: Int, length: Int): Int {
        if (!open.get()) return C.RESULT_END_OF_INPUT
        val leftover = pending
        if (leftover != null) {
            val toCopy = minOf(length, leftover.size - pendingOffset)
            System.arraycopy(leftover, pendingOffset, bufferOut, offset, toCopy)
            pendingOffset += toCopy
            if (pendingOffset >= leftover.size) {
                pending = null
                pendingOffset = 0
            }
            bytesTransferred(toCopy)
            return toCopy
        }
        val sock = socket ?: return C.RESULT_END_OF_INPUT
        return try {
            val packet = DatagramPacket(buffer, buffer.size)
            sock.receive(packet)
            val n = packet.length
            if (n <= length) {
                System.arraycopy(packet.data, packet.offset, bufferOut, offset, n)
                bytesTransferred(n)
                n
            } else {
                System.arraycopy(packet.data, packet.offset, bufferOut, offset, length)
                pending = packet.data.copyOfRange(packet.offset + length, packet.offset + n)
                pendingOffset = 0
                bytesTransferred(length)
                length
            }
        } catch (e: Exception) {
            throw IOException("SRT/UDP read failed: ${e.message}", e)
        }
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        open.set(false)
        socket?.close()
        socket = null
        pending = null
        transferEnded()
    }

    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource = SrtDataSource()
    }

    companion object {
        /** Parses host/port from srt:// URLs (java.net — JVM unit-test safe). */
        fun parseEndpoint(url: String): Pair<String, Int>? =
            SrtOptions.parse(url)?.let { it.host to it.port }
    }
}
