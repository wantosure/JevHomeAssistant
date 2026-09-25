package com.jev.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.ui.theme.AmberAccent
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.DarkSurface
import com.jev.assistant.ui.theme.DarkSurfaceBorder
import com.jev.assistant.ui.theme.DarkSurfaceVariant
import com.jev.assistant.ui.theme.EmeraldAccent
import com.jev.assistant.ui.theme.TextMuted
import com.jev.assistant.ui.theme.TextPrimary
import com.jev.assistant.ui.theme.TextSecondary

@Composable
fun SimulatedDeviceCard(
    device: DeviceItem,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val powerVal = device.currentState["power"]
    val isOn = powerVal == 1 || powerVal == "1" || powerVal == true || powerVal == 1.0

    // 状态变化的平滑色彩过渡动效
    val cardBorderColor by animateColorAsState(
        targetValue = if (isOn) EmeraldAccent.copy(alpha = 0.8f) else DarkSurfaceBorder,
        animationSpec = tween(350),
        label = "BorderAnim"
    )

    val iconBgColor by animateColorAsState(
        targetValue = if (isOn) EmeraldAccent.copy(alpha = 0.25f) else Color(0xFF1E293B),
        animationSpec = tween(350),
        label = "IconBgAnim"
    )

    // 变化时的轻微呼吸弹簧动效
    val scaleAnim by animateFloatAsState(
        targetValue = if (isOn) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ScaleAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isOn) {
                    Brush.verticalGradient(
                        listOf(DarkSurfaceVariant, Color(0xFF0F2218))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(DarkSurfaceVariant, DarkSurface)
                    )
                }
            )
            .border(1.2.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .clickable {
                if (device.capabilities.contains("power")) {
                    onToggle(device.logicalId, isOn)
                }
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：设备图标与详细状态
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = device.icon, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanAccent.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📍 ${device.room}",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // 动态状态说明字符串
                    val statusText = buildString {
                        append(if (isOn) "● 已开启" else "○ 已关闭")
                        device.currentState["brightness"]?.let { append(" · 亮度 $it%") }
                        device.currentState["target_temperature"]?.let { append(" · $it℃") }
                        device.currentState["mode"]?.let {
                            val modeStr = when (it.toString()) {
                                "cool" -> "制冷"
                                "heat" -> "制热"
                                "auto" -> "自动"
                                else -> it.toString()
                            }
                            append(" · $modeStr")
                        }
                        device.currentState["position"]?.let { append(" · 开合 $it%") }
                    }

                    Text(
                        text = statusText,
                        color = if (isOn) EmeraldAccent else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isOn) FontWeight.Medium else FontWeight.Normal
                    )

                    Text(
                        text = "ID: ${device.logicalId} · ${device.type}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // 右侧：开关操控与状态反馈
            if (device.capabilities.contains("power")) {
                Switch(
                    checked = isOn,
                    onCheckedChange = { onToggle(device.logicalId, isOn) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldAccent,
                        checkedTrackColor = EmeraldAccent.copy(alpha = 0.35f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceBorder
                    )
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmberAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "只读监测", color = AmberAccent, fontSize = 11.sp)
                }
            }
        }
    }
}
