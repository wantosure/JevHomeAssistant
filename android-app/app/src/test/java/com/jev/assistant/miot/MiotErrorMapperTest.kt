package com.jev.assistant.miot
import com.jev.assistant.device.DeviceErrorKind
import com.jev.assistant.device.isRetryable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiotErrorMapperTest {

    @Test
    fun `三个成功码都应判为成功`() {
        assertTrue(MiotErrorMapper.isSuccess(0))
        assertTrue(MiotErrorMapper.isSuccess(-702000000))
        assertTrue(MiotErrorMapper.isSuccess(-702010000))
        assertFalse(MiotErrorMapper.isSuccess(-704042011))
    }

    @Test
    fun `设备离线码应归类为离线`() {
        assertEquals(DeviceErrorKind.OFFLINE, MiotErrorMapper.map(-704042011).kind)
    }

    @Test
    fun `设备未找到码应归类为不存在`() {
        assertEquals(DeviceErrorKind.NOT_FOUND, MiotErrorMapper.map(-704042001).kind)
        assertEquals(DeviceErrorKind.NOT_FOUND, MiotErrorMapper.map(-704090001).kind)
    }

    @Test
    fun `属性缺失与只读应分别归类`() {
        assertEquals(DeviceErrorKind.PROP_MISSING, MiotErrorMapper.map(-704040003).kind)
        assertEquals(DeviceErrorKind.PROP_MISSING, MiotErrorMapper.map(-704040005).kind)
        assertEquals(DeviceErrorKind.READ_ONLY, MiotErrorMapper.map(-704030023).kind)
    }

    @Test
    fun `参数越界应归类为取值错误`() {
        assertEquals(DeviceErrorKind.BAD_VALUE, MiotErrorMapper.map(-704220043).kind)
    }

    @Test
    fun `鉴权失效应归类为鉴权`() {
        assertEquals(DeviceErrorKind.AUTH, MiotErrorMapper.map(-704012906).kind)
    }

    @Test
    fun `超时码应归类为超时`() {
        assertEquals(DeviceErrorKind.TIMEOUT, MiotErrorMapper.map(-704083036).kind)
    }

    @Test
    fun `未知码应回落到服务端消息`() {
        val failure = MiotErrorMapper.map(-999999, "服务端自定义说明")
        assertEquals(DeviceErrorKind.UNKNOWN, failure.kind)
        assertEquals("服务端自定义说明", failure.message)
    }

    @Test
    fun `未知码无服务端消息时应带上错误码`() {
        val failure = MiotErrorMapper.map(-999999)
        assertTrue(failure.message.contains("-999999"))
    }

    @Test
    fun `每条已知错误都应有非空中文说明`() {
        val codes = listOf(
            -704042011, -704042001, -704090001, -704040003,
            -704040005, -704030023, -704220043, -704012906, -704083036,
        )
        codes.forEach { code ->
            val failure = MiotErrorMapper.map(code)
            assertTrue("错误码 $code 缺少说明", failure.message.isNotBlank())
        }
    }

    /**
     * 只有超时与离线值得重试；参数错误、只读、鉴权失败重试没有意义，
     * 无限重试反而会放大对米家云的请求量。
     */
    @Test
    fun `仅离线与超时标记为可重试`() {
        assertTrue(DeviceErrorKind.OFFLINE.isRetryable())
        assertTrue(DeviceErrorKind.TIMEOUT.isRetryable())
        assertFalse(DeviceErrorKind.BAD_VALUE.isRetryable())
        assertFalse(DeviceErrorKind.READ_ONLY.isRetryable())
        assertFalse(DeviceErrorKind.AUTH.isRetryable())
        assertFalse(DeviceErrorKind.NOT_FOUND.isRetryable())
    }
}
