package com.jev.assistant.device

import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.ExecutionPlan
import com.jev.assistant.data.RunEvent
import com.jev.assistant.data.RunStatus
import kotlinx.coroutines.delay

/**
 * 家居控制的执行层。
 *
 * ## 关于观察模式
 *
 * 这里是**唯一**决定"是否产生真实副作用"的地方。[executeHomeControl] 的 `isLive`
 * 参数是硬门控：为 false 时绝不调用适配器的写接口，只做预演并返回 [RunStatus.PLANNED]。
 *
 * 这条约束不能下移到适配器里——门控只有一处，才便于审查是否真的没有旁路。
 *
 * ## 关于批量控制
 *
 * 逐台串行下发并节流，不并发：真实设备下并发写既可能触发云端限流，
 * 又会让失败时的状态难以归因。每台设备的结果单独记录，状态如实汇总为
 * 成功 / 部分成功 / 失败，不做"全都算成功"的乐观汇报。
 */
class HomeExecutor(
    private val registry: DeviceRegistry,
    /** 设备适配器；未绑定账号时为 null。 */
    private val adapterProvider: () -> DeviceAdapter? = { null },
) {

    suspend fun executeHomeControl(plan: ExecutionPlan, isLive: Boolean): ExecutionPlan {
        plan.status = RunStatus.VALIDATING

        val adapter = adapterProvider()
            ?: return block(plan, "尚未绑定米家账号，无法控制设备")

        val targets = resolveTargets(plan)
        if (targets.isEmpty()) {
            return block(plan, "未能确定要控制的设备，本次未执行")
        }
        if (targets.size < requestedCount(plan)) {
            plan.events.add(
                RunEvent(
                    stage = "目标解析",
                    message = "部分目标设备不在当前家庭中，已跳过 ${requestedCount(plan) - targets.size} 台",
                    isSuccess = false,
                ),
            )
        }

        plan.events.add(
            RunEvent(stage = "能力校验", message = "校验 ${targets.size} 台设备的能力与参数合规性"),
        )

        // ---- 安全门控：观察模式到此为止，绝不产生副作用 ----
        if (!isLive) {
            return observe(plan, adapter, targets)
        }

        // LIVE 模式：未完成能力映射的设备不允许下发
        val writable = targets.filter { it.isWritable }
        if (writable.isEmpty()) {
            return block(plan, "目标设备尚未完成能力映射，已按观察模式生成计划，未下发")
        }
        val skipped = targets - writable.toSet()
        if (skipped.isNotEmpty()) {
            plan.events.add(
                RunEvent(
                    stage = "能力校验",
                    message = "跳过 ${skipped.size} 台能力未知的设备：${skipped.joinToString("、") { it.name }}",
                    isSuccess = false,
                ),
            )
        }

        return executeTargets(plan, adapter, writable)
    }

    // ---------------- 观察模式 ----------------

    /**
     * 只做预演：把将要下发的属性与取值展示出来，让用户确认理解是否正确。
     */
    private suspend fun observe(
        plan: ExecutionPlan,
        adapter: DeviceAdapter,
        targets: List<DeviceItem>,
    ): ExecutionPlan {
        plan.events.add(RunEvent(stage = "观察模式", message = "未启用真实执行，以下为将要下发的计划"))

        var previewed = 0
        targets.forEach { device ->
            val preview = adapter.preview(device.logicalId, plan.action, plan.parameters)
            val text = preview.getOrElse { "无法预演：${it.message}" }
            if (preview.isSuccess) previewed++

            plan.events.add(
                RunEvent(
                    stage = "下发计划",
                    message = "「${device.name}」$text",
                    isSuccess = preview.isSuccess,
                ),
            )
        }

        plan.status = RunStatus.PLANNED
        plan.statusMessage = if (previewed == targets.size) {
            "观察模式：已生成 ${targets.size} 台设备的下发计划，未调用米家云"
        } else {
            "观察模式：${targets.size} 台设备中 $previewed 台可生成计划，其余不可执行"
        }
        return plan
    }

    // ---------------- 真实执行 ----------------

    private suspend fun executeTargets(
        plan: ExecutionPlan,
        adapter: DeviceAdapter,
        targets: List<DeviceItem>,
    ): ExecutionPlan {
        plan.status = RunStatus.EXECUTING

        val batch = targets.take(MAX_BATCH)
        val truncated = targets.size - batch.size
        if (truncated > 0) {
            // 明确告知被截断，避免"看起来全做了"
            plan.events.add(
                RunEvent(
                    stage = "批量下发",
                    message = "单次下发上限 $MAX_BATCH 台，本次仅执行 $MAX_BATCH 台，其余 $truncated 台未下发",
                    isSuccess = false,
                ),
            )
        }

        var succeeded = 0
        val failures = mutableListOf<String>()

        batch.forEachIndexed { index, device ->
            if (index > 0) delay(BATCH_THROTTLE_MS)

            val result = adapter.execute(device.logicalId, plan.action, plan.parameters)
                .getOrElse { error ->
                    DeviceActionResult.failure(
                        DeviceErrorKind.UNKNOWN, 0, error.message ?: "执行失败",
                    )
                }

            if (result.success) {
                succeeded++
                // 用回读到的真实状态更新本地视图
                if (result.readBack.isNotEmpty()) {
                    registry.applyPropertyUpdate(device.logicalId, result.readBack)
                }
                plan.events.add(
                    RunEvent(stage = "已下发", message = "「${device.name}」${describeReadBack(result.readBack)}"),
                )
            } else {
                failures += device.name
                plan.events.add(
                    RunEvent(
                        stage = "下发失败",
                        message = "「${device.name}」${result.message}",
                        isSuccess = false,
                    ),
                )
            }
        }

        plan.status = when {
            // 有设备因超出上限而未下发时，即便已下发的全部成功也不能报整体成功
            truncated > 0 -> RunStatus.PARTIAL
            succeeded == batch.size -> RunStatus.SUCCEEDED
            succeeded > 0 -> RunStatus.PARTIAL
            else -> RunStatus.FAILED
        }
        plan.statusMessage = buildSummary(plan, batch.size, succeeded, failures) +
            if (truncated > 0) "，另有 $truncated 台未下发" else ""
        return plan
    }

    private fun buildSummary(
        plan: ExecutionPlan,
        total: Int,
        succeeded: Int,
        failures: List<String>,
    ): String {
        val actionName = describeAction(plan.action, plan.parameters)
        return when {
            failures.isEmpty() -> "已${actionName}（共 $total 台）"
            succeeded == 0 -> "${actionName}失败：${failures.joinToString("、")}"
            else -> "部分完成：$succeeded/$total 台已${actionName}，${failures.joinToString("、")}失败"
        }
    }

    private fun describeReadBack(readBack: Map<String, Any>): String {
        if (readBack.isEmpty()) return "已下发"
        return readBack.entries.joinToString("，") { (key, value) ->
            "${labelOf(key)}=${formatValue(value)}"
        }
    }

    // ---------------- 目标解析 ----------------

    private fun requestedCount(plan: ExecutionPlan): Int {
        @Suppress("UNCHECKED_CAST")
        val ids = plan.parameters["device_ids"] as? List<String>
        return ids?.size ?: 1
    }

    /**
     * 解析本次要控制的设备。
     *
     * 组控制用 `device_ids`，单设备用 `targetDeviceId`。找不到的设备不静默丢弃，
     * 由调用方对比数量后如实报告。
     */
    private fun resolveTargets(plan: ExecutionPlan): List<DeviceItem> {
        @Suppress("UNCHECKED_CAST")
        val groupIds = plan.parameters["device_ids"] as? List<String>
        val isGroup = plan.parameters["is_group"] == true ||
            plan.targetDeviceId.startsWith("group_") ||
            groupIds != null

        val ids = if (isGroup) groupIds.orEmpty() else listOf(plan.targetDeviceId)
        return ids.mapNotNull { registry.findDeviceById(it) }
    }

    // ---------------- 文案 ----------------

    private fun describeAction(action: String, params: Map<String, Any>): String = when (action) {
        "set_power_on" -> "开启"
        "set_power_off" -> "关闭"
        "set_brightness" -> "调至亮度 ${numberParam(params, "percent")}%"
        "set_temperature" -> "调至 ${numberParam(params, "celsius")}℃"
        "set_color_temperature" -> "调至色温 ${numberParam(params, "kelvin")}K"
        "open" -> "打开"
        "close" -> "关闭"
        "stop" -> "停止"
        else -> "执行"
    }

    private fun numberParam(params: Map<String, Any>, key: String): String {
        val value = (params[key] as? Number)?.toDouble() ?: return "?"
        return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    private fun formatValue(value: Any): String = when (value) {
        is Boolean -> if (value) "开" else "关"
        is Double -> if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
        else -> value.toString()
    }

    private fun labelOf(key: String): String = when (key) {
        "power" -> "开关"
        "brightness" -> "亮度"
        "color_temperature" -> "色温"
        "target_temperature" -> "温度"
        "position" -> "开合度"
        "motion" -> "动作"
        "mode" -> "模式"
        else -> key
    }

    private fun block(plan: ExecutionPlan, reason: String): ExecutionPlan {
        plan.status = RunStatus.FAILED
        plan.statusMessage = reason
        plan.events.add(RunEvent(stage = "未执行", message = reason, isSuccess = false))
        return plan
    }

    private companion object {
        /** 单次批量下发的设备数上限，防止误伤与触发云端限流。 */
        const val MAX_BATCH = 30

        /** 批量下发时每台之间的间隔，属于限流而非模拟时延。 */
        const val BATCH_THROTTLE_MS = 120L
    }
}
