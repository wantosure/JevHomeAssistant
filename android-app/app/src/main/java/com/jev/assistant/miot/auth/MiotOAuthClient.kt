package com.jev.assistant.miot.auth

import com.jev.assistant.miot.MiotCloudClient
import com.jev.assistant.miot.MiotConfig
import com.jev.assistant.miot.MiotCredentials
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * 小米账号 OAuth 授权。
 *
 * 流程（协议事实）：
 *  1. 本地生成 uuid，`device_id` 为 `mico.{uuid}`；
 *  2. `state` 为 `SHA1("d=mico.{uuid}")` 的十六进制串，用于校验回执来源；
 *  3. 授权页完成登录后跳转到固定的回调展示页，页面给出可复制的 code 与 state；
 *  4. 用 code 换令牌，或日后用 refresh_token 续期。
 *
 * 换令牌接口不使用加密信封，且参数形态特殊，详见 [MiotCloudClient.requestToken]。
 */
class MiotOAuthClient(
    private val credentialsProvider: () -> MiotCredentials,
    private val cloud: MiotCloudClient,
) {

    /**
     * 由 uuid 推导设备标识。
     */
    fun deviceIdOf(uuid: String): String = "${MiotConfig.PROJECT_CODE}.$uuid"

    /**
     * 由 uuid 推导 state。协议固定算法，双方必须一致。
     */
    fun stateOf(uuid: String): String = sha1Hex("d=${deviceIdOf(uuid)}")

    /**
     * 生成授权页链接。
     *
     * `skip_confirm` 取 `false`：与协议实现一致，让用户每次都能看到授权确认页，
     * 便于确认自己登录的是哪个小米账号。
     */
    fun buildAuthUrl(uuid: String): String {
        val credentials = credentialsProvider()
        val params = linkedMapOf(
            "redirect_uri" to credentials.redirectUri,
            "client_id" to credentials.clientId,
            "response_type" to "code",
            "device_id" to deviceIdOf(uuid),
            "state" to stateOf(uuid),
            "skip_confirm" to "false",
        )
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        return "${MiotConfig.AUTH_URL}?$query"
    }

    /** 校验回执中的 state 是否与本地推导的一致。 */
    fun verifyState(uuid: String, returnedState: String): Boolean =
        stateOf(uuid) == returnedState

    /**
     * 用授权码换取令牌。
     *
     * 注意参数中没有 `grant_type`，换码与续期靠字段组合区分。
     */
    suspend fun exchangeCode(uuid: String, code: String): Result<MiotAuthState> {
        val credentials = credentialsProvider()
        return cloud.requestToken(
            mapOf(
                "client_id" to credentials.clientId,
                "redirect_uri" to credentials.redirectUri,
                "code" to code,
                "device_id" to deviceIdOf(uuid),
            ),
        ).mapCatching { envelope -> envelope.toAuthState(nowSec = nowSec()) }
    }

    /**
     * 用 refresh_token 续期。
     */
    suspend fun refresh(refreshToken: String): Result<MiotAuthState> {
        val credentials = credentialsProvider()
        return cloud.requestToken(
            mapOf(
                "client_id" to credentials.clientId,
                "redirect_uri" to credentials.redirectUri,
                "refresh_token" to refreshToken,
            ),
        ).mapCatching { envelope ->
            envelope.toAuthState(nowSec = nowSec(), fallbackRefreshToken = refreshToken)
        }
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private fun sha1Hex(input: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun com.jev.assistant.miot.MiotTokenEnvelopeDto.toAuthState(
        nowSec: Long,
        fallbackRefreshToken: String? = null,
    ): MiotAuthState {
        val body = result ?: throw IllegalStateException("换令牌响应缺少 result")
        // 部分续期响应不回传 refresh_token，此时沿用旧的
        val effectiveRefresh = body.refreshToken.takeIf { it.isNotBlank() }
            ?: fallbackRefreshToken
            ?: throw IllegalStateException("换令牌响应缺少 refresh_token")
        if (body.accessToken.isBlank()) throw IllegalStateException("换令牌响应缺少 access_token")

        return MiotAuthState(
            accessToken = body.accessToken,
            refreshToken = effectiveRefresh,
            expiresAtSec = MiotAuthState.expiresAtFrom(nowSec, body.expiresIn),
            boundAtSec = nowSec,
            lastRefreshedAtSec = nowSec,
        )
    }
}
