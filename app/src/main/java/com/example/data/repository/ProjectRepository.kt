package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.model.ProjectFileItem
import com.example.data.model.ProjectManifest
import com.example.data.storage.FileStorageManager
import com.example.data.storage.HtapkArchiveManager
import com.example.util.ShortcutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class ProjectRepository(
    private val context: Context,
    private val database: AppDatabase,
    val fileStorage: FileStorageManager,
    val archiveManager: HtapkArchiveManager
) {
    private val projectDao = database.projectDao()

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun searchProjects(query: String): Flow<List<ProjectEntity>> {
        return projectDao.searchProjects(query)
    }

    suspend fun getProjectEntity(id: String): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun getProjectManifest(id: String): ProjectManifest? = withContext(Dispatchers.IO) {
        fileStorage.readManifest(id)
    }

    suspend fun saveProject(manifest: ProjectManifest, initialHtml: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectId = manifest.meta.id
            fileStorage.writeManifest(manifest)

            if (initialHtml != null) {
                fileStorage.saveProjectFileContent(projectId, manifest.source.entry, initialHtml)
            }

            val files = fileStorage.listProjectFiles(projectId)
            val entity = ProjectEntity(
                id = projectId,
                name = manifest.meta.name,
                description = manifest.meta.description,
                version = manifest.meta.version,
                author = manifest.meta.author,
                createdAt = manifest.meta.createdAt,
                updatedAt = manifest.meta.updatedAt,
                tags = manifest.meta.tags.joinToString(","),
                entryPoint = manifest.source.entry,
                iconForeground = manifest.icon.foreground,
                iconBackground = manifest.icon.background,
                iconInitialLetter = manifest.icon.initialLetter,
                iconEmoji = manifest.icon.emoji,
                fullscreenMode = manifest.window.fullscreen,
                orientation = manifest.window.orientation,
                isPinned = false,
                hasPinLock = manifest.security.pinLock.enabled,
                isOfflineOnly = manifest.urlPolicy.offlineOnly,
                fileCount = files.size
            )
            projectDao.insertProject(entity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            fileStorage.deleteProject(id)
            projectDao.deleteProjectById(id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun duplicateProject(id: String, newName: String): String? = withContext(Dispatchers.IO) {
        try {
            val newId = fileStorage.duplicateProject(id, newName) ?: return@withContext null
            val manifest = fileStorage.readManifest(newId) ?: return@withContext null
            saveProject(manifest)
            newId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportProjectToStream(id: String, outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        archiveManager.exportToStream(id, outputStream)
    }

    suspend fun importProjectFromStream(inputStream: InputStream): Result<ProjectManifest> = withContext(Dispatchers.IO) {
        val result = archiveManager.importFromStream(inputStream)
        result.onSuccess { manifest ->
            saveProject(manifest)
        }
        result
    }

    suspend fun createDesktopShortcut(manifest: ProjectManifest): Boolean = withContext(Dispatchers.IO) {
        val iconDir = fileStorage.getProjectIconDir(manifest.meta.id)
        val success = ShortcutHelper.requestPinShortcut(context, manifest, iconDir)
        if (success) {
            projectDao.setPinned(manifest.meta.id, true)
        }
        success
    }

    suspend fun listProjectFiles(id: String): List<ProjectFileItem> = withContext(Dispatchers.IO) {
        fileStorage.listProjectFiles(id)
    }

    suspend fun readFileContent(id: String, relativePath: String): String? = withContext(Dispatchers.IO) {
        fileStorage.readProjectFileContent(id, relativePath)
    }

    suspend fun saveFileContent(id: String, relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val success = fileStorage.saveProjectFileContent(id, relativePath, content)
        if (success) {
            // Update entity file count and updatedAt
            val manifest = fileStorage.readManifest(id)
            if (manifest != null) {
                val updated = manifest.copy(meta = manifest.meta.copy(updatedAt = System.currentTimeMillis()))
                fileStorage.writeManifest(updated)
                saveProject(updated)
            }
        }
        success
    }

    suspend fun deleteFile(id: String, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val success = fileStorage.deleteProjectFile(id, relativePath)
        if (success) {
            val manifest = fileStorage.readManifest(id)
            if (manifest != null) {
                val updated = manifest.copy(meta = manifest.meta.copy(updatedAt = System.currentTimeMillis()))
                fileStorage.writeManifest(updated)
                saveProject(updated)
            }
        }
        success
    }
}
