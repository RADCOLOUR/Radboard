package com.radcolour.myapplication

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ProjectManager {

    private const val PREFS_NAME = "project_manager"
    private const val KEY_ACTIVE_PROJECT = "active_project"
    private const val DEFAULT_PROJECT = "Untitled Project"

    data class ProjectInfo(
        val name: String,
        val created: String,
        val bpm: String,
        val key: String,
        val scale: String,
        val description: String,
        val timeSpentSeconds: Long
    )

    private fun getProjectsRoot(context: Context): File {
        return File(context.getExternalFilesDir(null), "projects")
    }

    fun init(context: Context) {
        getProjectsRoot(context).mkdirs()
        if (getAllProjects(context).isEmpty()) {
            createProject(context, DEFAULT_PROJECT)
            setActiveProject(context, DEFAULT_PROJECT)
        }
        if (getActiveProject(context).isEmpty()) {
            setActiveProject(context, getAllProjects(context).first())
        }
    }

    fun getActiveProject(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_PROJECT, "") ?: ""
    }

    fun setActiveProject(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_PROJECT, name)
            .apply()
    }

    fun getAllProjects(context: Context): List<String> {
        return getProjectsRoot(context).listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun createProject(context: Context, name: String): Boolean {
        val dir = File(getProjectsRoot(context), name)
        if (dir.exists()) return false
        dir.mkdirs()
        writeInfo(context, name, ProjectInfo(
            name = name,
            created = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            bpm = "",
            key = "",
            scale = "",
            description = "",
            timeSpentSeconds = 0
        ))
        getNotepadFile(context, name).writeText("")
        getProgressionFile(context, name).writeText("[]")
        return true
    }

    fun renameProject(context: Context, oldName: String, newName: String): Boolean {
        val oldDir = File(getProjectsRoot(context), oldName)
        val newDir = File(getProjectsRoot(context), newName)
        if (!oldDir.exists() || newDir.exists()) return false
        val renamed = oldDir.renameTo(newDir)
        if (renamed && getActiveProject(context) == oldName) {
            setActiveProject(context, newName)
        }
        return renamed
    }

    fun deleteProject(context: Context, name: String): Boolean {
        val dir = File(getProjectsRoot(context), name)
        if (!dir.exists()) return false
        dir.deleteRecursively()
        if (getActiveProject(context) == name) {
            val remaining = getAllProjects(context)
            if (remaining.isNotEmpty()) {
                setActiveProject(context, remaining.first())
            } else {
                createProject(context, DEFAULT_PROJECT)
                setActiveProject(context, DEFAULT_PROJECT)
            }
        }
        return true
    }

    fun getNotepadFile(context: Context, projectName: String): File {
        return File(File(getProjectsRoot(context), projectName), "notes.txt")
    }

    fun getProgressionFile(context: Context, projectName: String): File {
        return File(File(getProjectsRoot(context), projectName), "progression.txt")
    }

    fun getInfoFile(context: Context, projectName: String): File {
        return File(File(getProjectsRoot(context), projectName), "info.txt")
    }

    fun readNotepad(context: Context, projectName: String): String {
        val file = getNotepadFile(context, projectName)
        return if (file.exists()) file.readText() else ""
    }

    fun writeNotepad(context: Context, projectName: String, content: String) {
        val file = getNotepadFile(context, projectName)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun readProgression(context: Context, projectName: String): String {
        val file = getProgressionFile(context, projectName)
        return if (file.exists()) file.readText() else "[]"
    }

    fun writeProgression(context: Context, projectName: String, content: String) {
        val file = getProgressionFile(context, projectName)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun readInfo(context: Context, projectName: String): ProjectInfo {
        val file = getInfoFile(context, projectName)
        val defaults = ProjectInfo(
            name = projectName,
            created = "",
            bpm = "",
            key = "",
            scale = "",
            description = "",
            timeSpentSeconds = 0
        )
        if (!file.exists()) return defaults

        val map = mutableMapOf<String, String>()
        file.readLines().forEach { line ->
            val idx = line.indexOf('=')
            if (idx >= 0) {
                map[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
        }

        return ProjectInfo(
            name = map["name"] ?: projectName,
            created = map["created"] ?: "",
            bpm = map["bpm"] ?: "",
            key = map["key"] ?: "",
            scale = map["scale"] ?: "",
            description = map["description"] ?: "",
            timeSpentSeconds = map["time_spent"]?.toLongOrNull() ?: 0
        )
    }

    fun writeInfo(context: Context, projectName: String, info: ProjectInfo) {
        val file = getInfoFile(context, projectName)
        file.parentFile?.mkdirs()
        val sb = StringBuilder()
        sb.appendLine("name=${info.name}")
        sb.appendLine("created=${info.created}")
        sb.appendLine("bpm=${info.bpm}")
        sb.appendLine("key=${info.key}")
        sb.appendLine("scale=${info.scale}")
        sb.appendLine("description=${info.description}")
        sb.appendLine("time_spent=${info.timeSpentSeconds}")
        file.writeText(sb.toString())
    }

    fun addTimeSpent(context: Context, projectName: String, seconds: Long) {
        val info = readInfo(context, projectName)
        writeInfo(context, projectName, info.copy(
            timeSpentSeconds = info.timeSpentSeconds + seconds
        ))
    }

    fun exportProject(context: Context, projectName: String): File? {
        return try {
            val projectDir = File(getProjectsRoot(context), projectName)
            if (!projectDir.exists()) return null

            val exportDir = File(context.getExternalFilesDir(null), "exports")
            exportDir.mkdirs()

            val zipFile = File(exportDir, "$projectName.zip")
            if (zipFile.exists()) zipFile.delete()

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                projectDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entryName = "$projectName/${file.relativeTo(projectDir).path}"
                        zos.putNextEntry(ZipEntry(entryName))
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(zipFile.absolutePath),
                arrayOf("application/zip"),
                null
            )

            zipFile
        } catch (e: Exception) {
            android.util.Log.e("ProjectManager", "Export failed: ${e.message}")
            null
        }
    }
}