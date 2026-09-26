package com.jev.assistant.device

import com.jev.assistant.data.DeviceCapability
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.MappingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 自定义房间名下的编组与匹配。
 *
 * 房间名取自真实账号样本（「北次卧」「南主卧」「菜园」「办公室」），
 * 这些名字既不是硬编码常量，也不符合「主卧/次卧」的简单写法，
 * 正是改造前会失效的场景。
 */
class DeviceGroupRealRoomsTest {

    private fun light(id: String, room: String, name: String = "${room}灯") = DeviceItem(
        logicalId = id,
        sourceDeviceId = id,
        name = name,
        room = room,
        roomId = "r_$room",
        type = "灯",
        capabilities = listOf("power"),
        rawCapabilities = listOf(
            DeviceCapability(key = "power", label = "开关", kind = "write", type = "boolean", siid = 2, piid = 1),
        ),
        mappingStatus = MappingStatus.MAPPED,
    )

    private fun ac(id: String, room: String) = DeviceItem(
        logicalId = id,
        sourceDeviceId = id,
        name = "${room}空调",
        room = room,
        roomId = "r_$room",
        type = "空调",
        capabilities = listOf("power"),
        rawCapabilities = listOf(
            DeviceCapability(key = "power", label = "开关", kind = "write", type = "boolean", siid = 2, piid = 1),
        ),
        mappingStatus = MappingStatus.MAPPED,
    )

    private val devices = listOf(
        light("l_living", "客厅"),
        light("l_north", "北次卧"),
        light("l_south", "南主卧"),
        light("l_garden", "菜园"),
        ac("ac_office", "办公室"),
    )

    private val groups = DeviceGroupManager.buildGroups(devices)
    private val aliases = RoomAliasResolver.build(devices.map { it.room }.distinct())

    private fun match(text: String) = DeviceGroupManager.matchGroup(
        text = text,
        groups = groups,
        devices = devices,
        roomAliases = aliases,
    )

    // ---------------- 自定义房间名 ----------------

    @Test
    fun `应匹配自定义房间名的灯组`() {
        val group = match("关掉北次卧所有灯")
        assertNotNull("自定义房间名应能匹配", group)
        assertEquals(listOf("l_north"), group!!.deviceIds)
    }

    @Test
    fun `应匹配菜园的灯组`() {
        val group = match("把菜园的灯关掉")
        assertNotNull(group)
        assertEquals(listOf("l_garden"), group!!.deviceIds)
    }

    /** 「南主卧」与「主卧」共存时，绝不能命中子串导致控错房间。 */
    @Test
    fun `长房间名不应被子串房间名抢占`() {
        val group = match("打开南主卧的灯")
        assertNotNull(group)
        assertEquals("应命中南主卧，而不是被『主卧』抢先", listOf("l_south"), group!!.deviceIds)
    }

    @Test
    fun `未提及房间时应匹配全屋该类别`() {
        val group = match("把所有灯都关掉")
        assertNotNull(group)
        assertEquals("group_all_lights", group!!.id)
        assertEquals(4, group.deviceIds.size)
    }

    @Test
    fun `应匹配非灯具类别`() {
        val group = match("关掉办公室的空调")
        assertNotNull(group)
        assertEquals(listOf("ac_office"), group!!.deviceIds)
    }

    @Test
    fun `房间名加类别应命中对应组`() {
        val group = match("客厅所有灯关掉")
        assertNotNull(group)
        assertEquals(listOf("l_living"), group!!.deviceIds)
    }

    // ---------------- 卧室聚合 ----------------

    @Test
    fun `卧室聚合应识别自定义卧室名`() {
        val group = match("把所有卧室的灯关掉")
        assertNotNull("北次卧与南主卧都应算卧室", group)
        assertTrue(group!!.deviceIds.contains("l_north"))
        assertTrue(group.deviceIds.contains("l_south"))
        assertFalse("菜园不是卧室", group.deviceIds.contains("l_garden"))
        assertFalse("客厅不是卧室", group.deviceIds.contains("l_living"))
    }

    // ---------------- 反向排除 ----------------

    @Test
    fun `排除自定义房间应正确反选`() {
        val group = match("除了客厅其他房间灯都关掉")
        assertNotNull(group)
        assertFalse("绝不应包含客厅灯", group!!.deviceIds.contains("l_living"))
        assertTrue(group.deviceIds.contains("l_north"))
        assertTrue(group.deviceIds.contains("l_south"))
        assertTrue(group.deviceIds.contains("l_garden"))
        assertFalse("空调不应混入灯组", group.deviceIds.contains("ac_office"))
    }

    @Test
    fun `排除菜园应只排除菜园`() {
        val group = match("除了菜园，其他房间的灯都关掉")
        assertNotNull(group)
        assertFalse(group!!.deviceIds.contains("l_garden"))
        assertTrue(group.deviceIds.contains("l_living"))
    }

    // ---------------- 安全性：不再有兜底 ----------------

    /**
     * 这是删除 `groups.firstOrNull()` 兜底后的回归测试。
     *
     * 改造前，任何未识别的语句都会命中一个任意组，进而批量误控真实设备。
     * 现在必须返回 null，由上层退回单设备决策路径。
     */
    @Test
    fun `无法识别的语句不应命中任何组`() {
        assertNull(match("把那个东西弄一下"))
        assertNull(match("今天天气不错"))
        assertNull(match("嗯"))
    }

    @Test
    fun `提到不存在的房间时不应乱猜`() {
        assertNull("『车库』不在设备房间里，不应命中任何组", match("关掉车库的所有灯"))
    }

    @Test
    fun `设备集合为空时不应命中任何组`() {
        assertNull(
            DeviceGroupManager.matchGroup(
                text = "关掉所有灯",
                groups = emptyList(),
                devices = emptyList(),
            ),
        )
    }

    // ---------------- 别名歧义 ----------------

    /**
     * 「北次卧」与「南次卧」都会派生出「次卧」这一别名，存在歧义。
     * 此时应放弃该别名，宁可匹配不上让人澄清，也不能猜错房间去控制设备。
     */
    @Test
    fun `歧义别名应被放弃`() {
        val ambiguous = listOf(light("a", "北次卧"), light("b", "南次卧"))
        val ambiguousAliases = RoomAliasResolver.build(ambiguous.map { it.room })
        assertFalse("存在歧义时不应派生『次卧』别名", ambiguousAliases.containsKey("次卧"))
    }

    @Test
    fun `无歧义的别名应正常派生`() {
        val single = listOf(light("a", "北次卧"), light("b", "客厅"))
        val singleAliases = RoomAliasResolver.build(single.map { it.room })
        assertEquals("北次卧", singleAliases["次卧"])
    }

    @Test
    fun `人工别名仅在目标房间存在时生效`() {
        val withLiving = RoomAliasResolver.build(listOf("客厅", "北次卧"))
        assertEquals("客厅", withLiving["大厅"])

        val withoutLiving = RoomAliasResolver.build(listOf("北次卧"))
        assertFalse("没有客厅时『大厅』别名不应生效", withoutLiving.containsKey("大厅"))
    }
}
