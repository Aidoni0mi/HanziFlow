package com.hanziflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LifeCourseTest {
    private val header = "situation_id\tcategory_ru\tsituation_title_ru\tlevel\ttext_id\tkind\ttext_title_ru\tline_order\tspeaker\thanzi\tpinyin\trussian"

    @Test fun parsesDialogueAndStory() {
        val course = parseLifeCourse(
            listOf(
                header,
                "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t1\tГость\t我要茶。\tWǒ yào chá.\tЯ хочу чай.",
                "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t2\tОфициант\t好的。\tHǎo de.\tХорошо.",
                "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t1\tРассказчик\t小王去吃饭。\tXiǎo Wáng qù chīfàn.\tСяо Ван пошёл поесть.",
                "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t2\tРассказчик\t他点了茶。\tTā diǎn le chá.\tОн заказал чай.",
            ).joinToString("\n")
        )
        assertEquals(1, course.size)
        assertEquals("В кафе", course.single().titleRu)
        assertEquals(setOf(LifeTextKind.DIALOGUE, LifeTextKind.STORY), course.single().texts.map { it.kind }.toSet())
        assertEquals("Wǒ yào chá.", course.single().texts.first().lines.first().pinyin)
    }

    @Test fun rejectsMissingStoryAndBrokenOrder() {
        val onlyDialogue = listOf(
            header,
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t1\tГость\t我要茶。\tWǒ yào chá.\tЯ хочу чай.",
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t2\tОфициант\t好的。\tHǎo de.\tХорошо.",
        ).joinToString("\n")
        assertFailsWith<IllegalArgumentException> { parseLifeCourse(onlyDialogue) }

        val brokenOrder = listOf(
            header,
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t2\tГость\t我要茶。\tWǒ yào chá.\tЯ хочу чай.",
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t3\tОфициант\t好的。\tHǎo de.\tХорошо.",
            "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t1\tРассказчик\t小王去吃饭。\tXiǎo Wáng qù chīfàn.\tСяо Ван пошёл поесть.",
            "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t2\tРассказчик\t他点了茶。\tTā diǎn le chá.\tОн заказал чай.",
        ).joinToString("\n")
        assertFailsWith<IllegalArgumentException> { parseLifeCourse(brokenOrder) }
    }

    @Test fun rejectsExtraDialogueAndTextIdReusedAcrossSituations() {
        val extraDialogue = listOf(
            header,
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t1\tГость\t我要茶。\tWǒ yào chá.\tЯ хочу чай.",
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue\tDIALOGUE\tЗаказ\t2\tОфициант\t好的。\tHǎo de.\tХорошо.",
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue_two\tDIALOGUE\tОплата\t1\tГость\t买单。\tMǎidān.\tСчёт, пожалуйста.",
            "cafe\tЕда\tВ кафе\t1\tcafe_dialogue_two\tDIALOGUE\tОплата\t2\tОфициант\t好的。\tHǎo de.\tХорошо.",
            "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t1\tРассказчик\t小王去吃饭。\tXiǎo Wáng qù chīfàn.\tСяо Ван пошёл поесть.",
            "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t2\tРассказчик\t他点了茶。\tTā diǎn le chá.\tОн заказал чай.",
        ).joinToString("\n")
        assertFailsWith<IllegalArgumentException> { parseLifeCourse(extraDialogue) }

        val reusedTextId = listOf(
            header,
            "cafe\tЕда\tВ кафе\t1\tshared_dialogue\tDIALOGUE\tЗаказ\t1\tГость\t我要茶。\tWǒ yào chá.\tЯ хочу чай.",
            "cafe\tЕда\tВ кафе\t1\tshared_dialogue\tDIALOGUE\tЗаказ\t2\tОфициант\t好的。\tHǎo de.\tХорошо.",
            "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t1\tРассказчик\t小王去吃饭。\tXiǎo Wáng qù chīfàn.\tСяо Ван пошёл поесть.",
            "cafe\tЕда\tВ кафе\t1\tcafe_story\tSTORY\tОбед\t2\tРассказчик\t他点了茶。\tTā diǎn le chá.\tОн заказал чай.",
            "shop\tПокупки\tВ магазине\t1\tshared_dialogue\tDIALOGUE\tПокупка\t1\tПокупатель\t多少钱？\tDuōshao qián?\tСколько стоит?",
            "shop\tПокупки\tВ магазине\t1\tshared_dialogue\tDIALOGUE\tПокупка\t2\tПродавец\t十块。\tShí kuài.\tДесять юаней.",
            "shop\tПокупки\tВ магазине\t1\tshop_story\tSTORY\tПокупка\t1\tРассказчик\t小李买水果。\tXiǎo Lǐ mǎi shuǐguǒ.\tСяо Ли покупает фрукты.",
            "shop\tПокупки\tВ магазине\t1\tshop_story\tSTORY\tПокупка\t2\tРассказчик\t他付了钱。\tTā fù le qián.\tОн заплатил.",
        ).joinToString("\n")
        assertFailsWith<IllegalArgumentException> { parseLifeCourse(reusedTextId) }
    }
}
