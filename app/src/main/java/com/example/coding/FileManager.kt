package com.example.coding

import java.io.File

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val children: List<FileNode> = emptyList()
)

data class FileOperationResult(
    val success: Boolean,
    val action: String,
    val path: String,
    val message: String,
    val content: String? = null
)

class FileManager(private val baseDir: File) {

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    private fun resolveSafeFile(relativePath: String): File {
        val clean = relativePath.trim().removePrefix("/").replace("..", "")
        return File(baseDir, clean)
    }

    fun listFiles(relativePath: String = ""): List<FileNode> {
        val target = resolveSafeFile(relativePath)
        if (!target.exists()) return emptyList()
        return listNodeRecursive(target)
    }

    private fun listNodeRecursive(file: File): List<FileNode> {
        val children = file.listFiles() ?: return emptyList()
        return children.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { child ->
                FileNode(
                    name = child.name,
                    path = child.relativeTo(baseDir).path.replace("\\", "/"),
                    isDirectory = child.isDirectory,
                    size = if (child.isFile) child.length() else 0,
                    children = if (child.isDirectory) listNodeRecursive(child) else emptyList()
                )
            }
    }

    fun readFile(relativePath: String): FileOperationResult {
        val target = resolveSafeFile(relativePath)
        if (!target.exists()) {
            return FileOperationResult(false, "read", relativePath, "File does not exist: $relativePath")
        }
        if (target.isDirectory) {
            return FileOperationResult(false, "read", relativePath, "Cannot read directory as text: $relativePath")
        }
        return try {
            val content = target.readText(Charsets.UTF_8)
            FileOperationResult(true, "read", relativePath, "File read successfully", content = content)
        } catch (e: Exception) {
            FileOperationResult(false, "read", relativePath, "Error reading file: ${e.message}")
        }
    }

    fun createFile(relativePath: String, content: String = ""): FileOperationResult {
        val target = resolveSafeFile(relativePath)
        return try {
            target.parentFile?.mkdirs()
            target.writeText(content, Charsets.UTF_8)
            FileOperationResult(true, "create_file", relativePath, "File created: $relativePath", content = content)
        } catch (e: Exception) {
            FileOperationResult(false, "create_file", relativePath, "Error creating file: ${e.message}")
        }
    }

    fun updateFile(relativePath: String, content: String): FileOperationResult {
        val target = resolveSafeFile(relativePath)
        return try {
            target.parentFile?.mkdirs()
            target.writeText(content, Charsets.UTF_8)
            FileOperationResult(true, "update_file", relativePath, "File updated: $relativePath", content = content)
        } catch (e: Exception) {
            FileOperationResult(false, "update_file", relativePath, "Error updating file: ${e.message}")
        }
    }

    fun createFolder(relativePath: String): FileOperationResult {
        val target = resolveSafeFile(relativePath)
        return try {
            val created = target.mkdirs()
            FileOperationResult(true, "create_folder", relativePath, "Directory created: $relativePath")
        } catch (e: Exception) {
            FileOperationResult(false, "create_folder", relativePath, "Error creating directory: ${e.message}")
        }
    }

    fun renameFile(oldRelativePath: String, newRelativePath: String): FileOperationResult {
        val source = resolveSafeFile(oldRelativePath)
        val dest = resolveSafeFile(newRelativePath)
        if (!source.exists()) {
            return FileOperationResult(false, "rename", oldRelativePath, "Source does not exist")
        }
        return try {
            dest.parentFile?.mkdirs()
            val ok = source.renameTo(dest)
            if (ok) {
                FileOperationResult(true, "rename", newRelativePath, "Renamed $oldRelativePath to $newRelativePath")
            } else {
                FileOperationResult(false, "rename", oldRelativePath, "Rename operation failed")
            }
        } catch (e: Exception) {
            FileOperationResult(false, "rename", oldRelativePath, "Error renaming: ${e.message}")
        }
    }

    fun deleteFile(relativePath: String): FileOperationResult {
        val target = resolveSafeFile(relativePath)
        if (!target.exists()) {
            return FileOperationResult(false, "delete", relativePath, "Target does not exist")
        }
        return try {
            val ok = if (target.isDirectory) target.deleteRecursively() else target.delete()
            if (ok) {
                FileOperationResult(true, "delete", relativePath, "Deleted: $relativePath")
            } else {
                FileOperationResult(false, "delete", relativePath, "Failed to delete: $relativePath")
            }
        } catch (e: Exception) {
            FileOperationResult(false, "delete", relativePath, "Error deleting: ${e.message}")
        }
    }
}
