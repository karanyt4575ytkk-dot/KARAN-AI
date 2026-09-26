package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memory")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general", // "preference", "profile", "workflow", "coding_style"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_memory")
data class ProjectMemoryEntity(
    @PrimaryKey val projectId: String,
    val name: String,
    val description: String,
    val technology: String, // "HTML/JS", "Python", "React", "Firebase", "Kotlin"
    val rootPath: String,
    val status: String = "active", // "active", "archived", "completed"
    val metadataJson: String = "{}"
)

@Entity(tableName = "task_memory")
data class TaskMemoryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val task: String,
    val planJson: String = "[]",
    val status: String = "pending", // "pending", "in_progress", "completed", "failed", "paused"
    val currentStepIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversation_memory")
data class ConversationMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "default",
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val toolCallsJson: String? = null,
    val toolResultsJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "preference_memory")
data class PreferenceMemoryEntity(
    @PrimaryKey val key: String,
    val value: String
)
