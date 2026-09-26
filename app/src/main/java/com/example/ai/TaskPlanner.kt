package com.example.ai

import com.example.data.models.TaskMemoryEntity
import com.example.memory.MemoryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ResumeResult(
    val success: Boolean,
    val message: String,
    val taskName: String? = null
)

class TaskPlanner(private val memoryManager: MemoryManager) {

    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    private val _currentProjectName = MutableStateFlow<String?>(null)
    val currentProjectName: StateFlow<String?> = _currentProjectName.asStateFlow()

    private val _currentTask = MutableStateFlow<String?>(null)
    val currentTask: StateFlow<String?> = _currentTask.asStateFlow()

    private val _filesChanged = MutableStateFlow<List<String>>(emptyList())
    val filesChanged: StateFlow<List<String>> = _filesChanged.asStateFlow()

    private val _knownErrors = MutableStateFlow<List<String>>(emptyList())
    val knownErrors: StateFlow<List<String>> = _knownErrors.asStateFlow()

    private val _pendingTasks = MutableStateFlow<List<String>>(emptyList())
    val pendingTasks: StateFlow<List<String>> = _pendingTasks.asStateFlow()

    private val _completedTasks = MutableStateFlow<List<String>>(emptyList())
    val completedTasks: StateFlow<List<String>> = _completedTasks.asStateFlow()

    fun setActiveProject(projectId: String, projectName: String) {
        _currentProjectId.value = projectId
        _currentProjectName.value = projectName
    }

    fun recordFileChanged(filePath: String) {
        val current = _filesChanged.value.toMutableList()
        if (!current.contains(filePath)) {
            current.add(filePath)
            _filesChanged.value = current
        }
    }

    fun recordError(error: String) {
        val current = _knownErrors.value.toMutableList()
        if (!current.contains(error)) {
            current.add(error)
            _knownErrors.value = current
        }
    }

    suspend fun createPlan(taskGoal: String, steps: List<String>, projectId: String) {
        _currentTask.value = taskGoal
        _pendingTasks.value = steps
        _completedTasks.value = emptyList()

        val jsonArray = JSONArray()
        steps.forEach { jsonArray.put(it) }

        val entity = TaskMemoryEntity(
            id = "task_" + UUID.randomUUID().toString().take(8),
            projectId = projectId,
            task = taskGoal,
            planJson = jsonArray.toString(),
            status = "in_progress",
            currentStepIndex = 0,
            updatedAt = System.currentTimeMillis()
        )
        memoryManager.saveTask(entity)
    }

    suspend fun markStepCompleted(step: String) {
        val pending = _pendingTasks.value.toMutableList()
        pending.remove(step)
        _pendingTasks.value = pending

        val done = _completedTasks.value.toMutableList()
        done.add(step)
        _completedTasks.value = done

        if (pending.isEmpty() && _currentTask.value != null) {
            // Task is completed
            _currentTask.value = null
        }
    }

    suspend fun resumeLatestTask(): ResumeResult {
        val task = memoryManager.getLatestActiveTask()
        if (task == null) {
            return ResumeResult(false, "No unfinished coding task found to resume.")
        }

        _currentProjectId.value = task.projectId
        _currentTask.value = task.task

        val steps = mutableListOf<String>()
        try {
            val arr = JSONArray(task.planJson)
            for (i in 0 until arr.length()) {
                steps.add(arr.getString(i))
            }
        } catch (_: Exception) {}

        if (steps.isNotEmpty() && task.currentStepIndex < steps.size) {
            val remaining = steps.drop(task.currentStepIndex)
            _pendingTasks.value = remaining
            _completedTasks.value = steps.take(task.currentStepIndex)
            val nextStep = remaining.firstOrNull() ?: ""
            return ResumeResult(
                true,
                "Resuming task '${task.task}' in project '${task.projectId}'. Next step: $nextStep",
                task.task
            )
        }

        return ResumeResult(true, "Resuming task: ${task.task}", task.task)
    }
}
