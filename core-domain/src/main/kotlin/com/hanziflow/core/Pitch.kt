package com.hanziflow.core

import kotlin.math.*
data class PitchFrame(val seconds: Double, val hz: Double?, val periodicity: Double)
data class ContourPoint(val time: Double, val relativeSemitones: Double)
data class PitchResult(val frames: List<PitchFrame>, val contour: List<ContourPoint>, val voicedFraction: Double, val message: String)
/** YIN-inspired cumulative mean normalized difference; 40 ms frames, 10 ms hop. No ASR. */
object Pitch {
    fun analyze(pcm: ShortArray, sampleRate: Int = 16000): PitchResult {
        require(sampleRate in 8000..48000)
        val frame = sampleRate*40/1000; val hop = sampleRate/100; val minLag = sampleRate/550; val maxLag = min(sampleRate/65,frame/2)
        val frames = mutableListOf<PitchFrame>()
        for (start in 0..(pcm.size-frame) step hop) {
            val mean = (0 until frame).sumOf { pcm[start+it].toDouble() }/frame
            val rms = sqrt((0 until frame).sumOf { (pcm[start+it]-mean).pow(2) }/frame)/32768
            val t = (start+frame/2).toDouble()/sampleRate
            if(rms < .008) { frames += PitchFrame(t,null,0.0); continue }
            val cm = DoubleArray(maxLag+1) { 1.0 }; var sum = 0.0
            for(lag in 1..maxLag) {
                var d = 0.0
                for(i in 0 until frame-maxLag) { val x = (pcm[start+i]-pcm[start+i+lag]).toDouble(); d += x*x }
                sum += d; cm[lag] = if(sum>0) d*lag/sum else 1.0
            }
            var lag = minLag
            while(lag < maxLag && cm[lag] >= .18) lag++
            if(cm[lag] >= .18) { frames += PitchFrame(t,null,0.0); continue }
            while(lag<maxLag && cm[lag+1]<cm[lag]) lag++
            val delta = if(lag>minLag && lag<maxLag) { val a=cm[lag-1]; val b=cm[lag]; val c=cm[lag+1]; val den=a-2*b+c; if(abs(den)>1e-9) (.5*(a-c)/den).coerceIn(-.5,.5) else 0.0 } else 0.0
            frames += PitchFrame(t,sampleRate/(lag+delta), (1-cm[lag]).coerceIn(0.0,1.0))
        }
        val voiced = frames.filter { it.hz != null }; val fraction = voiced.size.toDouble()/frames.size.coerceAtLeast(1)
        val runs = mutableListOf<MutableList<PitchFrame>>()
        for (voicedFrame in voiced) {
            val current = runs.lastOrNull()
            if (current == null || voicedFrame.seconds-current.last().seconds > .031) {
                runs.add(mutableListOf(voicedFrame))
            } else {
                current += voicedFrame
            }
        }
        // A recording can contain several speech/noise bursts. Tone direction must
        // come from one continuous syllable rather than being drawn across silence.
        val primary = runs.maxByOrNull { it.size }.orEmpty()
        if(primary.size < 8) return PitchResult(frames,emptyList(),fraction,"Недостаточно устойчивого голоса. Повторите слог в тихом помещении.")
        val hz = primary.map { it.hz!! }.sorted(); val median = hz[hz.size/2]; val first=primary.first().seconds; val span=(primary.last().seconds-first).coerceAtLeast(.01)
        val contour = primary.map { ContourPoint((it.seconds-first)/span,12*ln(it.hz!!/median)/ln(2.0)) }
        return PitchResult(frames,contour,fraction,"F0 измерена по самому длинному устойчивому участку голоса. Это акустическая подсказка, не оценка произношения.")
    }
    fun template(tone: Int): List<ContourPoint> = when(tone) {
        1 -> listOf(0.0,0.0,0.0); 2 -> listOf(-3.0,-1.0,3.0); 3 -> listOf(1.0,-3.0,2.0); 4 -> listOf(3.0,0.0,-4.0); else -> emptyList()
    }.mapIndexed { i, x -> ContourPoint(i/2.0,x) }
    /** Heuristic direction only, no calibrated confidence or grade. */
    fun feedback(result: PitchResult, tone: Int): String {
        val p=result.contour; if(p.size<12 || tone !in 1..4) return "Оценка направления недоступна."
        val a=p.take(max(1,p.size/5)).map { it.relativeSemitones }.average(); val b=p.takeLast(max(1,p.size/5)).map { it.relativeSemitones }.average(); val middle=p.subList(p.size/3,p.size*2/3).map { it.relativeSemitones }.average()
        return when(tone) {
            1 -> if(p.map{it.relativeSemitones}.let{it.maxOrNull()!!-it.minOrNull()!!}>4) "Попробуйте удерживать высоту ровнее." else "Контур относительно ровный."
            2 -> if(b-a<2) "Попробуйте более заметный подъём к концу слога." else "В конце наблюдается подъём F0."
            3 -> if(min(a,b)-middle<1) "Для изолированного третьего тона попробуйте понижение и подъём; в связной речи форма меняется." else "Наблюдается впадина F0."
            else -> if(a-b<2) "Попробуйте более выраженное снижение высоты." else "К концу наблюдается снижение F0."
        }
    }
}
