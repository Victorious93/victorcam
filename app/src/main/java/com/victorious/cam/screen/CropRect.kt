package com.victorious.cam.screen

/**
 * Immutable screen crop rectangle in display pixels.
 */
data class CropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) {
    init {
        require(width > 0 && height > 0) { "Crop size must be positive" }
        require(left >= 0 && top >= 0) { "Crop origin must be non-negative" }
    }

    fun summary(): String = "${width}x${height} @ ($left,$top)"

    fun clampedTo(maxW: Int, maxH: Int): CropRect {
        val l = left.coerceIn(0, (maxW - 1).coerceAtLeast(0))
        val t = top.coerceIn(0, (maxH - 1).coerceAtLeast(0))
        val w = width.coerceAtMost(maxW - l).coerceAtLeast(1)
        val h = height.coerceAtMost(maxH - t).coerceAtLeast(1)
        return CropRect(l, t, w, h)
    }

    companion object {
        fun fullHd(): CropRect = CropRect(0, 0, 1920, 1080)
        fun parse(csv: String): CropRect? {
            val p = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (p.size != 4) return null
            return try { CropRect(p[0], p[1], p[2], p[3]) } catch (_: Exception) { null }
        }
    }
}
