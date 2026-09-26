package com.jev.assistant.data

data class AppConfig(
    val jevApiKey: String = "",
    val defaultRoom: String = "客厅",
    // 默认必须是观察模式：接入真实米家后，LIVE 意味着真的会开关用户家里的电器。
    // 与 LocalStorage.isLiveMode() 的默认值保持一致，避免两处不一致导致误控。
    val isLiveMode: Boolean = false,
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

    /** 批量下发中部分设备成功、部分失败。 */
    PARTIAL,

    /** 观察模式：已生成下发计划但未真正下发，无任何副作用。 */
    PLANNED,

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

/**
 * 设备能力的映射状态，决定该设备是否允许真实下发。
 *
 * 语义与 docs/device-control-contract.md 一致：只有 [VERIFIED] 与 [MAPPED]
 * 允许在 LIVE 模式下写属性，[DRAFT] 一律拒绝。
 */
enum class MappingStatus {
    /** 未能解析设备规范（缺 URN 或规范不可得），能力未知，禁止下发。 */
    DRAFT,

    /** 已解析出能力，可用于读取与观察模式下的计划展示。 */
    MAPPED,

    /** 已通过一次真实属性回读确认能力存在，允许 LIVE 写入。 */
    VERIFIED,

    /** 用户禁用，或设备类别本身不可控（传感器、网关等）。 */
    DISABLED
}

data class CapabilityValue(
    val value: Int,
    val label: String,
    val comment: String? = null
)

data class DeviceCapability(
    val key: String,
    val label: String,
    val kind: String = "write",
    /** boolean | number | enum | string。旧目录 JSON 里该字段被 Gson 丢弃，现在真正承载。 */
    val type: String = "string",
    val min: Number? = null,
    val max: Number? = null,
    val step: Number? = null,
    val unit: String? = null,
    val valueList: List<CapabilityValue> = emptyList(),
    /** 执行时需要：MIoT 服务号与属性号。 */
    val siid: Int = 0,
    val piid: Int = 0,
    /** 规范中的原始英文名，如 `on` / `brightness` / `color-temperature`。 */
    val specTypeName: String = "",
    val value: Any? = null
) {
    val isWritable: Boolean get() = kind == "write"

    /** 供 Jev 判断参数是否合法的紧凑描述，如 `brightness(亮度,1-100%)`。 */
    fun describeForPrompt(): String = when {
        type == "boolean" -> "$key($label,开/关)"
        valueList.isNotEmpty() -> "$key($label: ${valueList.joinToString("/") { it.label }})"
        min != null && max != null -> {
            val unitText = unit?.let { u -> unitSuffix(u) } ?: ""
            val stepText = if (step != null && step.toDouble() != 1.0) " 步长${trimNumber(step.toDouble())}" else ""
            "$key($label,${trimNumber(min.toDouble())}-${trimNumber(max.toDouble())}$unitText$stepText)"
        }
        else -> "$key($label)"
    }

    private companion object {
        fun unitSuffix(unit: String): String = when (unit) {
            "percentage" -> "%"
            "celsius" -> "℃"
            "kelvin" -> "K"
            else -> unit
        }

        fun trimNumber(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }
}

/**
 * 一台设备。逻辑 ID 由适配器生成，`sourceDeviceId` 保存米家的真实 did。
 */
data class DeviceItem(
    /** 逻辑 ID，形如 `miot_2026857030`。与物理 did 不同，见设备协议文档。 */
    val logicalId: String,
    /** 米家真实设备 ID；未接入时为 null。 */
    val sourceDeviceId: String? = null,
    val name: String,
    /** 去掉房间名前缀后的短名，便于口语匹配。 */
    val shortName: String = "",
    val room: String,
    /** 米家真实房间 ID。 */
    val roomId: String = "",
    /** 中文类型，如「灯」「空调」，由 URN 类别推导。 */
    val type: String,
    val icon: String = "💡",
    val capabilities: List<String> = emptyList(),
    val rawCapabilities: List<DeviceCapability> = emptyList(),
    var mappingStatus: MappingStatus = MappingStatus.DRAFT,
    var currentState: MutableMap<String, Any> = mutableMapOf(),
    /** URN 的类别段，如 `light`；用于可靠分组，不依赖中文串匹配。 */
    val profile: String = "",
    val urn: String = "",
    val model: String = "",
    val homeId: String = "",
    val homeName: String = "",
    /** 设备是否被明确归入某个房间。未归入时 room 会等于家庭名。 */
    val roomAssigned: Boolean = true,
    val isOnline: Boolean = true,
    /** 子设备所属的父设备 did。 */
    val parentId: String? = null,
    /** 子设备通道号，如 `s1`。 */
    val subDeviceKey: String? = null,
    /** 最近一次读取属性的时间戳。 */
    val stateReadAt: Long = 0L
) {
    /**
     * 该设备是否具备被写入的条件。
     *
     * 两个条件都要满足：能力映射已完成（不是 DRAFT），且**确实存在可写属性**。
     * 只检查映射状态会放过"只读设备"，让它白跑一次注定失败的网络请求。
     */
    val isWritable: Boolean
        get() = (mappingStatus == MappingStatus.VERIFIED || mappingStatus == MappingStatus.MAPPED) &&
            rawCapabilities.any { it.isWritable }

    fun capabilityOf(key: String): DeviceCapability? = rawCapabilities.firstOrNull { it.key == key }
}
