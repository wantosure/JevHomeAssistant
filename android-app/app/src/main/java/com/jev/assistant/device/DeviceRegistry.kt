package com.jev.assistant.device

import android.content.Context
import com.google.gson.Gson
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.MappingStatus
import com.jev.assistant.data.MijiaCatalogJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStreamReader

class DeviceRegistry(private val context: Context) {

    private val _devicesFlow = MutableStateFlow<List<DeviceItem>>(emptyList())
    val devicesFlow: StateFlow<List<DeviceItem>> = _devicesFlow.asStateFlow()

    private val _roomsFlow = MutableStateFlow<List<String>>(emptyList())
    val roomsFlow: StateFlow<List<String>> = _roomsFlow.asStateFlow()

    private val _groupsFlow = MutableStateFlow<List<DeviceGroup>>(emptyList())
    val groupsFlow: StateFlow<List<DeviceGroup>> = _groupsFlow.asStateFlow()

    val roomAliases = mapOf(
        "卧室" to "主卧",
        "我的卧室" to "主卧",
        "大厅" to "客厅",
        "玄关" to "门厅",
        "门口" to "门厅",
        "小卧" to "次卧",
        "小卧室" to "次卧",
        "工具室" to "小工具",
        "机房" to "后台",
        "工作室" to "基地"
    )

    init {
        loadCatalogFromAssets()
    }

    private fun loadCatalogFromAssets() {
        try {
            val inputStream = context.assets.open("mijia-100-devices.json")
            val reader = InputStreamReader(inputStream, Charsets.UTF_8)
            val catalog = Gson().fromJson(reader, MijiaCatalogJson::class.java)
            reader.close()

            val roomList = mutableListOf("全屋")
            roomList.addAll(catalog.rooms.map { it.name }.distinct())
            _roomsFlow.value = roomList

            val deviceItems = catalog.devices.map { raw ->
                val capsList = raw.capabilities.map { it.key }
                val initialMap = mutableMapOf<String, Any>()
                raw.capabilities.forEach { cap ->
                    cap.value?.let { v -> initialMap[cap.key] = v }
                }
                if (!initialMap.containsKey("power") && capsList.contains("power")) {
                    initialMap["power"] = 0
                }

                DeviceItem(
                    logicalId = raw.id,
                    sourceDeviceId = raw.id,
                    name = raw.name,
                    shortName = raw.shortName,
                    room = raw.room,
                    roomId = raw.roomId,
                    type = raw.type,
                    icon = when {
                        raw.type.contains("灯") -> "💡"
                        raw.type.contains("空调") -> "❄️"
                        raw.type.contains("风扇") -> "🌀"
                        raw.type.contains("窗帘") -> "🪟"
                        raw.type.contains("插座") || raw.type.contains("开关") -> "🔌"
                        raw.type.contains("音箱") -> "🔊"
                        raw.type.contains("加湿") -> "💧"
                        raw.type.contains("晾衣") -> "👔"
                        else -> "📱"
                    },
                    capabilities = capsList,
                    rawCapabilities = raw.capabilities,
                    mappingStatus = MappingStatus.SIMULATED_ACTIVE,
                    currentState = initialMap
                )
            }
            _devicesFlow.value = deviceItems
            _groupsFlow.value = DeviceGroupManager.buildGroups(deviceItems)
        } catch (_: Exception) {
            _roomsFlow.value = listOf("全屋", "客厅", "主卧", "次卧", "厨房", "阳台")
        }
    }

    fun findDeviceById(id: String): DeviceItem? {
        return _devicesFlow.value.find { it.logicalId == id || it.sourceDeviceId == id }
    }

    // 智能提取用户言语中的明确房间
    fun extractExplicitRoom(text: String): String? {
        for ((alias, canonical) in roomAliases) {
            if (text.contains(alias)) return canonical
        }
        val allRooms = _roomsFlow.value.filter { it != "全屋" }
        for (r in allRooms) {
            if (text.contains(r)) return r
        }
        return null
    }

    fun resolveRoom(text: String, defaultRoom: String): String {
        val explicit = extractExplicitRoom(text)
        if (explicit != null) return explicit
        return if (defaultRoom == "全屋") "全屋" else defaultRoom
    }

    /**
     * 关键修复：不要在客厅就只传客厅的设备！
     * 无论在哪个房间，都要返回全屋范围内最相关的候选集合，若提到其他房间优先排列该房间设备。
     */
    fun getCandidatesForUtterance(text: String, currentRoom: String): List<DeviceItem> {
        val all = _devicesFlow.value
        val explicitRoom = extractExplicitRoom(text)

        // 1. 如果用户显式提到某个房间（如“主卧灯”），优先把主卧设备排在最前
        if (explicitRoom != null) {
            val roomDevs = all.filter { it.room == explicitRoom }
            val otherDevs = all.filter { it.room != explicitRoom }
            return roomDevs + otherDevs.take(20)
        }

        // 2. 如果用户没有显式提房间
        if (currentRoom == "全屋" || currentRoom.isBlank()) {
            return all
        }

        // 3. 如果当前所在是某个房间（如客厅），把客厅的排在前，但绝不丢弃全屋其他房间同类型设备！
        val currentDevs = all.filter { it.room == currentRoom }
        val otherDevs = all.filter { it.room != currentRoom }
        return currentDevs + otherDevs
    }

    fun updateDeviceProperty(deviceId: String, key: String, value: Any) {
        val current = _devicesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.logicalId == deviceId }
        if (index != -1) {
            val dev = current[index]
            val newMap = dev.currentState.toMutableMap()
            newMap[key] = value
            current[index] = dev.copy(currentState = newMap)
            _devicesFlow.value = current
        }
    }
}
