package com.jev.assistant.miot.auth

import com.jev.assistant.miot.MiotCloudClient
import com.jev.assistant.miot.MiotCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

/**
 * 授权链接与 state 推导。
 *
 * STATE_KNOWN 由参考协议实现（Python hashlib）以固定 uuid 生成，
 * 用于确认 Kotlin 侧的 state 算法与协议逐字节一致——
 * 该值一旦不符，服务端会拒绝授权，且错误现象很难定位。
 */
class MiotOAuthClientTest {

    private val client = MiotOAuthClient(
        credentialsProvider = { MiotCredentials() },
        cloud = MiotCloudClient(
            credentialsProvider = { MiotCredentials() },
            accessTokenProvider = { null },
        ),
    )

    @Test
    fun `设备标识应为mico前缀加uuid`() {
        assertEquals("mico.0123456789abcdef0123456789abcdef", client.deviceIdOf(FIXED_UUID))
    }

    @Test
    fun `state推导应与协议已知答案一致`() {
        assertEquals(STATE_KNOWN, client.stateOf(FIXED_UUID))
    }

    @Test
    fun `不同uuid应产生不同state`() {
        assertFalse(client.stateOf("uuid-a") == client.stateOf("uuid-b"))
    }

    @Test
    fun `state应为40位十六进制`() {
        val state = client.stateOf(FIXED_UUID)
        assertEquals(40, state.length)
        assertTrue("state 应为十六进制", state.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `校验函数应只接受匹配的state`() {
        assertTrue(client.verifyState(FIXED_UUID, STATE_KNOWN))
        assertFalse(client.verifyState(FIXED_UUID, "伪造的state"))
        assertFalse(client.verifyState("其它uuid", STATE_KNOWN))
    }

    @Test
    fun `授权链接应包含协议要求的全部参数`() {
        val url = client.buildAuthUrl(FIXED_UUID)
        val query = url.substringAfter('?')

        assertTrue("应指向小米授权页", url.startsWith("https://account.xiaomi.com/oauth2/authorize?"))
        assertEquals("code", param(query, "response_type"))
        assertEquals(MiotCredentials.DEFAULT_CLIENT_ID, param(query, "client_id"))
        assertEquals("mico.$FIXED_UUID", param(query, "device_id"))
        assertEquals(STATE_KNOWN, param(query, "state"))
        assertEquals(MiotConfigRedirect.redirectUri, param(query, "redirect_uri"))
    }

    @Test
    fun `授权链接的redirect应指向官方回调展示页`() {
        val url = client.buildAuthUrl(FIXED_UUID)
        assertEquals(
            "https://mico.api.mijia.tech/login_redirect",
            param(url.substringAfter('?'), "redirect_uri"),
        )
    }

    @Test
    fun `切换区域后授权链接的设备标识不变`() {
        val eu = MiotOAuthClient(
            credentialsProvider = { MiotCredentials(region = com.jev.assistant.miot.MiotConfig.CloudRegion.DE) },
            cloud = MiotCloudClient(
                credentialsProvider = { MiotCredentials() },
                accessTokenProvider = { null },
            ),
        )
        // 授权始终在 account.xiaomi.com，与区域无关；device_id 也不应因区域而变
        assertEquals(client.buildAuthUrl(FIXED_UUID), eu.buildAuthUrl(FIXED_UUID))
    }

    private fun param(query: String, key: String): String? =
        query.split('&')
            .mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
            }
            .firstOrNull { it.first == key }
            ?.second
            ?.let { URLDecoder.decode(it, "UTF-8") }

    private object MiotConfigRedirect {
        const val redirectUri = "https://mico.api.mijia.tech/login_redirect"
    }

    private companion object {
        const val FIXED_UUID = "0123456789abcdef0123456789abcdef"
        /** Python: hashlib.sha1("d=mico.0123456789abcdef0123456789abcdef").hexdigest() */
        const val STATE_KNOWN = "dba692f9559c4c090a52d4869a9c8d900e59490d"
    }
}
