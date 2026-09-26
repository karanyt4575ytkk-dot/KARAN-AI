package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KaranApplication
import com.example.ai.GeminiModelResponse
import com.example.ai.PromptManager
import com.example.ai.ResponseManager
import com.example.ai.ToolExecutionResult
import com.example.automation.DeviceStatus
import com.example.coding.BuildManager
import com.example.coding.CodeAnalyzer
import com.example.coding.ErrorDiagnosis
import com.example.coding.FileNode
import com.example.coding.ProjectAnalysis
import com.example.data.models.ConversationMemoryEntity
import com.example.data.models.ProjectMemoryEntity
import com.example.data.models.UserMemoryEntity
import com.example.permissions.PermissionItem
import com.example.security.ConfirmationRequest
import com.example.voice.SpeechState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiMessage(
    val id: String = System.currentTimeMillis().toString(),
    val role: String, // "user", "assistant", "system"
    val content: String,
    val toolResult: ToolExecutionResult? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val canRetry: Boolean = false,
    val originalPrompt: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KaranApplication

    // State
    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _deviceStatus = MutableStateFlow(app.deviceActions.getDeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val speechRecognizer = com.example.voice.SpeechRecognizerManager(app) { recognizedText ->
        submitUserPrompt(recognizedText)
    }

    val speechState: StateFlow<SpeechState> = speechRecognizer.speechState
    val rmsLevel: StateFlow<Float> = speechRecognizer.rmsLevel
    val isSpeaking: StateFlow<Boolean> = app.textToSpeechManager.isSpeaking
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = app.confirmationManager.pendingRequest

    val allProjects: StateFlow<List<ProjectMemoryEntity>> = app.projectManager.allProjects
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allMemories: StateFlow<List<UserMemoryEntity>> = app.memoryManager.allUserMemories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val permissions: StateFlow<List<PermissionItem>> = app.permissionManager.permissionsState

    private val _activeProject = MutableStateFlow<ProjectMemoryEntity?>(null)
    val activeProject: StateFlow<ProjectMemoryEntity?> = _activeProject.asStateFlow()

    val currentTask: StateFlow<String?> = app.taskPlanner.currentTask

    // Coding state
    private val _projectFiles = MutableStateFlow<List<FileNode>>(emptyList())
    val projectFiles: StateFlow<List<FileNode>> = _projectFiles.asStateFlow()

    private val _selectedFile = MutableStateFlow<String?>(null)
    val selectedFile: StateFlow<String?> = _selectedFile.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent: StateFlow<String> = _fileContent.asStateFlow()

    private val _projectAnalysis = MutableStateFlow<ProjectAnalysis?>(null)
    val projectAnalysis: StateFlow<ProjectAnalysis?> = _projectAnalysis.asStateFlow()

    private val _webPreviewHtml = MutableStateFlow<String?>(null)
    val webPreviewHtml: StateFlow<String?> = _webPreviewHtml.asStateFlow()

    private val _errorDiagnosis = MutableStateFlow<ErrorDiagnosis?>(null)
    val errorDiagnosis: StateFlow<ErrorDiagnosis?> = _errorDiagnosis.asStateFlow()

    // Debug logs
    private val _debugLogs = MutableStateFlow<List<String>>(emptyList())
    val debugLogs: StateFlow<List<String>> = _debugLogs.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    private val _currentAiModel = MutableStateFlow(com.example.ai.GeminiConfig.DEFAULT_MODEL)
    val currentAiModel: StateFlow<String> = _currentAiModel.asStateFlow()

    init {
        logDebug("KARAN system initialized.")
        refreshStatus()
        loadInitialHistory()

        viewModelScope.launch {
            val savedModel = app.memoryManager.getPreference("SELECTED_GEMINI_MODEL")
            val modelToUse = if (!savedModel.isNullOrBlank() && savedModel != "gemini-3.5-flash") {
                com.example.ai.GeminiConfig.sanitizeModelName(savedModel)
            } else {
                com.example.ai.GeminiConfig.DEFAULT_MODEL
            }
            app.geminiClient.activeModel = modelToUse
            _currentAiModel.value = modelToUse
            logDebug("Active AI Model initialized: $modelToUse")

            val act = app.projectManager.getActiveProject()
            if (act != null) {
                selectProject(act.projectId)
            }
        }
    }

    fun logDebug(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val list = _debugLogs.value.toMutableList()
        list.add(0, "[$time] $message")
        if (list.size > 100) list.removeAt(list.lastIndex)
        _debugLogs.value = list
    }

    fun refreshStatus() {
        _deviceStatus.value = app.deviceActions.getDeviceStatus()
        app.permissionManager.refresh()
    }

    private fun loadInitialHistory() {
        viewModelScope.launch {
            val recent = app.memoryManager.getRecentMessages(20)
            if (recent.isNotEmpty()) {
                val uiList = recent.map {
                    UiMessage(
                        id = it.id.toString(),
                        role = it.role,
                        content = it.content,
                        timestamp = it.timestamp
                    )
                }
                _messages.value = uiList
            } else {
                // Initial greeting
                val welcome = "नमस्ते! मैं करण (KARAN) हूँ — आपका AI कोडिंग और डिवाइस ऑटोमेशन असिस्टेंट। आप मुझसे कोडिंग, प्रोजेक्ट निर्माण, या डिवाइस ऑटोमेशन के लिए कह सकते हैं।"
                _messages.value = listOf(
                    UiMessage(role = "assistant", content = welcome)
                )
            }
        }
    }

    fun submitUserPrompt(prompt: String) {
        if (prompt.isBlank() || _isGenerating.value) return

        val userMsg = UiMessage(role = "user", content = prompt)
        _messages.value = _messages.value + userMsg
        _isGenerating.value = true
        logDebug("User command: $prompt")

        viewModelScope.launch(Dispatchers.IO) {
            app.memoryManager.logConversationTurn("user", prompt)

            val currentProj = _activeProject.value
            val status = app.deviceActions.getDeviceStatus()
            val statusSummary = "Battery: ${status.batteryPercent}%, Online: ${status.isOnline}, Accessibility: ${status.accessibilityConnected}"
            val sysPrompt = PromptManager.getSystemPrompt(currentProj?.name, currentTask.value, statusSummary)

            val history = _messages.value.dropLast(1).map { it.role to it.content }
            val tools = app.toolRouter.getToolDeclarationsJson()

            val response: GeminiModelResponse = app.geminiClient.generateWithTools(
                systemInstruction = sysPrompt,
                conversationHistory = history,
                latestPrompt = prompt,
                toolsDeclarations = tools,
                onRetryAttempt = { attempt, model, delayMs ->
                    logDebug("Retrying AI request ($model, attempt $attempt) in ${delayMs}ms...")
                }
            )

            withContext(Dispatchers.Main) {
                _isGenerating.value = false

                if (!response.isSuccess) {
                    val friendlyMsg = response.friendlyErrorMessage
                        ?: "The AI service is temporarily unavailable. Please try again."
                    _messages.value = _messages.value + UiMessage(
                        role = "assistant",
                        content = friendlyMsg,
                        isError = true,
                        canRetry = response.isRetryable,
                        originalPrompt = prompt
                    )
                    logDebug("Gemini error (${response.modelUsed}): ${response.errorMessage}")
                    app.textToSpeechManager.speak("सेवा अभी व्यस्त है। कृपया पुनः प्रयास करें।")
                    return@withContext
                }

                // If function calls are requested by Gemini
                if (response.functionCalls.isNotEmpty()) {
                    for (call in response.functionCalls) {
                        logDebug("Calling tool: ${call.name} with args: ${call.arguments}")
                        val toolResult = app.toolRouter.executeTool(
                            name = call.name,
                            args = call.arguments,
                            activeProjectId = currentProj?.projectId
                        )
                        logDebug("Tool ${call.name} returned: ${toolResult.message}")

                        _messages.value = _messages.value + UiMessage(
                            role = "assistant",
                            content = "Executed action: ${toolResult.message}",
                            toolResult = toolResult
                        )

                        // Refresh project state if project tool was called
                        if (call.name.startsWith("coding_") || call.name.startsWith("project_")) {
                            currentProj?.projectId?.let { loadProjectFiles(it) }
                        }
                    }
                }

                // Display textual answer if provided
                if (!response.text.isNullOrBlank()) {
                    val assistantMsg = UiMessage(role = "assistant", content = response.text)
                    _messages.value = _messages.value + assistantMsg
                    app.memoryManager.logConversationTurn("assistant", response.text)

                    // Speak the response summary
                    val voiceText = ResponseManager.formatVoiceSummary(response.text)
                    app.textToSpeechManager.speak(voiceText)
                }
            }
        }
    }

    // Voice
    fun toggleVoiceListening() {
        if (speechRecognizer.speechState.value == SpeechState.LISTENING) {
            speechRecognizer.stopListening()
        } else {
            speechRecognizer.startListening()
        }
    }

    fun stopSpeaking() {
        app.textToSpeechManager.stop()
    }

    fun setSpeechLanguage(localeCode: String) {
        speechRecognizer.selectedLocale = localeCode
    }

    // Coding & Projects
    fun selectProject(projectId: String) {
        viewModelScope.launch {
            val proj = app.projectManager.getActiveProject()
            app.projectManager.setActiveProject(projectId)
            val updated = app.database.projectMemoryDao().getProjectById(projectId)
            _activeProject.value = updated
            loadProjectFiles(projectId)
            app.taskPlanner.setActiveProject(projectId, updated?.name ?: "Project")
        }
    }

    fun createNewProject(name: String, desc: String, technology: String) {
        viewModelScope.launch {
            val p = app.projectManager.createProject(name, desc, technology)
            selectProject(p.projectId)
            logDebug("Created new project: $name ($technology)")
        }
    }

    fun loadProjectFiles(projectId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fm = app.projectManager.getFileManagerForProject(projectId)
            val files = fm.listFiles()
            val analyzer = CodeAnalyzer(fm)
            val analysis = analyzer.analyzeProject()
            val builder = BuildManager(fm)
            val preview = builder.buildPreview()

            withContext(Dispatchers.Main) {
                _projectFiles.value = files
                _projectAnalysis.value = analysis
                _webPreviewHtml.value = preview.bundledHtml
                if (_selectedFile.value == null && files.isNotEmpty()) {
                    val first = files.firstOrNull { !it.isDirectory }
                    first?.let { loadFile(it.path) }
                }
            }
        }
    }

    fun loadFile(relativePath: String) {
        val pId = _activeProject.value?.projectId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val fm = app.projectManager.getFileManagerForProject(pId)
            val res = fm.readFile(relativePath)
            withContext(Dispatchers.Main) {
                _selectedFile.value = relativePath
                _fileContent.value = res.content ?: ""
            }
        }
    }

    fun saveCurrentFile(content: String) {
        val pId = _activeProject.value?.projectId ?: return
        val path = _selectedFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val fm = app.projectManager.getFileManagerForProject(pId)
            fm.updateFile(path, content)
            app.taskPlanner.recordFileChanged(path)
            withContext(Dispatchers.Main) {
                _fileContent.value = content
                loadProjectFiles(pId)
                logDebug("Saved file: $path")
            }
        }
    }

    fun deleteFile(relativePath: String) {
        val pId = _activeProject.value?.projectId ?: return
        viewModelScope.launch {
            val confirmed = app.confirmationManager.requestConfirmation(
                title = "Delete File",
                action = "delete",
                consequence = "The file '$relativePath' will be permanently deleted."
            )
            if (confirmed) {
                withContext(Dispatchers.IO) {
                    val fm = app.projectManager.getFileManagerForProject(pId)
                    fm.deleteFile(relativePath)
                }
                loadProjectFiles(pId)
                _selectedFile.value = null
                _fileContent.value = ""
            }
        }
    }

    fun diagnoseError(errorMsg: String) {
        val diagnosis = app.errorAnalyzer.diagnose(errorMsg)
        _errorDiagnosis.value = diagnosis
    }

    // Confirmation
    fun resolveConfirmation(approved: Boolean) {
        app.confirmationManager.resolve(approved)
    }

    // Memory actions
    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            app.memoryManager.deleteUserMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            app.memoryManager.clearAllUserMemories()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            app.memoryManager.clearConversationHistory()
            _messages.value = emptyList()
            loadInitialHistory()
        }
    }

    fun retryPrompt(prompt: String) {
        logDebug("Manual retry triggered for prompt: $prompt")
        submitUserPrompt(prompt)
    }

    // Settings
    fun saveCustomApiKey(key: String) {
        app.secretManager.storeSecret("CUSTOM_GEMINI_API_KEY", key.trim())
        logDebug("Gemini API key updated in vault.")
    }

    fun setAiModel(model: String) {
        val clean = com.example.ai.GeminiConfig.sanitizeModelName(model)
        app.geminiClient.activeModel = clean
        _currentAiModel.value = clean
        logDebug("Switched AI Model to $clean")
        viewModelScope.launch {
            app.memoryManager.setPreference("SELECTED_GEMINI_MODEL", clean)
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        _isDeveloperMode.value = enabled
    }

    fun openAppSettings() {
        app.permissionManager.openAppSettings()
    }

    fun openAccessibilitySettings() {
        app.permissionManager.openAccessibilitySettings()
    }
}
