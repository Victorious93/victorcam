package com.victorious.cam.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FeedPreset::class], version = 1, exportSchema = false)
abstract class FeedLibraryDatabase : RoomDatabase() {
    abstract fun presets(): FeedPresetDao

    companion object {
        @Volatile private var instance: FeedLibraryDatabase? = null

        fun get(context: Context): FeedLibraryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FeedLibraryDatabase::class.java,
                    "victorcam_library.db"
                ).build().also { instance = it }
            }
    }
}
