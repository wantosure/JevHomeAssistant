package com.jev.assistant.miot

/**
 * 归一化后的米家设备能力描述。
 *
 * 由 [MiotSpecParser] 从 miot-spec.org 的公开规范实例派生。字段按本项目自身需要设计，
 * 不镜像任何第三方 SDK 的模型。
 */
data class MiotDeviceSpec(
    val urn: String,
    /** 设备类别，取自 URN 第 4 段，如 `light` / `air-conditioner` / `curtain`。 */
    val typeName: String,
    /** 设备类别的人类可读描述（miot-spec 为英文，如 `Light`）。 */
    val description: String,
    val services: List<MiotService>,
) {
    /** 全部可读属性（跨 service 展开）。 */
    val allProperties: List<MiotProperty> get() = services.flatMap { it.properties }

    /** 全部动作（跨 service 展开）。 */
    val allActions: List<MiotAction> get() = services.flatMap { it.actions }
}

/** 一个 MIoT 服务（siid）。 */
data class MiotService(
    val iid: Int,
    val typeName: String,
    val description: String,
    val properties: List<MiotProperty> = emptyList(),
    val actions: List<MiotAction> = emptyList(),
)

/**
 * 一个 MIoT 属性（siid + piid）。
 *
 * `unit` 为 `percentage` / `kelvin` / `celsius` 等语义单位，缺失时为 null。
 */
data class MiotProperty(
    val serviceIid: Int,
    val iid: Int,
    val typeName: String,
    val description: String,
    val format: String,
    val readable: Boolean,
    val writable: Boolean,
    val notifiable: Boolean,
    val unit: String? = null,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val enumValues: List<MiotEnumValue> = emptyList(),
) {
    val siid: Int get() = serviceIid
    val piid: Int get() = iid

    /** 是否带数值范围约束（如亮度 1..100）。 */
    val isRangeConstrained: Boolean get() = min != null && max != null

    /** 是否是枚举型属性（如空调模式）。 */
    val isEnum: Boolean get() = enumValues.isNotEmpty()
}

/** `value-list` 中的一个枚举项。 */
data class MiotEnumValue(
    val value: Int,
    val description: String,
    val comment: String? = null,
)

/** 一个 MIoT 动作（siid + aiid）。首版不执行动作，仅解析保留结构。 */
data class MiotAction(
    val serviceIid: Int,
    val iid: Int,
    val typeName: String,
    val description: String,
    val inParams: List<MiotActionParam> = emptyList(),
) {
    val siid: Int get() = serviceIid
    val aiid: Int get() = iid
}

/** 动作的入参描述。 */
data class MiotActionParam(
    val name: String,
    val format: String,
)

/**
 * lite iid 与 API iid 之间的转换。
 *
 * 协议里存在两种 iid 书写形式：
 *  - lite 形式 `prop.0.{siid}.{piid}` / `action.0.{siid}.{aiid}`，中间的 `0` 是设备根节点占位；
 *  - API 形式 `prop.{siid}.{piid}` / `action.{siid}.{aiid}`，用于属性读写请求。
 */
object MiotIid {

    /** 由 siid/piid 构造 lite 形式的属性 iid。 */
    fun propLite(siid: Int, piid: Int): String = "prop.0.$siid.$piid"

    /** 由 siid/aiid 构造 lite 形式的动作 iid。 */
    fun actionLite(siid: Int, aiid: Int): String = "action.0.$siid.$aiid"

    /** lite 形式 → API 形式。已是 API 形式时原样返回。 */
    fun toApi(liteIid: String): String {
        val parts = liteIid.split('.')
        if (parts.size != 4) return liteIid
        if (parts[0] != "prop" && parts[0] != "action") return liteIid
        if (parts[1] != "0") return liteIid
        return "${parts[0]}.${parts[2]}.${parts[3]}"
    }

    /** API 形式 → `(类型, siid, iid)` 三元组。 */
    fun parseApi(apiIid: String): Triple<String, Int, Int> {
        val parts = apiIid.split('.')
        require(parts.size == 3) { "非法 API iid: $apiIid" }
        val kind = parts[0]
        require(kind == "prop" || kind == "action") { "非法 API iid 前缀: $apiIid" }
        val siid = parts[1].toIntOrNull() ?: throw IllegalArgumentException("非法 siid: $apiIid")
        val iid = parts[2].toIntOrNull() ?: throw IllegalArgumentException("非法 iid: $apiIid")
        return Triple(kind, siid, iid)
    }
}
