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
    private val prefs=app.getSharedPreferences("settings",0)
    val words=readWords(app); val segmenter=Segmenter(words)
    var level by mutableIntStateOf(prefs.getInt("level",4).coerceIn(1,9)); private set
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
    init { refresh() }
    fun updateLevel(n:Int){level=n; prefs.edit().putInt("level",n).apply()}
    fun saveDate(value:String){ if(value.isNotBlank() && runCatching{LocalDate.parse(value)}.isFailure){notice="Введите дату в формате ГГГГ-ММ-ДД";return}; date=value;prefs.edit().putString("date",value).apply();notice="Дата сохранена" }
    fun updateMinutes(n:Int){minutes=n; prefs.edit().putInt("minutes",n).apply()}
    fun updateTheme(value:String){theme=value;prefs.edit().putString("theme",value).apply()}
    fun message(text:String){notice=text}
    private fun refresh()=viewModelScope.launch { try { val data=withContext(Dispatchers.IO){Triple(db.reviews(),db.books(),db.lastQuiz())}; reviews=data.first;books=data.second;lastQuiz=data.third } catch(e:Exception){notice="Ошибка базы данных"} }
    private fun mutate(block:()->Unit)=viewModelScope.launch { try { withContext(Dispatchers.IO){block()};refresh() } catch(e:Exception){notice="Не удалось сохранить данные"} }
    fun add(w:Word){mutate{db.add(w.id)};notice="Слово добавляется в повторение"}
    fun rate(w:Word,g:Grade){mutate{db.grade(w.id,g)}}
    fun saveQuiz(n:Int,total:Int){mutate{db.quiz(n,total)}}
    fun savePage(b:Book,p:Int){mutate{db.page(b.id,p)}}
    fun tag(b:Book,v:String){mutate{db.version(b.id,v)}}
    fun delete(b:Book){mutate{if(files.delete(b)) db.deleteBook(b.id) else error("File deletion failed")}}
    fun importBook(uri:Uri){if(loading)return; viewModelScope.launch { loading=true;try{val book=files.import(uri);try{withContext(Dispatchers.IO){db.addBook(book)}}catch(e:Exception){files.delete(book);throw e}; refresh();notice="Файл импортирован локально"}catch(e:CancellationException){throw e}catch(e:Exception){notice=e.message?:"Ошибка импорта"}finally{loading=false} }}
    fun speak(text:String,speed:Float=1f){if(!speech.say(text,speed))notice="Установите офлайн-голос Mandarin Chinese в настройках Android."}
    fun record(){if(recording||playing)return; speech.stop(); samples=null;pitch=null;recording=true;audioJob=viewModelScope.launch{try{val pcm=VoiceRecorder.record();samples=pcm;pitch=withContext(Dispatchers.Default){Pitch.analyze(pcm)}}catch(e:CancellationException){throw e}catch(e:Exception){notice=e.message?:"Ошибка микрофона"}finally{recording=false}}}
    fun play(){val pcm=samples?:return;if(recording||playing)return;playing=true;audioJob=viewModelScope.launch{try{VoiceRecorder.play(pcm)}catch(e:CancellationException){throw e}catch(e:Exception){notice="Ошибка воспроизведения"}finally{playing=false}}}
    fun stopAudio(){audioJob?.cancel();speech.stop()}
    fun erase(){stopAudio();samples=null;pitch=null}
    override fun onCleared(){stopAudio();speech.close();db.close();super.onCleared()}
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
        fun navigate(t:String,s:String="Меню"){if(tab=="Речь" && t!="Речь")vm.stopAudio();tab=t;sub=s}
        Scaffold(modifier=Modifier.safeDrawingPadding(),topBar={TopAppBar(title={Column{Text("HanziFlow",fontWeight=FontWeight.Bold);Text("HSK ${vm.level} · 3.0",style=MaterialTheme.typography.labelMedium)}})}, snackbarHost={SnackbarHost(snackbar)},bottomBar={NavigationBar{tabs.forEach{(name,han)-> NavigationBarItem(selected=tab==name,onClick={navigate(name)},icon={Text(han,fontSize=22.sp)},label={Text(name,fontSize=10.sp)})}}}){padding->
            Box(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)) {
                when(tab){"Главная"->Home(vm){t,s->navigate(t,s)};"Учёба"->Learn(vm,sub){sub=it};"Речь"->ToneScreen(vm);"Экзамены"->ExamScreen(vm);else->Profile(vm)}
            }
        }
    }
}
@Composable private fun Page(title:String,body:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);body();Spacer(Modifier.height(16.dp))}}
@Composable private fun Panel(title:String,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);content()}}}
@Composable private fun WideButton(text:String,onClick:()->Unit){OutlinedButton(onClick,Modifier.fillMaxWidth()){Text(text)}}
@Composable private fun Home(vm:HanziViewModel,go:(String,String)->Unit)=Page("Сегодня"){
    Panel("HSK ${vm.level} · личная цель") { Text(if(vm.date.isBlank()) "Дата экзамена не установлена" else "${vm.date} · ${ChronoUnit.DAYS.between(LocalDate.now(),LocalDate.parse(vm.date)).coerceAtLeast(0)} дн. до экзамена");Text("${vm.reviews.values.count{it.repetitions>=3}} карточек с 3+ успешными повторениями из ${vm.reviews.size}");Text("Покрытие официальной программы пока не рассчитано.");Button({go("Учёба","Чтение")},Modifier.fillMaxWidth()){Text("Продолжить практику")} }
    Panel("План на сегодня · ${vm.minutes} мин") { Text("Лексика ${vm.minutes*35/100} мин · грамматика ${vm.minutes*20/100} мин · речь ${vm.minutes*25/100} мин · чтение/тест ${vm.minutes-vm.minutes*35/100-vm.minutes*20/100-vm.minutes*25/100} мин");Text("Воскресенье: тренировочный тест (короткий, не пробник HSK).") }
    WideButton("Повторить · ${vm.reviews.values.count{!it.due.isAfter(LocalDate.now())}} к повторению"){go("Учёба","Повторение")};WideButton("Тренировка тонов"){go("Речь","")};WideButton("Грамматика"){go("Учёба","Грамматика")};WideButton("Открыть библиотеку"){go("Учёба","Библиотека")};WideButton("Тренировочный тест"){go("Экзамены","")}
    Text("Последний тест: ${vm.lastQuiz}",style=MaterialTheme.typography.bodyMedium)
}
@Composable private fun Learn(vm:HanziViewModel,sub:String,choose:(String)->Unit){
    if(sub=="Меню")Page("Учебные материалы"){
        Panel("Уровни") { for(group in (1..9).chunked(3)) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){group.forEach{n->FilterChip(selected=vm.level==n,onClick={vm.updateLevel(n)},label={Text("HSK $n")},modifier=Modifier.weight(1f))}};Text("HSK 7–9: объединённый формат экзамена. Уровни доступны как личные цели, учебная база ещё не распределена.") }
        listOf("Словарь","Чтение","Грамматика","Повторение","Библиотека").forEach{label->WideButton(label){choose(label)}}
    }else Column(Modifier.fillMaxSize()){TextButton({choose("Меню")}){Text("‹ Учёба")};Box(Modifier.weight(1f)){when(sub){"Словарь"->Dictionary(vm);"Чтение"->Reader(vm);"Грамматика"->GrammarScreen(vm);"Повторение"->ReviewScreen(vm);"Библиотека"->Library(vm);else->Text("Раздел недоступен")}}}
}
@Composable private fun WordDetails(vm:HanziViewModel,w:Word,onClose:()->Unit){AlertDialog(onDismissRequest=onClose,title={Text(w.simplified,fontSize=32.sp)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(w.pinyin,style=MaterialTheme.typography.titleLarge);Text(w.russian);Text(w.pos);Text("Традиционный: ${w.traditional}");Text("HSK: не сверено · авторская статья, редакторская проверка требуется",style=MaterialTheme.typography.labelSmall);Text(w.example,fontSize=21.sp);Text(w.examplePinyin);Text(w.exampleRu);Row{TextButton({vm.speak(w.simplified)}){Text("Озвучить")};TextButton({vm.speak(w.example,.7f)}){Text("Пример 0.7×")}}}},confirmButton={TextButton({vm.add(w);onClose()}){Text("В повторение")}},dismissButton={TextButton(onClose){Text("Закрыть")}})}
@Composable private fun Dictionary(vm:HanziViewModel){var q by rememberSaveable{mutableStateOf("")};var chosen by remember{mutableStateOf<Word?>(null)};Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Словарь",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(q,{q=it},label={Text("汉字 / pinyin / русский")},singleLine=true,modifier=Modifier.fillMaxWidth());Text("30 стартовых слов · без подтверждённых уровней",style=MaterialTheme.typography.labelMedium);val found=vm.words.filter{w->listOf(w.simplified,w.traditional,w.pinyin,w.russian,java.text.Normalizer.normalize(w.pinyin,java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"),"")).any{it.contains(q.trim(),true)}};LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(found,key={it.id}){w->Card(onClick={chosen=w},modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(w.simplified,fontSize=25.sp);Text(w.pinyin);Text(w.russian)}}};if(found.isEmpty())item{Text("Ничего не найдено")}}};chosen?.let{WordDetails(vm,it){chosen=null}}}
@Composable private fun Interactive(vm:HanziViewModel,text:String){var chosen by remember{mutableStateOf<Word?>(null)};var unknown by remember{mutableStateOf<String?>(null)};val color=MaterialTheme.colorScheme.primary;val plain=MaterialTheme.colorScheme.onSurface;val tokens=remember(text){vm.segmenter.segment(text)};val annotated=remember(tokens,color,plain){buildAnnotatedString{tokens.forEachIndexed{i,t->pushStringAnnotation("token",i.toString());withStyle(SpanStyle(color=if(t.word!=null)color else plain)){append(t.text)};pop()}}};ClickableText(annotated,style=MaterialTheme.typography.bodyLarge.copy(fontSize=27.sp,lineHeight=42.sp),onClick={offset->annotated.getStringAnnotations("token",offset,offset).firstOrNull()?.item?.toIntOrNull()?.let{idx->tokens.getOrNull(idx)?.let{if(it.word!=null)chosen=it.word else unknown=it.text}}});chosen?.let{WordDetails(vm,it){chosen=null}};unknown?.let{s->AlertDialog(onDismissRequest={unknown=null},title={Text(s)},text={Text("Нет проверенной словарной статьи для этого фрагмента.")},confirmButton={TextButton({vm.speak(s)}){Text("Озвучить")}},dismissButton={TextButton({unknown=null}){Text("Закрыть")}})}}
@Composable private fun Reader(vm:HanziViewModel)=Page("Интерактивное чтение"){
    var pinyin by rememberSaveable{mutableStateOf(true)};var translation by rememberSaveable{mutableStateOf(false)};var text by rememberSaveable{mutableStateOf(practiceText)}
    Text("Авторский тренировочный текст. Нажмите на выделенное слово.");Panel("阅读 · Чтение"){Interactive(vm,text);if(pinyin && text==practiceText)Text(practicePinyin);if(translation&&text==practiceText)Text(practiceRu);Row{TextButton({pinyin=!pinyin}){Text(if(pinyin)"Скрыть pinyin" else "Pinyin")};TextButton({translation=!translation}){Text("Перевод")}};Row{TextButton({vm.speak(text)}){Text("Озвучить TTS")};TextButton({vm.speak(text,.7f)}){Text("0.7×")}}};OutlinedTextField(text,{if(it.length<=4000)text=it},label={Text("Свой текст · до 4000 символов")},modifier=Modifier.fillMaxWidth(),minLines=3);if(text!=practiceText)Text("Для пользовательского текста доступна словарная разбивка; полный перевод и контекстный pinyin не генерируются.");WideButton("Вернуть тренировочный текст"){text=practiceText}
}
@Composable private fun GrammarScreen(vm:HanziViewModel)=Page("Грамматика"){
    Text("5 авторских объяснений, уровень по программе 3.0 пока не присвоен.");grammar.forEach{g->Panel("${g.chinese} · ${g.name}"){Text(g.formula,fontWeight=FontWeight.Bold);Text(g.explanation);Interactive(vm,g.example);Text(g.pinyin);Text(g.ru)}}
}
@Composable private fun ReviewScreen(vm:HanziViewModel)=Page("Повторение"){
    var reveal by rememberSaveable{mutableStateOf(false)};var lastRated by rememberSaveable{mutableStateOf("")};val due=vm.words.filter{w->vm.reviews[w.id]?.due?.let{!it.isAfter(LocalDate.now())}==true};val w=due.firstOrNull();Text("${due.size} карточек на сегодня · SM-2-inspired");if(w==null){Text("Нет карточек к повторению. Добавьте слово из словаря.");WideButton("Добавить все 30 стартовых слов"){vm.words.forEach{vm.add(it)}}}else{LaunchedEffect(w.id){reveal=false;lastRated=""};Panel("Карточка"){Text(w.simplified,fontSize=44.sp);TextButton({vm.speak(w.simplified)}){Text("Прослушать")};if(!reveal)Button({reveal=true}){Text("Показать ответ")}else{Text(w.pinyin);Text(w.russian);for(pair in listOf(listOf(Grade.AGAIN to "Не знаю",Grade.HARD to "Трудно"),listOf(Grade.GOOD to "Хорошо",Grade.EASY to "Легко")))Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){pair.forEach{(g,label)->OutlinedButton(onClick={lastRated=w.id;vm.rate(w,g)},enabled=lastRated!=w.id,modifier=Modifier.weight(1f)){Text(label)}}}}}}
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
    val context=LocalContext.current;var tone by rememberSaveable{mutableIntStateOf(1)};val mic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)vm.record()else vm.message("Для записи нужно разрешение на микрофон")};val chars=listOf("妈","麻","马","骂","吗");val py=listOf("mā","má","mǎ","mà","ma")
    Panel("Изолированный слог") {Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){(1..5).forEach{n->FilterChip(selected=tone==n,onClick={tone=n;vm.erase()},enabled=!vm.recording,label={Text(if(n==5)"0" else "$n")})}};Text(chars[tone-1],fontSize=56.sp);Text(py[tone-1],fontSize=26.sp);Text(listOf("Ровный высокий","Восходящий","Низкий / нисходяще-восходящий","Резко нисходящий","Краткий без фиксированного контура")[tone-1]);Row{TextButton({vm.speak(chars[tone-1])},enabled=!vm.recording){Text("Эталон TTS")};TextButton({vm.speak(chars[tone-1],.7f)},enabled=!vm.recording){Text("0.7×")}};Text("Синтетический голос Android, не запись носителя.",style=MaterialTheme.typography.labelSmall);Button({if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.record()else mic.launch(Manifest.permission.RECORD_AUDIO)},enabled=!vm.recording&&!vm.playing,modifier=Modifier.fillMaxWidth()){Text(if(vm.recording)"Запись / анализ…" else "Записать 3 секунды")};Row{TextButton(vm::play,enabled=vm.samples!=null&&!vm.recording&&!vm.playing){Text("Моя запись")};TextButton(vm::erase,enabled=vm.samples!=null&&!vm.recording){Text("Удалить")}}}
    Panel("Контур высоты F0") {ToneChart(Pitch.template(tone),vm.pitch?.contour.orEmpty());Text("Янтарный — схематический ориентир; бирюзовый — измеренная F0. Высота относительно медианы записи, в полутонах. Время нормализовано.",style=MaterialTheme.typography.bodySmall);vm.pitch?.let{r->Text(r.message);Text("Озвученные кадры: ${(r.voicedFraction*100).toInt()}% (не точность)");Text(Pitch.feedback(r,tone))}?:Text("Запишите голос, чтобы получить измерение.");Text("Фонемы, артикуляция и точность тональной классификации: недоступны. Для нейтрального тона оценка контура не выполняется.")}
    Panel("Tone sandhi") {Text("你好 nǐ hǎo → ní hǎo (3 + 3 → 2 + 3)");Text("一次 yī cì → yí cì");Text("不是 bù shì → bú shì");Text("Третий тон перед другими тонами часто остаётся низким без полного подъёма. Изолированная тренировка не оценивает связную речь.")}
    Panel("声母 · артикуляция") {Text("j / q / x — передняя часть языка у твёрдого нёба; q с придыханием, j без него; x — щелевой звук.");Text("zh / ch / sh — кончик языка направлен к заальвеолярной области; ch с придыханием.");Text("ü — положение языка как для i, губы округлены.");Row{TextButton({vm.speak("鸡 七")}){Text("jī / qī")};TextButton({vm.speak("路 绿")}){Text("lù / lǜ")}}}
}
@Composable private fun ToneChart(reference:List<ContourPoint>,actual:List<ContourPoint>){val grid=MaterialTheme.colorScheme.outlineVariant;val primary=MaterialTheme.colorScheme.primary;val ref=MaterialTheme.colorScheme.tertiary;Canvas(Modifier.fillMaxWidth().height(170.dp)){val range=maxOf(6.0,(actual+reference).maxOfOrNull{abs(it.relativeSemitones)}?:6.0);fun point(p:ContourPoint): Offset { return Offset((p.time*size.width).toFloat(),(size.height*(.5-p.relativeSemitones/(2*range))).toFloat()) }
for(i in 0..4){val y=i*size.height/4;drawLine(grid,Offset(0f,y),Offset(size.width,y),1.dp.toPx())};for(i in 0..4){val x=i*size.width/4;drawLine(grid,Offset(x,0f),Offset(x,size.height),1.dp.toPx())};reference.zipWithNext().forEach{(a,b)->drawLine(ref,point(a),point(b),3.dp.toPx())};actual.zipWithNext().forEach{(a,b)->if(b.time-a.time<.12)drawLine(primary,point(a),point(b),2.dp.toPx())}}}
@Composable private fun ExamScreen(vm:HanziViewModel)=Page("Экзаменационная практика"){
    var i by rememberSaveable{mutableIntStateOf(0)};var score by rememberSaveable{mutableIntStateOf(0)};var selected by rememberSaveable{mutableIntStateOf(-1)};var finished by rememberSaveable{mutableStateOf(false)}
    Text("Original Practice · 5 заданий · не официальный HSK и не полный пробник.");if(finished){Panel("Результат") {Text("$score / ${questions.size}",fontSize=35.sp);Text("Результат передан на локальное сохранение.");WideButton("Начать заново"){i=0;score=0;selected=-1;finished=false}}}else{val q=questions[i];Panel("Задание ${i+1} / ${questions.size}"){Text(q.prompt,fontSize=24.sp);q.options.forEachIndexed{idx,option->OutlinedButton({selected=idx;if(idx==q.answer)score++},enabled=selected==-1,modifier=Modifier.fillMaxWidth()){Text(option)}};if(selected!=-1){Text(if(selected==q.answer)"Верно" else "Правильный ответ: ${q.options[q.answer]}",color=if(selected==q.answer)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error);Text(q.explanation);Button({if(i+1==questions.size){finished=true;vm.saveQuiz(score,questions.size)}else{i++;selected=-1}}){Text(if(i+1==questions.size)"Завершить" else "Далее")}}}}
    Panel("HSKK · самостоятельная практика") {Text("Расскажите за 1 минуту, зачем вы изучаете китайский: 你为什么学习汉语？");TextButton({vm.speak("你为什么学习汉语？")}){Text("Прослушать вопрос")};Text("Автоматическая проверка монолога и официальные таймеры пока не реализованы.")}
    Text("Официальные форматы: chinesetest.cn · HSK 3.0, запуск объявлен на 13.12.2026. Локальный центр необходимо уточнить.")
}
@Composable private fun Profile(vm:HanziViewModel)=Page("Профиль и настройки"){
    var date by rememberSaveable{mutableStateOf(vm.date)};Panel("Цель") {OutlinedTextField(date,{date=it},label={Text("Дата ГГГГ-ММ-ДД")},singleLine=true,modifier=Modifier.fillMaxWidth());WideButton("Сохранить дату"){vm.saveDate(date)};Text("Время в день: ${vm.minutes} мин");Slider(value=vm.minutes.toFloat(),onValueChange={vm.updateMinutes(it.toInt())},valueRange=15f..180f,steps=10);Text("30-дневный план: ${sprint(vm.words.size,30).first()} стартовое слово в день (не полная программа HSK)")};Panel("Внешний вид"){listOf("system" to "Системная","light" to "Светлая","dark" to "Тёмная").forEach{(id,label)->Row{RadioButton(vm.theme==id,{vm.updateTheme(id)});Text(label,Modifier.padding(top=12.dp))}};Text("Интерфейс: русский. Английская и китайская локализации запланированы.")};Panel("Статистика") {Text("Карточек: ${vm.reviews.size}");Text("Текущие успешные серии (сумма): ${vm.reviews.values.sumOf{it.repetitions}}");Text("Ошибок повторения: ${vm.reviews.values.sumOf{it.lapses}}");Text("Последний тест: ${vm.lastQuiz}");Text("Импортированных файлов: ${vm.books.size}")};Panel("Конфиденциальность") {Text("Нет сетевого разрешения. Голос хранится в памяти до удаления или закрытия приложения. Книги сохраняются в приватной директории Android.");WideButton("Удалить голосовую запись"){vm.erase()};Text("HanziFlow 0.1.0-alpha · сборка Android не проверена в текущей среде.")}
}
