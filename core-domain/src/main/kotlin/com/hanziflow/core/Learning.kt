package com.hanziflow.core

import java.time.LocalDate
import kotlin.math.roundToLong

data class Word(val id: String, val simplified: String, val traditional: String, val pinyin: String, val russian: String, val example: String, val examplePinyin: String, val exampleRu: String, val pos: String, val level: Int?, val source: String)
data class Token(val text: String, val word: Word?)
/** Deterministic maximum dictionary match; unknown Hanzi are preserved individually. Not contextual NLP. */
class Segmenter(words: List<Word>) {
    private class Node { val children = mutableMapOf<Int, Node>(); var word: Word? = null }
    private val root = Node()
    init { words.forEach { w -> listOf(w.simplified, w.traditional).distinct().filter { it.isNotEmpty() }.forEach { text ->
        var node = root; text.codePoints().toArray().forEach { cp -> node = node.children.getOrPut(cp) { Node() } }; if (node.word == null) node.word = w
    } } }
    fun segment(text: String): List<Token> {
        val points = text.codePoints().toArray(); var i = 0; val out = mutableListOf<Token>()
        while (i < points.size) {
            var n = root; var j = i; var last: Word? = null; var end = i + 1
            while (j < points.size) { n = n.children[points[j]] ?: break; j++; n.word?.let { last = it; end = j } }
            out += Token(String(points, i, end-i), last); i = end
        }; return out
    }
}
enum class Grade { AGAIN, HARD, GOOD, EASY }
data class Review(val repetitions: Int = 0, val lapses: Int = 0, val interval: Long = 0, val ease: Double = 2.5, val due: LocalDate = LocalDate.now())
private const val MIN_EASE = 1.3
private const val MAX_EASE = 3.5
private const val DEFAULT_EASE = 2.5
private const val MAX_INTERVAL_DAYS = 3650L

/**
 * Restores a persisted review without allowing one damaged legacy row to break the whole queue.
 * The database schema has been stable since 0.1, so this also defines the compatibility boundary
 * for older installations.
 */
fun restoreReview(
    repetitions: Int,
    lapses: Int,
    interval: Long,
    ease: Double,
    due: String,
    today: LocalDate,
): Review = Review(
    repetitions = repetitions.coerceAtLeast(0),
    lapses = lapses.coerceAtLeast(0),
    interval = interval.coerceIn(0, MAX_INTERVAL_DAYS),
    ease = ease.takeIf(Double::isFinite)?.coerceIn(MIN_EASE, MAX_EASE) ?: DEFAULT_EASE,
    due = runCatching { LocalDate.parse(due) }.getOrDefault(today),
)

/** Small SM-2-inspired variant, not FSRS. Again returns tomorrow; in-session retries are separate. */
fun schedule(old: Review, grade: Grade, today: LocalDate): Review {
    val current = restoreReview(old.repetitions, old.lapses, old.interval, old.ease, old.due.toString(), today)
    if (grade == Grade.AGAIN) return Review(
        repetitions = 0,
        lapses = if (current.lapses == Int.MAX_VALUE) Int.MAX_VALUE else current.lapses + 1,
        interval = 1,
        ease = (current.ease - 0.2).coerceAtLeast(MIN_EASE),
        due = today.plusDays(1),
    )
    val ease = (current.ease + when(grade) { Grade.HARD -> -0.15; Grade.EASY -> 0.15; else -> 0.0 }).coerceIn(MIN_EASE,MAX_EASE)
    val interval = when {
        current.repetitions == 0 -> if(grade == Grade.EASY) 4L else 1L
        current.repetitions == 1 -> if(grade == Grade.HARD) 2L else if(grade == Grade.EASY) 7L else 3L
        else -> {
            val multiplier = when(grade) { Grade.HARD -> 1.2; Grade.EASY -> ease*1.3; else -> ease }
            val calculated = (current.interval * multiplier).roundToLong()
            // Rounding 2 * 1.2 used to keep a repeatedly hard card at two days forever.
            val minimumGrowth = (current.interval + 1).coerceAtMost(MAX_INTERVAL_DAYS)
            calculated.coerceIn(minimumGrowth, MAX_INTERVAL_DAYS)
        }
    }
    val repetitions = if (current.repetitions == Int.MAX_VALUE) Int.MAX_VALUE else current.repetitions + 1
    return Review(repetitions,current.lapses,interval,ease,today.plusDays(interval))
}
/** Lexical tone input already disambiguated; punctuation/prosodic boundaries must be passed as separate groups.
 * Yi exceptions (ordinals, isolated numbers) are caller supplied. 3rd tone half contour is not a tone category. */
data class Syllable(val hanzi: String, val tone: Int, val yiUnchanged: Boolean = false)
fun sandhi(group: List<Syllable>): List<Int> = group.mapIndexed { i, s ->
    val next = group.getOrNull(i+1)?.tone
    when { s.hanzi == "不" && s.tone == 4 && next == 4 -> 2
        s.hanzi == "一" && s.tone == 1 && !s.yiUnchanged && next in 1..4 -> if(next == 4) 2 else 4
        s.tone == 3 && next == 3 -> 2
        else -> s.tone }
}
fun sprint(total: Int, days: Int): List<Int> { require(total>=0 && days>0); return List(days) { total/days + if(it<total%days) 1 else 0 } }
