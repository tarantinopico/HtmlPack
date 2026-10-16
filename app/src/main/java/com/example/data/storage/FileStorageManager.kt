package com.example.data.storage

import android.content.Context
import com.example.data.model.ManifestModelsAdapterHelper
import com.example.data.model.ProjectFileItem
import com.example.data.model.ProjectManifest
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FileStorageManager(private val context: Context) {

    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    fun getProjectDir(projectId: String): File {
        return File(projectsDir, projectId).apply { if (!exists()) mkdirs() }
    }

    fun getProjectSrcDir(projectId: String): File {
        return File(getProjectDir(projectId), "src").apply { if (!exists()) mkdirs() }
    }

    fun getProjectIconDir(projectId: String): File {
        return File(getProjectDir(projectId), "icon").apply { if (!exists()) mkdirs() }
    }

    fun getProjectInjectionsDir(projectId: String): File {
        return File(getProjectDir(projectId), "injections").apply { if (!exists()) mkdirs() }
    }

    fun writeManifest(manifest: ProjectManifest): Boolean {
        return try {
            val projectDir = getProjectDir(manifest.meta.id)
            val manifestFile = File(projectDir, "manifest.json")
            val json = ManifestModelsAdapterHelper.manifestToJson(manifest)
            manifestFile.writeText(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readManifest(projectId: String): ProjectManifest? {
        return try {
            val projectDir = File(projectsDir, projectId)
            val manifestFile = File(projectDir, "manifest.json")
            if (!manifestFile.exists()) return null
            val json = manifestFile.readText()
            ManifestModelsAdapterHelper.manifestFromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listProjectFiles(projectId: String): List<ProjectFileItem> {
        val srcDir = getProjectSrcDir(projectId)
        val files = mutableListOf<ProjectFileItem>()
        if (srcDir.exists()) {
            srcDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relPath = file.relativeTo(srcDir).path.replace('\\', '/')
                val mimeType = when {
                    file.name.endsWith(".html", true) -> "text/html"
                    file.name.endsWith(".css", true) -> "text/css"
                    file.name.endsWith(".js", true) -> "application/javascript"
                    file.name.endsWith(".json", true) -> "application/json"
                    file.name.endsWith(".png", true) -> "image/png"
                    file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) -> "image/jpeg"
                    file.name.endsWith(".svg", true) -> "image/svg+xml"
                    else -> "application/octet-stream"
                }
                files.add(
                    ProjectFileItem(
                        name = file.name,
                        relativePath = relPath,
                        size = file.length(),
                        isDirectory = false,
                        lastModified = file.lastModified(),
                        mimeType = mimeType
                    )
                )
            }
        }
        return files.sortedBy { it.relativePath }
    }

    fun readProjectFileContent(projectId: String, relativePath: String): String? {
        return try {
            val file = File(getProjectSrcDir(projectId), relativePath)
            if (file.exists() && file.isFile) {
                file.readText()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveProjectFileContent(projectId: String, relativePath: String, content: String): Boolean {
        return try {
            val file = File(getProjectSrcDir(projectId), relativePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveProjectFileBytes(projectId: String, relativePath: String, bytes: ByteArray): Boolean {
        return try {
            val file = File(getProjectSrcDir(projectId), relativePath)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(bytes) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteProjectFile(projectId: String, relativePath: String): Boolean {
        return try {
            val file = File(getProjectSrcDir(projectId), relativePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteProject(projectId: String): Boolean {
        return try {
            val dir = File(projectsDir, projectId)
            if (dir.exists()) dir.deleteRecursively() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun duplicateProject(sourceId: String, newName: String): String? {
        val srcManifest = readManifest(sourceId) ?: return null
        val newId = UUID.randomUUID().toString()
        val sourceDir = File(projectsDir, sourceId)
        val targetDir = File(projectsDir, newId)

        return try {
            sourceDir.copyRecursively(targetDir, overwrite = true)
            val updatedManifest = srcManifest.copy(
                meta = srcManifest.meta.copy(
                    id = newId,
                    name = newName,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            writeManifest(updatedManifest)
            newId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateTotalStorageSize(): Long {
        return if (projectsDir.exists()) {
            projectsDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
    }
}
