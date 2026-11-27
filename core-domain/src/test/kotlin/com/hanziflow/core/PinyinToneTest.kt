package com.hanziflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PinyinToneTest {
    @Test fun parsesToneMarksAndPreservesUmlaut() {
        val syllables = pinyinToneSyllables("Nǐ hǎo! Wǒ jiào Lǜ.")
        assertEquals(listOf("ni", "hao", "wo", "jiao", "lü"), syllables.map { it.plain })
        assertEquals(listOf(3, 3, 3, 4, 4), syllables.map { it.tone })
        assertEquals("Nǐ", syllables.first().display)
    }

    @Test fun parsesNumericPinyinAndUmlautAliases() {
        val syllables = pinyinToneSyllables("nu:3 lv4 nüe4 ma0 de5 ni3hao3")
        assertEquals(
            listOf("nü", "lü", "nüe", "ma", "de", "ni", "hao"),
            syllables.map { it.plain },
        )
        assertEquals(listOf(3, 4, 4, 5, 5, 3, 3), syllables.map { it.tone })
    }

    @Test fun handlesCaseApostrophesHyphensAndPunctuation() {
        val syllables = pinyinToneSyllables("XI1'AN1, NÜ3-ER2; ma?")
        assertEquals(listOf("xi", "an", "nü", "er", "ma"), syllables.map { it.plain })
        assertEquals(listOf(1, 1, 3, 2, 5), syllables.map { it.tone })
    }

    @Test fun splitsSyllablesJoinedInsidePinyinWords() {
        val syllables = pinyinToneSyllables("shénme gāoxìng yìdiǎnr Xiǎoyǔ xué")
        assertEquals(
            listOf("shen", "me", "gao", "xing", "yi", "dianr", "xiao", "yu", "xue"),
            syllables.map { it.plain },
        )
        assertEquals(listOf(2, 5, 1, 4, 4, 3, 3, 3, 2), syllables.map { it.tone })
    }

    @Test fun keepsRAsTheNextInitialUnlessErhuaEndsTheWord() {
        val syllables = pinyinToneSyllables("lùrén fùzérén shōukuǎnrén shōujiànrén yìdiǎnr")
        assertEquals(
            listOf("lu", "ren", "fu", "ze", "ren", "shou", "kuan", "ren", "shou", "jian", "ren", "yi", "dianr"),
            syllables.map { it.plain },
        )
        assertEquals(listOf(4, 2, 4, 2, 2, 1, 3, 2, 1, 4, 2, 4, 3), syllables.map { it.tone })
    }

    @Test fun handlesSyllabicConsonantsAndCombiningMarks() {
        val syllables = pinyinToneSyllables("m̄ ḿ m̌ m̀, ń ň ǹ")
        assertEquals(List(7) { if (it < 4) "m" else "n" }, syllables.map { it.plain })
        assertEquals(listOf(1, 2, 3, 4, 2, 3, 4), syllables.map { it.tone })
    }

    @Test fun explicitSpellingControlsToneWithoutHiddenSandhi() {
        val syllables = pinyinToneSyllables("nǐ2 nǐ hǎo ní hǎo")
        assertEquals(listOf(2, 3, 3, 2, 3), syllables.map { it.tone })
    }

    @Test fun buildsDisplayOnlyPhraseTemplate() {
        val contour = phraseToneTemplate("shì shénme")
        assertEquals(9, contour.size)
        assertEquals(0.0, contour.first().time, 0.0001)
        assertEquals(1.0, contour.last().time, 0.0001)
        assertTrue(contour.all { it.time in 0.0..1.0 })
        assertTrue(contour.first().relativeSemitones > contour[2].relativeSemitones)
        assertTrue(contour[3].relativeSemitones < contour[5].relativeSemitones)

        val neutral = phraseToneTemplate("ma5")
        assertEquals(listOf(0.0, 0.5, 1.0), neutral.map { it.time })
        assertTrue(neutral.all { it.relativeSemitones == 0.0 })
        assertTrue(phraseToneTemplate("？！").isEmpty())
    }
}
