package com.jev.assistant.miot

import com.jev.assistant.device.DeviceErrorKind

/**
 * 米家云错误码归类。
 *
 * 错误码数值属于协议事实；此处的中文文案由本项目自行撰写，用于界面展示与日志。
 * 归类结果使用设备层的 [DeviceErrorKind]，使执行链路不依赖米家这一具体实现。
 */
data class MiotFailure(val kind: DeviceErrorKind, val message: String)

object MiotErrorMapper {

    /** 判断业务码是否表示成功。 */
    fun isSuccess(code: Int): Boolean = code in MiotConfig.SUCCESS_CODES

    /**
     * 把业务码映射为归类与中文说明。
     *
     * @param code 服务端返回的 `code`
     * @param serverMessage 服务端原始消息，仅在未能识别时作为兜底展示
     */
    fun map(code: Int, serverMessage: String? = null): MiotFailure {
        val kind = when (code) {
            -704042011 -> DeviceErrorKind.OFFLINE
            -704042001, -704090001 -> DeviceErrorKind.NOT_FOUND
            -704040003, -704040005 -> DeviceErrorKind.PROP_MISSING
            -704030023 -> DeviceErrorKind.READ_ONLY
            -704220043 -> DeviceErrorKind.BAD_VALUE
            -704012906 -> DeviceErrorKind.AUTH
            -704083036 -> DeviceErrorKind.TIMEOUT
            else -> DeviceErrorKind.UNKNOWN
        }
        return MiotFailure(kind, describe(kind, code, serverMessage))
    }

    /** 鉴权失效的统一构造，供多处复用。 */
    fun authRequired(message: String = "授权已失效，请重新绑定米家账号"): MiotFailure =
        MiotFailure(DeviceErrorKind.AUTH, message)

    private fun describe(kind: DeviceErrorKind, code: Int, serverMessage: String?): String = when (kind) {
        DeviceErrorKind.OFFLINE -> "设备当前离线，请确认已通电并联网"
        DeviceErrorKind.NOT_FOUND -> "设备未找到，可能需要重新同步设备列表"
        DeviceErrorKind.PROP_MISSING -> "该设备不支持此功能"
        DeviceErrorKind.READ_ONLY -> "该功能为只读，无法设置"
        DeviceErrorKind.BAD_VALUE -> "设置的值超出设备允许范围"
        DeviceErrorKind.AUTH -> "授权已失效，请重新绑定米家账号"
        DeviceErrorKind.TIMEOUT -> "设备响应超时，请稍后重试"
        DeviceErrorKind.UNKNOWN -> serverMessage?.takeIf { it.isNotBlank() }
            ?: "控制失败（错误码 $code）"
    }
}
