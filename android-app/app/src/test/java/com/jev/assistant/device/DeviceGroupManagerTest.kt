package com.jev.assistant.device

import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.MappingStatus
import org.junit.Assert.*
import org.junit.Test

class DeviceGroupManagerTest {

    private val testDevices = listOf(
        DeviceItem(
            logicalId = "living_light_1",
            sourceDeviceId = "living_light_1",
            name = "客厅主吸顶灯",
            shortName = "客厅吸顶灯",
            room = "客厅",
            roomId = "living",
            type = "灯具",
            icon = "💡",
            capabilities = listOf("power", "brightness"),
            rawCapabilities = emptyList(),
            mappingStatus = MappingStatus.MAPPED,
            currentState = mutableMapOf<String, Any>("power" to 1)
        ),
        DeviceItem(
            logicalId = "living_light_2",
            sourceDeviceId = "living_light_2",
            name = "客厅落地灯",
            shortName = "落地灯",
            room = "客厅",
            roomId = "living",
            type = "灯具",
            icon = "💡",
            capabilities = listOf("power", "brightness"),
            rawCapabilities = emptyList(),
            mappingStatus = MappingStatus.MAPPED,
            currentState = mutableMapOf<String, Any>("power" to 1)
        ),
        DeviceItem(
            logicalId = "master_light_1",
            sourceDeviceId = "master_light_1",
            name = "主卧吸顶灯",
            shortName = "主卧吸顶灯",
            room = "主卧",
            roomId = "master",
            type = "灯具",
            icon = "💡",
            capabilities = listOf("power", "brightness"),
            rawCapabilities = emptyList(),
            mappingStatus = MappingStatus.MAPPED,
            currentState = mutableMapOf<String, Any>("power" to 1)
        ),
        DeviceItem(
            logicalId = "master_light_2",
            sourceDeviceId = "master_light_2",
            name = "主卧床头灯",
            shortName = "床头灯",
            room = "主卧",
            roomId = "master",
            type = "灯具",
            icon = "💡",
            capabilities = listOf("power"),
            rawCapabilities = emptyList(),
            mappingStatus = MappingStatus.MAPPED,
            currentState = mutableMapOf<String, Any>("power" to 1)
        ),
        DeviceItem(
            logicalId = "guest_light_1",
            sourceDeviceId = "guest_light_1",
            name = "次卧吸顶灯",
            shortName = "次卧吸顶灯",
            room = "次卧",
            roomId = "guest",
            type = "灯具",
            icon = "💡",
            capabilities = listOf("power"),
            rawCapabilities = emptyList(),
            mappingStatus = MappingStatus.MAPPED,
            currentState = mutableMapOf<String, Any>("power" to 1)
        ),
        DeviceItem(
            logicalId = "kitchen_light_1",
            sourceDeviceId = "kitchen_light_1",
            name = "厨房平板灯",
            shortName = "平板灯",
            room = "厨房",
            roomId = "kitchen",
            type = "灯具",
            icon = "💡",
            capabilities = listOf("power"),
            rawCapabilities = emptyList(),
            mappingStatus = MappingStatus.MAPPED,
            currentState = mutableMapOf<String, Any>("power" to 1)
        )
    )

    private val testGroups = DeviceGroupManager.buildGroups(testDevices)

    @Test
    fun testExceptLivingRoomLights() {
        val utterance = "除了客厅其他房间灯都关掉"
        assertTrue(DeviceGroupManager.isGroupCommand(utterance))
        assertTrue(DeviceGroupManager.isExclusionCommand(utterance))

        val matched = DeviceGroupManager.matchGroup(
            text = utterance,
            groups = testGroups,
            devices = testDevices
        )

        assertNotNull("应该成功解析排除设备组", matched)
        val group = matched!!
        assertEquals("除客厅外所有灯", group.name)
        assertEquals("全屋(除客厅)", group.scope)

        // 验证设备列表：必须包含主卧、次卧、厨房的灯，且绝对不能包含客厅的任何灯！
        assertTrue(group.deviceIds.contains("master_light_1"))
        assertTrue(group.deviceIds.contains("master_light_2"))
        assertTrue(group.deviceIds.contains("guest_light_1"))
        assertTrue(group.deviceIds.contains("kitchen_light_1"))

        assertFalse("绝对不能包含客厅灯 1", group.deviceIds.contains("living_light_1"))
        assertFalse("绝对不能包含客厅灯 2", group.deviceIds.contains("living_light_2"))
        assertEquals(4, group.deviceIds.size)
    }

    @Test
    fun testExceptMasterAndGuestLights() {
        val utterance = "除了主卧和次卧，全屋灯都关掉"
        assertTrue(DeviceGroupManager.isGroupCommand(utterance))
        assertTrue(DeviceGroupManager.isExclusionCommand(utterance))

        val matched = DeviceGroupManager.matchGroup(
            text = utterance,
            groups = testGroups,
            devices = testDevices
        )

        assertNotNull(matched)
        val group = matched!!
        // 应该只剩下客厅的2台灯和厨房的1台灯，共3台
        assertTrue(group.deviceIds.contains("living_light_1"))
        assertTrue(group.deviceIds.contains("living_light_2"))
        assertTrue(group.deviceIds.contains("kitchen_light_1"))
        assertFalse(group.deviceIds.contains("master_light_1"))
        assertFalse(group.deviceIds.contains("guest_light_1"))
        assertEquals(3, group.deviceIds.size)
    }

    @Test
    fun testNormalLivingRoomLightsNotAffected() {
        val utterance = "关掉客厅所有灯"
        val matched = DeviceGroupManager.matchGroup(
            text = utterance,
            groups = testGroups,
            devices = testDevices
        )

        assertNotNull(matched)
        val group = matched!!
        assertEquals("group_客厅_lights", group.id)
        assertEquals(2, group.deviceIds.size)
        assertTrue(group.deviceIds.contains("living_light_1"))
        assertTrue(group.deviceIds.contains("living_light_2"))
    }
}
