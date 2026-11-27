package com.hanziflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DialogueTest {
    @Test fun plainAndStructuredRows() {
        assertEquals(
            listOf(
                DialogueLine("你好！"),
                DialogueLine("你好吗？", "Nǐ hǎo ma?", "Как ты?")
            ),
            parseDialogueLines("你好！\n\n你好吗？\tNǐ hǎo ma?\tКак ты?")
        )
    }

    @Test fun trimsAndLimitsPrivateInput() {
        assertEquals(
            listOf(DialogueLine("第一句"), DialogueLine("第二句")),
            parseDialogueLines("  第一句  \n第二句\n第三句", limit = 2)
        )
        assertFailsWith<IllegalArgumentException> { parseDialogueLines("你好", limit = 0) }
    }
}
