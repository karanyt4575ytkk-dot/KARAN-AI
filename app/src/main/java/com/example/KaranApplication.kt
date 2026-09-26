package com.example

import android.app.Application
import com.example.ai.GeminiClient
import com.example.ai.PromptManager
import com.example.ai.TaskPlanner
import com.example.ai.ToolRouter
import com.example.automation.AppLauncher
import com.example.automation.BrowserAutomation
import com.example.automation.DeviceActions
import com.example.coding.CodingAgent
import com.example.coding.ErrorAnalyzer
import com.example.coding.ProjectManager
import com.example.data.KaranDatabase
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.security.ConfirmationManager
import com.example.security.SecretManager
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import com.example.voice.WakeWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KaranApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: KaranDatabase
        private set
    lateinit var secretManager: SecretManager
        private set
    lateinit var memoryManager: MemoryManager
        private set
    lateinit var projectManager: ProjectManager
        private set
    lateinit var errorAnalyzer: ErrorAnalyzer
        private set
    lateinit var codingAgent: CodingAgent
        private set
    lateinit var appLauncher: AppLauncher
        private set
    lateinit var browserAutomation: BrowserAutomation
        private set
    lateinit var deviceActions: DeviceActions
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var confirmationManager: ConfirmationManager
        private set
    lateinit var taskPlanner: TaskPlanner
        private set
    lateinit var toolRouter: ToolRouter
        private set
    lateinit var geminiClient: GeminiClient
        private set
    lateinit var textToSpeechManager: TextToSpeechManager
        private set
    lateinit var wakeWordManager: WakeWordManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = KaranDatabase.getInstance(this)
        secretManager = SecretManager(this)
        memoryManager = MemoryManager(database)
        projectManager = ProjectManager(this, database.projectMemoryDao())
        errorAnalyzer = ErrorAnalyzer()
        codingAgent = CodingAgent(projectManager, errorAnalyzer)
        appLauncher = AppLauncher(this)
        browserAutomation = BrowserAutomation(this)
        deviceActions = DeviceActions(this)
        permissionManager = PermissionManager(this)
        confirmationManager = ConfirmationManager()
        taskPlanner = TaskPlanner(memoryManager)

        toolRouter = ToolRouter(
            projectManager = projectManager,
            codingAgent = codingAgent,
            appLauncher = appLauncher,
            browserAutomation = browserAutomation,
            deviceActions = deviceActions,
            memoryManager = memoryManager,
            taskPlanner = taskPlanner,
            confirmationManager = confirmationManager
        )

        geminiClient = GeminiClient(secretManager)
        textToSpeechManager = TextToSpeechManager(this)
        wakeWordManager = WakeWordManager()

        // Seed initial project if none exists
        applicationScope.launch(Dispatchers.IO) {
            val active = projectManager.getActiveProject()
            if (active == null) {
                val defaultProject = projectManager.createProject(
                    name = "KARAN Showcase",
                    description = "Personal AI coding sandbox with live web preview and Firebase auth support.",
                    technology = "HTML/JS"
                )
                taskPlanner.setActiveProject(defaultProject.projectId, defaultProject.name)
            } else {
                taskPlanner.setActiveProject(active.projectId, active.name)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        textToSpeechManager.shutdown()
    }

    companion object {
        lateinit var instance: KaranApplication
            private set
    }
}
