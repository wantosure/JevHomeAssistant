import json
from pathlib import Path

rooms = [
    {"id": "base", "name": "基地", "area": 25},
    {"id": "living", "name": "客厅", "area": 35},
    {"id": "kitchen", "name": "厨房", "area": 12},
    {"id": "guest", "name": "次卧", "area": 16},
    {"id": "master", "name": "主卧", "area": 20},
    {"id": "entry", "name": "门厅", "area": 8},
    {"id": "tools", "name": "小工具", "area": 6},
    {"id": "backend", "name": "后台", "area": 10},
    {"id": "untested", "name": "未测", "area": 15},
]

# 87 台真实设备列表
raw_devices = [
    # 完全可用 (Row 1)
    {"name": "白色电风扇", "room": "次卧", "type": "风扇", "profile": "fan", "icon": "❋"},
    {"name": "Yeelight 智能浴霸 S20", "room": "厨房", "type": "浴霸", "profile": "bath_heater", "icon": "♨"},
    {"name": "小米智能多模网关2", "room": "客厅", "type": "网关", "profile": "gateway", "icon": "☵"},
    {"name": "小米小爱音箱Play 增强版", "room": "厨房", "type": "音箱", "profile": "speaker", "icon": "♫"},
    {"name": "热水", "room": "厨房", "type": "插座", "profile": "socket", "icon": "⚡"},
    {"name": "米家智能晾衣机", "room": "厨房", "type": "晾衣架", "profile": "clothes_dryer", "icon": "☲"},
    {"name": "米家新风空调（尊享版）", "room": "主卧", "type": "空调", "profile": "ac", "icon": "❄"},
    {"name": "米家新风空调立式（3匹）", "room": "客厅", "type": "空调", "profile": "ac", "icon": "❄"},
    {"name": "米家新风空调（尊享版）-次卧", "room": "次卧", "type": "空调", "profile": "ac", "icon": "❄"},
    {"name": "小米智能中控屏", "room": "门厅", "type": "控制面板", "profile": "panel", "icon": "🔲"},

    # 完全可用 (Row 2)
    {"name": "米家智能窗帘2", "room": "基地", "type": "窗帘", "profile": "curtain", "icon": "▥"},
    {"name": "米家无线洗地机4 Max", "room": "客厅", "type": "洗地机", "profile": "vacuum", "icon": "🧹"},
    {"name": "Xiaomi 智能音箱", "room": "次卧", "type": "音箱", "profile": "speaker", "icon": "♫"},
    {"name": "米家无线吸尘器3 显微版", "room": "客厅", "type": "吸尘器", "profile": "vacuum", "icon": "🧹"},
    {"name": "米家吸顶灯Pro 超薄版", "room": "次卧", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "米家吸顶灯", "room": "客厅", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "Xiaomi Sound 2 Max", "room": "客厅", "type": "音箱", "profile": "speaker", "icon": "♫"},
    {"name": "小米智能开关Pro（三开）", "room": "客厅", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "小米智能开关Pro（双开）-次卧", "room": "次卧", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "小米智能开关Pro（双开）-客厅", "room": "客厅", "type": "开关", "profile": "switch", "icon": "◫"},

    # 完全可用 (Row 3)
    {"name": "厕所灯控", "room": "厨房", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "落地插座", "room": "未测", "type": "插座", "profile": "socket", "icon": "⚡"},
    {"name": "PTX 智能六键开关", "room": "未测", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "子擎存在传感器 Lite", "room": "厨房", "type": "存在传感器", "profile": "sensor", "icon": "👁"},
    {"name": "视频", "room": "门厅", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "屏快", "room": "门厅", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "小米智能中控屏 Max 2", "room": "后台", "type": "控制面板", "profile": "panel", "icon": "🔲"},
    {"name": "领普人体存在传感器 S1", "room": "厨房", "type": "存在传感器", "profile": "sensor", "icon": "👁"},
    {"name": "小米智能摄像机 4C 300万", "room": "次卧", "type": "摄像机", "profile": "camera", "icon": "📷"},
    {"name": "WantS_2.4G", "room": "后台", "type": "路由器", "profile": "router", "icon": "📶"},

    # 完全可用 (Row 4)
    {"name": "小米智能门锁 5 Max", "room": "门厅", "type": "门锁", "profile": "lock", "icon": "🔒"},
    {"name": "小米智能摄像机 5 Pro", "room": "门厅", "type": "摄像机", "profile": "camera", "icon": "📷"},
    {"name": "小米智能开关（双开）", "room": "厨房", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "米家空气净化器 5S 内循环", "room": "客厅", "type": "空气净化器", "profile": "purifier", "icon": "༄"},
    {"name": "叮零智能宠物喂食器", "room": "客厅", "type": "宠物喂食器", "profile": "pet_feeder", "icon": "🥣"},
    {"name": "米家显示器挂灯2", "room": "主卧", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "小米智能开关2（双开）", "room": "主卧", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "米家净化加湿器3 Pro", "room": "主卧", "type": "加湿器", "profile": "humidifier", "icon": "💧"},
    {"name": "米家筒灯3 Pro-2", "room": "次卧", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "米家筒灯3 Pro-4", "room": "次卧", "type": "灯具", "profile": "light", "icon": "◉"},

    # 完全可用 (Row 5)
    {"name": "小米充电宝 Pro 25000", "room": "小工具", "type": "插座", "profile": "socket", "icon": "⚡"},
    {"name": "米家筒灯3 Pro 人在亮", "room": "次卧", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "米家射灯3 Pro-3", "room": "次卧", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "人体传感器2", "room": "主卧", "type": "传感器", "profile": "sensor", "icon": "👁"},
    {"name": "小米电子温湿度计", "room": "客厅", "type": "传感器", "profile": "sensor", "icon": "🌡"},
    {"name": "温湿度传感器2", "room": "客厅", "type": "传感器", "profile": "sensor", "icon": "🌡"},
    {"name": "领普压力有感传感器", "room": "主卧", "type": "压力传感器", "profile": "sensor", "icon": "⚖"},
    {"name": "宝宝温控", "room": "主卧", "type": "传感器", "profile": "sensor", "icon": "🌡"},
    {"name": "宝宝温湿度", "room": "次卧", "type": "传感器", "profile": "sensor", "icon": "🌡"},
    {"name": "次卧灯开关", "room": "次卧", "type": "开关", "profile": "switch", "icon": "◫"},

    # 完全可用 (Row 6)
    {"name": "B", "room": "门厅", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "小米智能无线开关（双键版）", "room": "客厅", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "米家八电极体脂秤 S800", "room": "小工具", "type": "体脂秤", "profile": "scale", "icon": "⚖"},
    {"name": "小米智能卫士2", "room": "厨房", "type": "传感器", "profile": "sensor", "icon": "🚨"},
    {"name": "床灯", "room": "主卧", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "灯组", "room": "厨房", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "米家筒灯3pro", "room": "厨房", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "无线开关", "room": "厨房", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "无线开关2", "room": "门厅", "type": "开关", "profile": "switch", "icon": "◫"},
    {"name": "电视开关", "room": "客厅", "type": "开关", "profile": "switch", "icon": "◫"},

    # 完全可用 (Row 7)
    {"name": "自空灯", "room": "后台", "type": "灯具", "profile": "light", "icon": "◉"},

    # 部分可用 (Row 7 后续)
    {"name": "雅克风扇2", "room": "客厅", "type": "风扇", "profile": "fan", "icon": "❋"},
    {"name": "插排", "room": "客厅", "type": "插排", "profile": "socket", "icon": "⚡"},
    {"name": "小米AI音箱", "room": "主卧", "type": "音箱", "profile": "speaker", "icon": "♫"},
    {"name": "窗帘", "room": "主卧", "type": "窗帘", "profile": "curtain", "icon": "▥"},
    {"name": "主卧吸顶灯", "room": "主卧", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "AI智能窗帘电机", "room": "客厅", "type": "窗帘", "profile": "curtain", "icon": "▥"},
    {"name": "米家微波炉", "room": "厨房", "type": "微波炉", "profile": "microwave", "icon": "♨"},
    {"name": "米家智能直流变频落地扇", "room": "主卧", "type": "风扇", "profile": "fan", "icon": "❋"},
    {"name": "小米智能猫眼2", "room": "门厅", "type": "可视门铃", "profile": "camera", "icon": "📷"},
    {"name": "多模网关", "room": "客厅", "type": "网关", "profile": "gateway", "icon": "☵"},

    # 不可用 (Row 8)
    {"name": "小爱音箱触屏版", "room": "主卧", "type": "音箱", "profile": "speaker", "icon": "♫"},
    {"name": "米家智能加湿器 3", "room": "客厅", "type": "加湿器", "profile": "humidifier", "icon": "💧"},
    {"name": "绘睡水暖垫HS2205", "room": "主卧", "type": "电热毯", "profile": "heater", "icon": "♨"},
    {"name": "黑色电风扇", "room": "客厅", "type": "风扇", "profile": "fan", "icon": "❋"},
    {"name": "米家纯净式智能加湿器", "room": "次卧", "type": "加湿器", "profile": "humidifier", "icon": "💧"},
    {"name": "米家智能水暖毯", "room": "小工具", "type": "电热毯", "profile": "heater", "icon": "♨"},
    {"name": "米家无雾加湿器3 600", "room": "主卧", "type": "加湿器", "profile": "humidifier", "icon": "💧"},
    {"name": "Xiaomi 无线音频送传器", "room": "客厅", "type": "影音配件", "profile": "audio", "icon": "📻"},
    {"name": "米家皮皮灯", "room": "小工具", "type": "灯具", "profile": "light", "icon": "◉"},
    {"name": "Xiaomi 中枢网关", "room": "客厅", "type": "网关", "profile": "gateway", "icon": "☵"},

    # 不可用 (Row 9)
    {"name": "Xiaomi Sound Move", "room": "小工具", "type": "音箱", "profile": "speaker", "icon": "♫"},
    {"name": "小米室外摄像机CW700", "room": "基地", "type": "摄像机", "profile": "camera", "icon": "📷"},
    {"name": "小米智能摄像机3 Pro", "room": "次卧", "type": "摄像机", "profile": "camera", "icon": "📷"},
    {"name": "小米智能单眼2", "room": "门厅", "type": "可视门铃", "profile": "camera", "icon": "📷"},
    {"name": "小米智能门铃 4", "room": "门厅", "type": "可视门铃", "profile": "camera", "icon": "📷"},
    {"name": "米家扫拖机器人 S Pro", "room": "厨房", "type": "扫拖机器人", "profile": "vacuum", "icon": "🧹"}
]

room_map = {r["name"]: r["id"] for r in rooms}

def build_capabilities(profile, dev_name):
    caps = []
    if profile == "light":
        caps = [
            {"key": "power", "label": "开关", "kind": "write", "type": "boolean", "value": 0},
            {"key": "brightness", "label": "亮度", "kind": "write", "type": "number", "min": 1, "max": 100, "step": 1, "unit": "%", "value": 50},
            {"key": "color_temperature", "label": "色温", "kind": "write", "type": "number", "min": 2700, "max": 6500, "step": 100, "unit": "K", "value": 4000}
        ]
    elif profile == "ac":
        caps = [
            {"key": "power", "label": "开关", "kind": "write", "type": "boolean", "value": 1},
            {"key": "target_temperature", "label": "目标温度", "kind": "write", "type": "number", "min": 16.0, "max": 32.0, "step": 0.5, "unit": "°C", "value": 26.0},
            {"key": "mode", "label": "工作模式", "kind": "write", "type": "enum", "values": {"cool": "制冷", "heat": "制热", "fan": "送风", "auto": "自动", "dry": "除湿"}, "value": "cool"},
            {"key": "fresh_air", "label": "新风", "kind": "write", "type": "boolean", "value": 1}
        ]
    elif profile == "fan":
        caps = [
            {"key": "power", "label": "开关", "kind": "write", "type": "boolean", "value": 0},
            {"key": "fan_speed", "label": "风速档位", "kind": "write", "type": "number", "min": 1, "max": 4, "step": 1, "unit": "档", "value": 2},
            {"key": "oscillation", "label": "摇头摆风", "kind": "write", "type": "boolean", "value": 1}
        ]
    elif profile == "curtain":
        caps = [
            {"key": "position", "label": "开合比例", "kind": "write", "type": "number", "min": 0, "max": 100, "step": 1, "unit": "%", "value": 100},
            {"key": "motion", "label": "窗帘动作", "kind": "write", "type": "enum", "values": {"open": "打开", "close": "关闭", "stop": "暂停"}, "value": "stop"}
        ]
    elif profile in ("switch", "socket"):
        caps = [
            {"key": "power", "label": "电源", "kind": "write", "type": "boolean", "value": 1}
        ]
    elif profile == "humidifier":
        caps = [
            {"key": "power", "label": "电源", "kind": "write", "type": "boolean", "value": 0},
            {"key": "target_humidity", "label": "目标湿度", "kind": "write", "type": "number", "min": 30, "max": 80, "step": 5, "unit": "%", "value": 50},
            {"key": "mode", "label": "工作档位", "kind": "write", "type": "enum", "values": {"auto": "自动", "sleep": "睡眠", "strong": "强力"}, "value": "auto"}
        ]
    elif profile == "vacuum":
        caps = [
            {"key": "power", "label": "工作状态", "kind": "write", "type": "boolean", "value": 0},
            {"key": "task", "label": "执行任务", "kind": "write", "type": "enum", "values": {"clean": "清扫", "pause": "暂停", "dock": "回充"}, "value": "dock"}
        ]
    elif profile == "bath_heater":
        caps = [
            {"key": "power", "label": "电源", "kind": "write", "type": "boolean", "value": 0},
            {"key": "mode", "label": "功能模式", "kind": "write", "type": "enum", "values": {"warm_air": "暖风", "ventilation": "换气", "dry": "干燥", "blow": "吹风"}, "value": "warm_air"},
            {"key": "light", "label": "照明", "kind": "write", "type": "boolean", "value": 0}
        ]
    elif profile == "speaker":
        caps = [
            {"key": "playback", "label": "播放状态", "kind": "write", "type": "enum", "values": {"play": "播放", "pause": "暂停"}, "value": "pause"},
            {"key": "volume", "label": "音量大小", "kind": "write", "type": "number", "min": 0, "max": 100, "step": 5, "unit": "%", "value": 45}
        ]
    elif profile == "purifier":
        caps = [
            {"key": "power", "label": "开关", "kind": "write", "type": "boolean", "value": 1},
            {"key": "mode", "label": "净化模式", "kind": "write", "type": "enum", "values": {"auto": "自动", "sleep": "睡眠", "favorite": "最爱"}, "value": "auto"}
        ]
    elif profile == "heater":
        caps = [
            {"key": "power", "label": "加热开关", "kind": "write", "type": "boolean", "value": 0},
            {"key": "target_temperature", "label": "设定水温", "kind": "write", "type": "number", "min": 25, "max": 55, "step": 1, "unit": "°C", "value": 38}
        ]
    elif profile == "microwave":
        caps = [
            {"key": "power", "label": "启动", "kind": "write", "type": "boolean", "value": 0},
            {"key": "duration", "label": "加热秒数", "kind": "write", "type": "number", "min": 10, "max": 600, "step": 10, "unit": "秒", "value": 60}
        ]
    elif profile == "clothes_dryer":
        caps = [
            {"key": "motion", "label": "升降控制", "kind": "write", "type": "enum", "values": {"up": "上升", "down": "下降", "stop": "暂停"}, "value": "stop"},
            {"key": "light", "label": "照明", "kind": "write", "type": "boolean", "value": 0}
        ]
    elif profile == "lock":
        caps = [
            {"key": "locked", "label": "锁舌状态", "kind": "read", "type": "boolean", "value": 1}
        ]
    elif profile == "camera":
        caps = [
            {"key": "power", "label": "摄像机开关", "kind": "write", "type": "boolean", "value": 1}
        ]
    else:
        caps = [
            {"key": "power", "label": "状态", "kind": "write", "type": "boolean", "value": 1}
        ]
    return caps

devices = []
for i, d in enumerate(raw_devices, start=1):
    room_id = room_map.get(d["room"], "untested")
    dev_id = f"MJ-{i:03d}"
    caps = build_capabilities(d["profile"], d["name"])
    short = d["name"].replace("米家", "").replace("小米", "").replace("智能", "").replace("（双开）", "").replace("（三开）", "").strip()
    if not short:
        short = d["name"]
    devices.append({
        "id": dev_id,
        "name": d["name"],
        "shortName": short,
        "room": d["room"],
        "roomId": room_id,
        "profile": d["profile"],
        "type": d["type"],
        "icon": d["icon"],
        "capabilities": caps
    })

data = {
    "schemaVersion": 1,
    "simulated": True,
    "rooms": rooms,
    "devices": devices
}

out_path = Path(r"D:\Users\Wanto\Documents\ChatGPT\JEV\mijia-100-devices.json")
out_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

android_assets = Path(r"D:\Users\Wanto\Documents\ChatGPT\JEV\android-app\app\src\main\assets\mijia-100-devices.json")
android_assets.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

print(f"Generated {len(devices)} devices across {len(rooms)} rooms successfully!")
