package com.radcolour.myapplication

object RadChordParser {
    data class ParsedPosition(
        val startFret: Int,
        val strings: List<Int>,
        val frets: List<Int>,
        val barreFret: Int = -1,
        val barreFromString: Int = -1,
        val barreToString: Int = -1
    )
    data class ParsedChord(
        val name: String,
        val description: String,
        val guitarPositions: List<ParsedPosition>,
        val root: String = "",
        val type: String = ""
    )
    data class ParsedPack(
        val author: String,
        val description: String,
        val roots: List<String>,
        val types: List<String>,
        val chords: List<ParsedChord>,
        val errors: List<ParseError>
    )
    data class ParseError(
        val lineNumber: Int,
        val line: String,
        val reason: String
    )

    fun parseRadGuitar(content: String): ParsedChord? {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var name = ""
        var description = ""
        var root = ""
        var type = ""
        val positions = mutableListOf<ParsedPosition>()

        for (line in lines) {
            when {
                line.startsWith("name:", ignoreCase = true) ->
                    name = line.substringAfter(":").trim()
                line.startsWith("description:", ignoreCase = true) ->
                    description = line.substringAfter(":").trim()
                line.startsWith("root:", ignoreCase = true) ->
                    root = line.substringAfter(":").trim()
                line.startsWith("type:", ignoreCase = true) ->
                    type = line.substringAfter(":").trim()
                line.startsWith("position:", ignoreCase = true) -> {
                    val posStr = line.substringAfter(":").trim()
                    val pos = parsePosition(posStr, 6) ?: continue
                    positions.add(pos)
                }
            }
        }

        if (name.isEmpty() || positions.isEmpty()) return null
        return ParsedChord(name, description, positions, root, type)
    }

    fun parseRadPack(content: String): ParsedPack {
        var author = ""
        var packDescription = ""
        var roots = listOf<String>()
        var types = listOf<String>()
        val chords = mutableListOf<ParsedChord>()
        val errors = mutableListOf<ParseError>()

        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            val lineNumber = i + 1

            when {
                line.isEmpty() -> { i++; continue }

                line.startsWith("author:", ignoreCase = true) ->
                    author = line.substringAfter(":").trim()

                line.startsWith("description:", ignoreCase = true) ->
                    packDescription = line.substringAfter(":").trim()

                line.startsWith("roots:", ignoreCase = true) ->
                    roots = parseList(line.substringAfter(":"))

                line.startsWith("types:", ignoreCase = true) ->
                    types = parseList(line.substringAfter(":"))

                line.startsWith("[") -> {
                    val closeIdx = line.indexOf("]")
                    if (closeIdx < 0) {
                        errors.add(ParseError(lineNumber, line, "Missing closing ] in chord header"))
                        i++
                        continue
                    }

                    val name = line.substring(1, closeIdx).trim()
                    if (name.isEmpty()) {
                        errors.add(ParseError(lineNumber, line, "Chord name is empty"))
                        i++
                        continue
                    }

                    val rest = line.substring(closeIdx + 1).trim()
                    val metaParts = rest.split("|")
                    val chordDescription = metaParts[0].trim()
                    var root = ""
                    var type = ""

                    if (metaParts.size > 1) {
                        val meta = metaParts[1].trim()
                        root = extractInlineField(meta, "root") ?: ""
                        type = extractInlineField(meta, "type") ?: ""
                    }

                    val guitarPositions = mutableListOf<ParsedPosition>()
                    i++

                    while (i < lines.size) {
                        val inner = lines[i].trim()
                        if (inner.startsWith("[")) break
                        if (inner.isEmpty()) { i++; continue }

                        val innerLine = i + 1
                        when {
                            inner.startsWith("guitar:", ignoreCase = true) -> {
                                val posStr = inner.substringAfter(":").trim()
                                val pos = parsePosition(posStr, 6)
                                if (pos == null) {
                                    errors.add(ParseError(innerLine, inner, "Could not parse guitar position: $posStr"))
                                } else {
                                    guitarPositions.add(pos)
                                }
                            }
                            inner.startsWith("bass:", ignoreCase = true) -> { }
                            inner.startsWith("root:", ignoreCase = true) ->
                                root = inner.substringAfter(":").trim()
                            inner.startsWith("type:", ignoreCase = true) ->
                                type = inner.substringAfter(":").trim()
                            else ->
                                errors.add(ParseError(innerLine, inner, "Unrecognised line inside chord block"))
                        }
                        i++
                    }

                    if (guitarPositions.isEmpty()) {
                        errors.add(ParseError(lineNumber, line, "Chord '$name' has no valid guitar positions"))
                    } else {
                        chords.add(ParsedChord(name, chordDescription, guitarPositions, root, type))
                    }

                    continue
                }

                else ->
                    errors.add(ParseError(lineNumber, line, "Unrecognised top-level line"))
            }

            i++
        }

        return ParsedPack(author, packDescription, roots, types, chords, errors)
    }

    fun parsePosition(posStr: String, stringCount: Int): ParsedPosition? {
        return try {
            val parts = posStr.trim().split(Regex("\\s+"))
            val tabPart = parts[0].trim()
            var barreFret = -1
            var barreFrom = -1
            var barreTo = -1

            if (parts.size > 1) {
                val barrePart = parts[1].trim()
                if (barrePart.startsWith("barre:", ignoreCase = true)) {
                    val barreRange = barrePart.substringAfter(":").trim()
                    val barreNums = barreRange.split("-")
                    if (barreNums.size == 2) {
                        barreFrom = barreNums[0].trim().toInt()
                        barreTo = barreNums[1].trim().toInt()
                    }
                }
            }

            val atParts = tabPart.split("@")
            val tab = atParts[0].trim()
            val startFret = if (atParts.size > 1) atParts[1].trim().toInt() else 1

            if (tab.length != stringCount) return null

            if (barreFrom >= 0) barreFret = startFret

            val absoluteFrets = mutableListOf<Int>()
            val mutedOrOpen = mutableListOf<Boolean>()

            for (ch in tab) {
                when (ch.lowercaseChar()) {
                    'x' -> { absoluteFrets.add(0); mutedOrOpen.add(true) }
                    '0' -> { absoluteFrets.add(0); mutedOrOpen.add(true) }
                    else -> {
                        val digit = ch.digitToInt()
                        absoluteFrets.add(startFret + digit - 1)
                        mutedOrOpen.add(false)
                    }
                }
            }

            val strings = MutableList(stringCount) { 0 }
            val frets = MutableList(stringCount) { 0 }

            for (i in 0 until stringCount) {
                val ch = tab[i].lowercaseChar()
                when (ch) {
                    'x' -> { strings[i] = 0; frets[i] = 0 }
                    '0' -> { strings[i] = -1; frets[i] = 0 }
                    else -> { frets[i] = absoluteFrets[i] }
                }
            }

            if (barreFrom >= 0) {
                for (i in barreFrom..barreTo) {
                    if (i < stringCount && !mutedOrOpen[i]) strings[i] = 1
                }
            }

            val nonBarreStrings = (0 until stringCount)
                .filter { !mutedOrOpen[it] }
                .filter { i -> barreFrom < 0 || i < barreFrom || i > barreTo }
                .sortedBy { frets[it] }

            var nextFinger = if (barreFrom >= 0) 2 else 1
            for (i in nonBarreStrings) {
                strings[i] = nextFinger.coerceAtMost(4)
                nextFinger++
            }

            ParsedPosition(startFret, strings, frets, barreFret, barreFrom, barreTo)

        } catch (e: Exception) {
            null
        }
    }

    private fun parseList(raw: String): List<String> {
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun extractInlineField(meta: String, field: String): String? {
        val regex = Regex("$field:([^\\s]+)", RegexOption.IGNORE_CASE)
        return regex.find(meta)?.groupValues?.get(1)?.trim()
    }

    fun toChordPosition(parsed: ParsedPosition): FretboardView.ChordPosition {
        return FretboardView.ChordPosition(
            startFret = parsed.startFret,
            strings = parsed.strings,
            frets = parsed.frets,
            barreFret = parsed.barreFret,
            barreFromString = parsed.barreFromString,
            barreToString = parsed.barreToString
        )
    }

    fun toChordInfo(parsed: ParsedChord): ChordsActivity.ChordInfo {
        return ChordsActivity.ChordInfo(
            name = parsed.name,
            description = parsed.description,
            guitarPositions = parsed.guitarPositions.map { toChordPosition(it) },
            root = parsed.root,
            type = parsed.type
        )
    }
}