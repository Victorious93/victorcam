package com.victorious.cam.network

/**
 * Parsed SRT URL options. Forward-compatible with a future libsrt backend.
 * See docs/SRT.md for what is actually applied today.
 */
data class SrtOptions(
    val host: String,
    val port: Int,
    val mode: Mode = Mode.CALLER,
    val latencyMs: Int = 120,
    val passphrase: String? = null,
    val pbKeyLen: Int? = null,
    val streamId: String? = null
) {
    enum class Mode { CALLER, LISTENER, RENDEZVOUS }

    /** True when options request features that need real libsrt. */
    fun requiresNativeLibsrt(): Boolean =
        mode != Mode.CALLER || !passphrase.isNullOrEmpty() || streamId != null

    companion object {
        fun parse(url: String): SrtOptions? {
            return try {
                val u = java.net.URI(url.trim())
                if (u.scheme?.lowercase() != "srt") return null
                val host = u.host ?: return null
                val port = if (u.port > 0) u.port else 9000
                val q = parseQuery(u.rawQuery)
                val mode = when (q["mode"]?.lowercase()) {
                    "listener" -> Mode.LISTENER
                    "rendezvous" -> Mode.RENDEZVOUS
                    else -> Mode.CALLER
                }
                SrtOptions(
                    host = host,
                    port = port,
                    mode = mode,
                    latencyMs = q["latency"]?.toIntOrNull()?.coerceIn(20, 8000) ?: 120,
                    passphrase = q["passphrase"]?.takeIf { it.isNotEmpty() },
                    pbKeyLen = q["pbkeylen"]?.toIntOrNull(),
                    streamId = q["streamid"]?.takeIf { it.isNotEmpty() }
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun parseQuery(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split("&").mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null
                else {
                    val k = java.net.URLDecoder.decode(part.substring(0, i), Charsets.UTF_8)
                    val v = java.net.URLDecoder.decode(part.substring(i + 1), Charsets.UTF_8)
                    k.lowercase() to v
                }
            }.toMap()
        }
    }
}

/**
 * Capability label for SRT ingest (honest certainty for UI / docs).
 */
object SrtCapability {
    enum class Level {
        /** MPEG-TS over UDP caller path — lab feeds. */
        TS_OVER_UDP,
        /** Full libsrt (handshake / encrypt / rendezvous) — not bundled. */
        NATIVE_LIBSRT
    }

    fun shippedLevel(): Level = Level.TS_OVER_UDP

    fun certaintyLabel(level: Level = shippedLevel()): String = when (level) {
        Level.TS_OVER_UDP ->
            "Medium — MPEG-TS over UDP caller path; no libsrt handshake/encryption/rendezvous."
        Level.NATIVE_LIBSRT ->
            "High — full SRT via native libsrt (not currently vendored)."
    }

    fun summary(): String =
        "SRT: ${certaintyLabel()}. See docs/SRT.md. No free Maven libsrt AAR found; " +
            "passphrase/mode/streamid are parsed but not applied."
}
