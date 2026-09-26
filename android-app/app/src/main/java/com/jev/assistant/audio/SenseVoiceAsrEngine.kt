package com.jev.assistant.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jev.assistant.JevApp
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * 阿里 SenseVoice-Small + Silero-VAD 纯端侧独立神经网络 ASR 引擎
 * 针对极微弱声音（Whisper）、轻声细语和嘈杂环境深度定制
 * 具有完整的异常拦截、本地物理文件缓存和安全降级机制
 */
class SenseVoiceAsrEngine(
    private val context: Context,
    private val deviceRegistry: com.jev.assistant.device.DeviceRegistry? = null,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onInitFailed: () -> Unit
) {
    companion object {
        private const val TAG = "SenseVoiceAsr"
        private const val SAMPLE_RATE = 16000
        private const val DIGITAL_GAIN = 3.0f // 微弱音量放大倍数（约 +9.5dB）
    }

    private val isRunning = AtomicBoolean(false)
    private var recordThread: Thread? = null
    private var audioRecord: AudioRecord? = null

    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    @Synchronized
    private fun initModelsIfNeeded(): Boolean {
        if (!JevApp.isNativeSupported) {
            Log.w(TAG, "Native 库未就绪，终止 SenseVoice 初始化")
            return false
        }
        if (recognizer != null && vad != null) return true

        try {
            Log.i(TAG, "正在准备 SenseVoice 与 Silero-VAD 模型文件...")
            val sherpaDir = File(context.filesDir, "sherpa")
            if (!sherpaDir.exists()) sherpaDir.mkdirs()

            val vadFile = File(sherpaDir, "silero_vad.onnx")
            val modelFile = File(sherpaDir, "model.int8.onnx")
            val tokensFile = File(sherpaDir, "tokens.txt")

            // 安全解压模型到本地私有目录（使用物理文件构造避开 AAssetManager 内存限制）
            copyAssetToFileIfNeeded("sherpa/silero_vad.onnx", vadFile, 500_000L)
            copyAssetToFileIfNeeded(
                "sherpa/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17/model.int8.onnx",
                modelFile,
                200_000_000L
            )
            copyAssetToFileIfNeeded(
                "sherpa/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17/tokens.txt",
                tokensFile,
                200_000L
            )

            // 1. 初始化 Silero-VAD
            val vadConfig = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = vadFile.absolutePath,
                    threshold = 0.28f, // 极灵敏阈值，微弱耳语即可激活
                    minSilenceDuration = 0.7f,
                    minSpeechDuration = 0.12f,
                    windowSize = 512,
                    maxSpeechDuration = 12.0f
                ),
                sampleRate = SAMPLE_RATE,
                numThreads = 2
            )
            vad = Vad(assetManager = null, config = vadConfig)

            // 2. 初始化阿里 SenseVoice-Small
            val modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = modelFile.absolutePath,
                    language = "zh",
                    useInverseTextNormalization = true
                ),
                tokens = tokensFile.absolutePath,
                numThreads = 2,
                debug = false
            )
            val recognizerConfig = OfflineRecognizerConfig(
                modelConfig = modelConfig
            )
            recognizer = OfflineRecognizer(assetManager = null, config = recognizerConfig)

            Log.i(TAG, "SenseVoice 端侧模型初始化成功！")
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "初始化 SenseVoice 端侧模型失败: ${t.message}", t)
            release()
            return false
        }
    }

    private fun copyAssetToFileIfNeeded(assetPath: String, destFile: File, expectedMinSize: Long) {
        if (destFile.exists() && destFile.length() >= expectedMinSize) {
            return
        }
        val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
        try {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = context.assets.open(assetPath)
                outputStream = FileOutputStream(tmpFile)
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
            }
            if (!tmpFile.renameTo(destFile)) {
                tmpFile.copyTo(destFile, overwrite = true)
                tmpFile.delete()
            }

            // 必须校验落盘后的真实大小。
            // 仓库用 Git LFS 管理 onnx 文件，未执行 `git lfs pull` 时资源是几百字节的
            // 指针文本。那种内容交给原生推理库会在 C++ 层直接 abort，Java 的
            // catch 拦不住，表现为打开即闪退。这里主动失败，让上层降级到系统 ASR。
            if (destFile.length() < expectedMinSize) {
                destFile.delete()
                throw IllegalStateException(
                    "模型文件 $assetPath 大小异常（实际 ${destFile.length()} 字节，" +
                        "期望至少 $expectedMinSize 字节）。若资源是 Git LFS 指针，" +
                        "请先执行 git lfs pull 再重新构建。",
                )
            }

            Log.i(TAG, "成功解压模型文件: ${destFile.name} (${destFile.length()} bytes)")
        } catch (e: Exception) {
            tmpFile.delete()
            throw e
        }
    }

    fun start() {
        if (isRunning.getAndSet(true)) return

        recordThread = thread(name = "SenseVoice-AudioRecord", priority = Thread.MAX_PRIORITY) {
            val ok = initModelsIfNeeded()
            if (!ok) {
                isRunning.set(false)
                mainHandler.post { onInitFailed() }
                return@thread
            }

            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * 2)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioRecord 硬件初始化失败")
                }

                if (AutomaticGainControl.isAvailable()) {
                    try {
                        val agc = AutomaticGainControl.create(audioRecord!!.audioSessionId)
                        agc?.enabled = true
                    } catch (_: Exception) {}
                }

                audioRecord!!.startRecording()
                Log.i(TAG, "AudioRecord 开始高敏录音...")
                _isRecognizing.value = true

                // 预先动态生成并初始化通用与全屋专属合并热词库
                HotwordsManager.buildHotwords(deviceRegistry)
                val shortBuffer = ShortArray(512)
                var hasSpeechDetected = false

                while (isRunning.get()) {
                    val read = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: -1
                    if (read <= 0) continue

                    // 数字前置放大器：将微弱信号乘以增益，解决微小音量问题
                    val floatSamples = FloatArray(read)
                    var sumSquares = 0.0
                    for (i in 0 until read) {
                        val amplified = (shortBuffer[i] * DIGITAL_GAIN).coerceIn(-32768f, 32767f)
                        val norm = amplified / 32768.0f
                        floatSamples[i] = norm
                        sumSquares += (norm * norm)
                    }

                    // 计算实时音量 RMS 驱动 UI 极光动效
                    val rms = sqrt(sumSquares / read).toFloat().coerceIn(0f, 1f)
                    mainHandler.post { onRmsChanged(rms) }

                    // 送入 Silero-VAD
                    val v = vad
                    val rec = recognizer
                    if (v != null && rec != null) {
                        v.acceptWaveform(floatSamples)

                        if (v.isSpeechDetected() && !hasSpeechDetected) {
                            hasSpeechDetected = true
                            mainHandler.post { onPartialResult("正在聆听...") }
                        }

                        while (!v.empty()) {
                            val segment = v.front()
                            val samples = segment.samples
                            if (samples.isEmpty() || samples.size < 800) {
                                v.pop()
                                continue
                            }

                            // 内存拷贝隔离，防止 native 指针在 pop 后提前失效
                            val samplesCopy = samples.clone()
                            v.pop()
                            hasSpeechDetected = false

                            var stream: com.k2fsa.sherpa.onnx.OfflineStream? = null
                            try {
                                // 必须使用标准 createStream()：底层 C++ 原生对 CTC 结构（SenseVoice）不支持 contextual biasing，
                                // 调用带 hotwords 的 JNI 会触发 C++ SIGSEGV 闪退。热词加权通过后处理纠偏算法执行。
                                val activeStream = rec.createStream()
                                stream = activeStream

                                activeStream.acceptWaveform(samplesCopy, SAMPLE_RATE)
                                rec.decode(activeStream)
                                val res = rec.getResult(activeStream)

                                // 清洗 SenseVoice 的元标记，例如 <|zh|><|NEUTRAL|><|Speech|>
                                val cleanText = res.text
                                    .replace(Regex("<\\|.*?\\|>"), "")
                                    .replace(" ", "")
                                    .trim()

                                // 核心算法纠偏：使用智能家居同音与近音词库进行后处理校正
                                val refinedText = HotwordsManager.correctHomophones(cleanText)

                                if (refinedText.isNotBlank()) {
                                    Log.i(TAG, "SenseVoice 精准识别(经热词加权与纠偏): $refinedText")
                                    mainHandler.post {
                                        try {
                                            onFinalResult(refinedText)
                                        } catch (cbEx: Throwable) {
                                            Log.e(TAG, "onFinalResult 回调执行异常: ${cbEx.message}", cbEx)
                                        }
                                    }
                                }
                            } catch (decErr: Throwable) {
                                Log.e(TAG, "SenseVoice 解码异常: ${decErr.message}", decErr)
                                com.jev.assistant.utils.CrashReporter.recordCrash(context, Thread.currentThread(), decErr)
                            } finally {
                                try {
                                    stream?.release()
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "SenseVoice 录音识别循环捕获异常: ${t.message}", t)
                mainHandler.post { onInitFailed() }
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                audioRecord = null
                _isRecognizing.value = false
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            recordThread?.interrupt()
            recordThread = null
        } catch (_: Exception) {}
        _isRecognizing.value = false
        mainHandler.post { onRmsChanged(0f) }
    }

    fun release() {
        stop()
        try {
            vad?.release()
        } catch (_: Exception) {}
        vad = null
        try {
            recognizer?.release()
        } catch (_: Exception) {}
        recognizer = null
    }
}
