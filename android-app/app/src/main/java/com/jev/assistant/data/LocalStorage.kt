package com.jev.assistant.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalStorage(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jev_assistant_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _logsFlow = MutableStateFlow<List<SilentLog>>(emptyList())
    val logsFlow: StateFlow<List<SilentLog>> = _logsFlow.asStateFlow()

    private val _alarmsFlow = MutableStateFlow<List<AlarmRecord>>(emptyList())
    val alarmsFlow: StateFlow<List<AlarmRecord>> = _alarmsFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow<List<UtteranceHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<UtteranceHistoryItem>> = _historyFlow.asStateFlow()

    private val _totalCostFlow = MutableStateFlow(prefs.getFloat("total_jev_cost", 0.0f).toDouble())
    val totalCostFlow: StateFlow<Double> = _totalCostFlow.asStateFlow()

    private val _totalCallsFlow = MutableStateFlow(prefs.getInt("total_jev_calls", 0))
    val totalCallsFlow: StateFlow<Int> = _totalCallsFlow.asStateFlow()

    init {
        loadAlarms()
        loadLogs()
        loadHistory()
    }

    companion object {
        // 内置开发者 Key 经由异或混淆存储，禁止明文硬编码防止静态逆向
        private val OBFUSCATED_KEY_BYTES = byteArrayOf(
            59, 42, 51, 49, 63, 35, 5, 104, 107, 110, 104, 59, 63, 56, 60, 105, 62, 105, 59, 62, 107, 110, 63, 110, 110, 99, 56, 56, 110, 104, 105, 111, 63, 98, 57, 59, 111, 56, 111, 63, 105, 106, 108, 5, 98, 111, 56, 108, 110, 108, 110, 108, 105, 60, 110, 99, 104, 60, 99, 60, 60, 56, 62, 57, 108, 63, 106, 59, 109, 63, 106, 110, 56, 57, 111, 62, 56, 107, 111, 108, 105, 60, 107, 106, 106, 60, 98, 62, 99, 98, 111, 56, 56, 105, 99, 107, 109, 111, 107, 99, 111, 104, 98, 111, 105, 59, 109, 59
        )
        private const val OBFUSCATION_MASK = 0x5A

        private fun decodeBuiltinKey(): String {
            val chars = CharArray(OBFUSCATED_KEY_BYTES.size)
            for (i in OBFUSCATED_KEY_BYTES.indices) {
                chars[i] = (OBFUSCATED_KEY_BYTES[i].toInt() xor OBFUSCATION_MASK).toChar()
            }
            return String(chars)
        }
    }

    // Config: 获取当前生效的真实 Key 发起 HTTP 请求（若无自定义则使用解密后的内置 Key）
    fun getApiKey(): String {
        val saved = prefs.getString("jev_api_key", "")?.trim() ?: ""
        return if (saved.isNotBlank()) saved else decodeBuiltinKey()
    }

    // 是否正在使用用户自定义替换的 Key
    fun isUsingCustomKey(): Boolean {
        return (prefs.getString("jev_api_key", "")?.trim() ?: "").isNotBlank()
    }

    // 用于 UI 界面脱敏安全展示，绝不输出原始内置 Key 明文
    fun getMaskedApiKeyForDisplay(): String {
        val custom = prefs.getString("jev_api_key", "")?.trim() ?: ""
        return if (custom.isNotBlank()) {
            if (custom.length > 16) {
                "${custom.take(8)}••••••••••••${custom.takeLast(6)}"
            } else {
                "••••••••••••"
            }
        } else {
            "🔒 [系统内置开发者 Key · 已加密保护]"
        }
    }

    fun setApiKey(key: String) = prefs.edit().putString("jev_api_key", key.trim()).apply()
    fun clearCustomApiKey() = prefs.edit().remove("jev_api_key").apply()

    fun getDefaultRoom(): String = prefs.getString("default_room", "客厅") ?: "客厅"
    fun setDefaultRoom(room: String) = prefs.edit().putString("default_room", room).apply()

    fun isLiveMode(): Boolean = prefs.getBoolean("is_live_mode", false)
    fun setLiveMode(live: Boolean) = prefs.edit().putBoolean("is_live_mode", live).apply()

    fun getConfidenceThreshold(): Float = prefs.getFloat("confidence_threshold", 0.35f)
    fun setConfidenceThreshold(threshold: Float) =
        prefs.edit().putFloat("confidence_threshold", threshold).apply()

    fun getPcAgentUrl(): String =
        prefs.getString("pc_agent_url", "http://192.168.1.100:8765") ?: "http://192.168.1.100:8765"
    fun setPcAgentUrl(url: String) = prefs.edit().putString("pc_agent_url", url).apply()

    fun getPcAgentToken(): String =
        prefs.getString("pc_agent_token", "dev-secret-token") ?: "dev-secret-token"
    fun setPcAgentToken(token: String) = prefs.edit().putString("pc_agent_token", token).apply()

    // Logs
    @Synchronized
    fun addLog(log: SilentLog) {
        val current = _logsFlow.value.toMutableList()
        current.add(0, log)
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _logsFlow.value = current
        saveLogs()
    }

    private fun saveLogs() {
        val json = gson.toJson(_logsFlow.value)
        prefs.edit().putString("cached_logs", json).apply()
    }

    private fun loadLogs() {
        val json = prefs.getString("cached_logs", null) ?: return
        try {
            val type = object : TypeToken<List<SilentLog>>() {}.type
            val list: List<SilentLog> = gson.fromJson(json, type) ?: emptyList()
            _logsFlow.value = list
        } catch (_: Exception) {}
    }

    // Alarms
    @Synchronized
    fun addAlarm(alarm: AlarmRecord) {
        val current = _alarmsFlow.value.toMutableList()
        current.removeAll { it.id == alarm.id }
        current.add(0, alarm)
        _alarmsFlow.value = current
        saveAlarms()
    }

    @Synchronized
    fun removeAlarm(id: String) {
        val current = _alarmsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        _alarmsFlow.value = current
        saveAlarms()
    }

    private fun saveAlarms() {
        val json = gson.toJson(_alarmsFlow.value)
        prefs.edit().putString("cached_alarms", json).apply()
    }

    private fun loadAlarms() {
        val json = prefs.getString("cached_alarms", null) ?: return
        try {
            val type = object : TypeToken<List<AlarmRecord>>() {}.type
            val list: List<AlarmRecord> = gson.fromJson(json, type) ?: emptyList()
            _alarmsFlow.value = list
        } catch (_: Exception) {}
    }

    // 历史会话 (需求 2)
    @Synchronized
    fun addUtteranceHistory(item: UtteranceHistoryItem) {
        val current = _historyFlow.value.toMutableList()
        current.add(0, item)
        if (current.size > 150) {
            current.removeAt(current.size - 1)
        }
        _historyFlow.value = current
        saveHistory()
    }

    @Synchronized
    fun clearHistory() {
        _historyFlow.value = emptyList()
        prefs.edit().remove("cached_history").apply()
    }

    private fun saveHistory() {
        val json = gson.toJson(_historyFlow.value)
        prefs.edit().putString("cached_history", json).apply()
    }

    private fun loadHistory() {
        val json = prefs.getString("cached_history", null) ?: return
        try {
            val type = object : TypeToken<List<UtteranceHistoryItem>>() {}.type
            val list: List<UtteranceHistoryItem> = gson.fromJson(json, type) ?: emptyList()
            _historyFlow.value = list
        } catch (_: Exception) {}
    }

    // 费用统计 (需求 4)
    @Synchronized
    fun recordJevCost(cost: Double) {
        val newCost = _totalCostFlow.value + cost
        val newCalls = _totalCallsFlow.value + 1
        _totalCostFlow.value = newCost
        _totalCallsFlow.value = newCalls
        prefs.edit()
            .putFloat("total_jev_cost", newCost.toFloat())
            .putInt("total_jev_calls", newCalls)
            .apply()
    }

    fun getTotalJevCost(): Double = _totalCostFlow.value
    fun getTotalJevCalls(): Int = _totalCallsFlow.value
    fun resetJevCost() {
        _totalCostFlow.value = 0.0
        _totalCallsFlow.value = 0
        prefs.edit().putFloat("total_jev_cost", 0.0f).putInt("total_jev_calls", 0).apply()
    }
}
