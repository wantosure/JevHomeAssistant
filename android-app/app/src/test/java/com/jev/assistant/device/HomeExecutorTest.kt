package com.jev.assistant.device

import com.jev.assistant.data.DeviceCapability
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.ExecutionPlan
import com.jev.assistant.data.IntentType
import com.jev.assistant.data.MappingStatus
import com.jev.assistant.data.RunStatus
import com.jev.assistant.miot.MiotHome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 执行器的门控与结果汇总。
 *
 * 这是整个米家接入里最需要证明的一段逻辑：**观察模式必须零副作用**。
 * 因此测试用假适配器记录每一次调用，直接断言"没有发生过 write"，
 * 而不是依赖界面表现去推断。
 */
class HomeExecutorTest {

    private val registry = DeviceRegistry()

    /** 记录适配器被调用的轨迹。 */
    private val calls = mutableListOf<String>()

    /** 每台设备的执行结果，默认全部成功。 */
    private var executeResult: (String) -> DeviceActionResult = { DeviceActionResult.ok() }

    private val fakeAdapter = object : DeviceAdapter {
        override val adapterId = "fake"
        override val displayName = "测试适配器"

        override suspend fun listDevices(): Result<List<DeviceItem>> = Result.success(emptyList())

        override suspend fun readState(logicalId: String, keys: List<String>): Result<Map<String, Any>> =
            Result.success(emptyMap())

        override suspend fun execute(
            logicalId: String,
            action: String,
            params: Map<String, Any>,
        ): Result<DeviceActionResult> {
            calls += "execute:$logicalId"
            return Result.success(executeResult(logicalId))
        }

        override suspend fun preview(logicalId: String, action: String, params: Map<String, Any>): Result<String> {
            calls += "preview:$logicalId"
            return Result.success("power=开 → prop.2.1")
        }
    }

    private fun device(
        logicalId: String,
        name: String = "灯",
        writable: Boolean = true,
        status: MappingStatus = MappingStatus.VERIFIED,
    ) = DeviceItem(
        logicalId = logicalId,
        sourceDeviceId = logicalId.removePrefix("miot_"),
        name = name,
        room = "客厅",
        roomId = "r1",
        type = "灯",
        capabilities = listOf("power"),
        rawCapabilities = listOf(
            DeviceCapability(
                key = "power", label = "开关", kind = if (writable) "write" else "read",
                type = "boolean", siid = 2, piid = 1, specTypeName = "on",
            ),
        ),
        mappingStatus = status,
        homeId = "h1",
        homeName = "家",
    )

    private fun seed(vararg devices: DeviceItem) {
        registry.applySyncResult(
            devices = devices.toList(),
            homes = listOf(MiotHome("h1", "家", devices.size, devices.size, 1)),
            preferredHomeId = "h1",
        )
    }

    private fun plan(
        targetId: String,
        action: String = "set_power_on",
        params: Map<String, Any> = emptyMap(),
    ) = ExecutionPlan(
        runId = "run-1",
        utteranceText = "开灯",
        intent = IntentType.HOME_CONTROL,
        confidence = 0.9f,
        targetDeviceName = "灯",
        targetDeviceId = targetId,
        action = action,
        parameters = params,
        room = "客厅",
    )

    private fun executor(withAdapter: Boolean = true) =
        HomeExecutor(registry) { if (withAdapter) fakeAdapter else null }

    // ---------------- 安全性：观察模式 ----------------

    /**
     * 最关键的一条：观察模式下绝不能调用 execute。
     * 一旦这条失效，用户以为在"只看不做"，实际已经把家里的灯关了。
     */
    @Test
    fun `观察模式绝不能下发只做预演`() = runBlocking {
        seed(device("miot_1"))
        calls.clear()

        val result = executor().executeHomeControl(plan("miot_1"), isLive = false)

        assertFalse("观察模式下不应出现任何 execute 调用", calls.any { it.startsWith("execute:") })
        assertTrue("观察模式应做预演", calls.any { it.startsWith("preview:") })
        assertEquals(RunStatus.PLANNED, result.status)
    }

    @Test
    fun `观察模式的状态文案应说明未下发`() = runBlocking {
        seed(device("miot_1"))
        val result = executor().executeHomeControl(plan("miot_1"), isLive = false)
        assertTrue(result.statusMessage.contains("未调用") || result.statusMessage.contains("观察模式"))
    }

    @Test
    fun `观察模式应展示将要下发的属性`() = runBlocking {
        seed(device("miot_1"))
        val result = executor().executeHomeControl(plan("miot_1"), isLive = false)
        assertTrue(
            "应能看到 siid/piid 形式的计划",
            result.events.any { it.message.contains("prop.") },
        )
    }

    @Test
    fun `组控制在观察模式下同样不下发`() = runBlocking {
        seed(device("miot_1"), device("miot_2"), device("miot_3"))
        calls.clear()

        val result = executor().executeHomeControl(
            plan("group_all_lights", params = mapOf("is_group" to true, "device_ids" to listOf("miot_1", "miot_2", "miot_3"))),
            isLive = false,
        )

        assertEquals(0, calls.count { it.startsWith("execute:") })
        assertEquals(3, calls.count { it.startsWith("preview:") })
        assertEquals(RunStatus.PLANNED, result.status)
    }

    // ---------------- 未绑定 ----------------

    @Test
    fun `未绑定时应明确报错且不下发`() = runBlocking {
        seed(device("miot_1"))
        calls.clear()

        val result = executor(withAdapter = false).executeHomeControl(plan("miot_1"), isLive = true)

        assertEquals(RunStatus.FAILED, result.status)
        assertTrue("应提示未绑定: ${result.statusMessage}", result.statusMessage.contains("绑定"))
        assertTrue(calls.isEmpty())
    }

    // ---------------- LIVE 执行 ----------------

    @Test
    fun `LIVE模式应真实下发`() = runBlocking {
        seed(device("miot_1"))
        calls.clear()

        val result = executor().executeHomeControl(plan("miot_1"), isLive = true)

        assertEquals(listOf("execute:miot_1"), calls)
        assertEquals(RunStatus.SUCCEEDED, result.status)
    }

    @Test
    fun `下发成功应把回读状态写入注册表`() = runBlocking {
        seed(device("miot_1"))
        executeResult = { DeviceActionResult.ok(mapOf("power" to true)) }

        executor().executeHomeControl(plan("miot_1"), isLive = true)

        assertEquals(true, registry.findDeviceById("miot_1")?.currentState?.get("power"))
    }

    /** 能力未完成映射的设备不允许下发——设备协议文档的硬性要求。 */
    @Test
    fun `能力未知的设备在LIVE下不应下发`() = runBlocking {
        seed(device("miot_1", status = MappingStatus.DRAFT))
        calls.clear()

        val result = executor().executeHomeControl(plan("miot_1"), isLive = true)

        assertEquals(RunStatus.FAILED, result.status)
        assertEquals(0, calls.count { it.startsWith("execute:") })
    }

    @Test
    fun `只读设备的LIVE下发应被拒绝`() = runBlocking {
        seed(device("miot_1", writable = false, status = MappingStatus.MAPPED))
        calls.clear()

        val result = executor().executeHomeControl(plan("miot_1"), isLive = true)

        assertEquals(RunStatus.FAILED, result.status)
        assertEquals(0, calls.count { it.startsWith("execute:") })
    }

    @Test
    fun `找不到设备应失败而不是假装成功`() = runBlocking {
        seed(device("miot_1"))
        val result = executor().executeHomeControl(plan("miot_不存在"), isLive = true)

        assertEquals(RunStatus.FAILED, result.status)
        assertTrue(result.statusMessage.contains("未能确定"))
    }

    // ---------------- 批量结果汇总 ----------------

    @Test
    fun `全部成功应报成功并统计台数`() = runBlocking {
        seed(device("miot_1", "灯1"), device("miot_2", "灯2"))
        val result = executor().executeHomeControl(
            plan("group_all_lights", params = mapOf("is_group" to true, "device_ids" to listOf("miot_1", "miot_2"))),
            isLive = true,
        )

        assertEquals(RunStatus.SUCCEEDED, result.status)
        assertTrue(result.statusMessage.contains("2 台"))
    }

    /** 一台离线不应被算成整体成功，也不应被算成整体失败。 */
    @Test
    fun `部分失败应报部分成功`() = runBlocking {
        seed(device("miot_1", "灯1"), device("miot_2", "灯2"))
        executeResult = { id ->
            if (id == "miot_2") {
                DeviceActionResult.failure(DeviceErrorKind.OFFLINE, -704042011, "设备当前离线")
            } else {
                DeviceActionResult.ok()
            }
        }

        val result = executor().executeHomeControl(
            plan("group_all_lights", params = mapOf("is_group" to true, "device_ids" to listOf("miot_1", "miot_2"))),
            isLive = true,
        )

        assertEquals(RunStatus.PARTIAL, result.status)
        assertTrue("应说明部分完成: ${result.statusMessage}", result.statusMessage.contains("部分完成"))
        assertTrue("应点名失败设备: ${result.statusMessage}", result.statusMessage.contains("灯2"))
    }

    @Test
    fun `全部失败应报失败`() = runBlocking {
        seed(device("miot_1", "灯1"), device("miot_2", "灯2"))
        executeResult = { DeviceActionResult.failure(DeviceErrorKind.OFFLINE, -704042011, "设备离线") }

        val result = executor().executeHomeControl(
            plan("group_all_lights", params = mapOf("is_group" to true, "device_ids" to listOf("miot_1", "miot_2"))),
            isLive = true,
        )

        assertEquals(RunStatus.FAILED, result.status)
    }

    @Test
    fun `逐台失败原因应分别记录`() = runBlocking {
        seed(device("miot_1", "灯1"), device("miot_2", "灯2"))
        executeResult = { DeviceActionResult.failure(DeviceErrorKind.OFFLINE, -704042011, "设备当前离线") }

        val result = executor().executeHomeControl(
            plan("group_all_lights", params = mapOf("is_group" to true, "device_ids" to listOf("miot_1", "miot_2"))),
            isLive = true,
        )

        val failureEvents = result.events.filter { !it.isSuccess && it.message.contains("离线") }
        assertEquals("每台失败都应单独记录", 2, failureEvents.size)
    }

    /** 超出单次上限时必须明说，不能让用户以为全做了。 */
    @Test
    fun `超过批量上限应截断并明确告知`() = runBlocking {
        val many = (1..35).map { device("miot_$it", "灯$it") }
        seed(*many.toTypedArray())

        val result = executor().executeHomeControl(
            plan("group_all_lights", params = mapOf("is_group" to true, "device_ids" to many.map { it.logicalId })),
            isLive = true,
        )

        assertEquals("只应下发 30 台", 30, calls.count { it.startsWith("execute:") })
        assertTrue(
            "应告知有设备未下发: ${result.events.map { it.message }}",
            result.events.any { it.message.contains("未下发") },
        )
        assertEquals(RunStatus.PARTIAL, result.status)
    }

    @Test
    fun `组内不存在的设备应被跳过并告知`() = runBlocking {
        seed(device("miot_1"))
        calls.clear()

        val result = executor().executeHomeControl(
            plan("group_all_lights", params = mapOf("is_group" to true, "device_ids" to listOf("miot_1", "miot_幽灵"))),
            isLive = true,
        )

        assertEquals(1, calls.count { it.startsWith("execute:") })
        assertTrue(result.events.any { it.message.contains("跳过") })
    }

    // ---------------- 状态流转 ----------------

    @Test
    fun `观察模式的终态不应是成功`() = runBlocking {
        seed(device("miot_1"))
        val result = executor().executeHomeControl(plan("miot_1"), isLive = false)
        assertFalse(
            "观察模式不应报成功，否则会被灵动岛当成真实执行",
            result.status == RunStatus.SUCCEEDED,
        )
    }

    @Test
    fun `执行过程中应记录事件流`() = runBlocking {
        seed(device("miot_1"))
        val result = executor().executeHomeControl(plan("miot_1"), isLive = true)
        assertTrue("应至少有校验与下发两条事件", result.events.size >= 2)
    }
}
