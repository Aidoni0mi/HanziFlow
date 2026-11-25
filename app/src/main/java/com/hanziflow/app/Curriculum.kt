package com.hanziflow.app

import android.content.Context
import com.hanziflow.core.Word
import com.hanziflow.core.Segmenter
import java.text.Normalizer
import java.util.Locale

data class SyllabusWord(val serial:Int,val band:Int,val label:String,val levelName:String,val word:Word)
data class SyllabusHanzi(val band:Int,val hanzi:String,val writingBand:Int?,val readings:String)
data class SyllabusGrammar(val serial:Int,val band:Int,val content:String,val type:String,val category:String,val detail:String)
class Curriculum(val entries:List<SyllabusWord>,val hanzi:List<SyllabusHanzi>,val grammar:List<SyllabusGrammar>,starter:List<Word>){
    val byId=entries.associateBy{it.word.id}
    val byHanzi=hanzi.associateBy{it.hanzi}
    val reviewWords=starter+entries.map{it.word}
    val segmenter=Segmenter(entries.sortedBy{if(it.word.russian.isNotBlank())0 else 1}.map{it.word}+starter)
    val russianCount=entries.count{it.word.russian.isNotBlank()}
}
fun hskBand(level:Int)=level.coerceIn(1,7)
fun hskLabel(band:Int)=if(band>=7)"HSK 7–9" else "HSK $band"
fun inScope(band:Int,level:Int,cumulative:Boolean)=if(cumulative)band<=hskBand(level) else band==hskBand(level)
private fun readingKey(s:String)=Normalizer.normalize(s,Normalizer.Form.NFC).lowercase(Locale.ROOT).filterNot{it.isWhitespace()||it=='-'||it=='\''||it=='’'}
fun searchKey(s:String):String=Normalizer.normalize(s.lowercase(Locale.ROOT).replace('ü','v'),Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"),"").replace("u:","v").filterNot{it.isWhitespace()||it=='-'||it=='\''||it=='’'}
fun posRu(pos:String):String {
    val names=mapOf("名" to "существительное","动" to "глагол","形" to "прилагательное","副" to "наречие","代" to "местоимение","数" to "числительное","量" to "счётное слово","介" to "предлог","连" to "союз","助" to "частица","叹" to "междометие","拟声" to "звукоподражание","前缀" to "префикс","后缀" to "суффикс")
    return pos.split('、').filter{it.isNotBlank()}.joinToString(" / "){names[it.trim('（','）')]?:it}.ifBlank{"Не указана"}
}
private fun fields(ctx:Context,name:String,size:Int):List<List<String>> = ctx.assets.open("hsk2025/$name.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
    lines.drop(1).filter{it.isNotBlank()}.map{line->line.split('\t').also{require(it.size==size){"Invalid $name row"}}}.toList()
}
fun readCurriculum(ctx:Context,starter:List<Word>):Curriculum{
    val local=starter.groupBy{it.simplified}
    val v=fields(ctx,"vocabulary",7).map{f->
        val serial=f[0].toInt();val band=f[1].toInt();val han=f[3];val py=f[4]
        val original=local[han]?.firstOrNull{readingKey(it.pinyin)==readingKey(py)}
        val w=Word("hsk25-$serial",han,original?.traditional.orEmpty(),py,original?.russian.orEmpty(),original?.example.orEmpty(),original?.examplePinyin.orEmpty(),original?.exampleRu.orEmpty(),posRu(f[5]),band,if(original!=null)"hsk2025 + original-unreviewed" else "hsk2025")
        SyllabusWord(serial,band,f[2],f[6],w)
    }
    val h=fields(ctx,"hanzi",4).map{f->SyllabusHanzi(f[0].toInt(),f[1],f[2].toIntOrNull(),f[3])}
    val g=fields(ctx,"grammar",6).map{f->SyllabusGrammar(f[0].toInt(),f[1].toInt(),f[2],f[3],f[4],f[5])}
    fun counts(levels:List<Int>)=(1..7).map{n->levels.count{it==n}}
    require(v.size==11000 && v.map{it.serial}.toSet().size==11000 && counts(v.map{it.band})==listOf(300,200,500,1000,1600,1800,5600))
    require(h.size==3088 && h.map{it.hanzi}.toSet().size==3088 && counts(h.map{it.band})==listOf(246,125,284,441,431,413,1148))
    require(h.all{it.readings.isNotBlank()} && counts(h.mapNotNull{it.writingBand})==listOf(0,100,150,150,150,150,500))
    require(g.size==593 && counts(g.map{it.band})==listOf(70,78,96,95,70,50,134))
    return Curriculum(v,h,g,starter)
}
