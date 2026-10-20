package com.victorious.cam.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedPresetDao {
    @Query("SELECT * FROM feed_presets ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FeedPreset>>

    @Query("SELECT * FROM feed_presets ORDER BY name COLLATE NOCASE ASC")
    suspend fun listAll(): List<FeedPreset>

    @Query("SELECT * FROM feed_presets WHERE id = :id")
    suspend fun getById(id: Long): FeedPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: FeedPreset): Long

    @Update
    suspend fun update(preset: FeedPreset)

    @Query("UPDATE feed_presets SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(preset: FeedPreset)

    @Query("DELETE FROM feed_presets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
