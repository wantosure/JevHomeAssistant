package com.jev.assistant.miot.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * 授权回执解析。用户可能粘贴三种形态，任何一种失败都会让人卡在绑定这一步，
 * 因此这里覆盖得细一些。
 */
class MiotAuthPayloadParserTest {

    private fun success(raw: String): MiotAuthPayloadParser.Result {
        val outcome = MiotAuthPayloadParser.parse(raw)
        assertTrue("应当解析成功，实际: $outcome", outcome is MiotAuthPayloadParser.Outcome.Success)
        return (outcome as MiotAuthPayloadParser.Outcome.Success).payload
    }

    private fun failureReason(raw: String): String {
        val outcome = MiotAuthPayloadParser.parse(raw)
        assertTrue("应当解析失败，实际: $outcome", outcome is MiotAuthPayloadParser.Outcome.Failure)
        return (outcome as MiotAuthPayloadParser.Outcome.Failure).reason
    }

    @Test
    fun `应解析base64编码的JSON回执`() {
        val json = """{"code":"abc123","state":"deadbeef"}"""
        val b64 = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))

        val payload = success(b64)
        assertEquals("abc123", payload.code)
        assertEquals("deadbeef", payload.state)
    }

    @Test
    fun `应解析完整回调URL`() {
        val url = "https://mico.api.mijia.tech/login_redirect?code=abc123&state=deadbeef"
        val payload = success(url)
        assertEquals("abc123", payload.code)
        assertEquals("deadbeef", payload.state)
    }

    @Test
    fun `应解析查询串片段`() {
        val payload = success("code=abc123&state=deadbeef")
        assertEquals("abc123", payload.code)
        assertEquals("deadbeef", payload.state)
    }

    @Test
    fun `URL参数顺序颠倒也应正常解析`() {
        val payload = success("https://x.test/login_redirect?state=deadbeef&code=abc123")
        assertEquals("abc123", payload.code)
        assertEquals("deadbeef", payload.state)
    }

    @Test
    fun `URL中应忽略井号片段`() {
        val payload = success("https://x.test/r?code=abc123&state=deadbeef#/done")
        assertEquals("abc123", payload.code)
        assertEquals("deadbeef", payload.state)
    }

    @Test
    fun `URL编码的state应被解码`() {
        val payload = success("https://x.test/r?code=abc123&state=a%2Bb%3Dc")
        assertEquals("a+b=c", payload.state)
    }

    @Test
    fun `空输入应给出明确提示`() {
        assertTrue(failureReason("").contains("粘贴"))
        assertTrue(failureReason("   ").contains("粘贴"))
    }

    /** 只有 code 没有 state 时无法校验回执来源，必须拒绝而不是放行。 */
    @Test
    fun `缺少state应被拒绝并说明原因`() {
        val reason = failureReason("code=abc123")
        assertTrue("应提示缺少 state，实际: $reason", reason.contains("state") || reason.contains("格式"))
    }

    @Test
    fun `裸授权码应被拒绝并提示缺少state`() {
        val reason = failureReason("abcdef0123456789abcdef0123456789")
        assertTrue("应提示缺少 state，实际: $reason", reason.contains("state"))
    }

    @Test
    fun `无法识别的文本应给出格式提示`() {
        assertTrue(failureReason("这是一段随便的文字").contains("无法识别"))
    }

    @Test
    fun `base64但非JSON的内容应被拒绝`() {
        val b64 = Base64.getEncoder().encodeToString("not a json at all!!".toByteArray())
        assertTrue(failureReason(b64).isNotEmpty())
    }

    @Test
    fun `JSON缺少code应被拒绝`() {
        val b64 = Base64.getEncoder().encodeToString("""{"state":"x"}""".toByteArray())
        assertTrue(failureReason(b64).contains("code"))
    }

    @Test
    fun `JSON缺少state应被拒绝`() {
        val b64 = Base64.getEncoder().encodeToString("""{"code":"x"}""".toByteArray())
        assertTrue(failureReason(b64).contains("state"))
    }

    @Test
    fun `首尾空白应被忽略`() {
        val payload = success("  https://x.test/r?code=abc123&state=deadbeef  ")
        assertEquals("abc123", payload.code)
    }
}
