package com.jev.assistant.miot

import com.jev.assistant.data.DeviceCapability
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.device.DeviceErrorKind

/**
 * 把本项目的动作词表翻译成米家属性写入。
 *
 * 这是「模型结果不得直接执行」这条约束的落点之一：
 * 动作名、参数范围、枚举取值都在这里被逐项校验，任何一项不合法就整体拒绝，
 * 绝不发一个"差不多"的值出去。
 *
 * 优先选择幂等的「设置为」语义（如 `target-position = 100`），
 * 而非 `motor-control = open`，因为后者依赖设备当前状态，重放不安全。
 */
object MiotActionTranslator {

    /** 一次属性写入。 */
    data class Write(
        val key: String,
        val siid: Int,
        val piid: Int,
        val value: Any,
    )

    sealed interface Outcome {
        data class Ok(val writes: List<Write>) : Outcome

        data class Rejected(val reason: String, val kind: DeviceErrorKind) : Outcome
    }

    /**
     * 翻译一个动作。
     *
     * @param action 本项目动作名，如 `set_power_on`
     * @param params 动作参数，如 `{"percent": 80}`
     */
    fun translate(device: DeviceItem, action: String, params: Map<String, Any>): Outcome = when (action) {
        "set_power_on" -> setBoolean(device, "power", true)
        "set_power_off" -> setBoolean(device, "power", false)
        "set_brightness" -> setNumber(device, "brightness", numberParam(params, "percent"))
        "set_temperature" -> setNumber(device, "target_temperature", numberParam(params, "celsius"))
        "set_color_temperature" -> setNumber(device, "color_temperature", numberParam(params, "kelvin"))
        "set_target_humidity" -> setNumber(device, "target_humidity", numberParam(params, "percent"))

        // 窗帘：开合优先用位置（幂等），只有停止没有幂等的等价表达，才退回枚举动作
        "open" -> openCurtain(device, open = true)
        "close" -> openCurtain(device, open = false)
        "stop" -> enumByLabel(device, "motion", listOf("停", "暂停", "pause", "stop"))

        "set_mode" -> enumByLabel(device, "mode", listOf(params["mode"]?.toString().orEmpty()))

        else -> Outcome.Rejected("暂不支持的动作：$action", DeviceErrorKind.PROP_MISSING)
    }

    // ---------------- 具体动作 ----------------

    private fun setBoolean(device: DeviceItem, key: String, value: Boolean): Outcome {
        val capability = writable(device, key) ?: return missing(device, key)
        return Outcome.Ok(listOf(capability.toWrite(if (value) 1.0 else 0.0, booleanValue = value)))
    }

    private fun setNumber(device: DeviceItem, key: String, value: Double?): Outcome {
        val capability = writable(device, key) ?: return missing(device, key)
        if (value == null) {
            return Outcome.Rejected("${capability.label}缺少数值参数", DeviceErrorKind.BAD_VALUE)
        }
        val prepared = clamp(capability, value)
            ?: return Outcome.Rejected(
                "${capability.label}的取值 ${trim(value)} 超出设备允许范围",
                DeviceErrorKind.BAD_VALUE,
            )
        return Outcome.Ok(listOf(capability.toWrite(prepared)))
    }

    /**
     * 窗帘开合。
     *
     * 有位置能力时用「设为 0/100」，这是幂等的；只有动作枚举时才按描述匹配开/关，
     * 因为不同型号的枚举取值方向可能相反，硬编码会开成关。
     */
    private fun openCurtain(device: DeviceItem, open: Boolean): Outcome {
        writable(device, "position")?.let { capability ->
            val target = if (open) capability.max?.toDouble() ?: 100.0 else capability.min?.toDouble() ?: 0.0
            return Outcome.Ok(listOf(capability.toWrite(target)))
        }

        val labels = if (open) {
            listOf("开", "打开", "open")
        } else {
            listOf("关", "关闭", "close")
        }
        return enumByLabel(device, "motion", labels)
    }

    /** 按枚举项的显示名匹配取值。 */
    private fun enumByLabel(device: DeviceItem, key: String, labels: List<String>): Outcome {
        val capability = writable(device, key) ?: return missing(device, key)
        val wanted = labels.filter { it.isNotBlank() }
        if (wanted.isEmpty()) {
            return Outcome.Rejected("缺少${capability.label}的目标值", DeviceErrorKind.BAD_VALUE)
        }

        // 先精确匹配取值或名称，再退化为包含匹配，避免「关闭」误命中「打开」
        val exact = capability.valueList.firstOrNull { item ->
            wanted.any { it.equals(item.label, ignoreCase = true) }
        }
        val matched = exact ?: capability.valueList.firstOrNull { item ->
            wanted.any { label ->
                item.label.contains(label, ignoreCase = true) || item.comment?.contains(label, ignoreCase = true) == true
            }
        }

        return if (matched != null) {
            Outcome.Ok(listOf(capability.toWrite(matched.value.toDouble())))
        } else {
            Outcome.Rejected(
                "${capability.label}不支持该取值",
                DeviceErrorKind.BAD_VALUE,
            )
        }
    }

    // ---------------- 校验辅助 ----------------

    private fun writable(device: DeviceItem, key: String): DeviceCapability? =
        device.rawCapabilities.firstOrNull { it.key == key && it.isWritable }

    /**
     * 能力不可用时的拒绝原因。
     *
     * 区分「设备压根没有这个能力」与「有这个能力但只读」：前者是设备选错了，
     * 后者是动作选错了，给用户的解释完全不同。
     */
    private fun missing(device: DeviceItem, key: String): Outcome.Rejected {
        val label = KEY_LABELS[key] ?: key
        val exists = device.rawCapabilities.any { it.key == key }
        // 注意变量一律用 ${} 包裹：中文是合法的 Kotlin 标识符字符，
        // 写成 "$label为只读" 会被解析成变量名 `label为只读`。
        return if (exists) {
            Outcome.Rejected("「${device.name}」的${label}为只读，无法设置", DeviceErrorKind.READ_ONLY)
        } else {
            Outcome.Rejected("「${device.name}」不支持${label}", DeviceErrorKind.PROP_MISSING)
        }
    }

    /**
     * 把数值夹到设备允许的范围内并对齐步长。
     *
     * 超出范围时返回 null（拒绝），而不是静默夹取——用户说"亮度 150"时应明确告知不支持，
     * 而不是悄悄设成 100。
     */
    private fun clamp(capability: DeviceCapability, value: Double): Double? {
        val min = capability.min?.toDouble()
        val max = capability.max?.toDouble()

        if (min != null && value < min) return null
        if (max != null && value > max) return null

        val step = capability.step?.toDouble()
        if (step != null && step > 0 && step != 1.0) {
            val base = min ?: 0.0
            val snapped = base + Math.round((value - base) / step) * step
            return if (max != null) snapped.coerceAtMost(max) else snapped
        }
        return value
    }

    private fun numberParam(params: Map<String, Any>, key: String): Double? =
        (params[key] as? Number)?.toDouble()
        ?: (params["value"] as? Number)?.toDouble()
        ?: params.values.firstOrNull { it is Number }?.let { (it as Number).toDouble() }

    private fun DeviceCapability.toWrite(numeric: Double, booleanValue: Boolean? = null): Write {
        // 布尔属性必须送布尔值，数值属性送整数时不带小数点
        val payload: Any = when {
            booleanValue != null -> booleanValue
            type == "boolean" -> numeric != 0.0
            numeric == numeric.toLong().toDouble() -> numeric.toLong()
            else -> numeric
        }
        return Write(key = key, siid = siid, piid = piid, value = payload)
    }

    private fun trim(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private val KEY_LABELS = mapOf(
        "power" to "开关",
        "brightness" to "亮度",
        "color_temperature" to "色温",
        "target_temperature" to "温度调节",
        "target_humidity" to "湿度调节",
        "position" to "开合",
        "motion" to "开合动作",
        "mode" to "模式",
        "fan_speed" to "风速",
    )
}
