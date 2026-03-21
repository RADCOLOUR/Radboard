package com.radcolour.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChordsActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnByRoot: Button
    private lateinit var btnByType: Button
    private lateinit var btnGuitar: Button
    private lateinit var btnBass: Button
    private lateinit var btnImport: Button
    private lateinit var selectorRow: LinearLayout
    private lateinit var secondarySelectorList: LinearLayout
    private lateinit var positionSelector: LinearLayout
    private lateinit var tvChordName: TextView
    private lateinit var tvChordDescription: TextView
    private lateinit var fretboardView: FretboardView

    private var byRoot = true
    private var showingGuitar = true
    private var selectedPrimary = ""
    private var selectedSecondary = ""
    private var selectedChordKey = ""
    private var currentPositionIndex = 0

    val roots: List<String>
        get() = ChordRepository.getRoots()
    val chordTypes: List<String>
        get() = ChordRepository.getTypes()

    data class ChordInfo(
        val name: String,
        val description: String,
        val guitarPositions: List<FretboardView.ChordPosition>,
        val bassPositions: List<FretboardView.ChordPosition>,
        val root: String = "",
        val type: String = ""
    )

    private val chordData: Map<String, ChordInfo>
        get() = ChordRepository.getAllChords()

    private lateinit var btnTools: Button
    private var highlightedChords: Set<String> = emptySet()
    companion object {
        private const val REQUEST_IMPORT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()
        setContentView(R.layout.activity_chords)

        ChordRepository.init(this)

        btnBack = findViewById(R.id.btnBack)
        btnByRoot = findViewById(R.id.btnByRoot)
        btnByType = findViewById(R.id.btnByType)
        btnGuitar = findViewById(R.id.btnGuitar)
        btnBass = findViewById(R.id.btnBass)
        btnImport = findViewById(R.id.btnImport)
        btnTools = findViewById(R.id.btnTools)
        btnTools.setOnClickListener { showToolsMenu() }
        selectorRow = findViewById(R.id.selectorRow)
        secondarySelectorList = findViewById(R.id.secondarySelectorList)
        positionSelector = findViewById(R.id.positionSelector)
        tvChordName = findViewById(R.id.tvChordName)
        tvChordDescription = findViewById(R.id.tvChordDescription)
        fretboardView = findViewById(R.id.fretboardView)

        btnBack.setOnClickListener { finish() }
        btnByRoot.setOnClickListener { byRoot = true; updateModeButtons(); buildPrimarySelector() }
        btnByType.setOnClickListener { byRoot = false; updateModeButtons(); buildPrimarySelector() }
        btnGuitar.setOnClickListener { showingGuitar = true; updateInstrumentButtons(); refreshFretboard() }
        btnBass.setOnClickListener { showingGuitar = false; updateInstrumentButtons(); refreshFretboard() }
        btnImport.setOnClickListener { openImportPicker() }

        handleIncomingIntent(intent)
        buildPrimarySelector()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    // -------------------------------------------------------------------------
    // File import
    // -------------------------------------------------------------------------

    private fun openImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/octet-stream",
                "text/plain",
                "*/*"
            ))
        }
        startActivityForResult(intent, REQUEST_IMPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT && resultCode == RESULT_OK) {
            data?.data?.let { uri -> processImportUri(uri) }
        }
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri -> processImportUri(uri) }
        }
    }

    private fun processImportUri(uri: Uri) {
        try {
            val fileName = getFileName(uri) ?: ""
            val content = contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
                ?: return showToast("Could not read file")

            when {
                fileName.endsWith(".radpack", ignoreCase = true) ->
                    processRadPack(content, fileName)
                fileName.endsWith(".radguitar", ignoreCase = true) ->
                    processRadGuitar(content)
                fileName.endsWith(".radbass", ignoreCase = true) ->
                    processRadBass(content)
                else ->
                    showToast("Unrecognised file type. Use .radguitar, .radbass or .radpack")
            }

        } catch (e: Exception) {
            showToast("Failed to import: ${e.message}")
        }
    }

    private fun showToolsMenu() {
        val options = arrayOf(
            getString(R.string.tools_key_finder),
            getString(R.string.tools_transposer)
        )
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showKeyFinder()
                    1 -> showTransposer()
                }
            }
            .show()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.bg_card)
            }
    }

    private fun showKeyFinder() {
        KeyFinderDialog(
            context = this,
            onChordsHighlighted = { chords ->
                highlightedChords = chords
                buildPrimarySelector()
            },
            onDismiss = {
                highlightedChords = emptySet()
                buildPrimarySelector()
            }
        ).show()
    }

    private fun showTransposer() {
        // Collect all chords from current progression
        val prefs = getSharedPreferences("progression_prefs", MODE_PRIVATE)
        val json = prefs.getString("sections", null)
        val progressionChords = mutableListOf<String>()
        if (json != null) {
            try {
                val array = org.json.JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val chordsArray = obj.getJSONArray("chords")
                    for (j in 0 until chordsArray.length()) {
                        val chord = chordsArray.getString(j)
                        if (!progressionChords.contains(chord)) progressionChords.add(chord)
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        TransposerDialog(
            context = this,
            chordData = chordData,
            currentProgression = progressionChords,
            onProgressionTransposed = { transposed ->
                Toast.makeText(
                    this,
                    getString(R.string.transposer_result_progression, transposed.joinToString(" → ")),
                    Toast.LENGTH_LONG
                ).show()
            }
        ).show()
    }

    // -------------------------------------------------------------------------
    // .radpack import — confirmation popup + per-chord conflict dialogs
    // -------------------------------------------------------------------------

    private fun processRadPack(content: String, fileName: String) {
        val pack = RadChordParser.parseRadPack(content)

        // If there are parse errors, write log and reject
        if (pack.errors.isNotEmpty()) {
            writeErrorLog(fileName, pack.errors)
            showToast("Import failed — error log saved to sdcard/radcolour/radlog/")
            return
        }

        if (pack.chords.isEmpty()) {
            showToast("No chords found in file")
            return
        }

        // Build confirmation message
        val authorLine = if (pack.author.isNotEmpty()) "Author: ${pack.author}\n" else ""
        val descLine = if (pack.description.isNotEmpty()) "${pack.description}\n\n" else ""
        val message = "${authorLine}${descLine}This pack contains ${pack.chords.size} chord(s). Import?"

        AlertDialog.Builder(this)
            .setTitle("Import Chord Pack")
            .setMessage(message)
            .setPositiveButton("Import") { _, _ ->
                // Add new roots/types to whitelist
                pack.roots.forEach { ChordRepository.addRootIfMissing(it) }
                pack.types.forEach { ChordRepository.addTypeIfMissing(it) }
                // Start processing chords one by one
                processNextChord(pack.chords, 0, 0, 0)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Recursively processes chords one at a time, showing conflict dialogs as needed */
    private fun processNextChord(
        chords: List<RadChordParser.ParsedChord>,
        index: Int,
        imported: Int,
        skipped: Int
    ) {
        if (index >= chords.size) {
            // All done
            buildPrimarySelector()
            showToast("Imported $imported chord(s), skipped $skipped")
            return
        }

        val parsed = chords[index]
        val chord = RadChordParser.toChordInfo(parsed)

        // Add root/type to whitelist if needed
        if (chord.root.isNotEmpty()) ChordRepository.addRootIfMissing(chord.root)
        if (chord.type.isNotEmpty()) ChordRepository.addTypeIfMissing(chord.type)

        if (ChordRepository.conflicts(chord.name)) {
            // Ask user whether to overwrite
            AlertDialog.Builder(this)
                .setTitle("Chord Already Exists")
                .setMessage("\"${chord.name}\" already exists. Overwrite it?")
                .setPositiveButton("Overwrite") { _, _ ->
                    ChordRepository.importChord(this, chord)
                    processNextChord(chords, index + 1, imported + 1, skipped)
                }
                .setNegativeButton("Skip") { _, _ ->
                    processNextChord(chords, index + 1, imported, skipped + 1)
                }
                .show()
        } else {
            ChordRepository.importChord(this, chord)
            processNextChord(chords, index + 1, imported + 1, skipped)
        }
    }

    // -------------------------------------------------------------------------
    // .radguitar / .radbass import
    // -------------------------------------------------------------------------

    private fun processRadGuitar(content: String) {
        val parsed = RadChordParser.parseRadGuitar(content)
            ?: return showToast("Invalid .radguitar file")
        val existing = chordData[parsed.name]
        val chord = RadChordParser.toChordInfo(parsed).copy(
            bassPositions = existing?.bassPositions ?: emptyList()
        )
        finishSingleImport(chord)
    }

    private fun processRadBass(content: String) {
        val parsed = RadChordParser.parseRadBass(content)
            ?: return showToast("Invalid .radbass file")
        val existing = chordData[parsed.name]
        val chord = RadChordParser.toChordInfo(parsed).copy(
            guitarPositions = existing?.guitarPositions ?: emptyList()
        )
        finishSingleImport(chord)
    }

    private fun finishSingleImport(chord: ChordInfo) {
        if (chord.root.isNotEmpty()) ChordRepository.addRootIfMissing(chord.root)
        if (chord.type.isNotEmpty()) ChordRepository.addTypeIfMissing(chord.type)

        if (ChordRepository.conflicts(chord.name)) {
            AlertDialog.Builder(this)
                .setTitle("Chord Already Exists")
                .setMessage("\"${chord.name}\" already exists. Overwrite it?")
                .setPositiveButton("Overwrite") { _, _ ->
                    ChordRepository.importChord(this, chord)
                    buildPrimarySelector()
                    showToast("Imported ${chord.name}")
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            ChordRepository.importChord(this, chord)
            buildPrimarySelector()
            showToast("Imported ${chord.name}")
        }
    }

    // -------------------------------------------------------------------------
    // Error log writer
    // -------------------------------------------------------------------------

    private fun writeErrorLog(
        fileName: String,
        errors: List<RadChordParser.ParseError>
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val logFileName = "${fileName}_error_$timestamp.txt"

            val sb = StringBuilder()
            sb.appendLine("Radcolour Import Error Log")
            sb.appendLine("File: $fileName")
            sb.appendLine("Date: ${Date()}")
            sb.appendLine("Errors: ${errors.size}")
            sb.appendLine("=".repeat(40))
            errors.forEach { err ->
                sb.appendLine("Line ${err.lineNumber}: ${err.reason}")
                sb.appendLine("  > ${err.line}")
                sb.appendLine()
            }

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, logFileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Documents/radcolour/radlog")
            }

            val uri = contentResolver.insert(
                android.provider.MediaStore.Files.getContentUri("external"),
                contentValues
            ) ?: throw Exception("Could not create log file")

            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(sb.toString().toByteArray())
            }

            showToast("Import failed — error log saved to Documents/radcolour/radlog/")

        } catch (e: Exception) {
            showToast("Import failed and could not write error log: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name ?: uri.lastPathSegment
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    private fun updateModeButtons() {
        btnByRoot.backgroundTintList = ContextCompat.getColorStateList(this, if (byRoot) R.color.blue else R.color.dark)
        btnByRoot.setTextColor(if (byRoot) 0xFF000000.toInt() else 0xFF8BDCFF.toInt())
        btnByType.backgroundTintList = ContextCompat.getColorStateList(this, if (!byRoot) R.color.blue else R.color.dark)
        btnByType.setTextColor(if (!byRoot) 0xFF000000.toInt() else 0xFF8BDCFF.toInt())
    }

    private fun updateInstrumentButtons() {
        btnGuitar.backgroundTintList = ContextCompat.getColorStateList(this, if (showingGuitar) R.color.green else R.color.dark)
        btnGuitar.setTextColor(if (showingGuitar) 0xFF000000.toInt() else 0xFFD4FFBD.toInt())
        btnBass.backgroundTintList = ContextCompat.getColorStateList(this, if (!showingGuitar) R.color.cream else R.color.dark)
        btnBass.setTextColor(if (!showingGuitar) 0xFF000000.toInt() else 0xFFFFF2BD.toInt())
    }

    private fun buildPrimarySelector() {
        selectorRow.removeAllViews()
        secondarySelectorList.removeAllViews()
        positionSelector.removeAllViews()
        tvChordName.text = ""
        tvChordDescription.text = ""
        selectedPrimary = ""
        selectedSecondary = ""
        selectedChordKey = ""

        val items = if (byRoot) roots else chordTypes
        items.forEach { item ->
            val btn = Button(this)
            btn.text = item
            btn.textSize = 11f
            btn.setTextColor(0xFF8BDCFF.toInt())
            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.dark)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 8
            btn.layoutParams = params
            btn.setOnClickListener {
                selectedPrimary = item
                buildSecondarySelector(item)
            }
            selectorRow.addView(btn)
        }
    }

    private fun buildSecondarySelector(primary: String) {
        secondarySelectorList.removeAllViews()
        positionSelector.removeAllViews()
        tvChordName.text = ""
        tvChordDescription.text = ""
        selectedChordKey = ""

        val items = if (byRoot) chordTypes else roots

        items.forEach { item ->
            // Standard key e.g. "G Major"
            val standardKey = if (byRoot) "$primary $item" else "$item $primary"

            // Also check chords with explicit root= and type= fields
            val matchingChords = chordData.entries.filter { (_, chord) ->
                if (byRoot) chord.root == primary && chord.type == item
                else chord.root == item && chord.type == primary
            }

            val key = when {
                chordData.containsKey(standardKey) -> standardKey
                matchingChords.isNotEmpty() -> matchingChords.first().key
                else -> null
            }

            if (key != null) {
                val btn = Button(this)
                btn.text = item
                btn.textSize = 11f
                val isImported = ChordRepository.isImported(key)
                val isHighlighted = highlightedChords.contains(key)
                btn.setTextColor(when {
                    isHighlighted -> 0xFFBFFFAA.toInt()  // green for in-key chords
                    isImported -> 0xFFFFB3D9.toInt()      // pink for imported
                    else -> 0xFF7DD6FF.toInt()            // default blue
                })

                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.dark)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 4
                btn.layoutParams = params
                btn.setOnClickListener {
                    selectedSecondary = item
                    showChord(key)
                }
                secondarySelectorList.addView(btn)
            }
        }
    }

    private fun showChord(key: String) {
        selectedChordKey = key
        val chord = chordData[key] ?: return
        tvChordName.text = chord.name
        tvChordDescription.text = if (ChordRepository.isImported(key))
            "${chord.description}  [imported]"
        else
            chord.description
        currentPositionIndex = 0
        buildPositionSelector(chord)
        refreshFretboard(chord)
    }

    private fun buildPositionSelector(chord: ChordInfo) {
        positionSelector.removeAllViews()
        val positions = if (showingGuitar) chord.guitarPositions else chord.bassPositions

        positions.forEachIndexed { index, _ ->
            val btn = Button(this)
            btn.text = if (index == 0) "Open" else "Pos ${index + 1}"
            btn.textSize = 10f
            btn.setTextColor(if (index == currentPositionIndex) 0xFF000000.toInt() else 0xFF8BDCFF.toInt())
            btn.backgroundTintList = ContextCompat.getColorStateList(this,
                if (index == currentPositionIndex) R.color.blue else R.color.dark)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            params.marginEnd = 8
            btn.layoutParams = params
            btn.setOnClickListener {
                currentPositionIndex = index
                buildPositionSelector(chord)
                refreshFretboard(chord)
            }
            positionSelector.addView(btn)
        }
    }

    private fun refreshFretboard(chord: ChordInfo? = null) {
        val c = chord ?: chordData[selectedChordKey] ?: return
        val positions = if (showingGuitar) c.guitarPositions else c.bassPositions
        if (positions.isEmpty()) return
        val pos = positions[currentPositionIndex.coerceAtMost(positions.size - 1)]
        fretboardView.setPosition(pos, if (showingGuitar) 6 else 4)
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