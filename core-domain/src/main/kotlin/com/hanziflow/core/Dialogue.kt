package com.hanziflow.core

data class DialogueLine(
    val chinese: String,
    val pinyin: String = "",
    val russian: String = ""
)

/**
 * Parses a private, user-supplied dialogue without adding it to bundled content.
 * Each non-empty row may contain Chinese only, or Chinese, pinyin and Russian
 * separated by tabs. The final column keeps any additional tabs as plain text.
 */
fun parseDialogueLines(text: String, limit: Int = 100): List<DialogueLine> {
    require(limit > 0) { "Dialogue line limit must be positive" }
    return text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(limit)
        .map { row ->
            val fields = row.split('\t', limit = 3).map { it.trim() }
            DialogueLine(
                chinese = fields[0],
                pinyin = fields.getOrElse(1) { "" },
                russian = fields.getOrElse(2) { "" }
            )
        }
        .toList()
}
