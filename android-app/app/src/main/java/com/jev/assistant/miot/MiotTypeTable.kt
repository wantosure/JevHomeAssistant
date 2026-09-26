package com.jev.assistant.miot

/**
 * 米家设备类别 → 本项目的展示类型与分组类别。
 *
 * 设备类别取自 URN 的第 4 段（如 `urn:miot-spec-v2:device:light:0000A001:vendor-model:2` 中的 `light`），
 * 这是公开规范的稳定标识，比按型号字符串猜测可靠，也不受品牌影响。
 *
 * 中文类型名刻意保持简短且包含既有分组逻辑依赖的关键词
 * （`DeviceGroupManager` 用 `type.contains("灯")` 这类判断），因此「灯」不写成「灯具」也不影响。
 */
object MiotTypeTable {

    /** 分组类别常量，与 `DeviceGroup.type` 的取值保持一致。 */
    const val CAT_LIGHTS = "lights"
    const val CAT_ACS = "acs"
    const val CAT_CURTAINS = "curtains"
    const val CAT_FANS = "fans"
    const val CAT_OUTLETS = "outlets"
    const val CAT_PURIFIERS = "purifiers"
    const val CAT_HUMIDIFIERS = "humidifiers"
    const val CAT_AIRERS = "airers"
    const val CAT_HEATERS = "heaters"
    const val CAT_SENSORS = "sensors"
    const val CAT_HUBS = "hubs"
    const val CAT_OTHERS = "others"

    private data class Entry(val chinese: String, val icon: String, val category: String)

    /**
     * URN 类别名 → 展示信息。
     *
     * 未列出的类别会退回「其他设备」，不影响只读展示，但会让该设备不参与
     * 任何按类别的语音批量控制，属于安全侧降级。
     */
    private val TABLE: Map<String, Entry> = buildMap {
        fun reg(category: String, chinese: String, icon: String, vararg typeNames: String) {
            typeNames.forEach { put(it, Entry(chinese, icon, category)) }
        }

        reg(CAT_LIGHTS, "灯", "💡",
            "light", "night-light", "dimmer", "pickup-light", "carlight", "light-source", "ambient-light")

        reg(CAT_ACS, "空调", "❄️",
            "air-conditioner", "air-condition-outlet", "thermostat", "air-conditioner-outlet")

        reg(CAT_CURTAINS, "窗帘", "🪟",
            "curtain", "window-opener", "roller-shutter", "motorized-curtain")

        reg(CAT_FANS, "风扇", "🌀",
            "fan", "ceiling-fan", "water-propeller", "fan-control-unit", "breezelamp")

        reg(CAT_OUTLETS, "插座", "🔌",
            "outlet", "plug", "socket", "power-strip", "card-switch")

        reg(CAT_OUTLETS, "开关", "🔘",
            "switch", "relay", "electronic-valve", "wall-switch")

        reg(CAT_PURIFIERS, "净化器", "🌬️",
            "air-purifier", "air-fresh", "air-monitor")

        reg(CAT_HUMIDIFIERS, "加湿器", "💧",
            "humidifier", "dehumidifier", "diffuser", "aroma-diffuser")

        reg(CAT_AIRERS, "晾衣架", "👔",
            "airer", "clothes-dryer", "dryer", "drying-rack")

        reg(CAT_HEATERS, "取暖器", "🔥",
            "heater", "electric-blanket", "bath-heater", "water-heater", "heater-machine")

        reg(CAT_SENSORS, "温湿度传感器", "🌡️",
            "temperature-humidity-sensor", "thermometer", "weather-sensor", "hygrothermograph")

        reg(CAT_SENSORS, "传感器", "📡",
            "motion-sensor", "occupancy-sensor", "pressure-sensor", "illumination-sensor",
            "smoke-sensor", "submersion-sensor", "gas-sensor", "vibration-sensor",
            "magnet-sensor", "door-sensor", "water-leak-sensor", "sofa")

        reg(CAT_HUBS, "网关", "📶",
            "router", "gateway", "repeater", "stb", "tv-box", "module", "central-hub")

        reg(CAT_HUBS, "面板", "🎛️",
            "control-panel", "scene-panel", "switch-panel")

        reg("speakers", "音箱", "🔊",
            "speaker", "ble-speaker", "audio-video-amplifier", "sound-box")

        reg("cameras", "摄像头", "📷",
            "camera", "ipcamera", "video-doorbell", "doorbell", "camera-control")

        reg("locks", "门锁", "🔒",
            "lock", "dlock", "ulock", "safe-box", "door-lock")

        reg("cleaners", "扫地机", "🤖",
            "vacuum", "vacuum-cleaner", "mopping-machine", "sweeping-robot")

        reg("pets", "宠物设备", "🐾",
            "pet-feeder", "pet-drinking-fountain", "cat-litter-box", "pet-toy")

        reg("kitchen", "厨电", "🍳",
            "kettle", "coffee-machine", "rice-bin", "cooker", "microwave-oven", "oven",
            "induction-cooker", "pressure-cooker", "air-fryer", "dishwasher", "rice-cooker")

        reg("health", "健康设备", "⚕️",
            "scale", "sphygmomanometer", "health-pot", "toothbrush", "shaver",
            "massager", "mattress", "weight-scale", "body-fat-scale")

        reg("av", "影音设备", "📺",
            "television", "projector", "projector-screen", "tv", "monitor", "printer")
    }

    /**
     * 按 URN 类别名取展示类型；未收录时返回 null，由调用方决定降级策略。
     */
    private fun lookup(typeName: String?): Entry? = typeName?.let { TABLE[it.trim().lowercase()] }

    /** 展示用中文类型；未收录时返回「其他设备」。 */
    fun chineseType(typeName: String?): String = lookup(typeName)?.chinese ?: "其他设备"

    /** 展示用图标。 */
    fun icon(typeName: String?): String = lookup(typeName)?.icon ?: "📱"

    /** 分组类别；未收录时返回 null，表示该类别不具备分组语义。 */
    fun categoryOrNull(typeName: String?): String? = lookup(typeName)?.category

    /**
     * 由中文类型反查分组类别，供 `DeviceGroupManager` 在只有 `DeviceItem.type` 时使用。
     */
    fun categoryOfChineseType(chineseType: String): String =
        TABLE.values.firstOrNull { it.chinese == chineseType }?.category ?: CAT_OTHERS

    /**
     * 名称兜底推断，**仅用于展示与分组**，不参与控制决策。
     *
     * 当设备缺少可解析的 URN（`spec_type` 缺失且按型号反查失败）时，
     * 至少让它在界面上有个可读类型，并且不至于完全没有分组归属。
     */
    fun guessFromName(name: String): Triple<String, String, String> = when {
        name.contains("灯") || name.contains("照明") -> Triple("灯", "💡", CAT_LIGHTS)
        name.contains("空调") || name.contains("冷气") -> Triple("空调", "❄️", CAT_ACS)
        name.contains("窗帘") || name.contains("窗帘机") -> Triple("窗帘", "🪟", CAT_CURTAINS)
        name.contains("风扇") || name.contains("循环扇") -> Triple("风扇", "🌀", CAT_FANS)
        name.contains("插座") || name.contains("插排") -> Triple("插座", "🔌", CAT_OUTLETS)
        name.contains("开关") || name.contains("通断器") -> Triple("开关", "🔘", CAT_OUTLETS)
        name.contains("净化") -> Triple("净化器", "🌬️", CAT_PURIFIERS)
        name.contains("加湿") -> Triple("加湿器", "💧", CAT_HUMIDIFIERS)
        name.contains("晾衣") -> Triple("晾衣架", "👔", CAT_AIRERS)
        name.contains("网关") || name.contains("路由器") -> Triple("网关", "📶", CAT_HUBS)
        name.contains("摄像") || name.contains("门铃") -> Triple("摄像头", "📷", "cameras")
        name.contains("门锁") -> Triple("门锁", "🔒", "locks")
        name.contains("传感") -> Triple("传感器", "📡", CAT_SENSORS)
        else -> Triple("其他设备", "📱", CAT_OTHERS)
    }

    /** 该类别的设备是否允许发起写操作。传感器、网关等默认只读。 */
    fun isControllable(category: String): Boolean = category !in setOf(CAT_SENSORS, CAT_HUBS)
}
