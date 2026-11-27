package com.hanziflow.core

import java.text.Normalizer
import java.util.Locale

/**
 * One written pinyin syllable and the tone explicitly encoded in that spelling.
 * [plain] keeps ü distinct while removing the tone mark/number. Tone 5 means
 * neutral or unmarked. No contextual tone-sandhi rules are applied here.
 */
data class ToneSyllable(
    val display: String,
    val plain: String,
    val tone: Int,
)

private fun isLatinLetter(codePoint: Int): Boolean =
    Character.isLetter(codePoint) && Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN

private fun isCombiningMark(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
    Character.NON_SPACING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt() -> true
    else -> false
}

/** Splits pinyin into orthographic words. Tone digits also end a word token. */
private fun pinyinTokens(text: String): List<String> {
    val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
    val result = mutableListOf<String>()
    val token = StringBuilder()

    fun flush() {
        if (token.isNotEmpty()) result += token.toString()
        token.setLength(0)
    }

    normalized.codePoints().forEach { codePoint ->
        when {
            isLatinLetter(codePoint) || isCombiningMark(codePoint) || codePoint == ':'.code ->
                token.appendCodePoint(codePoint)
            codePoint in '0'.code..'5'.code -> {
                if (token.isNotEmpty()) {
                    token.appendCodePoint(codePoint)
                    flush()
                }
            }
            else -> flush()
        }
    }
    flush()
    return result
}

private val pinyinInitials = listOf(
    "zh", "ch", "sh",
    "b", "p", "m", "f", "d", "t", "n", "l", "g", "k", "h",
    "j", "q", "x", "r", "z", "c", "s", "y", "w",
)

private val pinyinFinals = setOf(
    "a", "o", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ong",
    "i", "ia", "ie", "iao", "iu", "ian", "in", "iang", "ing", "iong",
    "u", "ua", "uo", "uai", "ui", "uan", "un", "uang", "ueng",
    "ü", "üe", "üan", "ün", "ue", "er",
)

private fun isPinyinSyllable(plain: String): Boolean {
    if (plain in setOf("m", "n", "ng")) return true
    if (plain == "er") return true
    val initial = pinyinInitials.firstOrNull { plain.startsWith(it) }.orEmpty()
    val final = plain.removePrefix(initial)
    return final != "er" && final in pinyinFinals
}

/**
 * Standard pinyin joins syllables inside a word (`shénme`, `gāoxìng`). This
 * finds the longest valid syllable at each position; apostrophes already split
 * ambiguous boundaries such as `Xi'an` before this function is called.
 */
private fun pinyinSyllableEnds(plain: String): List<Int>? {
    val memo = mutableMapOf<Int, List<Int>?>()

    fun solve(start: Int): List<Int>? {
        if (start == plain.length) return emptyList()
        if (memo.containsKey(start)) return memo[start]
        val furthest = minOf(plain.length, start + 7)
        for (end in furthest downTo start + 1) {
            val candidate = plain.substring(start, end)
            val isFinalErhua = end == plain.length && candidate.length > 1 &&
                candidate.endsWith('r') && candidate != "er" &&
                isPinyinSyllable(candidate.dropLast(1))
            if (!isPinyinSyllable(candidate) && !isFinalErhua) continue
            val tail = solve(end) ?: continue
            return (listOf(end) + tail).also { memo[start] = it }
        }
        memo[start] = null
        return null
    }

    return solve(0)
}

private fun splitPinyinWord(token: String): List<String> {
    val plain = parseToneSyllable(token).plain
    val ends = pinyinSyllableEnds(plain) ?: return listOf(token)
    if (ends.size <= 1) return listOf(token)

    val cutsAfterLetters = ends.dropLast(1).toSet()
    val cuts = mutableListOf<Int>()
    var index = 0
    var letters = 0
    while (index < token.length) {
        val codePoint = token.codePointAt(index)
        index += Character.charCount(codePoint)
        if (!isLatinLetter(codePoint)) continue
        letters++
        while (index < token.length) {
            val suffix = token.codePointAt(index)
            if (!isCombiningMark(suffix) && suffix != ':'.code) break
            index += Character.charCount(suffix)
        }
        if (letters in cutsAfterLetters) cuts += index
    }
    if (cuts.size != cutsAfterLetters.size) return listOf(token)

    var start = 0
    return buildList {
        for (end in cuts + token.length) {
            add(token.substring(start, end))
            start = end
        }
    }
}

private fun parseToneSyllable(token: String): ToneSyllable {
    val trailing = token.lastOrNull()?.digitToIntOrNull()?.takeIf { it in 0..5 }
    val writtenBody = if (trailing != null) token.dropLast(1) else token
    val canonicalBody = writtenBody
        .lowercase(Locale.ROOT)
        .replace("u:", "ü")
        .replace('v', 'ü')
    val decomposed = Normalizer.normalize(canonicalBody, Normalizer.Form.NFD)
    val plain = StringBuilder()
    var markedTone: Int? = null

    decomposed.codePoints().forEach { codePoint ->
        when (codePoint) {
            0x0304 -> markedTone = markedTone ?: 1 // macron
            0x0301 -> markedTone = markedTone ?: 2 // acute
            0x030C -> markedTone = markedTone ?: 3 // caron
            0x0300 -> markedTone = markedTone ?: 4 // grave
            0x0308 -> { // diaeresis: u -> ü, not a tone mark
                if (plain.isNotEmpty() && plain.last() == 'u') plain.setCharAt(plain.lastIndex, 'ü')
            }
            else -> if (isLatinLetter(codePoint)) plain.appendCodePoint(codePoint)
        }
    }

    return ToneSyllable(
        display = token,
        plain = plain.toString(),
        tone = when {
            trailing == 0 || trailing == 5 -> 5
            trailing != null && trailing in 1..4 -> trailing
            else -> markedTone ?: 5
        },
    )
}

/** Parses only the tones written by the content author; it never invents sandhi. */
fun pinyinToneSyllables(pinyin: String): List<ToneSyllable> = pinyinTokens(pinyin)
    .flatMap(::splitPinyinWord)
    .map(::parseToneSyllable)
    .filter { it.plain.isNotEmpty() }

/**
 * Produces an equal-duration, schematic phrase contour for display only.
 * It is not aligned to recorded speech and must never be treated as a score.
 * Neutral syllables occupy time at the zero line because their real height is
 * context-dependent, not because zero is a pronunciation target.
 */
fun phraseToneTemplate(pinyin: String): List<ContourPoint> {
    val syllables = pinyinToneSyllables(pinyin)
    if (syllables.isEmpty()) return emptyList()
    val count = syllables.size.toDouble()
    return syllables.flatMapIndexed { index, syllable ->
        val shape = when (syllable.tone) {
            in 1..4 -> Pitch.template(syllable.tone)
            else -> listOf(
                ContourPoint(0.0, 0.0),
                ContourPoint(0.5, 0.0),
                ContourPoint(1.0, 0.0),
            )
        }
        shape.map { point ->
            ContourPoint(
                time = (index + point.time) / count,
                relativeSemitones = point.relativeSemitones,
            )
        }
    }
}
