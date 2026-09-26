package com.example.coding

data class BuildResult(
    val success: Boolean,
    val bundledHtml: String? = null,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

class BuildManager(private val fileManager: FileManager) {

    fun buildPreview(): BuildResult {
        val indexResult = fileManager.readFile("index.html")
        if (!indexResult.success || indexResult.content.isNullOrBlank()) {
            return BuildResult(
                success = false,
                errors = listOf("No index.html file found in project root. Please create index.html to preview.")
            )
        }

        var html = indexResult.content
        val warnings = mutableListOf<String>()

        // Inline CSS linked via <link rel="stylesheet" href="...">
        val cssLinkRegex = Regex("""<link[^>]+rel=["']stylesheet["'][^>]+href=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        html = cssLinkRegex.replace(html) { match ->
            val href = match.groupValues[1]
            if (!href.startsWith("http://") && !href.startsWith("https://")) {
                val cssContent = fileManager.readFile(href).content
                if (cssContent != null) {
                    "<style>\n/* Inlined from $href */\n$cssContent\n</style>"
                } else {
                    warnings.add("Could not find stylesheet: $href")
                    match.value
                }
            } else {
                match.value
            }
        }

        // Inline local JS linked via <script src="..."></script>
        val scriptSrcRegex = Regex("""<script[^>]+src=["']([^"']+)["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE)
        html = scriptSrcRegex.replace(html) { match ->
            val src = match.groupValues[1]
            if (!src.startsWith("http://") && !src.startsWith("https://")) {
                val jsContent = fileManager.readFile(src).content
                if (jsContent != null) {
                    "<script>\n// Inlined from $src\n$jsContent\n</script>"
                } else {
                    warnings.add("Could not find script: $src")
                    match.value
                }
            } else {
                match.value
            }
        }

        return BuildResult(
            success = true,
            bundledHtml = html,
            warnings = warnings
        )
    }
}
