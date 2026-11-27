package com.hanziflow.core

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class SpeechComparison(
    val targetLength: Int,
    val heardLength: Int,
    val distance: Int,
    val similarity: Double
) {
    val exact: Boolean get() = distance == 0
}

/**
 * Removes formatting that an Android recognizer may add while preserving the
 * actual letters and digits it recognized. NFKC also makes full-width Latin
 * letters/digits and their ordinary forms comparable without converting
 * simplified/traditional Han characters or ignoring tone marks.
 */
fun speechChars(s: String): List<Int> = Normalizer.normalize(s, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT)
    .codePoints()
    .toArray()
    .filter { Character.isLetterOrDigit(it) }

fun speechComparison(target: String, heard: String): SpeechComparison? {
    val expected = speechChars(target)
    val recognized = speechChars(heard)
    if (expected.isEmpty() || recognized.isEmpty()) return null

    var previous = IntArray(recognized.size + 1) { it }
    for (i in expected.indices) {
        val row = IntArray(recognized.size + 1)
        row[0] = i + 1
        for (j in recognized.indices) {
            row[j + 1] = minOf(
                row[j] + 1,
                previous[j + 1] + 1,
                previous[j] + if (expected[i] == recognized[j]) 0 else 1
            )
        }
        previous = row
    }
    val distance = previous.last()
    val longest = max(expected.size, recognized.size)
    return SpeechComparison(
        targetLength = expected.size,
        heardLength = recognized.size,
        distance = distance,
        similarity = (1.0 - distance.toDouble() / longest).coerceIn(0.0, 1.0)
    )
}

/** Picks the closest non-empty local ASR alternative, retaining the original text. */
fun bestSpeechHypothesis(target: String, alternatives: List<String>): String? = alternatives
    .asSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .withIndex()
    .minWithOrNull(
        compareBy<IndexedValue<String>>(
            { speechComparison(target, it.value)?.distance ?: Int.MAX_VALUE },
            { it.index }
        )
    )
    ?.value

fun compareSpeech(target: String, heard: String): String {
    val result = speechComparison(target, heard)
        ?: return "Недостаточно текста для сравнения."
    if (result.exact) {
        return "Распознанный текст совпал. Это проверка текста, а не оценка тонов или фонем."
    }
    val scale = max(result.targetLength, result.heardLength)
    val percent = (result.similarity * 100).roundToInt()
    return "Различий при сравнении текста: ${result.distance} из $scale. " +
        "Текстовое совпадение: $percent%. Возможны ошибки распознавания, омонимы или различия в записи; " +
        "это не оценка тонов или фонем."
}
