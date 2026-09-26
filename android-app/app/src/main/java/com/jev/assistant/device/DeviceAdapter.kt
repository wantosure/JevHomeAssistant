package com.jev.assistant.device

import com.jev.assistant.data.DeviceItem

/**
 * 设备来源适配器契约。
 *
 * 这是 docs/device-control-contract.md 所述「适配器处理顺序」的代码落地：
 * 找到设备 → 验证能力与参数 → 检查实际映射 → 转换 → 下发 → 回读。
 *
 * 抽象出这层接口的意义在于：决策链路（`DecisionEngine`）与执行链路（`HomeExecutor`）
 * 只依赖本契约，日后更换设备接入方式时只需替换实现。
 */
interface DeviceAdapter {

    /** 适配器标识，如 `miot`。 */
    val adapterId: String

    /** 展示名，如「米家云」。 */
    val displayName: String

    /**
     * 拉取全部设备。
     *
     * 返回的设备应已带上类型、能力与映射状态；调用方不应再做二次推断。
     */
    suspend fun listDevices(): Result<List<DeviceItem>>

    /**
     * 回读指定设备的属性。
     *
     * @param keys 能力 key 列表；传空表示读取全部可读能力
     */
    suspend fun readState(logicalId: String, keys: List<String>): Result<Map<String, Any>>

    /**
     * 下发一次控制。
     *
     * 实现方必须完成能力校验与实际映射；无映射时返回失败而非假装成功。
     */
    suspend fun execute(
        logicalId: String,
        action: String,
        params: Map<String, Any>,
    ): Result<DeviceActionResult>

    /**
     * 预演一次控制，**不产生任何副作用**。
     *
     * 观察模式下用它展示"本来会下发什么"，让用户能在不为所动的前提下确认理解是否正确。
     * 实现方需完成与 [execute] 相同的校验，因此预演失败即代表真实下发也会失败。
     */
    suspend fun preview(
        logicalId: String,
        action: String,
        params: Map<String, Any>,
    ): Result<String>
}

/**
 * 单台设备的执行结果。
 */
data class DeviceActionResult(
    val success: Boolean,
    val code: Int = 0,
    val message: String = "",
    val kind: DeviceErrorKind = DeviceErrorKind.UNKNOWN,
    /** 下发后回读到的属性，供执行卡片展示真实状态。 */
    val readBack: Map<String, Any> = emptyMap(),
) {
    companion object {
        fun ok(readBack: Map<String, Any> = emptyMap(), message: String = ""): DeviceActionResult =
            DeviceActionResult(success = true, code = 0, message = message, readBack = readBack)

        fun failure(kind: DeviceErrorKind, code: Int, message: String): DeviceActionResult =
            DeviceActionResult(success = false, code = code, message = message, kind = kind)
    }
}

/**
 * 适配器的整体状态，供界面展示与决策短路使用。
 */
sealed interface AdapterStatus {

    /** 尚未完成账号绑定。 */
    data object Unbound : AdapterStatus

    /** 正在同步设备。 */
    data object Syncing : AdapterStatus

    /** 已就绪。 */
    data class Ready(
        val deviceCount: Int,
        val onlineCount: Int,
        val homeCount: Int,
        val syncedAt: Long,
    ) : AdapterStatus

    /** 授权失效，需要重新绑定。 */
    data class AuthExpired(val message: String) : AdapterStatus

    /** 同步失败。 */
    data class Failed(val message: String) : AdapterStatus
}
