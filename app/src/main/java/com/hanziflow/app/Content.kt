package com.hanziflow.app

data class Grammar(val chinese:String,val name:String,val formula:String,val explanation:String,val example:String,val pinyin:String,val ru:String)
val grammar = listOf(
    Grammar("正在","Действие в процессе","S + 正在 + V","Указывает на действие, выполняющееся в данный момент. Не используется как обычное обозначение завершённого действия.","我正在学习汉语。","Wǒ zhèngzài xuéxí Hànyǔ.","Я сейчас изучаю китайский."),
    Grammar("因为……所以……","Причина и следствие","因为 + причина，所以 + результат","因为 вводит причину, 所以 — следствие. В контексте один из союзов может опускаться.","因为我想提高汉语水平，所以每天练习。","Yīnwèi wǒ xiǎng tígāo Hànyǔ shuǐpíng, suǒyǐ měitiān liànxí.","Поскольку я хочу повысить уровень китайского, я практикуюсь каждый день."),
    Grammar("虽然……但是……","Уступка","虽然 + факт，但是 + противопоставление","Указывает, что результат отличается от ожидаемого несмотря на факт.","虽然发音很难，但是我每天练习。","Suīrán fāyīn hěn nán, dànshì wǒ měitiān liànxí.","Хотя произношение сложное, я практикуюсь ежедневно."),
    Grammar("比","Сравнение","A + 比 + B + Adj","比 вводит объект сравнения. Обычно 很 не ставится перед прилагательным в такой сравнительной конструкции.","今天比昨天冷。","Jīntiān bǐ zuótiān lěng.","Сегодня холоднее, чем вчера."),
    Grammar("把","Объект воздействия","S + 把 + O + V + результат","Подчёркивает изменение конкретного объекта. После глагола обычно нужен элемент, описывающий результат или направление.","请把书放在桌子上。","Qǐng bǎ shū fàng zài zhuōzi shang.","Положи книгу на стол.")
)
data class Question(val prompt:String,val options:List<String>,val answer:Int,val explanation:String)
val questions=listOf(
    Question("我___学习汉语，现在不能出去。",listOf("正在","昨天","结果"),0,"正在 обозначает действие в процессе."),
    Question("因为下雨，___我们没出门。",listOf("但是","所以","虽然"),1,"所以 вводит следствие."),
    Question("___发音很难，但是我每天练习。",listOf("因为","所以","虽然"),2,"虽然…但是… — уступительное отношение."),
    Question("提高汉语水平",listOf("повысить уровень китайского","проверить расписание","написать письмо"),0,"提高 — повышать; 水平 — уровень."),
    Question("今天___昨天冷。",listOf("把","比","正在"),1,"比 вводит объект сравнения.")
)
const val practiceText="我正在学习汉语。因为我想提高汉语水平，所以每天坚持练习发音。虽然声调很难，但是我有一个学习计划。我正在准备考试。"
const val practicePinyin="Wǒ zhèngzài xuéxí Hànyǔ. Yīnwèi wǒ xiǎng tígāo Hànyǔ shuǐpíng, suǒyǐ měitiān jiānchí liànxí fāyīn. Suīrán shēngdiào hěn nán, dànshì wǒ yǒu yí ge xuéxí jìhuà. Wǒ zhèngzài zhǔnbèi kǎoshì."
const val practiceRu="Я изучаю китайский. Поскольку хочу повысить свой уровень, каждый день продолжаю тренировать произношение. Хотя тоны сложные, у меня есть учебный план. Я готовлюсь к экзамену."

data class ReadingLesson(val title:String,val text:String,val pinyin:String,val ru:String)
val readings = listOf(
    ReadingLesson("Учебный план",practiceText,practicePinyin,practiceRu),
    ReadingLesson("Библиотека","为了准备考试，我每天去图书馆。如果遇到不懂的词，我就查词典。这样不仅能学到新词，还能提高阅读能力。","Wèile zhǔnbèi kǎoshì, wǒ měitiān qù túshūguǎn. Rúguǒ yùdào bù dǒng de cí, wǒ jiù chá cídiǎn. Zhèyàng bùjǐn néng xuédào xīncí, hái néng tígāo yuèdú nénglì.","Чтобы подготовиться к экзамену, я каждый день хожу в библиотеку. Если встречаю непонятное слово, то смотрю его в словаре. Так можно не только выучить новые слова, но и улучшить навыки чтения."),
    ReadingLesson("Здоровые привычки","以前我常常很晚才睡觉，第二天总是觉得累。现在我开始注意健康，每天运动半个小时。只要坚持，就会慢慢养成好习惯。","Yǐqián wǒ chángcháng hěn wǎn cái shuìjiào, dì èr tiān zǒngshì juéde lèi. Xiànzài wǒ kāishǐ zhùyì jiànkāng, měitiān yùndòng bàn ge xiǎoshí. Zhǐyào jiānchí, jiù huì mànmàn yǎngchéng hǎo xíguàn.","Раньше я часто ложился спать очень поздно и на следующий день всегда чувствовал усталость. Теперь я уделяю внимание здоровью и каждый день занимаюсь спортом полчаса. Если продолжать, постепенно сформируется хорошая привычка."),
    ReadingLesson("Поездка","周末我打算和朋友一起去旅行。出发以前，我们先了解当地的天气，然后选择合适的交通方式。虽然时间不多，但是我们希望玩得开心。","Zhōumò wǒ dǎsuàn hé péngyou yìqǐ qù lǚxíng. Chūfā yǐqián, wǒmen xiān liǎojiě dāngdì de tiānqì, ránhòu xuǎnzé héshì de jiāotōng fāngshì. Suīrán shíjiān bù duō, dànshì wǒmen xīwàng wán de kāixīn.","На выходных я собираюсь отправиться в путешествие с другом. Перед отъездом мы сначала узнаем местную погоду, а затем выберем подходящий транспорт. Хотя времени мало, мы надеемся хорошо провести время."),
    ReadingLesson("Работа в команде","老师让我们一起完成一个任务。每个人都有不同的想法，所以我们先讨论，再决定怎么做。遇到问题的时候，大家互相帮助，最后顺利完成了任务。","Lǎoshī ràng wǒmen yìqǐ wánchéng yí ge rènwu. Měi ge rén dōu yǒu bùtóng de xiǎngfǎ, suǒyǐ wǒmen xiān tǎolùn, zài juédìng zěnme zuò. Yùdào wèntí de shíhou, dàjiā hùxiāng bāngzhù, zuìhòu shùnlì wánchéng le rènwu.","Преподаватель поручил нам вместе выполнить задание. У каждого были свои идеи, поэтому мы сначала обсудили их, а затем решили, что делать. При возникновении проблем все помогали друг другу и в итоге успешно завершили задание.")
)
val extraGrammar = listOf(
    Grammar("如果……就……","Условие","如果 + условие，就 + результат","就 вводит следствие при выполнении условия.","如果遇到问题，我就问老师。","Rúguǒ yùdào wèntí, wǒ jiù wèn lǎoshī.","Если возникнет проблема, я спрошу преподавателя."),
    Grammar("不仅……还……","Добавление","不仅 + A，还 + B","Вводит дополнительный факт; при общем подлежащем оно обычно перед 不仅.","他不仅会说汉语，还会说英语。","Tā bùjǐn huì shuō Hànyǔ, hái huì shuō Yīngyǔ.","Он говорит не только по-китайски, но и по-английски."),
    Grammar("只要……就……","Достаточное условие","只要 + условие，就 + результат","Указывает на достаточное условие; не путайте с 只有……才……, которое обозначает необходимое условие.","只要坚持，就会进步。","Zhǐyào jiānchí, jiù huì jìnbù.","Если продолжать заниматься, будет прогресс."),
    Grammar("先……然后……","Последовательность","先 + A，然后 + B","先 отмечает первое действие, 然后 вводит следующее.","我先复习，然后做练习。","Wǒ xiān fùxí, ránhòu zuò liànxí.","Я сначала повторю материал, затем сделаю упражнения."),
    Grammar("让","Побуждение","S + 让 + O + V","В зависимости от контекста 让 означает позволить, попросить или побудить выполнить действие.","老师让我们一起完成任务。","Lǎoshī ràng wǒmen yìqǐ wánchéng rènwu.","Преподаватель поручил нам вместе выполнить задание.")
)
val allGrammar = grammar + extraGrammar
data class CharacterPractice(val han:String,val pinyin:String,val ru:String,val example:String)
val characterPractice = listOf(
    CharacterPractice("学","xué","учиться","学习"), CharacterPractice("习","xí","упражняться; привычка","练习"),
    CharacterPractice("提","tí","поднимать; выдвигать","提高"), CharacterPractice("高","gāo","высокий","提高"),
    CharacterPractice("阅","yuè","просматривать; читать","阅读"), CharacterPractice("读","dú","читать","阅读"),
    CharacterPractice("查","chá","искать; проверять","查词典"), CharacterPractice("词","cí","слово","词典"),
    CharacterPractice("健","jiàn","здоровый; крепкий","健康"), CharacterPractice("康","kāng","здоровье; благополучие","健康"),
    CharacterPractice("旅","lǚ","путешествие","旅行"), CharacterPractice("行","xíng / háng","идти; ряд (в зависимости от слова)","旅行（lǚxíng）"),
    CharacterPractice("讨","tǎo","обсуждать; требовать","讨论"), CharacterPractice("论","lùn","рассуждать; обсуждать","讨论"),
    CharacterPractice("任","rèn","поручение; назначать (также другие чтения)","任务"), CharacterPractice("务","wù","дело; обязанность","任务")
)
data class ToneExample(val text:String,val pinyin:String,val ru:String)
val toneExamples = listOf(
    listOf(ToneExample("妈","mā","мама (в обращении)"),ToneExample("高","gāo","высокий"),ToneExample("书","shū","книга")),
    listOf(ToneExample("麻","má","конопля; онемение"),ToneExample("来","lái","приходить"),ToneExample("茶","chá","чай")),
    listOf(ToneExample("马","mǎ","лошадь"),ToneExample("好","hǎo","хороший"),ToneExample("你","nǐ","ты")),
    listOf(ToneExample("骂","mà","ругать"),ToneExample("大","dà","большой"),ToneExample("去","qù","идти; отправляться")),
    listOf(ToneExample("妈妈","māma","мама"),ToneExample("伯伯","bóbo","дядя"),ToneExample("姐姐","jiějie","старшая сестра"),ToneExample("兔子","tùzi","кролик"))
)
