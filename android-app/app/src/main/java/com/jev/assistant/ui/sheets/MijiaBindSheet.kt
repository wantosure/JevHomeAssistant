package com.jev.assistant.ui.sheets

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jev.assistant.miot.MiotHome
import com.jev.assistant.ui.theme.AmberAccent
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.DarkBackground
import com.jev.assistant.ui.theme.DarkSurface
import com.jev.assistant.ui.theme.DarkSurfaceBorder
import com.jev.assistant.ui.theme.EmeraldAccent
import com.jev.assistant.ui.theme.RedAccent
import com.jev.assistant.ui.theme.TextMuted
import com.jev.assistant.ui.theme.TextPrimary
import com.jev.assistant.ui.theme.TextSecondary

/**
 * 米家账号绑定。
 *
 * 授权回执需要用户手动复制粘贴——小米只注册了固定的回调展示页，
 * App 无法通过 intent-filter 拦截，这是协议约束而非实现偷懒。
 * 因此界面重点是把三步引导说清楚，并兼容用户可能粘贴的三种格式。
 */
@Composable
fun MijiaBindSheet(
    bound: Boolean,
    nickname: String?,
    expiresAtText: String?,
    nextRefreshText: String?,
    deviceSummary: String?,
    statusNote: String?,
    homes: List<MiotHome>,
    activeHomeId: String?,
    onBuildAuthUrl: () -> String,
    onSubmitPayload: (String) -> Unit,
    onResync: () -> Unit,
    onSelectHome: (String) -> Unit,
    onUnbind: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var payloadText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "🏠 米家账号",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "绑定后可直接读取你的真实设备与房间，并支持语音控制。" +
                "设备能力来自公开的米家设备规范，控制指令经小米云下发。",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        // ===== 绑定状态 =====
        StatusCard(
            bound = bound,
            nickname = nickname,
            expiresAtText = expiresAtText,
            nextRefreshText = nextRefreshText,
            deviceSummary = deviceSummary,
            statusNote = statusNote,
        )

        Spacer(Modifier.height(16.dp))

        if (!bound) {
            StepGuide(
                onOpenAuthPage = {
                    val url = onBuildAuthUrl()
                    val opened = runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }.isSuccess
                    if (!opened) {
                        Toast.makeText(context, "无法打开浏览器，请手动访问小米授权页", Toast.LENGTH_LONG).show()
                    }
                },
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = payloadText,
                onValueChange = { payloadText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("粘贴授权回执") },
                placeholder = { Text("支持整段文本、回调链接或授权码") },
                minLines = 3,
                maxLines = 6,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextSecondary,
                ),
            )
            Text(
                text = "回执中包含 code 与 state，两者缺一不可——state 用于确认这次授权确实由本机发起。",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    onSubmitPayload(payloadText)
                    payloadText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = payloadText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = DarkBackground,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("完成绑定", fontWeight = FontWeight.Bold)
            }
        } else {
            // ===== 已绑定：家庭选择与维护 =====
            if (homes.isNotEmpty()) {
                Text(
                    text = "当前家庭",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "「全屋」指的是当前选中的这个家庭，不会跨家庭执行。",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )

                homes.forEach { home ->
                    HomeRow(
                        home = home,
                        selected = home.homeId == activeHomeId,
                        onClick = { onSelectHome(home.homeId) },
                    )
                }

                Spacer(Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onResync,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = DarkBackground,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("重新同步设备")
                }
                OutlinedButton(
                    onClick = onUnbind,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("解除绑定")
                }
            }

            Text(
                text = "解除绑定会清除本机保存的授权，不影响你的小米账号本身。",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatusCard(
    bound: Boolean,
    nickname: String?,
    expiresAtText: String?,
    nextRefreshText: String?,
    deviceSummary: String?,
    statusNote: String?,
) {
    val accent = if (bound) EmeraldAccent else AmberAccent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            text = if (bound) "已绑定${nickname?.let { " · $it" } ?: ""}" else "尚未绑定小米账号",
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )

        if (bound) {
            expiresAtText?.let {
                Text("授权有效期至 $it", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            nextRefreshText?.let {
                Text(it, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
            deviceSummary?.let {
                Text(it, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            Text(
                text = "未绑定时没有可用设备，语音指令会被直接拒绝，不会产生任何控制。",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        statusNote?.let {
            Text(it, color = AmberAccent, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun StepGuide(onOpenAuthPage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text("绑定步骤", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        StepLine("1", "点下方按钮打开小米授权页，登录并同意授权")
        StepLine("2", "页面会显示一段可复制的回执文本，复制它")
        StepLine("3", "回到这里粘贴，点「完成绑定」")

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onOpenAuthPage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = DarkBackground,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("打开小米授权页", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepLine(index: String, text: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(index, color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun HomeRow(home: MiotHome, selected: Boolean, onClick: () -> Unit) {
    val accent = if (selected) CyanAccent else DarkSurfaceBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                if (selected) CyanAccent.copy(alpha = 0.1f) else DarkSurface,
                RoundedCornerShape(10.dp),
            )
            .border(1.dp, accent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = home.name,
                color = if (selected) CyanAccent else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${home.deviceCount} 台设备 · ${home.onlineCount} 在线 · ${home.roomCount} 个房间",
                color = TextMuted,
                fontSize = 11.sp,
            )
        }
        if (selected) {
            Text("当前", color = CyanAccent, fontSize = 11.sp)
        }
    }
}

/** 供界面显示的时间格式化。 */
internal fun formatEpochSeconds(epochSeconds: Long): String {
    if (epochSeconds <= 0) return "—"
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(epochSeconds * 1000))
}
