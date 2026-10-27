package com.victorious.cam.pipeline

/**
 * Layout for composing 2+ feed frames into one output (picture-in-picture / tiles).
 */
enum class PipPreset {
    /** Full-bleed primary; secondary as corner inset. */
    PIP_CORNER,
    /** Side-by-side 50/50. */
    SPLIT_VERTICAL,
    /** Top/bottom 50/50. */
    SPLIT_HORIZONTAL,
    /** 2x2 grid (up to 4 sources; extras ignored). */
    GRID_2X2
}

enum class PipCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

data class PipSlot(
    val sourceId: String,
    /** Normalized rect in output: left, top, right, bottom in 0..1 */
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val zIndex: Int = 0
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f)
        require(right > left && bottom > top)
    }
}

data class PipConfig(
    val preset: PipPreset = PipPreset.PIP_CORNER,
    val corner: PipCorner = PipCorner.BOTTOM_RIGHT,
    /** Inset size as fraction of the shorter output side (PIP_CORNER only). */
    val insetFraction: Float = 0.33f,
    val insetMarginFraction: Float = 0.04f
) {
    fun slotsFor(sourceIds: List<String>): List<PipSlot> {
        if (sourceIds.isEmpty()) return emptyList()
        return when (preset) {
            PipPreset.PIP_CORNER -> pipCorner(sourceIds)
            PipPreset.SPLIT_VERTICAL -> {
                val a = sourceIds[0]
                val b = sourceIds.getOrNull(1) ?: return listOf(full(a, 0))
                listOf(
                    PipSlot(a, 0f, 0f, 0.5f, 1f, 0),
                    PipSlot(b, 0.5f, 0f, 1f, 1f, 1)
                )
            }
            PipPreset.SPLIT_HORIZONTAL -> {
                val a = sourceIds[0]
                val b = sourceIds.getOrNull(1) ?: return listOf(full(a, 0))
                listOf(
                    PipSlot(a, 0f, 0f, 1f, 0.5f, 0),
                    PipSlot(b, 0f, 0.5f, 1f, 1f, 1)
                )
            }
            PipPreset.GRID_2X2 -> {
                val ids = sourceIds.take(4)
                val positions = listOf(
                    floatArrayOf(0f, 0f, 0.5f, 0.5f),
                    floatArrayOf(0.5f, 0f, 1f, 0.5f),
                    floatArrayOf(0f, 0.5f, 0.5f, 1f),
                    floatArrayOf(0.5f, 0.5f, 1f, 1f)
                )
                ids.mapIndexed { i, id ->
                    val p = positions[i]
                    PipSlot(id, p[0], p[1], p[2], p[3], i)
                }
            }
        }
    }

    private fun full(id: String, z: Int) = PipSlot(id, 0f, 0f, 1f, 1f, z)

    private fun pipCorner(sourceIds: List<String>): List<PipSlot> {
        val primary = sourceIds[0]
        val secondary = sourceIds.getOrNull(1) ?: return listOf(full(primary, 0))
        val f = insetFraction.coerceIn(0.15f, 0.5f)
        val m = insetMarginFraction.coerceIn(0f, 0.2f)
        val (l, t, r, b) = when (corner) {
            PipCorner.TOP_LEFT -> floatArrayOf(m, m, m + f, m + f)
            PipCorner.TOP_RIGHT -> floatArrayOf(1f - m - f, m, 1f - m, m + f)
            PipCorner.BOTTOM_LEFT -> floatArrayOf(m, 1f - m - f, m + f, 1f - m)
            PipCorner.BOTTOM_RIGHT -> floatArrayOf(1f - m - f, 1f - m - f, 1f - m, 1f - m)
        }
        return listOf(
            PipSlot(primary, 0f, 0f, 1f, 1f, 0),
            PipSlot(secondary, l, t, r, b, 1)
        )
    }
}
