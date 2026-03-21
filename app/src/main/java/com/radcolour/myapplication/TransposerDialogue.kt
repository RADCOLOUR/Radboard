package com.radcolour.myapplication

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.widget.*
import androidx.core.content.ContextCompat

class TransposerDialog(
    private val context: Context,
    private val chordData: Map<String, ChordsActivity.ChordInfo>,
    private val currentProgression: List<String>,
    private val onProgressionTransposed: (List<String>) -> Unit
) {

    private val notes = MusicTheory.ALL_NOTES
    private var mode = "single" // "single" or "progression"
    private var selectedChord = ""
    private var selectedFromRoot = "C"
    private var selectedToRoot = "G"
    private var result = ""

    fun show() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 16)
            setBackgroundColor(0xFF1A1A1A.toInt())
        }

        // Mode toggle
        val modeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 40
            ).also { it.bottomMargin = 16 }
            setBackgroundColor(0xFF2B2B2B.toInt())
        }

        val btnSingle = Button(context).apply {
            text = context.getString(R.string.transposer_single)
            textSize = 10f
            isAllCaps = false
            stateListAnimator = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        val btnProgression = Button(context).apply {
            text = context.getString(R.string.transposer_progression)
            textSize = 10f
            isAllCaps = false
            stateListAnimator = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        modeRow.addView(btnSingle)
        modeRow.addView(btnProgression)
        layout.addView(modeRow)

        // Content area — swaps between single and progression mode
        val contentArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(contentArea)

        // Result text
        val tvResult = TextView(context).apply {
            textSize = 12f
            setTextColor(0xFFFFB3D9.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 16 }
        }
        layout.addView(tvResult)

        // Build single mode UI
        fun buildSingleMode() {
            contentArea.removeAllViews()
            mode = "single"
            updateModeButtons(btnSingle, btnProgression)

            // Chord picker label
            contentArea.addView(TextView(context).apply {
                text = context.getString(R.string.transposer_pick_chord)
                textSize = 10f
                setTextColor(0xFF8A8A8A.toInt())
                isAllCaps = true
                letterSpacing = 0.1f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 8 }
            })

            // Chord spinner
            val chords = chordData.keys.sorted().toMutableList()
            val spinner = Spinner(context).apply {
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, chords).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 16 }
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                        selectedChord = chords[pos]
                        val transposed = MusicTheory.transposeChord(selectedChord, selectedFromRoot, selectedToRoot)
                        tvResult.text = context.getString(R.string.transposer_result_single, selectedChord, transposed)
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
            }
            if (chords.isNotEmpty()) selectedChord = chords[0]
            contentArea.addView(spinner)

            // From/To root row
            contentArea.addView(buildRootRow(tvResult))
        }

        // Build progression mode UI
        fun buildProgressionMode() {
            contentArea.removeAllViews()
            mode = "progression"
            updateModeButtons(btnProgression, btnSingle)

            if (currentProgression.isEmpty()) {
                contentArea.addView(TextView(context).apply {
                    text = context.getString(R.string.transposer_no_progression)
                    textSize = 11f
                    setTextColor(0xFF8A8A8A.toInt())
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = 8 }
                })
                return
            }

            // Show current progression
            contentArea.addView(TextView(context).apply {
                text = context.getString(R.string.transposer_current, currentProgression.joinToString(" → "))
                textSize = 10f
                setTextColor(0xFF7DD6FF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 16 }
            })

            // Auto detect key
            val detected = MusicTheory.detectKey(currentProgression)
            if (detected != null) {
                selectedFromRoot = detected.first
                contentArea.addView(TextView(context).apply {
                    text = context.getString(R.string.transposer_detected_key, detected.first, detected.second)
                    textSize = 10f
                    setTextColor(0xFF8A8A8A.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 8 }
                })
            }

            contentArea.addView(buildRootRow(tvResult, isProgression = true))
        }

        btnSingle.setOnClickListener { buildSingleMode() }
        btnProgression.setOnClickListener { buildProgressionMode() }

        // Start in single mode
        buildSingleMode()

        AlertDialog.Builder(context)
            .setTitle(R.string.transposer_title)
            .setView(layout)
            .setPositiveButton(R.string.btn_transpose) { _, _ ->
                if (mode == "progression" && currentProgression.isNotEmpty()) {
                    val semitones = MusicTheory.semitoneseBetween(selectedFromRoot, selectedToRoot)
                    val transposed = MusicTheory.transposeChords(currentProgression, semitones)
                    onProgressionTransposed(transposed)
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

    private fun buildRootRow(tvResult: TextView, isProgression: Boolean = false): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // From label
        row.addView(TextView(context).apply {
            text = context.getString(R.string.transposer_from)
            textSize = 10f
            setTextColor(0xFF8A8A8A.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = 8 }
        })

        // From spinner
        val fromSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, notes).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(notes.indexOf(selectedFromRoot))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    selectedFromRoot = notes[pos]
                    updateResult(tvResult, isProgression)
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        row.addView(fromSpinner)

        // Arrow
        row.addView(TextView(context).apply {
            text = " → "
            textSize = 14f
            setTextColor(0xFFFFB3D9.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        // To spinner
        val toSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, notes).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(notes.indexOf(selectedToRoot))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    selectedToRoot = notes[pos]
                    updateResult(tvResult, isProgression)
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        row.addView(toSpinner)

        return row
    }

    private fun updateResult(tvResult: TextView, isProgression: Boolean) {
        if (isProgression && currentProgression.isNotEmpty()) {
            val semitones = MusicTheory.semitoneseBetween(selectedFromRoot, selectedToRoot)
            val transposed = MusicTheory.transposeChords(currentProgression, semitones)
            tvResult.text = context.getString(
                R.string.transposer_result_progression,
                transposed.joinToString(" → ")
            )
        } else if (!isProgression && selectedChord.isNotEmpty()) {
            val transposed = MusicTheory.transposeChord(selectedChord, selectedFromRoot, selectedToRoot)
            tvResult.text = context.getString(R.string.transposer_result_single, selectedChord, transposed)
        }
    }

    private fun updateModeButtons(active: Button, inactive: Button) {
        active.setTextColor(0xFF003549.toInt())
        active.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF7DD6FF.toInt())
        inactive.setTextColor(0xFF7DD6FF.toInt())
        inactive.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2B2B2B.toInt())
    }
}