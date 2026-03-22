package com.radcolour.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

class ProjectPickerActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnNewProject: Button
    private lateinit var projectList: LinearLayout
    private lateinit var tvActiveProject: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_project_picker)

        ProjectManager.init(this)

        btnBack = findViewById(R.id.btnBack)
        btnNewProject = findViewById(R.id.btnNewProject)
        projectList = findViewById(R.id.projectList)
        tvActiveProject = findViewById(R.id.tvActiveProject)

        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnNewProject.setOnClickListener {
            showNewProjectDialog()
        }

        rebuildList()
    }

    private fun rebuildList() {
        projectList.removeAllViews()
        val active = ProjectManager.getActiveProject(this)
        tvActiveProject.text = getString(R.string.active_project_label, active)

        ProjectManager.getAllProjects().forEach { project ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = if (project == active)
                    getDrawable(R.drawable.bg_card_gradient_green)
                else
                    getDrawable(R.drawable.bg_card)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 56
                ).also { it.bottomMargin = 6 }
                setPadding(16, 0, 16, 0)
            }

            val tvName = TextView(this).apply {
                text = project
                textSize = 13f
                setTextColor(
                    if (project == active) 0xFFBFFFAA.toInt()
                    else 0xFFE6E6E6.toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val btnSelect = Button(this).apply {
                text = if (project == active)
                    getString(R.string.btn_active)
                else
                    getString(R.string.btn_select)
                textSize = 9f
                setTextColor(if (project == active) 0xFF000000.toInt() else 0xFF7DD6FF.toInt())
                background = getDrawable(R.drawable.bg_button_press)
                backgroundTintList = ContextCompat.getColorStateList(
                    context,
                    if (project == active) R.color.green else R.color.dark
                )
                stateListAnimator = null
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 36
                ).also { it.marginEnd = 8 }
            }

            val btnExport = Button(this).apply {
                text = getString(R.string.btn_export)
                textSize = 9f
                setTextColor(0xFF7DD6FF.toInt())
                background = getDrawable(R.drawable.bg_button_press)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark)
                stateListAnimator = null
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 36
                ).also { it.marginEnd = 8 }
            }

            val btnRename = Button(this).apply {
                text = getString(R.string.btn_rename)
                textSize = 9f
                setTextColor(0xFFFFE57A.toInt())
                background = getDrawable(R.drawable.bg_button_press)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark)
                stateListAnimator = null
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 36
                ).also { it.marginEnd = 8 }
            }

            val btnDelete = Button(this).apply {
                text = getString(R.string.btn_delete)
                textSize = 9f
                setTextColor(0xFFFF5449.toInt())
                background = getDrawable(R.drawable.bg_button_press)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark)
                stateListAnimator = null
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 36
                )
            }

            btnSelect.setOnClickListener {
                ProjectManager.setActiveProject(this, project)
                rebuildList()
                setResult(RESULT_OK)
            }

            btnExport.setOnClickListener {
                val zipFile = ProjectManager.exportProject(this, project)
                if (zipFile != null) {
                    val uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        zipFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.export_project_title)))
                } else {
                    Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
                }
            }

            btnRename.setOnClickListener {
                showRenameDialog(project)
            }

            btnDelete.setOnClickListener {
                if (ProjectManager.getAllProjects().size <= 1) {
                    Toast.makeText(this, getString(R.string.cannot_delete_last), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_project_title))
                    .setMessage(getString(R.string.delete_project_message, project))
                    .setPositiveButton(R.string.btn_delete) { _, _ ->
                        ProjectManager.deleteProject(this, project)
                        rebuildList()
                        setResult(RESULT_OK)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                    .apply {
                        getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFFF5449.toInt())
                        getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                        window?.setBackgroundDrawableResource(R.drawable.bg_card)
                    }
            }

            row.addView(tvName)
            row.addView(btnSelect)
            row.addView(btnExport)
            row.addView(btnRename)
            row.addView(btnDelete)
            projectList.addView(row)
        }

        if (ProjectManager.getAllProjects().size == 1) {
            projectList.addView(TextView(this).apply {
                text = getString(R.string.empty_projects_hint)
                textSize = 10f
                setTextColor(0xFF8A8A8A.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 16 }
            })
        }
    }

    private fun showNewProjectDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.project_name_hint)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF444444.toInt())
            setBackgroundColor(0xFF222222.toInt())
            textSize = 14f
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.new_project_title)
            .setView(input)
            .setPositiveButton(R.string.btn_create) { _, _ ->
                val name = input.text.toString().trim()
                when {
                    name.isEmpty() ->
                        Toast.makeText(this, getString(R.string.project_name_empty), Toast.LENGTH_SHORT).show()
                    ProjectManager.getAllProjects().contains(name) ->
                        Toast.makeText(this, getString(R.string.project_name_exists), Toast.LENGTH_SHORT).show()
                    else -> {
                        ProjectManager.createProject(name)
                        ProjectManager.setActiveProject(this, name)
                        rebuildList()
                        setResult(RESULT_OK)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFBFFFAA.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    private fun showRenameDialog(currentName: String) {
        val input = EditText(this).apply {
            setText(currentName)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF222222.toInt())
            textSize = 14f
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_project_title)
            .setView(input)
            .setPositiveButton(R.string.btn_rename) { _, _ ->
                val newName = input.text.toString().trim()
                when {
                    newName.isEmpty() ->
                        Toast.makeText(this, getString(R.string.project_name_empty), Toast.LENGTH_SHORT).show()
                    newName == currentName -> {}
                    ProjectManager.getAllProjects().contains(newName) ->
                        Toast.makeText(this, getString(R.string.project_name_exists), Toast.LENGTH_SHORT).show()
                    else -> {
                        ProjectManager.renameProject(this, currentName, newName)
                        rebuildList()
                        setResult(RESULT_OK)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFBFFFAA.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }
}