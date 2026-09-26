package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ConversationMemoryDao
import com.example.data.dao.PreferenceMemoryDao
import com.example.data.dao.ProjectMemoryDao
import com.example.data.dao.TaskMemoryDao
import com.example.data.dao.UserMemoryDao
import com.example.data.models.ConversationMemoryEntity
import com.example.data.models.PreferenceMemoryEntity
import com.example.data.models.ProjectMemoryEntity
import com.example.data.models.TaskMemoryEntity
import com.example.data.models.UserMemoryEntity

@Database(
    entities = [
        UserMemoryEntity::class,
        ProjectMemoryEntity::class,
        TaskMemoryEntity::class,
        ConversationMemoryEntity::class,
        PreferenceMemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KaranDatabase : RoomDatabase() {
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun projectMemoryDao(): ProjectMemoryDao
    abstract fun taskMemoryDao(): TaskMemoryDao
    abstract fun conversationMemoryDao(): ConversationMemoryDao
    abstract fun preferenceMemoryDao(): PreferenceMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: KaranDatabase? = null

        fun getInstance(context: Context): KaranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaranDatabase::class.java,
                    "karan_assistant.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
