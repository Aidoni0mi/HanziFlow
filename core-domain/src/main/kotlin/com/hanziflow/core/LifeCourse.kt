package com.hanziflow.core

enum class LifeTextKind { DIALOGUE, STORY }

data class LifeLine(
    val order: Int,
    val speaker: String,
    val hanzi: String,
    val pinyin: String,
    val russian: String,
)

data class LifeText(
    val id: String,
    val kind: LifeTextKind,
    val titleRu: String,
    val lines: List<LifeLine>,
)

data class LifeSituation(
    val id: String,
    val categoryRu: String,
    val titleRu: String,
    val level: Int,
    val texts: List<LifeText>,
)

private val lifeCourseHeader = listOf(
    "situation_id",
    "category_ru",
    "situation_title_ru",
    "level",
    "text_id",
    "kind",
    "text_title_ru",
    "line_order",
    "speaker",
    "hanzi",
    "pinyin",
    "russian",
)

private data class LifeCourseRow(
    val situationId: String,
    val categoryRu: String,
    val situationTitleRu: String,
    val level: Int,
    val textId: String,
    val kind: LifeTextKind,
    val textTitleRu: String,
    val line: LifeLine,
)

/**
 * Parses the bundled, original life-situation course. The strict format keeps
 * metadata stable and makes content mistakes fail during validation instead of
 * silently producing incomplete lessons in the app.
 */
fun parseLifeCourse(tsv: String): List<LifeSituation> {
    val rows = tsv.lineSequence().filter { it.isNotBlank() }.toList()
    require(rows.isNotEmpty()) { "Life course is empty" }
    val header = rows.first().removePrefix("\uFEFF").split('\t')
    require(header == lifeCourseHeader) { "Invalid life course header" }

    val parsed = rows.drop(1).mapIndexed { index, raw ->
        val fields = raw.split('\t')
        require(fields.size == lifeCourseHeader.size) {
            "Invalid life course row ${index + 2}: expected ${lifeCourseHeader.size} columns"
        }
        require(fields.all { it.isNotBlank() }) { "Blank value in life course row ${index + 2}" }
        val level = fields[3].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid level in life course row ${index + 2}")
        require(level in 1..4) { "Life course level must be 1..4 in row ${index + 2}" }
        val order = fields[7].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid line order in life course row ${index + 2}")
        require(order > 0) { "Line order must be positive in row ${index + 2}" }
        val kind = runCatching { LifeTextKind.valueOf(fields[5]) }
            .getOrElse { throw IllegalArgumentException("Invalid text kind in row ${index + 2}") }
        val idPattern = Regex("[a-z0-9][a-z0-9_-]*")
        require(idPattern.matches(fields[0]) && idPattern.matches(fields[4])) {
            "Invalid stable ID in life course row ${index + 2}"
        }
        LifeCourseRow(
            situationId = fields[0],
            categoryRu = fields[1],
            situationTitleRu = fields[2],
            level = level,
            textId = fields[4],
            kind = kind,
            textTitleRu = fields[6],
            line = LifeLine(order, fields[8], fields[9], fields[10], fields[11]),
        )
    }
    require(parsed.isNotEmpty()) { "Life course has no lessons" }
    parsed.groupBy { it.textId }.forEach { (textId, textRows) ->
        require(textRows.map { it.situationId }.distinct().size == 1) {
            "Text ID $textId is reused by different situations"
        }
    }

    return parsed.groupBy { it.situationId }.map { (situationId, situationRows) ->
        require(situationRows.map { it.categoryRu }.distinct().size == 1) {
            "Inconsistent category for $situationId"
        }
        require(situationRows.map { it.situationTitleRu }.distinct().size == 1) {
            "Inconsistent title for $situationId"
        }
        require(situationRows.map { it.level }.distinct().size == 1) {
            "Inconsistent level for $situationId"
        }
        val texts = situationRows.groupBy { it.textId }.map { (textId, textRows) ->
            require(textRows.map { it.kind }.distinct().size == 1) { "Inconsistent kind for $textId" }
            require(textRows.map { it.textTitleRu }.distinct().size == 1) { "Inconsistent title for $textId" }
            val lines = textRows.map { it.line }
            require(lines.size >= 2) { "Life text $textId needs at least two lines" }
            require(lines.map { it.order } == (1..lines.size).toList()) {
                "Line order for $textId must be contiguous and start at one"
            }
            if (textRows.first().kind == LifeTextKind.DIALOGUE) {
                require(lines.map { it.speaker }.distinct().size >= 2) {
                    "Dialogue $textId needs at least two speakers"
                }
            }
            LifeText(textId, textRows.first().kind, textRows.first().textTitleRu, lines)
        }
        require(texts.size == LifeTextKind.entries.size && texts.map { it.kind }.toSet() == LifeTextKind.entries.toSet()) {
            "Situation $situationId must contain exactly one dialogue and one story"
        }
        LifeSituation(
            id = situationId,
            categoryRu = situationRows.first().categoryRu,
            titleRu = situationRows.first().situationTitleRu,
            level = situationRows.first().level,
            texts = texts,
        )
    }
}
