package com.jev.assistant.device

import com.jev.assistant.data.ExecutionPlan
import com.jev.assistant.data.RunEvent
import com.jev.assistant.data.RunStatus
import kotlinx.coroutines.delay

class HomeExecutor(private val registry: DeviceRegistry) {

    suspend fun executeHomeControl(plan: ExecutionPlan, isLive: Boolean): ExecutionPlan {
        plan.status = RunStatus.VALIDATING

        // 1. 判断是否为批量设备组控制
        val isGroup = plan.parameters["is_group"] == true || plan.targetDeviceId.startsWith("group_")
        if (isGroup) {
            return executeGroupControl(plan)
        }

        // 2. 单设备控制
        plan.events.add(RunEvent(stage = "能力校验", message = "正在校验模拟设备能力与动作参数合规性..."))
        delay(100)

        val device = registry.findDeviceById(plan.targetDeviceId)
        if (device == null) {
            plan.status = RunStatus.FAILED
            plan.statusMessage = "未找到指定设备: ${plan.targetDeviceId}"
            plan.events.add(RunEvent(stage = "能力校验", message = plan.statusMessage, isSuccess = false))
            return plan
        }

        val action = plan.action
        val params = plan.parameters

        plan.status = RunStatus.EXECUTING
        plan.events.add(RunEvent(stage = "模拟下发", message = "向本地米家模拟总线下发控制: $action (参数: $params)"))
        delay(120)

        when (action) {
            "set_power_on" -> {
                registry.updateDeviceProperty(device.logicalId, "power", 1)
            }
            "set_power_off" -> {
                registry.updateDeviceProperty(device.logicalId, "power", 0)
            }
            "set_brightness" -> {
                val b = (params["percent"] as? Number)?.toInt()
                    ?: com.jev.assistant.utils.NumericExtractor.extractFirstInt(plan.utteranceText)
                    ?: 50
                registry.updateDeviceProperty(device.logicalId, "brightness", b)
                registry.updateDeviceProperty(device.logicalId, "power", 1)
            }
            "set_temperature" -> {
                val t = (params["celsius"] as? Number)?.toDouble() ?: 26.0
                registry.updateDeviceProperty(device.logicalId, "target_temperature", t)
                registry.updateDeviceProperty(device.logicalId, "power", 1)
            }
            "open" -> {
                registry.updateDeviceProperty(device.logicalId, "position", 100)
                registry.updateDeviceProperty(device.logicalId, "motion", "open")
            }
            "close" -> {
                registry.updateDeviceProperty(device.logicalId, "position", 0)
                registry.updateDeviceProperty(device.logicalId, "motion", "close")
            }
            "stop" -> {
                registry.updateDeviceProperty(device.logicalId, "motion", "stop")
            }
            else -> {
                registry.updateDeviceProperty(device.logicalId, "power", 1)
            }
        }

        val updatedDev = registry.findDeviceById(device.logicalId)
        val stateSummary = updatedDev?.currentState?.entries?.joinToString(", ") { "${it.key}: ${it.value}" } ?: ""

        plan.status = RunStatus.SUCCEEDED
        plan.statusMessage = "本地模拟设备状态已改变: $stateSummary"
        plan.events.add(RunEvent(stage = "状态回读", message = "状态同步成功: $stateSummary"))

        return plan
    }

    private suspend fun executeGroupControl(plan: ExecutionPlan): ExecutionPlan {
        plan.events.add(RunEvent(stage = "分组解析", message = "匹配到设备组: ${plan.targetDeviceName}"))
        delay(120)

        @Suppress("UNCHECKED_CAST")
        val deviceIds = (plan.parameters["device_ids"] as? List<String>) ?: emptyList()
        val action = plan.action
        val params = plan.parameters

        plan.status = RunStatus.EXECUTING
        plan.events.add(RunEvent(stage = "批量下发", message = "正在向 ${deviceIds.size} 台设备批量下发: $action..."))
        delay(180)

        var successCount = 0
        for (id in deviceIds) {
            when (action) {
                "set_power_on" -> {
                    registry.updateDeviceProperty(id, "power", 1)
                    successCount++
                }
                "set_power_off" -> {
                    registry.updateDeviceProperty(id, "power", 0)
                    successCount++
                }
                "set_brightness" -> {
                    val b = (params["percent"] as? Number)?.toInt()
                        ?: com.jev.assistant.utils.NumericExtractor.extractFirstInt(plan.utteranceText)
                        ?: 50
                    registry.updateDeviceProperty(id, "brightness", b)
                    registry.updateDeviceProperty(id, "power", 1)
                    successCount++
                }
                "set_temperature" -> {
                    val t = (params["celsius"] as? Number)?.toDouble() ?: 26.0
                    registry.updateDeviceProperty(id, "target_temperature", t)
                    registry.updateDeviceProperty(id, "power", 1)
                    successCount++
                }
                "open" -> {
                    registry.updateDeviceProperty(id, "position", 100)
                    successCount++
                }
                "close" -> {
                    registry.updateDeviceProperty(id, "position", 0)
                    successCount++
                }
                else -> {
                    registry.updateDeviceProperty(id, "power", 1)
                    successCount++
                }
            }
        }

        plan.status = RunStatus.SUCCEEDED
        plan.statusMessage = "设备组「${plan.targetDeviceName}」全部批量执行成功 (已同步 $successCount 台设备)"
        plan.events.add(RunEvent(stage = "批量回读", message = "共 $successCount 台设备完成状态更新"))

        return plan
    }
}
