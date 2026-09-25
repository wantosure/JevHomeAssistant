package com.jev.assistant.pc

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class PcAgentClient(
    private val getAgentUrl: () -> String,
    private val getAgentToken: () -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun executeAction(action: String, params: Map<String, Any>): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val baseUrl = getAgentUrl()
            val token = getAgentToken()

            val bodyMap = mapOf(
                "action" to action,
                "parameters" to params,
                "timestamp" to System.currentTimeMillis()
            )

            val request = Request.Builder()
                .url("$baseUrl/api/control")
                .addHeader("Authorization", "Bearer $token")
                .post(gson.toJson(bodyMap).toRequestBody(jsonMedia))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        true to "电脑代理执行成功: $action"
                    } else {
                        false to "电脑代理拒绝: HTTP ${response.code}"
                    }
                }
            } catch (e: Exception) {
                false to "电脑代理不可达 (请确认同一局域网运行代理): ${e.message}"
            }
        }
}
