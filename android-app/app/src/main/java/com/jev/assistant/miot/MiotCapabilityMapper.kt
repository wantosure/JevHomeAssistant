package com.jev.assistant.miot

import com.jev.assistant.data.CapabilityValue
import com.jev.assistant.data.DeviceCapability

/**
 * 把设备规范翻译成本项目统一的能力词表。
 *
 * 本项目的动作词表（`set_power` / `set_brightness` / …）由 docs/device-control-contract.md 定义，
 * 与米家规范的 `on` / `brightness` 等英文名不同，需要这一层对齐。
 *
 * 单位与方向一律以规范为准，不做猜测：
 * 例如窗帘的 `motor-control` 在不同型号里 0/1/2 的含义可能不同，
 * 因此开/停/关的名称直接取自规范的 `value-list` 描述。
 */
object MiotCapabilityMapper {

    private data class PropMapping(
        val key: String,
        val label: String,
        val writeKind: Boolean,
    )

    /**
     * 规范属性名 → 本项目能力 key。
     *
     * 顺序即优先级：同名属性在多个服务中出现时，取靠前的。
     */
    private val PROPERTY_MAP: Map<String, PropMapping> = buildMap {
        put("on", PropMapping("power", "开关", writeKind = true))
        put("brightness", PropMapping("brightness", "亮度", writeKind = true))
        put("color-temperature", PropMapping("color_temperature", "色温", writeKind = true))
        put("target-temperature", PropMapping("target_temperature", "目标温度", writeKind = true))
        put("target-humidity", PropMapping("target_humidity", "目标湿度", writeKind = true))
        put("mode", PropMapping("mode", "模式", writeKind = true))
        put("fan-level", PropMapping("fan_speed", "风速", writeKind = true))
        put("fan-speed", PropMapping("fan_speed", "风速", writeKind = true))
        put("speed-level", PropMapping("fan_speed", "风速", writeKind = true))
        put("target-position", PropMapping("position", "开合度", writeKind = true))
        put("motor-control", PropMapping("motion", "开合动作", writeKind = true))
        // 以下为只读读数
        put("temperature", PropMapping("temperature", "温度", writeKind = false))
        put("relative-humidity", PropMapping("relative_humidity", "湿度", writeKind = false))
        put("current-position", PropMapping("position", "开合度", writeKind = false))
        put("pm2.5-density", PropMapping("pm25", "PM2.5", writeKind = false))
    }

    /**
     * 服务优先级。数值越大越优先。
     *
     * 用于解决同名属性跨服务重复的问题——典型例子是插座：
     * 真正控制通断电的是 `switch` 服务的 `on`，
     * 而 `indicator-light` 服务的 `on` 只管指示灯，控错就会"开了开关但没通电"。
     */
    private fun servicePriority(typeName: String): Int = when {
        typeName in PRIMARY_SERVICES -> 100
        typeName.endsWith("-extension") -> 10
        typeName in AUXILIARY_SERVICES -> 5
        else -> 50
    }

    private val PRIMARY_SERVICES = setOf(
        "switch", "light", "air-conditioner", "outlet", "fan", "curtain",
        "humidifier", "air-purifier", "air-fresh", "heater", "airer",
        "pet-feeder", "vacuum", "cooker", "kettle", "television",
    )

    private val AUXILIARY_SERVICES = setOf(
        "indicator-light", "remote", "function", "device-information",
        "environment", "alarm", "physical-controls-locked",
    )

    /**
     * 生成能力列表。
     *
     * 同一 key 只保留一条，按两条规则取舍：
     *  1. 服务优先级高的胜出（解决插座 `switch` 与 `indicator-light` 都有 `on` 的问题）；
     *  2. 优先级相同时，**可写的胜出**——窗帘的 `target-position`（可写）与
     *     `current-position`（只读）都会映射成 `position`，若保留只读的那个，
     *     开合指令就只能退回动作枚举，失去幂等语义。
     */
    fun map(spec: MiotDeviceSpec): List<DeviceCapability> {
        val chosen = LinkedHashMap<String, Pair<MiotProperty, Int>>()

        // 用 for + continue 而非嵌套 forEach：两层 forEach 共用隐式标签 `forEach`，
        // return@forEach 到底作用于哪一层不直观，容易在后续改动中出错。
        for (service in spec.services) {
            val priority = servicePriority(service.typeName)
            for (property in service.properties) {
                val mapping = PROPERTY_MAP[property.typeName] ?: continue

                val existing = chosen[mapping.key]
                val better = existing == null ||
                    priority > existing.second ||
                    (priority == existing.second && !existing.first.writable && property.writable)

                if (better) {
                    chosen[mapping.key] = property to priority
                }
            }
        }

        return chosen.map { (key, pair) -> pair.first.toCapability(key, PROPERTY_MAP.getValue(pair.first.typeName)) }
    }

    private fun MiotProperty.toCapability(key: String, mapping: PropMapping): DeviceCapability {
        val capabilityType = when {
            enumValues.isNotEmpty() -> "enum"
            format == "bool" -> "boolean"
            format.startsWith("uint") || format.startsWith("int") || format == "float" -> "number"
            else -> "string"
        }
        // 规范里声明可写才允许写；只读属性即便有映射也只能读
        val kind = if (mapping.writeKind && writable) "write" else "read"

        return DeviceCapability(
            key = key,
            label = mapping.label,
            kind = kind,
            type = capabilityType,
            min = min,
            max = max,
            step = step,
            unit = unit,
            valueList = enumValues.map {
                CapabilityValue(value = it.value, label = it.description, comment = it.comment)
            },
            siid = siid,
            piid = piid,
            specTypeName = typeName,
        )
    }

    /**
     * 按本项目动作词表查找可写能力。
     */
    fun findWritable(capabilities: List<DeviceCapability>, key: String): DeviceCapability? =
        capabilities.firstOrNull { it.key == key && it.isWritable }
}
