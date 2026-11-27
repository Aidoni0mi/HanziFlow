package com.hanziflow.core
import kotlin.test.*
import kotlin.math.*
import java.time.LocalDate
class CoreTest {
    @Test fun speechTextComparison(){
        assertEquals(speechChars("你好，世界！"),speechChars("你好世界"))
        assertEquals(speechChars("ＡＢＣ１２３"),speechChars("abc123"))
        assertTrue(compareSpeech("你好","你好！").startsWith("Распознанный текст совпал"))
        assertTrue(compareSpeech("你好","你号").contains("Различий при сравнении текста: 1"))
        assertTrue(compareSpeech("我学习汉语","我学汉语").contains("Различий при сравнении текста: 1"))
        assertEquals(.8,speechComparison("我学习汉语","我学汉语")!!.similarity,0.0001)
        assertEquals("我学习汉语",bestSpeechHypothesis("我学习汉语",listOf("我学汉语","我学习汉语")))
        assertEquals("目标",bestSpeechHypothesis("目标",listOf("先到","目标")))
        assertNull(bestSpeechHypothesis("你好",listOf("", "   ")))
        assertTrue(compareSpeech("你好","").contains("Недостаточно"))
    }
    @Test fun pitch() { val pcm=ShortArray(16000){(12000*sin(2*PI*220*it/16000)).toInt().toShort()}; val r=Pitch.analyze(pcm); assertTrue(abs(r.frames.mapNotNull{it.hz}.average()-220)<3); assertTrue(Pitch.analyze(ShortArray(16000)).contour.isEmpty()) }
    @Test fun shortSyllableInsideLongRecording() {
        val pcm=ShortArray(48000)
        for(i in 16000 until 19200) pcm[i]=(12000*sin(2*PI*220*i/16000)).toInt().toShort()
        val r=Pitch.analyze(pcm)
        assertTrue(r.voicedFraction < .12)
        assertTrue(r.contour.isNotEmpty())
        assertTrue(Pitch.analyze(ShortArray(48000)).contour.isEmpty())
    }
    @Test fun pitchContourUsesOneContinuousVoicedRun() {
        val pcm=ShortArray(64000)
        for(i in 4000 until 7200) pcm[i]=(12000*sin(2*PI*220*i/16000)).toInt().toShort()
        for(i in 24000 until 30400) pcm[i]=(12000*sin(2*PI*330*i/16000)).toInt().toShort()
        val result=Pitch.analyze(pcm)
        val allVoiced=result.frames.count{it.hz!=null}
        assertTrue(result.contour.isNotEmpty())
        assertTrue(result.contour.size<allVoiced)
        assertTrue(result.contour.maxOf{it.relativeSemitones}-result.contour.minOf{it.relativeSemitones}<3.0)
    }
    @Test fun toneFourTemplateFalls() {
        val contour=Pitch.template(4)
        assertTrue(contour.zipWithNext().all { (a,b)->a.relativeSemitones>b.relativeSemitones })
        assertTrue(Pitch.template(5).isEmpty())
    }
    @Test fun sandhiRules() { assertEquals(listOf(2,3),sandhi(listOf(Syllable("你",3),Syllable("好",3)))); assertEquals(listOf(2,4),sandhi(listOf(Syllable("不",4),Syllable("是",4)))) }
    @Test fun reviewFirstIntervals() {
        val d=LocalDate.of(2026,11,25)
        assertEquals(1L,schedule(Review(due=d),Grade.HARD,d).interval)
        assertEquals(1L,schedule(Review(due=d),Grade.GOOD,d).interval)
        assertEquals(4L,schedule(Review(due=d),Grade.EASY,d).interval)
        val first=schedule(Review(due=d),Grade.GOOD,d)
        assertEquals(2L,schedule(first,Grade.HARD,d).interval)
        assertEquals(3L,schedule(first,Grade.GOOD,d).interval)
        assertEquals(7L,schedule(first,Grade.EASY,d).interval)
    }
    @Test fun reviewAgainResetsSeriesAndRecordsLapse() {
        val d=LocalDate.of(2026,11,25)
        val result=schedule(Review(5,2,30,2.1,d),Grade.AGAIN,d)
        assertEquals(0,result.repetitions);assertEquals(3,result.lapses);assertEquals(1,result.interval)
        assertEquals(1.9,result.ease,0.0001);assertEquals(d.plusDays(1),result.due)
    }
    @Test fun successfulReviewIntervalsKeepGrowingUntilCap() {
        val d=LocalDate.of(2026,11,25)
        val hard=schedule(Review(2,0,2,2.5,d),Grade.HARD,d)
        assertEquals(3L,hard.interval)
        val capped=schedule(Review(40,0,3650,3.5,d),Grade.EASY,d)
        assertEquals(3650L,capped.interval);assertEquals(d.plusDays(3650),capped.due)
    }
    @Test fun restoresLegacyReviewSafely() {
        val d=LocalDate.of(2026,11,25)
        val restored=restoreReview(-2,-3,-10,Double.NaN,"not-a-date",d)
        assertEquals(0,restored.repetitions);assertEquals(0,restored.lapses);assertEquals(0,restored.interval)
        assertEquals(2.5,restored.ease);assertEquals(d,restored.due)
    }
    @Test fun plan() { assertEquals(100,sprint(100,30).sum()) }
    @Test fun segmentation() { val w=Word("1","人工智能","人工智能","réngōng zhìnéng","ИИ","","","","noun",null,"original"); assertEquals(listOf("我","人工智能","。"),Segmenter(listOf(w)).segment("我人工智能。").map{it.text}) }
}
