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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sidebar: View
    private lateinit var btnMenu: Button
    private lateinit var tvClock: TextView
    private lateinit var tvSessionTimer: TextView
    private lateinit var tvBpm: TextView
    private lateinit var btnSessionStart: Button
    private lateinit var btnSessionReset: Button
    private lateinit var btnTap: Button
    private lateinit var btnBpmReset: Button
    private lateinit var btnChords: Button
    private lateinit var btnNotepad: Button
    private lateinit var btnProgressions: Button
    private lateinit var btnSettings: Button

    private val handler = Handler(Looper.getMainLooper())
    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private var sessionRunning = false
    private var sessionSeconds = 0L
    private val tapTimes = mutableListOf<Long>()
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private val swipeThreshold = 100f
    private val swipeVelocityThreshold = 50f

    private val clockRunnable = object : Runnable {
        override fun run() {
            tvClock.text = clockFormat.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    private val sessionRunnable = object : Runnable {
        override fun run() {
            if (sessionRunning) {
                sessionSeconds++
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
        btnSettings.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        handler.post(clockRunnable)
        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(sidebar)) {
                drawerLayout.closeDrawer(sidebar)
            } else {
                drawerLayout.openDrawer(sidebar)
            }
        }
        btnSessionStart.setOnClickListener {
            if (sessionRunning) {
                sessionRunning = false
                btnSessionStart.text = getString(R.string.btn_start)
            } else {
                sessionRunning = true
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
        }

        btnNotepad.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, NotepadActivity::class.java))
        }

        btnProgressions.setOnClickListener {
            drawerLayout.closeDrawer(sidebar)
            startActivity(Intent(this, ProgressionActivity::class.java))
        }

        drawerLayout.setOnTouchListener { _, event ->
            handleSwipe(event)
            false
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