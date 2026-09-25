package com.jev.assistant.utils

import java.util.regex.Pattern

object NumericExtractor {

    private val DIGITS = mapOf(
        '零' to "0", '〇' to "0", '一' to "1", '二' to "2", '两' to "2",
        '三' to "3", '四' to "4", '五' to "5", '六' to "6", '七' to "7",
        '八' to "8", '九' to "9"
    )

    private val UNITS = mapOf(
        '十' to 10L,
        '百' to 100L,
        '千' to 1000L,
        '万' to 10000L
    )

    /**
     * 提取用户话语中的全部数字候选列表（字符串形式，如 ["30", "87", "4000"]）
     * 严格对齐 Jev 原版 numeric_values.py 算法，支持 30%、百分之三十、一二三四、十五、八十七、四千等
     */
    fun extractCandidates(text: String): List<String> {
        val cleanText = text.replace("百分之", "").replace("%", "")
        val pattern = Pattern.compile("[负-]?[0-9零〇一二两三四五六七八九十百千万]+(?:[点.][0-9零〇一二两三四五六七八九]+)?")
        val matcher = pattern.matcher(cleanText)
        val results = mutableListOf<String>()

        while (matcher.find()) {
            val raw = matcher.group()
            val parsed = parseSingleRaw(raw)
            if (parsed != null && !results.contains(parsed)) {
                results.add(parsed)
            }
        }
        return results
    }

    /**
     * 提取数值候选列表为 Double 类型
     */
    fun extractNumbers(text: String): List<Double> {
        return extractCandidates(text).mapNotNull { it.toDoubleOrNull() }
    }

    /**
     * 便捷方法：获取文本中第一个整数
     */
    fun extractFirstInt(text: String): Int? {
        val candidates = extractNumbers(text)
        return candidates.firstOrNull()?.toInt()
    }

    /**
     * 便捷方法：获取文本中第一个浮点数
     */
    fun extractFirstDouble(text: String): Double? {
        val candidates = extractNumbers(text)
        return candidates.firstOrNull()
    }

    private fun parseSingleRaw(raw: String): String? {
        val isNegative = raw.startsWith("负") || raw.startsWith("-")
        val stripped = raw.trimStart('负', '-')

        // 纯阿拉伯数字
        if (stripped.matches(Regex("\\d+(\\.\\d+)?"))) {
            val v = stripped.toDoubleOrNull() ?: return null
            val finalVal = if (isNegative) -v else v
            return if (finalVal % 1.0 == 0.0) finalVal.toLong().toString() else finalVal.toString()
        }

        val parts = stripped.split("[点.]".toRegex())
        val wholePart = parts[0]
        val hasUnits = wholePart.any { it in UNITS }

        val wholeVal: Double
        if (!hasUnits) {
            val sb = StringBuilder()
            for (c in wholePart) {
                if (c in '0'..'9') sb.append(c)
                else if (DIGITS.containsKey(c)) sb.append(DIGITS[c])
            }
            wholeVal = sb.toString().toDoubleOrNull() ?: return null
        } else {
            var total = 0L
            var section = 0L
            var pending = ""

            for (c in wholePart) {
                if (c in UNITS) {
                    val u = UNITS[c]!!
                    val pVal = if (pending.isEmpty()) 1L else pending.toLongOrNull() ?: 1L
                    if (u == 10000L) {
                        total += (section + pVal) * 10000L
                        section = 0L
                    } else {
                        section += pVal * u
                    }
                    pending = ""
                } else if (c in '0'..'9') {
                    pending += c
                } else if (DIGITS.containsKey(c)) {
                    pending += DIGITS[c]
                }
            }
            val remaining = if (pending.isEmpty()) 0L else pending.toLongOrNull() ?: 0L
            total += section + remaining
            wholeVal = total.toDouble()
        }

        var finalVal = wholeVal
        if (parts.size > 1) {
            val fracSb = StringBuilder("0.")
            for (c in parts[1]) {
                if (c in '0'..'9') fracSb.append(c)
                else if (DIGITS.containsKey(c)) fracSb.append(DIGITS[c])
            }
            finalVal += fracSb.toString().toDoubleOrNull() ?: 0.0
        }

        if (isNegative) finalVal = -finalVal
        return if (finalVal % 1.0 == 0.0) finalVal.toLong().toString() else finalVal.toString()
    }
}
