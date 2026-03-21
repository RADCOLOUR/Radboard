package com.radcolour.myapplication

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
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

    private val projectsRoot: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "radcolour/projects"
        )

    // -------------------------------------------------------------------------
    // Init — creates default project if none exist
    // -------------------------------------------------------------------------

    fun init(context: Context) {
        projectsRoot.mkdirs()
        if (getAllProjects().isEmpty()) {
            createProject(DEFAULT_PROJECT)
            setActiveProject(context, DEFAULT_PROJECT)
        }
        if (getActiveProject(context).isEmpty()) {
            setActiveProject(context, getAllProjects().first())
        }
    }

    // -------------------------------------------------------------------------
    // Active project
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Project CRUD
    // -------------------------------------------------------------------------

    fun getAllProjects(): List<String> {
        return projectsRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun createProject(name: String): Boolean {
        val dir = File(projectsRoot, name)
        if (dir.exists()) return false
        dir.mkdirs()
        writeInfo(name)
        getNotepadFile(name).writeText("")
        getProgressionFile(name).writeText("[]")
        return true
    }

    fun renameProject(context: Context, oldName: String, newName: String): Boolean {
        val oldDir = File(projectsRoot, oldName)
        val newDir = File(projectsRoot, newName)
        if (!oldDir.exists() || newDir.exists()) return false
        val renamed = oldDir.renameTo(newDir)
        if (renamed && getActiveProject(context) == oldName) {
            setActiveProject(context, newName)
        }
        return renamed
    }

    fun deleteProject(context: Context, name: String): Boolean {
        val dir = File(projectsRoot, name)
        if (!dir.exists()) return false
        dir.deleteRecursively()
        if (getActiveProject(context) == name) {
            val remaining = getAllProjects()
            if (remaining.isNotEmpty()) {
                setActiveProject(context, remaining.first())
            } else {
                createProject(DEFAULT_PROJECT)
                setActiveProject(context, DEFAULT_PROJECT)
            }
        }
        return true
    }

    // -------------------------------------------------------------------------
    // File accessors
    // -------------------------------------------------------------------------

    fun getNotepadFile(projectName: String): File {
        return File(File(projectsRoot, projectName), "notes.txt")
    }

    fun getProgressionFile(projectName: String): File {
        return File(File(projectsRoot, projectName), "progression.txt")
    }

    fun getInfoFile(projectName: String): File {
        return File(File(projectsRoot, projectName), "info.txt")
    }

    fun readNotepad(projectName: String): String {
        val file = getNotepadFile(projectName)
        return if (file.exists()) file.readText() else ""
    }

    fun writeNotepad(projectName: String, content: String) {
        val file = getNotepadFile(projectName)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun readProgression(projectName: String): String {
        val file = getProgressionFile(projectName)
        return if (file.exists()) file.readText() else "[]"
    }

    fun writeProgression(projectName: String, content: String) {
        val file = getProgressionFile(projectName)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun writeInfo(projectName: String) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val info = StringBuilder()
        info.appendLine("name=$projectName")
        info.appendLine("created=$date")
        info.appendLine("bpm=")
        info.appendLine("key=")
        info.appendLine("notes=")
        getInfoFile(projectName).writeText(info.toString())
    }

    // -------------------------------------------------------------------------
    // Export — zips the project folder
    // -------------------------------------------------------------------------

    fun exportProject(context: Context, projectName: String): File? {
        return try {
            val projectDir = File(projectsRoot, projectName)
            if (!projectDir.exists()) return null

            val exportDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "radcolour/exports"
            )
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

            zipFile
        } catch (e: Exception) {
            android.util.Log.e("ProjectManager", "Export failed: ${e.message}")
            null
        }
    }
}