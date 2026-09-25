package com.jev.assistant.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class SystemAsrEngine(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var shouldKeepListening = false
    private var restartCount = 0

    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    // 看门狗：防止在任何视图切换（如展开设备看板）或系统挂起时丢失监听
    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (shouldKeepListening && !isListening) {
                initAndStartRecognizer()
            }
            if (shouldKeepListening) {
                mainHandler.postDelayed(this, 3000)
            }
        }
    }

    fun start() {
        shouldKeepListening = true
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            initAndStartRecognizer()
            mainHandler.postDelayed(watchdogRunnable, 3000)
        }
    }

    fun stop() {
        shouldKeepListening = false
        isListening = false
        _isRecognizing.value = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            safelyDestroyRecognizer()
        }
    }

    private fun safelyDestroyRecognizer() {
        try {
            recognizer?.cancel()
            recognizer?.setRecognitionListener(null)
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }

    private fun initAndStartRecognizer() {
        if (!shouldKeepListening) return

        safelyDestroyRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onPartialResult("系统未就绪语音识别服务")
            return
        }

        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        _isRecognizing.value = true
                        restartCount = 0
                    }

                    override fun onBeginningOfSpeech() {
                        onPartialResult("正在聆听...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val linear = ((rmsdB + 3f) / 12f).coerceIn(0f, 1f)
                        val amplified = sqrt(linear.toDouble()).toFloat()
                        onRmsChanged(amplified)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isRecognizing.value = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        _isRecognizing.value = false
                        // 无论发生任何中断，只要应持续监听，就在 200ms 内重置并重启
                        if (shouldKeepListening) {
                            mainHandler.removeCallbacks(watchdogRunnable)
                            mainHandler.postDelayed({
                                if (shouldKeepListening) {
                                    initAndStartRecognizer()
                                }
                            }, 200)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        _isRecognizing.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onFinalResult(text)
                        }

                        // 成句后立刻无缝重启，保持连贯识别
                        if (shouldKeepListening) {
                            mainHandler.postDelayed({
                                if (shouldKeepListening) {
                                    initAndStartRecognizer()
                                }
                            }, 150)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra("android.speech.extra.DICTATION_MODE", true)
                // 极致微弱音量收音优化：只要 100ms 弱音即触发拾音，允许 3 秒停顿细语不被切断
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 100L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra("android.speech.extra.AUDIO_SOURCE", 1) // MIC 源
            }

            recognizer?.startListening(intent)
        } catch (_: Exception) {
            isListening = false
            if (shouldKeepListening) {
                mainHandler.postDelayed({ initAndStartRecognizer() }, 500)
            }
        }
    }
}
