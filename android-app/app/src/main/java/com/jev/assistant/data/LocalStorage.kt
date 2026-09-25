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
        const val DEFAULT_BUILTIN_KEY = "apikey_2142aebf3d3ad14e449bb4235e8ca5b5e306_85b646463f492f9ffbdc6e0a7e04bc5db1563f100f8d985bb391751952853a7a"
    }

    // Config
    fun getApiKey(): String {
        val saved = prefs.getString("jev_api_key", "")?.trim() ?: ""
        return if (saved.isNotBlank()) saved else DEFAULT_BUILTIN_KEY
    }
    fun setApiKey(key: String) = prefs.edit().putString("jev_api_key", key.trim()).apply()

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
