package com.radcolour.myapplication

object MusicTheory {

    val ALL_NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    val SCALES = mapOf(
        "Major"           to listOf(0, 2, 4, 5, 7, 9, 11),
        "Natural Minor"   to listOf(0, 2, 3, 5, 7, 8, 10),
        "Harmonic Minor"  to listOf(0, 2, 3, 5, 7, 8, 11),
        "Pentatonic Major" to listOf(0, 2, 4, 7, 9),
        "Pentatonic Minor" to listOf(0, 3, 5, 7, 10),
        "Blues"           to listOf(0, 3, 5, 6, 7, 10),
        "Dorian"          to listOf(0, 2, 3, 5, 7, 9, 10),
        "Mixolydian"      to listOf(0, 2, 4, 5, 7, 9, 10)
    )

    val SCALE_CHORD_TYPES = mapOf(
        "Major" to mapOf(
            0 to "Major", 2 to "Minor", 4 to "Minor",
            5 to "Major", 7 to "Major", 9 to "Minor", 11 to "Dim"
        ),
        "Natural Minor" to mapOf(
            0 to "Minor", 2 to "Dim", 3 to "Major",
            5 to "Minor", 7 to "Minor", 8 to "Major", 10 to "Major"
        ),
        "Harmonic Minor" to mapOf(
            0 to "Minor", 2 to "Dim", 3 to "Major",
            5 to "Minor", 7 to "Major", 8 to "Major", 11 to "Dim"
        ),
        "Pentatonic Major" to mapOf(
            0 to "Major", 2 to "Minor", 4 to "Major",
            7 to "Major", 9 to "Minor"
        ),
        "Pentatonic Minor" to mapOf(
            0 to "Minor", 3 to "Major", 5 to "Minor",
            7 to "Minor", 10 to "Major"
        ),
        "Blues" to mapOf(
            0 to "7th", 3 to "Major", 5 to "7th",
            6 to "Dim", 7 to "7th", 10 to "Major"
        ),
        "Dorian" to mapOf(
            0 to "Minor", 2 to "Minor", 3 to "Major",
            5 to "Major", 7 to "Minor", 9 to "Dim", 10 to "Major"
        ),
        "Mixolydian" to mapOf(
            0 to "Major", 2 to "Minor", 4 to "Dim",
            5 to "Major", 7 to "Minor", 9 to "Minor", 10 to "Major"
        )
    )

    fun getChordsInKey(root: String, scale: String): Set<String> {
        val rootIndex = ALL_NOTES.indexOf(root)
        if (rootIndex < 0) return emptySet()

        val chordTypes = SCALE_CHORD_TYPES[scale] ?: return emptySet()
        val result = mutableSetOf<String>()

        chordTypes.forEach { (semitones, type) ->
            val noteIndex = (rootIndex + semitones) % 12
            val note = ALL_NOTES[noteIndex]
            result.add("$note $type")
        }

        return result
    }
    fun getScaleNotes(root: String, scale: String): List<String> {
        val rootIndex = ALL_NOTES.indexOf(root)
        if (rootIndex < 0) return emptyList()

        val intervals = SCALES[scale] ?: return emptyList()
        return intervals.map { semitones ->
            ALL_NOTES[(rootIndex + semitones) % 12]
        }
    }
    fun transposeChord(chordName: String, fromRoot: String, toRoot: String): String {
        val fromIndex = ALL_NOTES.indexOf(fromRoot)
        val toIndex = ALL_NOTES.indexOf(toRoot)
        if (fromIndex < 0 || toIndex < 0) return chordName

        val semitones = (toIndex - fromIndex + 12) % 12
        val chordRoot = ALL_NOTES.firstOrNull { chordName.startsWith(it) } ?: return chordName
        val chordType = chordName.removePrefix(chordRoot).trim()
        val chordRootIndex = ALL_NOTES.indexOf(chordRoot)
        val newRootIndex = (chordRootIndex + semitones) % 12
        val newRoot = ALL_NOTES[newRootIndex]

        return "$newRoot $chordType".trim()
    }
    fun transposeChords(chords: List<String>, semitones: Int): List<String> {
        return chords.map { chord ->
            val chordRoot = ALL_NOTES.firstOrNull { chord.startsWith(it) }
                ?: return@map chord
            val chordType = chord.removePrefix(chordRoot).trim()
            val rootIndex = ALL_NOTES.indexOf(chordRoot)
            val newIndex = (rootIndex + semitones + 12) % 12
            "${ALL_NOTES[newIndex]} $chordType".trim()
        }
    }
    fun semitoneseBetween(from: String, to: String): Int {
        val fromIndex = ALL_NOTES.indexOf(from)
        val toIndex = ALL_NOTES.indexOf(to)
        if (fromIndex < 0 || toIndex < 0) return 0
        return (toIndex - fromIndex + 12) % 12
    }
    fun detectKey(chords: List<String>): Pair<String, String>? {
        if (chords.isEmpty()) return null

        var bestKey: Pair<String, String>? = null
        var bestScore = 0

        ALL_NOTES.forEach { root ->
            SCALES.keys.forEach { scale ->
                val keyChords = getChordsInKey(root, scale)
                val score = chords.count { keyChords.contains(it) }
                if (score > bestScore) {
                    bestScore = score
                    bestKey = Pair(root, scale)
                }
            }
        }

        return bestKey
    }
}