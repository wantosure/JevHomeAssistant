package com.jev.assistant.miot

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 设备规范（urn → 能力）的获取与缓存。
 *
 * 缓存的是 miot-spec.org 返回的**原始规范 JSON**而非解析结果，
 * 这样日后改进 [MiotSpecParser] 时无需重新联网即可生效。
 *
 * 同一个 URN 的规范是稳定的，因此缓存不设过期时间。
 */
class MiotSpecRepository(
    private val cacheDir: File,
    /** 拉取原始规范 JSON 的能力，注入以便测试。 */
    private val fetchInstance: suspend (urn: String) -> Result<JsonObject>,
) {

    private val memory = mutableMapOf<String, MiotDeviceSpec>()
    private val memoryLock = Mutex()

    /** 限制对 miot-spec.org 的并发，避免首次同步几十台设备时触发限流。 */
    private val fetchGate = Semaphore(MAX_CONCURRENT_FETCH)

    /**
     * 取得某个 URN 的能力描述。命中内存或磁盘缓存时不发起网络请求。
     */
    suspend fun get(urn: String): Result<MiotDeviceSpec> {
        if (urn.isBlank()) {
            return Result.failure(IllegalArgumentException("URN 为空"))
        }

        memoryLock.withLock { memory[urn] }?.let { return Result.success(it) }

        readCache(urn)?.let { spec ->
            memoryLock.withLock { memory[urn] = spec }
            return Result.success(spec)
        }

        return fetchGate.withPermit {
            // 并发下可能有其它协程已写入，再查一次
            memoryLock.withLock { memory[urn] }?.let { return@withPermit Result.success(it) }
            readCache(urn)?.let { spec ->
                memoryLock.withLock { memory[urn] = spec }
                return@withPermit Result.success(spec)
            }

            fetchInstance(urn).mapCatching { json ->
                writeCache(urn, json)
                val spec = MiotSpecParser.parse(json)
                    ?: throw IllegalStateException("无法解析设备规范：$urn")
                memoryLock.withLock { memory[urn] = spec }
                spec
            }
        }
    }

    /**
     * 已缓存（内存或磁盘）的规范数量，用于界面展示。
     */
    fun cachedCount(): Int = memory.size + (cacheDir.listFiles()?.size ?: 0)

    /** 清空全部缓存。 */
    suspend fun clear() {
        memoryLock.withLock { memory.clear() }
        withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    // ---------------- 磁盘缓存 ----------------

    private fun cacheFile(urn: String): File =
        File(cacheDir, "${urn.replace(Regex("[^A-Za-z0-9._-]"), "_")}.json")

    private suspend fun readCache(urn: String): MiotDeviceSpec? = withContext(Dispatchers.IO) {
        val file = cacheFile(urn)
        if (!file.isFile) return@withContext null
        runCatching {
            val root = JsonParser.parseString(file.readText()).asJsonObject
            // 缓存版本不符时视为未命中，由后续拉取覆盖
            if (root.get("cacheVersion")?.asInt != CACHE_VERSION) return@withContext null
            root.getAsJsonObject("instance")?.let { MiotSpecParser.parse(it) }
        }.getOrNull()
    }

    private suspend fun writeCache(urn: String, instance: JsonObject) = withContext(Dispatchers.IO) {
        runCatching {
            cacheDir.mkdirs()
            val wrapper = JsonObject().apply {
                addProperty("cacheVersion", CACHE_VERSION)
                add("instance", instance)
            }
            // 先写临时文件再改名，避免写入中断留下半截缓存
            val target = cacheFile(urn)
            val temp = File(cacheDir, "${target.name}.tmp")
            temp.writeText(wrapper.toString())
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        }
    }

    companion object {
        /** 缓存结构版本；调整包装格式时递增，旧缓存会自动失效。 */
        private const val CACHE_VERSION = 1

        private const val MAX_CONCURRENT_FETCH = 3

        /**
         * 基于云客户端构造。规范来自 miot-spec.org 的公开接口，不需要小米账号鉴权。
         */
        fun create(cacheDir: File, cloud: MiotCloudClient): MiotSpecRepository =
            MiotSpecRepository(cacheDir) { urn ->
                cloud.getPublicJson(
                    url = "https://miot-spec.org/miot-spec-v2/instance",
                    params = mapOf("type" to urn),
                )
            }
    }
}
