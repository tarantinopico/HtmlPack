package com.example.data.storage

import android.content.Context
import com.example.data.model.ManifestModelsAdapterHelper
import com.example.data.model.ProjectManifest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class HtapkArchiveManager(
    private val context: Context,
    private val fileStorageManager: FileStorageManager
) {

    /**
     * Exports a project into an OutputStream (.htapk ZIP format)
     */
    fun exportToStream(projectId: String, outputStream: OutputStream): Boolean {
        return try {
            val projectDir = fileStorageManager.getProjectDir(projectId)
            val manifest = fileStorageManager.readManifest(projectId) ?: return false

            ZipOutputStream(outputStream).use { zos ->
                // 1. Write manifest.json at root of ZIP
                val manifestJson = ManifestModelsAdapterHelper.manifestToJson(manifest)
                val manifestEntry = ZipEntry("manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(manifestJson.toByteArray())
                zos.closeEntry()

                // 2. Write README.txt
                val readmeEntry = ZipEntry("README.txt")
                val readmeText = """
                    HTMLPack Application Archive (.htapk)
                    App: ${manifest.meta.name} (${manifest.meta.version})
                    Author: ${manifest.meta.author}
                    Entry: ${manifest.source.entry}
                    Created with HTMLPack for Android.
                """.trimIndent()
                zos.putNextEntry(readmeEntry)
                zos.write(readmeText.toByteArray())
                zos.closeEntry()

                // 3. Zip src/ folder
                val srcDir = fileStorageManager.getProjectSrcDir(projectId)
                if (srcDir.exists()) {
                    srcDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relPath = "src/" + file.relativeTo(srcDir).path.replace('\\', '/')
                        val entry = ZipEntry(relPath)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // 4. Zip icon/ folder if exists
                val iconDir = fileStorageManager.getProjectIconDir(projectId)
                if (iconDir.exists()) {
                    iconDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relPath = "icon/" + file.relativeTo(iconDir).path.replace('\\', '/')
                        val entry = ZipEntry(relPath)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // 5. Zip injections/ folder if exists
                val injDir = fileStorageManager.getProjectInjectionsDir(projectId)
                if (injDir.exists()) {
                    injDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relPath = "injections/" + file.relativeTo(injDir).path.replace('\\', '/')
                        val entry = ZipEntry(relPath)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // 6. Optional PWA manifest generation if configured
                if (manifest.pwa.generateWebManifest || manifest.pwa.includeInExport) {
                    val pwaEntry = ZipEntry("pwa/manifest.webmanifest")
                    val pwaJson = """
                        {
                          "name": "${manifest.meta.name}",
                          "short_name": "${manifest.meta.name.take(12)}",
                          "start_url": "./src/${manifest.source.entry}",
                          "display": "standalone",
                          "background_color": "${manifest.appearance.themeColorDark}",
                          "theme_color": "${manifest.appearance.themeColorLight}"
                        }
                    """.trimIndent()
                    zos.putNextEntry(pwaEntry)
                    zos.write(pwaJson.toByteArray())
                    zos.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Imports a .htapk ZIP from an InputStream into a project directory
     */
    fun importFromStream(
        inputStream: InputStream,
        forceNewId: Boolean = false
    ): Result<ProjectManifest> {
        val tempDir = File(context.cacheDir, "htapk_extract_${UUID.randomUUID()}").apply { mkdirs() }
        return try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val targetFile = File(tempDir, entry.name)

                    // Path traversal protection (Zip Slip)
                    if (!targetFile.canonicalPath.startsWith(tempDir.canonicalPath + File.separator)) {
                        throw SecurityException("Neplatná cesta v archivu: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Verify manifest.json
            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                return Result.failure(IllegalArgumentException("Archiv .htapk neobsahuje soubor manifest.json"))
            }

            val rawJson = manifestFile.readText()
            var manifest = ManifestModelsAdapterHelper.manifestFromJson(rawJson)
                ?: return Result.failure(IllegalArgumentException("Chyba při čtení manifest.json"))

            // Verify schema version
            if (manifest.meta.schemaVersion <= 0) {
                return Result.failure(IllegalArgumentException("Neznámá verze schématu manifestu"))
            }

            val targetId = if (forceNewId || fileStorageManager.readManifest(manifest.meta.id) != null) {
                UUID.randomUUID().toString()
            } else {
                manifest.meta.id
            }

            manifest = manifest.copy(
                meta = manifest.meta.copy(
                    id = targetId,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Move temp files to real project folder
            val destDir = fileStorageManager.getProjectDir(targetId)
            tempDir.copyRecursively(destDir, overwrite = true)
            fileStorageManager.writeManifest(manifest)

            Result.success(manifest)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
