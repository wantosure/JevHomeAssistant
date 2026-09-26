package com.jev.assistant.miot

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.jev.assistant.device.DeviceErrorKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 米家云的唯一 HTTP 出口。
 *
 * 两类端点的行为不同，务必区分：
 *  - **业务接口**（设备/家庭/属性）走加密信封：请求体加密、响应体也加密，且需要 `X-Client-Secret` 等头；
 *  - **换令牌接口**与 **miot-spec.org** 返回**明文 JSON**，且不带加密相关的头。
 *
 * 凭据与访问令牌通过 lambda 注入（与 `JevClient` 的做法一致），便于在令牌续期后随时取到最新值。
 */
class MiotCloudClient(
    private val credentialsProvider: () -> MiotCredentials,
    private val accessTokenProvider: () -> String?,
    /** 收到 401 时调用；返回刷新后的令牌，返回 null 表示无法刷新。 */
    private val onUnauthorized: suspend () -> String? = { null },
    /**
     * 业务接口的基地址覆盖项，仅用于测试（指向本地 MockWebServer）。
     * 为 null 时按凭据中的区域推导真实主机。
     */
    internal val baseUrlOverride: String? = null,
) {

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(MiotConfig.CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(MiotConfig.READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(MiotConfig.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    /** 会话 AES 密钥，进程存活期内固定；随 `X-Client-Secret` 一同送达服务端。 */
    private val sessionKey: ByteArray = MiotCrypto.newSessionKey()

    @Volatile
    private var cachedSecret: String? = null

    private fun clientSecret(): String = cachedSecret ?: synchronized(this) {
        cachedSecret ?: MiotCrypto
            .wrapSessionKey(sessionKey, credentialsProvider().publicKeyPem)
            .also { cachedSecret = it }
    }

    /** 切换凭据（例如用户改了 client_id 或公钥）后需要重新生成密钥。 */
    fun invalidateSession() {
        synchronized(this) { cachedSecret = null }
    }

    // ---------------- 业务接口（加密信封） ----------------

    /**
     * 发起一次加密信封请求并返回**已解密的完整外层信封**。
     *
     * 调用方从 `result` 字段取出业务数据；`code != 0` 时返回失败。
     */
    suspend fun postEnvelope(path: String, body: Any): Result<JsonObject> = withContext(Dispatchers.IO) {
        val token = accessTokenProvider()
            ?: return@withContext Result.failure(IllegalStateException(ERR_NOT_BOUND))

        runCatching { requestEnvelope(path, body, token) }
            .fold(
                onSuccess = { envelope ->
                    val code = envelope.get("code")?.asInt ?: 0
                    if (MiotErrorMapper.isSuccess(code)) {
                        Result.success(envelope)
                    } else {
                        val message = envelope.get("message")?.takeIf { !it.isJsonNull }?.asString
                        Result.failure(MiotCloudException(MiotErrorMapper.map(code, message), code))
                    }
                },
                onFailure = { Result.failure(it) },
            )
    }

    private suspend fun requestEnvelope(path: String, body: Any, token: String): JsonObject {
        val credentials = credentialsProvider()
        val requestBody = MiotCrypto.encrypt(gson.toJson(body), sessionKey)

        val response = execute(
            Request.Builder()
                .url("${bizBase()}$path")
                .header("Content-Type", "text/plain")
                .header("User-Agent", MiotConfig.USER_AGENT)
                .header("X-Client-BizId", MiotConfig.X_CLIENT_BIZID)
                .header("X-Encrypt-Type", MiotConfig.X_ENCRYPT_TYPE)
                .header("X-Client-AppId", credentials.clientId)
                .header("X-Client-Secret", clientSecret())
                .header("Host", credentials.bizHost)
                // 协议要求 Bearer 与令牌之间没有空格
                .header("Authorization", "Bearer$token")
                // 用字节数组重载构造请求体：String 重载会在 media type 缺少 charset 时
                // 自动补上 "; charset=utf-8"，导致与协议声明的 text/plain 产生偏差。
                .post(requestBody.toByteArray(Charsets.UTF_8).toRequestBody(PLAIN_TEXT))
                .build(),
        )

        val raw = response.body?.string().orEmpty()
        val plain = if (MiotCrypto.looksLikePlainJson(raw)) {
            raw
        } else {
            MiotCrypto.decrypt(raw, sessionKey)
        }
        return JsonParser.parseString(plain).asJsonObject
    }

    // ---------------- 明文接口 ----------------

    /**
     * 换令牌 / 刷新令牌。
     *
     * 协议形态：`GET {host}/app/v2/mico/oauth/get_token?data={json}`，
     * **明文 JSON 响应，无加密信封、无 `X-Client-Secret`**。
     * `data` 中不含 `grant_type`，换码与刷新靠字段区分。
     */
    suspend fun requestToken(data: Map<String, String>): Result<MiotTokenEnvelopeDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = buildString {
                    append(bizBase())
                    append("/app/v2/").append(MiotConfig.PROJECT_CODE).append("/oauth/get_token?data=")
                    append(URLEncoder.encode(gson.toJson(data), "UTF-8"))
                }
                val response = execute(
                    Request.Builder()
                        .url(url)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .get()
                        .build(),
                )
                val raw = response.body?.string().orEmpty()
                gson.fromJson(raw, MiotTokenEnvelopeDto::class.java)
                    ?: throw IOException("换令牌响应无法解析")
            }.fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(it) },
            )
        }

    /**
     * 读取未经加密的公开 JSON（当前用于 miot-spec.org 的规范接口）。
     */
    suspend fun getPublicJson(url: String, params: Map<String, String> = emptyMap()): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fullUrl = buildString {
                    append(url)
                    if (params.isNotEmpty()) {
                        append(if (url.contains('?')) '&' else '?')
                        append(params.entries.joinToString("&") { (k, v) ->
                            "$k=${URLEncoder.encode(v, "UTF-8")}"
                        })
                    }
                }
                val response = execute(Request.Builder().url(fullUrl).get().build())
                JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
            }.fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(it) },
            )
        }

    // ---------------- 业务方法 ----------------

    suspend fun getDeviceListPage(
        dids: List<String>,
        startDid: String? = null,
    ): Result<MiotDeviceListPageDto> =
        postEnvelope(
            path = "/app/v2/home/device_list_page",
            body = buildMap<String, Any> {
                put("limit", MiotConfig.DEVICE_PAGE_LIMIT)
                put("get_split_device", true)
                put("dids", dids)
                startDid?.let { put("start_did", it) }
            },
        ).map { envelope ->
            gson.fromJson(envelope.get("result"), MiotDeviceListPageDto::class.java)
                ?: MiotDeviceListPageDto()
        }

    /**
     * 拉取全部设备。
     *
     * 设备按 150 个一批请求，批内再用 `next_start_did` 翻页，直到拿全。
     * 单批失败视为整体失败，避免上层拿到半份设备列表却当作完整数据。
     */
    suspend fun getAllDevices(dids: List<String>): Result<List<MiotDeviceDto>> {
        if (dids.isEmpty()) return Result.success(emptyList())

        val collected = mutableListOf<MiotDeviceDto>()
        for (batch in dids.chunked(BATCH_DID_SIZE)) {
            var startDid: String? = null
            do {
                val page = getDeviceListPage(batch, startDid).getOrElse { return Result.failure(it) }
                collected += page.list
                startDid = if (page.hasMore) page.nextStartDid else null
            } while (startDid != null)
        }
        return Result.success(collected)
    }

    suspend fun getHomes(): Result<MiotHomeListDto> =
        postEnvelope(
            path = "/app/v2/homeroom/gethome",
            body = mapOf(
                "limit" to MiotConfig.HOME_PAGE_LIMIT,
                "fetch_share" to false,
                "fetch_share_dev" to false,
                "plat_form" to 0,
                "app_ver" to 9,
            ),
        ).map { envelope ->
            gson.fromJson(envelope.get("result"), MiotHomeListDto::class.java)
                ?: MiotHomeListDto()
        }

    suspend fun getDevRoomPage(startId: String?): Result<MiotDevRoomPageDto> =
        postEnvelope(
            path = "/app/v2/homeroom/get_dev_room_page",
            body = buildMap<String, Any> {
                startId?.let { put("start_id", it) }
                put("limit", MiotConfig.HOME_PAGE_LIMIT)
            },
        ).map { envelope ->
            gson.fromJson(envelope.get("result"), MiotDevRoomPageDto::class.java)
                ?: MiotDevRoomPageDto()
        }

    suspend fun getProps(queries: List<MiotPropQuery>): Result<List<MiotPropValueDto>> {
        if (queries.isEmpty()) return Result.success(emptyList())
        return postEnvelope(
            path = "/app/v2/miotspec/prop/get",
            // 读接口额外要求 datasource 字段
            body = mapOf("datasource" to 1, "params" to queries),
        ).map { envelope ->
            val type = object : TypeToken<List<MiotPropValueDto>>() {}.type
            gson.fromJson<List<MiotPropValueDto>>(envelope.get("result"), type).orEmpty()
        }
    }

    suspend fun setProps(writes: List<MiotPropWrite>): Result<List<MiotPropWriteResultDto>> {
        if (writes.isEmpty()) return Result.success(emptyList())
        return postEnvelope(
            path = "/app/v2/miotspec/prop/set",
            body = mapOf("params" to writes),
        ).map { envelope ->
            val type = object : TypeToken<List<MiotPropWriteResultDto>>() {}.type
            gson.fromJson<List<MiotPropWriteResultDto>>(envelope.get("result"), type).orEmpty()
        }
    }

    /**
     * 按型号反查设备规范 URN。
     *
     * 走 miot-spec.org 的公开接口（明文）。该路径位于 `internal/` 下，
     * 属于非公开契约，失败时返回 null 由调用方降级处理，不应阻塞同步。
     */
    suspend fun resolveUrnByModel(model: String): Result<String?> =
        getPublicJson(
            url = "https://miot-spec.org/internal/urn-by-model-version",
            params = mapOf("model" to model, "version" to "0"),
        ).map { json -> json.get("urn")?.takeIf { !it.isJsonNull }?.asString }

    // ---------------- 底层 ----------------

    /** 业务接口基地址；测试可通过 [baseUrlOverride] 指向本地服务。 */
    private fun bizBase(): String = baseUrlOverride ?: "https://${credentialsProvider().bizHost}"

    /**
     * 执行请求。收到 401 时尝试刷新一次令牌后重试，避免让调用方处理鉴权细节。
     */
    private suspend fun execute(request: Request): okhttp3.Response {
        val first = httpClient.newCall(request).execute()
        if (first.code != 401) {
            return first.also { ensureSuccessful(it) }
        }
        first.close()

        val refreshed = onUnauthorized() ?: throw MiotCloudException(
            MiotErrorMapper.map(-704012906), -704012906,
        )
        val retried = request.newBuilder()
            .header("Authorization", "Bearer$refreshed")
            .build()
        return httpClient.newCall(retried).execute().also { ensureSuccessful(it) }
    }

    private fun ensureSuccessful(response: okhttp3.Response) {
        if (response.isSuccessful) return
        val httpCode = response.code
        response.close()
        val failure = when {
            httpCode == 401 || httpCode == 403 ->
                MiotErrorMapper.map(-704012906)

            httpCode == 404 -> MiotFailure(DeviceErrorKind.NOT_FOUND, "米家云接口不存在，协议可能已变更")

            httpCode == 429 -> MiotFailure(DeviceErrorKind.TIMEOUT, "请求过于频繁，请稍后重试")

            httpCode >= 500 -> MiotFailure(DeviceErrorKind.TIMEOUT, "米家云服务暂时不可用（HTTP $httpCode）")

            else -> MiotFailure(DeviceErrorKind.UNKNOWN, "米家云请求失败（HTTP $httpCode）")
        }
        throw MiotCloudException(failure, httpCode)
    }

    private companion object {
        /**
         * 载荷是 base64 文本，无需 charset。
         *
         * 必须与协议一致地写成裸 `text/plain`：OkHttp 会以 RequestBody 的 media type
         * 覆盖手写的 Content-Type 头，若此处带上 `; charset=utf-8` 就会与协议产生偏差。
         */
        val PLAIN_TEXT = "text/plain".toMediaType()
        const val ERR_NOT_BOUND = "尚未绑定米家账号"

        /** 单次设备列表请求携带的 did 数量上限。 */
        const val BATCH_DID_SIZE = 150
    }
}

/** 米家云业务错误，携带服务端错误码以便归类重试。 */
class MiotCloudException(
    val failure: MiotFailure,
    val code: Int,
) : Exception(failure.message)
