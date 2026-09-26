package com.jev.assistant.miot

import com.google.gson.JsonParser
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.MappingStatus
import com.jev.assistant.device.DeviceErrorKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 动作翻译。用具夹具构建真实设备（灯具 / 插座 / 窗帘），
 * 覆盖参数越界、只读能力、枚举取值等拒绝路径。
 */
class MiotActionTranslatorTest {

    private fun device(fixture: String, type: String = "灯"): DeviceItem {
        val stream = javaClass.getResourceAsStream("/miot/$fixture")!!
        val root = JsonParser.parseString(stream.bufferedReader().readText()).asJsonObject
        val spec = MiotSpecParser.parse(root)!!
        val caps = MiotCapabilityMapper.map(spec)
        return DeviceItem(
            logicalId = "miot_test",
            sourceDeviceId = "test",
            name = "测试设备",
            room = "客厅",
            type = type,
            capabilities = caps.map { it.key },
            rawCapabilities = caps,
            mappingStatus = MappingStatus.MAPPED,
            profile = spec.typeName,
            urn = spec.urn,
        )
    }

    private fun ok(outcome: MiotActionTranslator.Outcome): List<MiotActionTranslator.Write> {
        assertTrue("应当翻译成功，实际: $outcome", outcome is MiotActionTranslator.Outcome.Ok)
        return (outcome as MiotActionTranslator.Outcome.Ok).writes
    }

    private fun rejected(outcome: MiotActionTranslator.Outcome): MiotActionTranslator.Outcome.Rejected {
        assertTrue("应当被拒绝，实际: $outcome", outcome is MiotActionTranslator.Outcome.Rejected)
        return outcome as MiotActionTranslator.Outcome.Rejected
    }

    // ---------------- 灯具 ----------------

    @Test
    fun `开灯应写入布尔真`() {
        val writes = ok(MiotActionTranslator.translate(device("light.json"), "set_power_on", emptyMap()))
        assertEquals(1, writes.size)
        assertEquals("power", writes[0].key)
        assertEquals(2, writes[0].siid)
        assertEquals(1, writes[0].piid)
        assertEquals(true, writes[0].value)
    }

    @Test
    fun `关灯应写入布尔假`() {
        val writes = ok(MiotActionTranslator.translate(device("light.json"), "set_power_off", emptyMap()))
        assertEquals(false, writes[0].value)
    }

    @Test
    fun `设置亮度应写入整数`() {
        val writes = ok(MiotActionTranslator.translate(device("light.json"), "set_brightness", mapOf("percent" to 80)))
        assertEquals("brightness", writes[0].key)
        assertEquals(2, writes[0].siid)
        assertEquals(2, writes[0].piid)
        assertEquals(80L, writes[0].value)
    }

    /** 越界应明确拒绝，而不是悄悄夹到边界值——否则用户以为设成了 150。 */
    @Test
    fun `亮度超出范围应被拒绝`() {
        val result = rejected(MiotActionTranslator.translate(device("light.json"), "set_brightness", mapOf("percent" to 150)))
        assertEquals(DeviceErrorKind.BAD_VALUE, result.kind)
        assertTrue(result.reason.contains("超出"))
    }

    @Test
    fun `亮度低于下限应被拒绝`() {
        val result = rejected(MiotActionTranslator.translate(device("light.json"), "set_brightness", mapOf("percent" to 0)))
        assertEquals(DeviceErrorKind.BAD_VALUE, result.kind)
    }

    @Test
    fun `缺少数值参数应被拒绝`() {
        val result = rejected(MiotActionTranslator.translate(device("light.json"), "set_brightness", emptyMap()))
        assertEquals(DeviceErrorKind.BAD_VALUE, result.kind)
    }

    @Test
    fun `设置色温应写入开尔文值`() {
        val writes = ok(MiotActionTranslator.translate(device("light.json"), "set_color_temperature", mapOf("kelvin" to 4000)))
        assertEquals("color_temperature", writes[0].key)
        assertEquals(4000L, writes[0].value)
    }

    @Test
    fun `色温超出范围应被拒绝`() {
        val result = rejected(MiotActionTranslator.translate(device("light.json"), "set_color_temperature", mapOf("kelvin" to 9000)))
        assertEquals(DeviceErrorKind.BAD_VALUE, result.kind)
    }

    @Test
    fun `未知动作应被拒绝`() {
        val result = rejected(MiotActionTranslator.translate(device("light.json"), "set_volume", emptyMap()))
        assertEquals(DeviceErrorKind.PROP_MISSING, result.kind)
    }

    // ---------------- 插座：只读能力 ----------

    /**
     * 插座的 `temperature` 是传感器读数，与空调的 `target-temperature`（设定温度）
     * 是两个不同的能力，绝不能用前者去满足"设置温度"的指令。
     */
    @Test
    fun `对无温度设定能力的设备设温度应报不支持`() {
        val plug = device("plug.json", type = "插座")
        val result = rejected(MiotActionTranslator.translate(plug, "set_temperature", mapOf("celsius" to 25)))

        assertEquals(DeviceErrorKind.PROP_MISSING, result.kind)
        assertTrue("应说明不支持温度调节: ${result.reason}", result.reason.contains("不支持"))
    }

    /** 能力存在但只读时，应报"只读"而非"不支持"——两者给用户的解释不同。 */
    @Test
    fun `对只读能力下设置指令应报只读`() {
        val light = device("light.json")
        // 构造"亮度只能读"的变体（部分灯具确实如此）
        val readOnly = light.copy(
            rawCapabilities = light.rawCapabilities.map {
                if (it.key == "brightness") it.copy(kind = "read") else it
            },
        )

        val result = rejected(
            MiotActionTranslator.translate(readOnly, "set_brightness", mapOf("percent" to 50)),
        )
        assertEquals(DeviceErrorKind.READ_ONLY, result.kind)
        assertTrue("应说明是只读: ${result.reason}", result.reason.contains("只读"))
    }

    @Test
    fun `插座开关应写到switch服务`() {
        val plug = device("plug.json", type = "插座")
        val writes = ok(MiotActionTranslator.translate(plug, "set_power_on", emptyMap()))
        assertEquals(2, writes[0].siid)
        assertEquals(true, writes[0].value)
    }

    // ---------------- 窗帘 ----------------

    /**
     * 窗帘的开合应走幂等的 target-position（设为 0 或 100），
     * 而不是依赖设备当前状态的动作枚举。
     */
    @Test
    fun `开窗帘应写入位置100`() {
        val curtain = device("curtain.json", type = "窗帘")
        val writes = ok(MiotActionTranslator.translate(curtain, "open", emptyMap()))
        assertEquals("position", writes[0].key)
        assertEquals(100L, writes[0].value)
    }

    @Test
    fun `关窗帘应写入位置0`() {
        val curtain = device("curtain.json", type = "窗帘")
        val writes = ok(MiotActionTranslator.translate(curtain, "close", emptyMap()))
        assertEquals("position", writes[0].key)
        assertEquals(0L, writes[0].value)
    }

    /**
     * 窗帘的可写位置是 target-position，只读的 current-position 不能被选中，
     * 否则开合指令会退化成动作枚举。
     */
    @Test
    fun `窗帘位置能力应是可写的target_position`() {
        val curtain = device("curtain.json", type = "窗帘")
        val position = curtain.rawCapabilities.first { it.key == "position" }
        assertTrue("position 应为可写能力", position.isWritable)
        assertEquals("target-position", position.specTypeName)
    }

    /** 停止没有幂等的等价表达，只能退回动作枚举，且取值必须来自规范而非硬编码。 */
    @Test
    fun `停止窗帘应匹配规范中的枚举取值`() {
        val curtain = device("curtain.json", type = "窗帘")
        val writes = ok(MiotActionTranslator.translate(curtain, "stop", emptyMap()))

        assertEquals("motion", writes[0].key)
        // 该型号 motor-control 的取值是 0=Pause 1=Open 2=Close
        assertEquals(0L, writes[0].value)
    }

    @Test
    fun `窗帘的动作用枚举应带出规范里的取值`() {
        val curtain = device("curtain.json", type = "窗帘")
        val motion = curtain.rawCapabilities.first { it.key == "motion" }
        assertEquals(4, motion.valueList.size)
        assertEquals(1, motion.valueList.first { it.label == "Open" }.value)
        assertEquals(2, motion.valueList.first { it.label == "Close" }.value)
    }

    @Test
    fun `不支持开合的设备执行开应被拒绝`() {
        val result = rejected(MiotActionTranslator.translate(device("light.json"), "open", emptyMap()))
        assertEquals(DeviceErrorKind.PROP_MISSING, result.kind)
    }
}
