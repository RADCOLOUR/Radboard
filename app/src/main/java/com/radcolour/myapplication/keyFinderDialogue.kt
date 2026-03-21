package com.radcolour.myapplication

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

class KeyFinderDialog(
    private val context: Context,
    private val onChordsHighlighted: (Set<String>) -> Unit,
    private val onDismiss: () -> Unit
) {

    private val notes = MusicTheory.ALL_NOTES
    private val scales = MusicTheory.SCALES.keys.toList()
    private var selectedRoot = "C"
    private var selectedScale = "Major"

    fun show() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 16)
            setBackgroundColor(0xFF1A1A1A.toInt())
        }

        layout.addView(TextView(context).apply {
            text = context.getString(R.string.key_finder_root)
            textSize = 10f
            setTextColor(0xFF8A8A8A.toInt())
            letterSpacing = 0.1f
            isAllCaps = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        })

        val rootScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 16 }
        }

        val rootRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
        }

        val rootButtons = mutableListOf<Button>()
        notes.forEach { note ->
            val btn = Button(context).apply {
                text = note
                textSize = 10f
                isAllCaps = false
                stateListAnimator = null
                layoutParams = LinearLayout.LayoutParams(56, 56).also { it.marginEnd = 6 }
            }
            updateRootButton(btn, note, note == selectedRoot)
            btn.setOnClickListener {
                selectedRoot = note
                rootButtons.forEach { b -> updateRootButton(b, b.text.toString(), b.text == note) }
            }
            rootButtons.add(btn)
            rootRow.addView(btn)
        }
        rootScroll.addView(rootRow)
        layout.addView(rootScroll)

        layout.addView(TextView(context).apply {
            text = context.getString(R.string.key_finder_scale)
            textSize = 10f
            setTextColor(0xFF8A8A8A.toInt())
            letterSpacing = 0.1f
            isAllCaps = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        })

        val scaleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 16 }
        }

        val scaleButtons = mutableListOf<Button>()
        scales.forEach { scale ->
            val btn = Button(context).apply {
                text = scale
                textSize = 10f
                isAllCaps = false
                stateListAnimator = null
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 40
                ).also { it.bottomMargin = 4 }
            }
            updateScaleButton(btn, scale, scale == selectedScale)
            btn.setOnClickListener {
                selectedScale = scale
                scaleButtons.forEach { b -> updateScaleButton(b, b.text.toString(), b.text == scale) }
            }
            scaleButtons.add(btn)
            scaleContainer.addView(btn)
        }
        layout.addView(scaleContainer)

        val tvScaleNotes = TextView(context).apply {
            textSize = 10f
            setTextColor(0xFF7DD6FF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }
        updateScaleNotes(tvScaleNotes)
        layout.addView(tvScaleNotes)

        rootButtons.forEach { btn ->
            btn.setOnClickListener {
                selectedRoot = btn.text.toString()
                rootButtons.forEach { b ->
                    updateRootButton(b, b.text.toString(), b.text == selectedRoot)
                }
                updateScaleNotes(tvScaleNotes)
            }
        }
        scaleButtons.forEach { btn ->
            btn.setOnClickListener {
                selectedScale = btn.text.toString()
                scaleButtons.forEach { b ->
                    updateScaleButton(b, b.text.toString(), b.text == selectedScale)
                }
                updateScaleNotes(tvScaleNotes)
            }
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.key_finder_title)
            .setView(layout)
            .setPositiveButton(R.string.btn_highlight) { _, _ ->
                val chords = MusicTheory.getChordsInKey(selectedRoot, selectedScale)
                onChordsHighlighted(chords)
            }
            .setNegativeButton(R.string.btn_clear) { _, _ ->
                onDismiss()
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
            .apply {
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xFFBFFFAA.toInt())
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xFFFF5449.toInt())
                getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(0xFF8A8A8A.toInt())
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    private fun updateRootButton(btn: Button, note: String, selected: Boolean) {
        btn.setTextColor(if (selected) 0xFF003549.toInt() else 0xFF7DD6FF.toInt())
        btn.backgroundTintList = if (selected)
            android.content.res.ColorStateList.valueOf(0xFF7DD6FF.toInt())
        else
            android.content.res.ColorStateList.valueOf(0xFF2B2B2B.toInt())
    }

    private fun updateScaleButton(btn: Button, scale: String, selected: Boolean) {
        btn.setTextColor(if (selected) 0xFF003549.toInt() else 0xFF7DD6FF.toInt())
        btn.backgroundTintList = if (selected)
            android.content.res.ColorStateList.valueOf(0xFF7DD6FF.toInt())
        else
            android.content.res.ColorStateList.valueOf(0xFF2B2B2B.toInt())
    }

    private fun updateScaleNotes(tv: TextView) {
        val notes = MusicTheory.getScaleNotes(selectedRoot, selectedScale)
        tv.text = context.getString(R.string.scale_notes_label, notes.joinToString(" — "))
    }
}