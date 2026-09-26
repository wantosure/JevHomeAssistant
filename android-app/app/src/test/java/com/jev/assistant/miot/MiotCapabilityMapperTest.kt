package com.jev.assistant.miot

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 能力映射。夹具为 miot-spec.org 的真实响应快照。
 */
class MiotCapabilityMapperTest {

    private fun specOf(name: String): MiotDeviceSpec {
        val stream = javaClass.getResourceAsStream("/miot/$name")!!
        val root = JsonParser.parseString(stream.bufferedReader().readText()).asJsonObject
        return MiotSpecParser.parse(root)!!
    }

    private fun caps(name: String) = MiotCapabilityMapper.map(specOf(name))

    // ---------------- 灯具 ----------------

    @Test
    fun `灯 应映射出开关亮度与色温`() {
        val caps = caps("light.json")
        val keys = caps.map { it.key }
        assertTrue(keys.contains("power"))
        assertTrue(keys.contains("brightness"))
        assertTrue(keys.contains("color_temperature"))
    }

    @Test
    fun `灯的开关应可写并带正确的siid与piid`() {
        val power = caps("light.json").first { it.key == "power" }
        assertTrue(power.isWritable)
        assertEquals(2, power.siid)
        assertEquals(1, power.piid)
        assertEquals("boolean", power.type)
        assertEquals("on", power.specTypeName)
    }

    @Test
    fun `灯的亮度应带百分比范围`() {
        val brightness = caps("light.json").first { it.key == "brightness" }
        assertEquals("number", brightness.type)
        assertEquals(1.0, brightness.min!!.toDouble(), 0.001)
        assertEquals(100.0, brightness.max!!.toDouble(), 0.001)
        assertEquals("percentage", brightness.unit)
        assertEquals(2, brightness.siid)
        assertEquals(2, brightness.piid)
    }

    @Test
    fun `灯的色温应带开尔文范围`() {
        val ct = caps("light.json").first { it.key == "color_temperature" }
        assertEquals(2700.0, ct.min!!.toDouble(), 0.001)
        assertEquals(6500.0, ct.max!!.toDouble(), 0.001)
        assertEquals("kelvin", ct.unit)
    }

    @Test
    fun `灯的模式应映射为枚举`() {
        val mode = caps("light.json").first { it.key == "mode" }
        assertEquals("enum", mode.type)
        assertTrue(mode.valueList.isNotEmpty())
        assertTrue(mode.valueList.all { it.label.isNotBlank() })
    }

    /** 只写属性没有回读语义，不应出现在能力里。 */
    @Test
    fun `灯的增量调节属性不应被映射`() {
        val keys = caps("light.json").map { it.key }
        assertFalse(keys.contains("brightness_delta"))
        assertFalse(keys.contains("brightness-delta"))
    }

    // ---------------- 插座（同名属性跨服务的关键用例） ----------------

    /**
     * 插座规范里 `on` 同时出现在 `switch` 服务与 `indicator-light` 服务。
     * 必须选中 `switch` 那个——控错目标会表现为「开关状态变了但没通电」。
     */
    @Test
    fun `插座的开关必须映射到switch服务而非指示灯服务`() {
        val caps = caps("plug.json")
        val power = caps.first { it.key == "power" }

        assertEquals("应选中 switch 服务的 on", 2, power.siid)
        assertEquals(1, power.piid)
        assertTrue(power.isWritable)

        // 指示灯服务的 on 位于 siid=3，绝不能被选中
        assertFalse("不应选中指示灯服务", power.siid == 3)
    }

    @Test
    fun `插座的温度读数应为只读`() {
        val temp = caps("plug.json").first { it.key == "temperature" }
        assertFalse(temp.isWritable)
        assertEquals("celsius", temp.unit)
        assertEquals(2, temp.siid)
    }

    @Test
    fun `插座不应出现重复的power能力`() {
        val powers = caps("plug.json").filter { it.key == "power" }
        assertEquals("同一能力只应保留一条", 1, powers.size)
    }

    // ---------------- 开关 ----------------

    @Test
    fun `开关应映射出开关能力`() {
        val power = caps("switch.json").first { it.key == "power" }
        assertEquals(2, power.siid)
        assertEquals(1, power.piid)
        assertTrue(power.isWritable)
    }

    @Test
    fun `未被词表覆盖的属性不应出现在能力中`() {
        val keys = caps("switch.json").map { it.key }
        // remote 服务下的遥控按键与 delaytime 等与本项目动作词表无关
        assertFalse(keys.contains("delaytime"))
        assertFalse(keys.contains("jog"))
        assertFalse(keys.contains("remote-a"))
    }

    // ---------------- 提示词渲染 ----------------

    @Test
    fun `数值能力应渲染出范围与单位供Jev判断`() {
        val brightness = caps("light.json").first { it.key == "brightness" }
        val text = brightness.describeForPrompt()

        assertTrue("应含 key: $text", text.contains("brightness"))
        assertTrue("应含范围: $text", text.contains("1-100"))
        assertTrue("应含单位: $text", text.contains("%"))
    }

    @Test
    fun `开关能力应渲染成开或关`() {
        val power = caps("light.json").first { it.key == "power" }
        assertTrue(power.describeForPrompt().contains("开/关"))
    }

    @Test
    fun `枚举能力应把选项渲染出来`() {
        val mode = caps("light.json").first { it.key == "mode" }
        val text = mode.describeForPrompt()
        assertTrue("应列出枚举项: $text", text.contains(":") && text.length > 12)
    }

    @Test
    fun `色温应渲染为开尔文范围`() {
        val ct = caps("light.json").first { it.key == "color_temperature" }
        val text = ct.describeForPrompt()
        assertTrue("应含 2700-6500: $text", text.contains("2700-6500"))
        assertTrue("应含 K 单位: $text", text.contains("K"))
    }

    @Test
    fun `查找可写能力时应排除只读项`() {
        val caps = caps("plug.json")
        assertNotNull(MiotCapabilityMapper.findWritable(caps, "power"))
        assertNull("温度是只读的，不应被当作可写能力", MiotCapabilityMapper.findWritable(caps, "temperature"))
    }

    @Test
    fun `空规范应产出空能力而不是崩溃`() {
        val empty = MiotDeviceSpec(urn = "urn:x", typeName = "x", description = "", services = emptyList())
        assertTrue(MiotCapabilityMapper.map(empty).isEmpty())
    }
}
