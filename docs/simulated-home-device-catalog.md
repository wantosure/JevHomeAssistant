# 家庭模拟设备、分组与语音指令全集 v1

版本：2026-09-27。用途：静默语音助手的意图识别、设备解析、参数提取与模拟执行评测。

本文件中每条执行、口语和设备专属负例句子都有固定用例 ID；英文对应句在[全量双语测试表](all-voice-test-cases.md)中使用相同 ID。分组用例也纳入该表。

## 1. 使用约定

- 共 87 个截图条目：85 个模拟叶子设备、2 个截图灯组；10 个房间分类。忽略截图的在线和可用状态，所有条目都可参与模拟测试。
- 名称、房间、类别取自米家极客版截图；名称保留截图省略号。语音称呼、ID、通道数、能力、数值范围、灯组成员由本规范人为设定，不是硬件实测结论。例如本版所有灯具均假设支持 RGB、2700–5500K 色温及调光。
- 本文是独立测试夹具，优先用于本次语料生成。既有 `mijia-100-devices.json` 含不同房间与命名，本次不使用、不覆盖。旧真实接入协议的限制仍适用于真实执行。
- `sim87_` 是条目 ID，`simgrp_` 是派生组 ID，永不当作米家物理 ID。改名和移动房间不改已分配 ID。每条语料记录 `registry_version=sim-home-v1`。
- 控制动作为封闭集合；未列出的动作不支持。只读设备同样有查询语句，但不得因测试需要虚构可写的温湿度、烟雾、人体状态。无线开关是输入设备，不是可远程按压的执行器。
- 物理墙壁开关只模拟各路通断，亮度/色温/颜色属于灯具。`电视开关` 不代表电视本体；`除臭风扇2` 按截图是插座；`热水` 是插座；`视频`、`解锁` 是独立开关，不推断其关联负载，也不隐含门锁动作。
- 本文的“穷举”是穷举支持的动作、离散参数和典型边界，并不是穷举无限自然语言。连续/整数范围以约束定义，语音例句覆盖最小、中间、最大；组合与越界值按第 6 节生成。
- 所有执行都写入模拟状态，不访问真实设备。微波炉加热、门锁开锁、出粮等在此仅用于分类测试，不据此扩展真实产品权限。

## 2. 统一语义与参数规则

### 2.1 判断输出

```json
{
  "registry_version": "sim-home-v1",
  "decision": "execute",
  "reason": "explicit_request",
  "target_id": "simgrp_all_ac",
  "action": "set_power",
  "parameters": {"on": false},
  "execution_mode": "simulate"
}
```

`decision` 固定为 `execute / ignore / needs_input`。不支持的动作或越界参数返回 `ignore`，reason 分别为 `unsupported_capability / invalid_parameter`；目标不唯一或缺参数返回 `needs_input`。普通闲聊、引用、否定返回 `ignore`。`needs_input` 只在页面显示缺失内容，不主动语音追问。查询也可为 execute，结果仅在页面显示。

### 2.2 参数和状态

- 数字按指定单位归一化：百分之五十、50 percent → 50；半开 → open_percent=50；2700K/2700开尔文 → 2700。绝对数值越界直接拒绝，不截断。
- 灯亮度 0 → power=false；亮度 1..100 → power=true。关灯保留上次非零亮度，开灯恢复该亮度（初始 50）。色温/颜色只改设置，不自动开灯；set_color 切为 RGB 模式，set_color_temperature 切为白光模式。黑色 #000000 保持供电状态、光输出为零，区别于关灯。
- “调亮一点/调暗一点”对应 adjust_brightness 的 delta=+10/-10 个百分点；该相对动作计算结果允许截断到 0..100。0 是合法 delta，表示不变。“最亮/最暗”明确映射 100/0。
- 空调 set_mode/set_temperature/set_fan_speed/扫风只修改设置，不隐含开机；“打开空调并制冷到26度”展开为有序的开机、模式、温度三个动作。新风是独立布尔功能；“切换到新风”映射 set_fresh_air(enabled=true)，不造 fresh_air 主模式。
- 插排总电源与分路状态独立，实际输出=总电源 AND 分路开关；单独开某一路不自动开总电源。墙壁开关没有 channel 时，多路需要补全；“整个/全部”用 all；单路默认 ch1。
- 窗帘百分比是打开比例；晾衣架百分比是高度，二者不要反向。移动动作在模拟器中立即到位；stop 保持当前值。
- 音量为整数 0..100，音箱与音频连接器 mute 独立于电源；音量大于0会解除静音。播放只操作预置本地测试曲目，不做在线检索和购买。
- 普通开关/插座与传感器无隐式联动。房间灯组不包含墙壁开关、浴霸照明、晾衣架照明或屏幕；“所有照明”若要跨这些类别应另行定义，不默认为所有灯。
- 默认关闭所有可切换功能，灯记忆亮度50/色温4000/RGB白色，空调26度/auto，新风1档，窗帘打开比例0，晾衣架高度100，音量40，风扇1档，加湿目标50%，加湿/香氛1档，水暖35度。其余数值默认取动作允许的最小值；枚举默认取表中首项。每条独立用例重置状态，多轮用例共享同一会话状态。
- 传感器固定夹具：温度24℃、湿度50%、occupied=false、motion=false、pressed=false、smoke_detected=false；电量80%；PM2.5=15μg/m³；机器人 docked；微波炉 idle；门锁 locked；最近体重60kg/体脂20%；路由器客户端和网关子设备列表初始为空。查询返回相应字段，get_state 返回该类型全部模拟状态。
- 其余查询状态：遥控器 last_event=null、battery_percent=80；喂食器 remaining_grams=1000、dispensed_portions=0；摄像机 recording=false、privacy=false；门铃 motion_alert=false；屏幕 screen_on=false；播放设备 playback=paused、track_index=0。喂食扣减每份10克，余粮不足返回模拟执行失败，不假装成功。
- 暂停/恢复/下一首等有状态动作必须在用例中指定前置状态；不满足前置条件时，意图判断仍可正确，模拟执行返回 invalid_state。门锁初始locked，重复lock等幂等设置成功但状态不变。所有开关通道初始关闭，不绑定其他设备。
- 不含隐藏的定时语义：“十分钟后开灯”交给调度业务，不能当作现在开灯；只有表中 set_timer / set_duration 有本设备计时功能。

## 3. 分组规则与完整成员

全屋组按类型聚合；房间组按房间+类型聚合，单成员也建组。明确“全部/所有/都”才展开组。没有“全部”且匹配多台时 needs_input，不随便选第一台。类型不跨房间猜测；未指定房间但某类型全屋唯一时可直接解析，其他情况需用例明确提供 default_room 或补充房间。

组操作递归展开、按设备 ID 去重，再做全部成员能力及参数校验，最后统一执行。任一成员不支持时，整组不执行并报告具体成员；不要静默跳过。只读组只聚合查询；成员能力不同则只接受共有动作。例如所有开关只接受 set_channel_power(channel=all,on=...)，不对所有开关统一指定 ch6。组相对调光使用各成员自身当前值。

示例：“关闭所有空调”→ simgrp_all_ac / set_power / {on:false}；“关闭主卧所有灯”→ simgrp_master_light / set_power / {on:false}；“所有空调设为26度”→ simgrp_all_ac / set_temperature / {celsius:26}；“厨房所有灯设为暖白光”→ simgrp_kitchen_light / set_color_temperature / {kelvin:3000}。

白光别名：暖光2700K、暖白3000K、自然光4000K、冷白5500K。各设备唯一语音称呼可直接用；原始同名必须结合房间。厨房“基础灯组”和“完整灯组”是下文人为设定的成员映射，不代表真实米家已有绑定。

| 分组 ID | 分组名 | 数量 | 叶子成员 ID |
| --- | --- | ---: | --- |
| `simgrp_all_ac` | 全屋所有空调 | 3 | `sim87_living_ac_001`、`sim87_master_ac_001`、`sim87_secondary_ac_001` |
| `simgrp_all_light` | 全屋所有灯 | 10 | `sim87_living_light_001`、`sim87_master_light_001`、`sim87_master_light_002`、`sim87_secondary_light_001`、`sim87_kitchen_light_001`、`sim87_kitchen_light_002`、`sim87_kitchen_light_003`、`sim87_kitchen_light_004`、`sim87_toilet_light_001`、`sim87_utility_light_001` |
| `simgrp_all_switch` | 全屋所有开关 | 9 | `sim87_living_switch3_001`、`sim87_living_switch2_001`、`sim87_living_switch6_001`、`sim87_master_switch2_001`、`sim87_secondary_switch2_001`、`sim87_kitchen_switch2_001`、`sim87_toilet_switch1_001`、`sim87_entry_switch1_001`、`sim87_entry_switch1_002` |
| `simgrp_all_socket` | 全屋所有插座 | 3 | `sim87_living_socket_001`、`sim87_living_socket_002`、`sim87_toilet_socket_001` |
| `simgrp_all_strip` | 全屋所有插排 | 1 | `sim87_living_strip_001` |
| `simgrp_all_curtain` | 全屋所有窗帘 | 3 | `sim87_living_curtain_001`、`sim87_master_curtain_001`、`sim87_kitchen_curtain_001` |
| `simgrp_all_speaker` | 全屋所有音箱 | 6 | `sim87_living_speaker_001`、`sim87_master_speaker_001`、`sim87_master_speaker_002`、`sim87_secondary_speaker_001`、`sim87_toilet_speaker_001`、`sim87_utility_speaker_001` |
| `simgrp_all_purifier` | 全屋所有空气净化器 | 1 | `sim87_living_purifier_001` |
| `simgrp_all_feeder` | 全屋所有宠物喂食机 | 1 | `sim87_living_feeder_001` |
| `simgrp_all_washer` | 全屋所有擦地机 | 1 | `sim87_living_washer_001` |
| `simgrp_all_vacuum` | 全屋所有吸尘器 | 1 | `sim87_living_vacuum_001` |
| `simgrp_all_camera` | 全屋所有摄像机 | 4 | `sim87_living_camera_001`、`sim87_secondary_camera_001`、`sim87_secondary_camera_002`、`sim87_garden_camera_001` |
| `simgrp_all_thermo` | 全屋所有温湿度传感器 | 4 | `sim87_living_thermo_001`、`sim87_living_thermo_002`、`sim87_secondary_thermo_001`、`sim87_secondary_thermo_002` |
| `simgrp_all_remote` | 全屋所有遥控器 | 7 | `sim87_living_remote_001`、`sim87_master_remote_001`、`sim87_secondary_remote_001`、`sim87_secondary_remote_002`、`sim87_toilet_remote_001`、`sim87_entry_remote_001`、`sim87_dining_remote_001` |
| `simgrp_all_router` | 全屋所有路由器 | 1 | `sim87_living_router_001` |
| `simgrp_all_gateway` | 全屋所有网关 | 3 | `sim87_living_gateway_001`、`sim87_living_gateway_002`、`sim87_living_gateway_003` |
| `simgrp_all_aroma` | 全屋所有香薰机 | 1 | `sim87_living_aroma_001` |
| `simgrp_all_fan` | 全屋所有风扇 | 3 | `sim87_living_fan_001`、`sim87_master_fan_001`、`sim87_secondary_fan_001` |
| `simgrp_all_audio` | 全屋所有影音配件 | 1 | `sim87_living_audio_001` |
| `simgrp_all_humidifier` | 全屋所有加湿器 | 3 | `sim87_master_humidifier_001`、`sim87_master_humidifier_002`、`sim87_secondary_humidifier_001` |
| `simgrp_all_motion` | 全屋所有人体传感器 | 1 | `sim87_master_motion_001` |
| `simgrp_all_pressure` | 全屋所有压力传感器 | 1 | `sim87_master_pressure_001` |
| `simgrp_all_blanket` | 全屋所有电热毯 | 2 | `sim87_master_blanket_001`、`sim87_utility_blanket_001` |
| `simgrp_all_presence` | 全屋所有存在传感器 | 2 | `sim87_kitchen_presence_001`、`sim87_toilet_presence_001` |
| `simgrp_all_smoke` | 全屋所有烟雾传感器 | 1 | `sim87_kitchen_smoke_001` |
| `simgrp_all_microwave` | 全屋所有微波炉 | 1 | `sim87_kitchen_microwave_001` |
| `simgrp_all_robot` | 全屋所有扫地机器人 | 1 | `sim87_kitchen_robot_001` |
| `simgrp_all_bath` | 全屋所有浴霸 | 1 | `sim87_toilet_bath_001` |
| `simgrp_all_panel` | 全屋所有控制面板 | 2 | `sim87_entry_panel_001`、`sim87_balcony_panel_001` |
| `simgrp_all_lock` | 全屋所有门锁 | 1 | `sim87_entry_lock_001` |
| `simgrp_all_doorbell` | 全屋所有可视门铃 | 3 | `sim87_entry_doorbell_001`、`sim87_entry_doorbell_002`、`sim87_entry_doorbell_003` |
| `simgrp_all_rack` | 全屋所有晾衣架 | 1 | `sim87_balcony_rack_001` |
| `simgrp_all_battery` | 全屋所有储能电源 | 1 | `sim87_utility_battery_001` |
| `simgrp_all_scale` | 全屋所有体重 | 1 | `sim87_utility_scale_001` |
| `simgrp_living_ac` | 客厅所有空调 | 1 | `sim87_living_ac_001` |
| `simgrp_living_light` | 客厅所有灯 | 1 | `sim87_living_light_001` |
| `simgrp_living_switch` | 客厅所有开关 | 3 | `sim87_living_switch3_001`、`sim87_living_switch2_001`、`sim87_living_switch6_001` |
| `simgrp_living_socket` | 客厅所有插座 | 2 | `sim87_living_socket_001`、`sim87_living_socket_002` |
| `simgrp_living_strip` | 客厅所有插排 | 1 | `sim87_living_strip_001` |
| `simgrp_living_curtain` | 客厅所有窗帘 | 1 | `sim87_living_curtain_001` |
| `simgrp_living_speaker` | 客厅所有音箱 | 1 | `sim87_living_speaker_001` |
| `simgrp_living_purifier` | 客厅所有空气净化器 | 1 | `sim87_living_purifier_001` |
| `simgrp_living_feeder` | 客厅所有宠物喂食机 | 1 | `sim87_living_feeder_001` |
| `simgrp_living_washer` | 客厅所有擦地机 | 1 | `sim87_living_washer_001` |
| `simgrp_living_vacuum` | 客厅所有吸尘器 | 1 | `sim87_living_vacuum_001` |
| `simgrp_living_camera` | 客厅所有摄像机 | 1 | `sim87_living_camera_001` |
| `simgrp_living_thermo` | 客厅所有温湿度传感器 | 2 | `sim87_living_thermo_001`、`sim87_living_thermo_002` |
| `simgrp_living_remote` | 客厅所有遥控器 | 1 | `sim87_living_remote_001` |
| `simgrp_living_router` | 客厅所有路由器 | 1 | `sim87_living_router_001` |
| `simgrp_living_gateway` | 客厅所有网关 | 3 | `sim87_living_gateway_001`、`sim87_living_gateway_002`、`sim87_living_gateway_003` |
| `simgrp_living_aroma` | 客厅所有香薰机 | 1 | `sim87_living_aroma_001` |
| `simgrp_living_fan` | 客厅所有风扇 | 1 | `sim87_living_fan_001` |
| `simgrp_living_audio` | 客厅所有影音配件 | 1 | `sim87_living_audio_001` |
| `simgrp_master_ac` | 主卧所有空调 | 1 | `sim87_master_ac_001` |
| `simgrp_master_light` | 主卧所有灯 | 2 | `sim87_master_light_001`、`sim87_master_light_002` |
| `simgrp_master_switch` | 主卧所有开关 | 1 | `sim87_master_switch2_001` |
| `simgrp_master_curtain` | 主卧所有窗帘 | 1 | `sim87_master_curtain_001` |
| `simgrp_master_speaker` | 主卧所有音箱 | 2 | `sim87_master_speaker_001`、`sim87_master_speaker_002` |
| `simgrp_master_remote` | 主卧所有遥控器 | 1 | `sim87_master_remote_001` |
| `simgrp_master_fan` | 主卧所有风扇 | 1 | `sim87_master_fan_001` |
| `simgrp_master_humidifier` | 主卧所有加湿器 | 2 | `sim87_master_humidifier_001`、`sim87_master_humidifier_002` |
| `simgrp_master_motion` | 主卧所有人体传感器 | 1 | `sim87_master_motion_001` |
| `simgrp_master_pressure` | 主卧所有压力传感器 | 1 | `sim87_master_pressure_001` |
| `simgrp_master_blanket` | 主卧所有电热毯 | 1 | `sim87_master_blanket_001` |
| `simgrp_secondary_ac` | 次卧所有空调 | 1 | `sim87_secondary_ac_001` |
| `simgrp_secondary_light` | 次卧所有灯 | 1 | `sim87_secondary_light_001` |
| `simgrp_secondary_switch` | 次卧所有开关 | 1 | `sim87_secondary_switch2_001` |
| `simgrp_secondary_speaker` | 次卧所有音箱 | 1 | `sim87_secondary_speaker_001` |
| `simgrp_secondary_camera` | 次卧所有摄像机 | 2 | `sim87_secondary_camera_001`、`sim87_secondary_camera_002` |
| `simgrp_secondary_thermo` | 次卧所有温湿度传感器 | 2 | `sim87_secondary_thermo_001`、`sim87_secondary_thermo_002` |
| `simgrp_secondary_remote` | 次卧所有遥控器 | 2 | `sim87_secondary_remote_001`、`sim87_secondary_remote_002` |
| `simgrp_secondary_fan` | 次卧所有风扇 | 1 | `sim87_secondary_fan_001` |
| `simgrp_secondary_humidifier` | 次卧所有加湿器 | 1 | `sim87_secondary_humidifier_001` |
| `simgrp_kitchen_light` | 厨房所有灯 | 4 | `sim87_kitchen_light_001`、`sim87_kitchen_light_002`、`sim87_kitchen_light_003`、`sim87_kitchen_light_004` |
| `simgrp_kitchen_switch` | 厨房所有开关 | 1 | `sim87_kitchen_switch2_001` |
| `simgrp_kitchen_curtain` | 厨房所有窗帘 | 1 | `sim87_kitchen_curtain_001` |
| `simgrp_kitchen_presence` | 厨房所有存在传感器 | 1 | `sim87_kitchen_presence_001` |
| `simgrp_kitchen_smoke` | 厨房所有烟雾传感器 | 1 | `sim87_kitchen_smoke_001` |
| `simgrp_kitchen_microwave` | 厨房所有微波炉 | 1 | `sim87_kitchen_microwave_001` |
| `simgrp_kitchen_robot` | 厨房所有扫地机器人 | 1 | `sim87_kitchen_robot_001` |
| `simgrp_toilet_light` | 厕所所有灯 | 1 | `sim87_toilet_light_001` |
| `simgrp_toilet_switch` | 厕所所有开关 | 1 | `sim87_toilet_switch1_001` |
| `simgrp_toilet_socket` | 厕所所有插座 | 1 | `sim87_toilet_socket_001` |
| `simgrp_toilet_speaker` | 厕所所有音箱 | 1 | `sim87_toilet_speaker_001` |
| `simgrp_toilet_remote` | 厕所所有遥控器 | 1 | `sim87_toilet_remote_001` |
| `simgrp_toilet_presence` | 厕所所有存在传感器 | 1 | `sim87_toilet_presence_001` |
| `simgrp_toilet_bath` | 厕所所有浴霸 | 1 | `sim87_toilet_bath_001` |
| `simgrp_entry_switch` | 门厅所有开关 | 2 | `sim87_entry_switch1_001`、`sim87_entry_switch1_002` |
| `simgrp_entry_remote` | 门厅所有遥控器 | 1 | `sim87_entry_remote_001` |
| `simgrp_entry_panel` | 门厅所有控制面板 | 1 | `sim87_entry_panel_001` |
| `simgrp_entry_lock` | 门厅所有门锁 | 1 | `sim87_entry_lock_001` |
| `simgrp_entry_doorbell` | 门厅所有可视门铃 | 3 | `sim87_entry_doorbell_001`、`sim87_entry_doorbell_002`、`sim87_entry_doorbell_003` |
| `simgrp_balcony_panel` | 阳台所有控制面板 | 1 | `sim87_balcony_panel_001` |
| `simgrp_balcony_rack` | 阳台所有晾衣架 | 1 | `sim87_balcony_rack_001` |
| `simgrp_utility_light` | 小工具所有灯 | 1 | `sim87_utility_light_001` |
| `simgrp_utility_speaker` | 小工具所有音箱 | 1 | `sim87_utility_speaker_001` |
| `simgrp_utility_blanket` | 小工具所有电热毯 | 1 | `sim87_utility_blanket_001` |
| `simgrp_utility_battery` | 小工具所有储能电源 | 1 | `sim87_utility_battery_001` |
| `simgrp_utility_scale` | 小工具所有体重 | 1 | `sim87_utility_scale_001` |
| `simgrp_dining_remote` | 餐厅所有遥控器 | 1 | `sim87_dining_remote_001` |
| `simgrp_garden_camera` | 菜地所有摄像机 | 1 | `sim87_garden_camera_001` |

### 3.1 灯光常用叫法与目标消歧

房间泛称“灯/照明”作为房间灯组的别名；产品类型词或名称别名用于定位单灯。如下口语例句是重要正例：

- **客厅灯组开灯**（1 盏，逐台执行）：
  - `GRP-simgrp_living_light-ROOM-ON-01` “开客厅灯”
  - `GRP-simgrp_living_light-ROOM-ON-02` “开客厅的灯”
  - `GRP-simgrp_living_light-ROOM-ON-03` “打开客厅照明”
  - `GRP-simgrp_living_light-ROOM-ON-04` “把客厅的灯打开”
  - `GRP-simgrp_living_light-ROOM-ON-05` “客厅开灯”
  - `GRP-simgrp_living_light-ROOM-ON-06` “点亮客厅所有灯”
  - `GRP-simgrp_living_light-ROOM-ON-07` “客厅灯都打开”
  - `GRP-simgrp_living_light-ROOM-ON-08` “麻烦开一下客厅的照明”
- **客厅灯组关灯**：
  - `GRP-simgrp_living_light-ROOM-OFF-01` “关客厅灯”
  - `GRP-simgrp_living_light-ROOM-OFF-02` “关客厅的灯”
  - `GRP-simgrp_living_light-ROOM-OFF-03` “关闭客厅照明”
  - `GRP-simgrp_living_light-ROOM-OFF-04` “把客厅的灯关掉”
  - `GRP-simgrp_living_light-ROOM-OFF-05` “客厅关灯”
  - `GRP-simgrp_living_light-ROOM-OFF-06` “熄灭客厅所有灯”
  - `GRP-simgrp_living_light-ROOM-OFF-07` “客厅灯都关掉”
  - `GRP-simgrp_living_light-ROOM-OFF-08` “麻烦关一下客厅的照明”
- **主卧灯组开灯**（2 盏，逐台执行）：
  - `GRP-simgrp_master_light-ROOM-ON-01` “开主卧灯”
  - `GRP-simgrp_master_light-ROOM-ON-02` “开主卧的灯”
  - `GRP-simgrp_master_light-ROOM-ON-03` “打开主卧照明”
  - `GRP-simgrp_master_light-ROOM-ON-04` “把主卧的灯打开”
  - `GRP-simgrp_master_light-ROOM-ON-05` “主卧开灯”
  - `GRP-simgrp_master_light-ROOM-ON-06` “点亮主卧所有灯”
  - `GRP-simgrp_master_light-ROOM-ON-07` “主卧灯都打开”
  - `GRP-simgrp_master_light-ROOM-ON-08` “麻烦开一下主卧的照明”
- **主卧灯组关灯**：
  - `GRP-simgrp_master_light-ROOM-OFF-01` “关主卧灯”
  - `GRP-simgrp_master_light-ROOM-OFF-02` “关主卧的灯”
  - `GRP-simgrp_master_light-ROOM-OFF-03` “关闭主卧照明”
  - `GRP-simgrp_master_light-ROOM-OFF-04` “把主卧的灯关掉”
  - `GRP-simgrp_master_light-ROOM-OFF-05` “主卧关灯”
  - `GRP-simgrp_master_light-ROOM-OFF-06` “熄灭主卧所有灯”
  - `GRP-simgrp_master_light-ROOM-OFF-07` “主卧灯都关掉”
  - `GRP-simgrp_master_light-ROOM-OFF-08` “麻烦关一下主卧的照明”
- **次卧灯组开灯**（1 盏，逐台执行）：
  - `GRP-simgrp_secondary_light-ROOM-ON-01` “开次卧灯”
  - `GRP-simgrp_secondary_light-ROOM-ON-02` “开次卧的灯”
  - `GRP-simgrp_secondary_light-ROOM-ON-03` “打开次卧照明”
  - `GRP-simgrp_secondary_light-ROOM-ON-04` “把次卧的灯打开”
  - `GRP-simgrp_secondary_light-ROOM-ON-05` “次卧开灯”
  - `GRP-simgrp_secondary_light-ROOM-ON-06` “点亮次卧所有灯”
  - `GRP-simgrp_secondary_light-ROOM-ON-07` “次卧灯都打开”
  - `GRP-simgrp_secondary_light-ROOM-ON-08` “麻烦开一下次卧的照明”
- **次卧灯组关灯**：
  - `GRP-simgrp_secondary_light-ROOM-OFF-01` “关次卧灯”
  - `GRP-simgrp_secondary_light-ROOM-OFF-02` “关次卧的灯”
  - `GRP-simgrp_secondary_light-ROOM-OFF-03` “关闭次卧照明”
  - `GRP-simgrp_secondary_light-ROOM-OFF-04` “把次卧的灯关掉”
  - `GRP-simgrp_secondary_light-ROOM-OFF-05` “次卧关灯”
  - `GRP-simgrp_secondary_light-ROOM-OFF-06` “熄灭次卧所有灯”
  - `GRP-simgrp_secondary_light-ROOM-OFF-07` “次卧灯都关掉”
  - `GRP-simgrp_secondary_light-ROOM-OFF-08` “麻烦关一下次卧的照明”
- **厨房灯组开灯**（4 盏，逐台执行）：
  - `GRP-simgrp_kitchen_light-ROOM-ON-01` “开厨房灯”
  - `GRP-simgrp_kitchen_light-ROOM-ON-02` “开厨房的灯”
  - `GRP-simgrp_kitchen_light-ROOM-ON-03` “打开厨房照明”
  - `GRP-simgrp_kitchen_light-ROOM-ON-04` “把厨房的灯打开”
  - `GRP-simgrp_kitchen_light-ROOM-ON-05` “厨房开灯”
  - `GRP-simgrp_kitchen_light-ROOM-ON-06` “点亮厨房所有灯”
  - `GRP-simgrp_kitchen_light-ROOM-ON-07` “厨房灯都打开”
  - `GRP-simgrp_kitchen_light-ROOM-ON-08` “麻烦开一下厨房的照明”
- **厨房灯组关灯**：
  - `GRP-simgrp_kitchen_light-ROOM-OFF-01` “关厨房灯”
  - `GRP-simgrp_kitchen_light-ROOM-OFF-02` “关厨房的灯”
  - `GRP-simgrp_kitchen_light-ROOM-OFF-03` “关闭厨房照明”
  - `GRP-simgrp_kitchen_light-ROOM-OFF-04` “把厨房的灯关掉”
  - `GRP-simgrp_kitchen_light-ROOM-OFF-05` “厨房关灯”
  - `GRP-simgrp_kitchen_light-ROOM-OFF-06` “熄灭厨房所有灯”
  - `GRP-simgrp_kitchen_light-ROOM-OFF-07` “厨房灯都关掉”
  - `GRP-simgrp_kitchen_light-ROOM-OFF-08` “麻烦关一下厨房的照明”
- **厕所灯组开灯**（1 盏，逐台执行）：
  - `GRP-simgrp_toilet_light-ROOM-ON-01` “开厕所灯”
  - `GRP-simgrp_toilet_light-ROOM-ON-02` “开厕所的灯”
  - `GRP-simgrp_toilet_light-ROOM-ON-03` “打开厕所照明”
  - `GRP-simgrp_toilet_light-ROOM-ON-04` “把厕所的灯打开”
  - `GRP-simgrp_toilet_light-ROOM-ON-05` “厕所开灯”
  - `GRP-simgrp_toilet_light-ROOM-ON-06` “点亮厕所所有灯”
  - `GRP-simgrp_toilet_light-ROOM-ON-07` “厕所灯都打开”
  - `GRP-simgrp_toilet_light-ROOM-ON-08` “麻烦开一下厕所的照明”
- **厕所灯组关灯**：
  - `GRP-simgrp_toilet_light-ROOM-OFF-01` “关厕所灯”
  - `GRP-simgrp_toilet_light-ROOM-OFF-02` “关厕所的灯”
  - `GRP-simgrp_toilet_light-ROOM-OFF-03` “关闭厕所照明”
  - `GRP-simgrp_toilet_light-ROOM-OFF-04` “把厕所的灯关掉”
  - `GRP-simgrp_toilet_light-ROOM-OFF-05` “厕所关灯”
  - `GRP-simgrp_toilet_light-ROOM-OFF-06` “熄灭厕所所有灯”
  - `GRP-simgrp_toilet_light-ROOM-OFF-07` “厕所灯都关掉”
  - `GRP-simgrp_toilet_light-ROOM-OFF-08` “麻烦关一下厕所的照明”
- **小工具灯组开灯**（1 盏，逐台执行）：
  - `GRP-simgrp_utility_light-ROOM-ON-01` “开小工具灯”
  - `GRP-simgrp_utility_light-ROOM-ON-02` “开小工具的灯”
  - `GRP-simgrp_utility_light-ROOM-ON-03` “打开小工具照明”
  - `GRP-simgrp_utility_light-ROOM-ON-04` “把小工具的灯打开”
  - `GRP-simgrp_utility_light-ROOM-ON-05` “小工具开灯”
  - `GRP-simgrp_utility_light-ROOM-ON-06` “点亮小工具所有灯”
  - `GRP-simgrp_utility_light-ROOM-ON-07` “小工具灯都打开”
  - `GRP-simgrp_utility_light-ROOM-ON-08` “麻烦开一下小工具的照明”
- **小工具灯组关灯**：
  - `GRP-simgrp_utility_light-ROOM-OFF-01` “关小工具灯”
  - `GRP-simgrp_utility_light-ROOM-OFF-02` “关小工具的灯”
  - `GRP-simgrp_utility_light-ROOM-OFF-03` “关闭小工具照明”
  - `GRP-simgrp_utility_light-ROOM-OFF-04` “把小工具的灯关掉”
  - `GRP-simgrp_utility_light-ROOM-OFF-05` “小工具关灯”
  - `GRP-simgrp_utility_light-ROOM-OFF-06` “熄灭小工具所有灯”
  - `GRP-simgrp_utility_light-ROOM-OFF-07` “小工具灯都关掉”
  - `GRP-simgrp_utility_light-ROOM-OFF-08` “麻烦关一下小工具的照明”
- “开灯”“把灯打开”“灯亮一下”“照明打开”只有 `context.default_room` 已设置且该房间有灯组时，才映射到默认房间灯组；缺默认房间时 `needs_input/missing_room`。
- “关灯”“把灯关了”“照明全关”按同一默认房间规则映射 `set_power(on=false)`；“所有房间灯都关了”“全屋灯关闭”映射 `simgrp_all_light`。
- “开吸顶灯”“把顶灯打开”先用明确房间或 `default_room` 消歧；全屋有多盏吸顶灯而没有房间上下文时 `needs_input/ambiguous_target`。有房间且该房间只匹配一盏吸顶灯时，映射该设备 ID。
- “开主卧灯”指主卧灯组；“开主卧吸顶灯”指主卧吸顶灯单设备；“开卧室灯”因主卧、次卧都有灯且卧室未指明，需澄清。厨房“所有灯”组展开4盏叶子灯，两个截图灯组条目不额外产生状态。
- “开电视灯/看电视时把灯关暗一点”不是设备目标明确的开关命令；需要照明氛围联动时应作为单独场景，不在本设备目录内推断。

## 4. 房间与数量索引

| 房间 | 条目数 |
| --- | ---: |
| 客厅 | 25 |
| 主卧 | 14 |
| 次卧 | 12 |
| 厨房 | 12 |
| 厕所 | 7 |
| 门厅 | 8 |
| 阳台 | 2 |
| 小工具 | 5 |
| 餐厅 | 1 |
| 菜地 | 1 |

## 5. 全部设备详情与逐动作语音指令

每个语音例句后是预期 action(parameters)。同一行各参数示例互为独立用例；并非一次执行多次。每台设备最后附一条不支持或越界负例。

### 01. 客厅空调

- 设备名字：米家新风空调立式（3…）
- 唯一 ID：`sim87_living_ac_001`
- 所在房间：客厅
- 设备类型：空调
- 唯一语音称呼：客厅空调
- 所属分组：`simgrp_all_ac`、`simgrp_living_ac`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_ac_001-01-01` “打开客厅空调” → `{"on":true}`<br>`CMD-sim87_living_ac_001-01-02` “关闭客厅空调” → `{"on":false}` |
| `set_mode` | mode: enum {cool, heat, dry, fan, auto} | `CMD-sim87_living_ac_001-02-01` “把客厅空调切换到制冷” → `{"mode":"cool"}`<br>`CMD-sim87_living_ac_001-02-02` “把客厅空调切换到制热” → `{"mode":"heat"}`<br>`CMD-sim87_living_ac_001-02-03` “把客厅空调切换到除湿” → `{"mode":"dry"}`<br>`CMD-sim87_living_ac_001-02-04` “把客厅空调切换到送风” → `{"mode":"fan"}`<br>`CMD-sim87_living_ac_001-02-05` “把客厅空调切换到自动模式” → `{"mode":"auto"}` |
| `set_temperature` | celsius: integer，16..30，步长 1；摄氏度 | `CMD-sim87_living_ac_001-03-01` “把客厅空调温度设为16度” → `{"celsius":16}`<br>`CMD-sim87_living_ac_001-03-02` “把客厅空调温度设为26度” → `{"celsius":26}`<br>`CMD-sim87_living_ac_001-03-03` “把客厅空调温度设为30度” → `{"celsius":30}` |
| `set_fan_speed` | level: enum {auto, low, medium, high} | `CMD-sim87_living_ac_001-04-01` “把客厅空调风速设为自动” → `{"level":"auto"}`<br>`CMD-sim87_living_ac_001-04-02` “把客厅空调风速设为低档” → `{"level":"low"}`<br>`CMD-sim87_living_ac_001-04-03` “把客厅空调风速设为中档” → `{"level":"medium"}`<br>`CMD-sim87_living_ac_001-04-04` “把客厅空调风速设为高档” → `{"level":"high"}` |
| `set_fresh_air` | enabled: boolean | `CMD-sim87_living_ac_001-05-01` “打开客厅空调的新风” → `{"enabled":true}`<br>`CMD-sim87_living_ac_001-05-02` “关闭客厅空调的新风” → `{"enabled":false}` |
| `set_fresh_air_level` | level: integer，1..3，步长 1；新风档位 | `CMD-sim87_living_ac_001-06-01` “把客厅空调新风设为1档” → `{"level":1}`<br>`CMD-sim87_living_ac_001-06-02` “把客厅空调新风设为2档” → `{"level":2}`<br>`CMD-sim87_living_ac_001-06-03` “把客厅空调新风设为3档” → `{"level":3}` |
| `set_vertical_swing` | enabled: boolean | `CMD-sim87_living_ac_001-07-01` “打开客厅空调的上下扫风” → `{"enabled":true}`<br>`CMD-sim87_living_ac_001-07-02` “关闭客厅空调的上下扫风” → `{"enabled":false}` |
| `set_horizontal_swing` | enabled: boolean | `CMD-sim87_living_ac_001-08-01` “打开客厅空调的左右扫风” → `{"enabled":true}`<br>`CMD-sim87_living_ac_001-08-02` “关闭客厅空调的左右扫风” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_ac_001-09-01` “查询客厅空调的状态” → `{}` |

负例 `NEG-sim87_living_ac_001`：“把客厅空调温度设为40度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_ac_001-001` “把客厅空调温度设为26度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-002` “客厅空调温度26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-003` “客厅的空调设为26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-004` “客厅那台空调调到二十六度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-005` “客厅空调给我调26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-006` “客厅空调，二十六度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-007` “二十六度，客厅空调” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-008` “客厅空调温度改成26℃” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-009` “麻烦客厅空调降到26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-010` “客厅空调温控拨到26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_living_ac_001-011` “客厅空调开制冷” → `set_mode` `{"mode":"cool"}`
- `VAR-sim87_living_ac_001-012` “客厅的空调新风打开” → `set_fresh_air` `{"enabled":true}`
- `VAR-sim87_living_ac_001-013` “客厅空调除湿一下” → `set_mode` `{"mode":"dry"}`

### 02. 客厅吸顶灯

- 设备名字：米家吸顶灯
- 唯一 ID：`sim87_living_light_001`
- 所在房间：客厅
- 设备类型：灯
- 唯一语音称呼：客厅吸顶灯
- 所属分组：`simgrp_all_light`、`simgrp_living_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_light_001-01-01` “打开客厅吸顶灯” → `{"on":true}`<br>`CMD-sim87_living_light_001-01-02` “关闭客厅吸顶灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_living_light_001-02-01` “把客厅吸顶灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_living_light_001-02-02` “把客厅吸顶灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_living_light_001-02-03` “把客厅吸顶灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_living_light_001-03-01` “把客厅吸顶灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_living_light_001-03-02` “把客厅吸顶灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_living_light_001-03-03` “把客厅吸顶灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_living_light_001-04-01` “把客厅吸顶灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_living_light_001-04-02` “把客厅吸顶灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_living_light_001-04-03` “把客厅吸顶灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_living_light_001-04-04` “把客厅吸顶灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_living_light_001-04-05` “把客厅吸顶灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_living_light_001-04-06` “把客厅吸顶灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_living_light_001-04-07` “把客厅吸顶灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_living_light_001-04-08` “把客厅吸顶灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_living_light_001-05-01` “把客厅吸顶灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_living_light_001-05-02` “把客厅吸顶灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_living_light_001-05-03` “把客厅吸顶灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_living_light_001-06-01` “查询客厅吸顶灯的状态” → `{}` |

负例 `NEG-sim87_living_light_001`：“打开客厅吸顶灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_light_001-001` “开客厅吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-002` “开客厅的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-003` “打开客厅的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-004` “把客厅吸顶灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-005` “点亮客厅的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-006` “麻烦开一下客厅吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-007` “客厅吸顶灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-008` “关掉客厅的吸顶灯” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-009` “客厅的吸顶灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-010` “把客厅吸顶灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-011` “开客厅顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-012` “开客厅的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-013` “打开客厅的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-014` “把客厅顶灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-015` “点亮客厅的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-016` “麻烦开一下客厅顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-017` “客厅顶灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-018` “关掉客厅的顶灯” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-019` “客厅的顶灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-020` “把客厅顶灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-021` “开客厅天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-022` “开客厅的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-023` “打开客厅的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-024` “把客厅天花板灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-025` “点亮客厅的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-026` “麻烦开一下客厅天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-027` “客厅天花板灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-028` “关掉客厅的天花板灯” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-029` “客厅的天花板灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-030` “把客厅天花板灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-031` “客厅吸顶灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_light_001-032` “把客厅吸顶灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_living_light_001-033` “客厅吸顶灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_living_light_001-034` “客厅吸顶灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_living_light_001-035` “客厅吸顶灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_living_light_001-036` “客厅吸顶灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_living_light_001-037` “客厅吸顶灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_living_light_001-038` “客厅吸顶灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 03. 客厅三键开关

- 设备名字：小米智能开关Pro（三…）
- 唯一 ID：`sim87_living_switch3_001`
- 所在房间：客厅
- 设备类型：开关
- 唯一语音称呼：客厅三键开关
- 所属分组：`simgrp_all_switch`、`simgrp_living_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, ch2, ch3, all}；on: boolean | `CMD-sim87_living_switch3_001-01-01` “打开客厅三键开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_living_switch3_001-01-02` “关闭客厅三键开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_living_switch3_001-01-03` “打开客厅三键开关第2路” → `{"channel":"ch2","on":true}`<br>`CMD-sim87_living_switch3_001-01-04` “关闭客厅三键开关第2路” → `{"channel":"ch2","on":false}`<br>`CMD-sim87_living_switch3_001-01-05` “打开客厅三键开关第3路” → `{"channel":"ch3","on":true}`<br>`CMD-sim87_living_switch3_001-01-06` “关闭客厅三键开关第3路” → `{"channel":"ch3","on":false}`<br>`CMD-sim87_living_switch3_001-01-07` “打开客厅三键开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_living_switch3_001-01-08` “关闭客厅三键开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_switch3_001-02-01` “查询客厅三键开关的状态” → `{}` |

负例 `NEG-sim87_living_switch3_001`：“把客厅三键开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_switch3_001-001` “客厅三键开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_living_switch3_001-002` “客厅三键开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_living_switch3_001-003` “客厅三键开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_living_switch3_001-004` “客厅三键开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 04. 客厅双键开关

- 设备名字：小米智能开关Pro（双…）
- 唯一 ID：`sim87_living_switch2_001`
- 所在房间：客厅
- 设备类型：开关
- 唯一语音称呼：客厅双键开关
- 所属分组：`simgrp_all_switch`、`simgrp_living_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, ch2, all}；on: boolean | `CMD-sim87_living_switch2_001-01-01` “打开客厅双键开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_living_switch2_001-01-02` “关闭客厅双键开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_living_switch2_001-01-03` “打开客厅双键开关第2路” → `{"channel":"ch2","on":true}`<br>`CMD-sim87_living_switch2_001-01-04` “关闭客厅双键开关第2路” → `{"channel":"ch2","on":false}`<br>`CMD-sim87_living_switch2_001-01-05` “打开客厅双键开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_living_switch2_001-01-06` “关闭客厅双键开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_switch2_001-02-01` “查询客厅双键开关的状态” → `{}` |

负例 `NEG-sim87_living_switch2_001`：“把客厅双键开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_switch2_001-001` “客厅双键开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_living_switch2_001-002` “客厅双键开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_living_switch2_001-003` “客厅双键开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_living_switch2_001-004` “客厅双键开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 05. 客厅六键开关

- 设备名字：PTX 智能六键开关
- 唯一 ID：`sim87_living_switch6_001`
- 所在房间：客厅
- 设备类型：开关
- 唯一语音称呼：客厅六键开关
- 所属分组：`simgrp_all_switch`、`simgrp_living_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, ch2, ch3, ch4, ch5, ch6, all}；on: boolean | `CMD-sim87_living_switch6_001-01-01` “打开客厅六键开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_living_switch6_001-01-02` “关闭客厅六键开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_living_switch6_001-01-03` “打开客厅六键开关第2路” → `{"channel":"ch2","on":true}`<br>`CMD-sim87_living_switch6_001-01-04` “关闭客厅六键开关第2路” → `{"channel":"ch2","on":false}`<br>`CMD-sim87_living_switch6_001-01-05` “打开客厅六键开关第3路” → `{"channel":"ch3","on":true}`<br>`CMD-sim87_living_switch6_001-01-06` “关闭客厅六键开关第3路” → `{"channel":"ch3","on":false}`<br>`CMD-sim87_living_switch6_001-01-07` “打开客厅六键开关第4路” → `{"channel":"ch4","on":true}`<br>`CMD-sim87_living_switch6_001-01-08` “关闭客厅六键开关第4路” → `{"channel":"ch4","on":false}`<br>`CMD-sim87_living_switch6_001-01-09` “打开客厅六键开关第5路” → `{"channel":"ch5","on":true}`<br>`CMD-sim87_living_switch6_001-01-10` “关闭客厅六键开关第5路” → `{"channel":"ch5","on":false}`<br>`CMD-sim87_living_switch6_001-01-11` “打开客厅六键开关第6路” → `{"channel":"ch6","on":true}`<br>`CMD-sim87_living_switch6_001-01-12` “关闭客厅六键开关第6路” → `{"channel":"ch6","on":false}`<br>`CMD-sim87_living_switch6_001-01-13` “打开客厅六键开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_living_switch6_001-01-14` “关闭客厅六键开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_switch6_001-02-01` “查询客厅六键开关的状态” → `{}` |

负例 `NEG-sim87_living_switch6_001`：“把客厅六键开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_switch6_001-001` “客厅六键开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_living_switch6_001-002` “客厅六键开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_living_switch6_001-003` “客厅六键开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_living_switch6_001-004` “客厅六键开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 06. 客厅落地插座

- 设备名字：落地插座
- 唯一 ID：`sim87_living_socket_001`
- 所在房间：客厅
- 设备类型：插座
- 唯一语音称呼：客厅落地插座
- 所属分组：`simgrp_all_socket`、`simgrp_living_socket`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_socket_001-01-01` “打开客厅落地插座” → `{"on":true}`<br>`CMD-sim87_living_socket_001-01-02` “关闭客厅落地插座” → `{"on":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_socket_001-02-01` “查询客厅落地插座的状态” → `{}` |

负例 `NEG-sim87_living_socket_001`：“把客厅落地插座亮度设为50%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_socket_001-001` “客厅落地插座开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_socket_001-002` “客厅落地插座关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_socket_001-003` “看一下客厅落地插座现在什么状态” → `get_state` `{}`

### 07. 客厅除臭风扇插座

- 设备名字：除臭风扇2
- 唯一 ID：`sim87_living_socket_002`
- 所在房间：客厅
- 设备类型：插座
- 唯一语音称呼：客厅除臭风扇插座
- 所属分组：`simgrp_all_socket`、`simgrp_living_socket`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_socket_002-01-01` “打开客厅除臭风扇插座” → `{"on":true}`<br>`CMD-sim87_living_socket_002-01-02` “关闭客厅除臭风扇插座” → `{"on":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_socket_002-02-01` “查询客厅除臭风扇插座的状态” → `{}` |

负例 `NEG-sim87_living_socket_002`：“把客厅除臭风扇插座亮度设为50%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_socket_002-001` “客厅除臭风扇插座开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_socket_002-002` “客厅除臭风扇插座关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_socket_002-003` “看一下客厅除臭风扇插座现在什么状态” → `get_state` `{}`

### 08. 客厅插排

- 设备名字：插排
- 唯一 ID：`sim87_living_strip_001`
- 所在房间：客厅
- 设备类型：插排
- 唯一语音称呼：客厅插排
- 所属分组：`simgrp_all_strip`、`simgrp_living_strip`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_strip_001-01-01` “打开客厅插排” → `{"on":true}`<br>`CMD-sim87_living_strip_001-01-02` “关闭客厅插排” → `{"on":false}` |
| `set_channel_power` | channel: enum {ch1,ch2,ch3}；on: boolean | `CMD-sim87_living_strip_001-02-01` “打开客厅插排第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_living_strip_001-02-02` “关闭客厅插排第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_living_strip_001-02-03` “打开客厅插排第2路” → `{"channel":"ch2","on":true}`<br>`CMD-sim87_living_strip_001-02-04` “关闭客厅插排第2路” → `{"channel":"ch2","on":false}`<br>`CMD-sim87_living_strip_001-02-05` “打开客厅插排第3路” → `{"channel":"ch3","on":true}`<br>`CMD-sim87_living_strip_001-02-06` “关闭客厅插排第3路” → `{"channel":"ch3","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_strip_001-03-01` “查询客厅插排的状态” → `{}` |

负例 `NEG-sim87_living_strip_001`：“把客厅插排电压调到300伏”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_strip_001-001` “客厅插排开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_strip_001-002` “客厅插排关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_strip_001-003` “看一下客厅插排现在什么状态” → `get_state` `{}`

### 09. 客厅窗帘

- 设备名字：Ai智能窗帘电机
- 唯一 ID：`sim87_living_curtain_001`
- 所在房间：客厅
- 设备类型：窗帘
- 唯一语音称呼：客厅窗帘
- 所属分组：`simgrp_all_curtain`、`simgrp_living_curtain`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `open` | 无参数 | `CMD-sim87_living_curtain_001-01-01` “打开客厅窗帘” → `{}` |
| `close` | 无参数 | `CMD-sim87_living_curtain_001-02-01` “关闭客厅窗帘” → `{}` |
| `stop` | 无参数 | `CMD-sim87_living_curtain_001-03-01` “停止客厅窗帘移动” → `{}` |
| `set_position` | open_percent: integer，0..100，步长 1；打开百分比，0 全关，100 全开 | `CMD-sim87_living_curtain_001-04-01` “把客厅窗帘打开到0%” → `{"open_percent":0}`<br>`CMD-sim87_living_curtain_001-04-02` “把客厅窗帘打开到50%” → `{"open_percent":50}`<br>`CMD-sim87_living_curtain_001-04-03` “把客厅窗帘打开到100%” → `{"open_percent":100}` |
| `get_state` | 无参数 | `CMD-sim87_living_curtain_001-05-01` “查询客厅窗帘的状态” → `{}` |

负例 `NEG-sim87_living_curtain_001`：“把客厅窗帘打开到120%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_curtain_001-001` “客厅窗帘拉开” → `open` `{}`
- `VAR-sim87_living_curtain_001-002` “客厅窗帘合上” → `close` `{}`
- `VAR-sim87_living_curtain_001-003` “客厅窗帘开一半” → `set_position` `{"open_percent":50}`
- `VAR-sim87_living_curtain_001-004` “客厅窗帘别动了” → `stop` `{}`

### 10. 客厅音箱

- 设备名字：Xiaomi Sound 2 Max
- 唯一 ID：`sim87_living_speaker_001`
- 所在房间：客厅
- 设备类型：音箱
- 唯一语音称呼：客厅音箱
- 所属分组：`simgrp_all_speaker`、`simgrp_living_speaker`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_speaker_001-01-01` “打开客厅音箱” → `{"on":true}`<br>`CMD-sim87_living_speaker_001-01-02` “关闭客厅音箱” → `{"on":false}` |
| `pause` | 无参数 | `CMD-sim87_living_speaker_001-02-01` “暂停客厅音箱播放” → `{}` |
| `resume` | 无参数 | `CMD-sim87_living_speaker_001-03-01` “继续客厅音箱播放” → `{}` |
| `next_track` | 无参数 | `CMD-sim87_living_speaker_001-04-01` “让客厅音箱播放下一首” → `{}` |
| `previous_track` | 无参数 | `CMD-sim87_living_speaker_001-05-01` “让客厅音箱播放上一首” → `{}` |
| `set_volume` | percent: integer，0..100，步长 1；百分比；0 静音，不是断电 | `CMD-sim87_living_speaker_001-06-01` “把客厅音箱音量设为0%” → `{"percent":0}`<br>`CMD-sim87_living_speaker_001-06-02` “把客厅音箱音量设为40%” → `{"percent":40}`<br>`CMD-sim87_living_speaker_001-06-03` “把客厅音箱音量设为100%” → `{"percent":100}` |
| `set_mute` | muted: boolean | `CMD-sim87_living_speaker_001-07-01` “让客厅音箱静音” → `{"muted":true}`<br>`CMD-sim87_living_speaker_001-07-02` “取消客厅音箱静音” → `{"muted":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_speaker_001-08-01` “查询客厅音箱的状态” → `{}` |

负例 `NEG-sim87_living_speaker_001`：“让客厅音箱购买一首歌”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_speaker_001-001` “客厅音箱音量一半” → `set_volume` `{"percent":50}`
- `VAR-sim87_living_speaker_001-002` “客厅音箱别出声了” → `set_mute` `{"muted":true}`
- `VAR-sim87_living_speaker_001-003` “客厅音箱下一首” → `next_track` `{}`
- `VAR-sim87_living_speaker_001-004` “客厅音箱继续播” → `resume` `{}`

### 11. 客厅空气净化器

- 设备名字：米家空气净化器 5S 内…
- 唯一 ID：`sim87_living_purifier_001`
- 所在房间：客厅
- 设备类型：空气净化器
- 唯一语音称呼：客厅空气净化器
- 所属分组：`simgrp_all_purifier`、`simgrp_living_purifier`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_purifier_001-01-01` “打开客厅空气净化器” → `{"on":true}`<br>`CMD-sim87_living_purifier_001-01-02` “关闭客厅空气净化器” → `{"on":false}` |
| `set_mode` | mode: enum {auto, sleep, manual} | `CMD-sim87_living_purifier_001-02-01` “把客厅空气净化器切换为自动模式” → `{"mode":"auto"}`<br>`CMD-sim87_living_purifier_001-02-02` “把客厅空气净化器切换为睡眠模式” → `{"mode":"sleep"}`<br>`CMD-sim87_living_purifier_001-02-03` “把客厅空气净化器切换为手动模式” → `{"mode":"manual"}` |
| `set_speed` | percent: integer，1..100，步长 1；风速百分比 | `CMD-sim87_living_purifier_001-03-01` “把客厅空气净化器风速设为1%” → `{"percent":1}`<br>`CMD-sim87_living_purifier_001-03-02` “把客厅空气净化器风速设为50%” → `{"percent":50}`<br>`CMD-sim87_living_purifier_001-03-03` “把客厅空气净化器风速设为100%” → `{"percent":100}` |
| `get_state` | 无参数 | `CMD-sim87_living_purifier_001-04-01` “查询客厅空气净化器的状态” → `{}` |

负例 `NEG-sim87_living_purifier_001`：“把客厅空气净化器温度设为26度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_purifier_001-001` “客厅空气净化器开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_purifier_001-002` “客厅空气净化器关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_purifier_001-003` “看一下客厅空气净化器现在什么状态” → `get_state` `{}`

### 12. 客厅喂食器

- 设备名字：叮零智能宠物喂食器
- 唯一 ID：`sim87_living_feeder_001`
- 所在房间：客厅
- 设备类型：宠物喂食机
- 唯一语音称呼：客厅喂食器
- 所属分组：`simgrp_all_feeder`、`simgrp_living_feeder`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `dispense` | portions: integer，1..10，步长 1；份，每份模拟 10 克 | `CMD-sim87_living_feeder_001-01-01` “让客厅喂食器出粮1份” → `{"portions":1}`<br>`CMD-sim87_living_feeder_001-01-02` “让客厅喂食器出粮3份” → `{"portions":3}`<br>`CMD-sim87_living_feeder_001-01-03` “让客厅喂食器出粮10份” → `{"portions":10}` |
| `get_state` | 无参数 | `CMD-sim87_living_feeder_001-02-01` “查询客厅喂食器的状态” → `{}` |

负例 `NEG-sim87_living_feeder_001`：“让客厅喂食器出粮100份”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_feeder_001-001` “看看客厅喂食器现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_feeder_001-002` “客厅喂食器状态查一下” → `get_state` `{}`

### 13. 客厅洗地机

- 设备名字：米家无线洗地机4 Ma…
- 唯一 ID：`sim87_living_washer_001`
- 所在房间：客厅
- 设备类型：擦地机/洗地机
- 唯一语音称呼：客厅洗地机
- 所属分组：`simgrp_all_washer`、`simgrp_living_washer`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_washer_001-01-01` “打开客厅洗地机” → `{"on":true}`<br>`CMD-sim87_living_washer_001-01-02` “关闭客厅洗地机” → `{"on":false}` |
| `set_mode` | mode: enum {eco, auto, turbo} | `CMD-sim87_living_washer_001-02-01` “把客厅洗地机切换为节能模式” → `{"mode":"eco"}`<br>`CMD-sim87_living_washer_001-02-02` “把客厅洗地机切换为自动模式” → `{"mode":"auto"}`<br>`CMD-sim87_living_washer_001-02-03` “把客厅洗地机切换为强力模式” → `{"mode":"turbo"}` |
| `start_self_clean` | 无参数 | `CMD-sim87_living_washer_001-03-01` “让客厅洗地机开始自清洁” → `{}` |
| `stop_self_clean` | 无参数 | `CMD-sim87_living_washer_001-04-01` “让客厅洗地机停止自清洁” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_living_washer_001-05-01` “查询客厅洗地机的状态” → `{}` |

负例 `NEG-sim87_living_washer_001`：“让客厅洗地机自己走到主卧清扫”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_washer_001-001` “客厅洗地机开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_washer_001-002` “客厅洗地机关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_washer_001-003` “看一下客厅洗地机现在什么状态” → `get_state` `{}`

### 14. 客厅吸尘器

- 设备名字：米家无线吸尘器3 基…
- 唯一 ID：`sim87_living_vacuum_001`
- 所在房间：客厅
- 设备类型：吸尘器
- 唯一语音称呼：客厅吸尘器
- 所属分组：`simgrp_all_vacuum`、`simgrp_living_vacuum`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_vacuum_001-01-01` “打开客厅吸尘器” → `{"on":true}`<br>`CMD-sim87_living_vacuum_001-01-02` “关闭客厅吸尘器” → `{"on":false}` |
| `set_mode` | mode: enum {eco, auto, turbo} | `CMD-sim87_living_vacuum_001-02-01` “把客厅吸尘器切换为节能模式” → `{"mode":"eco"}`<br>`CMD-sim87_living_vacuum_001-02-02` “把客厅吸尘器切换为自动模式” → `{"mode":"auto"}`<br>`CMD-sim87_living_vacuum_001-02-03` “把客厅吸尘器切换为强力模式” → `{"mode":"turbo"}` |
| `get_state` | 无参数 | `CMD-sim87_living_vacuum_001-03-01` “查询客厅吸尘器的状态” → `{}` |

负例 `NEG-sim87_living_vacuum_001`：“让客厅吸尘器自己走到主卧清扫”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_vacuum_001-001` “客厅吸尘器开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_vacuum_001-002` “客厅吸尘器关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_vacuum_001-003` “看一下客厅吸尘器现在什么状态” → `get_state` `{}`

### 15. 客厅摄像机

- 设备名字：小米智能摄像机 5 Pro…
- 唯一 ID：`sim87_living_camera_001`
- 所在房间：客厅
- 设备类型：摄像机
- 唯一语音称呼：客厅摄像机
- 所属分组：`simgrp_all_camera`、`simgrp_living_camera`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_camera_001-01-01` “打开客厅摄像机” → `{"on":true}`<br>`CMD-sim87_living_camera_001-01-02` “关闭客厅摄像机” → `{"on":false}` |
| `set_privacy` | enabled: boolean | `CMD-sim87_living_camera_001-02-01` “打开客厅摄像机隐私模式” → `{"enabled":true}`<br>`CMD-sim87_living_camera_001-02-02` “关闭客厅摄像机隐私模式” → `{"enabled":false}` |
| `set_recording` | enabled: boolean | `CMD-sim87_living_camera_001-03-01` “让客厅摄像机开始录像” → `{"enabled":true}`<br>`CMD-sim87_living_camera_001-03-02` “让客厅摄像机停止录像” → `{"enabled":false}` |
| `set_night_vision` | mode: enum {auto, on, off} | `CMD-sim87_living_camera_001-04-01` “把客厅摄像机夜视设为自动” → `{"mode":"auto"}`<br>`CMD-sim87_living_camera_001-04-02` “把客厅摄像机夜视设为开启” → `{"mode":"on"}`<br>`CMD-sim87_living_camera_001-04-03` “把客厅摄像机夜视设为关闭” → `{"mode":"off"}` |
| `get_state` | 无参数 | `CMD-sim87_living_camera_001-05-01` “查询客厅摄像机的状态” → `{}` |

负例 `NEG-sim87_living_camera_001`：“让客厅摄像机识别陌生人的身份证号码”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_camera_001-001` “客厅摄像机开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_camera_001-002` “客厅摄像机关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_camera_001-003` “看一下客厅摄像机现在什么状态” → `get_state` `{}`

### 16. 客厅电子温湿度计

- 设备名字：小米电子温湿度计
- 唯一 ID：`sim87_living_thermo_001`
- 所在房间：客厅
- 设备类型：温湿度传感器
- 唯一语音称呼：客厅电子温湿度计
- 所属分组：`simgrp_all_thermo`、`simgrp_living_thermo`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_temperature` | 无参数 | `CMD-sim87_living_thermo_001-01-01` “查询客厅电子温湿度计的温度” → `{}` |
| `get_humidity` | 无参数 | `CMD-sim87_living_thermo_001-02-01` “查询客厅电子温湿度计的湿度” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_living_thermo_001-03-01` “查询客厅电子温湿度计的状态” → `{}` |

负例 `NEG-sim87_living_thermo_001`：“把客厅电子温湿度计温度设为26度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_thermo_001-001` “看看客厅电子温湿度计现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_thermo_001-002` “客厅电子温湿度计状态查一下” → `get_state` `{}`

### 17. 客厅温湿度传感器2

- 设备名字：温湿度传感器2
- 唯一 ID：`sim87_living_thermo_002`
- 所在房间：客厅
- 设备类型：温湿度传感器
- 唯一语音称呼：客厅温湿度传感器2
- 所属分组：`simgrp_all_thermo`、`simgrp_living_thermo`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_temperature` | 无参数 | `CMD-sim87_living_thermo_002-01-01` “查询客厅温湿度传感器2的温度” → `{}` |
| `get_humidity` | 无参数 | `CMD-sim87_living_thermo_002-02-01` “查询客厅温湿度传感器2的湿度” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_living_thermo_002-03-01` “查询客厅温湿度传感器2的状态” → `{}` |

负例 `NEG-sim87_living_thermo_002`：“把客厅温湿度传感器2温度设为26度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_thermo_002-001` “看看客厅温湿度传感器2现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_thermo_002-002` “客厅温湿度传感器2状态查一下” → `get_state` `{}`

### 18. 客厅电视遥控开关

- 设备名字：电视开关
- 唯一 ID：`sim87_living_remote_001`
- 所在房间：客厅
- 设备类型：遥控器/无线开关
- 唯一语音称呼：客厅电视遥控开关
- 所属分组：`simgrp_all_remote`、`simgrp_living_remote`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_state` | 无参数 | `CMD-sim87_living_remote_001-01-01` “查询客厅电视遥控开关的状态” → `{}` |

负例 `NEG-sim87_living_remote_001`：“让客厅电视遥控开关模拟单击”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_remote_001-001` “看看客厅电视遥控开关现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_remote_001-002` “客厅电视遥控开关状态查一下” → `get_state` `{}`

### 19. 客厅路由器

- 设备名字：Want3_2.4G
- 唯一 ID：`sim87_living_router_001`
- 所在房间：客厅
- 设备类型：路由器
- 唯一语音称呼：客厅路由器
- 所属分组：`simgrp_all_router`、`simgrp_living_router`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_clients` | 无参数 | `CMD-sim87_living_router_001-01-01` “查询客厅路由器的已连接设备” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_living_router_001-02-01` “查询客厅路由器的状态” → `{}` |

负例 `NEG-sim87_living_router_001`：“修改客厅路由器的WiFi密码”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_router_001-001` “看看客厅路由器现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_router_001-002` “客厅路由器状态查一下” → `get_state` `{}`

### 20. 客厅多模网关

- 设备名字：小米智能多模网关2
- 唯一 ID：`sim87_living_gateway_001`
- 所在房间：客厅
- 设备类型：网关
- 唯一语音称呼：客厅多模网关
- 所属分组：`simgrp_all_gateway`、`simgrp_living_gateway`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_children` | 无参数 | `CMD-sim87_living_gateway_001-01-01` “查询客厅多模网关的子设备” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_living_gateway_001-02-01` “查询客厅多模网关的状态” → `{}` |

负例 `NEG-sim87_living_gateway_001`：“让客厅多模网关恢复出厂设置”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_gateway_001-001` “看看客厅多模网关现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_gateway_001-002` “客厅多模网关状态查一下” → `get_state` `{}`

### 21. 客厅易来网关

- 设备名字：易来网关
- 唯一 ID：`sim87_living_gateway_002`
- 所在房间：客厅
- 设备类型：网关
- 唯一语音称呼：客厅易来网关
- 所属分组：`simgrp_all_gateway`、`simgrp_living_gateway`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_children` | 无参数 | `CMD-sim87_living_gateway_002-01-01` “查询客厅易来网关的子设备” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_living_gateway_002-02-01` “查询客厅易来网关的状态” → `{}` |

负例 `NEG-sim87_living_gateway_002`：“让客厅易来网关恢复出厂设置”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_gateway_002-001` “看看客厅易来网关现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_gateway_002-002` “客厅易来网关状态查一下” → `get_state` `{}`

### 22. 客厅中枢网关

- 设备名字：Xiaomi 中枢网关
- 唯一 ID：`sim87_living_gateway_003`
- 所在房间：客厅
- 设备类型：网关
- 唯一语音称呼：客厅中枢网关
- 所属分组：`simgrp_all_gateway`、`simgrp_living_gateway`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_children` | 无参数 | `CMD-sim87_living_gateway_003-01-01` “查询客厅中枢网关的子设备” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_living_gateway_003-02-01` “查询客厅中枢网关的状态” → `{}` |

负例 `NEG-sim87_living_gateway_003`：“让客厅中枢网关恢复出厂设置”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_gateway_003-001` “看看客厅中枢网关现在什么状态” → `get_state` `{}`
- `VAR-sim87_living_gateway_003-002` “客厅中枢网关状态查一下” → `get_state` `{}`

### 23. 客厅香薰机

- 设备名字：米家智能调香机 3
- 唯一 ID：`sim87_living_aroma_001`
- 所在房间：客厅
- 设备类型：香薰机
- 唯一语音称呼：客厅香薰机
- 所属分组：`simgrp_all_aroma`、`simgrp_living_aroma`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_aroma_001-01-01` “打开客厅香薰机” → `{"on":true}`<br>`CMD-sim87_living_aroma_001-01-02` “关闭客厅香薰机” → `{"on":false}` |
| `set_intensity` | level: integer，1..3，步长 1；香氛强度档位 | `CMD-sim87_living_aroma_001-02-01` “把客厅香薰机香氛强度设为1档” → `{"level":1}`<br>`CMD-sim87_living_aroma_001-02-02` “把客厅香薰机香氛强度设为2档” → `{"level":2}`<br>`CMD-sim87_living_aroma_001-02-03` “把客厅香薰机香氛强度设为3档” → `{"level":3}` |
| `set_duration` | minutes: integer，1..120，步长 1；分钟 | `CMD-sim87_living_aroma_001-03-01` “让客厅香薰机运行1分钟” → `{"minutes":1}`<br>`CMD-sim87_living_aroma_001-03-02` “让客厅香薰机运行30分钟” → `{"minutes":30}`<br>`CMD-sim87_living_aroma_001-03-03` “让客厅香薰机运行120分钟” → `{"minutes":120}` |
| `get_state` | 无参数 | `CMD-sim87_living_aroma_001-04-01` “查询客厅香薰机的状态” → `{}` |

负例 `NEG-sim87_living_aroma_001`：“把客厅香薰机香氛强度设为10档”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_aroma_001-001` “客厅香薰机开一下” → `set_power` `{"on":true}`
- `VAR-sim87_living_aroma_001-002` “客厅香薰机关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_aroma_001-003` “看一下客厅香薰机现在什么状态” → `get_state` `{}`

### 24. 客厅黑色风扇

- 设备名字：黑色电风扇
- 唯一 ID：`sim87_living_fan_001`
- 所在房间：客厅
- 设备类型：风扇
- 唯一语音称呼：客厅黑色风扇
- 所属分组：`simgrp_all_fan`、`simgrp_living_fan`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_fan_001-01-01` “打开客厅黑色风扇” → `{"on":true}`<br>`CMD-sim87_living_fan_001-01-02` “关闭客厅黑色风扇” → `{"on":false}` |
| `set_speed` | level: integer，1..5，步长 1；档位 | `CMD-sim87_living_fan_001-02-01` “把客厅黑色风扇风速设为1档” → `{"level":1}`<br>`CMD-sim87_living_fan_001-02-02` “把客厅黑色风扇风速设为2档” → `{"level":2}`<br>`CMD-sim87_living_fan_001-02-03` “把客厅黑色风扇风速设为3档” → `{"level":3}`<br>`CMD-sim87_living_fan_001-02-04` “把客厅黑色风扇风速设为4档” → `{"level":4}`<br>`CMD-sim87_living_fan_001-02-05` “把客厅黑色风扇风速设为5档” → `{"level":5}` |
| `set_mode` | mode: enum {normal, natural, sleep} | `CMD-sim87_living_fan_001-03-01` “把客厅黑色风扇切换到标准风” → `{"mode":"normal"}`<br>`CMD-sim87_living_fan_001-03-02` “把客厅黑色风扇切换到自然风” → `{"mode":"natural"}`<br>`CMD-sim87_living_fan_001-03-03` “把客厅黑色风扇切换到睡眠风” → `{"mode":"sleep"}` |
| `set_oscillation` | enabled: boolean | `CMD-sim87_living_fan_001-04-01` “让客厅黑色风扇开始摇头” → `{"enabled":true}`<br>`CMD-sim87_living_fan_001-04-02` “让客厅黑色风扇停止摇头” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_fan_001-05-01` “查询客厅黑色风扇的状态” → `{}` |

负例 `NEG-sim87_living_fan_001`：“把客厅黑色风扇切换为制冷模式”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_fan_001-001` “客厅黑色风扇打开” → `set_power` `{"on":true}`
- `VAR-sim87_living_fan_001-002` “客厅黑色风扇关掉” → `set_power` `{"on":false}`
- `VAR-sim87_living_fan_001-003` “客厅黑色风扇风速三档” → `set_speed` `{"level":3}`
- `VAR-sim87_living_fan_001-004` “客厅黑色风扇不要摇头了” → `set_oscillation` `{"enabled":false}`

### 25. 客厅音频连接器

- 设备名字：Xiaomi 无线音频连接器
- 唯一 ID：`sim87_living_audio_001`
- 所在房间：客厅
- 设备类型：影音配件/音频连接器
- 唯一语音称呼：客厅音频连接器
- 所属分组：`simgrp_all_audio`、`simgrp_living_audio`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_living_audio_001-01-01` “打开客厅音频连接器” → `{"on":true}`<br>`CMD-sim87_living_audio_001-01-02` “关闭客厅音频连接器” → `{"on":false}` |
| `pause` | 无参数 | `CMD-sim87_living_audio_001-02-01` “暂停客厅音频连接器播放” → `{}` |
| `resume` | 无参数 | `CMD-sim87_living_audio_001-03-01` “继续客厅音频连接器播放” → `{}` |
| `next_track` | 无参数 | `CMD-sim87_living_audio_001-04-01` “让客厅音频连接器播放下一首” → `{}` |
| `previous_track` | 无参数 | `CMD-sim87_living_audio_001-05-01` “让客厅音频连接器播放上一首” → `{}` |
| `set_volume` | percent: integer，0..100，步长 1；百分比；0 静音，不是断电 | `CMD-sim87_living_audio_001-06-01` “把客厅音频连接器音量设为0%” → `{"percent":0}`<br>`CMD-sim87_living_audio_001-06-02` “把客厅音频连接器音量设为40%” → `{"percent":40}`<br>`CMD-sim87_living_audio_001-06-03` “把客厅音频连接器音量设为100%” → `{"percent":100}` |
| `set_mute` | muted: boolean | `CMD-sim87_living_audio_001-07-01` “让客厅音频连接器静音” → `{"muted":true}`<br>`CMD-sim87_living_audio_001-07-02` “取消客厅音频连接器静音” → `{"muted":false}` |
| `get_state` | 无参数 | `CMD-sim87_living_audio_001-08-01` “查询客厅音频连接器的状态” → `{}` |

负例 `NEG-sim87_living_audio_001`：“让客厅音频连接器制冷”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_living_audio_001-001` “客厅音频连接器音量一半” → `set_volume` `{"percent":50}`
- `VAR-sim87_living_audio_001-002` “客厅音频连接器别出声了” → `set_mute` `{"muted":true}`
- `VAR-sim87_living_audio_001-003` “客厅音频连接器下一首” → `next_track` `{}`
- `VAR-sim87_living_audio_001-004` “客厅音频连接器继续播” → `resume` `{}`

### 26. 主卧空调

- 设备名字：米家新风空调（尊享…）
- 唯一 ID：`sim87_master_ac_001`
- 所在房间：主卧
- 设备类型：空调
- 唯一语音称呼：主卧空调
- 所属分组：`simgrp_all_ac`、`simgrp_master_ac`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_ac_001-01-01` “打开主卧空调” → `{"on":true}`<br>`CMD-sim87_master_ac_001-01-02` “关闭主卧空调” → `{"on":false}` |
| `set_mode` | mode: enum {cool, heat, dry, fan, auto} | `CMD-sim87_master_ac_001-02-01` “把主卧空调切换到制冷” → `{"mode":"cool"}`<br>`CMD-sim87_master_ac_001-02-02` “把主卧空调切换到制热” → `{"mode":"heat"}`<br>`CMD-sim87_master_ac_001-02-03` “把主卧空调切换到除湿” → `{"mode":"dry"}`<br>`CMD-sim87_master_ac_001-02-04` “把主卧空调切换到送风” → `{"mode":"fan"}`<br>`CMD-sim87_master_ac_001-02-05` “把主卧空调切换到自动模式” → `{"mode":"auto"}` |
| `set_temperature` | celsius: integer，16..30，步长 1；摄氏度 | `CMD-sim87_master_ac_001-03-01` “把主卧空调温度设为16度” → `{"celsius":16}`<br>`CMD-sim87_master_ac_001-03-02` “把主卧空调温度设为26度” → `{"celsius":26}`<br>`CMD-sim87_master_ac_001-03-03` “把主卧空调温度设为30度” → `{"celsius":30}` |
| `set_fan_speed` | level: enum {auto, low, medium, high} | `CMD-sim87_master_ac_001-04-01` “把主卧空调风速设为自动” → `{"level":"auto"}`<br>`CMD-sim87_master_ac_001-04-02` “把主卧空调风速设为低档” → `{"level":"low"}`<br>`CMD-sim87_master_ac_001-04-03` “把主卧空调风速设为中档” → `{"level":"medium"}`<br>`CMD-sim87_master_ac_001-04-04` “把主卧空调风速设为高档” → `{"level":"high"}` |
| `set_fresh_air` | enabled: boolean | `CMD-sim87_master_ac_001-05-01` “打开主卧空调的新风” → `{"enabled":true}`<br>`CMD-sim87_master_ac_001-05-02` “关闭主卧空调的新风” → `{"enabled":false}` |
| `set_fresh_air_level` | level: integer，1..3，步长 1；新风档位 | `CMD-sim87_master_ac_001-06-01` “把主卧空调新风设为1档” → `{"level":1}`<br>`CMD-sim87_master_ac_001-06-02` “把主卧空调新风设为2档” → `{"level":2}`<br>`CMD-sim87_master_ac_001-06-03` “把主卧空调新风设为3档” → `{"level":3}` |
| `set_vertical_swing` | enabled: boolean | `CMD-sim87_master_ac_001-07-01` “打开主卧空调的上下扫风” → `{"enabled":true}`<br>`CMD-sim87_master_ac_001-07-02` “关闭主卧空调的上下扫风” → `{"enabled":false}` |
| `set_horizontal_swing` | enabled: boolean | `CMD-sim87_master_ac_001-08-01` “打开主卧空调的左右扫风” → `{"enabled":true}`<br>`CMD-sim87_master_ac_001-08-02` “关闭主卧空调的左右扫风” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_master_ac_001-09-01` “查询主卧空调的状态” → `{}` |

负例 `NEG-sim87_master_ac_001`：“把主卧空调温度设为40度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_ac_001-001` “把主卧空调温度设为26度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-002` “主卧空调温度26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-003` “主卧的空调设为26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-004` “主卧那台空调调到二十六度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-005` “主卧空调给我调26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-006` “主卧空调，二十六度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-007` “二十六度，主卧空调” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-008` “主卧空调温度改成26℃” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-009` “麻烦主卧空调降到26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-010` “主卧空调温控拨到26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_master_ac_001-011` “主卧空调开制冷” → `set_mode` `{"mode":"cool"}`
- `VAR-sim87_master_ac_001-012` “主卧的空调新风打开” → `set_fresh_air` `{"enabled":true}`
- `VAR-sim87_master_ac_001-013` “主卧空调除湿一下” → `set_mode` `{"mode":"dry"}`

### 27. 主卧显示器挂灯

- 设备名字：米家显示器挂灯2
- 唯一 ID：`sim87_master_light_001`
- 所在房间：主卧
- 设备类型：灯
- 唯一语音称呼：主卧显示器挂灯
- 所属分组：`simgrp_all_light`、`simgrp_master_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_light_001-01-01` “打开主卧显示器挂灯” → `{"on":true}`<br>`CMD-sim87_master_light_001-01-02` “关闭主卧显示器挂灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_master_light_001-02-01` “把主卧显示器挂灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_master_light_001-02-02` “把主卧显示器挂灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_master_light_001-02-03` “把主卧显示器挂灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_master_light_001-03-01` “把主卧显示器挂灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_master_light_001-03-02` “把主卧显示器挂灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_master_light_001-03-03` “把主卧显示器挂灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_master_light_001-04-01` “把主卧显示器挂灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_master_light_001-04-02` “把主卧显示器挂灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_master_light_001-04-03` “把主卧显示器挂灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_master_light_001-04-04` “把主卧显示器挂灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_master_light_001-04-05` “把主卧显示器挂灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_master_light_001-04-06` “把主卧显示器挂灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_master_light_001-04-07` “把主卧显示器挂灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_master_light_001-04-08` “把主卧显示器挂灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_master_light_001-05-01` “把主卧显示器挂灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_master_light_001-05-02` “把主卧显示器挂灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_master_light_001-05-03` “把主卧显示器挂灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_master_light_001-06-01` “查询主卧显示器挂灯的状态” → `{}` |

负例 `NEG-sim87_master_light_001`：“打开主卧显示器挂灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_light_001-001` “开主卧挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-002` “开主卧的挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-003` “打开主卧的挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-004` “把主卧挂灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-005` “点亮主卧的挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-006` “麻烦开一下主卧挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-007` “主卧挂灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-008` “关掉主卧的挂灯” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-009` “主卧的挂灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-010` “把主卧挂灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-011` “开主卧显示器灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-012` “开主卧的显示器灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-013` “打开主卧的显示器灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-014` “把主卧显示器灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-015` “点亮主卧的显示器灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-016` “麻烦开一下主卧显示器灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-017` “主卧显示器灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-018` “关掉主卧的显示器灯” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-019` “主卧的显示器灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-020` “把主卧显示器灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-021` “开主卧屏幕挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-022` “开主卧的屏幕挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-023` “打开主卧的屏幕挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-024` “把主卧屏幕挂灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-025` “点亮主卧的屏幕挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-026` “麻烦开一下主卧屏幕挂灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-027` “主卧屏幕挂灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-028` “关掉主卧的屏幕挂灯” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-029` “主卧的屏幕挂灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-030` “把主卧屏幕挂灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-031` “主卧显示器挂灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_001-032` “把主卧显示器挂灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_001-033` “主卧显示器挂灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_master_light_001-034` “主卧显示器挂灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_master_light_001-035` “主卧显示器挂灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_master_light_001-036` “主卧显示器挂灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_master_light_001-037` “主卧显示器挂灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_master_light_001-038` “主卧显示器挂灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 28. 主卧吸顶灯

- 设备名字：主卧吸顶灯
- 唯一 ID：`sim87_master_light_002`
- 所在房间：主卧
- 设备类型：灯
- 唯一语音称呼：主卧吸顶灯
- 所属分组：`simgrp_all_light`、`simgrp_master_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_light_002-01-01` “打开主卧吸顶灯” → `{"on":true}`<br>`CMD-sim87_master_light_002-01-02` “关闭主卧吸顶灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_master_light_002-02-01` “把主卧吸顶灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_master_light_002-02-02` “把主卧吸顶灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_master_light_002-02-03` “把主卧吸顶灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_master_light_002-03-01` “把主卧吸顶灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_master_light_002-03-02` “把主卧吸顶灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_master_light_002-03-03` “把主卧吸顶灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_master_light_002-04-01` “把主卧吸顶灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_master_light_002-04-02` “把主卧吸顶灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_master_light_002-04-03` “把主卧吸顶灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_master_light_002-04-04` “把主卧吸顶灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_master_light_002-04-05` “把主卧吸顶灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_master_light_002-04-06` “把主卧吸顶灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_master_light_002-04-07` “把主卧吸顶灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_master_light_002-04-08` “把主卧吸顶灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_master_light_002-05-01` “把主卧吸顶灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_master_light_002-05-02` “把主卧吸顶灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_master_light_002-05-03` “把主卧吸顶灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_master_light_002-06-01` “查询主卧吸顶灯的状态” → `{}` |

负例 `NEG-sim87_master_light_002`：“打开主卧吸顶灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_light_002-001` “开主卧吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-002` “开主卧的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-003` “打开主卧的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-004` “把主卧吸顶灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-005` “点亮主卧的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-006` “麻烦开一下主卧吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-007` “主卧吸顶灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-008` “关掉主卧的吸顶灯” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-009` “主卧的吸顶灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-010` “把主卧吸顶灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-011` “开主卧顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-012` “开主卧的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-013` “打开主卧的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-014` “把主卧顶灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-015` “点亮主卧的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-016` “麻烦开一下主卧顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-017` “主卧顶灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-018` “关掉主卧的顶灯” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-019` “主卧的顶灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-020` “把主卧顶灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-021` “开主卧天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-022` “开主卧的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-023` “打开主卧的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-024` “把主卧天花板灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-025` “点亮主卧的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-026` “麻烦开一下主卧天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-027` “主卧天花板灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-028` “关掉主卧的天花板灯” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-029` “主卧的天花板灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-030` “把主卧天花板灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-031` “主卧吸顶灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_master_light_002-032` “把主卧吸顶灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_master_light_002-033` “主卧吸顶灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_master_light_002-034` “主卧吸顶灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_master_light_002-035` “主卧吸顶灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_master_light_002-036` “主卧吸顶灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_master_light_002-037` “主卧吸顶灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_master_light_002-038` “主卧吸顶灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 29. 主卧双键开关

- 设备名字：小米智能开关2（双开）
- 唯一 ID：`sim87_master_switch2_001`
- 所在房间：主卧
- 设备类型：开关
- 唯一语音称呼：主卧双键开关
- 所属分组：`simgrp_all_switch`、`simgrp_master_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, ch2, all}；on: boolean | `CMD-sim87_master_switch2_001-01-01` “打开主卧双键开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_master_switch2_001-01-02` “关闭主卧双键开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_master_switch2_001-01-03` “打开主卧双键开关第2路” → `{"channel":"ch2","on":true}`<br>`CMD-sim87_master_switch2_001-01-04` “关闭主卧双键开关第2路” → `{"channel":"ch2","on":false}`<br>`CMD-sim87_master_switch2_001-01-05` “打开主卧双键开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_master_switch2_001-01-06` “关闭主卧双键开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_master_switch2_001-02-01` “查询主卧双键开关的状态” → `{}` |

负例 `NEG-sim87_master_switch2_001`：“把主卧双键开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_switch2_001-001` “主卧双键开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_master_switch2_001-002` “主卧双键开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_master_switch2_001-003` “主卧双键开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_master_switch2_001-004` “主卧双键开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 30. 主卧净化加湿器

- 设备名字：米家净化加湿器3 Pro
- 唯一 ID：`sim87_master_humidifier_001`
- 所在房间：主卧
- 设备类型：加湿器
- 唯一语音称呼：主卧净化加湿器
- 所属分组：`simgrp_all_humidifier`、`simgrp_master_humidifier`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_humidifier_001-01-01` “打开主卧净化加湿器” → `{"on":true}`<br>`CMD-sim87_master_humidifier_001-01-02` “关闭主卧净化加湿器” → `{"on":false}` |
| `set_mode` | mode: enum {auto, sleep, manual} | `CMD-sim87_master_humidifier_001-02-01` “把主卧净化加湿器切换为自动模式” → `{"mode":"auto"}`<br>`CMD-sim87_master_humidifier_001-02-02` “把主卧净化加湿器切换为睡眠模式” → `{"mode":"sleep"}`<br>`CMD-sim87_master_humidifier_001-02-03` “把主卧净化加湿器切换为手动模式” → `{"mode":"manual"}` |
| `set_target_humidity` | percent: integer，30..80，步长 1；相对湿度百分比 | `CMD-sim87_master_humidifier_001-03-01` “把主卧净化加湿器目标湿度设为30%” → `{"percent":30}`<br>`CMD-sim87_master_humidifier_001-03-02` “把主卧净化加湿器目标湿度设为50%” → `{"percent":50}`<br>`CMD-sim87_master_humidifier_001-03-03` “把主卧净化加湿器目标湿度设为80%” → `{"percent":80}` |
| `set_mist_level` | level: integer，1..3，步长 1；加湿档位；无雾型号也统一用此模拟字段 | `CMD-sim87_master_humidifier_001-04-01` “把主卧净化加湿器加湿档位设为1档” → `{"level":1}`<br>`CMD-sim87_master_humidifier_001-04-02` “把主卧净化加湿器加湿档位设为2档” → `{"level":2}`<br>`CMD-sim87_master_humidifier_001-04-03` “把主卧净化加湿器加湿档位设为3档” → `{"level":3}` |
| `get_state` | 无参数 | `CMD-sim87_master_humidifier_001-05-01` “查询主卧净化加湿器的状态” → `{}` |

负例 `NEG-sim87_master_humidifier_001`：“把主卧净化加湿器湿度设为120%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_humidifier_001-001` “主卧净化加湿器开一下” → `set_power` `{"on":true}`
- `VAR-sim87_master_humidifier_001-002` “主卧净化加湿器关掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_humidifier_001-003` “看一下主卧净化加湿器现在什么状态” → `get_state` `{}`

### 31. 主卧无雾加湿器

- 设备名字：米家无雾加湿器3 600…
- 唯一 ID：`sim87_master_humidifier_002`
- 所在房间：主卧
- 设备类型：加湿器
- 唯一语音称呼：主卧无雾加湿器
- 所属分组：`simgrp_all_humidifier`、`simgrp_master_humidifier`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_humidifier_002-01-01` “打开主卧无雾加湿器” → `{"on":true}`<br>`CMD-sim87_master_humidifier_002-01-02` “关闭主卧无雾加湿器” → `{"on":false}` |
| `set_mode` | mode: enum {auto, sleep, manual} | `CMD-sim87_master_humidifier_002-02-01` “把主卧无雾加湿器切换为自动模式” → `{"mode":"auto"}`<br>`CMD-sim87_master_humidifier_002-02-02` “把主卧无雾加湿器切换为睡眠模式” → `{"mode":"sleep"}`<br>`CMD-sim87_master_humidifier_002-02-03` “把主卧无雾加湿器切换为手动模式” → `{"mode":"manual"}` |
| `set_target_humidity` | percent: integer，30..80，步长 1；相对湿度百分比 | `CMD-sim87_master_humidifier_002-03-01` “把主卧无雾加湿器目标湿度设为30%” → `{"percent":30}`<br>`CMD-sim87_master_humidifier_002-03-02` “把主卧无雾加湿器目标湿度设为50%” → `{"percent":50}`<br>`CMD-sim87_master_humidifier_002-03-03` “把主卧无雾加湿器目标湿度设为80%” → `{"percent":80}` |
| `set_mist_level` | level: integer，1..3，步长 1；加湿档位；无雾型号也统一用此模拟字段 | `CMD-sim87_master_humidifier_002-04-01` “把主卧无雾加湿器加湿档位设为1档” → `{"level":1}`<br>`CMD-sim87_master_humidifier_002-04-02` “把主卧无雾加湿器加湿档位设为2档” → `{"level":2}`<br>`CMD-sim87_master_humidifier_002-04-03` “把主卧无雾加湿器加湿档位设为3档” → `{"level":3}` |
| `get_state` | 无参数 | `CMD-sim87_master_humidifier_002-05-01` “查询主卧无雾加湿器的状态” → `{}` |

负例 `NEG-sim87_master_humidifier_002`：“把主卧无雾加湿器湿度设为120%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_humidifier_002-001` “主卧无雾加湿器开一下” → `set_power` `{"on":true}`
- `VAR-sim87_master_humidifier_002-002` “主卧无雾加湿器关掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_humidifier_002-003` “看一下主卧无雾加湿器现在什么状态” → `get_state` `{}`

### 32. 主卧人体传感器

- 设备名字：人体传感器2
- 唯一 ID：`sim87_master_motion_001`
- 所在房间：主卧
- 设备类型：人体传感器
- 唯一语音称呼：主卧人体传感器
- 所属分组：`simgrp_all_motion`、`simgrp_master_motion`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_motion` | 无参数 | `CMD-sim87_master_motion_001-01-01` “主卧人体传感器检测到移动了吗” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_master_motion_001-02-01` “查询主卧人体传感器的状态” → `{}` |

负例 `NEG-sim87_master_motion_001`：“关闭主卧人体传感器的检测功能”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_motion_001-001` “看看主卧人体传感器现在什么状态” → `get_state` `{}`
- `VAR-sim87_master_motion_001-002` “主卧人体传感器状态查一下” → `get_state` `{}`

### 33. 主卧压力传感器

- 设备名字：领普压力有无传感器
- 唯一 ID：`sim87_master_pressure_001`
- 所在房间：主卧
- 设备类型：压力传感器
- 唯一语音称呼：主卧压力传感器
- 所属分组：`simgrp_all_pressure`、`simgrp_master_pressure`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_pressed` | 无参数 | `CMD-sim87_master_pressure_001-01-01` “主卧压力传感器感应到压力了吗” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_master_pressure_001-02-01` “查询主卧压力传感器的状态” → `{}` |

负例 `NEG-sim87_master_pressure_001`：“关闭主卧压力传感器的检测功能”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_pressure_001-001` “看看主卧压力传感器现在什么状态” → `get_state` `{}`
- `VAR-sim87_master_pressure_001-002` “主卧压力传感器状态查一下” → `get_state` `{}`

### 34. 主卧挂灯遥控开关

- 设备名字：挂灯
- 唯一 ID：`sim87_master_remote_001`
- 所在房间：主卧
- 设备类型：遥控器/无线开关
- 唯一语音称呼：主卧挂灯遥控开关
- 所属分组：`simgrp_all_remote`、`simgrp_master_remote`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_state` | 无参数 | `CMD-sim87_master_remote_001-01-01` “查询主卧挂灯遥控开关的状态” → `{}` |

负例 `NEG-sim87_master_remote_001`：“让主卧挂灯遥控开关模拟单击”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_remote_001-001` “看看主卧挂灯遥控开关现在什么状态” → `get_state` `{}`
- `VAR-sim87_master_remote_001-002` “主卧挂灯遥控开关状态查一下” → `get_state` `{}`

### 35. 主卧AI音箱

- 设备名字：小米AI音箱
- 唯一 ID：`sim87_master_speaker_001`
- 所在房间：主卧
- 设备类型：音箱
- 唯一语音称呼：主卧AI音箱
- 所属分组：`simgrp_all_speaker`、`simgrp_master_speaker`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_speaker_001-01-01` “打开主卧AI音箱” → `{"on":true}`<br>`CMD-sim87_master_speaker_001-01-02` “关闭主卧AI音箱” → `{"on":false}` |
| `pause` | 无参数 | `CMD-sim87_master_speaker_001-02-01` “暂停主卧AI音箱播放” → `{}` |
| `resume` | 无参数 | `CMD-sim87_master_speaker_001-03-01` “继续主卧AI音箱播放” → `{}` |
| `next_track` | 无参数 | `CMD-sim87_master_speaker_001-04-01` “让主卧AI音箱播放下一首” → `{}` |
| `previous_track` | 无参数 | `CMD-sim87_master_speaker_001-05-01` “让主卧AI音箱播放上一首” → `{}` |
| `set_volume` | percent: integer，0..100，步长 1；百分比；0 静音，不是断电 | `CMD-sim87_master_speaker_001-06-01` “把主卧AI音箱音量设为0%” → `{"percent":0}`<br>`CMD-sim87_master_speaker_001-06-02` “把主卧AI音箱音量设为40%” → `{"percent":40}`<br>`CMD-sim87_master_speaker_001-06-03` “把主卧AI音箱音量设为100%” → `{"percent":100}` |
| `set_mute` | muted: boolean | `CMD-sim87_master_speaker_001-07-01` “让主卧AI音箱静音” → `{"muted":true}`<br>`CMD-sim87_master_speaker_001-07-02` “取消主卧AI音箱静音” → `{"muted":false}` |
| `get_state` | 无参数 | `CMD-sim87_master_speaker_001-08-01` “查询主卧AI音箱的状态” → `{}` |

负例 `NEG-sim87_master_speaker_001`：“让主卧AI音箱购买一首歌”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_speaker_001-001` “主卧AI音箱音量一半” → `set_volume` `{"percent":50}`
- `VAR-sim87_master_speaker_001-002` “主卧AI音箱别出声了” → `set_mute` `{"muted":true}`
- `VAR-sim87_master_speaker_001-003` “主卧AI音箱下一首” → `next_track` `{}`
- `VAR-sim87_master_speaker_001-004` “主卧AI音箱继续播” → `resume` `{}`

### 36. 主卧触屏音箱

- 设备名字：小爱音箱触屏版
- 唯一 ID：`sim87_master_speaker_002`
- 所在房间：主卧
- 设备类型：音箱
- 唯一语音称呼：主卧触屏音箱
- 所属分组：`simgrp_all_speaker`、`simgrp_master_speaker`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_speaker_002-01-01` “打开主卧触屏音箱” → `{"on":true}`<br>`CMD-sim87_master_speaker_002-01-02` “关闭主卧触屏音箱” → `{"on":false}` |
| `pause` | 无参数 | `CMD-sim87_master_speaker_002-02-01` “暂停主卧触屏音箱播放” → `{}` |
| `resume` | 无参数 | `CMD-sim87_master_speaker_002-03-01` “继续主卧触屏音箱播放” → `{}` |
| `next_track` | 无参数 | `CMD-sim87_master_speaker_002-04-01` “让主卧触屏音箱播放下一首” → `{}` |
| `previous_track` | 无参数 | `CMD-sim87_master_speaker_002-05-01` “让主卧触屏音箱播放上一首” → `{}` |
| `set_volume` | percent: integer，0..100，步长 1；百分比；0 静音，不是断电 | `CMD-sim87_master_speaker_002-06-01` “把主卧触屏音箱音量设为0%” → `{"percent":0}`<br>`CMD-sim87_master_speaker_002-06-02` “把主卧触屏音箱音量设为40%” → `{"percent":40}`<br>`CMD-sim87_master_speaker_002-06-03` “把主卧触屏音箱音量设为100%” → `{"percent":100}` |
| `set_mute` | muted: boolean | `CMD-sim87_master_speaker_002-07-01` “让主卧触屏音箱静音” → `{"muted":true}`<br>`CMD-sim87_master_speaker_002-07-02` “取消主卧触屏音箱静音” → `{"muted":false}` |
| `get_state` | 无参数 | `CMD-sim87_master_speaker_002-08-01` “查询主卧触屏音箱的状态” → `{}` |

负例 `NEG-sim87_master_speaker_002`：“让主卧触屏音箱购买一首歌”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_speaker_002-001` “主卧触屏音箱音量一半” → `set_volume` `{"percent":50}`
- `VAR-sim87_master_speaker_002-002` “主卧触屏音箱别出声了” → `set_mute` `{"muted":true}`
- `VAR-sim87_master_speaker_002-003` “主卧触屏音箱下一首” → `next_track` `{}`
- `VAR-sim87_master_speaker_002-004` “主卧触屏音箱继续播” → `resume` `{}`

### 37. 主卧窗帘

- 设备名字：窗帘
- 唯一 ID：`sim87_master_curtain_001`
- 所在房间：主卧
- 设备类型：窗帘
- 唯一语音称呼：主卧窗帘
- 所属分组：`simgrp_all_curtain`、`simgrp_master_curtain`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `open` | 无参数 | `CMD-sim87_master_curtain_001-01-01` “打开主卧窗帘” → `{}` |
| `close` | 无参数 | `CMD-sim87_master_curtain_001-02-01` “关闭主卧窗帘” → `{}` |
| `stop` | 无参数 | `CMD-sim87_master_curtain_001-03-01` “停止主卧窗帘移动” → `{}` |
| `set_position` | open_percent: integer，0..100，步长 1；打开百分比，0 全关，100 全开 | `CMD-sim87_master_curtain_001-04-01` “把主卧窗帘打开到0%” → `{"open_percent":0}`<br>`CMD-sim87_master_curtain_001-04-02` “把主卧窗帘打开到50%” → `{"open_percent":50}`<br>`CMD-sim87_master_curtain_001-04-03` “把主卧窗帘打开到100%” → `{"open_percent":100}` |
| `get_state` | 无参数 | `CMD-sim87_master_curtain_001-05-01` “查询主卧窗帘的状态” → `{}` |

负例 `NEG-sim87_master_curtain_001`：“把主卧窗帘打开到120%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_curtain_001-001` “主卧窗帘拉开” → `open` `{}`
- `VAR-sim87_master_curtain_001-002` “主卧窗帘合上” → `close` `{}`
- `VAR-sim87_master_curtain_001-003` “主卧窗帘开一半” → `set_position` `{"open_percent":50}`
- `VAR-sim87_master_curtain_001-004` “主卧窗帘别动了” → `stop` `{}`

### 38. 主卧循环风扇

- 设备名字：米家智能直流变频循…
- 唯一 ID：`sim87_master_fan_001`
- 所在房间：主卧
- 设备类型：风扇
- 唯一语音称呼：主卧循环风扇
- 所属分组：`simgrp_all_fan`、`simgrp_master_fan`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_fan_001-01-01` “打开主卧循环风扇” → `{"on":true}`<br>`CMD-sim87_master_fan_001-01-02` “关闭主卧循环风扇” → `{"on":false}` |
| `set_speed` | level: integer，1..5，步长 1；档位 | `CMD-sim87_master_fan_001-02-01` “把主卧循环风扇风速设为1档” → `{"level":1}`<br>`CMD-sim87_master_fan_001-02-02` “把主卧循环风扇风速设为2档” → `{"level":2}`<br>`CMD-sim87_master_fan_001-02-03` “把主卧循环风扇风速设为3档” → `{"level":3}`<br>`CMD-sim87_master_fan_001-02-04` “把主卧循环风扇风速设为4档” → `{"level":4}`<br>`CMD-sim87_master_fan_001-02-05` “把主卧循环风扇风速设为5档” → `{"level":5}` |
| `set_mode` | mode: enum {normal, natural, sleep} | `CMD-sim87_master_fan_001-03-01` “把主卧循环风扇切换到标准风” → `{"mode":"normal"}`<br>`CMD-sim87_master_fan_001-03-02` “把主卧循环风扇切换到自然风” → `{"mode":"natural"}`<br>`CMD-sim87_master_fan_001-03-03` “把主卧循环风扇切换到睡眠风” → `{"mode":"sleep"}` |
| `set_oscillation` | enabled: boolean | `CMD-sim87_master_fan_001-04-01` “让主卧循环风扇开始摇头” → `{"enabled":true}`<br>`CMD-sim87_master_fan_001-04-02` “让主卧循环风扇停止摇头” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_master_fan_001-05-01` “查询主卧循环风扇的状态” → `{}` |

负例 `NEG-sim87_master_fan_001`：“把主卧循环风扇切换为制冷模式”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_fan_001-001` “主卧循环风扇打开” → `set_power` `{"on":true}`
- `VAR-sim87_master_fan_001-002` “主卧循环风扇关掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_fan_001-003` “主卧循环风扇风速三档” → `set_speed` `{"level":3}`
- `VAR-sim87_master_fan_001-004` “主卧循环风扇不要摇头了” → `set_oscillation` `{"enabled":false}`

### 39. 主卧水暖垫

- 设备名字：绘睡水暖垫HS2205
- 唯一 ID：`sim87_master_blanket_001`
- 所在房间：主卧
- 设备类型：电热毯/水暖垫
- 唯一语音称呼：主卧水暖垫
- 所属分组：`simgrp_all_blanket`、`simgrp_master_blanket`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_master_blanket_001-01-01` “打开主卧水暖垫” → `{"on":true}`<br>`CMD-sim87_master_blanket_001-01-02` “关闭主卧水暖垫” → `{"on":false}` |
| `set_temperature` | celsius: integer，25..45，步长 1；摄氏度 | `CMD-sim87_master_blanket_001-02-01` “把主卧水暖垫温度设为25度” → `{"celsius":25}`<br>`CMD-sim87_master_blanket_001-02-02` “把主卧水暖垫温度设为35度” → `{"celsius":35}`<br>`CMD-sim87_master_blanket_001-02-03` “把主卧水暖垫温度设为45度” → `{"celsius":45}` |
| `set_timer` | minutes: integer，1..480，步长 1；倒计时，到期关机 | `CMD-sim87_master_blanket_001-03-01` “让主卧水暖垫在1分钟后关闭” → `{"minutes":1}`<br>`CMD-sim87_master_blanket_001-03-02` “让主卧水暖垫在120分钟后关闭” → `{"minutes":120}`<br>`CMD-sim87_master_blanket_001-03-03` “让主卧水暖垫在480分钟后关闭” → `{"minutes":480}` |
| `get_state` | 无参数 | `CMD-sim87_master_blanket_001-04-01` “查询主卧水暖垫的状态” → `{}` |

负例 `NEG-sim87_master_blanket_001`：“把主卧水暖垫温度设为90度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_master_blanket_001-001` “主卧水暖垫开一下” → `set_power` `{"on":true}`
- `VAR-sim87_master_blanket_001-002` “主卧水暖垫关掉” → `set_power` `{"on":false}`
- `VAR-sim87_master_blanket_001-003` “看一下主卧水暖垫现在什么状态” → `get_state` `{}`

### 40. 次卧空调

- 设备名字：米家新风空调（尊享…）
- 唯一 ID：`sim87_secondary_ac_001`
- 所在房间：次卧
- 设备类型：空调
- 唯一语音称呼：次卧空调
- 所属分组：`simgrp_all_ac`、`simgrp_secondary_ac`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_secondary_ac_001-01-01` “打开次卧空调” → `{"on":true}`<br>`CMD-sim87_secondary_ac_001-01-02` “关闭次卧空调” → `{"on":false}` |
| `set_mode` | mode: enum {cool, heat, dry, fan, auto} | `CMD-sim87_secondary_ac_001-02-01` “把次卧空调切换到制冷” → `{"mode":"cool"}`<br>`CMD-sim87_secondary_ac_001-02-02` “把次卧空调切换到制热” → `{"mode":"heat"}`<br>`CMD-sim87_secondary_ac_001-02-03` “把次卧空调切换到除湿” → `{"mode":"dry"}`<br>`CMD-sim87_secondary_ac_001-02-04` “把次卧空调切换到送风” → `{"mode":"fan"}`<br>`CMD-sim87_secondary_ac_001-02-05` “把次卧空调切换到自动模式” → `{"mode":"auto"}` |
| `set_temperature` | celsius: integer，16..30，步长 1；摄氏度 | `CMD-sim87_secondary_ac_001-03-01` “把次卧空调温度设为16度” → `{"celsius":16}`<br>`CMD-sim87_secondary_ac_001-03-02` “把次卧空调温度设为26度” → `{"celsius":26}`<br>`CMD-sim87_secondary_ac_001-03-03` “把次卧空调温度设为30度” → `{"celsius":30}` |
| `set_fan_speed` | level: enum {auto, low, medium, high} | `CMD-sim87_secondary_ac_001-04-01` “把次卧空调风速设为自动” → `{"level":"auto"}`<br>`CMD-sim87_secondary_ac_001-04-02` “把次卧空调风速设为低档” → `{"level":"low"}`<br>`CMD-sim87_secondary_ac_001-04-03` “把次卧空调风速设为中档” → `{"level":"medium"}`<br>`CMD-sim87_secondary_ac_001-04-04` “把次卧空调风速设为高档” → `{"level":"high"}` |
| `set_fresh_air` | enabled: boolean | `CMD-sim87_secondary_ac_001-05-01` “打开次卧空调的新风” → `{"enabled":true}`<br>`CMD-sim87_secondary_ac_001-05-02` “关闭次卧空调的新风” → `{"enabled":false}` |
| `set_fresh_air_level` | level: integer，1..3，步长 1；新风档位 | `CMD-sim87_secondary_ac_001-06-01` “把次卧空调新风设为1档” → `{"level":1}`<br>`CMD-sim87_secondary_ac_001-06-02` “把次卧空调新风设为2档” → `{"level":2}`<br>`CMD-sim87_secondary_ac_001-06-03` “把次卧空调新风设为3档” → `{"level":3}` |
| `set_vertical_swing` | enabled: boolean | `CMD-sim87_secondary_ac_001-07-01` “打开次卧空调的上下扫风” → `{"enabled":true}`<br>`CMD-sim87_secondary_ac_001-07-02` “关闭次卧空调的上下扫风” → `{"enabled":false}` |
| `set_horizontal_swing` | enabled: boolean | `CMD-sim87_secondary_ac_001-08-01` “打开次卧空调的左右扫风” → `{"enabled":true}`<br>`CMD-sim87_secondary_ac_001-08-02` “关闭次卧空调的左右扫风” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_ac_001-09-01` “查询次卧空调的状态” → `{}` |

负例 `NEG-sim87_secondary_ac_001`：“把次卧空调温度设为40度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_ac_001-001` “把次卧空调温度设为26度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-002` “次卧空调温度26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-003` “次卧的空调设为26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-004` “次卧那台空调调到二十六度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-005` “次卧空调给我调26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-006` “次卧空调，二十六度” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-007` “二十六度，次卧空调” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-008` “次卧空调温度改成26℃” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-009` “麻烦次卧空调降到26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-010` “次卧空调温控拨到26” → `set_temperature` `{"celsius":26}`
- `VAR-sim87_secondary_ac_001-011` “次卧空调开制冷” → `set_mode` `{"mode":"cool"}`
- `VAR-sim87_secondary_ac_001-012` “次卧的空调新风打开” → `set_fresh_air` `{"enabled":true}`
- `VAR-sim87_secondary_ac_001-013` “次卧空调除湿一下” → `set_mode` `{"mode":"dry"}`

### 41. 次卧白色风扇

- 设备名字：白色电风扇
- 唯一 ID：`sim87_secondary_fan_001`
- 所在房间：次卧
- 设备类型：风扇
- 唯一语音称呼：次卧白色风扇
- 所属分组：`simgrp_all_fan`、`simgrp_secondary_fan`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_secondary_fan_001-01-01` “打开次卧白色风扇” → `{"on":true}`<br>`CMD-sim87_secondary_fan_001-01-02` “关闭次卧白色风扇” → `{"on":false}` |
| `set_speed` | level: integer，1..5，步长 1；档位 | `CMD-sim87_secondary_fan_001-02-01` “把次卧白色风扇风速设为1档” → `{"level":1}`<br>`CMD-sim87_secondary_fan_001-02-02` “把次卧白色风扇风速设为2档” → `{"level":2}`<br>`CMD-sim87_secondary_fan_001-02-03` “把次卧白色风扇风速设为3档” → `{"level":3}`<br>`CMD-sim87_secondary_fan_001-02-04` “把次卧白色风扇风速设为4档” → `{"level":4}`<br>`CMD-sim87_secondary_fan_001-02-05` “把次卧白色风扇风速设为5档” → `{"level":5}` |
| `set_mode` | mode: enum {normal, natural, sleep} | `CMD-sim87_secondary_fan_001-03-01` “把次卧白色风扇切换到标准风” → `{"mode":"normal"}`<br>`CMD-sim87_secondary_fan_001-03-02` “把次卧白色风扇切换到自然风” → `{"mode":"natural"}`<br>`CMD-sim87_secondary_fan_001-03-03` “把次卧白色风扇切换到睡眠风” → `{"mode":"sleep"}` |
| `set_oscillation` | enabled: boolean | `CMD-sim87_secondary_fan_001-04-01` “让次卧白色风扇开始摇头” → `{"enabled":true}`<br>`CMD-sim87_secondary_fan_001-04-02` “让次卧白色风扇停止摇头” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_fan_001-05-01` “查询次卧白色风扇的状态” → `{}` |

负例 `NEG-sim87_secondary_fan_001`：“把次卧白色风扇切换为制冷模式”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_fan_001-001` “次卧白色风扇打开” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_fan_001-002` “次卧白色风扇关掉” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_fan_001-003` “次卧白色风扇风速三档” → `set_speed` `{"level":3}`
- `VAR-sim87_secondary_fan_001-004` “次卧白色风扇不要摇头了” → `set_oscillation` `{"enabled":false}`

### 42. 次卧吸顶灯

- 设备名字：米家吸顶灯Pro 超薄…
- 唯一 ID：`sim87_secondary_light_001`
- 所在房间：次卧
- 设备类型：灯
- 唯一语音称呼：次卧吸顶灯
- 所属分组：`simgrp_all_light`、`simgrp_secondary_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_secondary_light_001-01-01` “打开次卧吸顶灯” → `{"on":true}`<br>`CMD-sim87_secondary_light_001-01-02` “关闭次卧吸顶灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_secondary_light_001-02-01` “把次卧吸顶灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_secondary_light_001-02-02` “把次卧吸顶灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_secondary_light_001-02-03` “把次卧吸顶灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_secondary_light_001-03-01` “把次卧吸顶灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_secondary_light_001-03-02` “把次卧吸顶灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_secondary_light_001-03-03` “把次卧吸顶灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_secondary_light_001-04-01` “把次卧吸顶灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_secondary_light_001-04-02` “把次卧吸顶灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_secondary_light_001-04-03` “把次卧吸顶灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_secondary_light_001-04-04` “把次卧吸顶灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_secondary_light_001-04-05` “把次卧吸顶灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_secondary_light_001-04-06` “把次卧吸顶灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_secondary_light_001-04-07` “把次卧吸顶灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_secondary_light_001-04-08` “把次卧吸顶灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_secondary_light_001-05-01` “把次卧吸顶灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_secondary_light_001-05-02` “把次卧吸顶灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_secondary_light_001-05-03` “把次卧吸顶灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_light_001-06-01` “查询次卧吸顶灯的状态” → `{}` |

负例 `NEG-sim87_secondary_light_001`：“打开次卧吸顶灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_light_001-001` “开次卧吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-002` “开次卧的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-003` “打开次卧的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-004` “把次卧吸顶灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-005` “点亮次卧的吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-006` “麻烦开一下次卧吸顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-007` “次卧吸顶灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-008` “关掉次卧的吸顶灯” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-009` “次卧的吸顶灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-010` “把次卧吸顶灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-011` “开次卧顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-012` “开次卧的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-013` “打开次卧的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-014` “把次卧顶灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-015` “点亮次卧的顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-016` “麻烦开一下次卧顶灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-017` “次卧顶灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-018` “关掉次卧的顶灯” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-019` “次卧的顶灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-020` “把次卧顶灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-021` “开次卧天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-022` “开次卧的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-023` “打开次卧的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-024` “把次卧天花板灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-025` “点亮次卧的天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-026` “麻烦开一下次卧天花板灯” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-027` “次卧天花板灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-028` “关掉次卧的天花板灯” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-029` “次卧的天花板灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-030` “把次卧天花板灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-031` “次卧吸顶灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_light_001-032` “把次卧吸顶灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_light_001-033` “次卧吸顶灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_secondary_light_001-034` “次卧吸顶灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_secondary_light_001-035` “次卧吸顶灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_secondary_light_001-036` “次卧吸顶灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_secondary_light_001-037` “次卧吸顶灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_secondary_light_001-038` “次卧吸顶灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 43. 次卧双键开关

- 设备名字：小米智能开关Pro（双…）
- 唯一 ID：`sim87_secondary_switch2_001`
- 所在房间：次卧
- 设备类型：开关
- 唯一语音称呼：次卧双键开关
- 所属分组：`simgrp_all_switch`、`simgrp_secondary_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, ch2, all}；on: boolean | `CMD-sim87_secondary_switch2_001-01-01` “打开次卧双键开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_secondary_switch2_001-01-02` “关闭次卧双键开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_secondary_switch2_001-01-03` “打开次卧双键开关第2路” → `{"channel":"ch2","on":true}`<br>`CMD-sim87_secondary_switch2_001-01-04` “关闭次卧双键开关第2路” → `{"channel":"ch2","on":false}`<br>`CMD-sim87_secondary_switch2_001-01-05` “打开次卧双键开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_secondary_switch2_001-01-06` “关闭次卧双键开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_switch2_001-02-01` “查询次卧双键开关的状态” → `{}` |

负例 `NEG-sim87_secondary_switch2_001`：“把次卧双键开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_switch2_001-001` “次卧双键开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_secondary_switch2_001-002` “次卧双键开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_secondary_switch2_001-003` “次卧双键开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_secondary_switch2_001-004` “次卧双键开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 44. 次卧音箱

- 设备名字：Xiaomi 智能音箱
- 唯一 ID：`sim87_secondary_speaker_001`
- 所在房间：次卧
- 设备类型：音箱
- 唯一语音称呼：次卧音箱
- 所属分组：`simgrp_all_speaker`、`simgrp_secondary_speaker`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_secondary_speaker_001-01-01` “打开次卧音箱” → `{"on":true}`<br>`CMD-sim87_secondary_speaker_001-01-02` “关闭次卧音箱” → `{"on":false}` |
| `pause` | 无参数 | `CMD-sim87_secondary_speaker_001-02-01` “暂停次卧音箱播放” → `{}` |
| `resume` | 无参数 | `CMD-sim87_secondary_speaker_001-03-01` “继续次卧音箱播放” → `{}` |
| `next_track` | 无参数 | `CMD-sim87_secondary_speaker_001-04-01` “让次卧音箱播放下一首” → `{}` |
| `previous_track` | 无参数 | `CMD-sim87_secondary_speaker_001-05-01` “让次卧音箱播放上一首” → `{}` |
| `set_volume` | percent: integer，0..100，步长 1；百分比；0 静音，不是断电 | `CMD-sim87_secondary_speaker_001-06-01` “把次卧音箱音量设为0%” → `{"percent":0}`<br>`CMD-sim87_secondary_speaker_001-06-02` “把次卧音箱音量设为40%” → `{"percent":40}`<br>`CMD-sim87_secondary_speaker_001-06-03` “把次卧音箱音量设为100%” → `{"percent":100}` |
| `set_mute` | muted: boolean | `CMD-sim87_secondary_speaker_001-07-01` “让次卧音箱静音” → `{"muted":true}`<br>`CMD-sim87_secondary_speaker_001-07-02` “取消次卧音箱静音” → `{"muted":false}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_speaker_001-08-01` “查询次卧音箱的状态” → `{}` |

负例 `NEG-sim87_secondary_speaker_001`：“让次卧音箱购买一首歌”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_speaker_001-001` “次卧音箱音量一半” → `set_volume` `{"percent":50}`
- `VAR-sim87_secondary_speaker_001-002` “次卧音箱别出声了” → `set_mute` `{"muted":true}`
- `VAR-sim87_secondary_speaker_001-003` “次卧音箱下一首” → `next_track` `{}`
- `VAR-sim87_secondary_speaker_001-004` “次卧音箱继续播” → `resume` `{}`

### 45. 次卧4C摄像机

- 设备名字：小米智能摄像机 4C 3…
- 唯一 ID：`sim87_secondary_camera_001`
- 所在房间：次卧
- 设备类型：摄像机
- 唯一语音称呼：次卧4C摄像机
- 所属分组：`simgrp_all_camera`、`simgrp_secondary_camera`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_secondary_camera_001-01-01` “打开次卧4C摄像机” → `{"on":true}`<br>`CMD-sim87_secondary_camera_001-01-02` “关闭次卧4C摄像机” → `{"on":false}` |
| `set_privacy` | enabled: boolean | `CMD-sim87_secondary_camera_001-02-01` “打开次卧4C摄像机隐私模式” → `{"enabled":true}`<br>`CMD-sim87_secondary_camera_001-02-02` “关闭次卧4C摄像机隐私模式” → `{"enabled":false}` |
| `set_recording` | enabled: boolean | `CMD-sim87_secondary_camera_001-03-01` “让次卧4C摄像机开始录像” → `{"enabled":true}`<br>`CMD-sim87_secondary_camera_001-03-02` “让次卧4C摄像机停止录像” → `{"enabled":false}` |
| `set_night_vision` | mode: enum {auto, on, off} | `CMD-sim87_secondary_camera_001-04-01` “把次卧4C摄像机夜视设为自动” → `{"mode":"auto"}`<br>`CMD-sim87_secondary_camera_001-04-02` “把次卧4C摄像机夜视设为开启” → `{"mode":"on"}`<br>`CMD-sim87_secondary_camera_001-04-03` “把次卧4C摄像机夜视设为关闭” → `{"mode":"off"}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_camera_001-05-01` “查询次卧4C摄像机的状态” → `{}` |

负例 `NEG-sim87_secondary_camera_001`：“让次卧4C摄像机识别陌生人的身份证号码”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_camera_001-001` “次卧4C摄像机开一下” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_camera_001-002` “次卧4C摄像机关掉” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_camera_001-003` “看一下次卧4C摄像机现在什么状态” → `get_state` `{}`

### 46. 次卧3Pro摄像机

- 设备名字：小米智能摄像机3 Pro…
- 唯一 ID：`sim87_secondary_camera_002`
- 所在房间：次卧
- 设备类型：摄像机
- 唯一语音称呼：次卧3Pro摄像机
- 所属分组：`simgrp_all_camera`、`simgrp_secondary_camera`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_secondary_camera_002-01-01` “打开次卧3Pro摄像机” → `{"on":true}`<br>`CMD-sim87_secondary_camera_002-01-02` “关闭次卧3Pro摄像机” → `{"on":false}` |
| `set_privacy` | enabled: boolean | `CMD-sim87_secondary_camera_002-02-01` “打开次卧3Pro摄像机隐私模式” → `{"enabled":true}`<br>`CMD-sim87_secondary_camera_002-02-02` “关闭次卧3Pro摄像机隐私模式” → `{"enabled":false}` |
| `set_recording` | enabled: boolean | `CMD-sim87_secondary_camera_002-03-01` “让次卧3Pro摄像机开始录像” → `{"enabled":true}`<br>`CMD-sim87_secondary_camera_002-03-02` “让次卧3Pro摄像机停止录像” → `{"enabled":false}` |
| `set_night_vision` | mode: enum {auto, on, off} | `CMD-sim87_secondary_camera_002-04-01` “把次卧3Pro摄像机夜视设为自动” → `{"mode":"auto"}`<br>`CMD-sim87_secondary_camera_002-04-02` “把次卧3Pro摄像机夜视设为开启” → `{"mode":"on"}`<br>`CMD-sim87_secondary_camera_002-04-03` “把次卧3Pro摄像机夜视设为关闭” → `{"mode":"off"}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_camera_002-05-01` “查询次卧3Pro摄像机的状态” → `{}` |

负例 `NEG-sim87_secondary_camera_002`：“让次卧3Pro摄像机识别陌生人的身份证号码”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_camera_002-001` “次卧3Pro摄像机开一下” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_camera_002-002` “次卧3Pro摄像机关掉” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_camera_002-003` “看一下次卧3Pro摄像机现在什么状态” → `get_state` `{}`

### 47. 次卧宝宝温度计

- 设备名字：宝宝温度
- 唯一 ID：`sim87_secondary_thermo_001`
- 所在房间：次卧
- 设备类型：温湿度传感器
- 唯一语音称呼：次卧宝宝温度计
- 所属分组：`simgrp_all_thermo`、`simgrp_secondary_thermo`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_temperature` | 无参数 | `CMD-sim87_secondary_thermo_001-01-01` “查询次卧宝宝温度计的温度” → `{}` |
| `get_humidity` | 无参数 | `CMD-sim87_secondary_thermo_001-02-01` “查询次卧宝宝温度计的湿度” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_thermo_001-03-01` “查询次卧宝宝温度计的状态” → `{}` |

负例 `NEG-sim87_secondary_thermo_001`：“把次卧宝宝温度计温度设为26度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_thermo_001-001` “看看次卧宝宝温度计现在什么状态” → `get_state` `{}`
- `VAR-sim87_secondary_thermo_001-002` “次卧宝宝温度计状态查一下” → `get_state` `{}`

### 48. 次卧宝宝湿度计

- 设备名字：宝宝湿度
- 唯一 ID：`sim87_secondary_thermo_002`
- 所在房间：次卧
- 设备类型：温湿度传感器
- 唯一语音称呼：次卧宝宝湿度计
- 所属分组：`simgrp_all_thermo`、`simgrp_secondary_thermo`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_temperature` | 无参数 | `CMD-sim87_secondary_thermo_002-01-01` “查询次卧宝宝湿度计的温度” → `{}` |
| `get_humidity` | 无参数 | `CMD-sim87_secondary_thermo_002-02-01` “查询次卧宝宝湿度计的湿度” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_thermo_002-03-01` “查询次卧宝宝湿度计的状态” → `{}` |

负例 `NEG-sim87_secondary_thermo_002`：“把次卧宝宝湿度计温度设为26度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_thermo_002-001` “看看次卧宝宝湿度计现在什么状态” → `get_state` `{}`
- `VAR-sim87_secondary_thermo_002-002` “次卧宝宝湿度计状态查一下” → `get_state` `{}`

### 49. 次卧灯遥控开关

- 设备名字：次卧灯开关
- 唯一 ID：`sim87_secondary_remote_001`
- 所在房间：次卧
- 设备类型：遥控器/无线开关
- 唯一语音称呼：次卧灯遥控开关
- 所属分组：`simgrp_all_remote`、`simgrp_secondary_remote`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_state` | 无参数 | `CMD-sim87_secondary_remote_001-01-01` “查询次卧灯遥控开关的状态” → `{}` |

负例 `NEG-sim87_secondary_remote_001`：“让次卧灯遥控开关模拟单击”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_remote_001-001` “看看次卧灯遥控开关现在什么状态” → `get_state` `{}`
- `VAR-sim87_secondary_remote_001-002` “次卧灯遥控开关状态查一下” → `get_state` `{}`

### 50. 次卧小米无线开关

- 设备名字：小米智能无线开关（…）
- 唯一 ID：`sim87_secondary_remote_002`
- 所在房间：次卧
- 设备类型：遥控器/无线开关
- 唯一语音称呼：次卧小米无线开关
- 所属分组：`simgrp_all_remote`、`simgrp_secondary_remote`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_state` | 无参数 | `CMD-sim87_secondary_remote_002-01-01` “查询次卧小米无线开关的状态” → `{}` |

负例 `NEG-sim87_secondary_remote_002`：“让次卧小米无线开关模拟单击”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_remote_002-001` “看看次卧小米无线开关现在什么状态” → `get_state` `{}`
- `VAR-sim87_secondary_remote_002-002` “次卧小米无线开关状态查一下” → `get_state` `{}`

### 51. 次卧加湿器

- 设备名字：米家纯净式智能加湿…
- 唯一 ID：`sim87_secondary_humidifier_001`
- 所在房间：次卧
- 设备类型：加湿器
- 唯一语音称呼：次卧加湿器
- 所属分组：`simgrp_all_humidifier`、`simgrp_secondary_humidifier`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_secondary_humidifier_001-01-01` “打开次卧加湿器” → `{"on":true}`<br>`CMD-sim87_secondary_humidifier_001-01-02` “关闭次卧加湿器” → `{"on":false}` |
| `set_mode` | mode: enum {auto, sleep, manual} | `CMD-sim87_secondary_humidifier_001-02-01` “把次卧加湿器切换为自动模式” → `{"mode":"auto"}`<br>`CMD-sim87_secondary_humidifier_001-02-02` “把次卧加湿器切换为睡眠模式” → `{"mode":"sleep"}`<br>`CMD-sim87_secondary_humidifier_001-02-03` “把次卧加湿器切换为手动模式” → `{"mode":"manual"}` |
| `set_target_humidity` | percent: integer，30..80，步长 1；相对湿度百分比 | `CMD-sim87_secondary_humidifier_001-03-01` “把次卧加湿器目标湿度设为30%” → `{"percent":30}`<br>`CMD-sim87_secondary_humidifier_001-03-02` “把次卧加湿器目标湿度设为50%” → `{"percent":50}`<br>`CMD-sim87_secondary_humidifier_001-03-03` “把次卧加湿器目标湿度设为80%” → `{"percent":80}` |
| `set_mist_level` | level: integer，1..3，步长 1；加湿档位；无雾型号也统一用此模拟字段 | `CMD-sim87_secondary_humidifier_001-04-01` “把次卧加湿器加湿档位设为1档” → `{"level":1}`<br>`CMD-sim87_secondary_humidifier_001-04-02` “把次卧加湿器加湿档位设为2档” → `{"level":2}`<br>`CMD-sim87_secondary_humidifier_001-04-03` “把次卧加湿器加湿档位设为3档” → `{"level":3}` |
| `get_state` | 无参数 | `CMD-sim87_secondary_humidifier_001-05-01` “查询次卧加湿器的状态” → `{}` |

负例 `NEG-sim87_secondary_humidifier_001`：“把次卧加湿器湿度设为120%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_secondary_humidifier_001-001` “次卧加湿器开一下” → `set_power` `{"on":true}`
- `VAR-sim87_secondary_humidifier_001-002` “次卧加湿器关掉” → `set_power` `{"on":false}`
- `VAR-sim87_secondary_humidifier_001-003` “看一下次卧加湿器现在什么状态” → `get_state` `{}`

### 52. 厨房窗帘

- 设备名字：米家智能窗帘2
- 唯一 ID：`sim87_kitchen_curtain_001`
- 所在房间：厨房
- 设备类型：窗帘
- 唯一语音称呼：厨房窗帘
- 所属分组：`simgrp_all_curtain`、`simgrp_kitchen_curtain`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `open` | 无参数 | `CMD-sim87_kitchen_curtain_001-01-01` “打开厨房窗帘” → `{}` |
| `close` | 无参数 | `CMD-sim87_kitchen_curtain_001-02-01` “关闭厨房窗帘” → `{}` |
| `stop` | 无参数 | `CMD-sim87_kitchen_curtain_001-03-01` “停止厨房窗帘移动” → `{}` |
| `set_position` | open_percent: integer，0..100，步长 1；打开百分比，0 全关，100 全开 | `CMD-sim87_kitchen_curtain_001-04-01` “把厨房窗帘打开到0%” → `{"open_percent":0}`<br>`CMD-sim87_kitchen_curtain_001-04-02` “把厨房窗帘打开到50%” → `{"open_percent":50}`<br>`CMD-sim87_kitchen_curtain_001-04-03` “把厨房窗帘打开到100%” → `{"open_percent":100}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_curtain_001-05-01` “查询厨房窗帘的状态” → `{}` |

负例 `NEG-sim87_kitchen_curtain_001`：“把厨房窗帘打开到120%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_curtain_001-001` “厨房窗帘拉开” → `open` `{}`
- `VAR-sim87_kitchen_curtain_001-002` “厨房窗帘合上” → `close` `{}`
- `VAR-sim87_kitchen_curtain_001-003` “厨房窗帘开一半” → `set_position` `{"open_percent":50}`
- `VAR-sim87_kitchen_curtain_001-004` “厨房窗帘别动了” → `stop` `{}`

### 53. 厨房双键开关

- 设备名字：小米智能开关（双开…）
- 唯一 ID：`sim87_kitchen_switch2_001`
- 所在房间：厨房
- 设备类型：开关
- 唯一语音称呼：厨房双键开关
- 所属分组：`simgrp_all_switch`、`simgrp_kitchen_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, ch2, all}；on: boolean | `CMD-sim87_kitchen_switch2_001-01-01` “打开厨房双键开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_kitchen_switch2_001-01-02` “关闭厨房双键开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_kitchen_switch2_001-01-03` “打开厨房双键开关第2路” → `{"channel":"ch2","on":true}`<br>`CMD-sim87_kitchen_switch2_001-01-04` “关闭厨房双键开关第2路” → `{"channel":"ch2","on":false}`<br>`CMD-sim87_kitchen_switch2_001-01-05` “打开厨房双键开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_kitchen_switch2_001-01-06` “关闭厨房双键开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_switch2_001-02-01` “查询厨房双键开关的状态” → `{}` |

负例 `NEG-sim87_kitchen_switch2_001`：“把厨房双键开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_switch2_001-001` “厨房双键开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_kitchen_switch2_001-002` “厨房双键开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_kitchen_switch2_001-003` “厨房双键开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_kitchen_switch2_001-004` “厨房双键开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 54. 厨房存在传感器

- 设备名字：子擎存在传感器 Lite
- 唯一 ID：`sim87_kitchen_presence_001`
- 所在房间：厨房
- 设备类型：存在传感器
- 唯一语音称呼：厨房存在传感器
- 所属分组：`simgrp_all_presence`、`simgrp_kitchen_presence`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_occupied` | 无参数 | `CMD-sim87_kitchen_presence_001-01-01` “厨房存在传感器检测到有人了吗” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_presence_001-02-01` “查询厨房存在传感器的状态” → `{}` |

负例 `NEG-sim87_kitchen_presence_001`：“关闭厨房存在传感器的检测功能”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_presence_001-001` “看看厨房存在传感器现在什么状态” → `get_state` `{}`
- `VAR-sim87_kitchen_presence_001-002` “厨房存在传感器状态查一下” → `get_state` `{}`

### 55. 厨房2号筒灯

- 设备名字：米家筒灯3 Pro-2
- 唯一 ID：`sim87_kitchen_light_001`
- 所在房间：厨房
- 设备类型：灯
- 唯一语音称呼：厨房2号筒灯
- 所属分组：`simgrp_all_light`、`simgrp_kitchen_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_kitchen_light_001-01-01` “打开厨房2号筒灯” → `{"on":true}`<br>`CMD-sim87_kitchen_light_001-01-02` “关闭厨房2号筒灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_kitchen_light_001-02-01` “把厨房2号筒灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_kitchen_light_001-02-02` “把厨房2号筒灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_kitchen_light_001-02-03` “把厨房2号筒灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_kitchen_light_001-03-01` “把厨房2号筒灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_kitchen_light_001-03-02` “把厨房2号筒灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_kitchen_light_001-03-03` “把厨房2号筒灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_kitchen_light_001-04-01` “把厨房2号筒灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_kitchen_light_001-04-02` “把厨房2号筒灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_kitchen_light_001-04-03` “把厨房2号筒灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_kitchen_light_001-04-04` “把厨房2号筒灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_kitchen_light_001-04-05` “把厨房2号筒灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_kitchen_light_001-04-06` “把厨房2号筒灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_kitchen_light_001-04-07` “把厨房2号筒灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_kitchen_light_001-04-08` “把厨房2号筒灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_kitchen_light_001-05-01` “把厨房2号筒灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_kitchen_light_001-05-02` “把厨房2号筒灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_kitchen_light_001-05-03` “把厨房2号筒灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_light_001-06-01` “查询厨房2号筒灯的状态” → `{}` |

负例 `NEG-sim87_kitchen_light_001`：“打开厨房2号筒灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_light_001-001` “开厨房2号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-002` “开厨房的2号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-003` “打开厨房的2号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-004` “把厨房2号筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-005` “点亮厨房的2号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-006` “麻烦开一下厨房2号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-007` “厨房2号筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-008` “关掉厨房的2号筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_001-009` “厨房的2号筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_001-010` “把厨房2号筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_001-011` “开厨房二号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-012` “开厨房的二号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-013` “打开厨房的二号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-014` “把厨房二号筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-015` “点亮厨房的二号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-016` “麻烦开一下厨房二号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-017` “厨房二号筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-018` “关掉厨房的二号筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_001-019` “厨房的二号筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_001-020` “把厨房二号筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_001-021` “厨房2号筒灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_001-022` “把厨房2号筒灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_001-023` “厨房2号筒灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_001-024` “厨房2号筒灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_001-025` “厨房2号筒灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_kitchen_light_001-026` “厨房2号筒灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_kitchen_light_001-027` “厨房2号筒灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_kitchen_light_001-028` “厨房2号筒灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 56. 厨房4号筒灯

- 设备名字：米家筒灯3 Pro-4
- 唯一 ID：`sim87_kitchen_light_002`
- 所在房间：厨房
- 设备类型：灯
- 唯一语音称呼：厨房4号筒灯
- 所属分组：`simgrp_all_light`、`simgrp_kitchen_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_kitchen_light_002-01-01` “打开厨房4号筒灯” → `{"on":true}`<br>`CMD-sim87_kitchen_light_002-01-02` “关闭厨房4号筒灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_kitchen_light_002-02-01` “把厨房4号筒灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_kitchen_light_002-02-02` “把厨房4号筒灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_kitchen_light_002-02-03` “把厨房4号筒灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_kitchen_light_002-03-01` “把厨房4号筒灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_kitchen_light_002-03-02` “把厨房4号筒灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_kitchen_light_002-03-03` “把厨房4号筒灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_kitchen_light_002-04-01` “把厨房4号筒灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_kitchen_light_002-04-02` “把厨房4号筒灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_kitchen_light_002-04-03` “把厨房4号筒灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_kitchen_light_002-04-04` “把厨房4号筒灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_kitchen_light_002-04-05` “把厨房4号筒灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_kitchen_light_002-04-06` “把厨房4号筒灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_kitchen_light_002-04-07` “把厨房4号筒灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_kitchen_light_002-04-08` “把厨房4号筒灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_kitchen_light_002-05-01` “把厨房4号筒灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_kitchen_light_002-05-02` “把厨房4号筒灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_kitchen_light_002-05-03` “把厨房4号筒灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_light_002-06-01` “查询厨房4号筒灯的状态” → `{}` |

负例 `NEG-sim87_kitchen_light_002`：“打开厨房4号筒灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_light_002-001` “开厨房4号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-002` “开厨房的4号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-003` “打开厨房的4号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-004` “把厨房4号筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-005` “点亮厨房的4号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-006` “麻烦开一下厨房4号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-007` “厨房4号筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-008` “关掉厨房的4号筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_002-009` “厨房的4号筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_002-010` “把厨房4号筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_002-011` “开厨房四号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-012` “开厨房的四号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-013` “打开厨房的四号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-014` “把厨房四号筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-015` “点亮厨房的四号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-016` “麻烦开一下厨房四号筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-017` “厨房四号筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-018` “关掉厨房的四号筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_002-019` “厨房的四号筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_002-020` “把厨房四号筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_002-021` “厨房4号筒灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_002-022` “把厨房4号筒灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_002-023` “厨房4号筒灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_002-024` “厨房4号筒灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_002-025` “厨房4号筒灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_kitchen_light_002-026` “厨房4号筒灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_kitchen_light_002-027` “厨房4号筒灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_kitchen_light_002-028` “厨房4号筒灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 57. 厨房感应筒灯

- 设备名字：米家筒灯3 Pro 人在感…
- 唯一 ID：`sim87_kitchen_light_003`
- 所在房间：厨房
- 设备类型：灯
- 唯一语音称呼：厨房感应筒灯
- 所属分组：`simgrp_all_light`、`simgrp_kitchen_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_kitchen_light_003-01-01` “打开厨房感应筒灯” → `{"on":true}`<br>`CMD-sim87_kitchen_light_003-01-02` “关闭厨房感应筒灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_kitchen_light_003-02-01` “把厨房感应筒灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_kitchen_light_003-02-02` “把厨房感应筒灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_kitchen_light_003-02-03` “把厨房感应筒灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_kitchen_light_003-03-01` “把厨房感应筒灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_kitchen_light_003-03-02` “把厨房感应筒灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_kitchen_light_003-03-03` “把厨房感应筒灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_kitchen_light_003-04-01` “把厨房感应筒灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_kitchen_light_003-04-02` “把厨房感应筒灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_kitchen_light_003-04-03` “把厨房感应筒灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_kitchen_light_003-04-04` “把厨房感应筒灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_kitchen_light_003-04-05` “把厨房感应筒灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_kitchen_light_003-04-06` “把厨房感应筒灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_kitchen_light_003-04-07` “把厨房感应筒灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_kitchen_light_003-04-08` “把厨房感应筒灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_kitchen_light_003-05-01` “把厨房感应筒灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_kitchen_light_003-05-02` “把厨房感应筒灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_kitchen_light_003-05-03` “把厨房感应筒灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_light_003-06-01` “查询厨房感应筒灯的状态” → `{}` |

负例 `NEG-sim87_kitchen_light_003`：“打开厨房感应筒灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_light_003-001` “开厨房感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-002` “开厨房的感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-003` “打开厨房的感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-004` “把厨房感应筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-005` “点亮厨房的感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-006` “麻烦开一下厨房感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-007` “厨房感应筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-008` “关掉厨房的感应筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_003-009` “厨房的感应筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_003-010` “把厨房感应筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_003-011` “开厨房人体感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-012` “开厨房的人体感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-013` “打开厨房的人体感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-014` “把厨房人体感应筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-015` “点亮厨房的人体感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-016` “麻烦开一下厨房人体感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-017` “厨房人体感应筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-018` “关掉厨房的人体感应筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_003-019` “厨房的人体感应筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_003-020` “把厨房人体感应筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_003-021` “厨房感应筒灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_003-022` “把厨房感应筒灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_003-023` “厨房感应筒灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_003-024` “厨房感应筒灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_003-025` “厨房感应筒灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_kitchen_light_003-026` “厨房感应筒灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_kitchen_light_003-027` “厨房感应筒灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_kitchen_light_003-028` “厨房感应筒灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 58. 厨房3号射灯

- 设备名字：米家射灯3 Pro-3
- 唯一 ID：`sim87_kitchen_light_004`
- 所在房间：厨房
- 设备类型：灯
- 唯一语音称呼：厨房3号射灯
- 所属分组：`simgrp_all_light`、`simgrp_kitchen_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_kitchen_light_004-01-01` “打开厨房3号射灯” → `{"on":true}`<br>`CMD-sim87_kitchen_light_004-01-02` “关闭厨房3号射灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_kitchen_light_004-02-01` “把厨房3号射灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_kitchen_light_004-02-02` “把厨房3号射灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_kitchen_light_004-02-03` “把厨房3号射灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_kitchen_light_004-03-01` “把厨房3号射灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_kitchen_light_004-03-02` “把厨房3号射灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_kitchen_light_004-03-03` “把厨房3号射灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_kitchen_light_004-04-01` “把厨房3号射灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_kitchen_light_004-04-02` “把厨房3号射灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_kitchen_light_004-04-03` “把厨房3号射灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_kitchen_light_004-04-04` “把厨房3号射灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_kitchen_light_004-04-05` “把厨房3号射灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_kitchen_light_004-04-06` “把厨房3号射灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_kitchen_light_004-04-07` “把厨房3号射灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_kitchen_light_004-04-08` “把厨房3号射灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_kitchen_light_004-05-01` “把厨房3号射灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_kitchen_light_004-05-02` “把厨房3号射灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_kitchen_light_004-05-03` “把厨房3号射灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_light_004-06-01` “查询厨房3号射灯的状态” → `{}` |

负例 `NEG-sim87_kitchen_light_004`：“打开厨房3号射灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_light_004-001` “开厨房3号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-002` “开厨房的3号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-003` “打开厨房的3号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-004` “把厨房3号射灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-005` “点亮厨房的3号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-006` “麻烦开一下厨房3号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-007` “厨房3号射灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-008` “关掉厨房的3号射灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_004-009` “厨房的3号射灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_004-010` “把厨房3号射灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_004-011` “开厨房三号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-012` “开厨房的三号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-013` “打开厨房的三号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-014` “把厨房三号射灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-015` “点亮厨房的三号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-016` “麻烦开一下厨房三号射灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-017` “厨房三号射灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-018` “关掉厨房的三号射灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_004-019` “厨房的三号射灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_004-020` “把厨房三号射灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_004-021` “厨房3号射灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_004-022` “把厨房3号射灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_004-023` “厨房3号射灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_004-024` “厨房3号射灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_004-025` “厨房3号射灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_kitchen_light_004-026` “厨房3号射灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_kitchen_light_004-027` “厨房3号射灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_kitchen_light_004-028` “厨房3号射灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 59. 厨房基础灯组

- 设备名字：灯组
- 唯一 ID：`sim87_kitchen_light_group_001`
- 所在房间：厨房
- 设备类型：截图灯组（虚拟）
- 唯一语音称呼：厨房基础灯组
- 模拟成员：`sim87_kitchen_light_001`、`sim87_kitchen_light_002`。按叶子展开，不保存独立灯状态。
- 所属派生组：不直接加入；成员已加入厨房所有灯、全屋所有灯，执行时去重。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_kitchen_light_group_001-01-01` “打开厨房基础灯组” → `{"on":true}`<br>`CMD-sim87_kitchen_light_group_001-01-02` “关闭厨房基础灯组” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_kitchen_light_group_001-02-01` “把厨房基础灯组亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_kitchen_light_group_001-02-02` “把厨房基础灯组亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_kitchen_light_group_001-02-03` “把厨房基础灯组亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_kitchen_light_group_001-03-01` “把厨房基础灯组色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_kitchen_light_group_001-03-02` “把厨房基础灯组色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_kitchen_light_group_001-03-03` “把厨房基础灯组色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_kitchen_light_group_001-04-01` “把厨房基础灯组设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_kitchen_light_group_001-04-02` “把厨房基础灯组设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_kitchen_light_group_001-04-03` “把厨房基础灯组设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_kitchen_light_group_001-04-04` “把厨房基础灯组设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_kitchen_light_group_001-04-05` “把厨房基础灯组设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_kitchen_light_group_001-04-06` “把厨房基础灯组设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_kitchen_light_group_001-04-07` “把厨房基础灯组设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_kitchen_light_group_001-04-08` “把厨房基础灯组设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_kitchen_light_group_001-05-01` “把厨房基础灯组亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_kitchen_light_group_001-05-02` “把厨房基础灯组亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_kitchen_light_group_001-05-03` “把厨房基础灯组亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_light_group_001-06-01` “查询厨房基础灯组的状态” → `{}` |

负例 `NEG-sim87_kitchen_light_group_001`：“让厨房基础灯组制冷”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_light_group_001-001` “开厨房厨房基础灯组” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-002` “开厨房的厨房基础灯组” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-003` “打开厨房的厨房基础灯组” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-004` “把厨房厨房基础灯组打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-005` “点亮厨房的厨房基础灯组” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-006` “麻烦开一下厨房厨房基础灯组” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-007` “厨房厨房基础灯组开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-008` “关掉厨房的厨房基础灯组” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_001-009` “厨房的厨房基础灯组灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_001-010` “把厨房厨房基础灯组关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_001-011` “厨房基础灯组开一下” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_001-012` “把厨房基础灯组给关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_001-013` “厨房基础灯组亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_group_001-014` “厨房基础灯组开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_group_001-015` “厨房基础灯组最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_kitchen_light_group_001-016` “厨房基础灯组暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_kitchen_light_group_001-017` “厨房基础灯组暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_kitchen_light_group_001-018` “厨房基础灯组改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 60. 厨房完整灯组

- 设备名字：米家筒灯3pro
- 唯一 ID：`sim87_kitchen_light_group_002`
- 所在房间：厨房
- 设备类型：截图灯组（虚拟）
- 唯一语音称呼：厨房完整灯组
- 模拟成员：`sim87_kitchen_light_001`、`sim87_kitchen_light_002`、`sim87_kitchen_light_003`、`sim87_kitchen_light_004`。按叶子展开，不保存独立灯状态。
- 所属派生组：不直接加入；成员已加入厨房所有灯、全屋所有灯，执行时去重。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_kitchen_light_group_002-01-01` “打开厨房完整灯组” → `{"on":true}`<br>`CMD-sim87_kitchen_light_group_002-01-02` “关闭厨房完整灯组” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_kitchen_light_group_002-02-01` “把厨房完整灯组亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_kitchen_light_group_002-02-02` “把厨房完整灯组亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_kitchen_light_group_002-02-03` “把厨房完整灯组亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_kitchen_light_group_002-03-01` “把厨房完整灯组色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_kitchen_light_group_002-03-02` “把厨房完整灯组色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_kitchen_light_group_002-03-03` “把厨房完整灯组色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_kitchen_light_group_002-04-01` “把厨房完整灯组设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_kitchen_light_group_002-04-02` “把厨房完整灯组设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_kitchen_light_group_002-04-03` “把厨房完整灯组设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_kitchen_light_group_002-04-04` “把厨房完整灯组设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_kitchen_light_group_002-04-05` “把厨房完整灯组设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_kitchen_light_group_002-04-06` “把厨房完整灯组设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_kitchen_light_group_002-04-07` “把厨房完整灯组设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_kitchen_light_group_002-04-08` “把厨房完整灯组设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_kitchen_light_group_002-05-01` “把厨房完整灯组亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_kitchen_light_group_002-05-02` “把厨房完整灯组亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_kitchen_light_group_002-05-03` “把厨房完整灯组亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_light_group_002-06-01` “查询厨房完整灯组的状态” → `{}` |

负例 `NEG-sim87_kitchen_light_group_002`：“让厨房完整灯组制冷”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_light_group_002-001` “开厨房感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-002` “开厨房的感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-003` “打开厨房的感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-004` “把厨房感应筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-005` “点亮厨房的感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-006` “麻烦开一下厨房感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-007` “厨房感应筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-008` “关掉厨房的感应筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_002-009` “厨房的感应筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_002-010` “把厨房感应筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_002-011` “开厨房人在感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-012` “开厨房的人在感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-013` “打开厨房的人在感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-014` “把厨房人在感应筒灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-015` “点亮厨房的人在感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-016` “麻烦开一下厨房人在感应筒灯” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-017` “厨房人在感应筒灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-018` “关掉厨房的人在感应筒灯” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_002-019` “厨房的人在感应筒灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_002-020` “把厨房人在感应筒灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_002-021` “厨房完整灯组开一下” → `set_power` `{"on":true}`
- `VAR-sim87_kitchen_light_group_002-022` “把厨房完整灯组给关了” → `set_power` `{"on":false}`
- `VAR-sim87_kitchen_light_group_002-023` “厨房完整灯组亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_group_002-024` “厨房完整灯组开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_kitchen_light_group_002-025` “厨房完整灯组最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_kitchen_light_group_002-026` “厨房完整灯组暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_kitchen_light_group_002-027` “厨房完整灯组暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_kitchen_light_group_002-028` “厨房完整灯组改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 61. 厨房烟雾传感器

- 设备名字：小米烟感卫士2
- 唯一 ID：`sim87_kitchen_smoke_001`
- 所在房间：厨房
- 设备类型：烟雾传感器
- 唯一语音称呼：厨房烟雾传感器
- 所属分组：`simgrp_all_smoke`、`simgrp_kitchen_smoke`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_smoke_detected` | 无参数 | `CMD-sim87_kitchen_smoke_001-01-01` “厨房烟雾传感器检测到烟雾了吗” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_smoke_001-02-01` “查询厨房烟雾传感器的状态” → `{}` |

负例 `NEG-sim87_kitchen_smoke_001`：“关闭厨房烟雾传感器的检测功能”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_smoke_001-001` “看看厨房烟雾传感器现在什么状态” → `get_state` `{}`
- `VAR-sim87_kitchen_smoke_001-002` “厨房烟雾传感器状态查一下” → `get_state` `{}`

### 62. 厨房微波炉

- 设备名字：米家微波炉
- 唯一 ID：`sim87_kitchen_microwave_001`
- 所在房间：厨房
- 设备类型：微波炉
- 唯一语音称呼：厨房微波炉
- 所属分组：`simgrp_all_microwave`、`simgrp_kitchen_microwave`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `start_heating` | seconds: integer 1..1800；power_percent: enum {20,40,60,80,100} | `CMD-sim87_kitchen_microwave_001-01-01` “让厨房微波炉以20%火力加热1秒” → `{"seconds":1,"power_percent":20}`<br>`CMD-sim87_kitchen_microwave_001-01-02` “让厨房微波炉以40%火力加热60秒” → `{"seconds":60,"power_percent":40}`<br>`CMD-sim87_kitchen_microwave_001-01-03` “让厨房微波炉以60%火力加热120秒” → `{"seconds":120,"power_percent":60}`<br>`CMD-sim87_kitchen_microwave_001-01-04` “让厨房微波炉以80%火力加热300秒” → `{"seconds":300,"power_percent":80}`<br>`CMD-sim87_kitchen_microwave_001-01-05` “让厨房微波炉以100%火力加热1800秒” → `{"seconds":1800,"power_percent":100}` |
| `pause` | 无参数 | `CMD-sim87_kitchen_microwave_001-02-01` “暂停厨房微波炉加热” → `{}` |
| `resume` | 无参数 | `CMD-sim87_kitchen_microwave_001-03-01` “继续厨房微波炉加热” → `{}` |
| `stop` | 无参数 | `CMD-sim87_kitchen_microwave_001-04-01` “停止厨房微波炉加热” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_microwave_001-05-01` “查询厨房微波炉的状态” → `{}` |

负例 `NEG-sim87_kitchen_microwave_001`：“让厨房微波炉加热两个小时”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_microwave_001-001` “看看厨房微波炉现在什么状态” → `get_state` `{}`
- `VAR-sim87_kitchen_microwave_001-002` “厨房微波炉状态查一下” → `get_state` `{}`

### 63. 厨房扫地机器人

- 设备名字：米家扫拖机器人 5 Pro
- 唯一 ID：`sim87_kitchen_robot_001`
- 所在房间：厨房
- 设备类型：扫地机器人
- 唯一语音称呼：厨房扫地机器人
- 所属分组：`simgrp_all_robot`、`simgrp_kitchen_robot`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `start_clean` | 无参数 | `CMD-sim87_kitchen_robot_001-01-01` “让厨房扫地机器人开始清扫” → `{}` |
| `pause` | 无参数 | `CMD-sim87_kitchen_robot_001-02-01` “暂停厨房扫地机器人清扫” → `{}` |
| `resume` | 无参数 | `CMD-sim87_kitchen_robot_001-03-01` “继续厨房扫地机器人清扫” → `{}` |
| `return_to_dock` | 无参数 | `CMD-sim87_kitchen_robot_001-04-01` “让厨房扫地机器人返回充电” → `{}` |
| `set_mode` | mode: enum {vacuum, mop, both} | `CMD-sim87_kitchen_robot_001-05-01` “让厨房扫地机器人切换为只扫地” → `{"mode":"vacuum"}`<br>`CMD-sim87_kitchen_robot_001-05-02` “让厨房扫地机器人切换为只拖地” → `{"mode":"mop"}`<br>`CMD-sim87_kitchen_robot_001-05-03` “让厨房扫地机器人切换为扫拖同时” → `{"mode":"both"}` |
| `set_suction` | level: integer，1..4，步长 1；吸力档位 | `CMD-sim87_kitchen_robot_001-06-01` “把厨房扫地机器人吸力设为1档” → `{"level":1}`<br>`CMD-sim87_kitchen_robot_001-06-02` “把厨房扫地机器人吸力设为2档” → `{"level":2}`<br>`CMD-sim87_kitchen_robot_001-06-03` “把厨房扫地机器人吸力设为3档” → `{"level":3}`<br>`CMD-sim87_kitchen_robot_001-06-04` “把厨房扫地机器人吸力设为4档” → `{"level":4}` |
| `set_water_level` | level: integer，1..3，步长 1；水量档位 | `CMD-sim87_kitchen_robot_001-07-01` “把厨房扫地机器人水量设为1档” → `{"level":1}`<br>`CMD-sim87_kitchen_robot_001-07-02` “把厨房扫地机器人水量设为2档” → `{"level":2}`<br>`CMD-sim87_kitchen_robot_001-07-03` “把厨房扫地机器人水量设为3档” → `{"level":3}` |
| `get_state` | 无参数 | `CMD-sim87_kitchen_robot_001-08-01` “查询厨房扫地机器人的状态” → `{}` |

负例 `NEG-sim87_kitchen_robot_001`：“让厨房扫地机器人清扫地图上不存在的区域”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_kitchen_robot_001-001` “看看厨房扫地机器人现在什么状态” → `get_state` `{}`
- `VAR-sim87_kitchen_robot_001-002` “厨房扫地机器人状态查一下” → `get_state` `{}`

### 64. 厕所浴霸

- 设备名字：Yeelight 智能浴霸 S20
- 唯一 ID：`sim87_toilet_bath_001`
- 所在房间：厕所
- 设备类型：浴霸
- 唯一语音称呼：厕所浴霸
- 所属分组：`simgrp_all_bath`、`simgrp_toilet_bath`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_light` | on: boolean | `CMD-sim87_toilet_bath_001-01-01` “打开厕所浴霸照明” → `{"on":true}`<br>`CMD-sim87_toilet_bath_001-01-02` “关闭厕所浴霸照明” → `{"on":false}` |
| `set_ventilation` | on: boolean | `CMD-sim87_toilet_bath_001-02-01` “打开厕所浴霸换气” → `{"on":true}`<br>`CMD-sim87_toilet_bath_001-02-02` “关闭厕所浴霸换气” → `{"on":false}` |
| `set_heating` | on: boolean | `CMD-sim87_toilet_bath_001-03-01` “打开厕所浴霸暖风” → `{"on":true}`<br>`CMD-sim87_toilet_bath_001-03-02` “关闭厕所浴霸暖风” → `{"on":false}` |
| `set_temperature` | celsius: integer，20..40，步长 1；摄氏度 | `CMD-sim87_toilet_bath_001-04-01` “把厕所浴霸暖风温度设为20度” → `{"celsius":20}`<br>`CMD-sim87_toilet_bath_001-04-02` “把厕所浴霸暖风温度设为28度” → `{"celsius":28}`<br>`CMD-sim87_toilet_bath_001-04-03` “把厕所浴霸暖风温度设为40度” → `{"celsius":40}` |
| `get_state` | 无参数 | `CMD-sim87_toilet_bath_001-05-01` “查询厕所浴霸的状态” → `{}` |

负例 `NEG-sim87_toilet_bath_001`：“让厕所浴霸制冷”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_toilet_bath_001-001` “看看厕所浴霸现在什么状态” → `get_state` `{}`
- `VAR-sim87_toilet_bath_001-002` “厕所浴霸状态查一下” → `get_state` `{}`

### 65. 厕所音箱

- 设备名字：小米小爱音箱Play 增…
- 唯一 ID：`sim87_toilet_speaker_001`
- 所在房间：厕所
- 设备类型：音箱
- 唯一语音称呼：厕所音箱
- 所属分组：`simgrp_all_speaker`、`simgrp_toilet_speaker`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_toilet_speaker_001-01-01` “打开厕所音箱” → `{"on":true}`<br>`CMD-sim87_toilet_speaker_001-01-02` “关闭厕所音箱” → `{"on":false}` |
| `pause` | 无参数 | `CMD-sim87_toilet_speaker_001-02-01` “暂停厕所音箱播放” → `{}` |
| `resume` | 无参数 | `CMD-sim87_toilet_speaker_001-03-01` “继续厕所音箱播放” → `{}` |
| `next_track` | 无参数 | `CMD-sim87_toilet_speaker_001-04-01` “让厕所音箱播放下一首” → `{}` |
| `previous_track` | 无参数 | `CMD-sim87_toilet_speaker_001-05-01` “让厕所音箱播放上一首” → `{}` |
| `set_volume` | percent: integer，0..100，步长 1；百分比；0 静音，不是断电 | `CMD-sim87_toilet_speaker_001-06-01` “把厕所音箱音量设为0%” → `{"percent":0}`<br>`CMD-sim87_toilet_speaker_001-06-02` “把厕所音箱音量设为40%” → `{"percent":40}`<br>`CMD-sim87_toilet_speaker_001-06-03` “把厕所音箱音量设为100%” → `{"percent":100}` |
| `set_mute` | muted: boolean | `CMD-sim87_toilet_speaker_001-07-01` “让厕所音箱静音” → `{"muted":true}`<br>`CMD-sim87_toilet_speaker_001-07-02` “取消厕所音箱静音” → `{"muted":false}` |
| `get_state` | 无参数 | `CMD-sim87_toilet_speaker_001-08-01` “查询厕所音箱的状态” → `{}` |

负例 `NEG-sim87_toilet_speaker_001`：“让厕所音箱购买一首歌”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_toilet_speaker_001-001` “厕所音箱音量一半” → `set_volume` `{"percent":50}`
- `VAR-sim87_toilet_speaker_001-002` “厕所音箱别出声了” → `set_mute` `{"muted":true}`
- `VAR-sim87_toilet_speaker_001-003` “厕所音箱下一首” → `next_track` `{}`
- `VAR-sim87_toilet_speaker_001-004` “厕所音箱继续播” → `resume` `{}`

### 66. 厕所热水插座

- 设备名字：热水
- 唯一 ID：`sim87_toilet_socket_001`
- 所在房间：厕所
- 设备类型：插座
- 唯一语音称呼：厕所热水插座
- 所属分组：`simgrp_all_socket`、`simgrp_toilet_socket`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_toilet_socket_001-01-01` “打开厕所热水插座” → `{"on":true}`<br>`CMD-sim87_toilet_socket_001-01-02` “关闭厕所热水插座” → `{"on":false}` |
| `get_state` | 无参数 | `CMD-sim87_toilet_socket_001-02-01` “查询厕所热水插座的状态” → `{}` |

负例 `NEG-sim87_toilet_socket_001`：“把厕所热水插座亮度设为50%”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_toilet_socket_001-001` “厕所热水插座开一下” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_socket_001-002` “厕所热水插座关掉” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_socket_001-003` “看一下厕所热水插座现在什么状态” → `get_state` `{}`

### 67. 厕所灯控开关

- 设备名字：厕所灯控
- 唯一 ID：`sim87_toilet_switch1_001`
- 所在房间：厕所
- 设备类型：开关
- 唯一语音称呼：厕所灯控开关
- 所属分组：`simgrp_all_switch`、`simgrp_toilet_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, all}；on: boolean | `CMD-sim87_toilet_switch1_001-01-01` “打开厕所灯控开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_toilet_switch1_001-01-02` “关闭厕所灯控开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_toilet_switch1_001-01-03` “打开厕所灯控开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_toilet_switch1_001-01-04` “关闭厕所灯控开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_toilet_switch1_001-02-01` “查询厕所灯控开关的状态” → `{}` |

负例 `NEG-sim87_toilet_switch1_001`：“把厕所灯控开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_toilet_switch1_001-001` “厕所灯控开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_toilet_switch1_001-002` “厕所灯控开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_toilet_switch1_001-003` “厕所灯控开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_toilet_switch1_001-004` “厕所灯控开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 68. 厕所存在传感器

- 设备名字：领普人体存在传感器3…
- 唯一 ID：`sim87_toilet_presence_001`
- 所在房间：厕所
- 设备类型：存在传感器
- 唯一语音称呼：厕所存在传感器
- 所属分组：`simgrp_all_presence`、`simgrp_toilet_presence`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_occupied` | 无参数 | `CMD-sim87_toilet_presence_001-01-01` “厕所存在传感器检测到有人了吗” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_toilet_presence_001-02-01` “查询厕所存在传感器的状态” → `{}` |

负例 `NEG-sim87_toilet_presence_001`：“关闭厕所存在传感器的检测功能”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_toilet_presence_001-001` “看看厕所存在传感器现在什么状态” → `get_state` `{}`
- `VAR-sim87_toilet_presence_001-002` “厕所存在传感器状态查一下” → `get_state` `{}`

### 69. 厕所无线开关

- 设备名字：无线开关
- 唯一 ID：`sim87_toilet_remote_001`
- 所在房间：厕所
- 设备类型：遥控器/无线开关
- 唯一语音称呼：厕所无线开关
- 所属分组：`simgrp_all_remote`、`simgrp_toilet_remote`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_state` | 无参数 | `CMD-sim87_toilet_remote_001-01-01` “查询厕所无线开关的状态” → `{}` |

负例 `NEG-sim87_toilet_remote_001`：“让厕所无线开关模拟单击”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_toilet_remote_001-001` “看看厕所无线开关现在什么状态” → `get_state` `{}`
- `VAR-sim87_toilet_remote_001-002` “厕所无线开关状态查一下” → `get_state` `{}`

### 70. 厕所青空灯

- 设备名字：青空灯
- 唯一 ID：`sim87_toilet_light_001`
- 所在房间：厕所
- 设备类型：灯
- 唯一语音称呼：厕所青空灯
- 所属分组：`simgrp_all_light`、`simgrp_toilet_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_toilet_light_001-01-01` “打开厕所青空灯” → `{"on":true}`<br>`CMD-sim87_toilet_light_001-01-02` “关闭厕所青空灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_toilet_light_001-02-01` “把厕所青空灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_toilet_light_001-02-02` “把厕所青空灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_toilet_light_001-02-03` “把厕所青空灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_toilet_light_001-03-01` “把厕所青空灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_toilet_light_001-03-02` “把厕所青空灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_toilet_light_001-03-03` “把厕所青空灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_toilet_light_001-04-01` “把厕所青空灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_toilet_light_001-04-02` “把厕所青空灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_toilet_light_001-04-03` “把厕所青空灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_toilet_light_001-04-04` “把厕所青空灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_toilet_light_001-04-05` “把厕所青空灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_toilet_light_001-04-06` “把厕所青空灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_toilet_light_001-04-07` “把厕所青空灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_toilet_light_001-04-08` “把厕所青空灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_toilet_light_001-05-01` “把厕所青空灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_toilet_light_001-05-02` “把厕所青空灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_toilet_light_001-05-03` “把厕所青空灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_toilet_light_001-06-01` “查询厕所青空灯的状态” → `{}` |

负例 `NEG-sim87_toilet_light_001`：“打开厕所青空灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_toilet_light_001-001` “开厕所青空灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-002` “开厕所的青空灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-003` “打开厕所的青空灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-004` “把厕所青空灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-005` “点亮厕所的青空灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-006` “麻烦开一下厕所青空灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-007` “厕所青空灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-008` “关掉厕所的青空灯” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_light_001-009` “厕所的青空灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_light_001-010` “把厕所青空灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_light_001-011` “开厕所厕所灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-012` “开厕所的厕所灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-013` “打开厕所的厕所灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-014` “把厕所厕所灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-015` “点亮厕所的厕所灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-016` “麻烦开一下厕所厕所灯” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-017` “厕所厕所灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-018` “关掉厕所的厕所灯” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_light_001-019` “厕所的厕所灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_light_001-020` “把厕所厕所灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_light_001-021` “厕所青空灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_toilet_light_001-022` “把厕所青空灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_toilet_light_001-023` “厕所青空灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_toilet_light_001-024` “厕所青空灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_toilet_light_001-025` “厕所青空灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_toilet_light_001-026` “厕所青空灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_toilet_light_001-027` “厕所青空灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_toilet_light_001-028` “厕所青空灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 71. 门厅中控屏

- 设备名字：小米智能中控屏
- 唯一 ID：`sim87_entry_panel_001`
- 所在房间：门厅
- 设备类型：控制面板
- 唯一语音称呼：门厅中控屏
- 所属分组：`simgrp_all_panel`、`simgrp_entry_panel`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_screen` | on: boolean | `CMD-sim87_entry_panel_001-01-01` “点亮门厅中控屏屏幕” → `{"on":true}`<br>`CMD-sim87_entry_panel_001-01-02` “熄灭门厅中控屏屏幕” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；屏幕亮度百分比 | `CMD-sim87_entry_panel_001-02-01` “把门厅中控屏屏幕亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_entry_panel_001-02-02` “把门厅中控屏屏幕亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_entry_panel_001-02-03` “把门厅中控屏屏幕亮度设为100%” → `{"percent":100}` |
| `set_volume` | percent: integer，0..100，步长 1；屏幕音量百分比 | `CMD-sim87_entry_panel_001-03-01` “把门厅中控屏音量设为0%” → `{"percent":0}`<br>`CMD-sim87_entry_panel_001-03-02` “把门厅中控屏音量设为40%” → `{"percent":40}`<br>`CMD-sim87_entry_panel_001-03-03` “把门厅中控屏音量设为100%” → `{"percent":100}` |
| `get_state` | 无参数 | `CMD-sim87_entry_panel_001-04-01` “查询门厅中控屏的状态” → `{}` |

负例 `NEG-sim87_entry_panel_001`：“给门厅中控屏安装任意软件”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_panel_001-001` “看看门厅中控屏现在什么状态” → `get_state` `{}`
- `VAR-sim87_entry_panel_001-002` “门厅中控屏状态查一下” → `get_state` `{}`

### 72. 门厅视频开关

- 设备名字：视频
- 唯一 ID：`sim87_entry_switch1_001`
- 所在房间：门厅
- 设备类型：开关
- 唯一语音称呼：门厅视频开关
- 所属分组：`simgrp_all_switch`、`simgrp_entry_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, all}；on: boolean | `CMD-sim87_entry_switch1_001-01-01` “打开门厅视频开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_entry_switch1_001-01-02` “关闭门厅视频开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_entry_switch1_001-01-03` “打开门厅视频开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_entry_switch1_001-01-04` “关闭门厅视频开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_entry_switch1_001-02-01` “查询门厅视频开关的状态” → `{}` |

负例 `NEG-sim87_entry_switch1_001`：“把门厅视频开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_switch1_001-001` “门厅视频开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_entry_switch1_001-002` “门厅视频开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_entry_switch1_001-003` “门厅视频开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_entry_switch1_001-004` “门厅视频开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 73. 门厅解锁开关

- 设备名字：解锁
- 唯一 ID：`sim87_entry_switch1_002`
- 所在房间：门厅
- 设备类型：开关
- 唯一语音称呼：门厅解锁开关
- 所属分组：`simgrp_all_switch`、`simgrp_entry_switch`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_channel_power` | channel: enum {ch1, all}；on: boolean | `CMD-sim87_entry_switch1_002-01-01` “打开门厅解锁开关第1路” → `{"channel":"ch1","on":true}`<br>`CMD-sim87_entry_switch1_002-01-02` “关闭门厅解锁开关第1路” → `{"channel":"ch1","on":false}`<br>`CMD-sim87_entry_switch1_002-01-03` “打开门厅解锁开关所有通道” → `{"channel":"all","on":true}`<br>`CMD-sim87_entry_switch1_002-01-04` “关闭门厅解锁开关所有通道” → `{"channel":"all","on":false}` |
| `get_state` | 无参数 | `CMD-sim87_entry_switch1_002-02-01` “查询门厅解锁开关的状态” → `{}` |

负例 `NEG-sim87_entry_switch1_002`：“把门厅解锁开关色温设为4000K”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_switch1_002-001` “门厅解锁开关全关” → `set_channel_power` `{"channel":"all","on":false}`
- `VAR-sim87_entry_switch1_002-002` “门厅解锁开关所有路打开” → `set_channel_power` `{"channel":"all","on":true}`
- `VAR-sim87_entry_switch1_002-003` “门厅解锁开关第一路给我开” → `set_channel_power` `{"channel":"ch1","on":true}`
- `VAR-sim87_entry_switch1_002-004` “门厅解锁开关1号键关了” → `set_channel_power` `{"channel":"ch1","on":false}`

### 74. 门厅门锁

- 设备名字：小米智能门锁 5 Max
- 唯一 ID：`sim87_entry_lock_001`
- 所在房间：门厅
- 设备类型：门锁
- 唯一语音称呼：门厅门锁
- 所属分组：`simgrp_all_lock`、`simgrp_entry_lock`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `lock` | 无参数 | `CMD-sim87_entry_lock_001-01-01` “锁上门厅门锁” → `{}` |
| `unlock` | 无参数 | `CMD-sim87_entry_lock_001-02-01` “解锁门厅门锁” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_entry_lock_001-03-01` “查询门厅门锁的状态” → `{}` |

负例 `NEG-sim87_entry_lock_001`：“把门厅门锁密码改成123456”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_lock_001-001` “看看门厅门锁现在什么状态” → `get_state` `{}`
- `VAR-sim87_entry_lock_001-002` “门厅门锁状态查一下” → `get_state` `{}`

### 75. 门厅无线开关2

- 设备名字：无线开关2
- 唯一 ID：`sim87_entry_remote_001`
- 所在房间：门厅
- 设备类型：遥控器/无线开关
- 唯一语音称呼：门厅无线开关2
- 所属分组：`simgrp_all_remote`、`simgrp_entry_remote`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_state` | 无参数 | `CMD-sim87_entry_remote_001-01-01` “查询门厅无线开关2的状态” → `{}` |

负例 `NEG-sim87_entry_remote_001`：“让门厅无线开关2模拟单击”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_remote_001-001` “看看门厅无线开关2现在什么状态” → `get_state` `{}`
- `VAR-sim87_entry_remote_001-002` “门厅无线开关2状态查一下” → `get_state` `{}`

### 76. 门厅猫眼2号

- 设备名字：小米智能猫眼2 2
- 唯一 ID：`sim87_entry_doorbell_001`
- 所在房间：门厅
- 设备类型：可视门铃/猫眼
- 唯一语音称呼：门厅猫眼2号
- 所属分组：`simgrp_all_doorbell`、`simgrp_entry_doorbell`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_privacy` | enabled: boolean | `CMD-sim87_entry_doorbell_001-01-01` “打开门厅猫眼2号隐私模式” → `{"enabled":true}`<br>`CMD-sim87_entry_doorbell_001-01-02` “关闭门厅猫眼2号隐私模式” → `{"enabled":false}` |
| `set_ring_volume` | percent: integer，0..100，步长 1；铃声音量百分比 | `CMD-sim87_entry_doorbell_001-02-01` “把门厅猫眼2号铃声音量设为0%” → `{"percent":0}`<br>`CMD-sim87_entry_doorbell_001-02-02` “把门厅猫眼2号铃声音量设为50%” → `{"percent":50}`<br>`CMD-sim87_entry_doorbell_001-02-03` “把门厅猫眼2号铃声音量设为100%” → `{"percent":100}` |
| `set_motion_alert` | enabled: boolean | `CMD-sim87_entry_doorbell_001-03-01` “打开门厅猫眼2号移动侦测提醒” → `{"enabled":true}`<br>`CMD-sim87_entry_doorbell_001-03-02` “关闭门厅猫眼2号移动侦测提醒” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_entry_doorbell_001-04-01` “查询门厅猫眼2号的状态” → `{}` |

负例 `NEG-sim87_entry_doorbell_001`：“让门厅猫眼2号打开门锁”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_doorbell_001-001` “看看门厅猫眼2号现在什么状态” → `get_state` `{}`
- `VAR-sim87_entry_doorbell_001-002` “门厅猫眼2号状态查一下” → `get_state` `{}`

### 77. 门厅猫眼1号

- 设备名字：小米智能猫眼2
- 唯一 ID：`sim87_entry_doorbell_002`
- 所在房间：门厅
- 设备类型：可视门铃/猫眼
- 唯一语音称呼：门厅猫眼1号
- 所属分组：`simgrp_all_doorbell`、`simgrp_entry_doorbell`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_privacy` | enabled: boolean | `CMD-sim87_entry_doorbell_002-01-01` “打开门厅猫眼1号隐私模式” → `{"enabled":true}`<br>`CMD-sim87_entry_doorbell_002-01-02` “关闭门厅猫眼1号隐私模式” → `{"enabled":false}` |
| `set_ring_volume` | percent: integer，0..100，步长 1；铃声音量百分比 | `CMD-sim87_entry_doorbell_002-02-01` “把门厅猫眼1号铃声音量设为0%” → `{"percent":0}`<br>`CMD-sim87_entry_doorbell_002-02-02` “把门厅猫眼1号铃声音量设为50%” → `{"percent":50}`<br>`CMD-sim87_entry_doorbell_002-02-03` “把门厅猫眼1号铃声音量设为100%” → `{"percent":100}` |
| `set_motion_alert` | enabled: boolean | `CMD-sim87_entry_doorbell_002-03-01` “打开门厅猫眼1号移动侦测提醒” → `{"enabled":true}`<br>`CMD-sim87_entry_doorbell_002-03-02` “关闭门厅猫眼1号移动侦测提醒” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_entry_doorbell_002-04-01` “查询门厅猫眼1号的状态” → `{}` |

负例 `NEG-sim87_entry_doorbell_002`：“让门厅猫眼1号打开门锁”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_doorbell_002-001` “看看门厅猫眼1号现在什么状态” → `get_state` `{}`
- `VAR-sim87_entry_doorbell_002-002` “门厅猫眼1号状态查一下” → `get_state` `{}`

### 78. 门厅门铃4

- 设备名字：小米智能门铃 4
- 唯一 ID：`sim87_entry_doorbell_003`
- 所在房间：门厅
- 设备类型：可视门铃/猫眼
- 唯一语音称呼：门厅门铃4
- 所属分组：`simgrp_all_doorbell`、`simgrp_entry_doorbell`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_privacy` | enabled: boolean | `CMD-sim87_entry_doorbell_003-01-01` “打开门厅门铃4隐私模式” → `{"enabled":true}`<br>`CMD-sim87_entry_doorbell_003-01-02` “关闭门厅门铃4隐私模式” → `{"enabled":false}` |
| `set_ring_volume` | percent: integer，0..100，步长 1；铃声音量百分比 | `CMD-sim87_entry_doorbell_003-02-01` “把门厅门铃4铃声音量设为0%” → `{"percent":0}`<br>`CMD-sim87_entry_doorbell_003-02-02` “把门厅门铃4铃声音量设为50%” → `{"percent":50}`<br>`CMD-sim87_entry_doorbell_003-02-03` “把门厅门铃4铃声音量设为100%” → `{"percent":100}` |
| `set_motion_alert` | enabled: boolean | `CMD-sim87_entry_doorbell_003-03-01` “打开门厅门铃4移动侦测提醒” → `{"enabled":true}`<br>`CMD-sim87_entry_doorbell_003-03-02` “关闭门厅门铃4移动侦测提醒” → `{"enabled":false}` |
| `get_state` | 无参数 | `CMD-sim87_entry_doorbell_003-04-01` “查询门厅门铃4的状态” → `{}` |

负例 `NEG-sim87_entry_doorbell_003`：“让门厅门铃4打开门锁”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_entry_doorbell_003-001` “看看门厅门铃4现在什么状态” → `get_state` `{}`
- `VAR-sim87_entry_doorbell_003-002` “门厅门铃4状态查一下” → `get_state` `{}`

### 79. 阳台晾衣架

- 设备名字：米家智能晾衣机
- 唯一 ID：`sim87_balcony_rack_001`
- 所在房间：阳台
- 设备类型：晾衣架
- 唯一语音称呼：阳台晾衣架
- 所属分组：`simgrp_all_rack`、`simgrp_balcony_rack`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `raise` | 无参数 | `CMD-sim87_balcony_rack_001-01-01` “升起阳台晾衣架” → `{}` |
| `lower` | 无参数 | `CMD-sim87_balcony_rack_001-02-01` “降下阳台晾衣架” → `{}` |
| `stop` | 无参数 | `CMD-sim87_balcony_rack_001-03-01` “停止阳台晾衣架移动” → `{}` |
| `set_position` | height_percent: integer，0..100，步长 1；高度百分比；0 最低，100 最高 | `CMD-sim87_balcony_rack_001-04-01` “把阳台晾衣架升到0%高度” → `{"height_percent":0}`<br>`CMD-sim87_balcony_rack_001-04-02` “把阳台晾衣架升到50%高度” → `{"height_percent":50}`<br>`CMD-sim87_balcony_rack_001-04-03` “把阳台晾衣架升到100%高度” → `{"height_percent":100}` |
| `set_light` | on: boolean | `CMD-sim87_balcony_rack_001-05-01` “打开阳台晾衣架照明” → `{"on":true}`<br>`CMD-sim87_balcony_rack_001-05-02` “关闭阳台晾衣架照明” → `{"on":false}` |
| `get_state` | 无参数 | `CMD-sim87_balcony_rack_001-06-01` “查询阳台晾衣架的状态” → `{}` |

负例 `NEG-sim87_balcony_rack_001`：“让阳台晾衣架旋转一圈”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_balcony_rack_001-001` “看看阳台晾衣架现在什么状态” → `get_state` `{}`
- `VAR-sim87_balcony_rack_001-002` “阳台晾衣架状态查一下” → `get_state` `{}`

### 80. 阳台中控屏

- 设备名字：小米智能中控屏 Max 2
- 唯一 ID：`sim87_balcony_panel_001`
- 所在房间：阳台
- 设备类型：控制面板
- 唯一语音称呼：阳台中控屏
- 所属分组：`simgrp_all_panel`、`simgrp_balcony_panel`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_screen` | on: boolean | `CMD-sim87_balcony_panel_001-01-01` “点亮阳台中控屏屏幕” → `{"on":true}`<br>`CMD-sim87_balcony_panel_001-01-02` “熄灭阳台中控屏屏幕” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；屏幕亮度百分比 | `CMD-sim87_balcony_panel_001-02-01` “把阳台中控屏屏幕亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_balcony_panel_001-02-02` “把阳台中控屏屏幕亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_balcony_panel_001-02-03` “把阳台中控屏屏幕亮度设为100%” → `{"percent":100}` |
| `set_volume` | percent: integer，0..100，步长 1；屏幕音量百分比 | `CMD-sim87_balcony_panel_001-03-01` “把阳台中控屏音量设为0%” → `{"percent":0}`<br>`CMD-sim87_balcony_panel_001-03-02` “把阳台中控屏音量设为40%” → `{"percent":40}`<br>`CMD-sim87_balcony_panel_001-03-03` “把阳台中控屏音量设为100%” → `{"percent":100}` |
| `get_state` | 无参数 | `CMD-sim87_balcony_panel_001-04-01` “查询阳台中控屏的状态” → `{}` |

负例 `NEG-sim87_balcony_panel_001`：“给阳台中控屏安装任意软件”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_balcony_panel_001-001` “看看阳台中控屏现在什么状态” → `get_state` `{}`
- `VAR-sim87_balcony_panel_001-002` “阳台中控屏状态查一下” → `get_state` `{}`

### 81. 小工具充电宝

- 设备名字：小米充电宝 Pro 2500…
- 唯一 ID：`sim87_utility_battery_001`
- 所在房间：小工具
- 设备类型：储能电源/充电宝
- 唯一语音称呼：小工具充电宝
- 所属分组：`simgrp_all_battery`、`simgrp_utility_battery`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_output` | enabled: boolean | `CMD-sim87_utility_battery_001-01-01` “打开小工具充电宝输出” → `{"enabled":true}`<br>`CMD-sim87_utility_battery_001-01-02` “关闭小工具充电宝输出” → `{"enabled":false}` |
| `get_battery` | 无参数 | `CMD-sim87_utility_battery_001-02-01` “查询小工具充电宝的剩余电量” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_utility_battery_001-03-01` “查询小工具充电宝的状态” → `{}` |

负例 `NEG-sim87_utility_battery_001`：“把小工具充电宝输出电压设为220伏”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_utility_battery_001-001` “看看小工具充电宝现在什么状态” → `get_state` `{}`
- `VAR-sim87_utility_battery_001-002` “小工具充电宝状态查一下” → `get_state` `{}`

### 82. 小工具体脂秤

- 设备名字：米家八电极体脂秤 S8…
- 唯一 ID：`sim87_utility_scale_001`
- 所在房间：小工具
- 设备类型：体重/体脂秤
- 唯一语音称呼：小工具体脂秤
- 所属分组：`simgrp_all_scale`、`simgrp_utility_scale`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_last_measurement` | 无参数 | `CMD-sim87_utility_scale_001-01-01` “查询小工具体脂秤最近一次测量” → `{}` |
| `get_battery` | 无参数 | `CMD-sim87_utility_scale_001-02-01` “查询小工具体脂秤的剩余电量” → `{}` |
| `get_state` | 无参数 | `CMD-sim87_utility_scale_001-03-01` “查询小工具体脂秤的状态” → `{}` |

负例 `NEG-sim87_utility_scale_001`：“把小工具体脂秤测量结果改为50公斤”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_utility_scale_001-001` “看看小工具体脂秤现在什么状态” → `get_state` `{}`
- `VAR-sim87_utility_scale_001-002` “小工具体脂秤状态查一下” → `get_state` `{}`

### 83. 小工具水暖毯

- 设备名字：米家智能水暖毯
- 唯一 ID：`sim87_utility_blanket_001`
- 所在房间：小工具
- 设备类型：电热毯/水暖垫
- 唯一语音称呼：小工具水暖毯
- 所属分组：`simgrp_all_blanket`、`simgrp_utility_blanket`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_utility_blanket_001-01-01` “打开小工具水暖毯” → `{"on":true}`<br>`CMD-sim87_utility_blanket_001-01-02` “关闭小工具水暖毯” → `{"on":false}` |
| `set_temperature` | celsius: integer，25..45，步长 1；摄氏度 | `CMD-sim87_utility_blanket_001-02-01` “把小工具水暖毯温度设为25度” → `{"celsius":25}`<br>`CMD-sim87_utility_blanket_001-02-02` “把小工具水暖毯温度设为35度” → `{"celsius":35}`<br>`CMD-sim87_utility_blanket_001-02-03` “把小工具水暖毯温度设为45度” → `{"celsius":45}` |
| `set_timer` | minutes: integer，1..480，步长 1；倒计时，到期关机 | `CMD-sim87_utility_blanket_001-03-01` “让小工具水暖毯在1分钟后关闭” → `{"minutes":1}`<br>`CMD-sim87_utility_blanket_001-03-02` “让小工具水暖毯在120分钟后关闭” → `{"minutes":120}`<br>`CMD-sim87_utility_blanket_001-03-03` “让小工具水暖毯在480分钟后关闭” → `{"minutes":480}` |
| `get_state` | 无参数 | `CMD-sim87_utility_blanket_001-04-01` “查询小工具水暖毯的状态” → `{}` |

负例 `NEG-sim87_utility_blanket_001`：“把小工具水暖毯温度设为90度”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_utility_blanket_001-001` “小工具水暖毯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_utility_blanket_001-002` “小工具水暖毯关掉” → `set_power` `{"on":false}`
- `VAR-sim87_utility_blanket_001-003` “看一下小工具水暖毯现在什么状态” → `get_state` `{}`

### 84. 小工具皮皮灯

- 设备名字：米家皮皮灯
- 唯一 ID：`sim87_utility_light_001`
- 所在房间：小工具
- 设备类型：灯
- 唯一语音称呼：小工具皮皮灯
- 所属分组：`simgrp_all_light`、`simgrp_utility_light`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_utility_light_001-01-01` “打开小工具皮皮灯” → `{"on":true}`<br>`CMD-sim87_utility_light_001-01-02` “关闭小工具皮皮灯” → `{"on":false}` |
| `set_brightness` | percent: integer，0..100，步长 1；百分比；0 等价关灯 | `CMD-sim87_utility_light_001-02-01` “把小工具皮皮灯亮度设为0%” → `{"percent":0}`<br>`CMD-sim87_utility_light_001-02-02` “把小工具皮皮灯亮度设为50%” → `{"percent":50}`<br>`CMD-sim87_utility_light_001-02-03` “把小工具皮皮灯亮度设为100%” → `{"percent":100}` |
| `set_color_temperature` | kelvin: integer，2700..5500，步长 1；K | `CMD-sim87_utility_light_001-03-01` “把小工具皮皮灯色温设为2700K” → `{"kelvin":2700}`<br>`CMD-sim87_utility_light_001-03-02` “把小工具皮皮灯色温设为4000K” → `{"kelvin":4000}`<br>`CMD-sim87_utility_light_001-03-03` “把小工具皮皮灯色温设为5500K” → `{"kelvin":5500}` |
| `set_color` | hex: string，格式 #RRGGBB；每通道 00..FF | `CMD-sim87_utility_light_001-04-01` “把小工具皮皮灯设为红色” → `{"hex":"#FF0000"}`<br>`CMD-sim87_utility_light_001-04-02` “把小工具皮皮灯设为绿色” → `{"hex":"#00FF00"}`<br>`CMD-sim87_utility_light_001-04-03` “把小工具皮皮灯设为蓝色” → `{"hex":"#0000FF"}`<br>`CMD-sim87_utility_light_001-04-04` “把小工具皮皮灯设为黄色” → `{"hex":"#FFFF00"}`<br>`CMD-sim87_utility_light_001-04-05` “把小工具皮皮灯设为紫色” → `{"hex":"#800080"}`<br>`CMD-sim87_utility_light_001-04-06` “把小工具皮皮灯设为青色” → `{"hex":"#00FFFF"}`<br>`CMD-sim87_utility_light_001-04-07` “把小工具皮皮灯设为白色” → `{"hex":"#FFFFFF"}`<br>`CMD-sim87_utility_light_001-04-08` “把小工具皮皮灯设为黑色” → `{"hex":"#000000"}` |
| `adjust_brightness` | delta: integer，-100..100，步长 1；百分点，有符号；结果截断到 0..100 | `CMD-sim87_utility_light_001-05-01` “把小工具皮皮灯亮度调整-100个百分点” → `{"delta":-100}`<br>`CMD-sim87_utility_light_001-05-02` “把小工具皮皮灯亮度调整10个百分点” → `{"delta":10}`<br>`CMD-sim87_utility_light_001-05-03` “把小工具皮皮灯亮度调整100个百分点” → `{"delta":100}` |
| `get_state` | 无参数 | `CMD-sim87_utility_light_001-06-01` “查询小工具皮皮灯的状态” → `{}` |

负例 `NEG-sim87_utility_light_001`：“打开小工具皮皮灯的新风”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_utility_light_001-001` “开小工具皮皮灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-002` “开小工具的皮皮灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-003` “打开小工具的皮皮灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-004` “把小工具皮皮灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-005` “点亮小工具的皮皮灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-006` “麻烦开一下小工具皮皮灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-007` “小工具皮皮灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-008` “关掉小工具的皮皮灯” → `set_power` `{"on":false}`
- `VAR-sim87_utility_light_001-009` “小工具的皮皮灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_utility_light_001-010` “把小工具皮皮灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_utility_light_001-011` “开小工具氛围灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-012` “开小工具的氛围灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-013` “打开小工具的氛围灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-014` “把小工具氛围灯打开” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-015` “点亮小工具的氛围灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-016` “麻烦开一下小工具氛围灯” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-017` “小工具氛围灯开起来” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-018` “关掉小工具的氛围灯” → `set_power` `{"on":false}`
- `VAR-sim87_utility_light_001-019` “小工具的氛围灯灭掉” → `set_power` `{"on":false}`
- `VAR-sim87_utility_light_001-020` “把小工具氛围灯关了” → `set_power` `{"on":false}`
- `VAR-sim87_utility_light_001-021` “小工具皮皮灯开一下” → `set_power` `{"on":true}`
- `VAR-sim87_utility_light_001-022` “把小工具皮皮灯给关了” → `set_power` `{"on":false}`
- `VAR-sim87_utility_light_001-023` “小工具皮皮灯亮度给我调到五十” → `set_brightness` `{"percent":50}`
- `VAR-sim87_utility_light_001-024` “小工具皮皮灯开到一半亮” → `set_brightness` `{"percent":50}`
- `VAR-sim87_utility_light_001-025` “小工具皮皮灯最亮” → `set_brightness` `{"percent":100}`
- `VAR-sim87_utility_light_001-026` “小工具皮皮灯暗一点” → `adjust_brightness` `{"delta":-10}`
- `VAR-sim87_utility_light_001-027` “小工具皮皮灯暖白光” → `set_color_temperature` `{"kelvin":3000}`
- `VAR-sim87_utility_light_001-028` “小工具皮皮灯改成红灯” → `set_color` `{"hex":"#FF0000"}`

### 85. 小工具便携音箱

- 设备名字：Xiaomi Sound Move
- 唯一 ID：`sim87_utility_speaker_001`
- 所在房间：小工具
- 设备类型：音箱
- 唯一语音称呼：小工具便携音箱
- 所属分组：`simgrp_all_speaker`、`simgrp_utility_speaker`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_utility_speaker_001-01-01` “打开小工具便携音箱” → `{"on":true}`<br>`CMD-sim87_utility_speaker_001-01-02` “关闭小工具便携音箱” → `{"on":false}` |
| `pause` | 无参数 | `CMD-sim87_utility_speaker_001-02-01` “暂停小工具便携音箱播放” → `{}` |
| `resume` | 无参数 | `CMD-sim87_utility_speaker_001-03-01` “继续小工具便携音箱播放” → `{}` |
| `next_track` | 无参数 | `CMD-sim87_utility_speaker_001-04-01` “让小工具便携音箱播放下一首” → `{}` |
| `previous_track` | 无参数 | `CMD-sim87_utility_speaker_001-05-01` “让小工具便携音箱播放上一首” → `{}` |
| `set_volume` | percent: integer，0..100，步长 1；百分比；0 静音，不是断电 | `CMD-sim87_utility_speaker_001-06-01` “把小工具便携音箱音量设为0%” → `{"percent":0}`<br>`CMD-sim87_utility_speaker_001-06-02` “把小工具便携音箱音量设为40%” → `{"percent":40}`<br>`CMD-sim87_utility_speaker_001-06-03` “把小工具便携音箱音量设为100%” → `{"percent":100}` |
| `set_mute` | muted: boolean | `CMD-sim87_utility_speaker_001-07-01` “让小工具便携音箱静音” → `{"muted":true}`<br>`CMD-sim87_utility_speaker_001-07-02` “取消小工具便携音箱静音” → `{"muted":false}` |
| `get_state` | 无参数 | `CMD-sim87_utility_speaker_001-08-01` “查询小工具便携音箱的状态” → `{}` |

负例 `NEG-sim87_utility_speaker_001`：“让小工具便携音箱购买一首歌”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_utility_speaker_001-001` “小工具便携音箱音量一半” → `set_volume` `{"percent":50}`
- `VAR-sim87_utility_speaker_001-002` “小工具便携音箱别出声了” → `set_mute` `{"muted":true}`
- `VAR-sim87_utility_speaker_001-003` “小工具便携音箱下一首” → `next_track` `{}`
- `VAR-sim87_utility_speaker_001-004` “小工具便携音箱继续播” → `resume` `{}`

### 86. 餐厅B遥控开关

- 设备名字：B
- 唯一 ID：`sim87_dining_remote_001`
- 所在房间：餐厅
- 设备类型：遥控器/无线开关
- 唯一语音称呼：餐厅B遥控开关
- 所属分组：`simgrp_all_remote`、`simgrp_dining_remote`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `get_state` | 无参数 | `CMD-sim87_dining_remote_001-01-01` “查询餐厅B遥控开关的状态” → `{}` |

负例 `NEG-sim87_dining_remote_001`：“让餐厅B遥控开关模拟单击”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_dining_remote_001-001` “看看餐厅B遥控开关现在什么状态” → `get_state` `{}`
- `VAR-sim87_dining_remote_001-002` “餐厅B遥控开关状态查一下” → `get_state` `{}`

### 87. 菜地室外摄像机

- 设备名字：小米室外摄像机CW7…
- 唯一 ID：`sim87_garden_camera_001`
- 所在房间：菜地
- 设备类型：摄像机
- 唯一语音称呼：菜地室外摄像机
- 所属分组：`simgrp_all_camera`、`simgrp_garden_camera`。

| 控制指令 / action | 参数、范围与单位 | 语音指令 → 预期参数 |
| --- | --- | --- |
| `set_power` | on: boolean | `CMD-sim87_garden_camera_001-01-01` “打开菜地室外摄像机” → `{"on":true}`<br>`CMD-sim87_garden_camera_001-01-02` “关闭菜地室外摄像机” → `{"on":false}` |
| `set_privacy` | enabled: boolean | `CMD-sim87_garden_camera_001-02-01` “打开菜地室外摄像机隐私模式” → `{"enabled":true}`<br>`CMD-sim87_garden_camera_001-02-02` “关闭菜地室外摄像机隐私模式” → `{"enabled":false}` |
| `set_recording` | enabled: boolean | `CMD-sim87_garden_camera_001-03-01` “让菜地室外摄像机开始录像” → `{"enabled":true}`<br>`CMD-sim87_garden_camera_001-03-02` “让菜地室外摄像机停止录像” → `{"enabled":false}` |
| `set_night_vision` | mode: enum {auto, on, off} | `CMD-sim87_garden_camera_001-04-01` “把菜地室外摄像机夜视设为自动” → `{"mode":"auto"}`<br>`CMD-sim87_garden_camera_001-04-02` “把菜地室外摄像机夜视设为开启” → `{"mode":"on"}`<br>`CMD-sim87_garden_camera_001-04-03` “把菜地室外摄像机夜视设为关闭” → `{"mode":"off"}` |
| `get_state` | 无参数 | `CMD-sim87_garden_camera_001-05-01` “查询菜地室外摄像机的状态” → `{}` |

负例 `NEG-sim87_garden_camera_001`：“让菜地室外摄像机识别陌生人的身份证号码”。预期不执行；越界数字为 `invalid_parameter`，其余为 `unsupported_capability`。

口语变体（预期均为 `execute`，设备目标均为上述 ID）：

- `VAR-sim87_garden_camera_001-001` “菜地室外摄像机开一下” → `set_power` `{"on":true}`
- `VAR-sim87_garden_camera_001-002` “菜地室外摄像机关掉” → `set_power` `{"on":false}`
- `VAR-sim87_garden_camera_001-003` “看一下菜地室外摄像机现在什么状态” → `get_state` `{}`

## 5.1 分组口语指令与可关联 ID

分组测试语句和标准答案也有稳定 ID，可在双语 TTS 表中按 ID 查找。

- `GRP-simgrp_all_ac-PWR-ON-01` “打开全屋所有空调” → `simgrp_all_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_all_ac-PWR-ON-02` “把全屋所有空调都打开” → `simgrp_all_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_all_ac-PWR-ON-03` “全屋所有空调全开” → `simgrp_all_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_all_ac-PWR-OFF-01` “关闭全屋所有空调” → `simgrp_all_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_all_ac-PWR-OFF-02` “把全屋所有空调都关掉” → `simgrp_all_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_all_ac-PWR-OFF-03` “全屋所有空调全关” → `simgrp_all_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_all_ac-TEMP-20-01` “把全屋所有空调设为20度” → `simgrp_all_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_all_ac-TEMP-20-02` “全屋所有空调温度20” → `simgrp_all_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_all_ac-TEMP-20-03` “所有空调都调到20度” → `simgrp_all_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_all_ac-TEMP-26-01` “把全屋所有空调设为26度” → `simgrp_all_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_all_ac-TEMP-26-02` “全屋所有空调温度26” → `simgrp_all_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_all_ac-TEMP-26-03` “所有空调都调到26度” → `simgrp_all_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_all_ac-TEMP-30-01` “把全屋所有空调设为30度” → `simgrp_all_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_all_ac-TEMP-30-02` “全屋所有空调温度30” → `simgrp_all_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_all_ac-TEMP-30-03` “所有空调都调到30度” → `simgrp_all_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_all_light-PWR-ON-01` “打开全屋所有灯” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-PWR-ON-02` “把全屋所有灯都打开” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-PWR-ON-03` “全屋所有灯全开” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-PWR-OFF-01` “关闭全屋所有灯” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-PWR-OFF-02` “把全屋所有灯都关掉” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-PWR-OFF-03` “全屋所有灯全关” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-ON-01` “开全屋灯” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-ON-02` “开全屋的灯” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-ON-03` “打开全屋照明” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-ON-04` “把全屋的灯打开” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-ON-05` “全屋开灯” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-ON-06` “点亮全屋所有灯” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-ON-07` “全屋灯都打开” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-ON-08` “麻烦开一下全屋的照明” → `simgrp_all_light` / `set_power` `{"on":true}`
- `GRP-simgrp_all_light-ROOM-OFF-01` “关全屋灯” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-OFF-02` “关全屋的灯” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-OFF-03` “关闭全屋照明” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-OFF-04` “把全屋的灯关掉” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-OFF-05` “全屋关灯” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-OFF-06` “熄灭全屋所有灯” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-OFF-07` “全屋灯都关掉” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_light-ROOM-OFF-08` “麻烦关一下全屋的照明” → `simgrp_all_light` / `set_power` `{"on":false}`
- `GRP-simgrp_all_socket-PWR-ON-01` “打开全屋所有插座” → `simgrp_all_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_all_socket-PWR-ON-02` “把全屋所有插座都打开” → `simgrp_all_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_all_socket-PWR-ON-03` “全屋所有插座全开” → `simgrp_all_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_all_socket-PWR-OFF-01` “关闭全屋所有插座” → `simgrp_all_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_all_socket-PWR-OFF-02` “把全屋所有插座都关掉” → `simgrp_all_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_all_socket-PWR-OFF-03` “全屋所有插座全关” → `simgrp_all_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_all_strip-PWR-ON-01` “打开全屋所有插排” → `simgrp_all_strip` / `set_power` `{"on":true}`
- `GRP-simgrp_all_strip-PWR-ON-02` “把全屋所有插排都打开” → `simgrp_all_strip` / `set_power` `{"on":true}`
- `GRP-simgrp_all_strip-PWR-ON-03` “全屋所有插排全开” → `simgrp_all_strip` / `set_power` `{"on":true}`
- `GRP-simgrp_all_strip-PWR-OFF-01` “关闭全屋所有插排” → `simgrp_all_strip` / `set_power` `{"on":false}`
- `GRP-simgrp_all_strip-PWR-OFF-02` “把全屋所有插排都关掉” → `simgrp_all_strip` / `set_power` `{"on":false}`
- `GRP-simgrp_all_strip-PWR-OFF-03` “全屋所有插排全关” → `simgrp_all_strip` / `set_power` `{"on":false}`
- `GRP-simgrp_all_speaker-PWR-ON-01` “打开全屋所有音箱” → `simgrp_all_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_all_speaker-PWR-ON-02` “把全屋所有音箱都打开” → `simgrp_all_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_all_speaker-PWR-ON-03` “全屋所有音箱全开” → `simgrp_all_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_all_speaker-PWR-OFF-01` “关闭全屋所有音箱” → `simgrp_all_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_all_speaker-PWR-OFF-02` “把全屋所有音箱都关掉” → `simgrp_all_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_all_speaker-PWR-OFF-03` “全屋所有音箱全关” → `simgrp_all_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_all_purifier-PWR-ON-01` “打开全屋所有空气净化器” → `simgrp_all_purifier` / `set_power` `{"on":true}`
- `GRP-simgrp_all_purifier-PWR-ON-02` “把全屋所有空气净化器都打开” → `simgrp_all_purifier` / `set_power` `{"on":true}`
- `GRP-simgrp_all_purifier-PWR-ON-03` “全屋所有空气净化器全开” → `simgrp_all_purifier` / `set_power` `{"on":true}`
- `GRP-simgrp_all_purifier-PWR-OFF-01` “关闭全屋所有空气净化器” → `simgrp_all_purifier` / `set_power` `{"on":false}`
- `GRP-simgrp_all_purifier-PWR-OFF-02` “把全屋所有空气净化器都关掉” → `simgrp_all_purifier` / `set_power` `{"on":false}`
- `GRP-simgrp_all_purifier-PWR-OFF-03` “全屋所有空气净化器全关” → `simgrp_all_purifier` / `set_power` `{"on":false}`
- `GRP-simgrp_all_washer-PWR-ON-01` “打开全屋所有擦地机” → `simgrp_all_washer` / `set_power` `{"on":true}`
- `GRP-simgrp_all_washer-PWR-ON-02` “把全屋所有擦地机都打开” → `simgrp_all_washer` / `set_power` `{"on":true}`
- `GRP-simgrp_all_washer-PWR-ON-03` “全屋所有擦地机全开” → `simgrp_all_washer` / `set_power` `{"on":true}`
- `GRP-simgrp_all_washer-PWR-OFF-01` “关闭全屋所有擦地机” → `simgrp_all_washer` / `set_power` `{"on":false}`
- `GRP-simgrp_all_washer-PWR-OFF-02` “把全屋所有擦地机都关掉” → `simgrp_all_washer` / `set_power` `{"on":false}`
- `GRP-simgrp_all_washer-PWR-OFF-03` “全屋所有擦地机全关” → `simgrp_all_washer` / `set_power` `{"on":false}`
- `GRP-simgrp_all_vacuum-PWR-ON-01` “打开全屋所有吸尘器” → `simgrp_all_vacuum` / `set_power` `{"on":true}`
- `GRP-simgrp_all_vacuum-PWR-ON-02` “把全屋所有吸尘器都打开” → `simgrp_all_vacuum` / `set_power` `{"on":true}`
- `GRP-simgrp_all_vacuum-PWR-ON-03` “全屋所有吸尘器全开” → `simgrp_all_vacuum` / `set_power` `{"on":true}`
- `GRP-simgrp_all_vacuum-PWR-OFF-01` “关闭全屋所有吸尘器” → `simgrp_all_vacuum` / `set_power` `{"on":false}`
- `GRP-simgrp_all_vacuum-PWR-OFF-02` “把全屋所有吸尘器都关掉” → `simgrp_all_vacuum` / `set_power` `{"on":false}`
- `GRP-simgrp_all_vacuum-PWR-OFF-03` “全屋所有吸尘器全关” → `simgrp_all_vacuum` / `set_power` `{"on":false}`
- `GRP-simgrp_all_camera-PWR-ON-01` “打开全屋所有摄像机” → `simgrp_all_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_all_camera-PWR-ON-02` “把全屋所有摄像机都打开” → `simgrp_all_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_all_camera-PWR-ON-03` “全屋所有摄像机全开” → `simgrp_all_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_all_camera-PWR-OFF-01` “关闭全屋所有摄像机” → `simgrp_all_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_all_camera-PWR-OFF-02` “把全屋所有摄像机都关掉” → `simgrp_all_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_all_camera-PWR-OFF-03` “全屋所有摄像机全关” → `simgrp_all_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_all_aroma-PWR-ON-01` “打开全屋所有香薰机” → `simgrp_all_aroma` / `set_power` `{"on":true}`
- `GRP-simgrp_all_aroma-PWR-ON-02` “把全屋所有香薰机都打开” → `simgrp_all_aroma` / `set_power` `{"on":true}`
- `GRP-simgrp_all_aroma-PWR-ON-03` “全屋所有香薰机全开” → `simgrp_all_aroma` / `set_power` `{"on":true}`
- `GRP-simgrp_all_aroma-PWR-OFF-01` “关闭全屋所有香薰机” → `simgrp_all_aroma` / `set_power` `{"on":false}`
- `GRP-simgrp_all_aroma-PWR-OFF-02` “把全屋所有香薰机都关掉” → `simgrp_all_aroma` / `set_power` `{"on":false}`
- `GRP-simgrp_all_aroma-PWR-OFF-03` “全屋所有香薰机全关” → `simgrp_all_aroma` / `set_power` `{"on":false}`
- `GRP-simgrp_all_fan-PWR-ON-01` “打开全屋所有风扇” → `simgrp_all_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_all_fan-PWR-ON-02` “把全屋所有风扇都打开” → `simgrp_all_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_all_fan-PWR-ON-03` “全屋所有风扇全开” → `simgrp_all_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_all_fan-PWR-OFF-01` “关闭全屋所有风扇” → `simgrp_all_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_all_fan-PWR-OFF-02` “把全屋所有风扇都关掉” → `simgrp_all_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_all_fan-PWR-OFF-03` “全屋所有风扇全关” → `simgrp_all_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_all_audio-PWR-ON-01` “打开全屋所有影音配件” → `simgrp_all_audio` / `set_power` `{"on":true}`
- `GRP-simgrp_all_audio-PWR-ON-02` “把全屋所有影音配件都打开” → `simgrp_all_audio` / `set_power` `{"on":true}`
- `GRP-simgrp_all_audio-PWR-ON-03` “全屋所有影音配件全开” → `simgrp_all_audio` / `set_power` `{"on":true}`
- `GRP-simgrp_all_audio-PWR-OFF-01` “关闭全屋所有影音配件” → `simgrp_all_audio` / `set_power` `{"on":false}`
- `GRP-simgrp_all_audio-PWR-OFF-02` “把全屋所有影音配件都关掉” → `simgrp_all_audio` / `set_power` `{"on":false}`
- `GRP-simgrp_all_audio-PWR-OFF-03` “全屋所有影音配件全关” → `simgrp_all_audio` / `set_power` `{"on":false}`
- `GRP-simgrp_all_humidifier-PWR-ON-01` “打开全屋所有加湿器” → `simgrp_all_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_all_humidifier-PWR-ON-02` “把全屋所有加湿器都打开” → `simgrp_all_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_all_humidifier-PWR-ON-03` “全屋所有加湿器全开” → `simgrp_all_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_all_humidifier-PWR-OFF-01` “关闭全屋所有加湿器” → `simgrp_all_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_all_humidifier-PWR-OFF-02` “把全屋所有加湿器都关掉” → `simgrp_all_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_all_humidifier-PWR-OFF-03` “全屋所有加湿器全关” → `simgrp_all_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_all_blanket-PWR-ON-01` “打开全屋所有电热毯” → `simgrp_all_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_all_blanket-PWR-ON-02` “把全屋所有电热毯都打开” → `simgrp_all_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_all_blanket-PWR-ON-03` “全屋所有电热毯全开” → `simgrp_all_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_all_blanket-PWR-OFF-01` “关闭全屋所有电热毯” → `simgrp_all_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_all_blanket-PWR-OFF-02` “把全屋所有电热毯都关掉” → `simgrp_all_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_all_blanket-PWR-OFF-03` “全屋所有电热毯全关” → `simgrp_all_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_living_ac-PWR-ON-01` “打开客厅所有空调” → `simgrp_living_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_living_ac-PWR-ON-02` “把客厅所有空调都打开” → `simgrp_living_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_living_ac-PWR-ON-03` “客厅所有空调全开” → `simgrp_living_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_living_ac-PWR-OFF-01` “关闭客厅所有空调” → `simgrp_living_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_living_ac-PWR-OFF-02` “把客厅所有空调都关掉” → `simgrp_living_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_living_ac-PWR-OFF-03` “客厅所有空调全关” → `simgrp_living_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_living_ac-TEMP-20-01` “把客厅所有空调设为20度” → `simgrp_living_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_living_ac-TEMP-20-02` “客厅所有空调温度20” → `simgrp_living_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_living_ac-TEMP-20-03` “客厅的空调调到20度” → `simgrp_living_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_living_ac-TEMP-26-01` “把客厅所有空调设为26度” → `simgrp_living_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_living_ac-TEMP-26-02` “客厅所有空调温度26” → `simgrp_living_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_living_ac-TEMP-26-03` “客厅的空调调到26度” → `simgrp_living_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_living_ac-TEMP-30-01` “把客厅所有空调设为30度” → `simgrp_living_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_living_ac-TEMP-30-02` “客厅所有空调温度30” → `simgrp_living_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_living_ac-TEMP-30-03` “客厅的空调调到30度” → `simgrp_living_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_living_light-PWR-ON-01` “打开客厅所有灯” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-PWR-ON-02` “把客厅所有灯都打开” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-PWR-ON-03` “客厅所有灯全开” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-PWR-OFF-01` “关闭客厅所有灯” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-PWR-OFF-02` “把客厅所有灯都关掉” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-PWR-OFF-03` “客厅所有灯全关” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-ON-01` “开客厅灯” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-ON-02` “开客厅的灯” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-ON-03` “打开客厅照明” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-ON-04` “把客厅的灯打开” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-ON-05` “客厅开灯” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-ON-06` “点亮客厅所有灯” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-ON-07` “客厅灯都打开” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-ON-08` “麻烦开一下客厅的照明” → `simgrp_living_light` / `set_power` `{"on":true}`
- `GRP-simgrp_living_light-ROOM-OFF-01` “关客厅灯” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-OFF-02` “关客厅的灯” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-OFF-03` “关闭客厅照明” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-OFF-04` “把客厅的灯关掉” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-OFF-05` “客厅关灯” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-OFF-06` “熄灭客厅所有灯” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-OFF-07` “客厅灯都关掉” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_light-ROOM-OFF-08` “麻烦关一下客厅的照明” → `simgrp_living_light` / `set_power` `{"on":false}`
- `GRP-simgrp_living_socket-PWR-ON-01` “打开客厅所有插座” → `simgrp_living_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_living_socket-PWR-ON-02` “把客厅所有插座都打开” → `simgrp_living_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_living_socket-PWR-ON-03` “客厅所有插座全开” → `simgrp_living_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_living_socket-PWR-OFF-01` “关闭客厅所有插座” → `simgrp_living_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_living_socket-PWR-OFF-02` “把客厅所有插座都关掉” → `simgrp_living_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_living_socket-PWR-OFF-03` “客厅所有插座全关” → `simgrp_living_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_living_strip-PWR-ON-01` “打开客厅所有插排” → `simgrp_living_strip` / `set_power` `{"on":true}`
- `GRP-simgrp_living_strip-PWR-ON-02` “把客厅所有插排都打开” → `simgrp_living_strip` / `set_power` `{"on":true}`
- `GRP-simgrp_living_strip-PWR-ON-03` “客厅所有插排全开” → `simgrp_living_strip` / `set_power` `{"on":true}`
- `GRP-simgrp_living_strip-PWR-OFF-01` “关闭客厅所有插排” → `simgrp_living_strip` / `set_power` `{"on":false}`
- `GRP-simgrp_living_strip-PWR-OFF-02` “把客厅所有插排都关掉” → `simgrp_living_strip` / `set_power` `{"on":false}`
- `GRP-simgrp_living_strip-PWR-OFF-03` “客厅所有插排全关” → `simgrp_living_strip` / `set_power` `{"on":false}`
- `GRP-simgrp_living_speaker-PWR-ON-01` “打开客厅所有音箱” → `simgrp_living_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_living_speaker-PWR-ON-02` “把客厅所有音箱都打开” → `simgrp_living_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_living_speaker-PWR-ON-03` “客厅所有音箱全开” → `simgrp_living_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_living_speaker-PWR-OFF-01` “关闭客厅所有音箱” → `simgrp_living_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_living_speaker-PWR-OFF-02` “把客厅所有音箱都关掉” → `simgrp_living_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_living_speaker-PWR-OFF-03` “客厅所有音箱全关” → `simgrp_living_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_living_purifier-PWR-ON-01` “打开客厅所有空气净化器” → `simgrp_living_purifier` / `set_power` `{"on":true}`
- `GRP-simgrp_living_purifier-PWR-ON-02` “把客厅所有空气净化器都打开” → `simgrp_living_purifier` / `set_power` `{"on":true}`
- `GRP-simgrp_living_purifier-PWR-ON-03` “客厅所有空气净化器全开” → `simgrp_living_purifier` / `set_power` `{"on":true}`
- `GRP-simgrp_living_purifier-PWR-OFF-01` “关闭客厅所有空气净化器” → `simgrp_living_purifier` / `set_power` `{"on":false}`
- `GRP-simgrp_living_purifier-PWR-OFF-02` “把客厅所有空气净化器都关掉” → `simgrp_living_purifier` / `set_power` `{"on":false}`
- `GRP-simgrp_living_purifier-PWR-OFF-03` “客厅所有空气净化器全关” → `simgrp_living_purifier` / `set_power` `{"on":false}`
- `GRP-simgrp_living_washer-PWR-ON-01` “打开客厅所有擦地机” → `simgrp_living_washer` / `set_power` `{"on":true}`
- `GRP-simgrp_living_washer-PWR-ON-02` “把客厅所有擦地机都打开” → `simgrp_living_washer` / `set_power` `{"on":true}`
- `GRP-simgrp_living_washer-PWR-ON-03` “客厅所有擦地机全开” → `simgrp_living_washer` / `set_power` `{"on":true}`
- `GRP-simgrp_living_washer-PWR-OFF-01` “关闭客厅所有擦地机” → `simgrp_living_washer` / `set_power` `{"on":false}`
- `GRP-simgrp_living_washer-PWR-OFF-02` “把客厅所有擦地机都关掉” → `simgrp_living_washer` / `set_power` `{"on":false}`
- `GRP-simgrp_living_washer-PWR-OFF-03` “客厅所有擦地机全关” → `simgrp_living_washer` / `set_power` `{"on":false}`
- `GRP-simgrp_living_vacuum-PWR-ON-01` “打开客厅所有吸尘器” → `simgrp_living_vacuum` / `set_power` `{"on":true}`
- `GRP-simgrp_living_vacuum-PWR-ON-02` “把客厅所有吸尘器都打开” → `simgrp_living_vacuum` / `set_power` `{"on":true}`
- `GRP-simgrp_living_vacuum-PWR-ON-03` “客厅所有吸尘器全开” → `simgrp_living_vacuum` / `set_power` `{"on":true}`
- `GRP-simgrp_living_vacuum-PWR-OFF-01` “关闭客厅所有吸尘器” → `simgrp_living_vacuum` / `set_power` `{"on":false}`
- `GRP-simgrp_living_vacuum-PWR-OFF-02` “把客厅所有吸尘器都关掉” → `simgrp_living_vacuum` / `set_power` `{"on":false}`
- `GRP-simgrp_living_vacuum-PWR-OFF-03` “客厅所有吸尘器全关” → `simgrp_living_vacuum` / `set_power` `{"on":false}`
- `GRP-simgrp_living_camera-PWR-ON-01` “打开客厅所有摄像机” → `simgrp_living_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_living_camera-PWR-ON-02` “把客厅所有摄像机都打开” → `simgrp_living_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_living_camera-PWR-ON-03` “客厅所有摄像机全开” → `simgrp_living_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_living_camera-PWR-OFF-01` “关闭客厅所有摄像机” → `simgrp_living_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_living_camera-PWR-OFF-02` “把客厅所有摄像机都关掉” → `simgrp_living_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_living_camera-PWR-OFF-03` “客厅所有摄像机全关” → `simgrp_living_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_living_aroma-PWR-ON-01` “打开客厅所有香薰机” → `simgrp_living_aroma` / `set_power` `{"on":true}`
- `GRP-simgrp_living_aroma-PWR-ON-02` “把客厅所有香薰机都打开” → `simgrp_living_aroma` / `set_power` `{"on":true}`
- `GRP-simgrp_living_aroma-PWR-ON-03` “客厅所有香薰机全开” → `simgrp_living_aroma` / `set_power` `{"on":true}`
- `GRP-simgrp_living_aroma-PWR-OFF-01` “关闭客厅所有香薰机” → `simgrp_living_aroma` / `set_power` `{"on":false}`
- `GRP-simgrp_living_aroma-PWR-OFF-02` “把客厅所有香薰机都关掉” → `simgrp_living_aroma` / `set_power` `{"on":false}`
- `GRP-simgrp_living_aroma-PWR-OFF-03` “客厅所有香薰机全关” → `simgrp_living_aroma` / `set_power` `{"on":false}`
- `GRP-simgrp_living_fan-PWR-ON-01` “打开客厅所有风扇” → `simgrp_living_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_living_fan-PWR-ON-02` “把客厅所有风扇都打开” → `simgrp_living_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_living_fan-PWR-ON-03` “客厅所有风扇全开” → `simgrp_living_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_living_fan-PWR-OFF-01` “关闭客厅所有风扇” → `simgrp_living_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_living_fan-PWR-OFF-02` “把客厅所有风扇都关掉” → `simgrp_living_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_living_fan-PWR-OFF-03` “客厅所有风扇全关” → `simgrp_living_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_living_audio-PWR-ON-01` “打开客厅所有影音配件” → `simgrp_living_audio` / `set_power` `{"on":true}`
- `GRP-simgrp_living_audio-PWR-ON-02` “把客厅所有影音配件都打开” → `simgrp_living_audio` / `set_power` `{"on":true}`
- `GRP-simgrp_living_audio-PWR-ON-03` “客厅所有影音配件全开” → `simgrp_living_audio` / `set_power` `{"on":true}`
- `GRP-simgrp_living_audio-PWR-OFF-01` “关闭客厅所有影音配件” → `simgrp_living_audio` / `set_power` `{"on":false}`
- `GRP-simgrp_living_audio-PWR-OFF-02` “把客厅所有影音配件都关掉” → `simgrp_living_audio` / `set_power` `{"on":false}`
- `GRP-simgrp_living_audio-PWR-OFF-03` “客厅所有影音配件全关” → `simgrp_living_audio` / `set_power` `{"on":false}`
- `GRP-simgrp_master_ac-PWR-ON-01` “打开主卧所有空调” → `simgrp_master_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_master_ac-PWR-ON-02` “把主卧所有空调都打开” → `simgrp_master_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_master_ac-PWR-ON-03` “主卧所有空调全开” → `simgrp_master_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_master_ac-PWR-OFF-01` “关闭主卧所有空调” → `simgrp_master_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_master_ac-PWR-OFF-02` “把主卧所有空调都关掉” → `simgrp_master_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_master_ac-PWR-OFF-03` “主卧所有空调全关” → `simgrp_master_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_master_ac-TEMP-20-01` “把主卧所有空调设为20度” → `simgrp_master_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_master_ac-TEMP-20-02` “主卧所有空调温度20” → `simgrp_master_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_master_ac-TEMP-20-03` “主卧的空调调到20度” → `simgrp_master_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_master_ac-TEMP-26-01` “把主卧所有空调设为26度” → `simgrp_master_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_master_ac-TEMP-26-02` “主卧所有空调温度26” → `simgrp_master_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_master_ac-TEMP-26-03` “主卧的空调调到26度” → `simgrp_master_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_master_ac-TEMP-30-01` “把主卧所有空调设为30度” → `simgrp_master_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_master_ac-TEMP-30-02` “主卧所有空调温度30” → `simgrp_master_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_master_ac-TEMP-30-03` “主卧的空调调到30度” → `simgrp_master_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_master_light-PWR-ON-01` “打开主卧所有灯” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-PWR-ON-02` “把主卧所有灯都打开” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-PWR-ON-03` “主卧所有灯全开” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-PWR-OFF-01` “关闭主卧所有灯” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-PWR-OFF-02` “把主卧所有灯都关掉” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-PWR-OFF-03` “主卧所有灯全关” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-ON-01` “开主卧灯” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-ON-02` “开主卧的灯” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-ON-03` “打开主卧照明” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-ON-04` “把主卧的灯打开” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-ON-05` “主卧开灯” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-ON-06` “点亮主卧所有灯” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-ON-07` “主卧灯都打开” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-ON-08` “麻烦开一下主卧的照明” → `simgrp_master_light` / `set_power` `{"on":true}`
- `GRP-simgrp_master_light-ROOM-OFF-01` “关主卧灯” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-OFF-02` “关主卧的灯” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-OFF-03` “关闭主卧照明” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-OFF-04` “把主卧的灯关掉” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-OFF-05` “主卧关灯” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-OFF-06` “熄灭主卧所有灯” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-OFF-07` “主卧灯都关掉” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_light-ROOM-OFF-08` “麻烦关一下主卧的照明” → `simgrp_master_light` / `set_power` `{"on":false}`
- `GRP-simgrp_master_speaker-PWR-ON-01` “打开主卧所有音箱” → `simgrp_master_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_master_speaker-PWR-ON-02` “把主卧所有音箱都打开” → `simgrp_master_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_master_speaker-PWR-ON-03` “主卧所有音箱全开” → `simgrp_master_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_master_speaker-PWR-OFF-01` “关闭主卧所有音箱” → `simgrp_master_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_master_speaker-PWR-OFF-02` “把主卧所有音箱都关掉” → `simgrp_master_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_master_speaker-PWR-OFF-03` “主卧所有音箱全关” → `simgrp_master_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_master_fan-PWR-ON-01` “打开主卧所有风扇” → `simgrp_master_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_master_fan-PWR-ON-02` “把主卧所有风扇都打开” → `simgrp_master_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_master_fan-PWR-ON-03` “主卧所有风扇全开” → `simgrp_master_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_master_fan-PWR-OFF-01` “关闭主卧所有风扇” → `simgrp_master_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_master_fan-PWR-OFF-02` “把主卧所有风扇都关掉” → `simgrp_master_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_master_fan-PWR-OFF-03` “主卧所有风扇全关” → `simgrp_master_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_master_humidifier-PWR-ON-01` “打开主卧所有加湿器” → `simgrp_master_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_master_humidifier-PWR-ON-02` “把主卧所有加湿器都打开” → `simgrp_master_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_master_humidifier-PWR-ON-03` “主卧所有加湿器全开” → `simgrp_master_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_master_humidifier-PWR-OFF-01` “关闭主卧所有加湿器” → `simgrp_master_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_master_humidifier-PWR-OFF-02` “把主卧所有加湿器都关掉” → `simgrp_master_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_master_humidifier-PWR-OFF-03` “主卧所有加湿器全关” → `simgrp_master_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_master_blanket-PWR-ON-01` “打开主卧所有电热毯” → `simgrp_master_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_master_blanket-PWR-ON-02` “把主卧所有电热毯都打开” → `simgrp_master_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_master_blanket-PWR-ON-03` “主卧所有电热毯全开” → `simgrp_master_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_master_blanket-PWR-OFF-01` “关闭主卧所有电热毯” → `simgrp_master_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_master_blanket-PWR-OFF-02` “把主卧所有电热毯都关掉” → `simgrp_master_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_master_blanket-PWR-OFF-03` “主卧所有电热毯全关” → `simgrp_master_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_ac-PWR-ON-01` “打开次卧所有空调” → `simgrp_secondary_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_ac-PWR-ON-02` “把次卧所有空调都打开” → `simgrp_secondary_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_ac-PWR-ON-03` “次卧所有空调全开” → `simgrp_secondary_ac` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_ac-PWR-OFF-01` “关闭次卧所有空调” → `simgrp_secondary_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_ac-PWR-OFF-02` “把次卧所有空调都关掉” → `simgrp_secondary_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_ac-PWR-OFF-03` “次卧所有空调全关” → `simgrp_secondary_ac` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_ac-TEMP-20-01` “把次卧所有空调设为20度” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_secondary_ac-TEMP-20-02` “次卧所有空调温度20” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_secondary_ac-TEMP-20-03` “次卧的空调调到20度” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":20}`
- `GRP-simgrp_secondary_ac-TEMP-26-01` “把次卧所有空调设为26度” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_secondary_ac-TEMP-26-02` “次卧所有空调温度26” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_secondary_ac-TEMP-26-03` “次卧的空调调到26度” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":26}`
- `GRP-simgrp_secondary_ac-TEMP-30-01` “把次卧所有空调设为30度” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_secondary_ac-TEMP-30-02` “次卧所有空调温度30” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_secondary_ac-TEMP-30-03` “次卧的空调调到30度” → `simgrp_secondary_ac` / `set_temperature` `{"celsius":30}`
- `GRP-simgrp_secondary_light-PWR-ON-01` “打开次卧所有灯” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-PWR-ON-02` “把次卧所有灯都打开” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-PWR-ON-03` “次卧所有灯全开” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-PWR-OFF-01` “关闭次卧所有灯” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-PWR-OFF-02` “把次卧所有灯都关掉” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-PWR-OFF-03` “次卧所有灯全关” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-ON-01` “开次卧灯” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-ON-02` “开次卧的灯” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-ON-03` “打开次卧照明” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-ON-04` “把次卧的灯打开” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-ON-05` “次卧开灯” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-ON-06` “点亮次卧所有灯” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-ON-07` “次卧灯都打开” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-ON-08` “麻烦开一下次卧的照明” → `simgrp_secondary_light` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_light-ROOM-OFF-01` “关次卧灯” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-OFF-02` “关次卧的灯” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-OFF-03` “关闭次卧照明” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-OFF-04` “把次卧的灯关掉” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-OFF-05` “次卧关灯” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-OFF-06` “熄灭次卧所有灯” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-OFF-07` “次卧灯都关掉” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_light-ROOM-OFF-08` “麻烦关一下次卧的照明” → `simgrp_secondary_light` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_speaker-PWR-ON-01` “打开次卧所有音箱” → `simgrp_secondary_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_speaker-PWR-ON-02` “把次卧所有音箱都打开” → `simgrp_secondary_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_speaker-PWR-ON-03` “次卧所有音箱全开” → `simgrp_secondary_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_speaker-PWR-OFF-01` “关闭次卧所有音箱” → `simgrp_secondary_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_speaker-PWR-OFF-02` “把次卧所有音箱都关掉” → `simgrp_secondary_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_speaker-PWR-OFF-03` “次卧所有音箱全关” → `simgrp_secondary_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_camera-PWR-ON-01` “打开次卧所有摄像机” → `simgrp_secondary_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_camera-PWR-ON-02` “把次卧所有摄像机都打开” → `simgrp_secondary_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_camera-PWR-ON-03` “次卧所有摄像机全开” → `simgrp_secondary_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_camera-PWR-OFF-01` “关闭次卧所有摄像机” → `simgrp_secondary_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_camera-PWR-OFF-02` “把次卧所有摄像机都关掉” → `simgrp_secondary_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_camera-PWR-OFF-03` “次卧所有摄像机全关” → `simgrp_secondary_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_fan-PWR-ON-01` “打开次卧所有风扇” → `simgrp_secondary_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_fan-PWR-ON-02` “把次卧所有风扇都打开” → `simgrp_secondary_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_fan-PWR-ON-03` “次卧所有风扇全开” → `simgrp_secondary_fan` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_fan-PWR-OFF-01` “关闭次卧所有风扇” → `simgrp_secondary_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_fan-PWR-OFF-02` “把次卧所有风扇都关掉” → `simgrp_secondary_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_fan-PWR-OFF-03` “次卧所有风扇全关” → `simgrp_secondary_fan` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_humidifier-PWR-ON-01` “打开次卧所有加湿器” → `simgrp_secondary_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_humidifier-PWR-ON-02` “把次卧所有加湿器都打开” → `simgrp_secondary_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_humidifier-PWR-ON-03` “次卧所有加湿器全开” → `simgrp_secondary_humidifier` / `set_power` `{"on":true}`
- `GRP-simgrp_secondary_humidifier-PWR-OFF-01` “关闭次卧所有加湿器” → `simgrp_secondary_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_humidifier-PWR-OFF-02` “把次卧所有加湿器都关掉” → `simgrp_secondary_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_secondary_humidifier-PWR-OFF-03` “次卧所有加湿器全关” → `simgrp_secondary_humidifier` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-PWR-ON-01` “打开厨房所有灯” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-PWR-ON-02` “把厨房所有灯都打开” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-PWR-ON-03` “厨房所有灯全开” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-PWR-OFF-01` “关闭厨房所有灯” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-PWR-OFF-02` “把厨房所有灯都关掉” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-PWR-OFF-03` “厨房所有灯全关” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-ON-01` “开厨房灯” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-ON-02` “开厨房的灯” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-ON-03` “打开厨房照明” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-ON-04` “把厨房的灯打开” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-ON-05` “厨房开灯” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-ON-06` “点亮厨房所有灯” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-ON-07` “厨房灯都打开” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-ON-08` “麻烦开一下厨房的照明” → `simgrp_kitchen_light` / `set_power` `{"on":true}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-01` “关厨房灯” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-02` “关厨房的灯” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-03` “关闭厨房照明” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-04` “把厨房的灯关掉” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-05` “厨房关灯” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-06` “熄灭厨房所有灯” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-07` “厨房灯都关掉” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_kitchen_light-ROOM-OFF-08` “麻烦关一下厨房的照明” → `simgrp_kitchen_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-PWR-ON-01` “打开厕所所有灯” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-PWR-ON-02` “把厕所所有灯都打开” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-PWR-ON-03` “厕所所有灯全开” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-PWR-OFF-01` “关闭厕所所有灯” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-PWR-OFF-02` “把厕所所有灯都关掉” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-PWR-OFF-03` “厕所所有灯全关” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-ON-01` “开厕所灯” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-ON-02` “开厕所的灯” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-ON-03` “打开厕所照明” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-ON-04` “把厕所的灯打开” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-ON-05` “厕所开灯” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-ON-06` “点亮厕所所有灯” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-ON-07` “厕所灯都打开” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-ON-08` “麻烦开一下厕所的照明” → `simgrp_toilet_light` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_light-ROOM-OFF-01` “关厕所灯” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-OFF-02` “关厕所的灯” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-OFF-03` “关闭厕所照明” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-OFF-04` “把厕所的灯关掉” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-OFF-05` “厕所关灯” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-OFF-06` “熄灭厕所所有灯” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-OFF-07` “厕所灯都关掉” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_light-ROOM-OFF-08` “麻烦关一下厕所的照明” → `simgrp_toilet_light` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_socket-PWR-ON-01` “打开厕所所有插座” → `simgrp_toilet_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_socket-PWR-ON-02` “把厕所所有插座都打开” → `simgrp_toilet_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_socket-PWR-ON-03` “厕所所有插座全开” → `simgrp_toilet_socket` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_socket-PWR-OFF-01` “关闭厕所所有插座” → `simgrp_toilet_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_socket-PWR-OFF-02` “把厕所所有插座都关掉” → `simgrp_toilet_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_socket-PWR-OFF-03` “厕所所有插座全关” → `simgrp_toilet_socket` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_speaker-PWR-ON-01` “打开厕所所有音箱” → `simgrp_toilet_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_speaker-PWR-ON-02` “把厕所所有音箱都打开” → `simgrp_toilet_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_speaker-PWR-ON-03` “厕所所有音箱全开” → `simgrp_toilet_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_toilet_speaker-PWR-OFF-01` “关闭厕所所有音箱” → `simgrp_toilet_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_speaker-PWR-OFF-02` “把厕所所有音箱都关掉” → `simgrp_toilet_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_toilet_speaker-PWR-OFF-03` “厕所所有音箱全关” → `simgrp_toilet_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-PWR-ON-01` “打开小工具所有灯” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-PWR-ON-02` “把小工具所有灯都打开” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-PWR-ON-03` “小工具所有灯全开” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-PWR-OFF-01` “关闭小工具所有灯” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-PWR-OFF-02` “把小工具所有灯都关掉” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-PWR-OFF-03` “小工具所有灯全关” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-ON-01` “开小工具灯” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-ON-02` “开小工具的灯” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-ON-03` “打开小工具照明” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-ON-04` “把小工具的灯打开” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-ON-05` “小工具开灯” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-ON-06` “点亮小工具所有灯” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-ON-07` “小工具灯都打开” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-ON-08` “麻烦开一下小工具的照明” → `simgrp_utility_light` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_light-ROOM-OFF-01` “关小工具灯” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-OFF-02` “关小工具的灯” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-OFF-03` “关闭小工具照明” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-OFF-04` “把小工具的灯关掉” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-OFF-05` “小工具关灯” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-OFF-06` “熄灭小工具所有灯” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-OFF-07` “小工具灯都关掉” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_light-ROOM-OFF-08` “麻烦关一下小工具的照明” → `simgrp_utility_light` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_speaker-PWR-ON-01` “打开小工具所有音箱” → `simgrp_utility_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_speaker-PWR-ON-02` “把小工具所有音箱都打开” → `simgrp_utility_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_speaker-PWR-ON-03` “小工具所有音箱全开” → `simgrp_utility_speaker` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_speaker-PWR-OFF-01` “关闭小工具所有音箱” → `simgrp_utility_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_speaker-PWR-OFF-02` “把小工具所有音箱都关掉” → `simgrp_utility_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_speaker-PWR-OFF-03` “小工具所有音箱全关” → `simgrp_utility_speaker` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_blanket-PWR-ON-01` “打开小工具所有电热毯” → `simgrp_utility_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_blanket-PWR-ON-02` “把小工具所有电热毯都打开” → `simgrp_utility_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_blanket-PWR-ON-03` “小工具所有电热毯全开” → `simgrp_utility_blanket` / `set_power` `{"on":true}`
- `GRP-simgrp_utility_blanket-PWR-OFF-01` “关闭小工具所有电热毯” → `simgrp_utility_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_blanket-PWR-OFF-02` “把小工具所有电热毯都关掉” → `simgrp_utility_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_utility_blanket-PWR-OFF-03` “小工具所有电热毯全关” → `simgrp_utility_blanket` / `set_power` `{"on":false}`
- `GRP-simgrp_garden_camera-PWR-ON-01` “打开菜地所有摄像机” → `simgrp_garden_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_garden_camera-PWR-ON-02` “把菜地所有摄像机都打开” → `simgrp_garden_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_garden_camera-PWR-ON-03` “菜地所有摄像机全开” → `simgrp_garden_camera` / `set_power` `{"on":true}`
- `GRP-simgrp_garden_camera-PWR-OFF-01` “关闭菜地所有摄像机” → `simgrp_garden_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_garden_camera-PWR-OFF-02` “把菜地所有摄像机都关掉” → `simgrp_garden_camera` / `set_power` `{"on":false}`
- `GRP-simgrp_garden_camera-PWR-OFF-03` “菜地所有摄像机全关” → `simgrp_garden_camera` / `set_power` `{"on":false}`

## 6. 语音测试扩展与验收标准

### 6.1 语句变体

每一条正例至少扩展以下形式，预期结构不变。第 5 节还为每台设备给出经逐动作校验的口语变体：

| 表达类型 | 例句 |
| --- | --- |
| 直接指令 | 关闭客厅吸顶灯 |
| 礼貌请求 | 麻烦帮我把客厅吸顶灯关一下 |
| 简略命令 | 客厅吸顶灯，关掉 |
| 口语修饰 | 那个客厅吸顶灯给我关了吧 |
| 英文 | Turn off the living room ceiling light. |
| 中英混合 | 把客厅吸顶灯 turn off |
| 参数口语 | 厨房窗帘开一半；主卧空调二十六度 |
| 指令加闲话 | 有点刺眼，帮我把客厅吸顶灯关掉 |

口语变化要跨越语序、动词、省略、数字读法和礼貌程度，不能只替换“请/帮我”。完整温控等价集（以下正例均为 `sim87_living_ac_001 / set_temperature / {celsius:26}`）：

| 变化方向 | 例句 |
| --- | --- |
| 正式动作 | 把客厅空调温度设为26度；将客厅空调调至26摄氏度 |
| 省去动词 | 客厅空调温度26；客厅空调二十六度；客厅那台空调，26 |
| 省去“温度” | 客厅的空调设为26；客厅空调给我调26；客厅空调弄成26 |
| 宾语前置 | 26度，客厅空调；二十六，客厅那台空调 |
| 口语谓词 | 客厅空调温控拨到26；客厅空调降到26；客厅空调别那么热，26度 |
| 单位与数字 | 客厅空调调26℃；客厅空调调二十六度；客厅空调调到26度整 |
| 礼貌与填充词 | 麻烦把客厅那个空调调成26呗；嗯，客厅的空调，调到26就行 |
| 房间代称，仅有唯一可写目标时 | 客厅的温度改成26；客厅温度给我调到26度 |

“客厅的温度改成26”没有说空调，但本夹具中客厅仅有一台可写 `set_temperature` 的设备，因此可推断为空调；客厅温湿度计是只读设备，不是候选。相反，“主卧的温度改成26”同时可能指主卧空调或主卧水暖垫，标注为 `needs_input/ambiguous_target`。没有明确修改动词的“客厅温度26”可能是陈述，不作为执行正例；“客厅温度多少”是查询，不能混入调温正例。显式说“空调”的“主卧空调温度26”可消除歧义。“客厅空调开到26”同时包含开机，标注两个有序动作：`set_power(on=true)`、`set_temperature(celsius=26)`。

不同动作也要做语序和省略变化：

| 目标动作 | 等价说法（同一行语义一致） |
| --- | --- |
| 客厅吸顶灯关 | 关了客厅灯；客厅的吸顶灯灭掉；客厅灯给我关一下；客厅那盏灯别亮了 |
| 厨房窗帘开至50% | 厨房窗帘开一半；厨房的窗帘拉到中间；厨房窗帘打开百分之五十 |
| 次卧白色风扇3档 | 次卧白色风扇三档；白色风扇在次卧，风速开三档；次卧那台白色电风扇调到3挡 |
| 主卧净化加湿器目标50% | 主卧净化加湿器目标湿度五十；把主卧那台净化加湿器设成百分之五十 |
| 关闭所有空调 | 全屋空调都关掉；把每个房间的空调关了；三台空调全关 |
| 关闭主卧所有灯 | 主卧的灯全灭；主卧两盏灯都关上；把主卧所有照明灯关了（仅指 light 类型） |

同一目标还需对照非指令：“客厅空调是不是26度？”是查询；“如果客厅空调到26度就好了”是愿望；“昨天客厅空调设了26”是历史；“别把客厅空调设26”是否定；“客厅空调……不对，次卧空调26”最终只改次卧。评分时必须成对覆盖，不能只看命中关键词。

英文房间固定映射：living room=客厅，master bedroom=主卧，second bedroom=次卧，kitchen=厨房，bathroom/toilet=厕所，entrance=门厅，balcony=阳台，utility=小工具，dining room=餐厅，garden=菜地。“bedroom/卧室”不自动等于主卧。

英文设备类型按本表动作模板映射，多个同类仍需额外限定。例如 master bedroom purifier humidifier=主卧净化加湿器，master bedroom mist-free humidifier=主卧无雾加湿器；second bedroom 4C camera / 3 Pro camera；kitchen downlight number two / four、kitchen occupancy downlight、kitchen spotlight number three。不允许用完全相同英文称呼标注为两个不同设备的确定正例。

### 6.2 每台设备必须配套的负例

1. 否定：“不要关闭客厅吸顶灯”→ ignore/negated。
2. 引用：“他说‘关闭客厅吸顶灯’”→ ignore/quoted。
3. 历史：“我昨天关闭了客厅吸顶灯”→ ignore/past_event。
4. 能力询问：“客厅吸顶灯能调色温吗”→ ignore/capability_question，不执行调色温。
5. 状态查询：“客厅吸顶灯现在开着吗”→ execute/get_state，与能力询问区分。
6. 不完整：“把主卧空调温度调到”→ needs_input/missing_parameter。
7. 歧义：“打开主卧音箱”→ needs_input/ambiguous_target，因为有 AI 音箱和触屏音箱。
8. 纠正：“打开次卧白色风扇，不对，关掉”→ 仅 set_power(false)，同一最终语句先完成解析再执行。
9. 条件：“如果热了就开主卧空调”→ ignore/unsupported_condition，不马上开机。
10. 不存在：“打开书房空调”→ ignore/unknown_target，不用主卧或客厅空调替代。
11. 撤回：“把客厅吸顶灯打开，算了别开了”→ ignore/cancelled，不留下首次开灯副作用。
12. 多轮代词：“把它关掉”只有 context.last_target 唯一且仍有效时才解析，否则 needs_input。有效期在用例中固定为30秒。

### 6.3 数值、组合和重复覆盖

- 每个整数参数：min、min+1、中间值、max-1、max；非法 min-1、max+1、小数、缺失、NaN、错误单位。参数未允许小数时不自动四舍五入。
- 每个枚举：全部合法取值、一个不存在的值；每个布尔值 true/false；通道逐路测试，不只测试第一路。
- RGB 解析接受 #RRGGBB 或具名颜色表。任意合法 RGB 可构造；拒绝不合法十六进制、缺位和超出0..255的通道。不把“暖白”误当 RGB 白色。
- 双参数动作按合法域生成笛卡尔组合或成对覆盖；微波炉用不同火力与秒数交叉，不只沿用表中5个示例配对。
- 状态依赖用例显式记录 initial_state。例如灯当前95，调亮10后为100；灯当前5，调暗10后关灯；窗帘停止不改变已有位置。
- 群组覆盖全屋组、房间组、单成员组、截图虚拟灯组、重叠组和不存在的组。例：“关闭厨房基础灯组和厨房所有灯”只对4个唯一叶子灯各执行一次。
- 复合指令先完全解析，再验证所有动作。例如“主卧空调开机制冷26度”三步都正确才提交；“关客厅灯并把空调设为100度”整条不执行，防止部分副作用。
- 用固定 utterance_id / request_id 测试幂等：同一 final ASR 的网络重试只执行一次；用户重新说相同指令但新 request_id，应允许再次执行。出粮等非幂等动作尤其需要此测试。
- 语义分组后划分数据集，再合成多个 TTS 声音；同一文本、翻译、近义改写及其音频变体不跨训练/调试/最终测试分区泄漏。

### 6.4 标注字段和判分

每条用例至少包含 text_id、semantic_group、language、text、registry_version、context（默认房间/时间/历史/初始状态）、expected_decision、expected_reason、expected_actions（target_id/action/parameters）、expected_leaf_ids、expected_final_state。负例的 expected_actions 为空。音频另记 audio_path、tts_engine、voice、rate，不改变语义标签。

分别统计：是否应执行、业务意图、目标设备/组、动作、参数、展开后的叶子集合、最终状态、误执行率。负例误执行单独报告，不能被大量简单开关正例掩盖。多目标指令要求集合完全匹配，选少或多选都不能算全对。

## 7. 范围边界

本规范对模拟集提供完整封闭动作集合；真实接入仍需独立映射验证。截图无法确定的型号不补全，灯组成员与多路通道均为测试设定。无线开关关联哪个灯、插座控制哪个负载、摄像机物理云台能力、真实音箱媒体服务等均不自动推断。

本次不改变安卓应用现有配置，不接入设备，不生成TTS音频；本文可直接交给另一个AI用来生成有标准答案的语音测试集和模拟器。

生成校验：87 条设备/组详情；85 个叶子设备；101 个派生分组；803 条逐动作正例表达；610 条逐设备口语变体；87 条设备专属负例；432 条分组口语用例。
