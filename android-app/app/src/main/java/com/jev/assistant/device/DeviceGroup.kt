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

    private val GROUP_KEYWORDS = listOf("所有", "全部", "全屋", "整个家", "每个", "各个", "全关", "全开", "都关", "都开", "都调", "全都")

    fun isGroupCommand(text: String): Boolean {
        return GROUP_KEYWORDS.any { text.contains(it) }
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

    fun matchGroup(text: String, groups: List<DeviceGroup>): DeviceGroup? {
        // 先按明确房间名优先匹配
        if (text.contains("主卧") && text.contains("灯")) {
            return groups.find { it.id == "group_主卧_lights" }
        }
        if (text.contains("客厅") && text.contains("灯")) {
            return groups.find { it.id == "group_客厅_lights" }
        }
        if (text.contains("次卧") && text.contains("灯")) {
            return groups.find { it.id == "group_次卧_lights" }
        }
        if (text.contains("卧室") && text.contains("灯")) {
            return groups.find { it.id == "group_bedrooms_lights" || it.id == "group_主卧_lights" }
        }
        if (text.contains("空调")) {
            if (text.contains("主卧")) return groups.find { it.id == "group_主卧_acs" }
            if (text.contains("客厅")) return groups.find { it.id == "group_客厅_acs" }
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
