package com.jev.assistant.miot.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * 小米账号授权信息的持久化。
 *
 * **必须使用 Android Keystore 支持的加密存储**：refresh_token 是长期凭据，
 * 一旦泄露等同于账号被他人控制设备。因此这里在加密存储不可用时**直接报错而非降级为明文**
 * ——宁可让用户无法绑定，也不能把凭据明文落盘。
 *
 * 注意项目里既有的 Jev API Key 是明文存于普通 SharedPreferences 的
 * （见 `LocalStorage`），本类刻意不沿用那套做法。
 */
class MiotTokenStore(private val context: Context) : MiotAuthStore {

    /** 加密存储不可用时抛出，调用方应向用户明确报错。 */
    class SecureStorageUnavailableException(cause: Throwable) :
        IllegalStateException("设备的安全存储不可用，无法安全保存米家授权，已拒绝绑定", cause)

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.e(TAG, "初始化加密存储失败，拒绝以明文保存授权", e)
            throw SecureStorageUnavailableException(e)
        }
    }

    /**
     * 设备标识。首次访问时生成并持久化，解绑不清除
     * ——它只用于推导 state，不含隐私信息。
     */
    override fun uuid(): String {
        prefs.getString(KEY_UUID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val generated = UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString(KEY_UUID, generated).apply()
        return generated
    }

    override fun load(): MiotAuthState? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        return MiotAuthState(
            accessToken = access,
            refreshToken = refresh,
            expiresAtSec = prefs.getLong(KEY_EXPIRES_AT, 0L),
            nickname = prefs.getString(KEY_NICKNAME, null),
            boundAtSec = prefs.getLong(KEY_BOUND_AT, 0L),
            lastRefreshedAtSec = prefs.getLong(KEY_LAST_REFRESHED_AT, 0L),
        )
    }

    /**
     * 保存令牌。
     *
     * 续期时服务端可能不回传 refresh_token，此时保留原有的，避免把续期凭据弄丢。
     */
    override fun save(state: MiotAuthState) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, state.accessToken)
            .putString(KEY_REFRESH_TOKEN, state.refreshToken)
            .putLong(KEY_EXPIRES_AT, state.expiresAtSec)
            .putLong(KEY_BOUND_AT, state.boundAtSec)
            .putLong(KEY_LAST_REFRESHED_AT, state.lastRefreshedAtSec)
            .apply {
                state.nickname?.let { putString(KEY_NICKNAME, it) }
            }
            .apply()
    }

    /** 退出绑定：清除全部令牌，保留 uuid。 */
    override fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_NICKNAME)
            .remove(KEY_BOUND_AT)
            .remove(KEY_LAST_REFRESHED_AT)
            .apply()
    }

    private companion object {
        const val TAG = "MiotTokenStore"
        const val FILE_NAME = "jev_miot_auth"
        const val KEY_UUID = "device_uuid"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_NICKNAME = "nickname"
        const val KEY_BOUND_AT = "bound_at"
        const val KEY_LAST_REFRESHED_AT = "last_refreshed_at"
    }
}
