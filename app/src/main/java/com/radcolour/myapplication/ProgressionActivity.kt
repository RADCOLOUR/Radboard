package com.radcolour.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

class ProgressionActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnAddSection: Button
    private lateinit var sectionsContainer: LinearLayout
    private lateinit var tvSongTitle: TextView

    private val PREFS_NAME = "progression_prefs"
    private val KEY_SECTIONS = "sections"

    private val defaultSectionTypes = listOf(
        "Intro", "Verse", "Pre-Chorus", "Chorus", "Bridge", "Outro"
    )

    data class Section(
        val name: String,
        val chords: MutableList<String> = mutableListOf()
    )

    private val sections = mutableListOf<Section>()

    // Height for each fretboard diagram in the diagrams row
    private val diagramHeightDp = 160

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_progression)

        ChordRepository.init(this)

        btnBack = findViewById(R.id.btnBack)
        btnAddSection = findViewById(R.id.btnAddSection)
        sectionsContainer = findViewById(R.id.sectionsContainer)
        tvSongTitle = findViewById(R.id.tvSongTitle)

        loadSections()
        rebuildUI()

        btnBack.setOnClickListener {
            saveSections()
            finish()
        }

        btnAddSection.setOnClickListener {
            showAddSectionDialog()
        }
    }

    // -------------------------------------------------------------------------
    // Section dialogs
    // -------------------------------------------------------------------------

    private fun showAddSectionDialog() {
        val options = (defaultSectionTypes + listOf("Custom...")).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Add Section")
            .setItems(options) { _, which ->
                if (which == options.size - 1) {
                    showCustomSectionDialog()
                } else {
                    addSection(options[which])
                }
            }
            .show()
    }

    private fun showCustomSectionDialog() {
        val input = EditText(this).apply {
            hint = "Section name"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF444444.toInt())
            setBackgroundColor(0xFF222222.toInt())
            textSize = 14f
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Custom Section")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) addSection(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addSection(name: String) {
        sections.add(Section(name))
        saveSections()
        rebuildUI()
    }

    private fun showAddChordDialog(section: Section, onAdded: () -> Unit) {
        val chords = ChordRepository.getAllChords().keys.sorted().toTypedArray()
        if (chords.isEmpty()) {
            Toast.makeText(this, "No chords in library", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Add Chord to ${section.name}")
            .setItems(chords) { _, which ->
                section.chords.add(chords[which])
                saveSections()
                onAdded()
            }
            .show()
    }

    // -------------------------------------------------------------------------
    // UI builder
    // -------------------------------------------------------------------------

    private fun rebuildUI() {
        sectionsContainer.removeAllViews()
        sections.forEach { section ->
            sectionsContainer.addView(buildSectionView(section))
        }
    }

    private fun buildSectionView(section: Section): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 4 }
        }

        // Section header row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 40
            )
            setBackgroundColor(0xFF1A1A1A.toInt())
            setPadding(12, 0, 12, 0)
        }

        val tvName = TextView(this).apply {
            text = section.name
            textSize = 12f
            setTextColor(0xFFFFBDE1.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
            ).also { it.gravity = android.view.Gravity.CENTER_VERTICAL }
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val btnAddChord = Button(this).apply {
            text = "+ Chord"
            textSize = 9f
            setTextColor(0xFF000000.toInt())
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.green)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 32
            ).also { it.marginEnd = 8 }
        }

        val btnDeleteSection = Button(this).apply {
            text = "✕"
            textSize = 9f
            setTextColor(0xFFFF4444.toInt())
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 32
            )
        }

        headerRow.addView(tvName)
        headerRow.addView(btnAddChord)
        headerRow.addView(btnDeleteSection)
        container.addView(headerRow)

        // Chord chips row (names)
        val chipsScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(0xFF0A0A0A.toInt())
        }

        val chipsInner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 4, 8, 4)
        }
        chipsScroll.addView(chipsInner)
        container.addView(chipsScroll)

        // Fretboard diagrams row
        val diagramsScroll = HorizontalScrollView(this).apply {
            val heightPx = (diagramHeightDp * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx
            )
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(0xFF050505.toInt())
        }

        val diagramsInner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(8, 4, 8, 4)
        }
        diagramsScroll.addView(diagramsInner)
        container.addView(diagramsScroll)

        // Divider
        container.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(0xFF222222.toInt())
        })

        // Populate chips and diagrams
        rebuildSectionContent(section, chipsInner, diagramsInner)

        // Wire buttons
        btnAddChord.setOnClickListener {
            showAddChordDialog(section) {
                rebuildSectionContent(section, chipsInner, diagramsInner)
            }
        }

        btnDeleteSection.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Section")
                .setMessage("Delete \"${section.name}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    sections.remove(section)
                    saveSections()
                    rebuildUI()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        return container
    }

    private fun rebuildSectionContent(
        section: Section,
        chipsInner: LinearLayout,
        diagramsInner: LinearLayout
    ) {
        chipsInner.removeAllViews()
        diagramsInner.removeAllViews()

        if (section.chords.isEmpty()) {
            // Empty state in chips row
            chipsInner.addView(TextView(this).apply {
                text = "No chords yet — tap + Chord to add"
                textSize = 10f
                setTextColor(0xFF444444.toInt())
                setPadding(8, 0, 0, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            })
            return
        }

        section.chords.forEachIndexed { index, chordName ->

            // Chip
            val chip = Button(this).apply {
                text = chordName
                textSize = 10f
                setTextColor(0xFF8BDCFF.toInt())
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 36
                ).also { it.marginEnd = 6 }
            }
            chip.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Remove Chord")
                    .setMessage("Remove \"$chordName\" from ${section.name}?")
                    .setPositiveButton("Remove") { _, _ ->
                        section.chords.removeAt(index)
                        saveSections()
                        rebuildSectionContent(section, chipsInner, diagramsInner)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            chipsInner.addView(chip)

            // Fretboard diagram
            val chordInfo = ChordRepository.getAllChords()[chordName]
            val diagramContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    (diagramHeightDp * 0.75 * resources.displayMetrics.density).toInt(),
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).also { it.marginEnd = 8 }
            }

            // Chord name label above diagram
            val tvLabel = TextView(this).apply {
                text = chordName
                textSize = 9f
                setTextColor(0xFF8BDCFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            diagramContainer.addView(tvLabel)

            if (chordInfo != null && chordInfo.guitarPositions.isNotEmpty()) {
                val fretboard = FretboardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    setPosition(chordInfo.guitarPositions[0], 6)
                }
                diagramContainer.addView(fretboard)
            } else {
                // Chord not found in library
                val tvMissing = TextView(this).apply {
                    text = "?"
                    textSize = 20f
                    setTextColor(0xFF444444.toInt())
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                }
                diagramContainer.addView(tvMissing)
            }

            diagramsInner.addView(diagramContainer)
        }
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private fun saveSections() {
        val array = JSONArray()
        sections.forEach { section ->
            val obj = JSONObject()
            obj.put("name", section.name)
            val chords = JSONArray()
            section.chords.forEach { chords.put(it) }
            obj.put("chords", chords)
            array.put(obj)
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_SECTIONS, array.toString())
            .apply()
    }

    private fun loadSections() {
        sections.clear()
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(KEY_SECTIONS, null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val chordsArray = obj.getJSONArray("chords")
                val chords = (0 until chordsArray.length())
                    .map { chordsArray.getString(it) }
                    .toMutableList()
                sections.add(Section(name, chords))
            }
        } catch (e: Exception) {
            sections.clear()
        }
    }

    override fun onPause() {
        super.onPause()
        saveSections()
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