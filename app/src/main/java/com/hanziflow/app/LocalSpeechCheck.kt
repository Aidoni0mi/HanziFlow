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
import com.hanziflow.core.bestSpeechHypothesis
import com.hanziflow.core.compareSpeech

class LocalSpeechCheck(private val context:Context){
    private var engine:SpeechRecognizer?=null
    var busy by mutableStateOf(false);private set
    var heard by mutableStateOf("");private set
    var feedback by mutableStateOf("");private set
    fun cancel(){val e=engine;engine=null;busy=false;if(e!=null){runCatching{e.cancel()};runCatching{e.destroy()}}}
    fun close()=cancel()
    fun reset(){cancel();heard="";feedback=""}
    private fun alternatives(results:Bundle?):List<String> =
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
    private fun errorMessage(error:Int):String = when(error){
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Речь не обнаружена. Попробуйте ещё раз в тихом помещении."
        SpeechRecognizer.ERROR_NO_MATCH -> "Речь не распознана. Произнесите фразу ближе к микрофону."
        SpeechRecognizer.ERROR_AUDIO -> "Системная служба не смогла получить звук с микрофона."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на запись с микрофона."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Системная служба речи занята. Подождите и повторите."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Локальный китайский языковой пакет недоступен."
        else -> "Локальное распознавание не завершено (код $error). Проверьте микрофон и китайский языковой пакет."
    }
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
                override fun onError(error:Int){
                    if(engine!==recognizer)return
                    feedback=if(heard.isBlank()) errorMessage(error)
                    else "${errorMessage(error)} Показан предварительный результат; он не оценён."
                    cancel()
                }
                override fun onResults(results:Bundle?){
                    if(engine!==recognizer)return
                    val choices=alternatives(results).filter{it.isNotBlank()}
                    heard=bestSpeechHypothesis(target,choices).orEmpty()
                    feedback=if(heard.isBlank())"Речь не распознана." else buildString{
                        append(compareSpeech(target,heard))
                        if(choices.size>1)append(" Из ${choices.size} локальных вариантов выбран ближайший по тексту.")
                    }
                    cancel()
                }
                override fun onPartialResults(partialResults:Bundle?){
                    if(engine!==recognizer)return
                    alternatives(partialResults).firstOrNull{it.isNotBlank()}?.let{
                        heard=it
                        feedback="Предварительный результат; дождитесь завершения распознавания."
                    }
                }
                override fun onEvent(eventType:Int,params:Bundle?){}
            })
            val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
            }
            busy=true;feedback="Слушаю…";recognizer.startListening(intent)
        }catch(e:Exception){cancel();feedback="Локальная служба речи недоступна. Проверьте настройки Android."}
    }
}
