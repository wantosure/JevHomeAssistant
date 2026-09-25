package com.jev.assistant.jev

import com.google.gson.annotations.SerializedName

data class JevQuestion(
    val type: String = "choice",
    val instructions: String,
    val criteria: Map<String, String>
)

data class JevRequest(
    val state: Map<String, Any>,
    val model: String = "jev-latest",
    val questions: Map<String, JevQuestion>
)

data class JevAnswer(
    val choice: String,
    val confidence: Float? = null,
    val probabilities: Map<String, Float>? = null,
    val explanation: String? = null
)

data class JevUsage(
    @SerializedName("input_tokens") val inputTokens: Int = 0,
    @SerializedName("output_tokens") val outputTokens: Int = 0,
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0,
    @SerializedName("total_tokens") val totalTokens: Int = 0
) {
    /**
     * 根据 Jev 官方公开计费标准按真实输入 Token 计费：
     * 每百万输入 Token 0.042 美元，折合人民币约 0.3024 元 / 百万 Token
     */
    fun calculateCostCny(): Double {
        val tokens = when {
            inputTokens > 0 -> inputTokens
            promptTokens > 0 -> promptTokens
            totalTokens > 0 -> totalTokens
            else -> 0
        }
        if (tokens <= 0) return 0.0
        return tokens * 0.042 * 7.2 / 1_000_000.0
    }
}

data class JevResponse(
    val answers: Map<String, JevAnswer> = emptyMap(),
    val usage: JevUsage? = null,
    val model: String? = null
) {
    fun getCostCny(): Double = usage?.calculateCostCny() ?: 0.0
}

data class StageOneResult(
    val engagement: String, // actionable, ignore, uncertain
    val intent: String,     // home_control, alarm, computer_control, daily_chat, other
    val confidence: Float,
    val rawResponse: JevResponse
)

data class StageTwoResult(
    val targetDeviceId: String?,
    val action: String?,
    val parameters: Map<String, Any>,
    val confidence: Float,
    val rawResponse: JevResponse
)
