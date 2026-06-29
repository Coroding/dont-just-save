package com.coroding.dontjustsave.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TopicCardEntity::class, CreationTaskEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun topicCardDao(): TopicCardDao
    abstract fun creationTaskDao(): CreationTaskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dont_just_save.db",
                )
                    // P0 Demo: allow rebuilding local-only data while Task18 link metadata
                    // and AI batch-classification fields are still evolving.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
