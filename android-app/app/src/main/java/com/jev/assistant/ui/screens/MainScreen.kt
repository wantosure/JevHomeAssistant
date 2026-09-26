package com.jev.assistant.ui.screens

import android.widget.Toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jev.assistant.data.RunStatus
import com.jev.assistant.ui.components.ExecutionHeroCard
import com.jev.assistant.ui.components.GlowingAuraVisualizer
import com.jev.assistant.ui.components.HistorySessionFoldView
import com.jev.assistant.ui.components.SilentLogStream
import com.jev.assistant.ui.components.SimulatedDeviceCard
import com.jev.assistant.ui.components.SubtitleFlowView
import com.jev.assistant.ui.components.TopControlBar
import com.jev.assistant.ui.sheets.AlarmSheet
import com.jev.assistant.ui.sheets.MijiaBindSheet
import com.jev.assistant.ui.sheets.SettingsSheet
import com.jev.assistant.ui.sheets.formatEpochSeconds
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.DarkBackground
import com.jev.assistant.ui.theme.DarkSurface
import com.jev.assistant.ui.theme.DarkSurfaceBorder
import com.jev.assistant.ui.theme.DarkSurfaceVariant
import com.jev.assistant.ui.theme.EmeraldAccent
import com.jev.assistant.ui.theme.TextMuted
import com.jev.assistant.ui.theme.TextPrimary
import com.jev.assistant.ui.theme.TextSecondary
import com.jev.assistant.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

enum class SheetType {
    NONE,
    ALARMS,
    SETTINGS,
    ROOMS,
    MIJIA_BIND
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AssistantViewModel) {
    val isListening by viewModel.isListening.collectAsState()
    val currentRms by viewModel.currentRms.collectAsState()
    val subtitle by viewModel.streamingSubtitle.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val isDeciding by viewModel.isDeciding.collectAsState()
    val silentLogs by viewModel.silentLogs.collectAsState()
    val defaultRoom by viewModel.defaultRoom.collectAsState()
    val isLiveMode by viewModel.isLiveMode.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val crashLog by viewModel.crashLogFlow.collectAsState()
    val asrEngineName by viewModel.asrEngineName.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val totalJevCost by viewModel.totalJevCost.collectAsState()
    val totalJevCalls by viewModel.totalJevCalls.collectAsState()
    // 房间列表来自真实同步结果，不再硬编码
    val rooms by viewModel.rooms.collectAsState()
    val isMijiaBound = viewModel.isMijiaBound
    val context = androidx.compose.ui.platform.LocalContext.current

    var activeDialogSheet by remember { mutableStateOf(SheetType.NONE) }
    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 房间筛选
    var selectedRoomTab by remember { mutableStateOf("全部") }
    val roomTabs = remember(devices) {
        listOf("全部") + devices.map { it.room }.distinct()
    }
    val filteredDevices = remember(selectedRoomTab, devices) {
        if (selectedRoomTab == "全部") devices else devices.filter { it.room == selectedRoomTab }
    }

    val activeCount = remember(devices) {
        devices.count {
            val p = it.currentState["power"]
            p == 1 || p == "1" || p == true || p == 1.0
        }
    }

    // 手势上划底部抽屉
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val isExpanded = bottomSheetState.currentValue == SheetValue.Expanded
    var isFullScreenDevices by remember { mutableStateOf(false) }

    // 打开全屋看板或全屏时，强制持续开启麦克风和 ASR 录音 (需求 3：全屏显示设备列表时也要持续录音)
    androidx.compose.runtime.LaunchedEffect(isExpanded, isFullScreenDevices) {
        if (!isListening) {
            viewModel.audioPipeline.startListening()
        }
    }

    if (isFullScreenDevices) {
        // ===== 需求 3 核心：全屏显示设备列表，且麦克风与 ASR 持续录音监听 =====
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. 顶部操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { isFullScreenDevices = false }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回主页", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "返回大屏", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🏠 米家全屋设备大看板 (${filteredDevices.size}台)",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldAccent.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "$activeCount 台开启", color = EmeraldAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 持续录音与 ASR 字幕条 (核心需求 3：全屏查看时持续录音并展示识别与调用过程)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, if (isListening) EmeraldAccent.copy(alpha = 0.6f) else DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isListening) EmeraldAccent.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isListening) EmeraldAccent else TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (subtitle.isNotBlank()) "语音字幕: \"$subtitle\"" else "🎙️ 全屏持续监听中 · 可直接开口说如「关闭主卧所有灯」",
                        color = if (subtitle.isNotBlank()) CyanAccent else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                if (activePlan != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExecutionHeroCard(plan = activePlan)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 房间筛选滑轨
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roomTabs.forEach { r ->
                    val isSelected = r == selectedRoomTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) CyanAccent else DarkSurfaceVariant)
                            .clickable { selectedRoomTab = r }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = r,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 4. 设备列表。未绑定时必须给出解释，否则只是一个空白看板
            if (devices.isEmpty()) {
                DeviceListEmptyState(
                    bound = isMijiaBound,
                    onOpenBind = { activeDialogSheet = SheetType.MIJIA_BIND },
                    onResync = { viewModel.syncMijiaDevices() },
                    modifier = Modifier.weight(1f),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDevices, key = { it.logicalId }) { dev ->
                    SimulatedDeviceCard(
                        device = dev,
                        onToggle = { id, currentPower ->
                            viewModel.toggleDevicePower(id, currentPower)
                        }
                    )
                }
            }
        }
    } else {
        BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 84.dp,
        sheetContainerColor = DarkSurface,
        sheetContentColor = TextPrimary,
        sheetDragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            if (bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                bottomSheetState.expand()
                            } else {
                                bottomSheetState.partialExpand()
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF334155))
                )
            }
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                // 1. 顶部标题栏与收起/展开按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                if (bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                    bottomSheetState.expand()
                                } else {
                                    bottomSheetState.partialExpand()
                                }
                            }
                        }
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🏠 米家全屋设备大看板",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldAccent.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$activeCount 台已开启",
                                color = EmeraldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanAccent.copy(alpha = 0.2f))
                                .clickable { isFullScreenDevices = true }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Fullscreen, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "全屏大看板", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    if (bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                        bottomSheetState.expand()
                                    } else {
                                        bottomSheetState.partialExpand()
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = if (isExpanded) "下划收起" else "展开",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. 关键需求 6：在全屋设备看板展开时，顶部区域也要显示 ASR 识别结果和调用过程！
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) EmeraldAccent.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isListening) EmeraldAccent else TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (subtitle.isNotBlank()) "语音字幕: \"$subtitle\"" else "正在静默监听... (可直接开口说话)",
                                color = if (subtitle.isNotBlank()) CyanAccent else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        // 如果有活跃执行卡片，在看板顶部也呈现调用流水线！
                        if (activePlan != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExecutionHeroCard(plan = activePlan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. 房间筛选横向滑轨
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roomTabs.forEach { r ->
                        val isSelected = r == selectedRoomTab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) CyanAccent else DarkSurfaceVariant)
                                .clickable { selectedRoomTab = r }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = r,
                                color = if (isSelected) DarkBackground else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 4. 设备列表（实时动态变化展示）
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isExpanded) 420.dp else 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDevices, key = { it.logicalId }) { dev ->
                        SimulatedDeviceCard(
                            device = dev,
                            onToggle = { id, currentPower ->
                                viewModel.toggleDevicePower(id, currentPower)
                            }
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        // 背景主视觉：极光声波大屏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 顶部状态与控制栏
                TopControlBar(
                    isListening = isListening,
                    defaultRoom = defaultRoom,
                    isLiveMode = isLiveMode,
                    onToggleListening = { viewModel.toggleListening() },
                    onRoomClick = { activeDialogSheet = SheetType.ROOMS },
                    onToggleLiveMode = { viewModel.toggleLiveMode() },
                    onOpenDevices = {
                        isFullScreenDevices = true
                    },
                    onOpenAlarms = { activeDialogSheet = SheetType.ALARMS },
                    onOpenSettings = { activeDialogSheet = SheetType.SETTINGS }
                )

                // 2. 崩溃日志醒目横幅（如果上次发生过崩溃/闪退）
                if (crashLog != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    com.jev.assistant.ui.components.CrashReportBanner(
                        crashLog = crashLog,
                        onCopy = { viewModel.copyCrashLog(context) },
                        onClear = { viewModel.clearCrashLog() }
                    )
                }

                // 3. ASR 引擎指示与快捷切换条 + 累计费用 (需求 4)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val switchNative = asrEngineName.contains("系统")
                            viewModel.switchAsrEngine(switchNative)
                        }
                    ) {
                        Text(
                            text = "🎙️ $asrEngineName",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "「切换」",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "•", color = DarkSurfaceBorder, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "💰 累计: ¥${String.format(java.util.Locale.CHINA, "%.4f", totalJevCost)} ($totalJevCalls 次)",
                        color = EmeraldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // 2. 核心声波能量球与粒子流光光环
                GlowingAuraVisualizer(
                    isListening = isListening,
                    isDeciding = isDeciding,
                    rms = currentRms,
                    isSuccess = activePlan?.status == RunStatus.SUCCEEDED
                )

                // 3. 动态流式字幕
                SubtitleFlowView(subtitle = subtitle)

                Spacer(modifier = Modifier.height(4.dp))

                // 4. 需求 2：识别后的文字以灰色小字折叠展示历史会话
                HistorySessionFoldView(
                    historyList = historyList,
                    onClearHistory = { viewModel.clearHistory() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 5. 执行卡片
                ExecutionHeroCard(
                    plan = activePlan,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.weight(0.15f))

                // 5. 快速测试滑轨（包含设备编组和数值调节）
                Text(
                    text = "快速指令测试（支持设备编组与数值解析）",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPrompts = listOf(
                        "主卧灯亮度80",
                        "把主卧主灯打开",
                        "关闭所有灯",
                        "关主卧所有灯",
                        "打开所有空调",
                        "把客厅空调调到24度",
                        "设置明天早上七点半的闹钟",
                        "今天天气真好 (闲聊忽略)"
                    )
                    quickPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkSurface)
                                .clickable { viewModel.simulateUtterance(prompt) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = prompt,
                                color = if (prompt.contains("闲聊")) TextMuted else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. 底部灰色静默决策日志
                SilentLogStream(logs = silentLogs)

                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // 弹窗抽屉（闹钟、设置、房间）
        if (activeDialogSheet != SheetType.NONE) {
            ModalBottomSheet(
                onDismissRequest = { activeDialogSheet = SheetType.NONE },
                sheetState = modalSheetState,
                containerColor = DarkSurface
            ) {
                when (activeDialogSheet) {
                    SheetType.ALARMS -> AlarmSheet(
                        alarms = alarms,
                        onDeleteAlarm = { viewModel.removeAlarm(it) }
                    )
                    SheetType.SETTINGS -> SettingsSheet(
                        isUsingCustomKey = viewModel.isUsingCustomKey(),
                        maskedKeyDisplay = viewModel.getMaskedApiKeyForDisplay(),
                        onSaveApiKey = { viewModel.saveApiKey(it) },
                        onResetToBuiltinKey = { viewModel.resetToBuiltinKey() },
                        currentEngineName = asrEngineName,
                        onSwitchEngine = { viewModel.switchAsrEngine(it) },
                        crashLog = crashLog,
                        onCopyCrashLog = { viewModel.copyCrashLog(context) },
                        onClearCrashLog = { viewModel.clearCrashLog() },
                        onCopyDiagnosticReport = { viewModel.copyDiagnosticReport(context) },
                        totalCost = totalJevCost,
                        totalCalls = totalJevCalls,
                        onResetCost = { viewModel.resetJevCost() },
                        mijiaBound = isMijiaBound,
                        mijiaSummary = if (isMijiaBound) {
                            "${devices.size} 台设备 · ${devices.count { it.isOnline }} 在线"
                        } else {
                            ""
                        },
                        onOpenMijia = { activeDialogSheet = SheetType.MIJIA_BIND }
                    )
                    SheetType.MIJIA_BIND -> {
                        val bindStatus by viewModel.miotBindStatus.collectAsState()
                        val homes by viewModel.homes.collectAsState()
                        val activeHome by viewModel.activeHomeId.collectAsState()
                        val adapterState by viewModel.adapterStatus.collectAsState()
                        val bound = bindStatus is com.jev.assistant.miot.auth.MiotBindStatus.Bound

                        MijiaBindSheet(
                            bound = bound,
                            nickname = (bindStatus as? com.jev.assistant.miot.auth.MiotBindStatus.Bound)?.nickname,
                            expiresAtText = (bindStatus as? com.jev.assistant.miot.auth.MiotBindStatus.Bound)
                                ?.expiresAtSec?.let { formatEpochSeconds(it) },
                            nextRefreshText = (bindStatus as? com.jev.assistant.miot.auth.MiotBindStatus.Bound)
                                ?.nextRefreshAtSec?.let { "将于 ${formatEpochSeconds(it)} 自动续期" },
                            deviceSummary = "${devices.size} 台设备 · ${devices.count { it.isOnline }} 在线",
                            statusNote = when (val s = adapterState) {
                                is com.jev.assistant.device.AdapterStatus.Failed -> s.message
                                is com.jev.assistant.device.AdapterStatus.AuthExpired -> s.message
                                else -> null
                            },
                            homes = homes,
                            activeHomeId = activeHome,
                            onBuildAuthUrl = { viewModel.buildMijiaAuthUrl() },
                            onSubmitPayload = { payload ->
                                viewModel.bindMijia(payload) { ok, message ->
                                    Toast.makeText(
                                        context,
                                        if (ok) "绑定成功，已切回观察模式" else (message ?: "绑定失败"),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            onResync = { viewModel.syncMijiaDevices() },
                            onSelectHome = { viewModel.setActiveHome(it) },
                            onUnbind = { viewModel.unbindMijia() },
                        )
                    }
                    SheetType.ROOMS -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "切换默认区域倾向", color = CyanAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "选择「全屋」则无任何房间倾向；即使选了客厅，也能自由控制主卧的灯。",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )
                            // 房间列表来自真实同步结果，不再硬编码
                            rooms.forEach { room ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setDefaultRoom(room)
                                            scope.launch { modalSheetState.hide() }.invokeOnCompletion {
                                                activeDialogSheet = SheetType.NONE
                                            }
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "${if (room == defaultRoom) "✓ " else "  "}$room",
                                        color = if (room == defaultRoom) CyanAccent else TextSecondary,
                                        fontSize = 15.sp,
                                        fontWeight = if (room == defaultRoom) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
}

/**
 * 设备列表的空状态。
 *
 * 接入真实米家后设备不再来自随包资源，未绑定时列表就是空的。
 * 空白看板会让人以为功能坏了，必须明确说明原因并给出下一步。
 */
@Composable
private fun DeviceListEmptyState(
    bound: Boolean,
    onOpenBind: () -> Unit,
    onResync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (bound) "📭" else "🏠",
            fontSize = 40.sp,
        )
        Text(
            text = if (bound) "暂无设备" else "尚未绑定米家账号",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = if (bound) {
                "当前家庭下没有设备，可能是同步失败或该家庭确实为空。"
            } else {
                "绑定后即可读取你真实的米家设备与房间，并按房间进行语音控制。"
            },
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        Button(
            onClick = if (bound) onResync else onOpenBind,
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = DarkBackground,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(if (bound) "重新同步设备" else "去绑定米家账号", fontWeight = FontWeight.Bold)
        }
    }
}
