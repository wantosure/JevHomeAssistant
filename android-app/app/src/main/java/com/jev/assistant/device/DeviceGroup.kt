package com.jev.assistant.device

import com.jev.assistant.data.DeviceItem

data class DeviceGroup(
    val id: String,
    val name: String,
    val scope: String, // "全屋" 或房间名
    val type: String,  // "lights", "acs", "curtains", "fans", ...
    val deviceIds: List<String>
)

/**
 * 设备编组与组指令匹配。
 *
 * 接入真实米家后，房间名由用户自定义（「北次卧」「南主卧」「菜园」），
 * 因此这里**不再依赖任何硬编码的房间名或组 ID**，全部由当前设备集合动态派生。
 */
object DeviceGroupManager {

    private val GROUP_KEYWORDS = listOf(
        "所有", "全部", "全屋", "整个家", "每个", "各个",
        "全关", "全开", "都关", "都开", "都调", "全都",
        "除了", "除开", "除去", "其他房间", "其余房间", "其余",
    )

    /**
     * 类别关键词 → 分组类别。
     *
     * 顺序即优先级；同一句话命中多个类别时取关键词更长的那个，
     * 因为更长的词通常更具体（「吸顶灯」比「灯」具体）。
     */
    private val CATEGORY_KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "lights" to listOf("吸顶灯", "筒灯", "射灯", "灯带", "台灯", "顶灯", "照明", "灯"),
        "acs" to listOf("空调", "冷气", "暖风", "制冷", "制热"),
        "curtains" to listOf("窗帘", "纱帘", "卷帘", "百叶"),
        "fans" to listOf("循环扇", "吊扇", "凉霸", "风扇"),
        "outlets" to listOf("插排", "插座", "通断器", "开关"),
        "purifiers" to listOf("新风", "净化"),
        "humidifiers" to listOf("加湿", "除湿"),
        "airers" to listOf("晾衣"),
        "heaters" to listOf("取暖", "电暖", "浴霸", "暖风机"),
    )

    /** 房间名含这些字时视为卧室，用于「所有卧室的灯」这类聚合。 */
    private val BEDROOM_HINTS = listOf("卧", "儿童房", "客房", "老人房", "主卧", "次卧")

    /** 表示"整个家"的说法——出现这些词时说明用户没有把动作限定到某个房间。 */
    private val GLOBAL_SCOPE_MARKERS = listOf(
        "全屋", "全部", "所有", "整个家", "全家", "每个", "各个", "家里", "全",
    )

    /** 泛指的房间类型——不是具体房间名，但属于合法的范围限定。 */
    private val GENERIC_ROOM_SCOPES = listOf("卧室", "睡房", "卧房", "房间")

    /** 从类别词之前剥离的填充词，用于判断用户是否指定了一个具体房间。 */
    private val SCOPE_FILLERS = listOf(
        "帮我", "请", "麻烦", "把", "将", "给", "想", "要", "我",
        "打开", "关闭", "关掉", "开掉", "开启", "调低", "调高", "调节", "调整",
        "全屋", "全部", "所有", "整个家", "每个", "各个",
        "一下", "的", "都", "房间", "里面",
    )

    fun isGroupCommand(text: String): Boolean =
        GROUP_KEYWORDS.any { text.contains(it) } || isExclusionCommand(text)

    /**
     * 判断是否为反向排除式批量控制（例如："除了客厅其他房间灯都关掉"）。
     */
    fun isExclusionCommand(text: String): Boolean =
        text.contains("除了") || text.contains("除开") || text.contains("除去") ||
            (text.contains("除") && (text.contains("外") || text.contains("其他") || text.contains("其余") || text.contains("所有") || text.contains("都")))

    // ---------------- 建组 ----------------

    /**
     * 由当前设备集合构建可用的组。
     *
     * 组 ID 保留中文房间名（如 `group_客厅_lights`）而非 MIoT 的不透明 roomId，
     * 因为该 ID 会出现在执行日志里，可读性更重要。
     */
    fun buildGroups(devices: List<DeviceItem>): List<DeviceGroup> {
        val groups = mutableListOf<DeviceGroup>()
        val categories = devices.mapNotNull { categoryOf(it) }.distinct()

        // 全屋维度
        categories.forEach { category ->
            val ids = devices.filter { categoryOf(it) == category }.map { it.logicalId }
            if (ids.isNotEmpty()) {
                groups.add(
                    DeviceGroup(
                        id = "group_all_$category",
                        name = "全屋所有${categoryLabel(category)}",
                        scope = DeviceRegistry.ALL_HOME,
                        type = category,
                        deviceIds = ids,
                    ),
                )
            }
        }

        // 房间维度
        val rooms = devices.map { it.room }.filter { it.isNotBlank() }.distinct()
        for (room in rooms) {
            val inRoom = devices.filter { it.room == room }
            categories.forEach { category ->
                val ids = inRoom.filter { categoryOf(it) == category }.map { it.logicalId }
                if (ids.isNotEmpty()) {
                    groups.add(
                        DeviceGroup(
                            id = "group_${room}_$category",
                            name = "$room${categoryLabel(category)}",
                            scope = room,
                            type = category,
                            deviceIds = ids,
                        ),
                    )
                }
            }
        }

        // 卧室聚合：不再硬编码"主卧/次卧/儿童房"，按房间名是否含"卧"等特征判定
        val bedrooms = rooms.filter { room -> BEDROOM_HINTS.any { room.contains(it) } }
        if (bedrooms.size >= 2) {
            categories.forEach { category ->
                val ids = devices
                    .filter { it.room in bedrooms && categoryOf(it) == category }
                    .map { it.logicalId }
                if (ids.isNotEmpty()) {
                    groups.add(
                        DeviceGroup(
                            id = "group_bedrooms_$category",
                            name = "所有卧室的${categoryLabel(category)}",
                            scope = "bedrooms",
                            type = category,
                            deviceIds = ids,
                        ),
                    )
                }
            }
        }

        return groups
    }

    // ---------------- 匹配 ----------------

    /**
     * 把一句组指令匹配到具体的设备组。
     *
     * **匹配不上时返回 null**，由上层退回到单设备决策路径。
     * 早期版本在这里返回 `groups.firstOrNull()` 作为兜底，在真实环境下
     * 任何未识别的语句都可能命中一个任意组，导致整批设备被误控——这是必须避免的。
     */
    fun matchGroup(
        text: String,
        groups: List<DeviceGroup>,
        devices: List<DeviceItem> = emptyList(),
        roomAliases: Map<String, String> = emptyMap(),
    ): DeviceGroup? {
        if (devices.isEmpty()) return null

        // 1. 反向排除优先（如"除了客厅其他房间灯都关掉"）
        if (isExclusionCommand(text)) {
            resolveExclusionGroup(text, devices, roomAliases)?.let { return it }
        }

        val category = detectCategory(text)
        val rooms = detectRooms(text, devices, roomAliases)
        val knownRooms = devices.map { it.room }.filter { it.isNotBlank() }.distinct()

        // 用户给动作限定了某个房间，但那个房间并不存在（如"关掉车库的所有灯"）。
        // 此时若退化成"全屋该类别"，就会把整个家的灯都关掉——必须拒绝匹配，
        // 交给上层去澄清或报找不到设备。
        if (rooms.isEmpty() && hasUnknownRoomScope(text, knownRooms)) {
            return null
        }

        return when {
            // 泛指"卧室"时走卧室聚合组
            rooms.isEmpty() && category != null && mentionsBedrooms(text) ->
                groups.firstOrNull { it.id == "group_bedrooms_$category" }
                    ?: groups.firstOrNull { it.id == "group_all_$category" }

            // 无房间、有类别 → 全屋该类别
            rooms.isEmpty() && category != null ->
                groups.firstOrNull { it.id == "group_all_$category" }

            // 有房间、有类别
            rooms.isNotEmpty() && category != null -> {
                if (rooms.size == 1) {
                    groups.firstOrNull { it.scope == rooms.first() && it.type == category }
                        ?: dynamicGroup(rooms, category, devices)
                } else {
                    dynamicGroup(rooms, category, devices)
                }
            }

            // 有房间、无类别 → 该房间的全部设备
            rooms.isNotEmpty() -> dynamicGroup(rooms, category = null, devices = devices)

            // 都没匹配上：不猜
            else -> null
        }
    }

    /** 是否泛指"卧室"（而非某个具体卧室名）。 */
    private fun mentionsBedrooms(text: String): Boolean =
        text.contains("卧室") || text.contains("睡房") || text.contains("卧房")

    /**
     * 判断话语是否把动作限定在某个**不存在**的房间上。
     *
     * 例："关掉车库的所有灯" 中「车库」不是任何设备的房间，属于用户说错了目标。
     * 这类情况必须拒绝匹配，否则会退化成"全屋灯全关"这种大范围误控。
     */
    private fun hasUnknownRoomScope(text: String, knownRooms: List<String>): Boolean {
        val keyword = CATEGORY_KEYWORDS
            .flatMap { it.second }
            .filter { text.contains(it) }
            .maxByOrNull { it.length }
            ?: return false

        val index = text.indexOf(keyword)
        if (index <= 0) return false

        // 取类别词之前的片段，剥离常见填充词后看是否剩下一个"房间样"的词
        var scope = text.substring(0, index)
        SCOPE_FILLERS.forEach { scope = scope.replace(it, "") }
        val token = scope.trim()

        if (token.isEmpty()) return false
        if (GLOBAL_SCOPE_MARKERS.any { token.contains(it) }) return false
        if (knownRooms.any { token.contains(it) }) return false
        // 泛指的房间类型（"所有卧室的灯"）不算未知房间，由卧室聚合处理
        if (GENERIC_ROOM_SCOPES.any { token.contains(it) }) return false
        return true
    }

    /** 判断一句话里提到的分组类别；没提到返回 null。 */
    private fun detectCategory(text: String): String? {
        var best: Pair<String, Int>? = null
        CATEGORY_KEYWORDS.forEach { (category, keywords) ->
            keywords.forEach { keyword ->
                if (text.contains(keyword)) {
                    val current = best
                    if (current == null || keyword.length > current.second) {
                        best = category to keyword.length
                    }
                }
            }
        }
        return best?.first
    }

    /**
     * 判断一句话里提到的房间。
     *
     * 长名优先，避免「主卧」抢先命中「南主卧」；同时把口语别名折算成真实房间名。
     */
    private fun detectRooms(
        text: String,
        devices: List<DeviceItem>,
        roomAliases: Map<String, String>,
    ): List<String> {
        val realRooms = devices.map { it.room }.filter { it.isNotBlank() }.distinct()
        val candidates = (realRooms + roomAliases.keys)
            .distinct()
            .sortedByDescending { it.length }

        val matched = mutableListOf<String>()
        for (name in candidates) {
            if (!text.contains(name)) continue
            val room = roomAliases[name] ?: name
            if (room !in matched) matched.add(room)
        }
        return matched
    }

    /**
     * 按房间集合与类别即时构造设备组。
     *
     * 用于匹配组合（如"主卧和次卧的灯"）或没有预建组的房间。
     */
    private fun dynamicGroup(
        rooms: List<String>,
        category: String?,
        devices: List<DeviceItem>,
    ): DeviceGroup? {
        val matched = devices.filter { device ->
            device.room in rooms && (category == null || categoryOf(device) == category)
        }
        if (matched.isEmpty()) return null

        val roomLabel = rooms.joinToString("和")
        val typeLabel = category?.let { categoryLabel(it) } ?: "设备"
        return DeviceGroup(
            id = "group_dyn_${rooms.joinToString("_")}_${category ?: "all"}",
            name = "$roomLabel$typeLabel",
            scope = roomLabel,
            type = category ?: "all",
            deviceIds = matched.map { it.logicalId },
        )
    }

    /**
     * 解析反向排除式控制，动态构建被排除房间以外的设备组。
     *
     * 这一层完全基于当前设备集合的房间名，因此天然适配用户自定义房间。
     */
    fun resolveExclusionGroup(
        text: String,
        devices: List<DeviceItem>,
        roomAliases: Map<String, String> = emptyMap(),
    ): DeviceGroup? {
        if (!isExclusionCommand(text)) return null

        val knownRooms = devices.map { it.room }.filter { it.isNotBlank() && it != DeviceRegistry.ALL_HOME }.distinct()
        val excludedRooms = mutableListOf<String>()

        // 提取被排除的部分：从 "除了/除开/除去/除" 开始截取
        val afterExclude = when {
            text.contains("除了") -> text.substringAfter("除了")
            text.contains("除开") -> text.substringAfter("除开")
            text.contains("除去") -> text.substringAfter("除去")
            text.contains("除") -> text.substringAfter("除")
            else -> text
        }

        // 截取到排除范围结束的分隔词
        val stopWords = listOf("以外", "之外", "外", "其他", "其余", "所有", "全部", "全屋", "的", "都")
        var roomPart = afterExclude
        for (sw in stopWords) {
            if (roomPart.contains(sw)) {
                roomPart = roomPart.substringBefore(sw)
                break
            }
        }

        // 在被排除的片段中匹配房间名；长名优先避免子串误判
        knownRooms.sortedByDescending { it.length }.forEach { room ->
            if (roomPart.contains(room)) excludedRooms.add(room)
        }
        roomAliases.forEach { (alias, canonical) ->
            if (roomPart.contains(alias) && canonical !in excludedRooms) {
                excludedRooms.add(canonical)
            }
        }

        // 容错兜底：片段截取不精确时，在全句中检查"除X"
        if (excludedRooms.isEmpty()) {
            knownRooms.forEach { room ->
                if (text.contains("除了$room") || text.contains("除开$room") || text.contains("除去$room")) {
                    excludedRooms.add(room)
                }
            }
            roomAliases.forEach { (alias, canonical) ->
                if (text.contains("除了$alias") || text.contains("除开$alias")) {
                    excludedRooms.add(canonical)
                }
            }
        }

        if (excludedRooms.isEmpty()) return null

        val category = detectCategory(text) ?: "lights"
        val excluded = excludedRooms.distinct()

        val matched = devices.filter { device ->
            device.room !in excluded && categoryOf(device) == category
        }
        if (matched.isEmpty()) return null

        val excludedName = excluded.joinToString("和")
        return DeviceGroup(
            id = "group_except_${excluded.joinToString("_")}_$category",
            name = "除${excludedName}外所有${categoryLabel(category)}",
            scope = "全屋(除${excludedName})",
            type = category,
            deviceIds = matched.map { it.logicalId },
        )
    }

    // ---------------- 类别推导 ----------------

    /**
     * 由设备的中文类型推导分组类别。
     *
     * 只用 `type` 字段的字符串特征，不依赖具体接入实现，因此对
     * 「灯具」「灯」这类写法差异都能兼容。
     */
    private fun categoryOf(device: DeviceItem): String? = when {
        device.type.contains("灯") -> "lights"
        device.type.contains("空调") -> "acs"
        device.type.contains("窗帘") -> "curtains"
        device.type.contains("风扇") -> "fans"
        device.type.contains("插座") || device.type.contains("开关") -> "outlets"
        device.type.contains("净化") || device.type.contains("新风") -> "purifiers"
        device.type.contains("加湿") || device.type.contains("除湿") -> "humidifiers"
        device.type.contains("晾衣") -> "airers"
        device.type.contains("取暖") || device.type.contains("浴霸") -> "heaters"
        else -> null
    }

    private fun categoryLabel(category: String): String = when (category) {
        "lights" -> "灯"
        "acs" -> "空调"
        "curtains" -> "窗帘"
        "fans" -> "风扇"
        "outlets" -> "插座"
        "purifiers" -> "净化器"
        "humidifiers" -> "加湿器"
        "airers" -> "晾衣架"
        "heaters" -> "取暖器"
        else -> "设备"
    }
}
