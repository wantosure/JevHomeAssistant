package com.jev.assistant.device

/**
 * 由真实房间名派生口语别名。
 *
 * 接入真实米家后，房间名由用户自己起（「北次卧」「南主卧」「菜园」），
 * 原先硬编码的「卧室→主卧」这类映射不再适用，需要从实际房间名推导。
 *
 * 歧义一律放弃：若两个房间会派生出同一别名（「北次卧」「南次卧」都 →「次卧」），
 * 宁可让该别名失效走澄清路径，也不能猜错房间去控制设备。
 */
object RoomAliasResolver {

    /**
     * 跨词的口语别名，无法从房间名自动派生，保留人工映射。
     *
     * 仅在目标房间确实存在时才生效。
     */
    private val MANUAL: Map<String, String> = mapOf(
        "大厅" to "客厅",
        "客厅" to "客厅",
        "卧室" to "主卧",
        "我的卧室" to "主卧",
        "玄关" to "门厅",
        "门口" to "门厅",
        "小卧" to "次卧",
        "小卧室" to "次卧",
        "卫生间" to "厕所",
        "洗手间" to "厕所",
        "盥洗室" to "厕所",
    )

    /** 方位/序数前缀，去掉后往往就是用户口语里说的名字。 */
    private val PREFIXES = listOf(
        "北", "南", "东", "西", "大", "小", "新", "老", "前", "后", "上", "下", "中",
    )

    /** 常见后缀，口语中常被省略。 */
    private val SUFFIXES = listOf("房间", "卧室房", "室", "间")

    fun build(rooms: List<String>): Map<String, String> {
        val real = rooms.filter { it.isNotBlank() }.distinct()
        if (real.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, String>()
        val claimed = mutableMapOf<String, MutableSet<String>>()

        fun claim(alias: String, room: String) {
            if (alias.isBlank() || alias == room || alias.length < 2) return
            claimed.getOrPut(alias) { mutableSetOf() }.add(room)
        }

        real.forEach { room ->
            PREFIXES.forEach { prefix ->
                if (room.startsWith(prefix) && room.length > prefix.length + 1) {
                    claim(room.removePrefix(prefix), room)
                }
            }
            SUFFIXES.forEach { suffix ->
                if (room.endsWith(suffix) && room.length > suffix.length + 1) {
                    claim(room.removeSuffix(suffix), room)
                }
            }
        }

        // 人工别名优先，但只在目标房间真实存在时保留
        MANUAL.forEach { (alias, target) ->
            if (real.contains(target)) {
                result[alias] = target
                claimed.remove(alias)
            }
        }

        // 派生别名：只在无歧义且未被人工映射占用时采纳
        claimed.forEach { (alias, targets) ->
            if (targets.size == 1 && !result.containsKey(alias)) {
                result[alias] = targets.first()
            }
        }

        return result
    }
}
