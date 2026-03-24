package com.radcolour.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ProjectInfoActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnEdit: Button
    private lateinit var btnTimerToggle: Button
    private lateinit var tvProjectName: TextView
    private lateinit var tvCreated: TextView
    private lateinit var tvKey: TextView
    private lateinit var tvScale: TextView
    private lateinit var tvBpm: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvTimeSpent: TextView
    private lateinit var projectName: String
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunning = false
    private var sessionSeconds = 0L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (timerRunning) {
                sessionSeconds++
                updateTimerDisplay()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateTimerDisplay()
            handler.postDelayed(this, 1000)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_project_info)

        ProjectManager.init(this)
        projectName = ProjectManager.getActiveProject(this)

        btnBack = findViewById(R.id.btnBack)
        btnEdit = findViewById(R.id.btnEdit)
        btnTimerToggle = findViewById(R.id.btnTimerToggle)
        tvProjectName = findViewById(R.id.tvProjectName)
        tvCreated = findViewById(R.id.tvCreated)
        tvKey = findViewById(R.id.tvKey)
        tvScale = findViewById(R.id.tvScale)
        tvBpm = findViewById(R.id.tvBpm)
        tvDescription = findViewById(R.id.tvDescription)
        tvTimeSpent = findViewById(R.id.tvTimeSpent)

        loadInfo()

        btnBack.setOnClickListener {
            saveSessionTime()
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnEdit.setOnClickListener {
            showEditDialog()
        }

        btnTimerToggle.setOnClickListener {
            if (timerRunning) {
                timerRunning = false
                btnTimerToggle.text = getString(R.string.btn_start)
                saveSessionTime()
                sessionSeconds = 0L
            } else {
                timerRunning = true
                btnTimerToggle.text = getString(R.string.btn_stop)
                handler.post(timerRunnable)
            }
        }
    }

    private fun loadInfo() {
        val info = ProjectManager.readInfo(this, projectName)
        tvProjectName.text = info.name
        tvCreated.text = if (info.created.isNotEmpty())
            getString(R.string.info_created, info.created)
        else
            getString(R.string.info_created, getString(R.string.info_unknown))
        tvKey.text = if (info.key.isNotEmpty())
            getString(R.string.info_key, info.key)
        else
            getString(R.string.info_key, getString(R.string.info_not_set))
        tvScale.text = if (info.scale.isNotEmpty())
            getString(R.string.info_scale, info.scale)
        else
            getString(R.string.info_scale, getString(R.string.info_not_set))
        tvBpm.text = if (info.bpm.isNotEmpty())
            getString(R.string.info_bpm, info.bpm)
        else
            getString(R.string.info_bpm, getString(R.string.info_not_set))
        tvDescription.text = if (info.description.isNotEmpty())
            info.description
        else
            getString(R.string.info_no_description)
        updateTotalTimeDisplay(info.timeSpentSeconds)
    }

    private fun saveSessionTime() {
        if (sessionSeconds > 0) {
            ProjectManager.addTimeSpent(this, projectName, sessionSeconds)
            sessionSeconds = 0L
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerDisplay() {
        val info = ProjectManager.readInfo(this, projectName)
        val total = info.timeSpentSeconds + SessionManager.sessionSeconds
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        tvTimeSpent.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        btnTimerToggle.text = if (SessionManager.sessionRunning)
            getString(R.string.btn_stop)
        else
            getString(R.string.btn_start)
    }

    @SuppressLint("DefaultLocale")
    private fun updateTotalTimeDisplay(totalSeconds: Long) {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        tvTimeSpent.text = getString(
            R.string.info_time_spent,
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        )
    }



    private fun showEditDialog() {
        val info = ProjectManager.readInfo(this, projectName)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            setBackgroundColor(0xFF1A1A1A.toInt())
        }

        fun makeField(hint: String, value: String): EditText {
            return EditText(this).apply {
                setText(value)
                this.hint = hint
                setTextColor(0xFFFFFFFF.toInt())
                setHintTextColor(0xFF444444.toInt())
                setBackgroundColor(0xFF2B2B2B.toInt())
                textSize = 13f
                setPadding(16, 12, 16, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 8 }
            }
        }

        val etKey = makeField(getString(R.string.info_field_key), info.key)
        val etScale = makeField(getString(R.string.info_field_scale), info.scale)
        val etBpm = makeField(getString(R.string.info_field_bpm), info.bpm)
        val etDescription = makeField(getString(R.string.info_field_description), info.description)

        layout.addView(etKey)
        layout.addView(etScale)
        layout.addView(etBpm)
        layout.addView(etDescription)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_project_info_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val updated = info.copy(
                    key = etKey.text.toString().trim(),
                    scale = etScale.text.toString().trim(),
                    bpm = etBpm.text.toString().trim(),
                    description = etDescription.text.toString().trim()
                )
                ProjectManager.writeInfo(this, projectName, updated)
                loadInfo()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFBFFFAA.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
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