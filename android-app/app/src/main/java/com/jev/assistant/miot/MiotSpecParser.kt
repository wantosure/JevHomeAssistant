package com.jev.assistant.miot

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * 把 miot-spec.org 的规范实例 JSON 解析为 [MiotDeviceSpec]。
 *
 * 解析规则来自公开规范本身（`services[].properties[].access/format/value-range/value-list`
 * 与 URN 的段位约定），不依赖任何第三方 SDK。
 *
 * 有意不做类型目录（type level）过滤：那需要额外拉取数百个接口，
 * 且查不到类别时会把整个服务丢弃，反而导致能力解析失败。
 */
object MiotSpecParser {

    /** 元数据服务，其中的 manufacturer/model/serial-number 等与设备控制无关。 */
    private const val META_SERVICE = "device-information"

    fun parse(root: JsonObject): MiotDeviceSpec? {
        val urn = root.stringOrNull("type") ?: return null
        val typeName = typeNameOf(urn) ?: return null

        val services = mutableListOf<MiotService>()
        root.arrayOrNull("services")?.forEach { element ->
            val serviceObj = element.asJsonObjectOrNull() ?: return@forEach
            val serviceUrn = serviceObj.stringOrNull("type") ?: return@forEach
            val serviceTypeName = typeNameOf(serviceUrn) ?: return@forEach
            // 跳过设备元信息服务
            if (serviceTypeName == META_SERVICE) return@forEach

            val siid = serviceObj.intOrNull("iid") ?: return@forEach
            services += MiotService(
                iid = siid,
                typeName = serviceTypeName,
                description = serviceObj.stringOrNull("description").orEmpty(),
                properties = parseProperties(serviceObj, siid),
                actions = parseActions(serviceObj, siid),
            )
        }

        return MiotDeviceSpec(
            urn = urn,
            typeName = typeName,
            description = root.stringOrNull("description").orEmpty(),
            services = services,
        )
    }

    private fun parseProperties(service: JsonObject, siid: Int): List<MiotProperty> {
        val result = mutableListOf<MiotProperty>()
        service.arrayOrNull("properties")?.forEach { element ->
            val obj = element.asJsonObjectOrNull() ?: return@forEach
            val piid = obj.intOrNull("iid") ?: return@forEach
            val propUrn = obj.stringOrNull("type") ?: return@forEach
            val propTypeName = typeNameOf(propUrn) ?: return@forEach

            val access = obj.arrayOrNull("access")?.mapNotNull { it.asStringOrNull() }.orEmpty()
            val range = parseValueRange(obj.get("value-range"))

            result += MiotProperty(
                serviceIid = siid,
                iid = piid,
                typeName = propTypeName,
                description = obj.stringOrNull("description").orEmpty(),
                format = obj.stringOrNull("format").orEmpty(),
                readable = access.contains("read"),
                writable = access.contains("write"),
                notifiable = access.contains("notify"),
                unit = normalizeUnit(obj.get("unit")),
                min = range?.first,
                max = range?.second,
                step = range?.third,
                enumValues = parseValueList(obj.get("value-list")),
            )
        }
        return result
    }

    private fun parseActions(service: JsonObject, siid: Int): List<MiotAction> {
        val result = mutableListOf<MiotAction>()
        service.arrayOrNull("actions")?.forEach { element ->
            val obj = element.asJsonObjectOrNull() ?: return@forEach
            val aiid = obj.intOrNull("iid") ?: return@forEach
            val actionUrn = obj.stringOrNull("type") ?: return@forEach
            val actionTypeName = typeNameOf(actionUrn) ?: return@forEach

            val inParams = obj.arrayOrNull("in")?.mapNotNull { paramElement ->
                val param = paramElement.asJsonObjectOrNull() ?: return@mapNotNull null
                MiotActionParam(
                    name = param.stringOrNull("type")?.let { typeNameOf(it) }
                        ?: param.stringOrNull("name").orEmpty(),
                    format = param.stringOrNull("format").orEmpty(),
                )
            }.orEmpty()

            result += MiotAction(
                serviceIid = siid,
                iid = aiid,
                typeName = actionTypeName,
                description = obj.stringOrNull("description").orEmpty(),
                inParams = inParams,
            )
        }
        return result
    }

    /**
     * 解析 `value-range`。
     *
     * 公开规范实际返回的是三元素数组 `[min, max, step]`（如 `[1,100,1]`、`[2700,6500,1]`）；
     * 此处同时容忍 `{min,max,step}` 对象形态，以免个别设备返回变体时整条能力丢失。
     */
    private fun parseValueRange(element: JsonElement?): Triple<Double, Double, Double>? {
        if (element == null || element.isJsonNull) return null

        if (element.isJsonArray) {
            val arr = element.asJsonArray
            if (arr.size() < 2) return null
            val min = arr.get(0).asDoubleOrNull() ?: return null
            val max = arr.get(1).asDoubleOrNull() ?: return null
            val step = if (arr.size() >= 3) arr.get(2).asDoubleOrNull() else null
            return Triple(min, max, step ?: 1.0)
        }

        if (element.isJsonObject) {
            val obj = element.asJsonObject
            val min = obj.get("min").asDoubleOrNull() ?: obj.get("min_").asDoubleOrNull() ?: return null
            val max = obj.get("max").asDoubleOrNull() ?: obj.get("max_").asDoubleOrNull() ?: return null
            val step = obj.get("step").asDoubleOrNull() ?: 1.0
            return Triple(min, max, step)
        }

        return null
    }

    private fun parseValueList(element: JsonElement?): List<MiotEnumValue> {
        if (element == null || !element.isJsonArray) return emptyList()
        val result = mutableListOf<MiotEnumValue>()
        (element as JsonArray).forEach { item ->
            val obj = item.asJsonObjectOrNull() ?: return@forEach
            val value = obj.intOrNull("value") ?: return@forEach
            result += MiotEnumValue(
                value = value,
                description = obj.stringOrNull("description").orEmpty(),
                comment = obj.stringOrNull("comment"),
            )
        }
        return result
    }

    /** `unit` 缺失或为 `none` 时统一归为 null。 */
    private fun normalizeUnit(element: JsonElement?): String? {
        val raw = element.asStringOrNull()?.trim().orEmpty()
        return raw.takeIf { it.isNotEmpty() && !it.equals("none", ignoreCase = true) }
    }

    /**
     * 从 URN 取出类型名，即 `:` 分隔的第 4 段。
     *
     * 形如 `urn:miot-spec-v2:device:light:0000A001:yeelink-ceiling1:2` → `light`。
     */
    fun typeNameOf(urn: String): String? {
        val parts = urn.split(':')
        if (parts.size < 4) return null
        return parts[3].takeIf { it.isNotBlank() }
    }
}

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? =
    if (this != null && !this.isJsonNull && this.isJsonObject) this.asJsonObject else null

private fun JsonObject.arrayOrNull(name: String): JsonArray? =
    get(name)?.takeIf { !it.isJsonNull && it.isJsonArray }?.asJsonArray

private fun JsonObject.stringOrNull(name: String): String? =
    get(name)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString

private fun JsonObject.intOrNull(name: String): Int? =
    get(name)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asInt

private fun JsonElement?.asDoubleOrNull(): Double? =
    if (this != null && !this.isJsonNull && this.isJsonPrimitive) asDouble else null

private fun JsonElement?.asStringOrNull(): String? =
    if (this != null && !this.isJsonNull && this.isJsonPrimitive) asString else null
