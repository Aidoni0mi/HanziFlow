package com.hanziflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.*
import com.hanziflow.core.compareSpeech

class LocalSpeechCheck(private val context:Context){
    private var engine:SpeechRecognizer?=null
    var busy by mutableStateOf(false);private set
    var heard by mutableStateOf("");private set
    var feedback by mutableStateOf("");private set
    fun cancel(){val e=engine;engine=null;busy=false;if(e!=null){runCatching{e.cancel()};runCatching{e.destroy()}}}
    fun close()=cancel()
    fun reset(){cancel();heard="";feedback=""}
    fun start(target:String){
        reset()
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){feedback="Разрешите доступ к микрофону.";return}
        if(Build.VERSION.SDK_INT<31){feedback="Локальное распознавание требуется Android 12 или новее.";return}
        if(!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)){feedback="На устройстве нет локального распознавания. Установите китайский языковой пакет системной службы речи.";return}
        try{
            val recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            engine=recognizer
            recognizer.setRecognitionListener(object:RecognitionListener{
                override fun onReadyForSpeech(params:Bundle?){}
                override fun onBeginningOfSpeech(){}
                override fun onRmsChanged(rmsdB:Float){}
                override fun onBufferReceived(buffer:ByteArray?){}
                override fun onEndOfSpeech(){}
                override fun onError(error:Int){if(engine!==recognizer)return;feedback="Распознавание не завершено (код $error). Проверьте микрофон и локальный китайский пакет.";cancel()}
                override fun onResults(results:Bundle?){
                    if(engine!==recognizer)return
                    heard=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    feedback=if(heard.isBlank())"Речь не распознана." else compareSpeech(target,heard)
                    cancel()
                }
                override fun onPartialResults(partialResults:Bundle?){}
                override fun onEvent(eventType:Int,params:Bundle?){}
            })
            val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
            }
            busy=true;feedback="Слушаю…";recognizer.startListening(intent)
        }catch(e:Exception){cancel();feedback="Локальная служба речи недоступна. Проверьте настройки Android."}
    }
}
