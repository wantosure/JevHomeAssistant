package com.jev.assistant.miot

import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.MappingStatus

/**
 * 一个家庭。
 */
data class MiotHome(
    val homeId: String,
    val name: String,
    val deviceCount: Int,
    val onlineCount: Int,
    val roomCount: Int,
)

/**
 * 一次同步的完整结果。
 */
data class MiotSyncResult(
    val homes: List<MiotHome>,
    val devices: List<DeviceItem>,
    val syncedAt: Long,
)

/**
 * 设备同步：把家庭、房间、设备与能力聚合成本项目的设备模型。
 *
 * 关键点：**家庭与房间信息来自家庭接口，不来自设备接口**。
 * 设备接口只返回 did/名称/型号等，不含归属；两者需要用 did 做关联。
 */
class MiotDeviceRepository(
    private val cloud: MiotCloudClient,
    private val specRepository: MiotSpecRepository,
    private val cache: MiotDeviceCache? = null,
) {

    /** 设备在家庭中的归属。 */
    private data class Placement(
        val homeId: String,
        val homeName: String,
        val roomId: String,
        val roomName: String,
        /** 是否被明确归入某个房间；不属于任何房间的设备为 false。 */
        val assigned: Boolean,
    )

    /** 在线同步：拉取家庭与设备，并写入本地快照。 */
    suspend fun sync(): Result<MiotSyncResult> {
        val homes = fetchHomes().getOrElse { return Result.failure(it) }
        val placements = buildPlacements(homes)
        if (placements.isEmpty()) {
            return Result.success(MiotSyncResult(emptyList(), emptyList(), nowSec()))
        }

        val rawDevices = cloud.getAllDevices(placements.keys.toList())
            .getOrElse { return Result.failure(it) }

        cache?.save(homes, rawDevices)
        return Result.success(assemble(homes, rawDevices))
    }

    /**
     * 从本地快照重建，不发起任何网络请求。
     *
     * 用于冷启动或断网时展示已有设备；能力由规范缓存提供，
     * 若规范未被缓存过，对应设备会退化为 `DRAFT`（不可下发），这是安全侧降级。
     */
    suspend fun loadCached(): MiotSyncResult? {
        val (homes, devices) = cache?.load() ?: return null
        return assemble(homes, devices)
    }

    /**
     * 由原始接口数据构建同步结果。在线同步与离线重建共用同一段逻辑，
     * 保证两条路径产出的设备模型完全一致。
     */
    private suspend fun assemble(
        homes: List<MiotHomeDto>,
        rawDevices: List<MiotDeviceDto>,
    ): MiotSyncResult {
        val placements = buildPlacements(homes)
        val devices = rawDevices.map { dto -> buildDeviceItem(dto, placements) }

        // 子设备的房间继承：did 形如 `123.s1` 的设备通常不在房间列表中，
        // 用父设备的房间补齐，避免它们全部落到"未分配房间"。
        val bySourceId = devices.associateBy { it.sourceDeviceId }
        val resolved = devices.map { device ->
            if (device.roomAssigned) {
                device
            } else {
                val parent = device.parentId?.let { bySourceId[it] }
                if (parent != null) {
                    device.copy(
                        roomId = parent.roomId,
                        room = parent.room,
                        roomAssigned = true,
                        homeId = parent.homeId,
                        homeName = parent.homeName,
                    )
                } else {
                    device
                }
            }
        }

        return MiotSyncResult(summarizeHomes(homes, resolved), resolved, nowSec())
    }

    // ---------------- 家庭与房间 ----------------

    /**
     * 拉取全部家庭。首页之后的分页由另一个接口提供，
     * 且其列表字段名与首页不同（`info` vs `homelist`）。
     */
    private suspend fun fetchHomes(): Result<List<MiotHomeDto>> {
        val first = cloud.getHomes().getOrElse { return Result.failure(it) }
        val homes = first.homeList.toMutableList()

        var cursor = if (first.hasMore) first.maxId else null
        var guard = 0
        while (cursor != null && guard++ < MAX_HOME_PAGES) {
            val page = cloud.getDevRoomPage(cursor).getOrElse { return Result.failure(it) }
            homes += page.info
            cursor = if (page.hasMore) page.maxId else null
        }
        return Result.success(homes)
    }

    /**
     * 建立 did → 房间归属的映射。
     *
     * 不属于任何房间的设备会退化为使用家庭名作为房间名——这是协议行为，
     * 此处如实保留，同时用 [Placement.assigned] 显式记录，避免下游只能靠字符串比较去猜。
     */
    private fun buildPlacements(homes: List<MiotHomeDto>): Map<String, Placement> {
        val map = mutableMapOf<String, Placement>()

        homes.forEach { home ->
            if (home.id.isBlank()) return@forEach

            // 先铺一层"不属于任何房间"的默认归属
            home.dids.forEach { did ->
                map[did] = Placement(
                    homeId = home.id,
                    homeName = home.name,
                    roomId = home.id,
                    roomName = home.name,
                    assigned = false,
                )
            }
            // 再让明确的房间归属覆盖它
            home.roomList.forEach { room ->
                room.dids.forEach { did ->
                    map[did] = Placement(
                        homeId = home.id,
                        homeName = home.name,
                        roomId = room.id,
                        roomName = room.name,
                        assigned = true,
                    )
                }
            }
        }
        return map
    }

    private fun summarizeHomes(homes: List<MiotHomeDto>, devices: List<DeviceItem>): List<MiotHome> =
        homes.filter { it.id.isNotBlank() }.map { home ->
            val owned = devices.filter { it.homeId == home.id }
            MiotHome(
                homeId = home.id,
                name = home.name,
                deviceCount = owned.size,
                onlineCount = owned.count { it.isOnline },
                roomCount = owned.map { it.roomId }.distinct().size,
            )
        }

    // ---------------- 单台设备 ----------------

    private suspend fun buildDeviceItem(
        dto: MiotDeviceDto,
        placements: Map<String, Placement>,
    ): DeviceItem {
        val placement = placements[dto.did]
        val urn = resolveUrn(dto)
        val spec = urn?.let { specRepository.get(it).getOrNull() }

        val profile = urn?.let { MiotSpecParser.typeNameOf(it) }.orEmpty()
        val capabilities = spec?.let { MiotCapabilityMapper.map(it) }.orEmpty()

        // 规范解析不出来时退回按名称猜类型，仅供展示与分组，能力保持为空
        val guessed = if (profile.isBlank()) MiotTypeTable.guessFromName(dto.name) else null
        val type = guessed?.first ?: MiotTypeTable.chineseType(profile)
        val icon = guessed?.second ?: MiotTypeTable.icon(profile)
        val category = guessed?.third ?: MiotTypeTable.categoryOrNull(profile)

        val mappingStatus = when {
            capabilities.isEmpty() -> MappingStatus.DRAFT
            category != null && !MiotTypeTable.isControllable(category) -> MappingStatus.DISABLED
            else -> MappingStatus.MAPPED
        }

        // 只读设备即便有可写能力也不允许下发
        val effectiveCaps = if (mappingStatus == MappingStatus.DISABLED) {
            capabilities.map { it.copy(kind = "read") }
        } else {
            capabilities
        }

        return DeviceItem(
            logicalId = logicalIdOf(dto.did),
            sourceDeviceId = dto.did,
            name = dto.name,
            shortName = deriveShortName(dto.name, placement?.roomName),
            room = placement?.roomName.orEmpty(),
            roomId = placement?.roomId.orEmpty(),
            type = type,
            icon = icon,
            capabilities = effectiveCaps.map { it.key },
            rawCapabilities = effectiveCaps,
            mappingStatus = mappingStatus,
            currentState = mutableMapOf(),
            profile = profile,
            urn = urn.orEmpty(),
            model = dto.model,
            homeId = placement?.homeId.orEmpty(),
            homeName = placement?.homeName.orEmpty(),
            roomAssigned = placement?.assigned ?: false,
            isOnline = dto.isOnline,
            parentId = dto.parentId ?: dto.parentDidFromSuffix,
            subDeviceKey = dto.subDeviceKey,
        )
    }

    /**
     * 解析设备规范 URN。
     *
     * 设备接口一般直接给出 `spec_type`；缺失时按型号反查，
     * 该路径位于 miot-spec.org 的非公开接口下，失败属于可预期情况，不应中断同步。
     */
    private suspend fun resolveUrn(dto: MiotDeviceDto): String? {
        dto.specType?.takeIf { it.isNotBlank() }?.let { return it }
        if (dto.model.isBlank()) return null
        return cloud.resolveUrnByModel(dto.model).getOrNull()?.takeIf { it.isNotBlank() }
    }

    /**
     * 由设备名去掉房间前缀得到短名，便于口语匹配。
     *
     * 例如「北次卧主灯」+ 房间「北次卧」→「主灯」。结果过短则留空，避免产生无意义的短名。
     */
    private fun deriveShortName(name: String, roomName: String?): String {
        if (roomName.isNullOrBlank()) return ""
        val stripped = name.removePrefix(roomName).trim()
        return if (stripped.length >= 2 && stripped != name) stripped else ""
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    companion object {
        private const val MAX_HOME_PAGES = 20

        /** 由物理 did 生成逻辑 ID。与 did 不同串，避免混淆逻辑标识与物理标识。 */
        fun logicalIdOf(did: String): String = "miot_${did.replace('.', '_')}"

        /** 由逻辑 ID 还原物理 did，供执行时使用。 */
        fun sourceIdOf(logicalId: String): String? =
            logicalId.removePrefix("miot_").replace('_', '.').takeIf { it != logicalId }
    }
}
