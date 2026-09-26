package com.jev.assistant.miot.auth

import com.jev.assistant.miot.MiotConfig

/**
 * 已持久化的授权状态。
 *
 * 时间统一用「秒级 Unix 时间戳」，与协议中的 `expires_ts` 保持一致。
 */
data class MiotAuthState(
    val accessToken: String,
    val refreshToken: String,
    /** 访问令牌的本地到期时间（已按协议比例折算，比服务端实际有效期提前）。 */
    val expiresAtSec: Long,
    val nickname: String? = null,
    val boundAtSec: Long = 0L,
    val lastRefreshedAtSec: Long = 0L,
) {
    /**
     * 令牌是否仍可用。
     *
     * 留出 [skewSec] 的提前量，避免请求刚好卡在过期瞬间。
     */
    fun isValid(nowSec: Long, skewSec: Long = MiotConfig.TOKEN_REFRESH_SKEW_SEC): Boolean =
        accessToken.isNotBlank() && expiresAtSec > nowSec + skewSec

    /** 下次建议续期的时间点。 */
    fun nextRefreshAtSec(skewSec: Long = MiotConfig.TOKEN_REFRESH_SKEW_SEC): Long =
        expiresAtSec - skewSec

    companion object {
        /**
         * 由协议返回的 `expires_in` 推算本地到期时间。
         *
         * 协议约定按比例提前到期（0.7），以便留出续期窗口。
         */
        fun expiresAtFrom(nowSec: Long, expiresInSec: Long): Long =
            nowSec + (expiresInSec * MiotConfig.TOKEN_EXPIRES_RATIO).toLong()
    }
}

/**
 * 绑定状态，供界面展示。
 */
sealed interface MiotBindStatus {
    /** 尚未绑定小米账号。 */
    data object Unbound : MiotBindStatus

    /** 已绑定且令牌有效。 */
    data class Bound(
        val nickname: String?,
        val expiresAtSec: Long,
        val nextRefreshAtSec: Long,
        val boundAtSec: Long,
    ) : MiotBindStatus

    /** 已绑定但令牌已过期且续期失败，需要重新授权。 */
    data class Expired(val reason: String) : MiotBindStatus
}
