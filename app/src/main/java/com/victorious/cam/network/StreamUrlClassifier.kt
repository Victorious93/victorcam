package com.victorious.cam.network

/**
 * Classifies OBS-style network input URLs.
 */
object StreamUrlClassifier {
    enum class Kind {
        RTSP, RTMP, SRT, HTTP, HLS, DASH, NDI, UNSUPPORTED
    }

    fun classify(raw: String): Kind {
        val u = raw.trim().lowercase()
        return when {
            u.startsWith("ndi://") || u.startsWith("ndi:") -> Kind.NDI
            u.startsWith("rtsp://") -> Kind.RTSP
            u.startsWith("rtmp://") || u.startsWith("rtmps://") -> Kind.RTMP
            u.startsWith("srt://") -> Kind.SRT
            u.contains(".m3u8") -> Kind.HLS
            u.contains(".mpd") -> Kind.DASH
            u.startsWith("http://") || u.startsWith("https://") -> Kind.HTTP
            else -> Kind.UNSUPPORTED
        }
    }

    fun isPlayable(kind: Kind): Boolean = when (kind) {
        Kind.RTSP, Kind.RTMP, Kind.SRT, Kind.HTTP, Kind.HLS, Kind.DASH -> true
        Kind.NDI, Kind.UNSUPPORTED -> false
    }

    @Deprecated("Use isPlayable", ReplaceWith("isPlayable(kind)"))
    fun isPlayableBestEffort(kind: Kind): Boolean = isPlayable(kind)
}
