package com.jev.assistant.audio

import android.util.Log
import com.jev.assistant.device.DeviceRegistry

/**
 * 智能家居 ASR 关键词库与热词偏置算法引擎
 * 分层体系：
 * 1. [所有人都需要的通用智能家居热词库 (Base Hotwords)]
 * 2. [根据用户自身 87 台设备拓扑动态自适应生成的专属热词库 (Custom Device Hotwords)]
 * 3. [智能家居高频同音/近音错字纠偏器 (Homophone Post-Processor)]
 */
object HotwordsManager {

    private const val TAG = "HotwordsManager"

    // =========================================================================
    // 1. 所有人都需要的智能家居基础通用热词库 (Base Hotwords)
    // =========================================================================
    val BASE_HOTWORDS: List<String> = listOf(
        // (1) 常见操作动词
        "打开", "关闭", "关掉", "开启", "调到", "调节", "调大", "调小", "调高", "调低",
        "升高", "降低", "增加", "减少", "设置", "切换", "启动", "停止", "暂停", "恢复",
        "亮一点", "暗一点", "热一点", "冷一点", "大一点", "小一点", "快一点", "慢一点",

        // (2) 常见属性、状态与量纲
        "亮度", "色温", "温度", "风速", "风向", "湿度", "模式", "定时", "倒计时",
        "百分之", "度", "摄氏度", "档", "档位", "暖光", "白光", "中性光", "自然光",
        "制冷", "制热", "送风", "除湿", "自动", "睡眠", "强力", "节能", "静音", "离家", "回家",

        // (3) 常见通用房间区域
        "全屋", "家里", "主卧", "次卧", "儿童房", "老人房", "客房", "书房",
        "客厅", "餐厅", "厨房", "卫生间", "主卫", "客卫", "阳台", "玄关",
        "走廊", "过道", "衣帽间", "储藏室", "地下室", "车库", "飘窗", "吧台",

        // (4) 常见基础设备品类名词
        "所有灯", "全部灯", "主灯", "顶灯", "吸顶灯", "筒灯", "射灯", "灯带", "氛围灯",
        "落地灯", "台灯", "床头灯", "夜灯", "吊灯", "磁吸轨道灯", "灯光", "辅灯",
        "空调", "新风", "新风机", "风扇", "循环扇", "空气净化器", "除湿机", "加湿器",
        "扫地机", "扫地机器人", "扫拖一体机", "吸尘器", "洗衣机", "烘干机",
        "窗帘", "电动窗帘", "开合帘", "百叶帘", "罗马帘", "纱帘", "卷帘",
        "插座", "智能插座", "排插", "开关", "门锁", "智能门锁", "摄像头", "门铃",
        "浴霸", "凉霸", "排气扇", "热水器", "燃气热水器", "电热水器", "电视", "音箱",

        // (5) 中文自然数字与数值量纲
        "一", "二", "两", "三", "四", "五", "六", "七", "八", "九", "十",
        "十六", "十八", "二十", "二十一", "二十二", "二十三", "二十四", "二十五", "二十六",
        "二十七", "二十八", "三十", "三十五", "四十", "五十", "六十", "七十", "八十", "九十", "一百"
    )

    // =========================================================================
    // 2. 常见同音/近音错字纠偏字典 (Homophone Replacer)
    // 专门解决微弱发音、口语吞音导致的生僻错字
    // =========================================================================
    private val HOMOPHONE_CORRECTIONS = mapOf(
        // 设备名词同音纠错
        "洗顶灯" to "吸顶灯",
        "西顶灯" to "吸顶灯",
        "吸定灯" to "吸顶灯",
        "洗定灯" to "吸顶灯",
        "细顶灯" to "吸顶灯",
        "星风" to "新风",
        "新峰" to "新风",
        "星峰" to "新风",
        "心风" to "新风",
        "亮霸" to "凉霸",
        "良霸" to "凉霸",
        "排气善" to "排气扇",
        "开合连" to "开合帘",
        "沙帘" to "纱帘",

        // 房间同音纠错
        "猪卧" to "主卧",
        "著卧" to "主卧",
        "竹卧" to "主卧",
        "主握" to "主卧",
        "刺卧" to "次卧",
        "赐卧" to "次卧",
        "客微" to "客卫",
        "克味" to "客卫",
        "克卫" to "客卫",
        "客味间" to "客卫",
        "主微" to "主卫",
        "悬关" to "玄关",
        "全屋灯" to "全屋灯",

        // 量纲与动作同音纠错
        "两度" to "亮度",
        "凉度" to "亮度",
        "量度" to "亮度",
        "良度" to "亮度",
        "摄氏渡" to "摄氏度",
        "摄氏独" to "摄氏度",
        "百分质" to "百分之",
        "百份之" to "百分之",
        "百份" to "百分",
        "关悼" to "关掉",
        "开起" to "开启",
        "调两" to "调亮",
        "关辟" to "关闭",
        "二拾" to "二十",
        "三拾" to "三十",
        "八拾" to "八十"
    )

    /**
     * 算法：动态构建多层级合并热词字符串
     * @param deviceRegistry 设备注册中心（包含当前家庭实时的 87 台设备与空间）
     * @return 格式化为 Sherpa-ONNX 标准的热词字符串（以 '/' 隔开，且长词优先排序）
     */
    fun buildHotwords(deviceRegistry: DeviceRegistry?): String {
        val resultTokens = LinkedHashSet<String>()

        // 1. 注入所有人都需要的通用智能家居基础热词
        resultTokens.addAll(BASE_HOTWORDS)

        // 2. 动态注入根据用户自身拥有的 87 台设备生成的专属热词
        if (deviceRegistry != null) {
            val devices = deviceRegistry.devicesFlow.value
            for (dev in devices) {
                // (a) 设备完整名称（例如 "主卧顶灯"、"次卧风扇灯"、"阳台烘干机"）
                val rawName = dev.name.trim()
                if (rawName.isNotBlank()) {
                    resultTokens.add(rawName)

                    // 词根拆解提炼：例如 "主卧顶灯" -> 提取 "顶灯"；"客厅悬浮灯带" -> 提取 "灯带"
                    if (rawName.length >= 4) {
                        resultTokens.add(rawName.takeLast(2))
                        resultTokens.add(rawName.takeLast(3))
                    }
                }

                // (b) 设备归属房间名（例如 "主卧"、"客卫"）
                val room = dev.room.trim()
                if (room.isNotBlank() && room != "全部") {
                    resultTokens.add(room)
                    // 组合词：例如 "主卧灯"、"客厅空调"
                    if (rawName.contains("灯")) resultTokens.add("${room}灯")
                    if (rawName.contains("空调")) resultTokens.add("${room}空调")
                    if (rawName.contains("窗帘")) resultTokens.add("${room}窗帘")
                }
            }

            // (c) 房间列表全量注入
            val rooms = deviceRegistry.roomsFlow.value
            for (r in rooms) {
                val cleanRoom = r.trim()
                if (cleanRoom.isNotBlank() && cleanRoom != "全部") {
                    resultTokens.add(cleanRoom)
                    resultTokens.add("${cleanRoom}所有灯")
                }
            }
        }

        // 3. 排序策略：长词优先（降序）。
        // 在前缀树/声学模型热词匹配中，优先长词能防止专有设备名被简单单字切碎。
        val sortedList = resultTokens.filter { it.length >= 2 }.sortedByDescending { it.length }

        val hotwordsFormatted = sortedList.joinToString("/")
        Log.i(TAG, "已成功装配热词库：通用基础词 ${BASE_HOTWORDS.size} 个，全屋设备自定义词 ${sortedList.size - BASE_HOTWORDS.size} 个，总计 ${sortedList.size} 词")
        return hotwordsFormatted
    }

    /**
     * 算法：对 ASR 解码出的最终文本进行同音字修正与智能家居术语规整
     * 消除因发音含糊导致的“洗顶灯”、“星风”、“猪卧”等问题
     */
    fun correctHomophones(rawText: String): String {
        if (rawText.isBlank()) return rawText
        var processed = rawText
        for ((wrong, right) in HOMOPHONE_CORRECTIONS) {
            if (processed.contains(wrong)) {
                processed = processed.replace(wrong, right)
            }
        }
        return processed
    }
}
