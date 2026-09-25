package com.jev.assistant.device

object DeviceContract {

    fun generateLogicalId(room: String, type: String, index: Int): String {
        val roomCode = when (room) {
            "客厅" -> "living"
            "主卧" -> "master"
            "次卧" -> "secondary"
            "厨房" -> "kitchen"
            "餐厅" -> "dining"
            "卫生间", "主卫", "客卫", "厕所" -> "toilet"
            "阳台" -> "balcony"
            "玄关", "门厅" -> "entry"
            else -> "unassigned"
        }

        val typeCode = when {
            type.contains("灯") -> "light"
            type.contains("空调") -> "ac"
            type.contains("风扇") -> "fan"
            type.contains("窗帘") -> "curtain"
            type.contains("插座") || type.contains("插排") -> "outlet"
            type.contains("开关") -> "switch"
            type.contains("门锁") -> "lock"
            type.contains("传感器") -> "sensor"
            type.contains("晾衣机") || type.contains("晾衣架") -> "drying_rack"
            else -> "device"
        }

        return String.format("h01_%s_%s_%03d", roomCode, typeCode, index)
    }

    fun validateAction(type: String, action: String, params: Map<String, Any>): Pair<Boolean, String> {
        when (action) {
            "set_power", "set_power_on", "set_power_off" -> {
                return true to "OK"
            }
            "set_brightness" -> {
                val value = (params["percent"] as? Number)?.toInt() ?: 50
                if (value !in 1..100) return false to "亮度百分比必须在 1..100 之间"
                return true to "OK"
            }
            "set_temperature" -> {
                val value = (params["celsius"] as? Number)?.toDouble() ?: 26.0
                if (value !in 16.0..32.0) return false to "空调温度必须在 16..32℃ 之间"
                return true to "OK"
            }
            "open", "close", "stop" -> {
                return true to "OK"
            }
            "set_speed" -> {
                val value = (params["percent"] as? Number)?.toInt() ?: 50
                if (value !in 1..100) return false to "风扇风速必须在 1..100 之间"
                return true to "OK"
            }
            else -> return true to "OK"
        }
    }
}
