package com.jev.assistant.miot.auth

import com.jev.assistant.miot.MiotCloudClient
import com.jev.assistant.miot.MiotCredentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

/**
 * 绑定、解绑与自动续期。
 *
 * 用内存假存储 + MockWebServer，不依赖 Android 环境。
 */
class MiotAccountManagerTest {

    private lateinit var server: MockWebServer
    private lateinit var store: FakeStore
    private val bindingEvents = mutableListOf<Boolean>()

    private class FakeStore(var uuidValue: String = FIXED_UUID) : MiotAuthStore {
        var saved: MiotAuthState? = null
        var clearCalled = false

        override fun uuid(): String = uuidValue
        override fun load(): MiotAuthState? = saved
        override fun save(state: MiotAuthState) {
            saved = state
        }

        override fun clear() {
            saved = null
            clearCalled = true
        }
    }

    /**
     * 与 [FIXED_UUID] 对应的合法 state。
     * Python: hashlib.sha1("d=mico.$FIXED_UUID").hexdigest()
     */
    private val validState = VALID_STATE

    private fun manager(): MiotAccountManager {
        val credentials = { MiotCredentials() }
        val cloud = MiotCloudClient(
            credentialsProvider = credentials,
            accessTokenProvider = { null },
            baseUrlOverride = server.url("/").toString().trimEnd('/'),
        )
        return MiotAccountManager(
            store = store,
            oauth = MiotOAuthClient(credentials, cloud),
            credentialsProvider = credentials,
            onBindingChanged = { bindingEvents += it },
        )
    }

    private fun enqueueToken(access: String, refresh: String, expiresIn: Long) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"code":0,"result":{"access_token":"$access","refresh_token":"$refresh","expires_in":$expiresIn}}""",
            ),
        )
    }

    private fun base64Payload(code: String, state: String): String =
        Base64.getEncoder().encodeToString("""{"code":"$code","state":"$state"}""".toByteArray())

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        store = FakeStore()
        bindingEvents.clear()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ---------------- 状态 ----------------

    @Test
    fun `未绑定时状态应为未绑定`() {
        val manager = manager()
        manager.initialize()
        assertEquals(MiotBindStatus.Unbound, manager.status.value)
        assertNull(manager.currentAccessToken())
    }

    @Test
    fun `存储中有有效令牌时初始化应为已绑定`() {
        store.saved = MiotAuthState(
            accessToken = "tok",
            refreshToken = "ref",
            expiresAtSec = nowSec() + 3600,
            nickname = "小亦",
        )
        val manager = manager()
        manager.initialize()

        val status = manager.status.value
        assertTrue(status is MiotBindStatus.Bound)
        assertEquals("小亦", (status as MiotBindStatus.Bound).nickname)
        assertEquals("tok", manager.currentAccessToken())
    }

    @Test
    fun `存储中令牌已过期时初始化应为已过期`() {
        store.saved = MiotAuthState("tok", "ref", expiresAtSec = nowSec() - 10)
        val manager = manager()
        manager.initialize()
        assertTrue(manager.status.value is MiotBindStatus.Expired)
    }

    // ---------------- 绑定 ----------------

    @Test
    fun `合法回执应完成绑定并持久化`() = runBlocking {
        enqueueToken("new-access", "new-refresh", 3600)
        val manager = manager()
        manager.initialize()

        val state = manager.bind(base64Payload("the-code", validState)).getOrThrow()

        assertEquals("new-access", state.accessToken)
        assertEquals("new-refresh", state.refreshToken)
        assertEquals("new-access", store.saved?.accessToken)
        assertEquals("new-refresh", store.saved?.refreshToken)
        assertTrue(manager.status.value is MiotBindStatus.Bound)
        assertEquals("new-access", manager.currentAccessToken())
    }

    @Test
    fun `绑定成功应发出已绑定事件以触发安全动作`() = runBlocking {
        enqueueToken("a", "r", 3600)
        val manager = manager()
        manager.initialize()

        manager.bind(base64Payload("code", validState)).getOrThrow()

        assertEquals(listOf(true), bindingEvents)
    }

    /** state 不匹配意味着回执可能被伪造，必须拒绝且不发任何网络请求。 */
    @Test
    fun `state不匹配应拒绝绑定且不发起请求`() = runBlocking {
        val manager = manager()
        manager.initialize()

        val result = manager.bind(base64Payload("code", "wrong-state"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("校验失败"))
        assertNull(store.saved)
        assertEquals("不应发起网络请求", 0, server.requestCount)
        assertTrue(bindingEvents.isEmpty())
    }

    @Test
    fun `格式错误的回执应被拒绝`() = runBlocking {
        val manager = manager()
        manager.initialize()

        val result = manager.bind("这不是一个回执")

        assertTrue(result.isFailure)
        assertNull(store.saved)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `换令牌失败时不应留下绑定状态`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":-1,"message":"bad code"}"""))
        val manager = manager()
        manager.initialize()

        val result = manager.bind(base64Payload("bad-code", validState))

        assertTrue(result.isFailure)
        assertNull(store.saved)
        assertTrue(bindingEvents.isEmpty())
    }

    // ---------------- 解绑 ----------------

    @Test
    fun `解绑应清除令牌并发出事件`() {
        store.saved = MiotAuthState("tok", "ref", nowSec() + 3600)
        val manager = manager()
        manager.initialize()

        manager.unbind()

        assertTrue(store.clearCalled)
        assertEquals(MiotBindStatus.Unbound, manager.status.value)
        assertNull(manager.currentAccessToken())
        assertEquals(listOf(false), bindingEvents)
    }

    @Test
    fun `解绑不应清除uuid`() {
        val manager = manager()
        manager.initialize()
        manager.unbind()
        // uuid 决定 state 推导，解绑后重新绑定仍需一致
        assertEquals(FIXED_UUID, manager.currentUuid())
    }

    // ---------------- 续期 ----------------

    @Test
    fun `令牌有效时续期不应发请求`() = runBlocking {
        store.saved = MiotAuthState("tok", "ref", nowSec() + 3600)
        val manager = manager()
        manager.initialize()

        val state = manager.ensureFresh().getOrThrow()

        assertEquals("tok", state.accessToken)
        assertEquals("不应发起网络请求", 0, server.requestCount)
    }

    @Test
    fun `令牌即将过期时应自动续期`() = runBlocking {
        // 距过期仅 10 秒，小于 60 秒的安全提前量
        store.saved = MiotAuthState("old", "ref", nowSec() + 10)
        enqueueToken("fresh", "ref2", 3600)
        val manager = manager()
        manager.initialize()

        val state = manager.ensureFresh().getOrThrow()

        assertEquals("fresh", state.accessToken)
        assertEquals(1, server.requestCount)
        assertEquals("fresh", manager.currentAccessToken())
        assertEquals("fresh", store.saved?.accessToken)
    }

    @Test
    fun `强制续期应无视本地有效期`() = runBlocking {
        store.saved = MiotAuthState("old", "ref", nowSec() + 99999)
        enqueueToken("fresh", "ref2", 3600)
        val manager = manager()
        manager.initialize()

        val state = manager.ensureFresh(force = true).getOrThrow()

        assertEquals("fresh", state.accessToken)
        assertEquals(1, server.requestCount)
    }

    /** 续期响应有时不回传 refresh_token，此时必须沿用旧的，否则下次续期会失败。 */
    @Test
    fun `续期未返回新的refresh_token时应沿用旧的`() = runBlocking {
        store.saved = MiotAuthState("old", "keep-me", nowSec() + 10)
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"code":0,"result":{"access_token":"fresh","refresh_token":"","expires_in":3600}}"""),
        )
        val manager = manager()
        manager.initialize()

        val state = manager.ensureFresh().getOrThrow()

        assertEquals("fresh", state.accessToken)
        assertEquals("keep-me", state.refreshToken)
        assertEquals("keep-me", store.saved?.refreshToken)
    }

    @Test
    fun `未绑定时续期应失败`() = runBlocking {
        val manager = manager()
        manager.initialize()

        val result = manager.ensureFresh()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("绑定"))
    }

    @Test
    fun `续期失败且令牌已过期时应切换为已过期状态`() = runBlocking {
        store.saved = MiotAuthState("old", "ref", nowSec() - 5)
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":-1,"message":"invalid"}"""))
        val manager = manager()
        manager.initialize()

        assertTrue(manager.ensureFresh().isFailure)
        assertTrue(manager.status.value is MiotBindStatus.Expired)
    }

    @Test
    fun `到期时间应按协议比例折算`() = runBlocking {
        enqueueToken("a", "r", 3600)
        val manager = manager()
        manager.initialize()
        val before = nowSec()

        val state = manager.bind(base64Payload("code", validState)).getOrThrow()

        // 3600 * 0.7 = 2520
        val expected = before + 2520
        assertTrue(
            "到期时间应约为 $expected，实际 ${state.expiresAtSec}",
            kotlin.math.abs(state.expiresAtSec - expected) <= 2,
        )
    }

    @Test
    fun `昵称应可单独更新且不影响令牌`() {
        store.saved = MiotAuthState("tok", "ref", nowSec() + 3600)
        val manager = manager()
        manager.initialize()

        manager.updateNickname("小米用户")

        assertEquals("tok", manager.currentAccessToken())
        assertEquals("小米用户", store.saved?.nickname)
    }

    @Test
    fun `空白昵称应被忽略`() {
        store.saved = MiotAuthState("tok", "ref", nowSec() + 3600, nickname = "原昵称")
        val manager = manager()
        manager.initialize()

        manager.updateNickname("   ")

        assertEquals("原昵称", store.saved?.nickname)
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private companion object {
        const val FIXED_UUID = "test-uuid-1234"
        const val VALID_STATE = "464fb5473ea4a8a1e2cb102effab1d8507584993"
    }
}
