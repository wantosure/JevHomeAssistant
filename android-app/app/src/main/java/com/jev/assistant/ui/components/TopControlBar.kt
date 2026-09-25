package com.jev.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jev.assistant.ui.theme.AmberAccent
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.DarkSurface
import com.jev.assistant.ui.theme.DarkSurfaceBorder
import com.jev.assistant.ui.theme.EmeraldAccent
import com.jev.assistant.ui.theme.RedAccent
import com.jev.assistant.ui.theme.TextMuted
import com.jev.assistant.ui.theme.TextPrimary

@Composable
fun TopControlBar(
    isListening: Boolean,
    defaultRoom: String,
    isLiveMode: Boolean,
    onToggleListening: () -> Unit,
    onRoomClick: () -> Unit,
    onToggleLiveMode: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface.copy(alpha = 0.9f))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 监听状态胶囊
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isListening) EmeraldAccent.copy(alpha = 0.15f) else RedAccent.copy(alpha = 0.15f))
                .clickable { onToggleListening() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isListening) EmeraldAccent else RedAccent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = null,
                tint = if (isListening) EmeraldAccent else RedAccent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isListening) "静默监听中" else "已暂停",
                color = if (isListening) EmeraldAccent else RedAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 2. 房间切换
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceBorder.copy(alpha = 0.5f))
                .clickable { onRoomClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "📍 $defaultRoom",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3. Observe / Live 模式指示器
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isLiveMode) EmeraldAccent.copy(alpha = 0.2f) else AmberAccent.copy(alpha = 0.2f))
                .clickable { onToggleLiveMode() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isLiveMode) "LIVE 真实" else "OBSERVE 观察",
                color = if (isLiveMode) EmeraldAccent else AmberAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 4. 工具图标组
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDevices, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "设备列表",
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onOpenAlarms, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "本地闹钟",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "系统设置",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
