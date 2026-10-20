package com.victorious.cam.overlay

/**
 * Configurable frame overlays composited onto feed frames.
 */
data class OverlayConfig(
    val textEnabled: Boolean = false,
    val text: String = "",
    val textPosition: OverlayPosition = OverlayPosition.TOP_LEFT,
    val textOpacity: Float = 1f,
    val logoEnabled: Boolean = false,
    val logoUri: String? = null,
    val logoPosition: OverlayPosition = OverlayPosition.BOTTOM_RIGHT,
    val logoOpacity: Float = 0.85f,
    val timestampEnabled: Boolean = false,
    val timestampPosition: OverlayPosition = OverlayPosition.BOTTOM_LEFT,
    val timestampOpacity: Float = 0.9f,
    val timestampPattern: String = "yyyy-MM-dd HH:mm:ss"
) {
    init {
        require(textOpacity in 0f..1f)
        require(logoOpacity in 0f..1f)
        require(timestampOpacity in 0f..1f)
    }

    fun anyEnabled(): Boolean = textEnabled || logoEnabled || timestampEnabled
}

enum class OverlayPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}
