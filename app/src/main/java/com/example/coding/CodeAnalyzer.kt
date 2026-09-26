package com.example.coding

data class ProjectAnalysis(
    val fileCount: Int,
    val totalLines: Int,
    val detectedTech: List<String>,
    val hasIndexHtml: Boolean,
    val hasFirebaseConfig: Boolean,
    val issues: List<String>,
    val summary: String
)

class CodeAnalyzer(private val fileManager: FileManager) {

    fun analyzeProject(): ProjectAnalysis {
        val files = fileManager.listFiles()
        var fileCount = 0
        var totalLines = 0
        val techSet = mutableSetOf<String>()
        var hasIndex = false
        var hasFirebase = false
        val issues = mutableListOf<String>()

        fun walk(nodes: List<FileNode>) {
            for (node in nodes) {
                if (node.isDirectory) {
                    walk(node.children)
                } else {
                    fileCount++
                    val ext = node.name.substringAfterLast(".", "").lowercase()
                    when (ext) {
                        "html", "htm" -> techSet.add("HTML")
                        "css" -> techSet.add("CSS")
                        "js" -> techSet.add("JavaScript")
                        "ts" -> techSet.add("TypeScript")
                        "jsx", "tsx" -> techSet.add("React")
                        "py" -> techSet.add("Python")
                        "kt" -> techSet.add("Kotlin")
                        "java" -> techSet.add("Java")
                        "json" -> techSet.add("JSON")
                        "sql" -> techSet.add("SQL")
                    }

                    if (node.name.equals("index.html", ignoreCase = true)) {
                        hasIndex = true
                    }

                    val contentResult = fileManager.readFile(node.path)
                    if (contentResult.success && contentResult.content != null) {
                        val text = contentResult.content
                        totalLines += text.lines().size

                        if (text.contains("firebase") || text.contains("initializeApp")) {
                            hasFirebase = true
                            techSet.add("Firebase")
                        }

                        // Basic HTML checks
                        if (ext == "html") {
                            if (!text.contains("<meta name=\"viewport\"", ignoreCase = true)) {
                                issues.add("${node.path}: Missing mobile viewport meta tag")
                            }
                            val openHtml = Regex("<html", RegexOption.IGNORE_CASE).findAll(text).count()
                            val closeHtml = Regex("</html>", RegexOption.IGNORE_CASE).findAll(text).count()
                            if (openHtml != closeHtml) {
                                issues.add("${node.path}: Mismatched <html> tags")
                            }
                        }

                        // Basic JS checks
                        if (ext == "js") {
                            val openCurly = text.count { it == '{' }
                            val closeCurly = text.count { it == '}' }
                            if (openCurly != closeCurly) {
                                issues.add("${node.path}: Possible mismatched braces (open: $openCurly, close: $closeCurly)")
                            }
                        }
                    }
                }
            }
        }

        walk(files)

        if (fileCount == 0) {
            issues.add("Project is currently empty.")
        } else if (!hasIndex && techSet.contains("HTML")) {
            issues.add("No entrypoint 'index.html' found.")
        }

        val summary = "Project contains $fileCount files, $totalLines lines of code. Tech detected: ${techSet.joinToString(", ")}. Issues found: ${issues.size}."

        return ProjectAnalysis(
            fileCount = fileCount,
            totalLines = totalLines,
            detectedTech = techSet.toList().sorted(),
            hasIndexHtml = hasIndex,
            hasFirebaseConfig = hasFirebase,
            issues = issues,
            summary = summary
        )
    }
}
