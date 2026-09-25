# 家庭设备 ID 与控制协议

版本：v0.1，2026-09-24。来源：用户提供的米家极客版设备截图。

本文定义助手自己的设备标识与控制参数，供 Jev 判断、App 展示和执行器映射使用。用户已授权自行设计这些规则；表中的能力是设计模板，并非对每台设备硬件能力的确认。无需等待真实物理 ID 即可开发观察模式。

## 1. ID 规则

格式：`h01_<room>_<type>_<序号>`，例如 `h01_living_light_001`。序号使用三位数字，同家庭内不重复使用已删除设备的 ID。

房间编码：客厅 `living`、主卧 `master`、次卧 `secondary`、厨房 `kitchen`、餐厅 `dining`、厕所 `toilet`、门厅 `entry`、阳台 `balcony`、小工具 `utility`、菜地 `garden`、未确定 `unassigned`。原始房间名称另存，不因模板命名擅自更改。

类型编码示例：灯 `light`、空调 `ac`、风扇 `fan`、窗帘 `curtain`、插座 `outlet`、有线开关 `switch`、无线遥控器 `remote`、传感器 `sensor`。

房间和类型只用于首次生成可读 ID；以后设备改名或移动房间仍保持原 ID，更新对应字段。换成另一台物理设备则创建新 ID，不能悄悄复用旧映射。设备组另用 `group_h01_living_lights` 等 ID，由注册表动态展开。

米家物理 ID 保存在 `source_device_id`，未接入时为 `null`。执行器不得将逻辑 ID 冒充物理 ID 调用米家接口。

## 2. 标准动作与参数模板

单位和方向在本协议统一，执行器负责转换。仅将某设备已配置的能力提供给 Jev；真实执行还要求这些能力通过映射验证。

| 类型 | 自定义动作与参数 | 约束/说明 |
| --- | --- | --- |
| 吸顶灯、筒灯、台灯、灯组 | `set_power(on: boolean)`；`set_brightness(percent: 1..100)`；`set_color_temperature(kelvin: integer)` | 调光/色温为可选；色温范围必须按型号配置，不统一猜测 |
| 空调 | `set_power(on)`；`set_mode(mode)`；`set_temperature(celsius: number)`；`set_fan_speed(level)` | 候选模式 cool/heat/auto/dry/fan，逐设备取支持子集；温度范围与步长逐型号确定 |
| 电风扇、循环扇 | `set_power(on)`；`set_speed(percent: 1..100)`；`set_oscillation(enabled)` | 百分比是本系统标准值；只有确认档位换算后才能映射，回报实际档位 |
| 窗帘 | `open()`；`close()`；`stop()`；`set_position(open_percent: 0..100)` | 0 全关、100 全开；校验来源百分比方向，定位能力可选 |
| 晾衣架 | `raise()`；`lower()`；`stop()`；`set_light(on)` | 照明及其他功能按能力存在情况提供，运动操作单独验证 |
| 插座、插排、有线开关 | `set_power(channel: string, on: boolean)` | 通道是注册表枚举，如 left/right；不能由模型自由造通道；无功率控制能力的无线开关不适用 |
| 净化器 | `set_power(on)`；`set_mode(mode)`；`set_speed(percent)` | 模式与速度映射逐设备配置 |
| 加湿器 | `set_power(on)`；`set_target_humidity(percent: number)`；`set_mode(mode)` | 湿度范围和档位不能全设备共用一个猜测值 |
| 浴霸 | `set_light(on)`；`set_ventilation(on)`；`set_heating(on)` | 三项独立能力；热功能默认观察模式，接入后按白名单开放 |
| 电暖器、暖风机 | `set_power(on)`；`set_temperature(celsius)`；`set_mode(mode)` | 温控和模式可选，实际自动执行单独配置 |
| 音箱、音频连接器 | `pause()`；`resume()`；`set_volume(percent: 0..100)` | 是否可通过所选集成调用需验证；首版不加入开放式音乐检索 |
| 扫地机器人 | `start_clean()`；`pause()`；`return_to_dock()` | 分区清扫后续加入，需要真实地图/区域 ID |
| 宠物喂食器 | `dispense(portions: integer)` | 份数上限按型号及用户配置确定；未验证时禁止下发 |
| 微波炉 | 先只定义状态读取 | 加热启动、时长和火力映射另行接入，不因截图名称推定可远程启动 |
| 摄像机、门铃、可视门铃 | 可选 `set_privacy(enabled)`，其他先只读状态 | 不采集或上传视频；隐私开关方向和实际支持必须验证 |
| 门锁 | 只读门锁/电量状态 | 首版不提供开锁动作 |
| 人体/存在/压力传感器 | 只读 `occupied` 或 `pressed` | 按实际语义映射，不能互相替代 |
| 温湿度、烟雾、漏水传感器 | 只读温度、湿度、烟雾告警、漏水告警 | 仅提供设备实际拥有的字段 |
| 遥控器、无线开关 | 输入事件 `single / double / long_press` 等 | 事件枚举取来源支持子集，不假设可反向控制 |
| 路由器、网关、中枢、控制面板 | 首版只读在线状态和设备信息 | 不默认开放重启或网络设置 |
| 储能电源 | 首版只读电量/功率等已支持信息 | 输出开关等后续映射，不依据图标推断 |
| 体重秤/体脂秤 | 不提供控制动作 | 首版不采集测量历史 |

截图中被截断或无法确定类别的设备先标为 `unclassified`，保留可见名称，不能为了填满表格自动分配可执行能力。

## 3. 逻辑目录示例

以下为截图中可读名称的自定义 ID 示例，不是完整设备清单；所有示例尚未连接物理设备。

| 逻辑 ID | 名称 | 截图房间 | 模板 |
| --- | --- | --- | --- |
| `h01_secondary_fan_001` | 白色电风扇 | 次卧 | 风扇 |
| `h01_kitchen_curtain_001` | 米家智能窗帘2 | 厨房 | 窗帘 |
| `h01_living_light_001` | 米家吸顶灯 | 客厅 | 灯 |
| `h01_balcony_drying_rack_001` | 米家智能晾衣机 | 阳台 | 晾衣架 |
| `h01_entry_lock_001` | 小米智能门锁 5 Max | 门厅 | 门锁只读 |

每台目录记录具有 `mapping_status = draft / mapped / verified / disabled`。观察模式可用 draft 的标准参数模拟判断，但页面必须写“计划/未接入”；只有 verified 且允许执行的能力才会下发。

## 4. 指令示例

```json
{
  "device_id": "h01_living_light_001",
  "action": "set_power",
  "parameters": {"on": false},
  "registry_version": 1
}
```

适配器处理顺序：找到设备 → 验证能力与参数 → 检查实际映射 → 转成来源系统属性/动作 → 下发 → 回读或报告未确认。没有映射时返回 `device_not_connected`；缺少能力时返回 `unsupported_capability`，不能声称执行成功。

观察模式与真实模式使用相同的逻辑 ID、动作和参数。后续更换米家接入方式时，仅替换映射与执行适配器，App 和 Jev 协议保持一致。
