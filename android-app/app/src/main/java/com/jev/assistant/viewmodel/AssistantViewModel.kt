package com.jev.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jev.assistant.JevApp
import com.jev.assistant.alarm.AlarmScheduler
import com.jev.assistant.audio.AudioPipeline
import com.jev.assistant.data.AlarmRecord
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.ExecutionPlan
import com.jev.assistant.data.SilentLog
import com.jev.assistant.device.HomeExecutor
import com.jev.assistant.jev.DecisionEngine
import com.jev.assistant.jev.JevClient
import com.jev.assistant.pc.PcAgentClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel : ViewModel() {

    private val app = JevApp.instance
    private val storage = app.storage
    private val registry = app.deviceRegistry

    private val jevClient = JevClient(
        getApiKey = { storage.getApiKey() }
    )

    private val homeExecutor = HomeExecutor(registry)
    private val alarmScheduler = AlarmScheduler(app)
    private val pcClient = PcAgentClient(
        getAgentUrl = { storage.getPcAgentUrl() },
        getAgentToken = { storage.getPcAgentToken() }
    )

    private val decisionEngine = DecisionEngine(
        jevClient = jevClient,
        storage = storage,
        registry = registry,
        homeExecutor = homeExecutor,
        alarmScheduler = alarmScheduler,
        pcClient = pcClient
    )

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("AssistantViewModel", "协程链路发生未捕获异常: ${throwable.message}", throwable)
        com.jev.assistant.utils.CrashReporter.recordCrash(app, Thread.currentThread(), throwable)
        refreshCrashLog()
    }

    val audioPipeline = AudioPipeline(
        context = app,
        deviceRegistry = registry,
        onUtteranceFinal = { finalUtterance ->
            viewModelScope.launch(exceptionHandler) {
                try {
                    decisionEngine.processFinalUtterance(finalUtterance)
                } catch (t: Throwable) {
                    android.util.Log.e("AssistantViewModel", "处理用户话语失败: ${t.message}", t)
                    com.jev.assistant.utils.CrashReporter.recordCrash(app, Thread.currentThread(), t)
                    refreshCrashLog()
                }
            }
        }
    )

    val asrEngineName = audioPipeline.currentEngineName
    val isListening = audioPipeline.isListening
    val currentRms = audioPipeline.currentRms
    val streamingSubtitle = audioPipeline.currentSubtitle
    val activePlan = decisionEngine.activePlanFlow
    val isDeciding = decisionEngine.isDeciding

    // 崩溃日志状态
    private val _crashLogFlow = MutableStateFlow(com.jev.assistant.utils.CrashReporter.getCrashLog(app))
    val crashLogFlow: StateFlow<String?> = _crashLogFlow.asStateFlow()

    fun refreshCrashLog() {
        _crashLogFlow.value = com.jev.assistant.utils.CrashReporter.getCrashLog(app)
    }

    fun clearCrashLog() {
        com.jev.assistant.utils.CrashReporter.clearCrashLog(app)
        _crashLogFlow.value = null
    }

    fun copyCrashLog(context: android.content.Context): Boolean {
        val log = _crashLogFlow.value ?: return false
        return com.jev.assistant.utils.CrashReporter.copyToClipboard(context, log)
    }

    fun copyDiagnosticReport(context: android.content.Context): Boolean {
        val report = com.jev.assistant.utils.CrashReporter.generateDiagnosticReport(context, asrEngineName.value)
        return com.jev.assistant.utils.CrashReporter.copyToClipboard(context, report)
    }

    fun switchAsrEngine(useSenseVoice: Boolean) {
        audioPipeline.switchEngine(useSenseVoice)
    }

    val silentLogs: StateFlow<List<SilentLog>> = storage.logsFlow
    val devices: StateFlow<List<DeviceItem>> = registry.devicesFlow
    val alarms: StateFlow<List<AlarmRecord>> = storage.alarmsFlow
    val historyList: StateFlow<List<com.jev.assistant.data.UtteranceHistoryItem>> = storage.historyFlow
    val totalJevCost: StateFlow<Double> = storage.totalCostFlow
    val totalJevCalls: StateFlow<Int> = storage.totalCallsFlow

    fun clearHistory() {
        storage.clearHistory()
    }

    fun resetJevCost() {
        storage.resetJevCost()
    }

    private val _defaultRoom = MutableStateFlow(storage.getDefaultRoom())
    val defaultRoom: StateFlow<String> = _defaultRoom.asStateFlow()

    private val _isLiveMode = MutableStateFlow(storage.isLiveMode())
    val isLiveMode: StateFlow<Boolean> = _isLiveMode.asStateFlow()

    fun toggleListening() {
        if (isListening.value) {
            audioPipeline.stopListening()
        } else {
            audioPipeline.startListening()
        }
    }

    fun setDefaultRoom(room: String) {
        storage.setDefaultRoom(room)
        _defaultRoom.value = room
    }

    fun toggleLiveMode() {
        val newMode = !_isLiveMode.value
        storage.setLiveMode(newMode)
        _isLiveMode.value = newMode
    }

    fun toggleDevicePower(deviceId: String, currentPower: Boolean) {
        val newPower = if (currentPower) 0 else 1
        registry.updateDeviceProperty(deviceId, "power", newPower)
    }

    fun simulateUtterance(text: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                audioPipeline.updateStreamingSubtitle(text)
                decisionEngine.processFinalUtterance(text)
            } catch (t: Throwable) {
                android.util.Log.e("AssistantViewModel", "模拟话语异常: ${t.message}", t)
                com.jev.assistant.utils.CrashReporter.recordCrash(app, Thread.currentThread(), t)
                refreshCrashLog()
            }
        }
    }

    fun removeAlarm(id: String) {
        alarmScheduler.cancelAlarm(id)
    }

    fun saveApiKey(key: String) {
        storage.setApiKey(key)
    }

    fun getApiKey(): String = storage.getApiKey()

    override fun onCleared() {
        super.onCleared()
        audioPipeline.stopListening()
    }
}
