package com.radcolour.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.radcolour.myapplication.SessionManager.sessionRunning
import com.radcolour.myapplication.SessionManager.sessionSeconds
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), CompanionManager.ConnectionListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sidebar: View
    private lateinit var btnMenu: ImageButton
    private lateinit var tvClock: TextView
    private lateinit var tvSessionTimer: TextView
    private lateinit var tvBpm: TextView
    private lateinit var btnSessionStart: Button
    private lateinit var btnSessionReset: Button
    private lateinit var btnTap: Button
    private lateinit var btnBpmReset: Button
    private lateinit var btnChords: LinearLayout
    private lateinit var btnNotepad: LinearLayout
    private lateinit var btnProgressions: LinearLayout
    private lateinit var btnSettings: LinearLayout
    private lateinit var tvActiveProject: TextView
    private lateinit var btnProjectPicker: LinearLayout
    private lateinit var btnProjectInfo: LinearLayout
    private lateinit var chipCompanion: LinearLayout
    private lateinit var overlayCompanion: LinearLayout

    private val REQUEST_PROJECT_PICKER = 2001

    private val handler = Handler(Looper.getMainLooper())
    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val tapTimes = mutableListOf<Long>()
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private val swipeThreshold = 100f

    private val clockRunnable = object : Runnable {
        override fun run() {
            tvClock.text = clockFormat.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    private val sessionRunnable = object : Runnable {
        override fun run() {
            if (SessionManager.sessionRunning) {
                SessionManager.sessionSeconds++
                updateSessionDisplay()
                handler.postDelayed(this, 1000)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        sidebar = findViewById(R.id.sidebar)
        btnMenu = findViewById(R.id.btnMenu)
        tvClock = findViewById(R.id.tvClock)
        tvSessionTimer = findViewById(R.id.tvSessionTimer)
        tvBpm = findViewById(R.id.tvBpm)
        btnSessionStart = findViewById(R.id.btnSessionStart)
        btnSessionReset = findViewById(R.id.btnSessionReset)
        btnTap = findViewById(R.id.btnTap)
        btnBpmReset = findViewById(R.id.btnBpmReset)
        btnChords = findViewById(R.id.btnChords)
        btnNotepad = findViewById(R.id.btnNotepad)
        btnProgressions = findViewById(R.id.btnProgressions)
        btnSettings = findViewById(R.id.btnSettings)
        btnProjectInfo = findViewById(R.id.btnProjectInfo)
        tvActiveProject = findViewById(R.id.tvActiveProject)
        btnProjectPicker = findViewById(R.id.btnProjectPicker)
        chipCompanion = findViewById(R.id.chipCompanion)
        overlayCompanion = findViewById(R.id.overlayCompanion)

        handler.post(clockRunnable)

        ProjectManager.init(this)
        CompanionManager.init(this)

        tvActiveProject.text = ProjectManager.getActiveProject(this)

        btnProjectPicker.setOnClickListener {
            startActivityForResult(
                Intent(this, ProjectPickerActivity::class.java),
                REQUEST_PROJECT_PICKER
            )
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(sidebar)) {
                drawerLayout.closeDrawer(sidebar)
            } else {
                drawerLayout.openDrawer(sidebar)
            }
        }

        btnSessionStart.setOnClickListener {
            if (SessionManager.sessionRunning) {
                SessionManager.sessionRunning = false
                btnSessionStart.text = getString(R.string.btn_stop)
            } else {
                SessionManager.sessionRunning = true
                btnSessionStart.text = getString(R.string.btn_stop)
                handler.post(sessionRunnable)
            }
        }

        btnSessionReset.setOnClickListener {
            sessionRunning = false
            sessionSeconds = 0L
            btnSessionStart.text = getString(R.string.btn_start)
            updateSessionDisplay()
        }

        btnTap.setOnClickListener {
            val now = System.currentTimeMillis()
            tapTimes.add(now)
            if (tapTimes.size > 8) tapTimes.removeAt(0)
            if (tapTimes.size >= 2) {
                val intervals = mutableListOf<Long>()
                for (i in 1 until tapTimes.size) {
                    intervals.add(tapTimes[i] - tapTimes[i - 1])
                }
                val avgInterval = intervals.average()
                val bpm = (60000 / avgInterval).toInt()
                tvBpm.text = bpm.toString()
            }
        }

        btnBpmReset.setOnClickListener {
            tapTimes.clear()
            tvBpm.text = getString(R.string.default_bpm)
        }

        btnChords.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, ChordsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnNotepad.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, NotepadActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnProgressions.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, ProgressionActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnProjectInfo.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, ProjectInfoActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnSettings.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, SettingsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        drawerLayout.setOnTouchListener { _, event ->
            handleSwipe(event)
            false
        }
    }

    override fun onResume() {
        super.onResume()
        CompanionManager.start(this)
    }

    override fun onPause() {
        super.onPause()
        CompanionManager.stop()
    }

    override fun onConnectionChanged(connected: Boolean) {
        val fullscreen = SettingsActivity.isCompanionFullscreenEnabled(this)
        chipCompanion.visibility = if (connected) View.VISIBLE else View.GONE
        overlayCompanion.visibility = if (connected && fullscreen) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROJECT_PICKER) {
            tvActiveProject.text = ProjectManager.getActiveProject(this)
        }
    }

    private fun handleSwipe(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartX = event.x
                swipeStartY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - swipeStartX
                val deltaY = event.y - swipeStartY
                if (deltaX > swipeThreshold
                    && Math.abs(deltaY) < swipeThreshold
                    && swipeStartX < 80f) {
                    drawerLayout.openDrawer(sidebar)
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateSessionDisplay() {
        val hours = sessionSeconds / 3600
        val minutes = (sessionSeconds % 3600) / 60
        val seconds = sessionSeconds % 60
        tvSessionTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
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