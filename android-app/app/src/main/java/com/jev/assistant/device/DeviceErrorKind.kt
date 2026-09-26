package com.jev.assistant.device

/**
 * 设备操作失败的归类。
 *
 * 定义在设备层而非具体接入实现里：这些分类（离线、只读、越界……）
 * 与「哪家云」无关，是执行链路判断「是否可重试」「如何向用户解释」的共同依据。
 */
enum class DeviceErrorKind {
    /** 设备离线，未联网或断电。 */
    OFFLINE,

    /** 设备不存在，标识失效或不属于当前账号。 */
    NOT_FOUND,

    /** 设备没有该属性或功能。 */
    PROP_MISSING,

    /** 属性只读，不允许写入。 */
    READ_ONLY,

    /** 参数值不合法（越界、类型不符、枚举不存在）。 */
    BAD_VALUE,

    /** 鉴权失败，需要重新绑定。 */
    AUTH,

    /** 操作超时。 */
    TIMEOUT,

    /** 未被识别的错误。 */
    UNKNOWN,
}

/** 该错误是否值得重试。离线与超时可能是暂时性的；参数错误重试无意义。 */
fun DeviceErrorKind.isRetryable(): Boolean =
    this == DeviceErrorKind.TIMEOUT || this == DeviceErrorKind.OFFLINE
