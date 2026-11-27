package com.hanziflow.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanziflow.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class MainActivity: ComponentActivity() {
    private var model: HanziViewModel?=null
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { val vm:HanziViewModel=viewModel(); model=vm; HanziApp(vm) } }
    override fun onStop(){model?.stopAudio();super.onStop()}
}
class HanziViewModel(app:Application):AndroidViewModel(app) {
    private val db=Store(app); val files=LibraryFiles(app); val speech=Speech(app)
    val speechCheck=LocalSpeechCheck(app)
    private val dbMutex=Mutex()
    var reviewBusy by mutableStateOf(false); private set
    var practiceTarget by mutableStateOf(""); private set
    var speechRequest by mutableIntStateOf(0); private set
    fun practice(text:String){stopAudio();practiceTarget=text;speechRequest++}
    private val prefs=app.getSharedPreferences("settings",0)
    val starterWords=readWords(app)
    var curriculum by mutableStateOf<Curriculum?>(null); private set
    var curriculumError by mutableStateOf(""); private set
    var lifeCourse by mutableStateOf<List<LifeSituation>>(emptyList()); private set
    var lifeCourseError by mutableStateOf(""); private set
    private val starterSegmenter=Segmenter(starterWords)
    val words get()=curriculum?.reviewWords?:starterWords
    val segmenter get()=curriculum?.segmenter?:starterSegmenter
    var level by mutableIntStateOf(hskBand(prefs.getInt("level",4))); private set
    var date by mutableStateOf(prefs.getString("date","")?:""); private set
    var minutes by mutableIntStateOf(prefs.getInt("minutes",60)); private set
    var theme by mutableStateOf(prefs.getString("theme","system")?:"system"); private set
    var reviews by mutableStateOf<Map<String,Review>>(emptyMap()); private set
    var books by mutableStateOf<List<Book>>(emptyList()); private set
    var lastQuiz by mutableStateOf("Нет результатов"); private set
    var notice by mutableStateOf(""); private set
    var loading by mutableStateOf(false); private set
    var recording by mutableStateOf(false); private set
    var playing by mutableStateOf(false); private set
    var samples by mutableStateOf<ShortArray?>(null); private set
    var pitch by mutableStateOf<PitchResult?>(null); private set
    private var audioJob:Job?=null
    init { refresh(); loadCurriculum(); loadLifeCourse() }
    fun loadCurriculum(){if(curriculum!=null)return;viewModelScope.launch {
        curriculumError=""
        try{curriculum=withContext(Dispatchers.IO){readCurriculum(getApplication<Application>(),starterWords)}}
        catch(e:CancellationException){throw e}
        catch(e:Exception){curriculumError="Не удалось загрузить учебную базу: "+(e.message?:"ошибка данных")}
    }}
    fun loadLifeCourse(){if(lifeCourse.isNotEmpty())return;viewModelScope.launch {
        lifeCourseError=""
        try{lifeCourse=withContext(Dispatchers.IO){readLifeCourse(getApplication<Application>())}}
        catch(e:CancellationException){throw e}
        catch(e:Exception){lifeCourseError="Не удалось загрузить жизненные ситуации: "+(e.message?:"ошибка данных")}
    }}
    fun updateLevel(n:Int){level=hskBand(n); prefs.edit().putInt("level",level).apply()}
    fun saveDate(value:String){ if(value.isNotBlank() && runCatching{LocalDate.parse(value)}.isFailure){notice="Введите дату в формате ГГГГ-ММ-ДД";return}; date=value;prefs.edit().putString("date",value).apply();notice="Дата сохранена" }
    fun updateMinutes(n:Int){minutes=n; prefs.edit().putInt("minutes",n).apply()}
    fun updateTheme(value:String){theme=value;prefs.edit().putString("theme",value).apply()}
    fun message(text:String){notice=text}
    private suspend fun reload(){val data=withContext(Dispatchers.IO){Triple(db.reviews(),db.books(),db.lastQuiz())};reviews=data.first;books=data.second;lastQuiz=data.third}
    private fun refresh()=viewModelScope.launch{try{dbMutex.withLock{reload()}}catch(e:CancellationException){throw e}catch(e:Exception){notice="Ошибка базы данных"}}
    private fun mutate(block:()->Unit)=viewModelScope.launch{try{dbMutex.withLock{withContext(Dispatchers.IO){block()};reload()}}catch(e:CancellationException){throw e}catch(e:Exception){notice="Не удалось сохранить данные"}}
    fun add(w:Word)=viewModelScope.launch{try{dbMutex.withLock{withContext(Dispatchers.IO){db.add(w.id)};reload()};notice="Карточка сохранена"}catch(e:CancellationException){throw e}catch(e:Exception){notice="Не удалось добавить карточку"}}
    fun rate(w:Word,g:Grade){if(reviewBusy)return;reviewBusy=true;viewModelScope.launch{try{dbMutex.withLock{withContext(Dispatchers.IO){db.grade(w.id,g)};reload()};notice="Оценка сохранена · следующее повторение: "+reviews[w.id]?.due}catch(e:CancellationException){throw e}catch(e:Exception){notice="Не удалось сохранить оценку. Попробуйте снова."}finally{reviewBusy=false}}}
    fun addDailyWords(){val batch=curriculum?.entries?.filter{it.band==level&&!reviews.containsKey(it.word.id)}?.take(10).orEmpty();if(batch.isEmpty()){notice="Нет новых карточек";return};mutate{db.addAll(batch.map{it.word.id})}}
    fun saveQuiz(n:Int,total:Int){mutate{db.quiz(n,total)}}
    fun savePage(b:Book,p:Int){mutate{db.page(b.id,p)}}
    fun tag(b:Book,v:String){mutate{db.version(b.id,v)}}
    fun delete(b:Book){mutate{if(files.delete(b)) db.deleteBook(b.id) else error("File deletion failed")}}
    fun importBook(uri:Uri){if(loading)return; viewModelScope.launch { loading=true;try{val book=files.import(uri);try{withContext(Dispatchers.IO){db.addBook(book)}}catch(e:Exception){files.delete(book);throw e}; refresh();notice="Файл импортирован локально"}catch(e:CancellationException){throw e}catch(e:Exception){notice=e.message?:"Ошибка импорта"}finally{loading=false} }}
    fun speak(text:String,speed:Float=1f){if(speechCheck.busy||recording||playing)return;if(!speech.say(text,speed))notice="Установите офлайн-голос Mandarin Chinese в настройках Android."}
    fun record(seconds:Int=3){if(recording||playing||speechCheck.busy)return; speech.stop(); samples=null;pitch=null;recording=true;audioJob=viewModelScope.launch{try{val pcm=VoiceRecorder.record(seconds);samples=pcm;pitch=withContext(Dispatchers.Default){Pitch.analyze(pcm)}}catch(e:CancellationException){throw e}catch(e:Exception){notice=e.message?:"Ошибка микрофона"}finally{recording=false}}}
    fun play(){val pcm=samples?:return;if(recording||playing||speechCheck.busy)return;speech.stop();playing=true;audioJob=viewModelScope.launch{try{VoiceRecorder.play(pcm)}catch(e:CancellationException){throw e}catch(e:Exception){notice="Ошибка воспроизведения"}finally{playing=false}}}
    fun stopAudio(){audioJob?.cancel();speech.stop();speechCheck.cancel()}
    fun erase(){stopAudio();samples=null;pitch=null}
    override fun onCleared(){stopAudio();speechCheck.close();speech.close();db.close();super.onCleared()}
}
private val light=lightColorScheme(primary=Color(0xFF006B61),secondary=Color(0xFF4D635F),tertiary=Color(0xFF79591D),surface=Color(0xFFF7FAF8),background=Color(0xFFF7FAF8))
private val dark=darkColorScheme(primary=Color(0xFF79D8C8),secondary=Color(0xFFB1CCC4),tertiary=Color(0xFFE7C17B),surface=Color(0xFF101916),background=Color(0xFF101916))
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HanziApp(vm:HanziViewModel) {
    val darkMode=vm.theme=="dark" || (vm.theme=="system" && isSystemInDarkTheme())
    MaterialTheme(colorScheme=if(darkMode)dark else light) {
        var tab by rememberSaveable{mutableStateOf("Главная")}; var sub by rememberSaveable{mutableStateOf("Меню")}
        val snackbar=remember{SnackbarHostState()}
        LaunchedEffect(vm.notice){if(vm.notice.isNotEmpty()){snackbar.showSnackbar(vm.notice);vm.message("")}}
        val tabs=listOf("Главная" to "学","Учёба" to "书","Речь" to "声","Экзамены" to "考","Профиль" to "我")
        fun navigate(t:String,s:String="Меню"){if(tab!=t)vm.stopAudio();tab=t;sub=s}
        LaunchedEffect(vm.speechRequest){if(vm.speechRequest>0){tab="Речь";sub="Меню"}}
        Scaffold(modifier=Modifier.safeDrawingPadding(),topBar={TopAppBar(title={Column{Text("HanziFlow",fontWeight=FontWeight.Bold);Text("HSK ${vm.level} · 3.0",style=MaterialTheme.typography.labelMedium)}})}, snackbarHost={SnackbarHost(snackbar)},bottomBar={NavigationBar{tabs.forEach{(name,han)-> NavigationBarItem(selected=tab==name,onClick={navigate(name)},icon={Text(han,fontSize=22.sp)},label={Text(name,fontSize=10.sp)})}}}){padding->
            Box(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)) {
                when(tab){"Главная"->Home(vm){t,s->navigate(t,s)};"Учёба"->Learn(vm,sub){sub=it};"Речь"->SpeechHub(vm){ToneScreen(vm)};"Экзамены"->ExamScreen(vm);else->Profile(vm)}
            }
        }
    }
}
@Composable private fun Page(title:String,body:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);body();Spacer(Modifier.height(16.dp))}}
@Composable private fun Panel(title:String,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);content()}}}
@Composable private fun WideButton(text:String,onClick:()->Unit){OutlinedButton(onClick,Modifier.fillMaxWidth()){Text(text)}}
@Composable private fun Home(vm:HanziViewModel,go:(String,String)->Unit)=Page("Сегодня"){
    Panel("HSK ${vm.level} · личная цель") { Text(if(vm.date.isBlank()) "Дата экзамена не установлена" else "${vm.date} · ${ChronoUnit.DAYS.between(LocalDate.now(),LocalDate.parse(vm.date)).coerceAtLeast(0)} дн. до экзамена");Text("${vm.reviews.values.count{it.repetitions>=3}} карточек с 3+ успешными повторениями из ${vm.reviews.size}");Text(vm.curriculum?.let{d->"${d.entries.count{inScope(it.band,vm.level,true)}} слов · ${d.hanzi.count{inScope(it.band,vm.level,true)}} иероглифов · ${d.grammar.count{inScope(it.band,vm.level,true)}} тем"}?: "Загрузка учебной базы…");Text("Список программы 2025; переводы и объяснения пока неполные.");Button({go("Учёба","Урок дня")},Modifier.fillMaxWidth()){Text("Продолжить практику")} }
    Panel("План на сегодня · ${vm.minutes} мин") { Text("Лексика ${vm.minutes*35/100} мин · грамматика ${vm.minutes*20/100} мин · речь ${vm.minutes*25/100} мин · чтение/тест ${vm.minutes-vm.minutes*35/100-vm.minutes*20/100-vm.minutes*25/100} мин");Text("Воскресенье: тренировочный тест (короткий, не пробник HSK).") }
    WideButton("Повторить · ${vm.reviews.values.count{!it.due.isAfter(LocalDate.now())}} к повторению"){go("Учёба","Повторение")};WideButton("Тренировка тонов"){go("Речь","")};WideButton("Грамматика"){go("Учёба","Грамматика")};WideButton("Открыть библиотеку"){go("Учёба","Библиотека")};WideButton("Тренировочный тест"){go("Экзамены","")}
    Text("Последний тест: ${vm.lastQuiz}",style=MaterialTheme.typography.bodyMedium)
}
@Composable private fun Learn(vm:HanziViewModel,sub:String,choose:(String)->Unit){
    if(sub=="Меню")Page("Учебные материалы"){
        Panel("Уровни") { for(group in (1..7).chunked(3)) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){group.forEach{n->FilterChip(selected=vm.level==n,onClick={vm.updateLevel(n)},label={Text(hskLabel(n))},modifier=Modifier.weight(1f))}};Text("HSK 7–9 используют общий список. Словарь, иероглифы и грамматика фильтруются по цели; можно включить предыдущие уровни.") }
        listOf("Урок дня","Жизненные ситуации","Словарь","Диалоги","Чтение","Грамматика","Иероглифы","Повторение","Библиотека").forEach{label->WideButton(label){choose(label)}}
    }else Column(Modifier.fillMaxSize()){TextButton({choose("Меню")}){Text("‹ Учёба")};Box(Modifier.weight(1f)){when(sub){"Урок дня"->DailyStudy(vm);"Жизненные ситуации"->LifeCourseScreen(vm);"Диалоги"->DialogueStudy(vm);"Словарь"->Dictionary(vm);"Чтение"->Reader(vm);"Грамматика"->GrammarScreen(vm);"Иероглифы"->HanziScreen(vm);"Повторение"->ReviewScreen(vm);"Библиотека"->Library(vm);else->Text("Раздел недоступен")}}}
}

@Composable private fun LifeCourseScreen(vm:HanziViewModel){
    var situationId by rememberSaveable{mutableStateOf("")}
    var textId by rememberSaveable{mutableStateOf("")}
    var practiceOrder by rememberSaveable{mutableStateOf<Int?>(null)}
    var showAll by rememberSaveable{mutableStateOf(true)}
    DisposableEffect(Unit){onDispose{vm.stopAudio()}}
    val situations=vm.lifeCourse
    val situation=situations.firstOrNull{it.id==situationId}

    if(situation==null){
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Жизненные ситуации",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("16 авторских сценариев: в каждом есть практический диалог и короткая история с иероглифами, pinyin и русским переводом.")
            if(situations.isEmpty()){
                if(vm.lifeCourseError.isBlank()){
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Загрузка курса…")
                }else{
                    Text(vm.lifeCourseError,color=MaterialTheme.colorScheme.error)
                    WideButton("Повторить загрузку"){vm.loadLifeCourse()}
                }
            }else{
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(selected=showAll,onClick={showAll=true},label={Text("Все ситуации")})
                    FilterChip(selected=!showAll,onClick={showAll=false},label={Text("До моего уровня")})
                }
                val shown=remember(situations,vm.level,showAll){
                    if(showAll)situations else situations.filter{it.level<=vm.level}
                }
                Text("Показано ${shown.size} из ${situations.size}",style=MaterialTheme.typography.labelMedium)
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    items(shown,key={it.id}){item->
                        Card(onClick={
                            vm.stopAudio();situationId=item.id
                            textId=item.texts.firstOrNull{it.kind==LifeTextKind.DIALOGUE}?.id.orEmpty()
                            practiceOrder=null
                        },modifier=Modifier.fillMaxWidth()){
                            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                    Text(item.titleRu,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                                    Text("Уровень ${item.level}",style=MaterialTheme.typography.labelMedium)
                                }
                                Text(item.categoryRu,style=MaterialTheme.typography.bodySmall)
                                Text("Диалог + история · ${item.texts.sumOf{it.lines.size}} строк",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if(shown.isEmpty())item{Text("Для выбранного уровня ситуаций пока нет.")}
                }
            }
        }
        return
    }

    val active=situation.texts.firstOrNull{it.id==textId}?:situation.texts.first()
    val practiceLine=active.lines.firstOrNull{it.order==practiceOrder}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        TextButton({vm.stopAudio();situationId="";textId="";practiceOrder=null}){Text("‹ Все ситуации")}
        Text(situation.titleRu,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("${situation.categoryRu} · уровень курса ${situation.level}",style=MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            situation.texts.sortedBy{it.kind}.forEach{lesson->
                FilterChip(
                    selected=lesson.id==active.id,
                    onClick={vm.stopAudio();textId=lesson.id;practiceOrder=null},
                    label={Text(if(lesson.kind==LifeTextKind.DIALOGUE)"Диалог" else "История")}
                )
            }
        }
        Text(active.titleRu,style=MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton({vm.speak(active.lines.joinToString(" "){it.hanzi})},enabled=!vm.recording&&!vm.playing&&!vm.speechCheck.busy,modifier=Modifier.weight(1f)){Text("Слушать всё")}
            OutlinedButton({vm.speak(active.lines.joinToString(" "){it.hanzi},.7f)},enabled=!vm.recording&&!vm.playing&&!vm.speechCheck.busy,modifier=Modifier.weight(1f)){Text("Медленно")}
        }
        active.lines.forEach{line->
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Text("${line.order}. ${line.speaker}",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold)
                    Text(line.hanzi,fontSize=24.sp,lineHeight=34.sp)
                    Text(line.pinyin,color=MaterialTheme.colorScheme.primary)
                    Text(line.russian)
                    Row{
                        TextButton({vm.speak(line.hanzi)},enabled=!vm.recording&&!vm.playing&&!vm.speechCheck.busy){Text("Слушать")}
                        TextButton({practiceOrder=line.order},enabled=!vm.recording&&!vm.playing&&!vm.speechCheck.busy){Text("Читать вслух")}
                    }
                }
            }
        }
        practiceLine?.let{line->LifeReadingPractice(vm,line){practiceOrder=null}}
        Text("Озвучивание выполняет системный Mandarin TTS. Все тексты курса оригинальные и хранятся внутри приложения.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun LifeReadingPractice(vm:HanziViewModel,line:LifeLine,onClose:()->Unit){
    val context=LocalContext.current
    val check=vm.speechCheck
    val recordSeconds=remember(line.hanzi){
        val characters=line.hanzi.count{it in '\u3400'..'\u9FFF'}
        ((characters+1)/2+2).coerceIn(5,15)
    }
    val speechPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted&&!vm.recording&&!vm.playing){vm.erase();check.start(line.hanzi)}
        else vm.message("Микрофон недоступен")
    }
    val tonePermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted&&!check.busy&&!vm.playing){check.cancel();vm.record(recordSeconds)}
        else vm.message("Микрофон недоступен")
    }
    val reference=remember(line.pinyin){phraseToneTemplate(line.pinyin)}
    val actual=remember(vm.pitch){vm.pitch?.let(Pitch::phraseContour).orEmpty()}
    LaunchedEffect(line.hanzi){vm.erase();check.reset()}
    DisposableEffect(Unit){onDispose{vm.stopAudio()}}
    Panel("Чтение вслух"){
        Text(line.hanzi,fontSize=27.sp,lineHeight=38.sp)
        Text(line.pinyin,color=MaterialTheme.colorScheme.primary)
        Text(line.russian)
        Row{
            TextButton({vm.speak(line.hanzi)},enabled=!vm.recording&&!vm.playing&&!check.busy){Text("Эталон TTS")}
            TextButton({vm.speak(line.hanzi,.7f)},enabled=!vm.recording&&!vm.playing&&!check.busy){Text("0.7×")}
            TextButton(onClose){Text("Закрыть")}
        }
        Button(onClick={
            if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){vm.erase();check.start(line.hanzi)}
            else speechPermission.launch(Manifest.permission.RECORD_AUDIO)
        },enabled=!check.busy&&!vm.recording&&!vm.playing,modifier=Modifier.fillMaxWidth()){
            Text(if(check.busy)"Слушаю текст…" else "Прочитать и проверить текст")
        }
        if(check.busy)WideButton("Отменить распознавание"){check.cancel()}
        if(check.heard.isNotBlank()){
            Text("Распознано:",style=MaterialTheme.typography.labelMedium)
            Text(check.heard,fontSize=22.sp)
        }
        if(check.feedback.isNotBlank())Text(check.feedback)
        Text("Совпадение проверяет только распознанные иероглифы. Оно не измеряет правильность отдельных звуков или тонов.",style=MaterialTheme.typography.bodySmall)

        HorizontalDivider()
        Text("Контур высоты голоса",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        Text("Прочитайте выбранную строку примерно за $recordSeconds секунд. Запись остаётся в памяти приложения и никуда не отправляется.")
        Button(onClick={
            if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){check.cancel();vm.record(recordSeconds)}
            else tonePermission.launch(Manifest.permission.RECORD_AUDIO)
        },enabled=!vm.recording&&!vm.playing&&!check.busy,modifier=Modifier.fillMaxWidth()){
            Text(if(vm.recording)"Записываю…" else "Записать контур до $recordSeconds секунд")
        }
        if(vm.recording){
            LinearProgressIndicator(Modifier.fillMaxWidth())
            WideButton("Отменить запись"){vm.stopAudio()}
        }
        if(vm.samples!=null&&!vm.recording){WideButton(if(vm.playing)"Воспроизведение…" else "Прослушать свою запись"){vm.play()}}
        ToneChart(reference,actual)
        Text("Янтарный — схематический рисунок написанных тонов; бирюзовый — измеренная F0. Слоги по времени не выравниваются, поэтому график не является оценкой произношения.",style=MaterialTheme.typography.bodySmall)
        vm.pitch?.let{result->
            Text(if(actual.isEmpty())"Недостаточно устойчивого голоса для контура. Повторите в тихом помещении." else "F0 показана по всей записи с видимыми паузами между озвученными участками.")
            Text("Озвученные кадры: ${(result.voicedFraction*100).toInt()}% (качество сигнала, не точность).",style=MaterialTheme.typography.bodySmall)
        }?:Text("После записи появится измеренный контур.")
    }
}

@Composable private fun WordDetails(vm:HanziViewModel,w:Word,onClose:()->Unit){
    val entry=vm.curriculum?.byId?.get(w.id)
    AlertDialog(onDismissRequest=onClose,title={Text(entry?.label?:w.simplified,fontSize=32.sp)},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(w.pinyin,style=MaterialTheme.typography.titleLarge)
            Text(w.russian.ifBlank{"Перевод на русский пока не добавлен."})
            val en=vm.curriculum?.englishById?.get(w.id)
            if(!en.isNullOrBlank()){Text("EN · CC-CEDICT",style=MaterialTheme.typography.labelMedium);Text(en);Text("Возможные значения данного чтения, не точное определение экзаменационного значения.",style=MaterialTheme.typography.labelSmall)}
            Text("Часть речи: "+w.pos.ifBlank{"не указана"})
            if(w.traditional.isNotBlank())Text("Традиционный: ${w.traditional}")
            Text(if(entry!=null)"${hskLabel(entry.band)} · № ${entry.serial} · ${entry.levelName} · программа 2025" else "Авторская практика · уровень не сверён",style=MaterialTheme.typography.labelMedium)
            if(entry!=null&&entry.label!=w.simplified)Text("Цифра в исходной записи различает словарные статьи, она не произносится.")
            if(w.russian.isNotBlank())Text("Русский перевод: авторская статья, редакторская проверка требуется.",style=MaterialTheme.typography.labelSmall)
            if(w.example.isNotBlank()){Text(w.example,fontSize=21.sp);Text(w.examplePinyin);Text(w.exampleRu)}
            Row{TextButton({vm.speak(w.simplified)}){Text("Озвучить")};TextButton({vm.speak(w.simplified,.7f)}){Text("0.7×")}}
            TextButton({onClose();vm.practice(w.simplified)}){Text("Тренировать произношение")}
            if(w.example.isNotBlank()){TextButton({vm.speak(w.example)}){Text("Пример TTS")};TextButton({onClose();vm.practice(w.example)}){Text("Произнести пример")}}
            Text("Произношение TTS синтетическое.",style=MaterialTheme.typography.labelSmall)
            val others=remember(w,vm.curriculum){vm.curriculum?.entries?.filter{it.word.simplified==w.simplified&&it.word.id!=w.id}.orEmpty()}
            if(others.isNotEmpty()){Text("Другие статьи этого написания:");others.forEach{Text("${it.label} · ${it.word.pinyin} · ${hskLabel(it.band)}")}}
        }
    },confirmButton={TextButton({vm.add(w);onClose()}){Text("В повторение")}},dismissButton={TextButton(onClose){Text("Закрыть")}})
}
@Composable private fun LevelScope(vm:HanziViewModel,cumulative:Boolean,onScope:(Boolean)->Unit){
    var chooseLevel by remember{mutableStateOf(false)}
    Box{TextButton({chooseLevel=true}){Text("Выбрать "+hskLabel(vm.level)+" ▾")};DropdownMenu(expanded=chooseLevel,onDismissRequest={chooseLevel=false}){(1..7).forEach{n->DropdownMenuItem(text={Text(hskLabel(n))},onClick={vm.updateLevel(n);chooseLevel=false})}}}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        TextButton({vm.updateLevel((hskBand(vm.level)-1).coerceAtLeast(1))},enabled=hskBand(vm.level)>1){Text("‹ Уровень")}
        Text(hskLabel(vm.level),modifier=Modifier.padding(top=12.dp),fontWeight=FontWeight.Bold)
        TextButton({vm.updateLevel((hskBand(vm.level)+1).coerceAtMost(7))},enabled=hskBand(vm.level)<7){Text("Уровень ›")}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        FilterChip(selected=!cumulative,onClick={onScope(false)},label={Text("Только новые")})
        FilterChip(selected=cumulative,onClick={onScope(true)},label={Text("Все до уровня")})
    }
}
@Composable private fun CurriculumLoading(vm:HanziViewModel){
    Column(Modifier.fillMaxWidth().padding(16.dp)){
        if(vm.curriculumError.isBlank()){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Загрузка HSK…")}
        else{Text(vm.curriculumError);WideButton("Повторить загрузку"){vm.loadCurriculum()}}
    }
}
@Composable private fun Dictionary(vm:HanziViewModel){
    var q by rememberSaveable{mutableStateOf("")};var cumulative by rememberSaveable{mutableStateOf(true)}
    var chosen by remember{mutableStateOf<Word?>(null)}
    val data=vm.curriculum
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("Словарь HSK 3.0",style=MaterialTheme.typography.headlineSmall)
        LevelScope(vm,cumulative){cumulative=it}
        if(data==null){CurriculumLoading(vm);return@Column}
        OutlinedTextField(q,{q=it},label={Text("汉字 / pinyin / русский")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val level=vm.level
        val scoped=remember(data,level,cumulative){data.entries.filter{inScope(it.band,level,cumulative)}}
        val query=q.trim()
        // Each query gets its own state. Changing query or level cancels the old search.
        var searchResult by remember(data,scoped,query){
            mutableStateOf<List<SyllabusWord>?>(if(query.isBlank())scoped else null)
        }
        LaunchedEffect(data,scoped,query){
            if(query.isNotBlank()){
                val result=withContext(Dispatchers.Default){
                    val key=searchKey(query)
                    scoped.filter{e->
                        ensureActive()
                        val w=e.word
                        e.label.contains(query,true)||w.simplified.contains(query,true)||w.traditional.contains(query,true)||w.russian.contains(query,true)||(data.englishById[w.id]?.contains(query,true)==true)||(key.isNotEmpty()&&searchKey(w.pinyin).contains(key))
                    }
                }
                searchResult=result
            }
        }
        val found=searchResult.orEmpty()
        Text("${scoped.size} слов · найдено ${found.size} · RU: ${scoped.count{it.word.russian.isNotBlank()}}",style=MaterialTheme.typography.labelMedium)
        Text("Русские переводы частичные · EN: ${scoped.count{data.englishById.containsKey(it.word.id)}}",style=MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
            items(found,key={it.serial}){e->Card(onClick={chosen=e.word},modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(12.dp)){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(e.label,fontSize=25.sp);Text(hskLabel(e.band),style=MaterialTheme.typography.labelMedium)}
                    Text(e.word.pinyin);Text(e.word.russian.ifBlank{data.englishById[e.word.id]?.let{"EN: "+it.substringBefore(" / ")}?:"Перевод не добавлен"},style=MaterialTheme.typography.bodySmall)
                }
            }}
            if(searchResult==null)item{LinearProgressIndicator(Modifier.fillMaxWidth());Text("Поиск…")}else if(found.isEmpty())item{Text("Нет результатов.")}
        }
    }
    chosen?.let{WordDetails(vm,it){chosen=null}}
}

@Composable private fun Interactive(vm:HanziViewModel,text:String){var chosen by remember{mutableStateOf<Word?>(null)};var unknown by remember{mutableStateOf<String?>(null)};val color=MaterialTheme.colorScheme.primary;val plain=MaterialTheme.colorScheme.onSurface;val tokens=remember(text,vm.segmenter){vm.segmenter.segment(text)};val annotated=remember(tokens,color,plain){buildAnnotatedString{tokens.forEachIndexed{i,t->pushStringAnnotation("token",i.toString());withStyle(SpanStyle(color=if(t.word!=null)color else plain)){append(t.text)};pop()}}};ClickableText(annotated,style=MaterialTheme.typography.bodyLarge.copy(fontSize=27.sp,lineHeight=42.sp),onClick={offset->annotated.getStringAnnotations("token",offset,offset).firstOrNull()?.item?.toIntOrNull()?.let{idx->tokens.getOrNull(idx)?.let{if(it.word!=null)chosen=it.word else unknown=it.text}}});chosen?.let{WordDetails(vm,it){chosen=null}};unknown?.let{s->AlertDialog(onDismissRequest={unknown=null},title={Text(s)},text={Text("Нет проверенной словарной статьи для этого фрагмента.")},confirmButton={TextButton({vm.speak(s)}){Text("Озвучить")}},dismissButton={TextButton({unknown=null}){Text("Закрыть")}})}}

@Composable private fun Reader(vm:HanziViewModel)=Page("Интерактивное чтение"){
    var index by rememberSaveable{mutableIntStateOf(Math.floorMod(LocalDate.now().toEpochDay(),readings.size.toLong()).toInt())}
    val lesson=readings[index.coerceIn(0,readings.lastIndex)]
    var pinyin by rememberSaveable{mutableStateOf(true)}
    var translation by rememberSaveable{mutableStateOf(false)}
    var custom by rememberSaveable{mutableStateOf(false)}
    var ownText by rememberSaveable{mutableStateOf("")}
    val text=if(custom)ownText else lesson.text
    Text("5 авторских текстов · ежедневно новый по кругу · HSK 3.0 ещё не сверено")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        OutlinedButton({index=(index+readings.size-1)%readings.size;custom=false}){Text("Назад")}
        Text("${index+1} / ${readings.size}",Modifier.padding(top=12.dp))
        OutlinedButton({index=(index+1)%readings.size;custom=false}){Text("Далее")}
    }
    readings.forEachIndexed{i,r->TextButton({index=i;custom=false}){Text(if(index==i&&!custom) "• "+r.title else r.title)}}
    Panel(if(custom)"Свой текст" else lesson.title){
        Interactive(vm,text)
        if(pinyin&&!custom)Text(lesson.pinyin)
        if(translation&&!custom)Text(lesson.ru)
        Row{TextButton({pinyin=!pinyin}){Text(if(pinyin)"Скрыть pinyin" else "Pinyin")};TextButton({translation=!translation}){Text(if(translation)"Скрыть перевод" else "Перевод")}}
        Row{TextButton({vm.speak(text)}){Text("Озвучить TTS")};TextButton({vm.speak(text,.7f)}){Text("0.7×")}}
    }
    WideButton("Тренировать произношение текста"){if(text.isNotBlank())vm.practice(text.take(500))}
    WideButton(if(custom)"Вернуться к уроку" else "Вставить собственный текст"){custom=!custom}
    if(custom){OutlinedTextField(ownText,{if(it.length<=4000)ownText=it},label={Text("Китайский текст · до 4000 символов")},modifier=Modifier.fillMaxWidth(),minLines=3);Text("Для своего текста доступны словарные статьи; полный pinyin и перевод не генерируются.")}
}
@Composable private fun GrammarPracticeScreen(vm:HanziViewModel)=Page("Авторская грамматическая практика"){
    var index by rememberSaveable{mutableIntStateOf(Math.floorMod(LocalDate.now().toEpochDay(),allGrammar.size.toLong()).toInt())}
    val g=allGrammar[index.coerceIn(0,allGrammar.lastIndex)]
    Text("${allGrammar.size} авторских конструкций · ежедневная смена по кругу · без подтверждённого уровня")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){OutlinedButton({index=(index+allGrammar.size-1)%allGrammar.size}){Text("Назад")};Text("${index+1} / ${allGrammar.size}",Modifier.padding(top=12.dp));OutlinedButton({index=(index+1)%allGrammar.size}){Text("Далее")}}
    Panel("${g.chinese} · ${g.name}"){Text(g.formula,fontWeight=FontWeight.Bold);Text(g.explanation);Interactive(vm,g.example);Text(g.pinyin);Text(g.ru);WideButton("Прослушать пример"){vm.speak(g.example)}}
    Panel("Все конструкции"){allGrammar.forEachIndexed{i,item->TextButton({index=i}){Text("${i+1}. ${item.chinese} · ${item.name}")}}}
}
@Composable private fun HanziPracticeScreen(vm:HanziViewModel)=Page("Авторские карточки иероглифов"){
    var index by rememberSaveable{mutableIntStateOf(Math.floorMod(LocalDate.now().toEpochDay(),characterPractice.size.toLong()).toInt())}
    val h=characterPractice[index.coerceIn(0,characterPractice.lastIndex)]
    Text("${characterPractice.size} авторских карточек · не полный иероглифический справочник")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){OutlinedButton({index=(index+characterPractice.size-1)%characterPractice.size}){Text("Назад")};Text("${index+1} / ${characterPractice.size}",Modifier.padding(top=12.dp));OutlinedButton({index=(index+1)%characterPractice.size}){Text("Далее")}}
    Panel("Карточка"){Text(h.han,fontSize=88.sp);Text(h.pinyin,fontSize=25.sp);Text(h.ru);Text("Пример слова:");Interactive(vm,h.example.substringBefore("（"));WideButton("Озвучить слово"){vm.speak(h.example.substringBefore("（"))}}
    Panel("Все карточки"){characterPractice.chunked(4).forEachIndexed{row,items->Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){items.forEachIndexed{col,item->FilterChip(selected=index==row*4+col,onClick={index=row*4+col},label={Text(item.han,fontSize=20.sp)},modifier=Modifier.weight(1f))}}}}
    Text("Порядок черт и проверка рукописного ввода пока не реализованы.")
}

@Composable private fun GrammarScreen(vm:HanziViewModel){
    var practice by rememberSaveable{mutableStateOf(false)}
    var cumulative by rememberSaveable{mutableStateOf(true)}
    var query by rememberSaveable{mutableStateOf("")}
    var selected by remember{mutableStateOf<SyllabusGrammar?>(null)}
    val data=vm.curriculum
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
        TextButton({practice=!practice}){Text(if(practice)"‹ Список программы HSK" else "10 авторских объяснений ›")}
        if(practice){Box(Modifier.weight(1f)){GrammarPracticeScreen(vm)};return@Column}
        Text("Грамматика HSK 3.0",style=MaterialTheme.typography.headlineSmall)
        LevelScope(vm,cumulative){cumulative=it}
        if(data==null){CurriculumLoading(vm);return@Column}
        OutlinedTextField(query,{query=it},label={Text("Поиск конструкции или категории")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val level=vm.level
        val scoped=remember(data,level,cumulative){data.grammar.filter{inScope(it.band,level,cumulative)}}
        val shown=remember(scoped,query){scoped.filter{g->listOf(g.content,g.type,g.category,g.detail,grammarCategoryRu(g.type),grammarCategoryRu(g.category)).any{it.contains(query.trim(),true)}}}
        Text("${scoped.size} тем · найдено ${shown.size}",style=MaterialTheme.typography.labelMedium)
        Text("Указатель: ${data.grammar.count{it.band<=level}} накопительно; ${data.grammar.count{it.band==level}} новых. Объяснения ещё неполные.",style=MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
            items(shown,key={it.serial}){g->Card(onClick={selected=g},modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(g.title,fontSize=19.sp);Text(listOf(hskLabel(g.band),grammarCategoryLabel(g.type),grammarCategoryLabel(g.category),g.detail).filter{it.isNotBlank()}.joinToString(" · "),style=MaterialTheme.typography.bodySmall)}}}
            if(shown.isEmpty())item{Text("Нет результатов.")}
        }
    }
    selected?.let{g->AlertDialog(onDismissRequest={selected=null},title={Text(g.title)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(hskLabel(g.band));Text(listOf(grammarCategoryLabel(g.type),grammarCategoryLabel(g.category),g.detail).filter{it.isNotBlank()}.joinToString(" / "))
        Text("Тема № ${g.serial} · HSK 考试大纲, 2025.")
        Text("Развёрнутое объяснение на русском и упражнения для этого пункта ещё не добавлены. Авторские объяснения доступны отдельно.")
    }},confirmButton={TextButton({selected=null}){Text("Закрыть")}})}
}
@Composable private fun HanziScreen(vm:HanziViewModel){
    var practice by rememberSaveable{mutableStateOf(false)}
    var cumulative by rememberSaveable{mutableStateOf(true)}
    var writing by rememberSaveable{mutableStateOf(false)}
    var query by rememberSaveable{mutableStateOf("")}
    var selected by remember{mutableStateOf<SyllabusHanzi?>(null)}
    var chosenWord by remember{mutableStateOf<Word?>(null)}
    val data=vm.curriculum
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
        TextButton({practice=!practice}){Text(if(practice)"‹ Список программы HSK" else "16 авторских карточек ›")}
        if(practice){Box(Modifier.weight(1f)){HanziPracticeScreen(vm)};return@Column}
        Text("Иероглифы HSK 3.0",style=MaterialTheme.typography.headlineSmall)
        LevelScope(vm,cumulative){cumulative=it}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=!writing,onClick={writing=false},label={Text("Чтение")})
            FilterChip(selected=writing,onClick={writing=true},label={Text("Письмо")})
        }
        if(data==null){CurriculumLoading(vm);return@Column}
        OutlinedTextField(query,{query=it},label={Text("Иероглиф / pinyin")},singleLine=true,modifier=Modifier.fillMaxWidth())
        val level=vm.level
        val scoped=remember(data,level,cumulative,writing){data.hanzi.filter{h->if(writing)h.writingBand?.let{inScope(it,level,cumulative)}==true else inScope(h.band,level,cumulative)}}
        val shown=remember(scoped,query){val key=searchKey(query);scoped.filter{h->query.isBlank()||h.hanzi.contains(query.trim())||(key.isNotBlank()&&searchKey(h.readings).contains(key))}}
        Text("${scoped.size} иероглифов · найдено ${shown.size}",style=MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
            items(shown.chunked(4),key={it.first().hanzi}){row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                row.forEach{h->Card(onClick={selected=h},modifier=Modifier.weight(1f)){Column(Modifier.padding(7.dp)){Text(h.hanzi,fontSize=29.sp);Text(h.readings.substringBefore(','),fontSize=11.sp)}}}
                repeat(4-row.size){Spacer(Modifier.weight(1f))}
            }}
            if(shown.isEmpty())item{Text(if(writing&&vm.level==1)"Для HSK 1 список обязательного письма пуст." else "Нет результатов.")}
        }
    }
    selected?.let{h->
        val related=remember(h,data){data?.entries?.asSequence()?.filter{it.word.simplified.contains(h.hanzi)}?.take(12)?.toList().orEmpty()}
        val authored=remember(h){characterPractice.firstOrNull{it.han==h.hanzi}}
        AlertDialog(onDismissRequest={selected=null},title={Text(h.hanzi,fontSize=64.sp)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(h.readings,fontSize=23.sp);Text("Чтение: "+hskLabel(h.band));Text("Письмо: "+(h.writingBand?.let(::hskLabel)?:"не входит в обязательный список письма"))
            Text(authored?.ru?:"Значение на русском пока не добавлено.")
            Text("Чтения из pinyin-data. Правильный вариант зависит от слова.")
            TextButton({vm.speak(h.hanzi)}){Text("Озвучить TTS")};TextButton({selected=null;vm.practice(h.hanzi)}){Text("Произнести иероглиф")}
            Text("Слова из программы:")
            related.forEach{e->TextButton({selected=null;chosenWord=e.word}){Text("${e.label} · ${e.word.pinyin} · ${hskLabel(e.band)}")}}
            Text("Анимация и проверка черт пока не реализованы.",style=MaterialTheme.typography.bodySmall)
        }},confirmButton={TextButton({selected=null}){Text("Закрыть")}})
    }
    chosenWord?.let{WordDetails(vm,it){chosenWord=null}}
}

@Composable private fun ReviewScreen(vm:HanziViewModel)=Page("Повторение"){
    var mode by rememberSaveable{mutableStateOf("Чтение")}
    var all by rememberSaveable{mutableStateOf(false)}
    val due=vm.words.filter{w->vm.reviews[w.id]?.due?.let{!it.isAfter(LocalDate.now())}==true}
    val pool=if(all)vm.words.filter{vm.reviews.containsKey(it.id)} else due
    var cardIndex by rememberSaveable(all){mutableIntStateOf(0)}
    val w=if(all)pool.getOrNull(cardIndex.coerceAtMost((pool.size-1).coerceAtLeast(0))) else pool.firstOrNull()
    Text("К повторению: ${due.size} · сохранено: ${vm.reviews.size}")
    Row{FilterChip(selected=!all,onClick={all=false},label={Text("На сегодня")});Spacer(Modifier.width(8.dp));FilterChip(selected=all,onClick={all=true},label={Text("Все сохранённые")})}
    WideButton("Добавить 10 новых слов уровня"){vm.addDailyWords()}
    listOf(listOf("Чтение","На слух"),listOf("Перевод","Диктант")).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{value->FilterChip(selected=mode==value,onClick={mode=value},label={Text(value)},modifier=Modifier.weight(1f))}}}
    if(w==null){Text("Нет карточек в этом разделе. Добавьте слова из словаря или кнопкой выше. Оценённые карточки появятся снова в назначенный день.")}
    else key(w.id,mode){
        if(all){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton({cardIndex=(cardIndex-1).coerceAtLeast(0)},enabled=cardIndex>0&&!vm.reviewBusy){Text("Назад")};Text("${cardIndex.coerceAtMost(pool.lastIndex)+1}/${pool.size}",Modifier.padding(top=12.dp));TextButton({cardIndex=(cardIndex+1).coerceAtMost(pool.lastIndex)},enabled=cardIndex<pool.lastIndex&&!vm.reviewBusy){Text("Далее")}}}
        var reveal by rememberSaveable{mutableStateOf(false)}
        var input by rememberSaveable{mutableStateOf("")}
        val meaning=w.russian.ifBlank{vm.curriculum?.englishById?.get(w.id)?.let{"EN: $it"}?:"Значение пока не добавлено"}
        Panel("Карточка · "+mode){
            if(mode=="Чтение")Text(w.simplified,fontSize=44.sp)
            if(mode=="Перевод")Text(meaning)
            if(mode=="На слух"||mode=="Диктант")WideButton("Прослушать"){vm.speak(w.simplified)}
            if(mode=="Диктант"||mode=="Перевод")OutlinedTextField(input,{input=it},label={Text("Введите китайское слово")},singleLine=true,modifier=Modifier.fillMaxWidth())
            if(!reveal)WideButton("Показать ответ"){reveal=true}
            else{Text(w.simplified,fontSize=40.sp);Text(w.pinyin);Text(meaning);if(input.isNotBlank())Text(if(input.trim()==w.simplified)"Написание совпало" else "Сравните написание с ответом")
                Text("Выберите сложность. Следующая дата сохраняется автоматически.")
                for(pair in listOf(listOf(Grade.AGAIN to "Не знаю",Grade.HARD to "Трудно"),listOf(Grade.GOOD to "Хорошо",Grade.EASY to "Легко")))Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){pair.forEach{(g,label)->OutlinedButton(onClick={vm.rate(w,g)},enabled=!vm.reviewBusy,modifier=Modifier.weight(1f)){Text(label)}}}
                if(vm.reviewBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
                vm.reviews[w.id]?.let{Text("Следующее повторение: ${it.due} · ошибок: ${it.lapses}")}
            }
        }
    }
}
@Composable private fun Library(vm:HanziViewModel){
    var current by rememberSaveable{mutableStateOf<String?>(null)};val book=vm.books.firstOrNull{it.id==current};val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::importBook)}
    if(book!=null){Column{TextButton({current=null}){Text("‹ Библиотека")};Box(Modifier.weight(1f)){BookReader(vm,book)}};return}
    Page("Личная библиотека") {Text("Книги хранятся только на устройстве. PDF до 150 МБ, TXT UTF-8 до 2 МБ.");Button({picker.launch(arrayOf("application/pdf","text/plain"))},enabled=!vm.loading,modifier=Modifier.fillMaxWidth()){Text(if(vm.loading)"Импорт…" else "Импортировать книгу")};if(vm.loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(vm.books.isEmpty())Text("Нет импортированных книг");vm.books.forEach{b->Panel(b.name){Text("${b.kind.uppercase()} · ${b.version}");Row{TextButton({vm.tag(b,"HSK 2.0")}){Text("2.0")};TextButton({vm.tag(b,"HSK 3.0")}){Text("3.0")};TextButton({vm.tag(b,"unknown")}){Text("Не знаю")}};WideButton("Открыть"){current=b.id};var confirm by remember{mutableStateOf(false)};TextButton({confirm=true}){Text("Удалить локальную копию")};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Удалить книгу?")},text={Text("Оригинал в файловом менеджере не изменится.")},confirmButton={TextButton({vm.delete(b);confirm=false}){Text("Удалить")}},dismissButton={TextButton({confirm=false}){Text("Отмена")}})}}}
}
@Composable private fun BookReader(vm:HanziViewModel,b:Book){var page by rememberSaveable(b.id){mutableIntStateOf(b.page)};var bitmap by remember{mutableStateOf<Bitmap?>(null)};var total by remember{mutableIntStateOf(0)};var content by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(true)}
    LaunchedEffect(b.id,page){loading=true;error="";try{if(b.kind=="pdf"){val result=vm.files.pdf(b,page);bitmap=result.first;total=result.second}else{if(content.isEmpty())content=vm.files.text(b);total=((content.length+1999)/2000).coerceAtLeast(1)};if(page>=total)page=total-1;vm.savePage(b,page)}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"Не удалось открыть файл"}finally{loading=false}}
    Page(b.name){if(error.isNotEmpty())Text(error);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({page--},enabled=page>0&&!loading){Text("Назад")};Text("${page+1} / $total",modifier=Modifier.padding(top=12.dp));OutlinedButton({page++},enabled=page+1<total&&!loading){Text("Вперёд")}};if(!loading&&error.isEmpty()){if(b.kind=="pdf"){bitmap?.let{Image(it.asImageBitmap(),contentDescription="Страница ${page+1}",modifier=Modifier.fillMaxWidth())};Text("PDF отображается как оригинальная страница. OCR и нажатие на слова не реализованы.")}else{val start=(page*2000).coerceAtMost(content.length);Interactive(vm,content.substring(start,(start+2000).coerceAtMost(content.length)))}}}
}

@Composable private fun ToneScreen(vm:HanziViewModel)=Page("Произношение и тоны"){
    val context=LocalContext.current
    var tone by rememberSaveable{mutableIntStateOf(1)}
    var exampleIndex by rememberSaveable{mutableIntStateOf(0)}
    val examples=toneExamples[tone-1]
    val example=examples[exampleIndex.coerceIn(0,examples.lastIndex)]
    val mic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)vm.record()else vm.message("Для записи нужно разрешение на микрофон")}
    Panel("Выбери тон"){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            (1..5).forEach{n->FilterChip(selected=tone==n,onClick={tone=n;exampleIndex=0;vm.erase()},enabled=!vm.recording&&!vm.playing,label={Text(if(n==5)"0" else "$n")},modifier=Modifier.weight(1f))}
        }
        Text(listOf("1 · 阴平 · 55","2 · 阳平 · 35","3 · 上声 · 214","4 · 去声 · 51","0 · 轻声 · нейтральный")[tone-1],fontWeight=FontWeight.Bold)
        Text(listOf("Высокий и ровный.","От средней высоты поднимай голос.","Низкий тон; в изоляции часто с подъёмом в конце.","Начни высоко и быстро опусти голос.","Второй слог произноси кратко и легко; высота зависит от первого.")[tone-1])
        ToneGuide(tone,exampleIndex)
        Text(if(tone==5)"Схема: полный тон + краткий нейтральный слог. Высота условная." else "Схема высоты 1–5: от низкой к высокой. Не измерение F0.")
        Text(example.text,fontSize=50.sp);Text(example.pinyin,fontSize=25.sp);Text(example.ru)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){OutlinedButton({exampleIndex=(exampleIndex+examples.size-1)%examples.size;vm.erase()},enabled=!vm.recording&&!vm.playing){Text("Назад")};Text("${exampleIndex+1}/${examples.size}",Modifier.padding(top=12.dp));OutlinedButton({exampleIndex=(exampleIndex+1)%examples.size;vm.erase()},enabled=!vm.recording&&!vm.playing){Text("Далее")}}
        Row{TextButton({vm.speak(example.text)},enabled=!vm.recording&&!vm.playing){Text("Прослушать TTS")};TextButton({vm.speak(example.text,.7f)},enabled=!vm.recording&&!vm.playing){Text("0.7×")}}
        Text("Синтетический голос Android, не запись носителя.",style=MaterialTheme.typography.labelSmall)
    }
    Panel("Моя запись"){
        Button({if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.record()else mic.launch(Manifest.permission.RECORD_AUDIO)},enabled=!vm.recording&&!vm.playing,modifier=Modifier.fillMaxWidth()){Text(if(vm.recording)"Запись / анализ…" else "Записать 3 секунды")}
        Text("Произнеси пример один раз. Не растягивай четвёртый и нейтральный тоны.")
        Row{TextButton(vm::play,enabled=vm.samples!=null&&!vm.recording&&!vm.playing){Text("Моя запись")};TextButton(vm::erase,enabled=vm.samples!=null&&!vm.recording&&!vm.playing){Text("Удалить")}}
        ToneChart(Pitch.template(tone),vm.pitch?.contour.orEmpty())
        Text(if(tone==5)"Бирюзовый — измеренная F0; для нейтрального тона нет универсального эталона F0." else "Янтарный — схематический ориентир; бирюзовый — измеренная F0.",style=MaterialTheme.typography.bodySmall)
        Text("Высота относительно медианы записи, в полутонах. Время нормализовано.",style=MaterialTheme.typography.bodySmall)
        vm.pitch?.let{r->Text(r.message);Text("Озвученные кадры: ${(r.voicedFraction*100).toInt()}% (не точность)");if(tone!=5)Text(Pitch.feedback(r,tone)) else Text("Нейтральный тон пока не оценивается автоматически.")}?:Text("Запиши голос, чтобы получить измерение.")
        Text("Фонемы, артикуляция и точность классификации пока не оцениваются.")
    }
    Panel("Tone sandhi") {Text("你好 nǐ hǎo → ní hǎo (3 + 3 → 2 + 3)");Text("一次 yī cì → yí cì");Text("不是 bù shì → bú shì");Text("Третий тон перед другими тонами часто остаётся низким без полного подъёма. Изолированная тренировка не оценивает связную речь.")}
    Panel("声母 · артикуляция") {Text("j / q / x — передняя часть языка у твёрдого нёба; q с придыханием, j без него; x — щелевой звук.");Text("zh / ch / sh — кончик языка направлен к заальвеолярной области; ch с придыханием.");Text("ü — положение языка как для i, губы округлены.");Row{TextButton({vm.speak("鸡 七")}){Text("jī / qī")};TextButton({vm.speak("路 绿")}){Text("lù / lǜ")}}}
}

@Composable private fun ToneGuide(tone:Int,example:Int){
    val grid=MaterialTheme.colorScheme.outlineVariant;val ink=MaterialTheme.colorScheme.primary;val neutral=MaterialTheme.colorScheme.tertiary
    val patterns=listOf(listOf(5f,5f),listOf(3f,5f),listOf(2f,1f,4f),listOf(5f,1f))
    val prior=example.coerceIn(0,3)
    val first=if(tone==5 && prior==2)listOf(2f,1f) else patterns[if(tone==5)prior else (tone-1).coerceIn(0,3)]
    val target=listOf(2f,3f,4f,1f)[prior]
    Row(Modifier.fillMaxWidth()){
        Column(Modifier.height(145.dp).padding(end=6.dp),verticalArrangement=Arrangement.SpaceBetween){Text("5");Text("4");Text("3");Text("2");Text("1")}
        Canvas(Modifier.weight(1f).height(145.dp)){
            val px=12.dp.toPx();val left=px;val right=size.width-px;val top=px;val bottom=size.height-px
            fun x(t:Float)=left+(right-left)*t
            fun y(h:Float)=bottom-(bottom-top)*(h-1f)/4f
            for(i in 1..5)drawLine(grid,Offset(left,y(i.toFloat())),Offset(right,y(i.toFloat())),1.dp.toPx())
            val span=if(tone==5).66f else 1f
            val points=first.mapIndexed{i,v->Offset(x(i.toFloat()/first.lastIndex*span),y(v))}
            points.zipWithNext().forEach{(u,v)->drawLine(ink,u,v,4.dp.toPx())}
            points.forEach{drawCircle(ink,4.dp.toPx(),it)}
            if(tone==5){drawLine(grid,Offset(x(.72f),top),Offset(x(.72f),bottom),1.dp.toPx());drawLine(neutral,Offset(x(.77f),y(target)),Offset(x(1f),y(target)),4.dp.toPx());drawCircle(neutral,4.dp.toPx(),Offset(x(.77f),y(target)))}
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(tone==5)"Полный тон" else "Начало",style=MaterialTheme.typography.labelMedium);Text(if(tone==5)"Лёгкий слог" else "Конец",style=MaterialTheme.typography.labelMedium)}
}
@Composable private fun ToneChart(reference:List<ContourPoint>,actual:List<ContourPoint>){
    val grid=MaterialTheme.colorScheme.outlineVariant;val primary=MaterialTheme.colorScheme.primary;val ref=MaterialTheme.colorScheme.tertiary
    val actualStep=remember(actual){actual.zipWithNext().map{(a,b)->b.time-a.time}.filter{it>1e-6}.sorted().let{it.getOrNull(it.size/2)}}
    Canvas(Modifier.fillMaxWidth().height(170.dp)){
        val range=maxOf(6.0,(actual+reference).maxOfOrNull{abs(it.relativeSemitones)}?:6.0)
        val pad=10.dp.toPx()
        fun point(p:ContourPoint)=Offset(pad+(p.time*(size.width-2*pad)).toFloat(),pad+((size.height-2*pad)*(.5-p.relativeSemitones/(2*range))).toFloat())
        for(i in 0..4){val y=pad+i*(size.height-2*pad)/4;drawLine(grid,Offset(pad,y),Offset(size.width-pad,y),1.dp.toPx())}
        for(i in 0..4){val x=pad+i*(size.width-2*pad)/4;drawLine(grid,Offset(x,pad),Offset(x,size.height-pad),1.dp.toPx())}
        reference.zipWithNext().forEach{(u,v)->if(v.time-u.time>1e-9)drawLine(ref,point(u),point(v),4.dp.toPx())}
        reference.forEach{drawCircle(ref,3.dp.toPx(),point(it))}
        actual.zipWithNext().forEach{(u,v)->if(actualStep!=null&&v.time-u.time<=actualStep*3.0)drawLine(primary,point(u),point(v),2.dp.toPx())}
        actual.forEach{drawCircle(primary,1.5.dp.toPx(),point(it))}
    }
}
@Composable private fun ExamScreen(vm:HanziViewModel)=Page("Экзаменационная практика"){
    var i by rememberSaveable{mutableIntStateOf(0)};var score by rememberSaveable{mutableIntStateOf(0)};var selected by rememberSaveable{mutableIntStateOf(-1)};var finished by rememberSaveable{mutableStateOf(false)}
    Text("Original Practice · 5 заданий · не официальный HSK и не полный пробник.");if(finished){Panel("Результат") {Text("$score / ${questions.size}",fontSize=35.sp);Text("Результат передан на локальное сохранение.");WideButton("Начать заново"){i=0;score=0;selected=-1;finished=false}}}else{val q=questions[i];Panel("Задание ${i+1} / ${questions.size}"){Text(q.prompt,fontSize=24.sp);q.options.forEachIndexed{idx,option->OutlinedButton({selected=idx;if(idx==q.answer)score++},enabled=selected==-1,modifier=Modifier.fillMaxWidth()){Text(option)}};if(selected!=-1){Text(if(selected==q.answer)"Верно" else "Правильный ответ: ${q.options[q.answer]}",color=if(selected==q.answer)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error);Text(q.explanation);Button({if(i+1==questions.size){finished=true;vm.saveQuiz(score,questions.size)}else{i++;selected=-1}}){Text(if(i+1==questions.size)"Завершить" else "Далее")}}}}
    Panel("HSKK · самостоятельная практика") {Text("Расскажите за 1 минуту, зачем вы изучаете китайский: 你为什么学习汉语？");TextButton({vm.speak("你为什么学习汉语？")}){Text("Прослушать вопрос")};Text("Автоматическая проверка монолога и официальные таймеры пока не реализованы.")}
    Text("Официальные форматы: chinesetest.cn · HSK 3.0, запуск объявлен на 13.12.2026. Локальный центр необходимо уточнить.")
}
@Composable private fun Profile(vm:HanziViewModel)=Page("Профиль и настройки"){
    var date by rememberSaveable{mutableStateOf(vm.date)}
    Panel("Цель") {
        OutlinedTextField(date,{date=it},label={Text("Дата ГГГГ-ММ-ДД")},singleLine=true,modifier=Modifier.fillMaxWidth())
        WideButton("Сохранить дату"){vm.saveDate(date)}
        Text("Время в день: ${vm.minutes} мин")
        Slider(value=vm.minutes.toFloat(),onValueChange={vm.updateMinutes(it.toInt())},valueRange=15f..180f,steps=10)
        Text("30-дневная оценка нагрузки: ${sprint(vm.curriculum?.entries?.count{inScope(it.band,vm.level,false)}?:0,30).first()} новых слов в день. Не адаптивный план.")
    }
    Panel("Внешний вид"){
        listOf("system" to "Системная","light" to "Светлая","dark" to "Тёмная").forEach{(id,label)->Row{RadioButton(vm.theme==id,{vm.updateTheme(id)});Text(label,Modifier.padding(top=12.dp))}}
        Text("Интерфейс: русский. Английская и китайская локализации запланированы.")
    }
    Panel("Статистика") {
        Text("Карточек: ${vm.reviews.size}")
        Text("Текущие успешные серии (сумма): ${vm.reviews.values.sumOf{it.repetitions}}")
        Text("Ошибок повторения: ${vm.reviews.values.sumOf{it.lapses}}")
        Text("Последний тест: ${vm.lastQuiz}")
        Text("Импортированных файлов: ${vm.books.size}")
    }
    Panel("Конфиденциальность") {
        Text("Нет сетевого разрешения. Голос хранится в памяти до удаления или закрытия приложения. Книги и личные диалоги сохраняются в приватной директории Android.")
        WideButton("Удалить голосовую запись"){vm.erase()}
        Text("HanziFlow 0.4.0-alpha · учебные режимы.")
        Text("Источники: ChineseTest / profesorm/hsk30 (CC BY 4.0); mozillazg/pinyin-data (MIT). Английские значения: CC-CEDICT (CC BY-SA 4.0), через chinese-typing-practice.com/data. Полные атрибуции: hsk2025/NOTICE.txt, PINYIN_LICENSE.txt и CEDICT_NOTICE.txt.")
    }
}
