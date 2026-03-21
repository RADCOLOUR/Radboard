package com.radcolour.myapplication

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ChordRepository {

    private const val PREFS_NAME = "chord_repository"
    private const val KEY_IMPORTED = "imported_chords"

    private var builtInChords: Map<String, ChordsActivity.ChordInfo> = emptyMap()
    private var importedChords: MutableMap<String, ChordsActivity.ChordInfo> = mutableMapOf()

    private var _roots: List<String> = listOf("C","D","E","F","G","A","B")
    private var _types: List<String> = listOf("Major","Minor","7th","Maj7","Min7","Sus2","Sus4","Aug","Dim","Compound")

    private var initialised = false

    fun init(context: Context) {
        if (initialised) return
        val (chords, roots, types) = loadBuiltIn(context)
        builtInChords = chords
        if (roots.isNotEmpty()) _roots = roots
        if (types.isNotEmpty()) _types = types
        importedChords = loadImported(context).toMutableMap()
        initialised = true
    }

    fun getAllChords(): Map<String, ChordsActivity.ChordInfo> = builtInChords + importedChords

    fun getImportedChords(): Map<String, ChordsActivity.ChordInfo> = importedChords.toMap()

    fun isImported(key: String): Boolean = importedChords.containsKey(key)

    fun isBuiltIn(key: String): Boolean = builtInChords.containsKey(key)

    fun getRoots(): List<String> = _roots

    fun getTypes(): List<String> = _types

    fun addRootIfMissing(root: String) {
        if (!_roots.contains(root)) _roots = _roots + root
    }

    fun addTypeIfMissing(type: String) {
        if (!_types.contains(type)) _types = _types + type
    }
    fun importChord(context: Context, chord: ChordsActivity.ChordInfo) {
        importedChords[chord.name] = chord
        saveImported(context)
    }

    fun importChords(context: Context, chords: List<ChordsActivity.ChordInfo>) {
        chords.forEach { importedChords[it.name] = it }
        saveImported(context)
    }

    fun deleteImported(context: Context, name: String) {
        importedChords.remove(name)
        saveImported(context)
    }

    fun clearImported(context: Context) {
        importedChords.clear()
        saveImported(context)
    }

    fun conflicts(name: String): Boolean {
        return builtInChords.containsKey(name) || importedChords.containsKey(name)
    }

    private data class BuiltInResult(
        val chords: Map<String, ChordsActivity.ChordInfo>,
        val roots: List<String>,
        val types: List<String>
    )

    private fun loadBuiltIn(context: Context): BuiltInResult {
        return try {
            val raw = context.resources.openRawResource(R.raw.builtin_chords)
                .bufferedReader().use { it.readText() }
            val pack = RadChordParser.parseRadPack(raw)
            val chords = pack.chords.associate { it.name to RadChordParser.toChordInfo(it) }
            BuiltInResult(chords, pack.roots, pack.types)
        } catch (e: Exception) {
            BuiltInResult(emptyMap(), emptyList(), emptyList())
        }
    }
    private fun saveImported(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        importedChords.values.forEach { chord ->
            val obj = JSONObject()
            obj.put("name", chord.name)
            obj.put("description", chord.description)
            obj.put("root", chord.root)
            obj.put("type", chord.type)
            obj.put("guitarPositions", positionsToJson(chord.guitarPositions))
            obj.put("bassPositions", positionsToJson(chord.bassPositions))
            array.put(obj)
        }
        prefs.edit().putString(KEY_IMPORTED, array.toString()).apply()
    }

    private fun loadImported(context: Context): Map<String, ChordsActivity.ChordInfo> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_IMPORTED, null) ?: return emptyMap()
        return try {
            val array = JSONArray(json)
            val map = mutableMapOf<String, ChordsActivity.ChordInfo>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val description = obj.getString("description")
                val root = obj.optString("root", "")
                val type = obj.optString("type", "")
                val guitar = jsonToPositions(obj.getJSONArray("guitarPositions"))
                val bass = jsonToPositions(obj.getJSONArray("bassPositions"))
                map[name] = ChordsActivity.ChordInfo(name, description, guitar, bass, root, type)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun positionsToJson(positions: List<FretboardView.ChordPosition>): JSONArray {
        val array = JSONArray()
        positions.forEach { pos ->
            val obj = JSONObject()
            obj.put("startFret", pos.startFret)
            obj.put("strings", JSONArray(pos.strings))
            obj.put("frets", JSONArray(pos.frets))
            obj.put("barreFret", pos.barreFret)
            obj.put("barreFromString", pos.barreFromString)
            obj.put("barreToString", pos.barreToString)
            array.put(obj)
        }
        return array
    }

    private fun jsonToPositions(array: JSONArray): List<FretboardView.ChordPosition> {
        val list = mutableListOf<FretboardView.ChordPosition>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val strings = obj.getJSONArray("strings").let { a ->
                (0 until a.length()).map { a.getInt(it) }
            }
            val frets = obj.getJSONArray("frets").let { a ->
                (0 until a.length()).map { a.getInt(it) }
            }
            list.add(FretboardView.ChordPosition(
                startFret = obj.getInt("startFret"),
                strings = strings,
                frets = frets,
                barreFret = obj.optInt("barreFret", -1),
                barreFromString = obj.optInt("barreFromString", -1),
                barreToString = obj.optInt("barreToString", -1)
            ))
        }
        return list
    }
}