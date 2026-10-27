package com.victorious.cam.network

/**
 * Maps NDI-style URLs onto RTP/RTSP (or other playable) endpoints.
 *
 * Official NDI SDK is proprietary — not vendored. See docs/NDI.md.
 */
object NdiRtpBridge {

    data class Resolution(
        val playableUrl: String,
        val kind: StreamUrlClassifier.Kind,
        val viaGateway: Boolean,
        val certainty: String
    )

    /**
     * @param gateway map of NDI source name (case-insensitive) → playable URL
     */
    fun resolve(rawUrl: String, gateway: Map<String, String> = emptyMap()): Resolution? {
        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("ndi+rtp://") || lower.startsWith("rtp://") -> {
                val normalized = if (lower.startsWith("ndi+rtp://")) {
                    "rtp://" + trimmed.substringAfter("://")
                } else trimmed
                Resolution(
                    playableUrl = normalized,
                    kind = StreamUrlClassifier.Kind.NDI_RTP,
                    viaGateway = false,
                    certainty = "Medium — RTP/MPEG-TS UDP path (NDI-over-RTP fallback)"
                )
            }
            lower.startsWith("ndi://") || lower.startsWith("ndi:") -> {
                val name = when {
                    "://" in trimmed -> trimmed.substringAfter("://")
                    trimmed.lowercase().startsWith("ndi:") -> trimmed.substringAfter(":")
                    else -> ""
                }.trim().trim('/')
                if (name.isEmpty()) return null
                val mapped = gateway.entries.firstOrNull {
                    it.key.equals(name, ignoreCase = true)
                }?.value
                if (mapped.isNullOrBlank()) {
                    null
                } else {
                    val kind = StreamUrlClassifier.classify(mapped).let {
                        if (it == StreamUrlClassifier.Kind.UNSUPPORTED) StreamUrlClassifier.Kind.NDI_RTP
                        else it
                    }
                    Resolution(
                        playableUrl = mapped,
                        kind = kind,
                        viaGateway = true,
                        certainty = "Medium — gateway-mapped NDI name → ${kind.name}"
                    )
                }
            }
            else -> null
        }
    }

    fun isNdiFamily(url: String): Boolean {
        val u = url.trim().lowercase()
        return u.startsWith("ndi://") || u.startsWith("ndi:") ||
            u.startsWith("ndi+rtp://") || u.startsWith("rtp://")
    }
}

/**
 * Honest capability label for NDI.
 */
object NdiCapability {
    fun summary(): String =
        "NDI SDK not bundled (proprietary). NDI-over-RTP / gateway fallback available; " +
            "see docs/NDI.md. Certainty: Medium for RTP/gateway, N/A for native NDI."
}
