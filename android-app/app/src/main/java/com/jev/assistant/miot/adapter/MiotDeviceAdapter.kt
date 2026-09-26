package com.jev.assistant.miot.adapter

import com.jev.assistant.data.DeviceItem
import com.jev.assistant.device.DeviceAdapter
import com.jev.assistant.device.DeviceActionResult
import com.jev.assistant.device.DeviceErrorKind
import com.jev.assistant.miot.MiotActionTranslator
import com.jev.assistant.miot.MiotCloudClient
import com.jev.assistant.miot.MiotDeviceRepository
import com.jev.assistant.miot.MiotErrorMapper
import com.jev.assistant.miot.MiotPropQuery
import com.jev.assistant.miot.MiotPropWrite

/**
 * 米家云设备适配器。
 *
 * 实现 [DeviceAdapter] 契约，把本项目的动作词表翻译成米家属性写入，
 * 并在写入后回读真实状态。
 *
 * 注意本类**不做**观察模式的门控——是否允许产生副作用由执行层（`HomeExecutor`）决定，
 * 适配器只负责"如果被要求写，就正确地写"。这样门控逻辑只有一处，便于审查。
 */
class MiotDeviceAdapter(
    private val cloud: MiotCloudClient,
    private val repository: MiotDeviceRepository,
    /** 按逻辑 ID 取设备；由注册表提供，避免适配器持有全量设备状态。 */
    private val deviceProvider: (String) -> DeviceItem?,
    /** 成功回读到真实属性时回调，用于把设备从 MAPPED 提升为 VERIFIED。 */
    private val onStateVerified: (String) -> Unit = {},
) : DeviceAdapter {

    override val adapterId: String = "miot"

    override val displayName: String = "米家云"

    override suspend fun listDevices(): Result<List<DeviceItem>> =
        repository.sync().map { it.devices }

    override suspend fun readState(logicalId: String, keys: List<String>): Result<Map<String, Any>> {
        val device = deviceProvider(logicalId)
            ?: return Result.failure(IllegalStateException("设备不存在：$logicalId"))
        val did = device.sourceDeviceId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("设备缺少物理标识：${device.name}"))

        val targets = device.rawCapabilities.filter { capability ->
            capability.siid != 0 && (keys.isEmpty() || capability.key in keys)
        }
        if (targets.isEmpty()) return Result.success(emptyMap())

        val queries = targets.map { MiotPropQuery(did = did, siid = it.siid, piid = it.piid) }
        return cloud.getProps(queries).map { values ->
            // 回读成功即证明能力真实存在，可提升映射可信度
            onStateVerified(logicalId)

            val byIid = values.associateBy { it.siid to it.piid }
            targets.mapNotNull { capability ->
                val value = byIid[capability.siid to capability.piid]
                if (value == null || !MiotErrorMapper.isSuccess(value.code)) {
                    null
                } else {
                    capability.key to (value.value ?: return@mapNotNull null)
                }
            }.toMap()
        }
    }

    /**
     * 预演：走与真实下发完全相同的翻译与校验，只是不发出请求。
     * 因此预演能成功，说明真实下发也具备执行条件。
     */
    override suspend fun preview(logicalId: String, action: String, params: Map<String, Any>): Result<String> {
        val device = deviceProvider(logicalId)
            ?: return Result.failure(IllegalStateException("设备不存在：$logicalId"))

        return when (val outcome = MiotActionTranslator.translate(device, action, params)) {
            is MiotActionTranslator.Outcome.Rejected -> Result.failure(
                IllegalStateException(outcome.reason),
            )

            is MiotActionTranslator.Outcome.Ok -> Result.success(
                outcome.writes.joinToString("；") { write ->
                    "${write.key}=${formatValue(write.value)} → prop.${write.siid}.${write.piid}"
                },
            )
        }
    }

    private fun formatValue(value: Any): String = when (value) {
        is Boolean -> if (value) "开" else "关"
        else -> value.toString()
    }

    override suspend fun execute(
        logicalId: String,
        action: String,
        params: Map<String, Any>,
    ): Result<DeviceActionResult> {
        val device = deviceProvider(logicalId)
            ?: return Result.success(
                DeviceActionResult.failure(DeviceErrorKind.NOT_FOUND, 0, "设备不存在，可能需要重新同步"),
            )
        val did = device.sourceDeviceId?.takeIf { it.isNotBlank() }
            ?: return Result.success(
                DeviceActionResult.failure(DeviceErrorKind.NOT_FOUND, 0, "「${device.name}」尚未接入，无法控制"),
            )

        // 动作与参数在这里被逐项校验；不合法就直接拒绝，不发请求
        when (val outcome = MiotActionTranslator.translate(device, action, params)) {
            is MiotActionTranslator.Outcome.Rejected ->
                return Result.success(
                    DeviceActionResult.failure(outcome.kind, 0, outcome.reason),
                )

            is MiotActionTranslator.Outcome.Ok -> {
                if (outcome.writes.isEmpty()) {
                    return Result.success(DeviceActionResult.failure(
                        DeviceErrorKind.PROP_MISSING, 0, "没有可下发的属性",
                    ))
                }

                val writes = outcome.writes.map {
                    MiotPropWrite(did = did, siid = it.siid, piid = it.piid, value = it.value)
                }

                return cloud.setProps(writes).map { results ->
                    val failed = results.firstOrNull { !MiotErrorMapper.isSuccess(it.code) }
                    if (failed != null) {
                        val mapped = MiotErrorMapper.map(failed.code)
                        DeviceActionResult.failure(mapped.kind, failed.code, mapped.message)
                    } else {
                        DeviceActionResult.ok(
                            readBack = readBackAfterWrite(logicalId, outcome.writes.map { it.key }),
                        )
                    }
                }
            }
        }
    }

    /**
     * 写入后回读，让执行卡片展示的是**真实状态**而不是"我们刚才发了什么"。
     *
     * 回读失败不影响本次下发成功的判定——写入已经生效，回读只是补充信息。
     */
    private suspend fun readBackAfterWrite(logicalId: String, keys: List<String>): Map<String, Any> =
        readState(logicalId, keys).getOrDefault(emptyMap())
}
