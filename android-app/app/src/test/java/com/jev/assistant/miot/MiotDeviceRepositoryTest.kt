package com.jev.assistant.miot

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jev.assistant.data.MappingStatus
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 设备同步：家庭/房间归属、子设备房间继承、类型与能力推导。
 *
 * 用 MockWebServer 模拟云接口、用注入的假规范源避免访问 miot-spec.org。
 */
class MiotDeviceRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var cacheDir: File

    private val lightSpec: JsonObject by lazy { fixture("light.json") }
    private val plugSpec: JsonObject by lazy { fixture("plug.json") }

    private fun fixture(name: String): JsonObject {
        val stream = javaClass.getResourceAsStream("/miot/$name")!!
        return JsonParser.parseString(stream.bufferedReader().readText()).asJsonObject
    }

    private fun repository(specs: Map<String, JsonObject> = emptyMap()) = MiotDeviceRepository(
        cloud = MiotCloudClient(
            credentialsProvider = { MiotCredentials() },
            accessTokenProvider = { "token" },
            baseUrlOverride = server.url("/").toString().trimEnd('/'),
        ),
        specRepository = MiotSpecRepository(cacheDir) { urn ->
            val spec = specs.entries.firstOrNull { urn.contains(it.key) }?.value
            if (spec != null) Result.success(spec) else Result.failure(IllegalStateException("无规范"))
        },
        cache = null,
    )

    private fun enqueue(body: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        cacheDir = Files.createTempDirectory("miot-repo-test").toFile()
    }

    @After
    fun tearDown() {
        server.shutdown()
        cacheDir.deleteRecursively()
    }

    private val lightUrn = "urn:miot-spec-v2:device:light:0000A001:yeelink-ceiling1:2"
    private val plugUrn = "urn:miot-spec-v2:device:outlet:0000A002:chuangmi-m1:1"

    // ---------------- 家庭与房间归属 ----------------

    @Test
    fun `应把设备归入所属房间`() = runBlocking {
        enqueue(
            """
            {"code":0,"result":{"homelist":[
              {"id":"h1","name":"海淀幸福里","roomlist":[
                {"id":"r1","name":"南主卧","dids":["101"]}
              ],"dids":[]}
            ],"has_more":false}}
            """.trimIndent(),
        )
        enqueue("""{"code":0,"result":{"list":[{"did":"101","name":"主灯","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        val result = repository(mapOf("light" to lightSpec)).sync().getOrThrow()

        val device = result.devices.single()
        assertEquals("南主卧", device.room)
        assertEquals("r1", device.roomId)
        assertTrue(device.roomAssigned)
        assertEquals("海淀幸福里", device.homeName)
        assertEquals("h1", device.homeId)
    }

    /**
     * 不属于任何房间的设备，协议上会让房间名退化为家庭名。
     * 此处如实保留该行为，同时用 roomAssigned 显式标记，避免下游靠字符串比较去猜。
     */
    @Test
    fun `未分配房间的设备应标记为未分配`() = runBlocking {
        enqueue(
            """
            {"code":0,"result":{"homelist":[
              {"id":"h1","name":"亿达302","roomlist":[],"dids":["202"]}
            ],"has_more":false}}
            """.trimIndent(),
        )
        enqueue("""{"code":0,"result":{"list":[{"did":"202","name":"插座","spec_type":"$plugUrn","model":"m","isOnline":false}],"has_more":false}}""")

        val device = repository(mapOf("outlet" to plugSpec)).sync().getOrThrow().devices.single()

        assertFalse(device.roomAssigned)
        assertEquals("亿达302", device.room)
        assertFalse(device.isOnline)
    }

    /** 子设备 did 形如 `123.s1`，通常不在任何房间列表里，应继承父设备的房间。 */
    @Test
    fun `子设备应继承父设备的房间`() = runBlocking {
        enqueue(
            """
            {"code":0,"result":{"homelist":[
              {"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["100"]}],"dids":[]}
            ],"has_more":false}}
            """.trimIndent(),
        )
        enqueue(
            """
            {"code":0,"result":{"list":[
              {"did":"100","name":"网关","spec_type":"$plugUrn","model":"m","isOnline":true},
              {"did":"100.s1","name":"网关子设备","spec_type":"$plugUrn","model":"m","isOnline":true}
            ],"has_more":false}}
            """.trimIndent(),
        )

        val devices = repository(mapOf("outlet" to plugSpec)).sync().getOrThrow().devices
        val child = devices.first { it.subDeviceKey == "s1" }

        assertEquals("客厅", child.room)
        assertTrue("子设备应继承父房间", child.roomAssigned)
        assertEquals("100", child.parentId)
    }

    // ---------------- 逻辑 ID ----------------

    @Test
    fun `逻辑ID应可还原物理ID`() {
        val logical = MiotDeviceRepository.logicalIdOf("2026857030")
        assertEquals("miot_2026857030", logical)
        assertEquals("2026857030", MiotDeviceRepository.sourceIdOf(logical))
    }

    @Test
    fun `子设备的逻辑ID应可还原含点号的物理ID`() {
        val logical = MiotDeviceRepository.logicalIdOf("100.s1")
        assertEquals("miot_100_s1", logical)
        assertEquals("100.s1", MiotDeviceRepository.sourceIdOf(logical))
    }

    @Test
    fun `逻辑ID不应与物理ID相同`() {
        // 设备协议要求逻辑标识不得冒充物理标识
        assertFalse(MiotDeviceRepository.logicalIdOf("2026857030") == "2026857030")
    }

    @Test
    fun `设备应带上真实的物理ID`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["2026857030"]}],"dids":[]}]}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"2026857030","name":"灯","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        val device = repository(mapOf("light" to lightSpec)).sync().getOrThrow().devices.single()
        assertEquals("2026857030", device.sourceDeviceId)
    }

    // ---------------- 类型与能力 ----------------

    @Test
    fun `应由URN推导出中文类型与图标`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["101"]}],"dids":[]}]}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"101","name":"吸顶灯","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        val device = repository(mapOf("light" to lightSpec)).sync().getOrThrow().devices.single()

        assertEquals("灯", device.type)
        assertEquals("light", device.profile)
        assertEquals("💡", device.icon)
        assertEquals(MappingStatus.MAPPED, device.mappingStatus)
        assertTrue(device.rawCapabilities.any { it.key == "brightness" })
    }

    @Test
    fun `短名应去掉房间前缀`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"北次卧","dids":["101"]}],"dids":[]}]}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"101","name":"北次卧主灯","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        val device = repository(mapOf("light" to lightSpec)).sync().getOrThrow().devices.single()
        assertEquals("主灯", device.shortName)
    }

    /** 规范拿不到时不能假装设备可控，必须降级为 DRAFT 且不产出任何能力。 */
    @Test
    fun `规范不可得时应降级为DRAFT且无能力`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["101"]}],"dids":[]}]}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"101","name":"神秘灯","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        // 规范源返回失败
        val device = repository(emptyMap()).sync().getOrThrow().devices.single()

        assertEquals(MappingStatus.DRAFT, device.mappingStatus)
        assertTrue("DRAFT 设备不应有任何能力", device.rawCapabilities.isEmpty())
        assertFalse("DRAFT 设备不应可写", device.isWritable)
    }

    /** 传感器类别不可控，即便规范里有可写属性也要禁用。 */
    @Test
    fun `传感器类设备应标记为禁用`() = runBlocking {
        val sensorUrn = "urn:miot-spec-v2:device:motion-sensor:0000A00B:lumi-motion:1"
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["301"]}],"dids":[]}]}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"301","name":"人体传感器","spec_type":"$sensorUrn","model":"m","isOnline":true}],"has_more":false}}""")

        // 用灯具规范冒充传感器规范，验证的是类别判定而非规范内容
        val device = repository(mapOf("miot-spec" to lightSpec)).sync().getOrThrow().devices.single()

        assertEquals("传感器", device.type)
        assertEquals(MappingStatus.DISABLED, device.mappingStatus)
        assertFalse("禁用设备的能力应全部降级为只读", device.rawCapabilities.any { it.isWritable })
    }

    // ---------------- 家庭汇总 ----------------

    @Test
    fun `应汇总每个家庭的设备数`() = runBlocking {
        enqueue(
            """
            {"code":0,"result":{"homelist":[
              {"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["101","102"]}],"dids":[]},
              {"id":"h2","name":"农场","roomlist":[{"id":"r2","name":"菜园","dids":["201"]}],"dids":[]}
            ],"has_more":false}}
            """.trimIndent(),
        )
        enqueue(
            """
            {"code":0,"result":{"list":[
              {"did":"101","name":"灯1","spec_type":"$lightUrn","model":"m","isOnline":true},
              {"did":"102","name":"灯2","spec_type":"$lightUrn","model":"m","isOnline":false},
              {"did":"201","name":"灯3","spec_type":"$lightUrn","model":"m","isOnline":true}
            ],"has_more":false}}
            """.trimIndent(),
        )

        val result = repository(mapOf("light" to lightSpec)).sync().getOrThrow()

        assertEquals(2, result.homes.size)
        val home = result.homes.first { it.homeId == "h1" }
        assertEquals("家", home.name)
        assertEquals(2, home.deviceCount)
        assertEquals(1, home.onlineCount)
        assertEquals(1, home.roomCount)
    }

    @Test
    fun `没有家庭时应返回空结果而不是失败`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[],"has_more":false}}""")

        val result = repository().sync().getOrThrow()
        assertTrue(result.devices.isEmpty())
        assertTrue(result.homes.isEmpty())
    }

    @Test
    fun `云接口失败时应向上传播`() = runBlocking {
        enqueue("""{"code":-704012906,"message":"auth failed"}""")

        val result = repository().sync()
        assertTrue(result.isFailure)
    }

    // ---------------- 分页 ----------------

    @Test
    fun `设备列表分页时应合并全部页`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["101","102"]}],"dids":[]}]}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"101","name":"灯1","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":true,"next_start_did":"102"}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"102","name":"灯2","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        val devices = repository(mapOf("light" to lightSpec)).sync().getOrThrow().devices
        assertEquals("两页设备都应合并", 2, devices.size)
    }

    @Test
    fun `家庭列表分页时应合并全部页`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["101"]}],"dids":[]}],"has_more":true,"max_id":"9"}}""")
        enqueue("""{"code":0,"result":{"info":[{"id":"h2","name":"农场","roomlist":[{"id":"r2","name":"菜园","dids":["201"]}],"dids":[]}],"has_more":false}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"101","name":"灯1","spec_type":"$lightUrn","model":"m","isOnline":true},{"did":"201","name":"灯2","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        val result = repository(mapOf("light" to lightSpec)).sync().getOrThrow()

        assertEquals(2, result.homes.size)
        assertEquals(2, result.devices.size)
    }

    @Test
    fun `逻辑ID与设备应保持一一对应`() = runBlocking {
        enqueue("""{"code":0,"result":{"homelist":[{"id":"h1","name":"家","roomlist":[{"id":"r1","name":"客厅","dids":["101"]}],"dids":[]}]}}""")
        enqueue("""{"code":0,"result":{"list":[{"did":"101","name":"灯","spec_type":"$lightUrn","model":"m","isOnline":true}],"has_more":false}}""")

        val device = repository(mapOf("light" to lightSpec)).sync().getOrThrow().devices.single()
        assertNotNull(device.sourceDeviceId)
        assertEquals(device.sourceDeviceId, MiotDeviceRepository.sourceIdOf(device.logicalId))
        assertNull(MiotDeviceRepository.sourceIdOf("不是逻辑ID"))
    }
}
