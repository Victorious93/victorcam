package com.victorious.cam.library

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.victorious.cam.feed.FeedSourceKind
import com.victorious.cam.overlay.OverlayPosition

@Entity(tableName = "feed_presets")
data class FeedPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceKind: String,
    val sourceUri: String? = null,
    val cropCsv: String? = null,
    val textEnabled: Boolean = false,
    val text: String = "",
    val textPosition: String = OverlayPosition.TOP_LEFT.name,
    val textOpacity: Float = 1f,
    val logoEnabled: Boolean = false,
    val logoUri: String? = null,
    val logoPosition: String = OverlayPosition.BOTTOM_RIGHT.name,
    val logoOpacity: Float = 0.85f,
    val timestampEnabled: Boolean = false,
    val timestampPosition: String = OverlayPosition.BOTTOM_LEFT.name,
    val timestampOpacity: Float = 0.9f,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun kindOrDefault(): FeedSourceKind =
        runCatching { FeedSourceKind.valueOf(sourceKind) }.getOrDefault(FeedSourceKind.VIDEO_FILE)
}
