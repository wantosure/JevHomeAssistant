package com.jev.assistant.miot

/**
 * 米家云（MIoT）接入常量与可配置凭据。
 *
 * 本文件只承载**协议事实**（端点、请求头名、互操作标识），不含任何来自第三方 SDK 的实现代码。
 * 详见 docs/miot-integration.md 的许可证边界说明。
 */
object MiotConfig {

    /** 项目标识，参与主机名与请求头构造。 */
    const val PROJECT_CODE = "mico"

    /** 用户代理，服务端会校验。 */
    const val USER_AGENT = "$PROJECT_CODE/docker"

    /** 业务标识请求头值。 */
    const val X_CLIENT_BIZID = "${PROJECT_CODE}api"

    /** 加密类型标识，固定为 1（AES 信封）。 */
    const val X_ENCRYPT_TYPE = "1"

    /** 小米账号授权页。 */
    const val AUTH_URL = "https://account.xiaomi.com/oauth2/authorize"

    /** 回调展示页。小米只注册了这一个回调地址，页面会展示可复制的授权码。 */
    const val REDIRECT_URI_DEFAULT = "https://$PROJECT_CODE.api.mijia.tech/login_redirect"

    /** 设备列表分页大小。 */
    const val DEVICE_PAGE_LIMIT = 200

    /** 家庭/房间分页大小。 */
    const val HOME_PAGE_LIMIT = 150

    /** 单次属性读取的最大参数数。 */
    const val PROP_GET_MAX = 150

    /** 令牌到期前多少秒开始续期。 */
    const val TOKEN_REFRESH_SKEW_SEC = 60L

    /** 服务端返回 `expires_in` 后按此比例折算本地到期时间（与协议一致）。 */
    const val TOKEN_EXPIRES_RATIO = 0.7

    /** 网络超时。 */
    const val CONNECT_TIMEOUT_SEC = 10L
    const val READ_TIMEOUT_SEC = 20L
    const val WRITE_TIMEOUT_SEC = 10L

    /** 服务端返回的成功码。 */
    val SUCCESS_CODES = setOf(0, -702000000, -702010000)

    /**
     * 可选的米家云区域。`cn` 为默认（中国大陆），其余会形成 `{region}.mico.api.mijia.tech`。
     */
    enum class CloudRegion(val code: String, val label: String) {
        CN("cn", "中国大陆"),
        DE("de", "欧洲"),
        US("us", "美国"),
        SG("sg", "新加坡"),
        RU("ru", "俄罗斯"),
        I2("i2", "印度");

        companion object {
            fun fromCode(code: String): CloudRegion =
                entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CN
        }
    }

    /**
     * 由区域推导业务主机名。`cn` 不带前缀。
     */
    fun bizHost(region: CloudRegion): String =
        if (region == CloudRegion.CN) "$PROJECT_CODE.api.mijia.tech"
        else "${region.code}.$PROJECT_CODE.api.mijia.tech"
}

/**
 * 米家云凭据。默认值仅为互操作所需，**不是本项目所有的凭据**，
 * 用户可在设置中替换为自己的小米开放平台应用凭据。
 */
data class MiotCredentials(
    val clientId: String = DEFAULT_CLIENT_ID,
    val publicKeyPem: String = DEFAULT_PUBLIC_KEY_PEM,
    val region: MiotConfig.CloudRegion = MiotConfig.CloudRegion.CN,
    val redirectUri: String = MiotConfig.REDIRECT_URI_DEFAULT,
) {
    val bizHost: String get() = MiotConfig.bizHost(region)

    companion object {
        /**
         * 默认 client_id。来自公开的米家互操作实现，仅作默认值；
         * 使用他人注册的应用凭据存在被限流或停用的风险，建议自行到小米开放平台申请。
         */
        const val DEFAULT_CLIENT_ID = "2882303761520431603"

        /**
         * MIoT 服务端的 RSA 公钥，用于包裹每次会话随机生成的 AES 密钥。
         * 这是公开的互操作密钥材料（公钥本身不保密），在多個开源米家实现中通用。
         */
        const val DEFAULT_PUBLIC_KEY_PEM = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzH220YGgZOlXJ4eSleFb
Beylq4qHsVNzhPTUTy/caDb4a3GzqH6SX4GiYRilZZZrjjU2ckkr8GM66muaIuJw
r8ZB9SSY3Hqwo32tPowpyxobTN1brmqGK146X6JcFWK/QiUYVXZlcHZuMgXLlWyn
zTMVl2fq7wPbzZwOYFxnSRh8YEnXz6edHAqJqLEqZMP00bNFBGP+yc9xmc7ySSyw
OgW/muVzfD09P2iWhl3x8N+fBBWpuI5HjvyQuiX8CZg3xpEeCV8weaprxMxR0epM
3l7T6rJuPXR1D7yhHaEQj2+dyrZTeJO8D8SnOgzV5j4bp1dTunlzBXGYVjqDsRhZ
qQIDAQAB
-----END PUBLIC KEY-----"""
    }
}
