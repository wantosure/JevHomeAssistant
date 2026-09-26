package com.jev.assistant.miot

import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 米家云的加密信封。
 *
 * 协议约定（来自 HTTP 交互本身，非任何 SDK 的实现表达）：
 *  1. 每次会话随机生成 16 字节 AES 密钥；
 *  2. 用 MIoT 的 RSA 公钥以 `RSA/ECB/PKCS1Padding` 包裹该密钥，base64 后放入 `X-Client-Secret` 头；
 *  3. 请求体 = base64( AES-128-CBC(JSON, key, **IV 与 key 相同**) 加 PKCS7 填充 )；
 *  4. 响应体使用同一密钥、同样的 AES-128-CBC 加密，需解密后再解析 JSON。
 *
 * 注意 IV 等于密钥本身是协议既定行为，因此同一密钥下相同明文会产生相同密文，
 * 这是预期结果而非缺陷。
 *
 * 刻意使用 `java.util.Base64`（API 26+，与 minSdk 一致）而非 `android.util.Base64`，
 * 以便本层可在纯 JVM 单元测试中验证，无需 Robolectric。
 */
object MiotCrypto {

    private const val AES_KEY_BYTES = 16
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getMimeDecoder()

    /** 生成本次会话的随机 AES 密钥。 */
    fun newSessionKey(): ByteArray {
        val key = ByteArray(AES_KEY_BYTES)
        SecureRandom().nextBytes(key)
        return key
    }

    /**
     * 用公钥包裹会话密钥，产出 `X-Client-Secret` 头的值。
     *
     * @param aesKey 本次会话的 16 字节 AES 密钥
     * @param publicKeyPem PEM 格式的 RSA 公钥
     */
    fun wrapSessionKey(aesKey: ByteArray, publicKeyPem: String): String {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, parsePublicKey(publicKeyPem))
        return encoder.encodeToString(cipher.doFinal(aesKey))
    }

    /** 加密请求体：明文 JSON → base64 密文。 */
    fun encrypt(plainJson: String, aesKey: ByteArray): String =
        encoder.encodeToString(newAesCipher(Cipher.ENCRYPT_MODE, aesKey).doFinal(plainJson.toByteArray(Charsets.UTF_8)))

    /** 解密响应体：base64 密文 → 明文 JSON。 */
    fun decrypt(base64CipherText: String, aesKey: ByteArray): String {
        val raw = decoder.decode(base64CipherText.trim())
        return String(newAesCipher(Cipher.DECRYPT_MODE, aesKey).doFinal(raw), Charsets.UTF_8)
    }

    /**
     * 判断一段响应文本能否直接当 JSON 解析。
     *
     * 服务端对不同端点返回形式不同：业务 API 走加密信封（base64 文本），
     * 而 OAuth 换令牌与 miot-spec.org 返回明文 JSON。调用方据此决定是否解密。
     */
    fun looksLikePlainJson(body: String): Boolean {
        val trimmed = body.trim()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    private fun newAesCipher(mode: Int, aesKey: ByteArray): Cipher {
        require(aesKey.size == AES_KEY_BYTES) {
            "会话密钥必须是 $AES_KEY_BYTES 字节，实际 ${aesKey.size}"
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // 协议约定 IV 与密钥相同
        cipher.init(mode, SecretKeySpec(aesKey, "AES"), IvParameterSpec(aesKey))
        return cipher
    }

    /** 解析 PEM 格式的 RSA 公钥。 */
    fun parsePublicKey(pem: String): PublicKey {
        val body = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")
        return KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(decoder.decode(body)))
    }
}
