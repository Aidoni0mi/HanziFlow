package com.hanziflow.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private data class Line(val han:String,val py:String,val ru:String)
private data class Dialogue(val title:String,val lines:List<Line>)
private val dialogues=listOf(
    Dialogue("Знакомство",listOf(Line("你好！你叫什么名字？","Nǐ hǎo! Nǐ jiào shénme míngzi?","Привет! Как тебя зовут?"),Line("我叫李明。你呢？","Wǒ jiào Lǐ Míng. Nǐ ne?","Меня зовут Ли Мин. А тебя?"),Line("很高兴认识你。","Hěn gāoxìng rènshi nǐ.","Очень приятно познакомиться."))),
    Dialogue("В кафе",listOf(Line("你想喝什么？","Nǐ xiǎng hē shénme?","Что ты хочешь выпить?"),Line("我要一杯茶，谢谢。","Wǒ yào yì bēi chá, xièxie.","Мне чашку чая, спасибо."),Line("还要吃点儿什么吗？","Hái yào chī diǎnr shénme ma?","Хочешь ещё что-нибудь поесть?"))),
    Dialogue("Учёба",listOf(Line("你为什么学习汉语？","Nǐ wèishénme xuéxí Hànyǔ?","Почему ты изучаешь китайский?"),Line("因为我想去中国读书。","Yīnwèi wǒ xiǎng qù Zhōngguó dúshū.","Потому что хочу учиться в Китае."),Line("那我们一起练习吧。","Nà wǒmen yìqǐ liànxí ba.","Тогда давай заниматься вместе."))),
    Dialogue("Подготовка к экзамену",listOf(Line("你准备得怎么样了？","Nǐ zhǔnbèi de zěnmeyàng le?","Как идёт подготовка?"),Line("虽然语法不难，但是听力还需要练习。","Suīrán yǔfǎ bù nán, dànshì tīnglì hái xūyào liànxí.","Хотя грамматика несложная, аудирование ещё нужно тренировать."),Line("每天听一段对话，再复述一遍。","Měitiān tīng yí duàn duìhuà, zài fùshù yí biàn.","Каждый день слушай один диалог, затем пересказывай его.")))
)
@Composable private fun StudyButton(label:String,onClick:()->Unit){OutlinedButton(onClick,Modifier.fillMaxWidth()){Text(label)}}
@Composable fun SpeechHub(vm:HanziViewModel,tones:@Composable ()->Unit){
    var mode by rememberSaveable{mutableStateOf("Тоны")}
    LaunchedEffect(vm.speechRequest){if(vm.speechRequest>0)mode="Речь"}
    Column(Modifier.fillMaxSize()){
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Тоны","Речь","На слух").forEach{m->FilterChip(selected=mode==m,onClick={vm.stopAudio();mode=m},label={Text(m)},modifier=Modifier.weight(1f))}}
        Box(Modifier.weight(1f)){when(mode){"Тоны"->tones();"Речь"->SpeechPractice(vm);else->ListeningPractice(vm)}}
    }
}
@Composable private fun SpeechPractice(vm:HanziViewModel){
    val ctx=LocalContext.current
    var mode by rememberSaveable{mutableStateOf("Иероглифы")}
    var index by rememberSaveable(vm.level,mode){mutableIntStateOf(0)}
    var custom by rememberSaveable{mutableStateOf(vm.practiceTarget)}
    LaunchedEffect(vm.speechRequest){if(vm.speechRequest>0){mode="Свой текст";custom=vm.practiceTarget}}
    val chars=remember(vm.curriculum,vm.level){vm.curriculum?.hanzi?.filter{it.band<=vm.level}.orEmpty()}
    val words=remember(vm.curriculum,vm.level){vm.curriculum?.entries?.filter{it.band<=vm.level}.orEmpty()}
    val h=chars.getOrNull(index);val w=words.getOrNull(index)
    val target=when(mode){"Иероглифы"->h?.hanzi.orEmpty();"Слова"->w?.word?.simplified.orEmpty();else->custom}
    val count=if(mode=="Иероглифы")chars.size else words.size
    val check=vm.speechCheck
    val mic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted&&target.isNotBlank()&&!vm.recording&&!vm.playing){vm.speech.stop();check.start(target)}else vm.message("Микрофон недоступен")}
    LaunchedEffect(target){check.reset()}
    DisposableEffect(Unit){onDispose{check.cancel()}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Произнеси и проверь",style=MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Иероглифы","Слова","Свой текст").forEach{m->FilterChip(selected=mode==m,onClick={mode=m},label={Text(m,fontSize=11.sp)},modifier=Modifier.weight(1f))}}
        if(mode=="Свой текст")OutlinedTextField(custom,{custom=it.take(500)},label={Text("Слово или предложение")},modifier=Modifier.fillMaxWidth(),minLines=2)
        else{Text("${index+1} / $count · "+hskLabel(vm.level));Text(target,fontSize=42.sp);Text(if(mode=="Иероглифы")h?.readings.orEmpty() else w?.word?.pinyin.orEmpty())
            Row{OutlinedButton({index=(index-1).coerceAtLeast(0)},enabled=index>0){Text("Назад")};Spacer(Modifier.weight(1f));OutlinedButton({index=(index+1).coerceAtMost((count-1).coerceAtLeast(0))},enabled=index+1<count){Text("Далее")}}
            if(mode=="Иероглифы")Text("Для многозначного иероглифа лучше проверить слово с нужным чтением.")
        }
        Row{TextButton({vm.speak(target)},enabled=target.isNotBlank()&&!check.busy){Text("Прослушать")};TextButton({vm.speak(target,.7f)},enabled=target.isNotBlank()&&!check.busy){Text("0.7×")}}
        Button({if(ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){vm.speech.stop();check.start(target)}else mic.launch(Manifest.permission.RECORD_AUDIO)},enabled=target.isNotBlank()&&!check.busy&&!vm.recording&&!vm.playing,modifier=Modifier.fillMaxWidth()){Text(if(check.busy)"Слушаю…" else "Произнести")}
        if(check.busy)StudyButton("Отменить"){check.cancel()}
        if(check.heard.isNotBlank()){Text("Распознано:");Text(check.heard,fontSize=24.sp)}
        if(check.feedback.isNotBlank())Text(check.feedback)
        Text("Проверяется совпадение распознанного текста, не точность отдельных тонов или звуков. Для анализа F0 откройте «Тоны». Нужен локальный Mandarin-пакет Android 12+. Облачного распознавания нет.")
    }
}
@Composable private fun ListeningPractice(vm:HanziViewModel){
    val pool=remember(vm.curriculum,vm.level){vm.curriculum?.entries?.filter{it.band<=vm.level}.orEmpty()}
    var round by rememberSaveable(vm.level){mutableIntStateOf(0)}
    val options=remember(pool,round){pool.shuffled(kotlin.random.Random(round+vm.level*31)).distinctBy{it.word.simplified}.take(4)}
    val target=options.firstOrNull()
    val shown=remember(options){options.shuffled(kotlin.random.Random(round+93))}
    var selected by rememberSaveable(vm.level,round){mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Услышь и выбери",style=MaterialTheme.typography.headlineSmall)
        if(target==null){Text("Загрузка словаря…");return@Column}
        StudyButton("Прослушать слово"){vm.speak(target.word.simplified)}
        StudyButton("Медленнее"){vm.speak(target.word.simplified,.7f)}
        shown.forEach{w->OutlinedButton({selected=w.word.id},enabled=selected.isBlank(),modifier=Modifier.fillMaxWidth()){Text(w.word.simplified,fontSize=24.sp)}}
        if(selected.isNotBlank()){Text(if(selected==target.word.id)"Верно" else "Ответ: "+target.word.simplified);Text(target.word.pinyin);StudyButton("Следующее слово"){round++}}
    }
}
@Composable fun DialogueStudy(vm:HanziViewModel){
    val ctx=LocalContext.current;val prefs=remember(ctx){ctx.getSharedPreferences("personal_dialogue",0)}
    var index by rememberSaveable{mutableIntStateOf(0)}
    var custom by rememberSaveable{mutableStateOf(false)}
    var pasted by rememberSaveable{mutableStateOf(prefs.getString("text","").orEmpty())}
    var show by rememberSaveable{mutableStateOf(true)}
    val d=dialogues[index]
    val lines=if(custom)pasted.lines().filter{it.isNotBlank()}.take(100).map{Line(it,"","")}else d.lines
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Диалоги",style=MaterialTheme.typography.headlineSmall)
        Text("Авторские диалоги · не тексты официальных учебников")
        Row{OutlinedButton({index=(index+dialogues.size-1)%dialogues.size;custom=false}){Text("Назад")};Spacer(Modifier.weight(1f));OutlinedButton({index=(index+1)%dialogues.size;custom=false}){Text("Далее")}}
        StudyButton(if(custom)"Авторские диалоги" else "Мой диалог из учебника"){custom=!custom}
        if(custom){OutlinedTextField(pasted,{pasted=it.take(12000)},label={Text("Вставьте реплики, по одной на строку")},modifier=Modifier.fillMaxWidth(),minLines=4);StudyButton("Сохранить на устройстве"){prefs.edit().putString("text",pasted).apply();vm.message("Диалог сохранён")}}
        else Text(d.title,style=MaterialTheme.typography.titleLarge)
        StudyButton(if(show)"Скрыть текст" else "Показать текст"){show=!show}
        StudyButton("Прослушать диалог"){vm.speak(lines.joinToString("。"){it.han})}
        lines.forEachIndexed{i,l->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
            Text("Реплика ${i+1}")
            if(show){Text(l.han,fontSize=24.sp);if(l.py.isNotBlank())Text(l.py);if(l.ru.isNotBlank())Text(l.ru)}
            Row{TextButton({vm.speak(l.han)}){Text("Слушать")};TextButton({vm.practice(l.han)}){Text("Повторить")}}
        }}}
    }
}
@Composable fun DailyStudy(vm:HanziViewModel){
    val ctx=LocalContext.current;val prefs=remember(ctx){ctx.getSharedPreferences("daily_study",0)}
    val day=LocalDate.now().toString()+"-"+vm.level
    var step by rememberSaveable(day){mutableIntStateOf(prefs.getInt(day,0).coerceIn(0,3))}
    val pool=remember(vm.curriculum,vm.level){vm.curriculum?.entries?.filter{it.band==vm.level}.orEmpty()}
    val words=remember(pool,day){if(pool.isEmpty())emptyList() else List(minOf(10,pool.size)){i->pool[Math.floorMod(LocalDate.now().toEpochDay()*10+i,pool.size.toLong()).toInt()]}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Урок дня · "+hskLabel(vm.level),style=MaterialTheme.typography.headlineSmall)
        LinearProgressIndicator(progress={step/3f},modifier=Modifier.fillMaxWidth())
        Text(listOf("1. Лексика","2. Диалог","3. Аудирование и повторение","Урок отмечен завершённым")[step])
        when(step){
            0->words.forEach{e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text(e.word.simplified,fontSize=24.sp);Text(e.word.pinyin);Text(e.word.russian.ifBlank{vm.curriculum?.englishById?.get(e.word.id)?.let{"EN: $it"}?:"Перевод не добавлен"});Row{TextButton({vm.speak(e.word.simplified)}){Text("Слушать")};TextButton({vm.add(e.word)}){Text("В повторение")}}}}}
            1-> {val d=dialogues[Math.floorMod(LocalDate.now().toEpochDay(),dialogues.size.toLong()).toInt()];Text(d.title);d.lines.forEach{Text(it.han);Text(it.ru);StudyButton("Прослушать"){vm.speak(it.han)}}}
            2-> {Text("Прослушайте слова, затем произнесите их самостоятельно. Оцените себя по ответу.");words.take(3).forEach{e->StudyButton("Произнести "+e.word.simplified){vm.practice(e.word.simplified)}}}
            else->Text("Отметка сохранена локально. Это авторский маршрут, не полный урок официального HSK.")
        }
        if(step<3)StudyButton("Отметить этап выполненным"){step++;prefs.edit().putInt(day,step).apply()}
        if(step>0)StudyButton("Предыдущий этап"){step--;prefs.edit().putInt(day,step).apply()}
    }
}
