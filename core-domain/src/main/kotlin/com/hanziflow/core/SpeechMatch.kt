package com.hanziflow.core

fun speechChars(s:String):List<Int> = s.codePoints().toArray().filter{Character.isLetterOrDigit(it)}
fun compareSpeech(target:String,heard:String):String {
    val a=speechChars(target);val b=speechChars(heard)
    if(a.isEmpty()||b.isEmpty())return "Недостаточно текста для сравнения."
    var prev=IntArray(b.size+1){it}
    for(i in a.indices){val row=IntArray(b.size+1);row[0]=i+1;for(j in b.indices)row[j+1]=minOf(row[j]+1,prev[j+1]+1,prev[j]+if(a[i]==b[j])0 else 1);prev=row}
    val distance=prev.last()
    return if(distance==0)"Распознанный текст совпал. Это не оценка тонов или фонем." else "Различий при сравнении текста: $distance. Возможны ошибки распознавания или омонимы; это не фонетическая оценка."
}
