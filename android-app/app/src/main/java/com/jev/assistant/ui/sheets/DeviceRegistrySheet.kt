package com.jev.assistant.ui.sheets

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jev.assistant.data.DeviceItem
import com.jev.assistant.ui.theme.AmberAccent
import com.jev.assistant.ui.theme.CyanAccent
import com.jev.assistant.ui.theme.DarkBackground
import com.jev.assistant.ui.theme.DarkSurfaceBorder
import com.jev.assistant.ui.theme.DarkSurfaceVariant
import com.jev.assistant.ui.theme.EmeraldAccent
import com.jev.assistant.ui.theme.TextMuted
import com.jev.assistant.ui.theme.TextPrimary
import com.jev.assistant.ui.theme.TextSecondary

@Composable
fun DeviceRegistrySheet(
    devices: List<DeviceItem>,
    onToggleDevice: (deviceId: String, currentPower: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedRoom by remember { mutableStateOf("全部") }
    val roomList = remember(devices) {
        listOf("全部") + devices.map { it.room }.distinct()
    }

    val filteredDevices = remember(selectedRoom, devices) {
        if (selectedRoom == "全部") devices else devices.filter { it.room == selectedRoom }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "米家全屋设备 · 本地模拟控制台",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共加载 ${devices.size} 台真实米家设备 · 支持语音与手动双向控制",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 房间水平筛选滑轨
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roomList.forEach { room ->
                val isSelected = room == selectedRoom
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) CyanAccent else DarkSurfaceVariant)
                        .clickable { selectedRoom = room }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = room,
                        color = if (isSelected) DarkBackground else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 设备列表展示
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(380.dp)
        ) {
            items(filteredDevices, key = { it.logicalId }) { dev ->
                val powerVal = dev.currentState["power"]
                val isOn = powerVal == 1 || powerVal == "1" || powerVal == true

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (isOn) EmeraldAccent.copy(alpha = 0.6f) else DarkSurfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧图标与信息
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isOn) EmeraldAccent.copy(alpha = 0.2f) else Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = dev.icon, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dev.name,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[${dev.room}]",
                                        color = CyanAccent,
                                        fontSize = 11.sp
                                    )
                                }
                                val stateDesc = buildString {
                                    append(dev.logicalId)
                                    dev.currentState["brightness"]?.let { append(" · 亮度: $it%") }
                                    dev.currentState["target_temperature"]?.let { append(" · 温度: ${it}℃") }
                                    dev.currentState["position"]?.let { append(" · 开合: $it%") }
                                }
                                Text(
                                    text = stateDesc,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // 右侧开关控件
                        if (dev.capabilities.contains("power") || dev.capabilities.contains("position")) {
                            Switch(
                                checked = isOn,
                                onCheckedChange = { onToggleDevice(dev.logicalId, isOn) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldAccent,
                                    checkedTrackColor = EmeraldAccent.copy(alpha = 0.3f),
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
                                Text(text = "只读状态", color = AmberAccent, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
