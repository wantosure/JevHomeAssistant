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

    /** 米家云设备适配器。 */
    private val miotAdapter = com.jev.assistant.miot.adapter.MiotDeviceAdapter(
        cloud = app.miotCloudClient,
        repository = app.miotDeviceRepository,
        deviceProvider = { registry.findDeviceById(it) },
        onStateVerified = { registry.markVerified(it) },
    )

    /**
     * 执行器只在**已绑定**时拿到适配器。
     *
     * 未绑定时返回 null，执行层会明确报"尚未绑定米家账号"；
     * 若把适配器一直传下去，失败会发生在网络层，提示反而不清楚。
     */
    private val homeExecutor = HomeExecutor(registry) {
        if (app.miotAccountManager.currentAccessToken() != null) miotAdapter else null
    }

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

    // ---------------- 米家接入状态 ----------------

    val miotBindStatus = app.miotAccountManager.status
    val homes: StateFlow<List<com.jev.assistant.miot.MiotHome>> = registry.homesFlow
    val activeHomeId: StateFlow<String?> = registry.activeHomeIdFlow
    val adapterStatus = registry.adapterStatusFlow

    /** 当前家庭的房间列表（首项为「全屋」）。 */
    val rooms: StateFlow<List<String>> = registry.roomsFlow

    /** 是否已绑定小米账号。 */
    val isMijiaBound: Boolean get() = app.miotAccountManager.currentAccessToken() != null

    /** 生成米家授权页链接，供浏览器打开。 */
    fun buildMijiaAuthUrl(): String = app.miotAccountManager.buildAuthUrl()

    /**
     * 用授权回执完成绑定。成功后自动同步一次设备。
     *
     * 注意绑定流程内部会把模式重置为观察模式（见 MiotAccountManager），
     * 界面上也会同步反映。
     */
    fun bindMijia(rawPayload: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            val result = app.miotAccountManager.bind(rawPayload)
            _isLiveMode.value = storage.isLiveMode()
            result.fold(
                onSuccess = {
                    syncMijiaDevices()
                    onResult(true, null)
                },
                onFailure = { onResult(false, it.message ?: "绑定失败") },
            )
        }
    }

    fun unbindMijia() {
        viewModelScope.launch(exceptionHandler) {
            app.miotAccountManager.unbind()
            registry.clear()
            registry.updateAdapterStatus(com.jev.assistant.device.AdapterStatus.Unbound)
            _isLiveMode.value = storage.isLiveMode()
        }
    }

    /**
     * 同步设备。
     *
     * 先尝试在线同步，失败时回落到本地快照，这样断网也能看到设备（只是状态可能陈旧）。
     */
    fun syncMijiaDevices() {
        viewModelScope.launch(exceptionHandler) {
            if (app.miotAccountManager.currentAccessToken() == null) {
                registry.updateAdapterStatus(com.jev.assistant.device.AdapterStatus.Unbound)
                registry.clear()
                return@launch
            }

            registry.updateAdapterStatus(com.jev.assistant.device.AdapterStatus.Syncing)
            val result = app.miotDeviceRepository.sync()

            result.fold(
                onSuccess = { applySync(it) },
                onFailure = { error ->
                    val cached = app.miotDeviceRepository.loadCached()
                    if (cached != null) {
                        applySync(cached)
                        registry.updateAdapterStatus(
                            com.jev.assistant.device.AdapterStatus.Failed(
                                "同步失败，当前展示的是本地缓存：${error.message}",
                            ),
                        )
                    } else {
                        registry.updateAdapterStatus(
                            com.jev.assistant.device.AdapterStatus.Failed(
                                error.message ?: "同步设备失败",
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun applySync(result: com.jev.assistant.miot.MiotSyncResult) {
        registry.applySyncResult(
            devices = result.devices,
            homes = result.homes,
            preferredHomeId = _activeHomeId.value,
        )
        registry.updateAdapterStatus(
            com.jev.assistant.device.AdapterStatus.Ready(
                deviceCount = result.devices.size,
                onlineCount = result.devices.count { it.isOnline },
                homeCount = result.homes.size,
                syncedAt = result.syncedAt,
            ),
        )
        _activeHomeId.value = registry.activeHomeIdFlow.value
    }

    private val _activeHomeId = MutableStateFlow<String?>(null)

    fun setActiveHome(homeId: String) {
        registry.setActiveHome(homeId)
        _activeHomeId.value = registry.activeHomeIdFlow.value
    }

    /**
     * 启动时恢复设备：先用本地快照立即上屏，再在后台在线同步刷新。
     *
     * 这样已绑定的用户在冷启动或断网时也能看到设备列表，而不是一片空白。
     */
    init {
        viewModelScope.launch(exceptionHandler) {
            if (app.miotAccountManager.currentAccessToken() == null) {
                registry.updateAdapterStatus(com.jev.assistant.device.AdapterStatus.Unbound)
                return@launch
            }
            app.miotDeviceRepository.loadCached()?.let { applySync(it) }
            syncMijiaDevices()
        }
    }

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

    /**
     * 设备卡片上的手动开关。
     *
     * 必须走执行器而不是直接改本地状态：执行器是唯一的副作用门控点，
     * 绕过去就等于给观察模式开了一条旁路，直接操作真实设备。
     */
    fun toggleDevicePower(deviceId: String, currentPower: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            val device = registry.findDeviceById(deviceId) ?: return@launch
            val plan = ExecutionPlan(
                runId = java.util.UUID.randomUUID().toString().take(8),
                utteranceText = "手动${if (currentPower) "关闭" else "开启"}${device.name}",
                intent = com.jev.assistant.data.IntentType.HOME_CONTROL,
                confidence = 1.0f,
                targetDeviceName = device.name,
                targetDeviceId = deviceId,
                action = if (currentPower) "set_power_off" else "set_power_on",
                room = device.room,
                isLive = storage.isLiveMode(),
            )
            homeExecutor.executeHomeControl(plan, storage.isLiveMode())
        }
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

    fun resetToBuiltinKey() {
        storage.clearCustomApiKey()
    }

    fun getApiKey(): String = storage.getApiKey()
    fun getMaskedApiKeyForDisplay(): String = storage.getMaskedApiKeyForDisplay()
    fun isUsingCustomKey(): Boolean = storage.isUsingCustomKey()

    override fun onCleared() {
        super.onCleared()
        audioPipeline.stopListening()
    }
}
