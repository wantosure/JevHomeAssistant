package com.jev.assistant.miot

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Spec 仓库的缓存行为测试。用注入的假取数函数，不触网。
 */
class MiotSpecRepositoryTest {

    private lateinit var cacheDir: File
    private var fetchCount = 0

    private val urn = "urn:miot-spec-v2:device:light:0000A001:yeelink-ceiling1:2"

    private val instanceJson: JsonObject by lazy {
        val stream = javaClass.getResourceAsStream("/miot/light.json")!!
        JsonParser.parseString(stream.bufferedReader().readText()).asJsonObject
    }

    private fun repository(onFetch: (() -> Result<JsonObject>)? = null) = MiotSpecRepository(cacheDir) { _ ->
        fetchCount++
        onFetch?.invoke() ?: Result.success(instanceJson)
    }

    @Before
    fun setUp() {
        cacheDir = Files.createTempDirectory("miot-spec-test").toFile()
        fetchCount = 0
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun `首次获取应拉取并解析`() = runBlocking {
        val spec = repository().get(urn).getOrThrow()
        assertEquals("light", spec.typeName)
        assertEquals(1, fetchCount)
    }

    @Test
    fun `重复获取应命中内存缓存而不重复拉取`() = runBlocking {
        val repo = repository()
        repo.get(urn).getOrThrow()
        repo.get(urn).getOrThrow()
        repo.get(urn).getOrThrow()
        assertEquals("内存缓存未生效", 1, fetchCount)
    }

    @Test
    fun `新建仓库实例应命中磁盘缓存而不重复拉取`() = runBlocking {
        repository().get(urn).getOrThrow()
        assertEquals(1, fetchCount)

        // 换一个实例，内存缓存清空但仍应命中磁盘
        val fresh = repository()
        val spec = fresh.get(urn).getOrThrow()
        assertEquals("light", spec.typeName)
        assertEquals("磁盘缓存未生效", 1, fetchCount)
    }

    @Test
    fun `空白 urn 应直接失败且不拉取`() = runBlocking {
        val result = repository().get("")
        assertTrue(result.isFailure)
        assertEquals(0, fetchCount)
    }

    @Test
    fun `拉取失败应向上传播`() = runBlocking {
        val repo = repository { Result.failure(java.io.IOException("网络不可达")) }
        val result = repo.get(urn)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is java.io.IOException)
    }

    @Test
    fun `规范无法解析时应失败而不是返回空能力`() = runBlocking {
        val repo = repository { Result.success(JsonObject()) } // 缺少 type/services
        assertTrue(repo.get(urn).isFailure)
    }

    @Test
    fun `清空缓存后应重新拉取`() = runBlocking {
        val repo = repository()
        repo.get(urn).getOrThrow()
        assertEquals(1, fetchCount)

        repo.clear()
        repo.get(urn).getOrThrow()
        assertEquals("清空后应重新拉取", 2, fetchCount)
    }

    @Test
    fun `缓存文件应带版本号`() = runBlocking {
        repository().get(urn).getOrThrow()
        val files = cacheDir.listFiles().orEmpty()
        assertEquals(1, files.size)
        val content = files.first().readText()
        assertTrue("缓存应包含版本标记", content.contains("cacheVersion"))
        assertTrue("缓存应保存原始规范", content.contains("instance"))
    }

    @Test
    fun `损坏的缓存应被忽略并重新拉取`() = runBlocking {
        repository().get(urn).getOrThrow()
        // 破坏缓存内容
        cacheDir.listFiles()!!.first().writeText("{ 这不是合法 JSON")

        val repo = repository()
        val spec = repo.get(urn).getOrThrow()
        assertNotNull(spec)
        assertEquals("损坏缓存应触发重新拉取", 2, fetchCount)
    }

    @Test
    fun `缓存的URN应做文件名安全处理`() = runBlocking {
        repository().get(urn).getOrThrow()
        val name = cacheDir.listFiles()!!.first().name
        assertTrue("文件名不应含冒号", !name.contains(':'))
        assertTrue(name.endsWith(".json"))
    }
}
