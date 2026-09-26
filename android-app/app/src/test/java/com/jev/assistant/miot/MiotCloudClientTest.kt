package com.jev.assistant.miot
import com.jev.assistant.device.DeviceErrorKind
import com.jev.assistant.device.isRetryable

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 云客户端的请求构造与响应处理。
 *
 * 用 MockWebServer 观察真实发出的报文。服务端响应刻意返回**明文 JSON**
 * （客户端会在识别到明文时跳过敏捷解密），从而无需持有会话密钥即可覆盖响应解析路径。
 */
class MiotCloudClientTest {

    private lateinit var server: MockWebServer
    private var token: String? = "test-token"
    private var refreshCalls = 0

    private fun client(refreshTo: String? = "refreshed-token") = MiotCloudClient(
        credentialsProvider = { MiotCredentials() },
        accessTokenProvider = { token },
        onUnauthorized = {
            refreshCalls++
            refreshTo.also { token = it }
        },
        baseUrlOverride = server.url("/").toString().trimEnd('/'),
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        token = "test-token"
        refreshCalls = 0
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueJson(body: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
    }

    // ---------------- 请求构造 ----------------

    @Test
    fun `业务请求应携带全部协议要求的请求头`() = runBlocking {
        enqueueJson("""{"code":0,"result":{"homelist":[]}}""")
        client().getHomes().getOrThrow()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/app/v2/homeroom/gethome", request.path)
        assertEquals("text/plain", request.getHeader("Content-Type"))
        assertEquals("mico/docker", request.getHeader("User-Agent"))
        assertEquals("micoapi", request.getHeader("X-Client-BizId"))
        assertEquals("1", request.getHeader("X-Encrypt-Type"))
        assertEquals(MiotCredentials.DEFAULT_CLIENT_ID, request.getHeader("X-Client-AppId"))
        assertNotNull(request.getHeader("X-Client-Secret"))
    }

    /**
     * 协议要求 `Bearer` 与令牌之间没有空格，这是最容易写错的一处。
     */
    @Test
    fun `Authorization头应为无空格的Bearer`() = runBlocking {
        enqueueJson("""{"code":0,"result":{"homelist":[]}}""")
        client().getHomes().getOrThrow()

        val auth = server.takeRequest().getHeader("Authorization")
        assertEquals("Bearer 与令牌之间不应有空格", "Bearertest-token", auth)
    }

    @Test
    fun `客户端密钥应为2048位RSA密文的base64`() = runBlocking {
        enqueueJson("""{"code":0,"result":{"homelist":[]}}""")
        client().getHomes().getOrThrow()

        val secret = server.takeRequest().getHeader("X-Client-Secret")!!
        assertEquals(344, secret.length)
    }

    @Test
    fun `请求体应为AES密文而非明文JSON`() = runBlocking {
        enqueueJson("""{"code":0,"result":{"homelist":[]}}""")
        client().getHomes().getOrThrow()

        val body = server.takeRequest().body.readUtf8()
        assertFalse("请求体不应是明文 JSON", MiotCrypto.looksLikePlainJson(body))
        // AES 分组长度为 16 字节，base64 解码后必须是其整数倍
        val raw = java.util.Base64.getMimeDecoder().decode(body)
        assertEquals("密文长度应为 AES 分组整数倍", 0, raw.size % 16)
    }

    @Test
    fun `读属性请求应带 datasource 字段且为POST`() = runBlocking {
        enqueueJson("""{"code":0,"result":[]}""")
        client().getProps(listOf(MiotPropQuery("2026857030", 2, 1)))

        val request = server.takeRequest()
        assertEquals("/app/v2/miotspec/prop/get", request.path)
        assertEquals("POST", request.method)
    }

    @Test
    fun `写属性应打到 set 端点`() = runBlocking {
        enqueueJson("""{"code":0,"result":[]}""")
        client().setProps(listOf(MiotPropWrite("2026857030", 2, 1, true)))

        assertEquals("/app/v2/miotspec/prop/set", server.takeRequest().path)
    }

    @Test
    fun `设备列表应打到 device_list_page 端点`() = runBlocking {
        enqueueJson("""{"code":0,"result":{"list":[],"has_more":false}}""")
        client().getDeviceListPage(listOf("2026857030"))

        assertEquals("/app/v2/home/device_list_page", server.takeRequest().path)
    }

    // ---------------- 响应处理 ----------------

    @Test
    fun `应解析出设备列表`() = runBlocking {
        enqueueJson(
            """
            {"code":0,"result":{"list":[
              {"did":"2026857030","name":"客厅灯","spec_type":"urn:miot-spec-v2:device:light:0000A001:v:1",
               "model":"yeelink.light.ceiling1","isOnline":true,"local_ip":"192.168.1.5"}
            ],"has_more":false}}
            """.trimIndent(),
        )
        val page = client().getDeviceListPage(listOf("2026857030")).getOrThrow()
        val devices = page.list

        assertEquals(1, devices.size)
        assertEquals(false, page.hasMore)
        assertEquals("2026857030", devices[0].did)
        assertEquals("客厅灯", devices[0].name)
        assertEquals("yeelink.light.ceiling1", devices[0].model)
        assertTrue(devices[0].isOnline)
        assertEquals("192.168.1.5", devices[0].localIp)
        assertEquals("light", MiotSpecParser.typeNameOf(devices[0].specType!!))
    }

    @Test
    fun `业务错误码应转换为失败并归类`() = runBlocking {
        enqueueJson("""{"code":-704042011,"message":"device offline"}""")
        val result = client().getHomes()

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as MiotCloudException
        assertEquals(DeviceErrorKind.OFFLINE, error.failure.kind)
    }

    @Test
    fun `鉴权失败码应归类为鉴权`() = runBlocking {
        enqueueJson("""{"code":-704012906,"message":"auth failed"}""")
        val error = client().getHomes().exceptionOrNull() as MiotCloudException
        assertEquals(DeviceErrorKind.AUTH, error.failure.kind)
    }

    // ---------------- 鉴权续期 ----------------

    @Test
    fun `未绑定时应直接失败且不发请求`() = runBlocking {
        token = null
        val result = client().getHomes()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("绑定"))
        assertEquals("不应发出网络请求", 0, server.requestCount)
    }

    @Test
    fun `收到401应刷新令牌并重试一次`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        enqueueJson("""{"code":0,"result":{"homelist":[]}}""")

        client().getHomes().getOrThrow()

        assertEquals("应触发一次续期", 1, refreshCalls)
        assertEquals("应发出两次请求", 2, server.requestCount)
        assertEquals("首次应带旧令牌", "Bearertest-token", server.takeRequest().getHeader("Authorization"))
        assertEquals("重试应带新令牌", "Bearerrefreshed-token", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `刷新令牌失败时应报鉴权错误`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = client(refreshTo = null).getHomes()

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as MiotCloudException
        assertEquals(DeviceErrorKind.AUTH, error.failure.kind)
    }

    @Test
    fun `服务端5xx应归类为可重试的超时`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        val error = client().getHomes().exceptionOrNull() as MiotCloudException
        assertTrue(error.failure.kind.isRetryable())
    }

    // ---------------- 换令牌 ----------------

    @Test
    fun `换令牌应使用GET且参数置于data查询串`() = runBlocking {
        enqueueJson("""{"code":0,"result":{"access_token":"a","refresh_token":"r","expires_in":3600}}""")
        val result = client().requestToken(mapOf("client_id" to "cid", "code" to "the-code"))

        val request: RecordedRequest = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue("路径应包含 get_token", request.path!!.contains("/app/v2/mico/oauth/get_token"))
        assertTrue("参数应置于 data 查询串", request.path!!.contains("data="))
        // 令牌接口的成功码是 0（非 HTTP 200）
        assertEquals(0, result.getOrThrow().code)
        assertEquals("a", result.getOrThrow().result!!.accessToken)
    }

    @Test
    fun `换令牌响应为明文JSON且不需解密`() = runBlocking {
        enqueueJson("""{"code":0,"result":{"access_token":"tok","refresh_token":"ref","expires_in":7200}}""")
        val envelope = client().requestToken(mapOf("code" to "x")).getOrThrow()

        assertEquals("tok", envelope.result!!.accessToken)
        assertEquals("ref", envelope.result!!.refreshToken)
        assertEquals(7200, envelope.result!!.expiresIn)
    }
}
