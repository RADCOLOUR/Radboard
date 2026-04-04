package com.radcolour.myapplication

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var switchPreRelease: Switch
    private lateinit var switchCompanionFullscreen: Switch
    private lateinit var btnClearChords: Button
    private lateinit var tvVersion: TextView
    private lateinit var cardVersion: View

    companion object {
        const val PREFS_NAME = "radboard_settings"
        const val KEY_PRERELEASE = "enable_prerelease"
        const val KEY_COMPANION_FULLSCREEN = "companion_fullscreen"

        fun isPreReleaseEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PRERELEASE, false)
        }

        fun isCompanionFullscreenEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_COMPANION_FULLSCREEN, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_settings)

        ChordRepository.init(this)

        btnBack = findViewById(R.id.btnBack)
        switchPreRelease = findViewById(R.id.switchPreRelease)
        switchCompanionFullscreen = findViewById(R.id.switchCompanionFullscreen)
        btnClearChords = findViewById(R.id.btnClearChords)
        tvVersion = findViewById(R.id.tvVersion)
        cardVersion = findViewById(R.id.cardVersion)

        tvVersion.text = getString(R.string.settings_version_value, UpdateManager.getCurrentVersion(this))

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        switchPreRelease.isChecked = prefs.getBoolean(KEY_PRERELEASE, false)
        switchPreRelease.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_PRERELEASE, isChecked).apply()
        }

        switchCompanionFullscreen.isChecked = prefs.getBoolean(KEY_COMPANION_FULLSCREEN, false)
        switchCompanionFullscreen.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_COMPANION_FULLSCREEN, isChecked).apply()
        }

        btnClearChords.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.settings_clear_chords_confirm_title)
                .setMessage(R.string.settings_clear_chords_confirm_message)
                .setPositiveButton(R.string.btn_clear) { _, _ ->
                    ChordRepository.clearImported(this)
                    Toast.makeText(this, getString(R.string.settings_chords_cleared), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
                .apply {
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFFF5449.toInt())
                    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFF8A8A8A.toInt())
                    window?.setBackgroundDrawableResource(R.drawable.bg_card)
                }
        }

        cardVersion.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("version", UpdateManager.getCurrentVersion(this))
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.settings_version_copied), Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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