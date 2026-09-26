package com.jev.assistant.miot.auth

import android.util.Log
import com.jev.assistant.miot.MiotCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 米家账号绑定状态的中枢。
 *
 * 负责绑定/解绑/自动续期，并向云客户端提供当前可用的访问令牌。
 * 令牌在内存中缓存，避免每次请求都读一次加密存储。
 */
class MiotAccountManager(
    private val store: MiotAuthStore,
    private val oauth: MiotOAuthClient,
    private val credentialsProvider: () -> MiotCredentials,
    /**
     * 绑定状态变化时的回调。
     *
     * 用于强制执行安全动作——**每次建立或解除绑定都必须切回观察模式**，
     * 避免用户在新的账号环境下无意间直接对真实设备下发指令。
     */
    private val onBindingChanged: (bound: Boolean) -> Unit = {},
) {

    private val _status = MutableStateFlow<MiotBindStatus>(MiotBindStatus.Unbound)
    val status: StateFlow<MiotBindStatus> = _status.asStateFlow()

    /** 令牌缓存。仅在内存中，进程重启后从加密存储恢复。 */
    @Volatile
    private var cached: MiotAuthState? = null

    private val refreshLock = Mutex()

    /** 从加密存储恢复绑定状态。应在应用启动时调用一次。 */
    fun initialize() {
        cached = runCatching { store.load() }
            .onFailure { Log.e(TAG, "读取米家授权失败", it) }
            .getOrNull()
        publishStatus()
    }

    fun currentUuid(): String = store.uuid()

    fun buildAuthUrl(): String = oauth.buildAuthUrl(store.uuid())

    /** 供云客户端同步取用的令牌；为空表示未绑定或不可用。 */
    fun currentAccessToken(): String? = cached?.accessToken?.takeIf { it.isNotBlank() }

    /**
     * 确保有一个可用的访问令牌。
     *
     * @param force 为 true 时即使本地判断未过期也强制续期（用于服务端返回 401 之后）
     */
    suspend fun ensureFresh(force: Boolean = false): Result<MiotAuthState> = refreshLock.withLock {
        val current = cached

        if (!force && current != null && current.isValid(nowSec())) {
            return@withLock Result.success(current)
        }

        val refreshToken = current?.refreshToken
            ?: return@withLock Result.failure(IllegalStateException("尚未绑定米家账号"))

        return@withLock oauth.refresh(refreshToken)
            .mapCatching { refreshed ->
                // 服务端可能不回传 refresh_token，沿用旧的，避免把续期凭据弄丢
                val merged = if (refreshed.refreshToken.isBlank()) {
                    refreshed.copy(refreshToken = refreshToken)
                } else {
                    refreshed
                }
                persist(merged)
                merged
            }
            .onFailure { error ->
                Log.w(TAG, "米家令牌续期失败", error)
                // 走到这里 current 必然非空（否则上面已提前返回）。
                // 若本地令牌也已过期，说明无法再自动恢复，必须重新授权。
                if (!current.isValid(nowSec())) {
                    _status.value = MiotBindStatus.Expired(
                        error.message ?: "授权已失效，请重新绑定",
                    )
                }
            }
    }

    /**
     * 用授权回执完成绑定。
     *
     * 回执需含 code 与 state，且 state 必须与本地推导值一致——这是防伪造回执的关键校验。
     */
    suspend fun bind(rawPayload: String): Result<MiotAuthState> {
        val outcome = MiotAuthPayloadParser.parse(rawPayload)
        val payload = when (outcome) {
            is MiotAuthPayloadParser.Outcome.Failure ->
                return Result.failure(IllegalArgumentException(outcome.reason))

            is MiotAuthPayloadParser.Outcome.Success -> outcome.payload
        }

        val uuid = store.uuid()
        if (!oauth.verifyState(uuid, payload.state)) {
            return Result.failure(
                IllegalStateException("授权回执校验失败（state 不匹配），请重新开始绑定后再试"),
            )
        }

        return oauth.exchangeCode(uuid, payload.code)
            .mapCatching { state ->
                persist(state)
                state
            }
    }

    /** 解除绑定：清除令牌并切回未绑定状态。 */
    fun unbind() {
        cached = null
        runCatching { store.clear() }
            .onFailure { Log.e(TAG, "清除米家授权失败", it) }
        _status.value = MiotBindStatus.Unbound
        onBindingChanged(false)
    }

    /** 记录小米昵称，仅用于界面展示。 */
    fun updateNickname(nickname: String?) {
        if (nickname.isNullOrBlank()) return
        val current = cached ?: return
        cached = current.copy(nickname = nickname)
        runCatching { store.save(cached!!) }
    }

    private fun persist(state: MiotAuthState) {
        cached = state
        store.save(state)
        publishStatus()
        onBindingChanged(true)
    }

    private fun publishStatus() {
        val current = cached
        _status.value = when {
            current == null -> MiotBindStatus.Unbound
            !current.isValid(nowSec()) -> MiotBindStatus.Expired("授权已过期，请重新绑定")
            else -> MiotBindStatus.Bound(
                nickname = current.nickname,
                expiresAtSec = current.expiresAtSec,
                nextRefreshAtSec = current.nextRefreshAtSec(),
                boundAtSec = current.boundAtSec,
            )
        }
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private companion object {
        const val TAG = "MiotAccountManager"
    }
}
