import com.hanziflow.core.*
import java.time.LocalDate
import kotlin.math.*
fun main() {
    val sr=16000
    fun signal(a: Double,b: Double, seconds: Int=1): ShortArray { var phase=0.0; return ShortArray(sr*seconds) { i -> phase+=2*PI*(a+(b-a)*i/(sr*seconds))/sr; (12000*sin(phase)).toInt().toShort() } }
    for(hz in listOf(90.0,140.0,220.0,360.0,480.0)) { val r=Pitch.analyze(signal(hz,hz)); check(r.contour.isNotEmpty()); check(abs(r.frames.mapNotNull{it.hz}.average()-hz)<3) { "F0 $hz" }; println("PASS F0 $hz Hz") }
    check(Pitch.analyze(ShortArray(sr)).contour.isEmpty()); println("PASS silence")
    check(Pitch.analyze(ShortArray(0)).contour.isEmpty()); println("PASS empty")
    val rise=Pitch.analyze(signal(130.0,230.0)); check(rise.contour.last().relativeSemitones>rise.contour.first().relativeSemitones+5); println("PASS rise")
    val fall=Pitch.analyze(signal(250.0,130.0)); check(fall.contour.last().relativeSemitones<fall.contour.first().relativeSemitones-5); println("PASS fall")
    val tones=pinyinToneSyllables("Nǐ hǎo, lǜ chá! ni3hao3"); check(tones.map{it.tone}==listOf(3,3,4,2,3,3)); check(tones.map{it.plain}==listOf("ni","hao","lü","cha","ni","hao")); check(phraseToneTemplate("nǐ hǎo").let{it.first().time==0.0&&it.last().time==1.0}); println("PASS pinyin tone guide")
    check(speechChars("ＡＢＣ１２３")==speechChars("abc123")); check(bestSpeechHypothesis("我学习汉语",listOf("我学汉语","我学习汉语"))=="我学习汉语"); println("PASS speech matching")
    val split=ShortArray(sr*4)
    for(i in 4000 until 7200) split[i]=(12000*sin(2*PI*220*i/sr)).toInt().toShort()
    for(i in 24000 until 30400) split[i]=(12000*sin(2*PI*330*i/sr)).toInt().toShort()
    val primary=Pitch.analyze(split); check(primary.contour.isNotEmpty()); check(primary.contour.size<primary.frames.count{it.hz!=null}); check(primary.contour.maxOf{it.relativeSemitones}-primary.contour.minOf{it.relativeSemitones}<3); println("PASS continuous pitch run")
    val w=Word("ai","人工智能","人工智慧","réngōng zhìnéng","ИИ","","","","noun",null,"original")
    val text="我正在学习人工智能。𠀀"; val tokens=Segmenter(listOf(w)).segment(text); check(tokens.joinToString(""){it.text}==text && tokens.any{it.word==w}); println("PASS segmentation/Unicode")
    check(sandhi(listOf(Syllable("你",3),Syllable("好",3)))==listOf(2,3)); check(sandhi(listOf(Syllable("一",1),Syllable("次",4)))==listOf(2,4)); check(sandhi(listOf(Syllable("不",4),Syllable("是",4)))==listOf(2,4)); check(sandhi(listOf(Syllable("一",1,true),Syllable("次",4)))==listOf(1,4)); println("PASS basic sandhi")
    val d=LocalDate.of(2026,11,25); val r=schedule(Review(due=d),Grade.GOOD,d); check(r.due==d.plusDays(1)); check(schedule(r,Grade.AGAIN,d).lapses==1); check(schedule(r,Grade.EASY,d).interval==7L); check(schedule(Review(2,0,2,2.5,d),Grade.HARD,d).interval==3L); check(restoreReview(-1,-1,-1,Double.NaN,"bad",d)==Review(due=d)); println("PASS SRS")
    check(sprint(100,30).sum()==100); println("PASS sprint")
}
