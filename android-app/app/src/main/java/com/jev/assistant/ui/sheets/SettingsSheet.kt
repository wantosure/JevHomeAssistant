package com.jev.assistant.ui.sheets

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jev.assistant.ui.theme.AmberAccent
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.DarkBackground
import com.jev.assistant.ui.theme.DarkSurface
import com.jev.assistant.ui.theme.DarkSurfaceBorder
import com.jev.assistant.ui.theme.DarkSurfaceVariant
import com.jev.assistant.ui.theme.EmeraldAccent
import com.jev.assistant.ui.theme.RedAccent
import com.jev.assistant.ui.theme.TextMuted
import com.jev.assistant.ui.theme.TextPrimary
import com.jev.assistant.ui.theme.TextSecondary

@Composable
fun SettingsSheet(
    isUsingCustomKey: Boolean,
    maskedKeyDisplay: String,
    onSaveApiKey: (String) -> Unit,
    onResetToBuiltinKey: () -> Unit,
    currentEngineName: String,
    onSwitchEngine: (Boolean) -> Unit,
    crashLog: String?,
    onCopyCrashLog: () -> Boolean,
    onClearCrashLog: () -> Unit,
    onCopyDiagnosticReport: () -> Boolean,
    totalCost: Double = 0.0,
    totalCalls: Int = 0,
    onResetCost: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var apiKeyText by remember { mutableStateOf("") }
    var saveSuccess by remember { mutableStateOf(false) }
    var showCrashDetails by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "⚙️ 系统与模型设置",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "客户端直连模式：App 直接发起 HTTPS 请求至 Jev (api.typesafe.ai)。无中间云服务器。",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        // ===== 1. ASR 语音识别引擎切换 =====
        Text(
            text = "🎙️ 语音识别引擎 (ASR)",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isSenseVoiceActive = currentEngineName.contains("SenseVoice")

            // 选项 A：阿里 SenseVoice 端侧离线 ASR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSenseVoiceActive) CyanAccent.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onSwitchEngine(true) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSenseVoiceActive) Icons.Default.CheckCircle else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isSenseVoiceActive) CyanAccent else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "阿里 SenseVoice 端侧离线 ASR (推荐)",
                        color = if (isSenseVoiceActive) CyanAccent else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSenseVoiceActive) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "内置神经网络 · 适配极微弱轻声细语 · 零网络延迟与零泄露",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // 选项 B：系统级高敏 ASR
            val isSystemActive = !isSenseVoiceActive
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSystemActive) EmeraldAccent.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onSwitchEngine(false) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSystemActive) Icons.Default.CheckCircle else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isSystemActive) EmeraldAccent else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "系统原生高敏 ASR (轻量备用)",
                        color = if (isSystemActive) EmeraldAccent else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSystemActive) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "直接使用安卓系统语音服务 · 零 Native 依赖 · 兼容性极强",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 2. Jev 调用与费用统计 (需求 4) =====
        Text(
            text = "💰 Jev 调用与费用统计",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "累计消费: ¥${String.format(java.util.Locale.CHINA, "%.4f", totalCost)}",
                        color = AmberAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val avgCost = if (totalCalls > 0) totalCost / totalCalls else 0.0
                    Text(
                        text = "累计调用: $totalCalls 次 · 均费: ¥${String.format(java.util.Locale.CHINA, "%.4f", avgCost)} (纯Token精算)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                OutlinedButton(
                    onClick = onResetCost,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("清零", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 3. 崩溃日志与运行诊断 (需求 2 核心入口) =====
        Text(
            text = "📋 崩溃排查与运行诊断报告",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (crashLog != null) Color(0xFF2E1717) else DarkSurfaceVariant)
                .border(
                    1.dp,
                    if (crashLog != null) RedAccent.copy(alpha = 0.8f) else DarkSurfaceBorder,
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (crashLog != null) Icons.Default.Warning else Icons.Default.BugReport,
                        contentDescription = null,
                        tint = if (crashLog != null) RedAccent else CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (crashLog != null) "已抓取到上次异常退出日志" else "当前运行正常 (无崩溃日志)",
                        color = if (crashLog != null) RedAccent else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (crashLog != null) {
                    OutlinedButton(
                        onClick = onClearCrashLog,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "清除", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 复制操作按钮栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (crashLog != null) {
                    Button(
                        onClick = {
                            val ok = onCopyCrashLog()
                            if (ok) {
                                Toast.makeText(context, "✅ 崩溃日志已复制到剪贴板！", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "复制崩溃日志", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showCrashDetails = !showCrashDetails },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(text = if (showCrashDetails) "收起" else "查看", fontSize = 12.sp, color = TextPrimary)
                    }
                } else {
                    Button(
                        onClick = {
                            val ok = onCopyDiagnosticReport()
                            if (ok) {
                                Toast.makeText(context, "✅ 系统运行诊断报告已复制到剪贴板！", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "一键复制完整系统与 ASR 诊断报告", fontSize = 12.sp, color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 展开查看堆栈
            if (showCrashDetails && crashLog != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = crashLog,
                        color = Color(0xFFFF9999),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 3. Jev API Key 安全配置 =====
        Text(
            text = "🔑 Jev API Key 安全配置",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        // 当前 Key 状态安全标识卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isUsingCustomKey) CyanAccent.copy(alpha = 0.15f) else EmeraldAccent.copy(alpha = 0.15f))
                .border(1.dp, if (isUsingCustomKey) CyanAccent.copy(alpha = 0.4f) else EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = if (isUsingCustomKey) "当前状态：已使用自定义 Key" else "当前状态：已启用内置开发者 Key (开箱即用)",
                    color = if (isUsingCustomKey) CyanAccent else EmeraldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = maskedKeyDisplay,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apiKeyText,
            onValueChange = {
                apiKeyText = it
                saveSuccess = false
            },
            placeholder = { Text("在此输入您的 Jev API Key 进行替换", color = TextMuted, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isUsingCustomKey) {
                OutlinedButton(
                    onClick = {
                        onResetToBuiltinKey()
                        apiKeyText = ""
                        saveSuccess = false
                        Toast.makeText(context, "已恢复为系统内置加密 Key", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("恢复内置 Key", fontSize = 12.sp, color = AmberAccent)
                }
            }

            Button(
                onClick = {
                    if (apiKeyText.isNotBlank()) {
                        onSaveApiKey(apiKeyText)
                        saveSuccess = true
                        apiKeyText = ""
                        Toast.makeText(context, "已成功保存并切换为您的自定义 Key", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "请输入有效的 API Key", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (saveSuccess) "✓ 已保存生效" else "保存替换 Key",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "运行建议：\n1. 请在系统设置中为本应用开启「无限制电池用量」，以保证熄屏 24 小时持续监听。\n2. 若处于 Observe 观察模式，所有指令均仅生成决策与验证日志，不向硬件下发。\n3. Live 真实模式需要配合局域网已部署的网关/代理。",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}
