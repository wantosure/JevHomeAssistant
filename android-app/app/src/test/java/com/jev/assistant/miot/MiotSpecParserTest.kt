package com.jev.assistant.miot

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 解析测试，夹具为 miot-spec.org 的真实响应快照
 * （`app/src/test/resources/miot/`，取自 yeelink.light.ceiling1、lemesh.switch.sw0c01、chuangmi.plug.m1）。
 */
class MiotSpecParserTest {

    private fun load(name: String): MiotDeviceSpec {
        val stream = javaClass.getResourceAsStream("/miot/$name")
            ?: error("缺少测试夹具 /miot/$name")
        val root = JsonParser.parseString(stream.bufferedReader().readText()).asJsonObject
        return MiotSpecParser.parse(root) ?: error("解析 $name 失败")
    }

    private fun MiotDeviceSpec.prop(typeName: String, siid: Int? = null): MiotProperty? =
        allProperties.firstOrNull { it.typeName == typeName && (siid == null || it.siid == siid) }

    // ---------- 灯具 ----------

    @Test
    fun `灯 应解析出正确类别与描述`() {
        val spec = load("light.json")
        assertEquals("light", spec.typeName)
        assertEquals("Light", spec.description)
        assertEquals("urn:miot-spec-v2:device:light:0000A001:yeelink-ceiling1:2", spec.urn)
    }

    @Test
    fun `灯 应跳过 device-information 元数据服务`() {
        val spec = load("light.json")
        assertNull(spec.services.firstOrNull { it.typeName == "device-information" })
        // 元数据属性不应出现在能力列表中
        assertNull(spec.prop("manufacturer"))
        assertNull(spec.prop("serial-number"))
    }

    @Test
    fun `灯 的开关属性应可读可写`() {
        val on = load("light.json").prop("on")
        assertNotNull(on)
        assertEquals(2, on!!.siid)
        assertEquals(1, on.piid)
        assertEquals("bool", on.format)
        assertTrue(on.readable)
        assertTrue(on.writable)
        assertNull(on.unit)
    }

    @Test
    fun `灯 的亮度应解析出百分比范围`() {
        val brightness = load("light.json").prop("brightness")
        assertNotNull(brightness)
        assertEquals("percentage", brightness!!.unit)
        assertEquals(1.0, brightness.min!!, 0.001)
        assertEquals(100.0, brightness.max!!, 0.001)
        assertEquals(1.0, brightness.step!!, 0.001)
        assertTrue(brightness.isRangeConstrained)
    }

    @Test
    fun `灯 的色温应解析出开尔文范围`() {
        val ct = load("light.json").prop("color-temperature")
        assertNotNull(ct)
        assertEquals("kelvin", ct!!.unit)
        assertEquals(2700.0, ct.min!!, 0.001)
        assertEquals(6500.0, ct.max!!, 0.001)
    }

    @Test
    fun `灯 的模式应解析出枚举值`() {
        val mode = load("light.json").prop("mode")
        assertNotNull(mode)
        assertTrue(mode!!.enumValues.isNotEmpty())
        assertTrue(mode.isEnum)
    }

    @Test
    fun `只写属性应标记为不可读`() {
        val delta = load("light.json").prop("brightness-delta")
        assertNotNull(delta)
        assertFalse("brightness-delta 应只写", delta!!.readable)
        assertTrue(delta.writable)
    }

    @Test
    fun `灯 应解析出动作`() {
        val spec = load("light.json")
        assertTrue(spec.allActions.isNotEmpty())
    }

    // ---------- 插座（同名属性跨服务重复，验证 siid 区分） ----------

    @Test
    fun `插座 的 on 在开关服务与指示灯服务中应各自保留`() {
        val spec = load("plug.json")
        assertEquals("outlet", spec.typeName)

        val switchOn = spec.prop("on", siid = 2)
        val indicatorOn = spec.prop("on", siid = 3)

        assertNotNull("service 2 的 on 应存在", switchOn)
        assertNotNull("service 3 的 on 应存在", indicatorOn)
        assertEquals(2, switchOn!!.siid)
        assertEquals(3, indicatorOn!!.siid)
        // 二者的 lite iid 必须不同，否则执行时会控错对象
        assertFalse(switchOn.persistedIid() == indicatorOn.persistedIid())
    }

    @Test
    fun `插座 的温度读数应为只读且带摄氏度范围`() {
        val temp = load("plug.json").prop("temperature")
        assertNotNull(temp)
        assertFalse("温度读数不应可写", temp!!.writable)
        assertEquals("celsius", temp.unit)
        assertEquals(-40.0, temp.min!!, 0.001)
        assertEquals(125.0, temp.max!!, 0.001)
        assertEquals(0.1, temp.step!!, 0.001)
    }

    // ---------- 开关（多服务、含遥控子服务） ----------

    @Test
    fun `开关 应解析全部服务`() {
        val spec = load("switch.json")
        assertEquals("switch", spec.typeName)
        assertTrue("应包含 switch 服务", spec.services.any { it.typeName == "switch" })
        assertTrue("应包含 remote 服务", spec.services.any { it.typeName == "remote" })
        assertNull(spec.services.firstOrNull { it.typeName == "device-information" })
    }

    @Test
    fun `开关 的大范围属性应正确解析`() {
        val lockState = load("switch.json").prop("lock-state")
        assertNotNull(lockState)
        assertEquals(0.0, lockState!!.min!!, 0.001)
        assertEquals(4294967295.0, lockState.max!!, 0.001)
    }

    // ---------- iid 转换 ----------

    @Test
    fun `lite iid 与 API iid 应能互转`() {
        assertEquals("prop.2.1", MiotIid.toApi("prop.0.2.1"))
        assertEquals("action.3.1", MiotIid.toApi("action.0.3.1"))
        // 已是 API 形式应原样返回
        assertEquals("prop.2.1", MiotIid.toApi("prop.2.1"))
    }

    @Test
    fun `API iid 应能拆分出 siid 与 iid`() {
        assertEquals(Triple("prop", 2, 1), MiotIid.parseApi("prop.2.1"))
        assertEquals(Triple("action", 3, 5), MiotIid.parseApi("action.3.5"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `非法 API iid 应抛错`() {
        MiotIid.parseApi("prop.2")
    }

    // ---------- 类型表 ----------

    @Test
    fun `类型表应覆盖真实设备类别`() {
        assertEquals("灯", MiotTypeTable.chineseType("light"))
        assertEquals("插座", MiotTypeTable.chineseType("outlet"))
        assertEquals("开关", MiotTypeTable.chineseType("switch"))
        assertEquals("空调", MiotTypeTable.chineseType("air-conditioner"))
        assertEquals("窗帘", MiotTypeTable.chineseType("curtain"))
    }

    @Test
    fun `未知类别应安全降级`() {
        assertEquals("其他设备", MiotTypeTable.chineseType("some-future-device-type"))
        assertNull(MiotTypeTable.categoryOrNull("some-future-device-type"))
        assertEquals(MiotTypeTable.CAT_OTHERS, MiotTypeTable.categoryOfChineseType("其他设备"))
    }

    @Test
    fun `中文类型应包含分组逻辑依赖的关键词`() {
        // DeviceGroupManager 用 type.contains("灯") 之类的判断做分组
        assertTrue(MiotTypeTable.chineseType("light").contains("灯"))
        assertTrue(MiotTypeTable.chineseType("air-conditioner").contains("空调"))
        assertTrue(MiotTypeTable.chineseType("curtain").contains("窗帘"))
        assertTrue(MiotTypeTable.chineseType("fan").contains("风扇"))
    }

    @Test
    fun `传感器与网关默认不可控`() {
        assertFalse(MiotTypeTable.isControllable(MiotTypeTable.CAT_SENSORS))
        assertFalse(MiotTypeTable.isControllable(MiotTypeTable.CAT_HUBS))
        assertTrue(MiotTypeTable.isControllable(MiotTypeTable.CAT_LIGHTS))
    }

    @Test
    fun `URN 类型名提取`() {
        assertEquals("light", MiotSpecParser.typeNameOf("urn:miot-spec-v2:device:light:0000A001:vendor:1"))
        assertNull(MiotSpecParser.typeNameOf("bad"))
    }

    /** 供断言使用：属性在 lite 形式下的唯一键。 */
    private fun MiotProperty.persistedIid(): String = MiotIid.propLite(siid, piid)
}
