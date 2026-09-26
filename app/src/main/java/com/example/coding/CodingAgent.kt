package com.example.coding

data class CodingTaskPlan(
    val title: String,
    val steps: List<String>,
    val targetFiles: List<String>
)

data class CodingResult(
    val success: Boolean,
    val summary: String,
    val changedFiles: List<String>,
    val issuesFound: List<String> = emptyList(),
    val previewReady: Boolean = false
)

class CodingAgent(
    private val projectManager: ProjectManager,
    private val errorAnalyzer: ErrorAnalyzer
) {

    suspend fun executeCodeWorkflow(
        projectId: String,
        instruction: String,
        targetFile: String? = null,
        fileContent: String? = null
    ): CodingResult {
        val fm = projectManager.getFileManagerForProject(projectId)
        val fileEditor = FileEditor(fm)
        val codeAnalyzer = CodeAnalyzer(fm)
        val buildManager = BuildManager(fm)

        val changedFiles = mutableListOf<String>()

        // If direct file content provided
        if (targetFile != null && fileContent != null) {
            val res = fm.updateFile(targetFile, fileContent)
            if (res.success) {
                changedFiles.add(targetFile)
            }
        }

        // Validate project
        val analysis = codeAnalyzer.analyzeProject()
        val build = buildManager.buildPreview()

        val summary = if (changedFiles.isNotEmpty()) {
            "Updated files: ${changedFiles.joinToString(", ")}. ${analysis.summary}"
        } else {
            analysis.summary
        }

        return CodingResult(
            success = analysis.issues.none { it.contains("Mismatched") },
            summary = summary,
            changedFiles = changedFiles,
            issuesFound = analysis.issues,
            previewReady = build.success
        )
    }

    fun diagnoseAndFix(errorMsg: String): ErrorDiagnosis {
        return errorAnalyzer.diagnose(errorMsg)
    }
}
