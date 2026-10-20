package com.victorious.cam.library

import kotlinx.coroutines.flow.Flow

class FeedLibraryRepository(private val dao: FeedPresetDao) {
    fun observePresets(): Flow<List<FeedPreset>> = dao.observeAll()
    suspend fun list(): List<FeedPreset> = dao.listAll()
    suspend fun get(id: Long): FeedPreset? = dao.getById(id)
    suspend fun save(preset: FeedPreset): Long = dao.upsert(preset)
    suspend fun rename(id: Long, name: String) {
        require(name.isNotBlank()) { "name blank" }
        dao.rename(id, name.trim())
    }
    suspend fun delete(id: Long) = dao.deleteById(id)
}
