package com.radcolour.myapplication

import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BulletSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class NotepadActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnBold: Button
    private lateinit var btnBullet: Button
    private lateinit var btnClear: Button
    private lateinit var etNotes: EditText
    private lateinit var emptyStateNotepad: EmptyStateView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_notepad)

        ProjectManager.init(this)

        btnBack = findViewById(R.id.btnBack)
        btnBold = findViewById(R.id.btnBold)
        btnBullet = findViewById(R.id.btnBullet)
        btnClear = findViewById(R.id.btnClear)
        etNotes = findViewById(R.id.etNotes)
        emptyStateNotepad = findViewById(R.id.emptyStateNotepad)

        emptyStateNotepad.setup(
            iconRes = R.drawable.ic_empty_notepad,
            title = getString(R.string.empty_notepad_title),
            subtitle = getString(R.string.empty_notepad_subtitle),
            accentColour = 0xFFFFB3D9.toInt()
        )

        loadNotes()
        updateEmptyState()

        etNotes.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateEmptyState() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnBack.setOnClickListener {
            saveNotes()
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
            updateEmptyState()
        }
    }

    private fun updateEmptyState() {
        val isEmpty = etNotes.text.isEmpty()
        emptyStateNotepad.visibility = if (isEmpty) View.VISIBLE else View.GONE
        etNotes.setBackgroundResource(if (isEmpty) android.R.color.transparent else R.drawable.bg_card_gradient_blue)
    }

    private fun loadNotes() {
        val project = ProjectManager.getActiveProject(this)
        etNotes.setText(ProjectManager.readNotepad(project))
    }

    private fun saveNotes() {
        val project = ProjectManager.getActiveProject(this)
        ProjectManager.writeNotepad(project, etNotes.text.toString())
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

    override fun onPause() {
        super.onPause()
        saveNotes()
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