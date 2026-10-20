package com.victorious.cam.library

import com.victorious.cam.feed.FeedSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedLibraryStoreTest {
    @Test
    fun saveSelectRenameDelete() {
        val store = InMemoryFeedPresetStore()
        val a = store.save(
            FeedPreset(name = "Demo", sourceKind = FeedSourceKind.VIDEO_FILE.name, sourceUri = "content://a")
        )
        val b = store.save(
            FeedPreset(name = "Alpha", sourceKind = FeedSourceKind.IMAGE.name, sourceUri = "content://b")
        )
        assertEquals(listOf("Alpha", "Demo"), store.list().map { it.name })
        assertEquals(a.id, store.get(a.id)?.id)

        store.rename(b.id, "Beta")
        assertEquals("Beta", store.get(b.id)?.name)

        store.delete(a.id)
        assertNull(store.get(a.id))
        assertEquals(1, store.list().size)
    }

    @Test
    fun kindOrDefault() {
        val p = FeedPreset(name = "x", sourceKind = "IMAGE")
        assertEquals(FeedSourceKind.IMAGE, p.kindOrDefault())
        assertEquals(FeedSourceKind.VIDEO_FILE, FeedPreset(name = "y", sourceKind = "NOPE").kindOrDefault())
    }

    @Test
    fun renameRejectsBlank() {
        val store = InMemoryFeedPresetStore()
        val p = store.save(FeedPreset(name = "A", sourceKind = "IMAGE"))
        try {
            store.rename(p.id, "  ")
            org.junit.Assert.fail()
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }
}
