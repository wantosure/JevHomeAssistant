package com.jev.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jev.assistant.data.ExecutionPlan
import com.jev.assistant.data.IntentType
import com.jev.assistant.data.RunStatus
import com.jev.assistant.ui.theme.AmberAccent
import com.jev.assistant.ui.theme.BlueNeon
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.DarkSurface
import com.jev.assistant.ui.theme.DarkSurfaceBorder
import com.jev.assistant.ui.theme.DarkSurfaceVariant
import com.jev.assistant.ui.theme.EmeraldAccent
import com.jev.assistant.ui.theme.RedAccent
import com.jev.assistant.ui.theme.TextMuted
import com.jev.assistant.ui.theme.TextPrimary
import com.jev.assistant.ui.theme.TextSecondary

@Composable
fun ExecutionHeroCard(
    plan: ExecutionPlan?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = plan != null,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (plan == null) return@AnimatedVisibility

        val borderColor = when (plan.status) {
            RunStatus.SUCCEEDED -> EmeraldAccent
            RunStatus.FAILED -> RedAccent
            RunStatus.DECIDING, RunStatus.VALIDATING, RunStatus.EXECUTING -> CyanAccent
            else -> DarkSurfaceBorder
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(DarkSurfaceVariant.copy(alpha = 0.95f), DarkSurface.copy(alpha = 0.98f))
                    )
                )
                .border(1.2.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                // 1. 顶部原话与业务徽章
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (plan.intent) {
                            IntentType.HOME_CONTROL -> Icons.Default.Lightbulb
                            IntentType.ALARM -> Icons.Default.Alarm
                            IntentType.COMPUTER_CONTROL -> Icons.Default.Computer
                            else -> Icons.Default.MeetingRoom
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BlueNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plan.targetDeviceName,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // 需求 1 核心：操作设备卡片显著标注房间
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyanAccent.copy(alpha = 0.25f))
                                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "📍 ${plan.room}",
                                        color = CyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "原话: \"${plan.utteranceText}\"",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // 需求 4 核心：标注每次会话的价格
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmberAccent.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "单次: ¥${String.format("%.4f", plan.cost)}",
                                        color = AmberAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // 模式标签
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (plan.isLive) EmeraldAccent.copy(alpha = 0.2f) else AmberAccent.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (plan.isLive) "LIVE 真实执行" else "OBSERVE 观察模式",
                            color = if (plan.isLive) EmeraldAccent else AmberAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 状态说明
                Text(
                    text = plan.statusMessage.ifBlank { "正在分析指令..." },
                    color = when (plan.status) {
                        RunStatus.SUCCEEDED -> EmeraldAccent
                        RunStatus.FAILED -> RedAccent
                        else -> CyanAccent
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 五阶段真实事件轨迹
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    plan.events.takeLast(4).forEach { event ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (event.isSuccess) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (event.isSuccess) EmeraldAccent else RedAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[${event.stage}] ${event.message}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
