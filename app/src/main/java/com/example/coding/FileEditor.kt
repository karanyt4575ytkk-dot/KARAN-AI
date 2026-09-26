package com.example.coding

class FileEditor(private val fileManager: FileManager) {

    fun replaceText(relativePath: String, target: String, replacement: String): FileOperationResult {
        val readResult = fileManager.readFile(relativePath)
        if (!readResult.success || readResult.content == null) {
            return readResult
        }
        if (!readResult.content.contains(target)) {
            return FileOperationResult(false, "replace_text", relativePath, "Target text was not found in $relativePath")
        }
        val updated = readResult.content.replace(target, replacement)
        return fileManager.updateFile(relativePath, updated)
    }

    fun appendText(relativePath: String, additionalContent: String): FileOperationResult {
        val readResult = fileManager.readFile(relativePath)
        val current = if (readResult.success && readResult.content != null) readResult.content else ""
        val updated = if (current.isEmpty()) additionalContent else "$current\n$additionalContent"
        return fileManager.updateFile(relativePath, updated)
    }

    fun insertAtLine(relativePath: String, lineNumber: Int, newText: String): FileOperationResult {
        val readResult = fileManager.readFile(relativePath)
        if (!readResult.success || readResult.content == null) {
            return readResult
        }
        val lines = readResult.content.lines().toMutableList()
        val index = (lineNumber - 1).coerceIn(0, lines.size)
        lines.add(index, newText)
        val updated = lines.joinToString("\n")
        return fileManager.updateFile(relativePath, updated)
    }
}
