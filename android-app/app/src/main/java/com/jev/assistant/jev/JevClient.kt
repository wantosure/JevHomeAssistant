package com.jev.assistant.jev

import com.google.gson.Gson
import com.jev.assistant.data.DeviceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class JevClient(private val getApiKey: () -> String) {

    companion object {
        private const val API_URL = "https://api.typesafe.ai/v1/systemone"
        private const val MODEL_NAME = "jev-latest"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun postSystemOne(state: Map<String, Any>, questions: Map<String, JevQuestion>): Result<JevResponse> =
        withContext(Dispatchers.IO) {
            val key = getApiKey()
            if (key.isBlank()) {
                // 如果没有配置 Key，返回明确提示
                return@withContext Result.failure(IllegalStateException("尚未在设置中配置 Jev API Key"))
            }

            val requestBodyObj = JevRequest(
                state = state,
                model = MODEL_NAME,
                questions = questions
            )
            val jsonBody = gson.toJson(requestBodyObj)

            val httpRequest = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(jsonBody.toRequestBody(JSON_MEDIA))
                .build()

            try {
                httpClient.newCall(httpRequest).execute().use { response ->
                    val responseStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Jev HTTP ${response.code}: $responseStr")
                        )
                    }
                    val jevResp = gson.fromJson(responseStr, JevResponse::class.java)
                    Result.success(jevResp)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 第一阶段：判断业务类别与介入态度
    suspend fun evaluateStageOne(userText: String, defaultRoom: String): Result<StageOneResult> {
        val state = mapOf(
            "user_text" to userText,
            "default_room" to defaultRoom,
            "timestamp" to System.currentTimeMillis()
        )

        val questions = mapOf(
            "engagement" to JevQuestion(
                instructions = "判断该话语是否为对助手发出的明确请求。闲聊、电视背景声、引用、否定、反问、自言自语请选 ignore。",
                criteria = mapOf(
                    "actionable" to "明确且直接的设备控制、闹钟设定或电脑操作请求",
                    "ignore" to "日常闲聊、看电视发出的声音、自言自语、否定假设或无关语句",
                    "uncertain" to "目标模糊不清、信息严重缺失、或无法判断意图"
                )
            ),
            "intent" to JevQuestion(
                instructions = "如果该请求需要处理，最主要属于哪一类业务？",
                criteria = mapOf(
                    "home_control" to "智能家居设备控制（开关灯、调温、拉窗帘、风扇等）",
                    "alarm" to "设置手机闹钟、提醒或定时响铃",
                    "computer_control" to "控制同一局域网下的电脑（调音量、暂停媒体、打开应用）",
                    "daily_chat" to "普通聊天或知识问答（不执行控制）",
                    "other" to "其他无法归入以上类别的请求"
                )
            )
        )

        val respResult = postSystemOne(state, questions)
        return respResult.map { resp ->
            val engageAnswer = resp.answers["engagement"]
            val intentAnswer = resp.answers["intent"]
            val engagement = engageAnswer?.choice ?: "uncertain"
            val intent = intentAnswer?.choice ?: "other"
            val conf = listOfNotNull(engageAnswer?.confidence, intentAnswer?.confidence)
                .minOrNull() ?: 0.5f

            StageOneResult(
                engagement = engagement,
                intent = intent,
                confidence = conf,
                rawResponse = resp
            )
        }
    }

    // 第二阶段：智能家居目标设备与动作提取，并由 Jev 选定具体数值参数（需求 3 & 4）
    suspend fun evaluateStageTwoHome(
        userText: String,
        room: String,
        candidateDevices: List<DeviceItem>
    ): Result<StageTwoResult> {
        val numericCandidates = com.jev.assistant.utils.NumericExtractor.extractCandidates(userText)
        val state = mutableMapOf<String, Any>(
            "user_text" to userText,
            "room" to room,
            "numeric_candidates" to numericCandidates
        )

        val deviceCriteria = candidateDevices.associate { dev ->
            dev.logicalId to "${dev.room} ${dev.name}（支持: ${dev.capabilities.joinToString(",")}）"
        }.toMutableMap()
        deviceCriteria["none"] = "没有匹配的候选设备，或无法确定设备"

        val questions = mutableMapOf(
            "target_device" to JevQuestion(
                instructions = "根据用户原话和房间上下文，选出最符合的目标设备：",
                criteria = deviceCriteria
            ),
            "action" to JevQuestion(
                instructions = "用户期望执行的具体动作是什么？",
                criteria = mapOf(
                    "set_power_on" to "开机/打开/启动电源",
                    "set_power_off" to "关机/关闭/切断电源",
                    "set_brightness" to "调节亮度（如调亮、调暗、设置特定百分比）",
                    "set_temperature" to "调节温度（如设置到26度）",
                    "set_speed" to "调节风扇风速/档位",
                    "open" to "打开/拉开窗帘",
                    "close" to "关闭/合上窗帘",
                    "stop" to "停止运行",
                    "none" to "无明确控制动作"
                )
            )
        )

        // 如果用户话语中提取到了数字（如 30%、一二三四、十五、八十七、四千等），让 Jev 确定具体参数
        if (numericCandidates.isNotEmpty()) {
            val brightnessOptions = mutableMapOf<String, String>("no_change" to "未请求改变亮度")
            numericCandidates.forEachIndexed { idx, numStr ->
                brightnessOptions["n$idx"] = "将亮度设为 $numStr (或百分之 $numStr)"
            }
            questions["set_brightness"] = JevQuestion(
                instructions = "用户是否明确要求设置目标设备的亮度？从候选数值中选出用户指定的值；未指定请选 no_change：",
                criteria = brightnessOptions
            )

            val tempOptions = mutableMapOf<String, String>("no_change" to "未请求改变温度")
            numericCandidates.forEachIndexed { idx, numStr ->
                tempOptions["n$idx"] = "将温度设为 $numStr 度"
            }
            questions["set_temperature"] = JevQuestion(
                instructions = "用户是否明确要求设置空调/设备温度？从候选数值中选出用户指定的值；未指定请选 no_change：",
                criteria = tempOptions
            )

            val posOptions = mutableMapOf<String, String>("no_change" to "未请求改变开合度")
            numericCandidates.forEachIndexed { idx, numStr ->
                posOptions["n$idx"] = "将窗帘开合度设为 $numStr%"
            }
            questions["set_position"] = JevQuestion(
                instructions = "用户是否明确要求设置窗帘开合位置？从候选数值中选出用户指定的值；未指定请选 no_change：",
                criteria = posOptions
            )
        }

        val respResult = postSystemOne(state, questions)
        return respResult.map { resp ->
            val devAns = resp.answers["target_device"]
            val actAns = resp.answers["action"]
            val devId = if (devAns?.choice != "none") devAns?.choice else null
            var action = if (actAns?.choice != "none") actAns?.choice else null
            val conf = listOfNotNull(devAns?.confidence, actAns?.confidence).minOrNull() ?: 0.5f

            val params = mutableMapOf<String, Any>()
            when (action) {
                "set_power_on" -> params["power"] = 1
                "set_power_off" -> params["power"] = 0
            }

            // 解析 Jev 选定的亮度数值
            val brightAns = resp.answers["set_brightness"]?.choice
            if (brightAns != null && brightAns.startsWith("n")) {
                val idx = brightAns.removePrefix("n").toIntOrNull()
                if (idx != null && idx in numericCandidates.indices) {
                    val num = numericCandidates[idx].toDoubleOrNull()?.toInt()
                    if (num != null) {
                        params["percent"] = num.coerceIn(1, 100)
                        params["power"] = 1
                        action = "set_brightness"
                    }
                }
            }

            // 解析 Jev 选定的温度数值
            val tempAns = resp.answers["set_temperature"]?.choice
            if (tempAns != null && tempAns.startsWith("n")) {
                val idx = tempAns.removePrefix("n").toIntOrNull()
                if (idx != null && idx in numericCandidates.indices) {
                    val num = numericCandidates[idx].toDoubleOrNull()
                    if (num != null) {
                        params["celsius"] = num.coerceIn(16.0, 32.0)
                        params["power"] = 1
                        action = "set_temperature"
                    }
                }
            }

            // 解析 Jev 选定的窗帘开合位置
            val posAns = resp.answers["set_position"]?.choice
            if (posAns != null && posAns.startsWith("n")) {
                val idx = posAns.removePrefix("n").toIntOrNull()
                if (idx != null && idx in numericCandidates.indices) {
                    val num = numericCandidates[idx].toDoubleOrNull()?.toInt()
                    if (num != null) {
                        params["position"] = num.coerceIn(0, 100)
                        action = "set_position"
                    }
                }
            }

            // 智能参数安全兜底：如果动作是调节亮度但参数中缺失 percent，从候选数值中直接绑定
            if (action == "set_brightness" && !params.containsKey("percent") && numericCandidates.isNotEmpty()) {
                val fallbackVal = numericCandidates.firstNotNullOfOrNull { it.toDoubleOrNull()?.toInt() }
                if (fallbackVal != null) {
                    params["percent"] = fallbackVal.coerceIn(1, 100)
                    params["power"] = 1
                }
            }

            // 智能参数安全兜底：如果动作是调节温度但参数中缺失 celsius，从候选数值中直接绑定
            if (action == "set_temperature" && !params.containsKey("celsius") && numericCandidates.isNotEmpty()) {
                val fallbackVal = numericCandidates.firstNotNullOfOrNull { it.toDoubleOrNull() }
                if (fallbackVal != null) {
                    params["celsius"] = fallbackVal.coerceIn(16.0, 32.0)
                    params["power"] = 1
                }
            }

            StageTwoResult(
                targetDeviceId = devId,
                action = action,
                parameters = params,
                confidence = conf,
                rawResponse = resp
            )
        }
    }
}
