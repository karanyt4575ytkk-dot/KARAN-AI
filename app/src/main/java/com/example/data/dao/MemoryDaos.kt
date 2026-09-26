package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.ConversationMemoryEntity
import com.example.data.models.PreferenceMemoryEntity
import com.example.data.models.ProjectMemoryEntity
import com.example.data.models.TaskMemoryEntity
import com.example.data.models.UserMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memory ORDER BY updatedAt DESC")
    fun getAllUserMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memory WHERE category = :category ORDER BY updatedAt DESC")
    fun getMemoriesByCategory(category: String): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memory WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(memory: UserMemoryEntity): Long

    @Delete
    suspend fun delete(memory: UserMemoryEntity)

    @Query("DELETE FROM user_memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_memory WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("DELETE FROM user_memory")
    suspend fun clearAll()
}

@Dao
interface ProjectMemoryDao {
    @Query("SELECT * FROM project_memory ORDER BY projectId DESC")
    fun getAllProjects(): Flow<List<ProjectMemoryEntity>>

    @Query("SELECT * FROM project_memory WHERE projectId = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectMemoryEntity?

    @Query("SELECT * FROM project_memory WHERE status = 'active' LIMIT 1")
    suspend fun getActiveProject(): ProjectMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(project: ProjectMemoryEntity)

    @Delete
    suspend fun delete(project: ProjectMemoryEntity)

    @Query("DELETE FROM project_memory WHERE projectId = :projectId")
    suspend fun deleteById(projectId: String)
}

@Dao
interface TaskMemoryDao {
    @Query("SELECT * FROM task_memory ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskMemoryEntity>>

    @Query("SELECT * FROM task_memory WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getTasksForProject(projectId: String): Flow<List<TaskMemoryEntity>>

    @Query("SELECT * FROM task_memory WHERE status = 'in_progress' OR status = 'pending' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestActiveTask(): TaskMemoryEntity?

    @Query("SELECT * FROM task_memory WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(task: TaskMemoryEntity)

    @Update
    suspend fun update(task: TaskMemoryEntity)

    @Query("DELETE FROM task_memory WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM task_memory WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)
}

@Dao
interface ConversationMemoryDao {
    @Query("SELECT * FROM conversation_memory WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getConversationHistory(sessionId: String = "default"): Flow<List<ConversationMemoryEntity>>

    @Query("SELECT * FROM conversation_memory WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: String = "default", limit: Int = 20): List<ConversationMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationMemoryEntity): Long

    @Query("DELETE FROM conversation_memory WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String = "default")

    @Query("DELETE FROM conversation_memory")
    suspend fun clearAll()
}

@Dao
interface PreferenceMemoryDao {
    @Query("SELECT * FROM preference_memory")
    fun getAllPreferences(): Flow<List<PreferenceMemoryEntity>>

    @Query("SELECT value FROM preference_memory WHERE `key` = :key LIMIT 1")
    suspend fun getPreference(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: PreferenceMemoryEntity)

    @Query("DELETE FROM preference_memory WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}
