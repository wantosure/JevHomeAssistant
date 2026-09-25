package com.jev.assistant.audio

import android.content.Context
import android.util.Log
import com.jev.assistant.JevApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPipeline(
    private val context: Context,
    private val deviceRegistry: com.jev.assistant.device.DeviceRegistry? = null,
    private val onUtteranceFinal: (String) -> Unit
) {
    companion object {
        private const val TAG = "AudioPipeline"
    }

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    private val _currentSubtitle = MutableStateFlow("")
    val currentSubtitle: StateFlow<String> = _currentSubtitle.asStateFlow()

    private var useSenseVoice = JevApp.isNativeSupported

    private val _currentEngineName = MutableStateFlow(
        if (useSenseVoice) "阿里 SenseVoice 端侧离线 ASR" else "系统高灵敏 ASR"
    )
    val currentEngineName: StateFlow<String> = _currentEngineName.asStateFlow()

    // 备用：系统级 ASR（经由高灵敏度调参，100ms 拾音，3秒静音容错，零 Native 依赖）
    private val systemAsr by lazy {
        SystemAsrEngine(
            context = context,
            onPartialResult = { partial ->
                _currentSubtitle.value = partial
            },
            onFinalResult = { rawUtterance ->
                val finalUtterance = HotwordsManager.correctHomophones(rawUtterance)
                _currentSubtitle.value = finalUtterance
                try {
                    onUtteranceFinal(finalUtterance)
                } catch (t: Throwable) {
                    Log.e(TAG, "派发系统 ASR 结果异常: ${t.message}", t)
                }
            },
            onRmsChanged = { rms ->
                _currentRms.value = rms
            }
        )
    }

    // 主力：阿里开源 SenseVoice-Small 端侧高敏神经网络 ASR (注入全屋设备热词与同音纠错)
    private val senseVoiceAsr by lazy {
        SenseVoiceAsrEngine(
            context = context,
            deviceRegistry = deviceRegistry ?: (context.applicationContext as? JevApp)?.deviceRegistry,
            onPartialResult = { partial ->
                _currentSubtitle.value = partial
            },
            onFinalResult = { finalUtterance ->
                _currentSubtitle.value = finalUtterance
                try {
                    onUtteranceFinal(finalUtterance)
                } catch (t: Throwable) {
                    Log.e(TAG, "派发 SenseVoice ASR 结果异常: ${t.message}", t)
                }
            },
            onRmsChanged = { rms ->
                _currentRms.value = rms
            },
            onInitFailed = {
                Log.w(TAG, "SenseVoice 端侧引擎异常，平滑降级至高敏系统级 ASR...")
                useSenseVoice = false
                _currentEngineName.value = "系统高灵敏 ASR (已自动降级)"
                if (_isListening.value) {
                    try {
                        systemAsr.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "启动系统 ASR 失败: ${e.message}", e)
                    }
                }
            }
        )
    }

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true
        _currentSubtitle.value = "正在聆听..."

        if (useSenseVoice) {
            try {
                senseVoiceAsr.start()
            } catch (t: Throwable) {
                Log.e(TAG, "启动 SenseVoice 发生异常: ${t.message}，立即回退至系统 ASR", t)
                useSenseVoice = false
                systemAsr.start()
            }
        } else {
            systemAsr.start()
        }
    }

    fun stopListening() {
        _isListening.value = false
        try {
            if (useSenseVoice) {
                senseVoiceAsr.stop()
            } else {
                systemAsr.stop()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "停止 ASR 异常: ${t.message}", t)
        }
        _currentRms.value = 0f
        _currentSubtitle.value = ""
    }

    fun switchEngine(useNative: Boolean) {
        if (useSenseVoice == useNative) return
        val wasListening = _isListening.value
        if (wasListening) {
            stopListening()
        }
        useSenseVoice = useNative && JevApp.isNativeSupported
        _currentEngineName.value = if (useSenseVoice) "阿里 SenseVoice 端侧离线 ASR" else "系统高灵敏 ASR"
        if (wasListening) {
            startListening()
        }
    }

    // 允许界面模拟输入与测试
    fun updateStreamingSubtitle(partial: String) {
        _currentSubtitle.value = partial
    }

    fun submitFinalUtterance(text: String) {
        _currentSubtitle.value = text
        onUtteranceFinal(text)
    }
}
