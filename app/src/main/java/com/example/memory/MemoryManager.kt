package com.example.memory

import com.example.data.KaranDatabase
import com.example.data.models.ConversationMemoryEntity
import com.example.data.models.ProjectMemoryEntity
import com.example.data.models.TaskMemoryEntity
import com.example.data.models.UserMemoryEntity
import com.example.security.SecretManager
import kotlinx.coroutines.flow.Flow

class MemoryManager(private val database: KaranDatabase) {

    private val userDao = database.userMemoryDao()
    private val projectDao = database.projectMemoryDao()
    private val taskDao = database.taskMemoryDao()
    private val conversationDao = database.conversationMemoryDao()
    private val prefDao = database.preferenceMemoryDao()

    val allUserMemories: Flow<List<UserMemoryEntity>> = userDao.getAllUserMemories()
    val allProjects: Flow<List<ProjectMemoryEntity>> = projectDao.getAllProjects()
    val allTasks: Flow<List<TaskMemoryEntity>> = taskDao.getAllTasks()
    val conversationHistory: Flow<List<ConversationMemoryEntity>> = conversationDao.getConversationHistory()

    suspend fun saveUserMemory(key: String, value: String, category: String = "general"): Boolean {
        // Enforce strict security: Never store passwords, OTPs, private keys
        if (SecretManager.isSensitive(key) || SecretManager.isSensitive(value)) {
            return false
        }
        val memory = UserMemoryEntity(
            key = key.trim(),
            value = value.trim(),
            category = category.trim().lowercase(),
            updatedAt = System.currentTimeMillis()
        )
        userDao.insertOrUpdate(memory)
        return true
    }

    suspend fun deleteUserMemory(id: Long) {
        userDao.deleteById(id)
    }

    suspend fun clearUserMemoriesByCategory(category: String) {
        userDao.deleteByCategory(category)
    }

    suspend fun clearAllUserMemories() {
        userDao.clearAll()
    }

    suspend fun getRecentMessages(limit: Int = 10): List<ConversationMemoryEntity> {
        return conversationDao.getRecentMessages(limit = limit).reversed()
    }

    suspend fun logConversationTurn(
        role: String,
        content: String,
        toolCallsJson: String? = null,
        toolResultsJson: String? = null
    ) {
        val entry = ConversationMemoryEntity(
            role = role,
            content = SecretManager.sanitizeForLogging(content),
            toolCallsJson = toolCallsJson?.let { SecretManager.sanitizeForLogging(it) },
            toolResultsJson = toolResultsJson?.let { SecretManager.sanitizeForLogging(it) }
        )
        conversationDao.insert(entry)
    }

    suspend fun clearConversationHistory() {
        conversationDao.clearAll()
    }

    suspend fun saveTask(task: TaskMemoryEntity) {
        taskDao.insertOrUpdate(task)
    }

    suspend fun getLatestActiveTask(): TaskMemoryEntity? {
        return taskDao.getLatestActiveTask()
    }

    suspend fun deleteTask(id: String) {
        taskDao.deleteById(id)
    }

    suspend fun getPreference(key: String): String? {
        return prefDao.getPreference(key)
    }

    suspend fun setPreference(key: String, value: String) {
        prefDao.setPreference(com.example.data.models.PreferenceMemoryEntity(key, value))
    }
}
