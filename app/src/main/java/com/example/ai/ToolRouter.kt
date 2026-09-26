package com.example.ai

import com.example.automation.AppLauncher
import com.example.automation.BrowserAutomation
import com.example.automation.DeviceActions
import com.example.automation.KaranAccessibilityService
import com.example.coding.BuildManager
import com.example.coding.CodeAnalyzer
import com.example.coding.CodingAgent
import com.example.coding.ErrorAnalyzer
import com.example.coding.FileManager
import com.example.coding.ProjectManager
import com.example.memory.MemoryManager
import com.example.security.ConfirmationManager
import org.json.JSONArray
import org.json.JSONObject

data class ToolExecutionResult(
    val success: Boolean,
    val action: String,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
    val error: String? = null
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("success", success)
        obj.put("action", action)
        obj.put("message", message)
        if (error != null) obj.put("error", error)
        val dataObj = JSONObject()
        data.forEach { (k, v) -> dataObj.put(k, v) }
        obj.put("data", dataObj)
        return obj
    }
}

class ToolRouter(
    private val projectManager: ProjectManager,
    private val codingAgent: CodingAgent,
    private val appLauncher: AppLauncher,
    private val browserAutomation: BrowserAutomation,
    private val deviceActions: DeviceActions,
    private val memoryManager: MemoryManager,
    private val taskPlanner: TaskPlanner,
    private val confirmationManager: ConfirmationManager
) {

    fun getToolDeclarationsJson(): JSONArray {
        val array = JSONArray()

        // 1. coding_create_or_update_file
        array.put(JSONObject("""
        {
          "name": "coding_create_or_update_file",
          "description": "Create or update a code or configuration file in the current active project.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "filePath": { "type": "STRING", "description": "Relative file path inside the project, e.g. index.html or css/style.css" },
              "content": { "type": "STRING", "description": "Complete source code content to write into the file" }
            },
            "required": ["filePath", "content"]
          }
        }
        """))

        // 2. coding_read_file
        array.put(JSONObject("""
        {
          "name": "coding_read_file",
          "description": "Read the text contents of a project file.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "filePath": { "type": "STRING", "description": "Relative file path" }
            },
            "required": ["filePath"]
          }
        }
        """))

        // 3. coding_list_files
        array.put(JSONObject("""
        {
          "name": "coding_list_files",
          "description": "List all files and subdirectories in the current active project.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "subPath": { "type": "STRING", "description": "Subdirectory to list (leave empty for root)" }
            }
          }
        }
        """))

        // 4. coding_delete_file
        array.put(JSONObject("""
        {
          "name": "coding_delete_file",
          "description": "Delete a file or folder from the current project. Requires explicit user confirmation.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "filePath": { "type": "STRING", "description": "Relative path of file to delete" }
            },
            "required": ["filePath"]
          }
        }
        """))

        // 5. project_create
        array.put(JSONObject("""
        {
          "name": "project_create",
          "description": "Create a new project workspace with starter files and set it as active.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "name": { "type": "STRING", "description": "Display name of project" },
              "description": { "type": "STRING", "description": "Short description of project" },
              "technology": { "type": "STRING", "description": "Technology stack: HTML/JS, React, Python, or Firebase" }
            },
            "required": ["name", "technology"]
          }
        }
        """))

        // 6. coding_analyze_project
        array.put(JSONObject("""
        {
          "name": "coding_analyze_project",
          "description": "Analyze all files in the active project for syntax issues, tech stack, and structure.",
          "parameters": { "type": "OBJECT", "properties": {} }
        }
        """))

        // 7. device_open_app
        array.put(JSONObject("""
        {
          "name": "device_open_app",
          "description": "Launch an installed Android application by name (e.g. browser, settings, chrome, etc.).",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "appName": { "type": "STRING", "description": "Name or alias of the app to launch" }
            },
            "required": ["appName"]
          }
        }
        """))

        // 8. device_open_browser
        array.put(JSONObject("""
        {
          "name": "device_open_browser",
          "description": "Open a website or web search in the device browser.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "url": { "type": "STRING", "description": "Web URL or search query" }
            },
            "required": ["url"]
          }
        }
        """))

        // 9. accessibility_action
        array.put(JSONObject("""
        {
          "name": "accessibility_action",
          "description": "Perform an authorized accessibility automation action on screen (click text, input text, scroll, home, back).",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "action": { "type": "STRING", "description": "Action type: click_text, input_text, scroll_down, scroll_up, go_home, go_back, inspect_screen" },
              "text": { "type": "STRING", "description": "Target text to click or input text" }
            },
            "required": ["action"]
          }
        }
        """))

        // 10. memory_save
        array.put(JSONObject("""
        {
          "name": "memory_save",
          "description": "Save a non-sensitive fact, user preference, or project insight to persistent local memory.",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "key": { "type": "STRING", "description": "Memory key, e.g. favorite_color, default_theme" },
              "value": { "type": "STRING", "description": "Memory value" },
              "category": { "type": "STRING", "description": "Category: preference, coding_style, profile, general" }
            },
            "required": ["key", "value"]
          }
        }
        """))

        // 11. task_plan_continue
        array.put(JSONObject("""
        {
          "name": "task_plan_continue",
          "description": "Resume the latest pending or in-progress multi-step coding task from where it was left off.",
          "parameters": { "type": "OBJECT", "properties": {} }
        }
        """))

        // 12. firebase_setup_assistant
        array.put(JSONObject("""
        {
          "name": "firebase_setup_assistant",
          "description": "Setup or integrate Firebase into the active project (scaffolds auth, firestore, or rules).",
          "parameters": {
            "type": "OBJECT",
            "properties": {
              "feature": { "type": "STRING", "description": "Feature to setup: auth, firestore, rules, or console" }
            },
            "required": ["feature"]
          }
        }
        """))

        return array
    }

    suspend fun executeTool(name: String, args: JSONObject, activeProjectId: String?): ToolExecutionResult {
        return try {
            when (name) {
                "coding_create_or_update_file" -> {
                    val pId = activeProjectId ?: return ToolExecutionResult(false, name, "No active project selected. Create or select a project first.")
                    val filePath = args.optString("filePath")
                    val content = args.optString("content")
                    val fm = projectManager.getFileManagerForProject(pId)
                    val res = fm.updateFile(filePath, content)
                    taskPlanner.recordFileChanged(filePath)
                    ToolExecutionResult(res.success, name, res.message, mapOf("filePath" to filePath))
                }

                "coding_read_file" -> {
                    val pId = activeProjectId ?: return ToolExecutionResult(false, name, "No active project selected.")
                    val filePath = args.optString("filePath")
                    val fm = projectManager.getFileManagerForProject(pId)
                    val res = fm.readFile(filePath)
                    ToolExecutionResult(res.success, name, res.message, mapOf("content" to res.content))
                }

                "coding_list_files" -> {
                    val pId = activeProjectId ?: return ToolExecutionResult(false, name, "No active project selected.")
                    val subPath = args.optString("subPath", "")
                    val fm = projectManager.getFileManagerForProject(pId)
                    val list = fm.listFiles(subPath).map { it.path }
                    ToolExecutionResult(true, name, "Files found in project: ${list.size}", mapOf("files" to list))
                }

                "coding_delete_file" -> {
                    val pId = activeProjectId ?: return ToolExecutionResult(false, name, "No active project selected.")
                    val filePath = args.optString("filePath")
                    // Consequential action: Request confirmation
                    val confirmed = confirmationManager.requestConfirmation(
                        title = "Delete File Confirmation",
                        action = "delete_file",
                        consequence = "The file '$filePath' in project '$pId' will be permanently deleted.",
                        details = filePath
                    )
                    if (!confirmed) {
                        return ToolExecutionResult(false, name, "File deletion was cancelled by user.", error = "User rejected deletion")
                    }
                    val fm = projectManager.getFileManagerForProject(pId)
                    val res = fm.deleteFile(filePath)
                    ToolExecutionResult(res.success, name, res.message)
                }

                "project_create" -> {
                    val pName = args.optString("name", "New Project")
                    val desc = args.optString("description", "Created by KARAN")
                    val tech = args.optString("technology", "HTML/JS")
                    val project = projectManager.createProject(pName, desc, tech)
                    taskPlanner.setActiveProject(project.projectId, project.name)
                    ToolExecutionResult(
                        true,
                        name,
                        "Created project '${project.name}' (${project.technology}) with ID: ${project.projectId}",
                        mapOf("projectId" to project.projectId, "name" to project.name)
                    )
                }

                "coding_analyze_project" -> {
                    val pId = activeProjectId ?: return ToolExecutionResult(false, name, "No active project selected.")
                    val fm = projectManager.getFileManagerForProject(pId)
                    val analyzer = CodeAnalyzer(fm)
                    val analysis = analyzer.analyzeProject()
                    ToolExecutionResult(
                        true,
                        name,
                        analysis.summary,
                        mapOf("fileCount" to analysis.fileCount, "lines" to analysis.totalLines, "issues" to analysis.issues)
                    )
                }

                "device_open_app" -> {
                    val appName = args.optString("appName")
                    val (success, msg) = appLauncher.launchAppByName(appName)
                    ToolExecutionResult(success, name, msg)
                }

                "device_open_browser" -> {
                    val url = args.optString("url")
                    val success = browserAutomation.openUrl(url)
                    ToolExecutionResult(success, name, if (success) "Opened $url in browser" else "Failed to open browser URL")
                }

                "accessibility_action" -> {
                    if (!KaranAccessibilityService.isServiceRunning()) {
                        return ToolExecutionResult(
                            false,
                            name,
                            "Accessibility Service is currently disabled. Please enable it in Settings to perform UI actions.",
                            error = "Accessibility service disconnected"
                        )
                    }
                    val action = args.optString("action")
                    val text = args.optString("text")
                    val result = when (action) {
                        "click_text" -> KaranAccessibilityService.clickByText(text)
                        "input_text" -> KaranAccessibilityService.inputText(text)
                        "scroll_down" -> KaranAccessibilityService.scroll(forward = true)
                        "scroll_up" -> KaranAccessibilityService.scroll(forward = false)
                        "go_home" -> KaranAccessibilityService.performHome()
                        "go_back" -> KaranAccessibilityService.performBack()
                        "inspect_screen" -> true
                        else -> false
                    }
                    val visible = if (action == "inspect_screen") KaranAccessibilityService.getVisibleTexts() else emptyList()
                    ToolExecutionResult(result, name, "Accessibility action '$action' executed with status: $result", mapOf("visibleTexts" to visible))
                }

                "memory_save" -> {
                    val key = args.optString("key")
                    val value = args.optString("value")
                    val cat = args.optString("category", "general")
                    val saved = memoryManager.saveUserMemory(key, value, cat)
                    if (saved) {
                        ToolExecutionResult(true, name, "Saved to memory: $key = $value ($cat)")
                    } else {
                        ToolExecutionResult(false, name, "Cannot store passwords, OTPs, or sensitive credentials in user memory for security reasons.", error = "Sensitive data rejected")
                    }
                }

                "task_plan_continue" -> {
                    val resumed = taskPlanner.resumeLatestTask()
                    ToolExecutionResult(resumed.success, name, resumed.message, mapOf("task" to (resumed.taskName ?: "")))
                }

                "firebase_setup_assistant" -> {
                    val feature = args.optString("feature", "auth")
                    val pId = activeProjectId ?: return ToolExecutionResult(false, name, "No active project selected. Please create or open a project first.")
                    val fm = projectManager.getFileManagerForProject(pId)

                    if (feature.lowercase() == "console") {
                        browserAutomation.openFirebaseConsole()
                        return ToolExecutionResult(true, name, "Opened Firebase Console in browser.")
                    }

                    // Scaffold Firebase Web integration
                    fm.createFolder("js")
                    fm.createFile("js/firebase-config.js", """
                    // Official Firebase Web Modular Configuration
                    // Configure with credentials from your Firebase Console
                    export const firebaseConfig = {
                      apiKey: "AIzaSyYOUR_ACTUAL_FIREBASE_KEY",
                      authDomain: "your-app.firebaseapp.com",
                      projectId: "your-app-id",
                      storageBucket: "your-app.appspot.com",
                      messagingSenderId: "123456789",
                      appId: "1:123456789:web:abcdef123"
                    };
                    """.trimIndent())

                    fm.updateFile("firestore.rules", """
                    rules_version = '2';
                    service cloud.firestore {
                      match /databases/{database}/documents {
                        match /users/{userId} {
                          allow read, write: if request.auth != null && request.auth.uid == userId;
                        }
                      }
                    }
                    """.trimIndent())

                    ToolExecutionResult(true, name, "Configured Firebase modular scaffold (js/firebase-config.js & firestore.rules).")
                }

                else -> ToolExecutionResult(false, name, "Unknown tool: $name", error = "Tool not found")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, name, "Tool execution encountered an exception: ${e.message}", error = e.localizedMessage)
        }
    }
}
