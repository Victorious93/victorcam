package com.victorious.cam.backup

import com.victorious.cam.feed.FeedSourceKind
import com.victorious.cam.library.FeedPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetBackupTest {
    private fun sample() = FeedPreset(
        name = "Demo",
        sourceKind = FeedSourceKind.VIDEO_FILE.name,
        sourceUri = "content://demo/a.mp4",
        textEnabled = true,
        text = "Hello",
        timestampEnabled = true
    )

    @Test
    fun jsonRoundTrip() {
        val json = PresetBackup.toJson(listOf(sample()))
        val back = PresetBackup.fromJson(json)
        assertEquals(1, back.size)
        assertEquals("Demo", back[0].name)
        assertEquals("Hello", back[0].text)
        assertTrue(back[0].timestampEnabled)
    }

    @Test
    fun zipRoundTripWithLogo() {
        val zip = PresetBackup.exportZip(
            listOf(sample()),
            logoFiles = mapOf("logo.bin" to byteArrayOf(1, 2, 3))
        )
        val imported = PresetBackup.importZip(zip)
        assertEquals(1, imported.presets.size)
        assertEquals(3, imported.logos["logo.bin"]?.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBadVersion() {
        PresetBackup.fromJson("""{"formatVersion":99,"presets":[]}""")
    }
}
