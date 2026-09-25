package com.jev.assistant.audio

import kotlin.math.sqrt

class VadProcessor(
    private val sampleRate: Int = 16000,
    private val silenceThresholdMs: Long = 700L,
    private val maxUtteranceDurationMs: Long = 20000L,
    private val energyThreshold: Double = 1200.0,
    private val onVoiceStart: () -> Unit,
    private val onVoiceEnd: () -> Unit,
    private val onRmsUpdate: (Float) -> Unit
) {
    private var isSpeaking = false
    private var lastSpeechTime = 0L
    private var utteranceStartTime = 0L

    fun processFrame(buffer: ShortArray, readSize: Int) {
        if (readSize <= 0) return

        // 计算 RMS 能量
        var sumSquares = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / readSize)
        val normalizedRms = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        onRmsUpdate(normalizedRms)

        val now = System.currentTimeMillis()
        val hasVoice = rms > energyThreshold

        if (hasVoice) {
            lastSpeechTime = now
            if (!isSpeaking) {
                isSpeaking = true
                utteranceStartTime = now
                onVoiceStart()
            }
        } else {
            if (isSpeaking) {
                val silenceDuration = now - lastSpeechTime
                val totalDuration = now - utteranceStartTime
                if (silenceDuration >= silenceThresholdMs || totalDuration >= maxUtteranceDurationMs) {
                    isSpeaking = false
                    onVoiceEnd()
                }
            }
        }
    }

    fun reset() {
        isSpeaking = false
        lastSpeechTime = 0L
        utteranceStartTime = 0L
    }
}
