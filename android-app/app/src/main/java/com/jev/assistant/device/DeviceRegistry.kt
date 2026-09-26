package com.jev.assistant.device

import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.MappingStatus
import com.jev.assistant.miot.MiotHome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设备注册表：持有同步结果并向决策链路提供设备视图。
 *
 * 接入真实米家后设备不再来自随包资源，而是由适配器同步得到。本类只保存状态、
 * 派生分组与房间别名，不直接发起网络请求——同步由上层驱动并调用 [applySyncResult]。
 *
 * **多家庭语义**：真实账号常有多个地理上分离的家庭（如"家里"与"农场"），
 * 因此 [devicesFlow] 只包含**当前激活家庭**的设备。
 * 用户说"把灯都关了"绝不应同时关掉另一个城市的房子。
 * 「全屋」因此等价于"当前家庭的全部设备"，而不是"账号下全部设备"。
 */
class DeviceRegistry {

    /** 全部设备（跨家庭），仅用于家庭统计与切换。 */
    private var allDevices: List<DeviceItem> = emptyList()

    private val _devicesFlow = MutableStateFlow<List<DeviceItem>>(emptyList())

    /** 当前激活家庭的设备。决策链路只应使用这一份。 */
    val devicesFlow: StateFlow<List<DeviceItem>> = _devicesFlow.asStateFlow()

    private val _roomsFlow = MutableStateFlow<List<String>>(emptyList())

    /** 当前家庭的房间列表，首项恒为 [ALL_HOME]。 */
    val roomsFlow: StateFlow<List<String>> = _roomsFlow.asStateFlow()

    private val _groupsFlow = MutableStateFlow<List<DeviceGroup>>(emptyList())
    val groupsFlow: StateFlow<List<DeviceGroup>> = _groupsFlow.asStateFlow()

    private val _homesFlow = MutableStateFlow<List<MiotHome>>(emptyList())
    val homesFlow: StateFlow<List<MiotHome>> = _homesFlow.asStateFlow()

    private val _activeHomeIdFlow = MutableStateFlow<String?>(null)
    val activeHomeIdFlow: StateFlow<String?> = _activeHomeIdFlow.asStateFlow()

    private val _adapterStatusFlow = MutableStateFlow<AdapterStatus>(AdapterStatus.Unbound)
    val adapterStatusFlow: StateFlow<AdapterStatus> = _adapterStatusFlow.asStateFlow()

    /**
     * 口语房间别名，由真实房间名派生（见 [RoomAliasResolver]）。
     * 每次同步后重算，不再硬编码。
     */
    var roomAliases: Map<String, String> = emptyMap()
        private set

    /** 当前激活家庭；未同步时为 null。 */
    val activeHomeName: String?
        get() = homesFlow.value.firstOrNull { it.homeId == _activeHomeIdFlow.value }?.name

    // ---------------- 状态写入 ----------------

    /**
     * 应用一次同步结果。
     *
     * @param preferredHomeId 上次选中的家庭；为空或已不存在时自动选择一个。
     */
    fun applySyncResult(
        devices: List<DeviceItem>,
        homes: List<MiotHome>,
        preferredHomeId: String? = null,
    ) {
        allDevices = devices
        _homesFlow.value = homes

        val targetHome = preferredHomeId
            ?.takeIf { id -> homes.any { it.homeId == id } }
            ?: homes.maxByOrNull { it.deviceCount }?.homeId
            ?: devices.firstOrNull()?.homeId

        _activeHomeIdFlow.value = targetHome
        rebuildDerived()
    }

    /** 切换当前家庭。 */
    fun setActiveHome(homeId: String) {
        if (homeId == _activeHomeIdFlow.value) return
        if (homesFlow.value.none { it.homeId == homeId }) return
        _activeHomeIdFlow.value = homeId
        rebuildDerived()
    }

    fun updateAdapterStatus(status: AdapterStatus) {
        _adapterStatusFlow.value = status
    }

    /** 解绑后清空全部设备状态。 */
    fun clear() {
        allDevices = emptyList()
        _homesFlow.value = emptyList()
        _activeHomeIdFlow.value = null
        rebuildDerived()
    }

    /** 用回读到的真实属性更新本地状态。 */
    fun applyPropertyUpdate(logicalId: String, values: Map<String, Any>) {
        if (values.isEmpty()) return
        replaceDevice(logicalId) { device ->
            val merged = device.currentState.toMutableMap()
            merged.putAll(values)
            device.copy(
                currentState = merged,
                stateReadAt = System.currentTimeMillis(),
            )
        }
    }

    /** 兼容单属性更新。 */
    fun updateDeviceProperty(logicalId: String, key: String, value: Any) {
        applyPropertyUpdate(logicalId, mapOf(key to value))
    }

    /**
     * 标记设备能力已通过真实回读验证。
     *
     * 只有验证过的设备才在 LIVE 模式下真正下发，这是设备协议文档的硬性要求。
     */
    fun markVerified(logicalId: String) {
        replaceDevice(logicalId) { device ->
            if (device.mappingStatus == MappingStatus.MAPPED) {
                device.copy(mappingStatus = MappingStatus.VERIFIED)
            } else {
                device
            }
        }
    }

    // ---------------- 查询 ----------------

    fun findDeviceById(id: String): DeviceItem? =
        _devicesFlow.value.firstOrNull { it.logicalId == id || it.sourceDeviceId == id }

    /** 明确出现在话语中的房间；未提及返回 null。 */
    fun extractExplicitRoom(text: String): String? {
        // 长名优先，避免「主卧」抢先命中「南主卧」
        val candidates = (_roomsFlow.value.filter { it != ALL_HOME } + roomAliases.keys)
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { it.length }

        for (name in candidates) {
            if (text.contains(name)) {
                return roomAliases[name] ?: name
            }
        }
        return null
    }

    fun resolveRoom(text: String, defaultRoom: String): String =
        extractExplicitRoom(text) ?: defaultRoom

    /**
     * 供决策使用的候选设备集合。
     *
     * 刻意**不按当前房间裁剪**：用户人在客厅也可能说"关掉主卧的灯"，
     * 丢设备比多给候选危险得多。提到房间时把该房间排前即可。
     */
    fun getCandidatesForUtterance(text: String, currentRoom: String): List<DeviceItem> {
        val all = _devicesFlow.value
        val explicitRoom = extractExplicitRoom(text)

        if (explicitRoom != null) {
            val inRoom = all.filter { it.room == explicitRoom }
            val others = all.filter { it.room != explicitRoom }
            return inRoom + others
        }

        if (currentRoom == ALL_HOME || currentRoom.isBlank()) return all

        val inCurrent = all.filter { it.room == currentRoom }
        val others = all.filter { it.room != currentRoom }
        return inCurrent + others
    }

    // ---------------- 内部 ----------------

    private fun replaceDevice(logicalId: String, transform: (DeviceItem) -> DeviceItem) {
        val index = allDevices.indexOfFirst { it.logicalId == logicalId }
        if (index == -1) return
        allDevices = allDevices.toMutableList().also { it[index] = transform(it[index]) }
        rebuildDerived()
    }

    private fun rebuildDerived() {
        val activeId = _activeHomeIdFlow.value
        val scoped = if (activeId == null) {
            emptyList()
        } else {
            allDevices.filter { it.homeId == activeId }
        }
        _devicesFlow.value = scoped

        val roomNames = scoped
            .filter { it.roomAssigned }
            .map { it.room }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        _roomsFlow.value = listOf(ALL_HOME) + roomNames
        roomAliases = RoomAliasResolver.build(roomNames)
        _groupsFlow.value = DeviceGroupManager.buildGroups(scoped)
    }

    companion object {
        /** 「全屋」的语义是当前激活家庭，不是账号下全部家庭。 */
        const val ALL_HOME = "全屋"
    }
}
