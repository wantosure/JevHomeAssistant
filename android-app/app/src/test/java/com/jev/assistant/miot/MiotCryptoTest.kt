package com.jev.assistant.miot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * 加密信封的已知答案测试。
 *
 * CIPHER_TEXT_KNOWN 由参考协议实现（Python `cryptography` 库）以固定密钥生成，
 * 用于确认 Kotlin 侧的 AES 参数（CBC 模式、IV 等于密钥、PKCS7 填充）与协议完全一致。
 */
class MiotCryptoTest {

    private val fixedKey = ByteArray(16) { it.toByte() } // 00 01 02 ... 0f

    @Test
    fun `加密结果与协议已知答案完全一致`() {
        val plain = """{"did":"2026857030","siid":2,"piid":1,"value":true}"""
        assertEquals(KNOWN_CIPHER_B64, MiotCrypto.encrypt(plain, fixedKey))
    }

    @Test
    fun `解密已知密文应还原明文`() {
        val plain = """{"did":"2026857030","siid":2,"piid":1,"value":true}"""
        assertEquals(plain, MiotCrypto.decrypt(KNOWN_CIPHER_B64, fixedKey))
    }

    @Test
    fun `加解密往返应保持一致`() {
        val plain = """{"params":[{"did":"1184797313","siid":2,"piid":1,"value":false}]}"""
        val restored = MiotCrypto.decrypt(MiotCrypto.encrypt(plain, fixedKey), fixedKey)
        assertEquals(plain, restored)
    }

    @Test
    fun `中文与多字节字符应能正确往返`() {
        val plain = """{"name":"北次卧主灯","room":"北次卧"}"""
        val restored = MiotCrypto.decrypt(MiotCrypto.encrypt(plain, fixedKey), fixedKey)
        assertEquals(plain, restored)
    }

    /**
     * IV 等于密钥是协议既定行为，因此同一密钥下相同明文恒定产生相同密文。
     * 这是预期结果（不是缺陷），在此显式固定该行为以防日后被"优化"成随机 IV。
     */
    @Test
    fun `相同密钥下相同明文应产生相同密文`() {
        val plain = """{"a":1}"""
        assertEquals(
            MiotCrypto.encrypt(plain, fixedKey),
            MiotCrypto.encrypt(plain, fixedKey),
        )
    }

    @Test
    fun `不同密钥应产生不同密文`() {
        val plain = """{"a":1}"""
        val otherKey = ByteArray(16) { (it + 1).toByte() }
        assertNotEquals(
            MiotCrypto.encrypt(plain, fixedKey),
            MiotCrypto.encrypt(plain, otherKey),
        )
    }

    @Test
    fun `会话密钥应为16字节`() {
        assertEquals(16, MiotCrypto.newSessionKey().size)
    }

    @Test
    fun `会话密钥每次生成应不同`() {
        assertNotEquals(
            MiotCrypto.newSessionKey().toList(),
            MiotCrypto.newSessionKey().toList(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `非16字节密钥应被拒绝`() {
        MiotCrypto.encrypt("""{"a":1}""", ByteArray(8))
    }

    @Test
    fun `包裹会话密钥应产出2048位RSA密文的base64`() {
        val secret = MiotCrypto.wrapSessionKey(fixedKey, MiotCredentials.DEFAULT_PUBLIC_KEY_PEM)
        // 2048 位 RSA → 256 字节密文 → base64 定长 344 字符
        assertEquals(344, secret.length)
        assertEquals(256, Base64.getDecoder().decode(secret).size)
    }

    @Test
    fun `内置换行与空白的PEM应能正确解析`() {
        // DEFAULT_PUBLIC_KEY_PEM 为多行格式，解析应当成功
        val key = MiotCrypto.parsePublicKey(MiotCredentials.DEFAULT_PUBLIC_KEY_PEM)
        assertEquals("RSA", key.algorithm)
    }

    @Test
    fun `明文JSON判定`() {
        assertTrue(MiotCrypto.looksLikePlainJson("""{"code":0}"""))
        assertTrue(MiotCrypto.looksLikePlainJson("  [1,2]  "))
        assertFalse(MiotCrypto.looksLikePlainJson("v3ObQh0xeSPkOvN4Puo2sQ=="))
    }

    private companion object {
        /** 由 Python cryptography 库以密钥 000102...0f 生成，见 MiotCryptoTest 头注释。 */
        const val KNOWN_CIPHER_B64 =
            "v3ObQh0xeSPkOvN4Puo2sZP/lX9zQxO9thdd2K0abG+H/hmis7EQg6CGHmdR9Gtg" +
                "AazaOpsJbb3ge4W0G4KFqQ=="
    }
}
