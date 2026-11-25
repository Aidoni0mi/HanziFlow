package com.hanziflow.core
import kotlin.test.*
import kotlin.math.*
import java.time.LocalDate
class CoreTest {
    @Test fun pitch() { val pcm=ShortArray(16000){(12000*sin(2*PI*220*it/16000)).toInt().toShort()}; val r=Pitch.analyze(pcm); assertTrue(abs(r.frames.mapNotNull{it.hz}.average()-220)<3); assertTrue(Pitch.analyze(ShortArray(16000)).contour.isEmpty()) }
    @Test fun shortSyllableInsideLongRecording() {
        val pcm=ShortArray(48000)
        for(i in 16000 until 19200) pcm[i]=(12000*sin(2*PI*220*i/16000)).toInt().toShort()
        val r=Pitch.analyze(pcm)
        assertTrue(r.voicedFraction < .12)
        assertTrue(r.contour.isNotEmpty())
        assertTrue(Pitch.analyze(ShortArray(48000)).contour.isEmpty())
    }
    @Test fun toneFourTemplateFalls() {
        val contour=Pitch.template(4)
        assertTrue(contour.zipWithNext().all { (a,b)->a.relativeSemitones>b.relativeSemitones })
        assertTrue(Pitch.template(5).isEmpty())
    }
    @Test fun sandhiRules() { assertEquals(listOf(2,3),sandhi(listOf(Syllable("你",3),Syllable("好",3)))); assertEquals(listOf(2,4),sandhi(listOf(Syllable("不",4),Syllable("是",4)))) }
    @Test fun review() { val d=LocalDate.of(2026,11,25); assertEquals(d.plusDays(1),schedule(Review(),Grade.GOOD,d).due); assertEquals(1,schedule(Review(),Grade.AGAIN,d).lapses) }
    @Test fun plan() { assertEquals(100,sprint(100,30).sum()) }
    @Test fun segmentation() { val w=Word("1","人工智能","人工智能","réngōng zhìnéng","ИИ","","","","noun",null,"original"); assertEquals(listOf("我","人工智能","。"),Segmenter(listOf(w)).segment("我人工智能。").map{it.text}) }
}
