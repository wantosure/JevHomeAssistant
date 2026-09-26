package com.jev.assistant.miot.auth

import com.google.gson.JsonParser
import java.net.URLDecoder
import java.util.Base64

/**
 * 授权回执的解析。
 *
 * 小米的授权页完成登录后跳转到固定的回调展示页，该页面会给出可复制的授权信息。
 * 用户可能粘贴三种形态，这里全部兼容：
 *  1. base64 编码的 JSON，形如 `eyJjb2RlIjoiLi4uIiwic3RhdGUiOiIuLi4ifQ==`；
 *  2. 完整回调 URL，形如 `https://...login_redirect?code=xxx&state=yyy`；
 *  3. 含 `code=` 与 `state=` 的查询串片段。
 */
object MiotAuthPayloadParser {

    data class Result(val code: String, val state: String)

    sealed interface Outcome {
        data class Success(val payload: Result) : Outcome
        data class Failure(val reason: String) : Outcome
    }

    fun parse(raw: String): Outcome {
        val text = raw.trim()
        if (text.isEmpty()) return Outcome.Failure("请粘贴授权回执")

        // 形态一：base64 JSON
        decodeBase64Json(text)?.let { return it }

        // 形态二 / 三：从 URL 或查询串中取参数
        extractParams(text)?.let { return it }

        // 兜底：用户可能只粘贴了裸 code。缺少 state 无法做来源校验，明确拒绝。
        if (looksLikeBareCode(text)) {
            return Outcome.Failure("仅识别到授权码，缺少 state 参数；请粘贴完整回执（含 state）")
        }

        return Outcome.Failure("无法识别授权回执格式，请粘贴授权页显示的完整内容")
    }

    private fun decodeBase64Json(text: String): Outcome? {
        val decoded = runCatching {
            String(Base64.getMimeDecoder().decode(text), Charsets.UTF_8)
        }.getOrNull() ?: return null

        val json = runCatching { JsonParser.parseString(decoded).asJsonObject }.getOrNull() ?: return null
        val code = json.get("code")?.takeIf { !it.isJsonNull }?.asString
        val state = json.get("state")?.takeIf { !it.isJsonNull }?.asString

        return when {
            code.isNullOrBlank() -> Outcome.Failure("回执中缺少 code")
            state.isNullOrBlank() -> Outcome.Failure("回执中缺少 state")
            else -> Outcome.Success(Result(code, state))
        }
    }

    private fun extractParams(text: String): Outcome? {
        if (!text.contains("code=") || !text.contains("state=")) return null

        val code = queryValue(text, "code")
        val state = queryValue(text, "state")
        return when {
            code.isNullOrBlank() -> Outcome.Failure("回执链接中缺少 code")
            state.isNullOrBlank() -> Outcome.Failure("回执链接中缺少 state")
            else -> Outcome.Success(Result(code, state))
        }
    }

    /** 从查询串中取值，对 URL 解码，忽略后续的 `#` 片段。 */
    private fun queryValue(text: String, key: String): String? {
        val query = text.substringAfter('?', text).substringBefore('#')
        return query.split('&')
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null else part.substring(0, idx) to part.substring(idx + 1)
            }
            .firstOrNull { it.first == key }
            ?.second
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    }

    /** 授权码通常是较长的十六进制或字母数字串。 */
    private fun looksLikeBareCode(text: String): Boolean =
        text.length in 16..512 && text.none { it.isWhitespace() } && !text.contains('=')
}
