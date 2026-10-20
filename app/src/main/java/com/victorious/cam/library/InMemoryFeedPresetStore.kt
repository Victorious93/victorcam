package com.victorious.cam.library

import java.util.concurrent.atomic.AtomicLong

/** Pure JVM stand-in for Room DAO CRUD used by unit tests. */
class InMemoryFeedPresetStore {
    private val seq = AtomicLong(1)
    private val map = linkedMapOf<Long, FeedPreset>()

    fun list(): List<FeedPreset> = map.values.sortedBy { it.name.lowercase() }

    fun get(id: Long): FeedPreset? = map[id]

    fun save(preset: FeedPreset): FeedPreset {
        val id = if (preset.id == 0L) seq.getAndIncrement() else preset.id
        val saved = preset.copy(id = id, updatedAt = System.currentTimeMillis())
        map[id] = saved
        return saved
    }

    fun rename(id: Long, name: String) {
        require(name.isNotBlank())
        val cur = map[id] ?: return
        map[id] = cur.copy(name = name.trim(), updatedAt = System.currentTimeMillis())
    }

    fun delete(id: Long) {
        map.remove(id)
    }
}
