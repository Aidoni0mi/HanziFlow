package com.hanziflow.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import java.io.File
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.UUID

class Speech(context:Context) {
    private var engine: TextToSpeech?=null
    @Volatile var available=false; private set
    init { engine=TextToSpeech(context.applicationContext) { status -> if(status==TextToSpeech.SUCCESS) { val t=engine; val voice=t?.voices?.filter { it.locale.language=="zh" && it.locale.country in listOf("CN","SG") && !it.isNetworkConnectionRequired }?.maxByOrNull{it.quality}; if(voice!=null && t!=null) { available=t.setVoice(voice)==TextToSpeech.SUCCESS } } } }
    fun say(text:String,speed:Float=1f):Boolean { if(!available) return false; val t=engine?:return false; t.setSpeechRate(speed); return t.speak(text,TextToSpeech.QUEUE_FLUSH,null,UUID.randomUUID().toString())==TextToSpeech.SUCCESS }
    fun stop(){engine?.stop()}
    fun close(){ available=false; engine?.stop(); engine?.shutdown(); engine=null }
}
object VoiceRecorder {
    const val SR=16000
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun record(seconds:Int=3):ShortArray=withContext(Dispatchers.IO) {
        require(seconds in 1..5); val min=AudioRecord.getMinBufferSize(SR,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT); require(min>0){"Микрофон недоступен"}
        val rec=AudioRecord(MediaRecorder.AudioSource.MIC,SR,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(min,4096)); val samples=ShortArray(SR*seconds); var offset=0
        try { check(rec.state==AudioRecord.STATE_INITIALIZED); rec.startRecording(); while(offset<samples.size) { coroutineContext.ensureActive(); val n=rec.read(samples,offset,minOf(1024,samples.size-offset)); check(n>0){"Ошибка записи: $n"}; offset+=n }; samples }
        finally { if(rec.recordingState==AudioRecord.RECORDSTATE_RECORDING) rec.stop(); rec.release() }
    }
    suspend fun play(samples:ShortArray)=withContext(Dispatchers.IO) {
        require(samples.isNotEmpty()); val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(samples.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
        try { check(track.write(samples,0,samples.size)==samples.size); track.play(); kotlinx.coroutines.delay(samples.size*1000L/SR+100) } finally { track.release() }
    }
}
class LibraryFiles(private val context:Context) {
    private val root=File(context.filesDir,"books").apply{mkdirs()}
    fun file(b:Book)=File(root,b.id)
    suspend fun import(uri:Uri):Book=withContext(Dispatchers.IO) {
        val resolver=context.contentResolver
        val name=resolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { if(it.moveToFirst()) it.getString(0) else null }?.take(200)?:"Файл"
        val mime=resolver.getType(uri); val kind=when { mime=="application/pdf" || name.endsWith(".pdf",true)->"pdf"; mime=="text/plain"||name.endsWith(".txt",true)->"txt"; else->error("Пока поддерживаются PDF и TXT") }
        val b=Book(UUID.randomUUID().toString(),name,kind); val out=file(b); val limit=if(kind=="pdf") 150L*1024*1024 else 2L*1024*1024
        try { resolver.openInputStream(uri)?.use { input -> out.outputStream().use { output -> val buf=ByteArray(65536); var total=0L; while(true){ coroutineContext.ensureActive(); val n=input.read(buf); if(n<0)break; total+=n; require(total<=limit){"Превышен размер файла"}; output.write(buf,0,n) } } }?:error("Файл недоступен")
            if(kind=="pdf") { require(out.inputStream().use{val h=ByteArray(5); it.read(h)==5 && String(h,Charsets.US_ASCII)=="%PDF-"}){"Неверный PDF"}; ParcelFileDescriptor.open(out,ParcelFileDescriptor.MODE_READ_ONLY).use { fd->PdfRenderer(fd).use{require(it.pageCount>0)} } } else { require(!out.readText(Charsets.UTF_8).contains('\u0000')){"Ожидается текст UTF-8"} }; b
        } catch(e:Throwable){out.delete();throw e}
    }
    suspend fun pdf(b:Book,page:Int):Pair<Bitmap,Int> =withContext(Dispatchers.IO){ ParcelFileDescriptor.open(file(b),ParcelFileDescriptor.MODE_READ_ONLY).use { fd-> PdfRenderer(fd).use { renderer-> renderer.openPage(page.coerceIn(0,renderer.pageCount-1)).use { p-> val width=minOf(1200,p.width*2); val height=(width.toDouble()*p.height/p.width).toInt().coerceIn(1,2400); val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.WHITE); p.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); bitmap to renderer.pageCount } } } }
    suspend fun text(b:Book)=withContext(Dispatchers.IO){file(b).readText(Charsets.UTF_8)}
    fun delete(b:Book)=file(b).delete()
}

suspend fun readLocalDialogueText(context:Context,uri:Uri,maxBytes:Int=64*1024):String=withContext(Dispatchers.IO){
    require(maxBytes>0){"Неверное ограничение размера"}
    val bytes=ByteArrayOutputStream(minOf(maxBytes,8192))
    context.contentResolver.openInputStream(uri)?.use{input->
        val buffer=ByteArray(8192);var total=0
        while(true){
            coroutineContext.ensureActive()
            val count=input.read(buffer)
            if(count<0)break
            total+=count
            require(total<=maxBytes){"TXT диалога превышает 64 КБ"}
            bytes.write(buffer,0,count)
        }
    }?:error("Файл недоступен")
    Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes.toByteArray()))
        .toString()
        .removePrefix("\uFEFF")
}
