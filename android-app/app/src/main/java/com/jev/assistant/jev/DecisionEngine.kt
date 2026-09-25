package com.jev.assistant.jev

import com.jev.assistant.alarm.AlarmScheduler
import com.jev.assistant.data.AlarmRecord
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.data.ExecutionPlan
import com.jev.assistant.data.IntentType
import com.jev.assistant.data.LocalStorage
import com.jev.assistant.data.RunEvent
import com.jev.assistant.data.RunStatus
import com.jev.assistant.data.SilentLog
import com.jev.assistant.device.DeviceGroupManager
import com.jev.assistant.device.DeviceRegistry
import com.jev.assistant.device.HomeExecutor
import com.jev.assistant.pc.PcAgentClient
import com.jev.assistant.utils.NumericExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.regex.Pattern

class DecisionEngine(
    private val jevClient: JevClient,
    private val storage: LocalStorage,
    private val registry: DeviceRegistry,
    private val homeExecutor: HomeExecutor,
    private val alarmScheduler: AlarmScheduler,
    private val pcClient: PcAgentClient
) {
    private val _activePlanFlow = MutableStateFlow<ExecutionPlan?>(null)
    val activePlanFlow: StateFlow<ExecutionPlan?> = _activePlanFlow.asStateFlow()

    private val _isDeciding = MutableStateFlow(false)
    val isDeciding: StateFlow<Boolean> = _isDeciding.asStateFlow()

    private var lastFinalText = ""
    private var lastFinalTime = 0L

    suspend fun processFinalUtterance(text: String) = withContext(Dispatchers.Default) {
        val trimmed = text.trim()
        if (trimmed.length < 2) return@withContext

        val now = System.currentTimeMillis()
        if (trimmed == lastFinalText && now - lastFinalTime < 3000L) {
            return@withContext
        }
        lastFinalText = trimmed
        lastFinalTime = now

        _isDeciding.value = true
        var plan: ExecutionPlan? = null
        try {
            val currentRoom = storage.getDefaultRoom()
            val isLive = storage.isLiveMode()
            val runId = UUID.randomUUID().toString().take(8)

            // 第一阶段评估：Jev 直连或本地智能规则
            val stageOneResult = jevClient.evaluateStageOne(trimmed, currentRoom)

            val engagement: String
            val intentStr: String
            val conf: Float
            var stageOneCost = 0.0

            if (stageOneResult.isSuccess) {
                val res = stageOneResult.getOrThrow()
                engagement = res.engagement
                intentStr = res.intent
                conf = res.confidence
                stageOneCost = res.rawResponse.getCostCny()
            } else {
                val fallback = localStageOneFallback(trimmed)
                engagement = fallback.first
                intentStr = fallback.second
                conf = fallback.third
            }

            val intentType = when (intentStr) {
                "home_control" -> IntentType.HOME_CONTROL
                "alarm" -> IntentType.ALARM
                "computer_control" -> IntentType.COMPUTER_CONTROL
                "daily_chat" -> IntentType.DAILY_CHAT
                else -> IntentType.OTHER
            }

            // 闲聊、自言自语静默忽略（记录实际发生的真实 Token 费用）
            if (engagement != "actionable" || intentType == IntentType.DAILY_CHAT || intentType == IntentType.OTHER) {
                val reason = when {
                    intentType == IntentType.DAILY_CHAT -> "日常聊天，已忽略"
                    engagement == "ignore" -> "检测到闲聊/电视杂音/否定句，已静默忽略"
                    engagement == "uncertain" -> "请求意图不明确，已忽略"
                    else -> "非控制指令，已忽略"
                }
                if (stageOneCost > 0.0) {
                    storage.recordJevCost(stageOneCost)
                }
                storage.addLog(
                    SilentLog(
                        id = runId,
                        rawText = trimmed,
                        intent = intentType,
                        confidence = conf,
                        reason = reason,
                        isIgnored = true,
                        cost = stageOneCost
                    )
                )
                return@withContext
            }

            val targetRoom = registry.resolveRoom(trimmed, currentRoom)
            val initialPlan = ExecutionPlan(
                runId = runId,
                utteranceText = trimmed,
                intent = intentType,
                confidence = conf,
                targetDeviceName = "正在定位目标...",
                targetDeviceId = "",
                action = "",
                room = targetRoom,
                status = RunStatus.DECIDING,
                statusMessage = "Jev 业务判定完成: 「${getIntentChinese(intentType)}」(置信度 ${(conf * 100).toInt()}%)",
                isLive = isLive,
                cost = stageOneCost
            )
            initialPlan.events.add(
                RunEvent(
                    stage = "意图理解",
                    message = "Jev 分类完成: ${getIntentChinese(intentType)} · 目标区域: $targetRoom"
                )
            )
            plan = initialPlan
            _activePlanFlow.value = initialPlan

            when (intentType) {
                IntentType.HOME_CONTROL -> handleHomeControl(trimmed, targetRoom, initialPlan, isLive)
                IntentType.ALARM -> handleAlarm(trimmed, initialPlan)
                IntentType.COMPUTER_CONTROL -> handleComputerControl(trimmed, initialPlan)
                else -> {}
            }
        } catch (t: Throwable) {
            android.util.Log.e("DecisionEngine", "决策流程捕获异常: ${t.message}", t)
            val p = plan ?: ExecutionPlan(
                runId = UUID.randomUUID().toString().take(8),
                utteranceText = trimmed,
                intent = IntentType.OTHER,
                confidence = 0f,
                targetDeviceName = "异常拦截",
                targetDeviceId = "",
                action = "error",
                room = "全屋",
                status = RunStatus.FAILED,
                statusMessage = "处理异常: ${t.message ?: "未知错误"}",
                isLive = false
            )
            p.status = RunStatus.FAILED
            p.statusMessage = "执行异常: ${t.message ?: "未知错误"}"
            p.events.add(RunEvent(stage = "异常保护", message = "已捕获并记录异常: ${t.message}", isSuccess = false))
            _activePlanFlow.value = p
            recordPlanLog(p)
        } finally {
            _isDeciding.value = false
        }
    }

    private suspend fun handleHomeControl(
        text: String,
        room: String,
        plan: ExecutionPlan,
        isLive: Boolean
    ) {
        // 1. 优先检查是否为设备组批量控制（包含普通群控与反向排除群控）
        val isGroup = DeviceGroupManager.isGroupCommand(text)
        if (isGroup) {
            val matchedGroup = DeviceGroupManager.matchGroup(
                text = text,
                groups = registry.groupsFlow.value,
                devices = registry.devicesFlow.value,
                roomAliases = registry.roomAliases
            )
            if (matchedGroup != null) {
                plan.targetDeviceId = matchedGroup.id
                plan.targetDeviceName = matchedGroup.name
                if (matchedGroup.scope.startsWith("全屋(除")) {
                    plan.room = matchedGroup.scope
                }

                var action = when {
                    text.contains("关") -> "set_power_off"
                    text.contains("开") -> "set_power_on"
                    text.contains("新风") -> "set_mode_fresh_air"
                    else -> "set_power_on"
                }
                val params = mutableMapOf<String, Any>(
                    "is_group" to true,
                    "device_ids" to matchedGroup.deviceIds
                )
                // 检查是否附带数值（如所有灯亮度80，空调26度）
                val nums = NumericExtractor.extractNumbers(text)
                if (nums.isNotEmpty() && text.contains("亮度")) {
                    params["percent"] = nums[0].toInt().coerceIn(1, 100)
                    action = "set_brightness"
                }
                if (nums.isNotEmpty() && (text.contains("度") || text.contains("温度"))) {
                    params["celsius"] = nums[0].toDouble().coerceIn(16.0, 32.0)
                    action = "set_temperature"
                }
                plan.action = action
                plan.parameters = params

                plan.events.add(
                    RunEvent(
                        stage = "编组匹配",
                        message = "识别到批量指令，匹配设备组: ${matchedGroup.name} (共 ${matchedGroup.deviceIds.size} 台)"
                    )
                )
                _activePlanFlow.value = plan
                val executed = homeExecutor.executeHomeControl(plan, isLive)
                _activePlanFlow.value = executed
                recordPlanLog(executed)
                return
            }
        }

        // 2. 单设备控制解析
        val candidates = registry.getCandidatesForUtterance(text, room)
        val stageTwoRes = jevClient.evaluateStageTwoHome(text, room, candidates)

        val targetDevId: String?
        val action: String?
        val params: Map<String, Any>

        if (stageTwoRes.isSuccess) {
            val res = stageTwoRes.getOrThrow()
            targetDevId = res.targetDeviceId
            action = res.action
            params = res.parameters.toMutableMap()
            plan.cost += res.rawResponse.getCostCny()
        } else {
            val fallback = localHomeStageTwoFallback(text, candidates)
            targetDevId = fallback.first
            action = fallback.second
            params = fallback.third
        }

        if (targetDevId == null || action == null) {
            plan.status = RunStatus.FAILED
            plan.statusMessage = "未能在区域「$room」中定位到匹配的设备或动作"
            plan.events.add(RunEvent(stage = "设备解析", message = plan.statusMessage, isSuccess = false))
            _activePlanFlow.value = plan
            recordPlanLog(plan)
            return
        }

        val device = registry.findDeviceById(targetDevId)
        val devName = device?.name ?: targetDevId
        plan.targetDeviceId = targetDevId
        plan.targetDeviceName = devName
        plan.action = action

        // 终极保障：从用户原话提取精确数字，绝不漏掉 30%、八十七、十五等数值
        val finalParams = params.toMutableMap()
        if (action == "set_brightness" && !finalParams.containsKey("percent")) {
            val num = NumericExtractor.extractFirstInt(text)
            if (num != null) {
                finalParams["percent"] = num.coerceIn(1, 100)
                finalParams["power"] = 1
            }
        }
        if (action == "set_temperature" && !finalParams.containsKey("celsius")) {
            val num = NumericExtractor.extractFirstDouble(text)
            if (num != null) {
                finalParams["celsius"] = num.coerceIn(16.0, 32.0)
                finalParams["power"] = 1
            }
        }
        plan.parameters = finalParams

        plan.events.add(
            RunEvent(
                stage = "目标定位",
                message = "定位设备: $devName ($targetDevId) · 动作: $action · 参数: $finalParams"
            )
        )
        _activePlanFlow.value = plan

        val executed = homeExecutor.executeHomeControl(plan, isLive)
        _activePlanFlow.value = executed
        recordPlanLog(executed)
    }

    private suspend fun handleAlarm(text: String, plan: ExecutionPlan) {
        plan.status = RunStatus.VALIDATING
        plan.targetDeviceName = "手机本地闹钟"

        val (hour, minute) = parseTimeFromText(text)
        val timeFormatted = String.format("%02d:%02d", hour, minute)
        val timestamp = AlarmScheduler.parseTimeToTodayOrTomorrow(hour, minute)

        val alarm = AlarmRecord(
            id = UUID.randomUUID().toString().take(6),
            timeFormatted = timeFormatted,
            timestamp = timestamp,
            label = "语音设定闹钟",
            isDaily = text.contains("每天") || text.contains("每日")
        )

        plan.events.add(RunEvent(stage = "时间解析", message = "解析时间为: $timeFormatted"))
        val success = alarmScheduler.scheduleAlarm(alarm)
        if (success) {
            plan.status = RunStatus.SUCCEEDED
            plan.statusMessage = "闹钟已成功调度到本地 AlarmManager: $timeFormatted"
            plan.events.add(RunEvent(stage = "本地调度", message = plan.statusMessage))
        } else {
            plan.status = RunStatus.FAILED
            plan.statusMessage = "未能获取精确闹钟权限，调度失败"
            plan.events.add(RunEvent(stage = "本地调度", message = plan.statusMessage, isSuccess = false))
        }

        _activePlanFlow.value = plan
        recordPlanLog(plan)
    }

    private suspend fun handleComputerControl(text: String, plan: ExecutionPlan) {
        plan.status = RunStatus.EXECUTING
        plan.targetDeviceName = "局域网 PC"

        val action: String
        val params = mutableMapOf<String, Any>()

        when {
            text.contains("静音") -> {
                action = "set_volume"
                params["percent"] = 0
            }
            text.contains("音量") -> {
                val nums = NumericExtractor.extractNumbers(text)
                val p = if (nums.isNotEmpty()) nums[0].toInt().coerceIn(0, 100) else 50
                action = "set_volume"
                params["percent"] = p
            }
            text.contains("暂停") -> {
                action = "pause_media"
            }
            else -> {
                action = "open_app"
                params["app_id"] = "browser"
            }
        }

        plan.action = action
        plan.parameters = params
        plan.events.add(RunEvent(stage = "指令构建", message = "准备向电脑代理发送: $action"))

        val (ok, msg) = pcClient.executeAction(action, params)
        if (ok) {
            plan.status = RunStatus.SUCCEEDED
            plan.statusMessage = msg
            plan.events.add(RunEvent(stage = "代理执行", message = msg))
        } else {
            plan.status = RunStatus.FAILED
            plan.statusMessage = msg
            plan.events.add(RunEvent(stage = "代理执行", message = msg, isSuccess = false))
        }

        _activePlanFlow.value = plan
        recordPlanLog(plan)
    }

    private fun recordPlanLog(plan: ExecutionPlan) {
        val cost = plan.cost
        storage.recordJevCost(cost)

        val timeFormatted = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        storage.addUtteranceHistory(
            com.jev.assistant.data.UtteranceHistoryItem(
                id = plan.runId,
                timestamp = System.currentTimeMillis(),
                timeFormatted = timeFormatted,
                text = plan.utteranceText,
                room = plan.room,
                statusText = if (plan.status == RunStatus.SUCCEEDED) "✓ 成功" else "✕ 失败",
                isSuccess = plan.status == RunStatus.SUCCEEDED,
                cost = cost
            )
        )

        storage.addLog(
            SilentLog(
                id = plan.runId,
                rawText = plan.utteranceText,
                intent = plan.intent,
                confidence = plan.confidence,
                reason = "执行结果: ${plan.statusMessage} (扣费: ¥${String.format("%.4f", cost)})",
                isIgnored = false,
                cost = cost
            )
        )

        // 需求 3 核心：若操作智能家居成功，调用小米 HyperOS 灵动岛 (焦点通知 + 悬浮胶囊)
        if (plan.intent == IntentType.HOME_CONTROL && plan.status == RunStatus.SUCCEEDED) {
            com.jev.assistant.ui.island.DynamicIslandManager.showIsland(
                context = com.jev.assistant.JevApp.instance,
                room = plan.room,
                deviceName = plan.targetDeviceName,
                actionDesc = plan.statusMessage.take(24),
                cost = cost
            )
        }
    }

    private fun parseTimeFromText(text: String): Pair<Int, Int> {
        val matcher = Pattern.compile("(\\d{1,2})[点时:](\\d{1,2})?").matcher(text)
        if (matcher.find()) {
            val h = matcher.group(1)?.toIntOrNull() ?: 8
            val m = matcher.group(2)?.toIntOrNull() ?: 0
            return h to m
        }
        val nums = NumericExtractor.extractNumbers(text)
        if (nums.isNotEmpty()) {
            val h = nums[0].toInt().coerceIn(0, 23)
            val m = if (nums.size > 1) nums[1].toInt().coerceIn(0, 59) else 0
            return h to m
        }
        return 8 to 0
    }

    private fun localStageOneFallback(text: String): Triple<String, String, Float> {
        val t = text.lowercase()
        return when {
            t.contains("灯") || t.contains("空调") || t.contains("风扇") || t.contains("窗帘") || t.contains("晾衣") || t.contains("插座") || t.contains("音箱") || t.contains("所有") || t.contains("全关") || t.contains("除了") || t.contains("除开") || t.contains("其他") || t.contains("其余") || t.contains("都关") || t.contains("都开") -> {
                Triple("actionable", "home_control", 0.98f)
            }
            t.contains("闹钟") || t.contains("提醒我") -> {
                Triple("actionable", "alarm", 0.96f)
            }
            t.contains("电脑") || t.contains("音量") -> {
                Triple("actionable", "computer_control", 0.92f)
            }
            t.contains("你好") || t.contains("今天天气") || t.contains("讲个笑话") || t.contains("你是谁") -> {
                Triple("ignore", "daily_chat", 0.98f)
            }
            else -> Triple("ignore", "other", 0.70f)
        }
    }

    /**
     * 关键修复：利用 NumericExtractor 精准提取“亮度 80”、“24度”等真实数值，绝不误填 70！
     */
    private fun localHomeStageTwoFallback(
        text: String,
        candidates: List<DeviceItem>
    ): Triple<String?, String?, Map<String, Any>> {
        val explicitRoom = registry.extractExplicitRoom(text)
        val pool = if (explicitRoom != null) candidates.filter { it.room == explicitRoom } else candidates

        val dev = pool.firstOrNull { d ->
            text.contains(d.name) || (d.shortName.isNotEmpty() && text.contains(d.shortName)) || text.contains(d.type)
        } ?: pool.firstOrNull()

        val nums = NumericExtractor.extractNumbers(text)
        val params = mutableMapOf<String, Any>()

        val action: String = when {
            text.contains("亮度") -> {
                val b = if (nums.isNotEmpty()) nums[0].toInt().coerceIn(1, 100) else 50
                params["percent"] = b
                params["power"] = 1
                "set_brightness"
            }
            (text.contains("度") || text.contains("温度")) && (text.contains("空调") || dev?.type?.contains("空调") == true) -> {
                val t = if (nums.isNotEmpty()) nums[0].coerceIn(16.0, 32.0) else 26.0
                params["celsius"] = t
                params["power"] = 1
                "set_temperature"
            }
            text.contains("关") -> {
                params["power"] = 0
                "set_power_off"
            }
            text.contains("开") -> {
                params["power"] = 1
                "set_power_on"
            }
            text.contains("拉开") || text.contains("打开窗帘") -> {
                params["position"] = 100
                "open"
            }
            text.contains("合上") || text.contains("关闭窗帘") -> {
                params["position"] = 0
                "close"
            }
            text.contains("停") -> "stop"
            else -> {
                params["power"] = 1
                "set_power_on"
            }
        }

        return Triple(dev?.logicalId, action, params)
    }

    private fun getIntentChinese(intent: IntentType): String = when (intent) {
        IntentType.HOME_CONTROL -> "智能家居控制"
        IntentType.ALARM -> "手机闹钟"
        IntentType.COMPUTER_CONTROL -> "电脑控制"
        IntentType.DAILY_CHAT -> "日常闲聊"
        IntentType.OTHER -> "其他"
    }
}
