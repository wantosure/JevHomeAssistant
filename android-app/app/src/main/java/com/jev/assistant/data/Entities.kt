package com.jev.assistant.data

data class AppConfig(
    val jevApiKey: String = "",
    val defaultRoom: String = "客厅",
    val isLiveMode: Boolean = true,
    val silenceThresholdMs: Long = 700L,
    val confidenceThreshold: Float = 0.35f,
    val pcAgentUrl: String = "http://192.168.1.100:8765",
    val pcAgentToken: String = "dev-secret-token"
)

enum class RunStatus {
    ACCEPTED,
    DECIDING,
    VALIDATING,
    EXECUTING,
    SUCCEEDED,
    PARTIAL,
    FAILED,
    UNKNOWN,
    IGNORED
}

enum class IntentType {
    HOME_CONTROL,
    ALARM,
    COMPUTER_CONTROL,
    DAILY_CHAT,
    OTHER
}

data class RunEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val stage: String,
    val message: String,
    val isSuccess: Boolean = true
)

data class ExecutionPlan(
    val runId: String,
    val utteranceText: String,
    val intent: IntentType,
    val confidence: Float,
    var targetDeviceName: String,
    var targetDeviceId: String,
    var action: String,
    var parameters: Map<String, Any> = emptyMap(),
    var room: String,
    var status: RunStatus = RunStatus.ACCEPTED,
    var statusMessage: String = "",
    val events: MutableList<RunEvent> = java.util.concurrent.CopyOnWriteArrayList(),
    val isLive: Boolean = true,
    var cost: Double = 0.0
)

data class SilentLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String,
    val intent: IntentType,
    val confidence: Float,
    val reason: String,
    val isIgnored: Boolean = true,
    val cost: Double = 0.0
)

data class UtteranceHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString().take(6),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String, // "21:15:03"
    val text: String,
    val room: String = "",
    val statusText: String = "",
    val isSuccess: Boolean = true,
    val cost: Double = 0.0
)

data class AlarmRecord(
    val id: String,
    val timeFormatted: String, // "07:30"
    val timestamp: Long,
    val label: String,
    val isDaily: Boolean,
    var isEnabled: Boolean = true
)

enum class MappingStatus {
    SIMULATED_ACTIVE,
    DRAFT,
    DISABLED
}

data class DeviceCapability(
    val key: String,
    val label: String,
    val kind: String = "write",
    val min: Number? = null,
    val max: Number? = null,
    val step: Number? = null,
    val unit: String? = null,
    val value: Any? = null
)

data class DeviceItem(
    val logicalId: String,            // 例如 MJ-001
    val sourceDeviceId: String? = null,
    val name: String,                 // 例如 主卧主灯
    val shortName: String = "",       // 例如 主灯
    val room: String,                 // 例如 主卧
    val roomId: String = "",          // 例如 master
    val type: String,                 // 例如 灯具
    val icon: String = "💡",
    val capabilities: List<String> = emptyList(),
    val rawCapabilities: List<DeviceCapability> = emptyList(),
    var mappingStatus: MappingStatus = MappingStatus.SIMULATED_ACTIVE,
    var currentState: MutableMap<String, Any> = mutableMapOf()
)

data class MijiaCatalogJson(
    val schemaVersion: Int = 1,
    val simulated: Boolean = true,
    val rooms: List<MijiaRoomJson> = emptyList(),
    val devices: List<MijiaDeviceJson> = emptyList()
)

data class MijiaRoomJson(
    val id: String,
    val name: String,
    val area: Int = 0
)

data class MijiaDeviceJson(
    val id: String,
    val name: String,
    val shortName: String = "",
    val room: String,
    val roomId: String,
    val profile: String? = null,
    val type: String,
    val icon: String? = null,
    val capabilities: List<DeviceCapability> = emptyList(),
    val controllable: Boolean = true
)
