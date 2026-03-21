package com.radcolour.myapplication

import android.os.Bundle
import android.text.Spannable
import android.text.style.BulletSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotepadActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnBold: Button
    private lateinit var btnBullet: Button
    private lateinit var btnClear: Button
    private lateinit var etNotes: EditText

    private val PREFS_NAME = "notepad_prefs"
    private val KEY_NOTES = "notes_content"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_notepad)

        btnBack = findViewById(R.id.btnBack)
        btnBold = findViewById(R.id.btnBold)
        btnBullet = findViewById(R.id.btnBullet)
        btnClear = findViewById(R.id.btnClear)
        etNotes = findViewById(R.id.etNotes)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        etNotes.setText(prefs.getString(KEY_NOTES, ""))

        btnBack.setOnClickListener {
            saveNotes()
            finish()
        }

        btnBold.setOnClickListener {
            applyBold()
        }

        btnBullet.setOnClickListener {
            applyBullet()
        }

        btnClear.setOnClickListener {
            etNotes.text.clear()
            saveNotes()
        }
    }

    private fun applyBold() {
        val start = etNotes.selectionStart
        val end = etNotes.selectionEnd
        if (start == end) return

        val spannable = etNotes.text
        val existing = spannable.getSpans(start, end, StyleSpan::class.java)
            .filter { it.style == Typeface.BOLD }

        if (existing.isNotEmpty()) {
            existing.forEach { spannable.removeSpan(it) }
        } else {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyBullet() {
        val start = etNotes.selectionStart
        val end = etNotes.selectionEnd
        val spannable = etNotes.text

        val existing = spannable.getSpans(start, end, BulletSpan::class.java)
        if (existing.isNotEmpty()) {
            existing.forEach { spannable.removeSpan(it) }
        } else {
            spannable.setSpan(
                BulletSpan(16, 0xFFFFFFFF.toInt()),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun saveNotes() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTES, etNotes.text.toString())
            .apply()
    }

    override fun onPause() {
        super.onPause()
        saveNotes()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
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