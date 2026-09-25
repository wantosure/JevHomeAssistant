package com.jev.assistant.device

import com.jev.assistant.data.DeviceItem

data class DeviceGroup(
    val id: String,
    val name: String,
    val scope: String, // "all", "master", "living", "bedrooms" 等
    val type: String,  // "lights", "acs", "curtains", "fans", "fresh_air"
    val deviceIds: List<String>
)

object DeviceGroupManager {

    private val GROUP_KEYWORDS = listOf(
        "所有", "全部", "全屋", "整个家", "每个", "各个",
        "全关", "全开", "都关", "都开", "都调", "全都",
        "除了", "除开", "除去", "其他房间", "其余房间", "其余"
    )

    fun isGroupCommand(text: String): Boolean {
        return GROUP_KEYWORDS.any { text.contains(it) } || isExclusionCommand(text)
    }

    /**
     * 判断是否为反向排除式批量控制（例如：“除了客厅其他房间灯都关掉”）
     */
    fun isExclusionCommand(text: String): Boolean {
        return text.contains("除了") || text.contains("除开") || text.contains("除去") ||
                (text.contains("除") && (text.contains("外") || text.contains("其他") || text.contains("其余") || text.contains("所有") || text.contains("都")))
    }

    /**
     * 解析反向排除式控制，动态构建被排除房间以外的设备组
     */
    fun resolveExclusionGroup(
        text: String,
        devices: List<DeviceItem>,
        roomAliases: Map<String, String> = emptyMap()
    ): DeviceGroup? {
        if (!isExclusionCommand(text)) return null

        val knownRooms = devices.map { it.room }.filter { it.isNotBlank() && it != "全屋" }.distinct()
        val excludedRooms = mutableListOf<String>()

        // 提取被排除的部分：从 "除了/除开/除去/除" 开始截取
        val afterExclude = when {
            text.contains("除了") -> text.substringAfter("除了")
            text.contains("除开") -> text.substringAfter("除开")
            text.contains("除去") -> text.substringAfter("除去")
            text.contains("除") -> text.substringAfter("除")
            else -> text
        }

        // 截取到排除范围结束的分隔词
        val stopWords = listOf("以外", "之外", "外", "其他", "其余", "所有", "全部", "全屋", "的", "都")
        var roomPart = afterExclude
        for (sw in stopWords) {
            if (roomPart.contains(sw)) {
                roomPart = roomPart.substringBefore(sw)
                break
            }
        }

        // 在被排除的片段中匹配房间名称
        for (r in knownRooms) {
            if (roomPart.contains(r)) {
                excludedRooms.add(r)
            }
        }
        for ((alias, canonical) in roomAliases) {
            if (roomPart.contains(alias) && !excludedRooms.contains(canonical)) {
                excludedRooms.add(canonical)
            }
        }

        // 容错兜底：若片段截取不精确，在全句中检查除X
        if (excludedRooms.isEmpty()) {
            for (r in knownRooms) {
                if (text.contains("除$r") || text.contains("除了$r") || text.contains("除开$r")) {
                    excludedRooms.add(r)
                }
            }
            for ((alias, canonical) in roomAliases) {
                if (text.contains("除$alias") || text.contains("除了$alias")) {
                    excludedRooms.add(canonical)
                }
            }
        }

        if (excludedRooms.isEmpty()) return null

        // 提取目标设备类别
        val targetType = when {
            text.contains("空调") -> "空调"
            text.contains("窗帘") -> "窗帘"
            text.contains("风扇") -> "风扇"
            text.contains("插座") || text.contains("开关") -> "插座"
            text.contains("灯") || text.contains("照明") -> "灯"
            else -> "灯"
        }

        val excludedDistinct = excludedRooms.distinct()
        // 筛选设备：房间不在 excludedDistinct 中，且类型匹配 targetType
        val matchedDevices = devices.filter { dev ->
            dev.room !in excludedDistinct && dev.type.contains(targetType)
        }

        if (matchedDevices.isEmpty()) return null

        val excludedName = excludedDistinct.joinToString("和")
        val groupName = "除${excludedName}外所有$targetType"
        val groupId = "group_except_${excludedDistinct.joinToString("_")}_$targetType"

        return DeviceGroup(
            id = groupId,
            name = groupName,
            scope = "全屋(除${excludedName})",
            type = targetType,
            deviceIds = matchedDevices.map { it.logicalId }
        )
    }

    fun buildGroups(devices: List<DeviceItem>): List<DeviceGroup> {
        val groups = mutableListOf<DeviceGroup>()

        // 1. 全屋所有灯
        val allLights = devices.filter { it.type.contains("灯") }.map { it.logicalId }
        if (allLights.isNotEmpty()) {
            groups.add(DeviceGroup("group_all_lights", "全屋所有灯", "all", "lights", allLights))
        }

        // 2. 全屋所有空调
        val allAcs = devices.filter { it.type.contains("空调") }.map { it.logicalId }
        if (allAcs.isNotEmpty()) {
            groups.add(DeviceGroup("group_all_acs", "全屋所有空调", "all", "acs", allAcs))
        }

        // 3. 全屋所有窗帘
        val allCurtains = devices.filter { it.type.contains("窗帘") }.map { it.logicalId }
        if (allCurtains.isNotEmpty()) {
            groups.add(DeviceGroup("group_all_curtains", "全屋所有窗帘", "all", "curtains", allCurtains))
        }

        // 4. 所有卧室的灯 (主卧、次卧、儿童房)
        val bedroomLights = devices.filter { it.room in listOf("主卧", "次卧", "儿童房") && it.type.contains("灯") }.map { it.logicalId }
        if (bedroomLights.isNotEmpty()) {
            groups.add(DeviceGroup("group_bedrooms_lights", "所有卧室的灯", "bedrooms", "lights", bedroomLights))
        }

        // 5. 各房间维度分组（如：主卧所有灯、客厅所有灯、厨房所有灯等）
        val rooms = devices.map { it.room }.distinct()
        for (r in rooms) {
            val rLights = devices.filter { it.room == r && it.type.contains("灯") }.map { it.logicalId }
            if (rLights.isNotEmpty()) {
                groups.add(DeviceGroup("group_${r}_lights", "${r}所有灯", r, "lights", rLights))
            }
            val rAcs = devices.filter { it.room == r && it.type.contains("空调") }.map { it.logicalId }
            if (rAcs.isNotEmpty()) {
                groups.add(DeviceGroup("group_${r}_acs", "${r}所有空调", r, "acs", rAcs))
            }
        }

        return groups
    }

    fun matchGroup(
        text: String,
        groups: List<DeviceGroup>,
        devices: List<DeviceItem> = emptyList(),
        roomAliases: Map<String, String> = emptyMap()
    ): DeviceGroup? {
        // 1. 最高优先级：如果是反向排除指令（如“除了客厅其他房间灯都关掉”），动态构建排除组
        if (isExclusionCommand(text) && devices.isNotEmpty()) {
            val exclusionGroup = resolveExclusionGroup(text, devices, roomAliases)
            if (exclusionGroup != null) {
                return exclusionGroup
            }
        }

        // 2. 常规设备组匹配（增加排除防误触：若指令提到除X，绝对不能误命中X本身！）
        if (text.contains("主卧") && text.contains("灯") && !text.contains("除主卧") && !text.contains("除了主卧")) {
            return groups.find { it.id == "group_主卧_lights" }
        }
        if (text.contains("客厅") && text.contains("灯") && !text.contains("除客厅") && !text.contains("除了客厅")) {
            return groups.find { it.id == "group_客厅_lights" }
        }
        if (text.contains("次卧") && text.contains("灯") && !text.contains("除次卧") && !text.contains("除了次卧")) {
            return groups.find { it.id == "group_次卧_lights" }
        }
        if (text.contains("卧室") && text.contains("灯")) {
            return groups.find { it.id == "group_bedrooms_lights" || it.id == "group_主卧_lights" }
        }
        if (text.contains("空调")) {
            if (text.contains("主卧") && !text.contains("除主卧") && !text.contains("除了主卧")) return groups.find { it.id == "group_主卧_acs" }
            if (text.contains("客厅") && !text.contains("除客厅") && !text.contains("除了客厅")) return groups.find { it.id == "group_客厅_acs" }
            return groups.find { it.id == "group_all_acs" }
        }
        if (text.contains("窗帘")) {
            return groups.find { it.id == "group_all_curtains" }
        }
        if (text.contains("灯")) {
            return groups.find { it.id == "group_all_lights" }
        }
        return groups.firstOrNull()
    }
}
