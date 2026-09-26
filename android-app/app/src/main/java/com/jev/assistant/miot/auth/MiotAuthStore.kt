package com.jev.assistant.miot.auth

/**
 * 授权信息的存储契约。
 *
 * 抽出接口是为了让绑定/续期流程可以脱离 Android 环境测试；
 * 生产实现见 [MiotTokenStore]，它使用 Keystore 加密存储。
 */
interface MiotAuthStore {

    /** 设备标识；首次访问时生成。解绑不清除。 */
    fun uuid(): String

    /** 读取已保存的授权；未绑定时返回 null。 */
    fun load(): MiotAuthState?

    fun save(state: MiotAuthState)

    /** 清除令牌，保留 uuid。 */
    fun clear()
}
