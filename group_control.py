"""Group selection and validated fan-out for the local simulated home."""
import re
import math
from numeric_values import numeric_candidates


def is_group_command(text):
    return bool(re.search(r"所有|全部|全屋|整个家|每个|各个|全关|全开|都关|都开|都调|全都", text))


def build_groups(data):
    scopes = [("all", "全屋", lambda d: True)]
    scopes += [(r["id"], r["name"], lambda d, name=r["name"]: d["room"] == name) for r in data["rooms"]]
    scopes += [("bedrooms", "所有卧室", lambda d: d["room"] in ("主卧", "次卧", "儿童房"))]
    types = [("lights", "灯（含灯具和灯带）", lambda d: d["type"] in ("灯具", "灯带"))]
    types += [(f"type{i}", t, lambda d, t=t: d["type"] == t) for i, t in enumerate(dict.fromkeys(d["type"] for d in data["devices"]))]
    groups = []
    for scope, room, match_room in scopes:
        for typ, label, match_type in types:
            ids = [d["id"] for d in data["devices"] if d.get("controllable") is not False and match_room(d) and match_type(d)]
            if ids:
                groups.append(dict(id=f"group:{scope}:{typ}", name=f"{room}所有{label}", scope=scope, room=room, device_ids=ids))
    return groups


def group_events(text, threshold, room_hint, data, states, api_call, rid):
    def choice(instructions, criteria):
        return dict(type="choice", instructions=instructions, criteria=criteria)
    def confident(answer):
        v = answer.get("confidence")
        return isinstance(v, (int, float)) and math.isfinite(v) and v >= threshold
    if re.search(r"除了|除外|以外|分别|不要|但不", text):
        yield dict(kind="result", status="clarify", text="当前批量指令支持一个设备组执行相同操作；排除设备或分别操作请拆成多条指令，本次未执行。")
        return
    explicit = next((r for r in data["rooms"] if r["name"] in text), None)
    scope = explicit["id"] if explicit else "all"
    if not explicit:
        if re.search(r"所有卧室|全部卧室|各个卧室|每个卧室", text):
            scope = "bedrooms"
        elif "卧室" in text:
            scope = next(r["id"] for r in data["rooms"] if r["name"] == "主卧")
        elif re.search(r"这里|这个房间|当前房间|我房间", text):
            scope = next((r["id"] for r in data["rooms"] if r["name"] == room_hint), "missing")
    groups = [g for g in build_groups(data) if g["scope"] == scope]
    by_id = {d["id"]: d for d in data["devices"]}
    mentioned_types = {d["type"] for d in data["devices"] if d["type"] in text}
    if "灯" in text and not mentioned_types.intersection({"灯带", "灯具"}):
        mentioned_types.update({"灯具", "灯带"})
    groups = [g for g in groups if not mentioned_types or all(by_id[i]["type"] in mentioned_types for i in g["device_ids"])]
    ids = set(i for g in groups for i in g["device_ids"])
    # Ask each relevant property independently within the same Jev request.
    terms = {"power": "开 关 启动", "brightness": "亮度 调亮 调暗", "color_temperature": "色温 暖白 冷白", "mode": "制冷 制热 暖风 冷风 模式 除湿", "target_temperature": "温度 摄氏 ℃", "position": "窗帘 窗纱 纱帘 遮光帘 开合", "motion": "窗帘 窗纱 纱帘 遮光帘 摇头", "volume": "音量", "playback": "播放 暂停", "fan_speed": "风速 风量", "target_humidity": "湿度"}
    defs = {}
    for device in data["devices"]:
        if device["id"] not in ids:
            continue
        for c in device["capabilities"]:
            if c["kind"] == "write" and any(t in text for t in terms.get(c["key"], c["label"]).split()):
                defs.setdefault(c["key"], dict(label=c["label"], caps=[]))["caps"].append(c)
    options = {g["id"]: f'{g["name"]}，共{len(g["device_ids"])}台：' + '、'.join(by_id[i]["name"] for i in g["device_ids"]) for g in groups}
    options["none"] = "没有完全匹配的设备组或请求包含多个不同操作、排除条件；不能扩大或缩小范围来凑匹配。"
    questions = {"group": choice("选择一个完整设备组。灯含灯具和灯带；未明确房间的所有灯/窗帘/空调指全屋。卧室指主卧，所有卧室指三个卧室。精确匹配范围和类型，不能只执行复合请求的一部分，否则选none。", options)}
    nums = numeric_candidates(text)
    for key, spec in defs.items():
        vals = {"no_change": "没有要求该属性，或此属性不适用于选中的组"}
        enums = {str(k): v for c in spec["caps"] for k, v in (c.get("values") or {}).items()}
        vals.update(enums or {f"n{i}": f"原文数值 {n}" for i, n in enumerate(nums)})
        questions["set_" + key] = choice(f'所有选中设备的{spec["label"]}应设为什么？只选用户要求的值。开关窗帘用motion的open/close，未指定百分比时position选no_change。', vals)
    state = dict(user_text=text, numeric_candidates=nums, group_scope=groups[0]["room"] if groups else "无匹配房间")
    yield dict(kind="log", level="info", message=f'批量范围：{state["group_scope"]}；数值候选：{nums}。灯组包含灯具和灯带。')
    yield dict(kind="debug", label="第二阶段设备组请求（无密钥）", data=dict(state=state, questions=questions, groups=groups))
    try:
        response, ms, costs = api_call(state, questions, rid, "设备组与参数")
    except Exception as e:
        yield dict(kind="error", message=str(e))
        return
    answers = response.get("answers", {})
    selected = answers.get("group", {})
    group = next((g for g in groups if g["id"] == selected.get("choice")), None)
    requested = {k: spec for k, spec in defs.items() if answers.get("set_" + k, {}).get("choice") not in (None, "no_change")}
    yield dict(kind="debug", label="第二阶段设备组原始响应", data=response)
    yield dict(kind="decision", stage=2, device=group["id"] if group else "none", device_name=group["name"] if group else "未选设备组", device_confidence=selected.get("confidence"), device_probabilities=selected.get("probabilities", {}), operations=[dict(key=k, label=spec["label"], value=nums[int(answers["set_"+k]["choice"][1:])] if re.fullmatch(r"n\d+", answers["set_"+k]["choice"]) and int(answers["set_"+k]["choice"][1:]) < len(nums) else answers["set_"+k]["choice"]) for k,spec in requested.items()], model=response.get("model"), ms=ms, costs=costs)
    if not group or not confident(selected) or not requested:
        yield dict(kind="result", status="clarify", text="未可靠选中完整设备组和参数，本次未执行。")
        return
    results, skipped = [], []
    for did in group["device_ids"]:
        d = by_id[did]
        before = dict(states[did])
        after, operations, reason = dict(before), [], ""
        for key, spec in requested.items():
            cap = next((c for c in d["capabilities"] if c["key"] == key and c["kind"] == "write"), None)
            ans = answers["set_" + key]
            if not cap or not confident(ans):
                reason = f'{spec["label"]}不支持或置信度不足'
                break
            value = ans["choice"]
            if cap.get("values"):
                valid = value in cap["values"]
            else:
                match = re.fullmatch(r"n(\d+)", value)
                value = float(nums[int(match[1])]) if match and int(match[1]) < len(nums) else float("nan")
                valid = math.isfinite(value) and cap["min"] <= value <= cap["max"] and abs((value-cap["min"])/cap["step"]-round((value-cap["min"])/cap["step"])) < 1e-6
            if not valid:
                reason = f'{spec["label"]}数值、范围或步长无效'
                break
            after[key] = value
            operations.append(dict(key=key, label=cap["label"], value=value, unit=cap.get("unit", "")))
        if reason:
            skipped.append(dict(device_id=did, device_name=d["name"], reason=reason))
            continue
        if d["type"] == "窗帘":
            motion = next((o["value"] for o in operations if o["key"] == "motion"), None)
            if motion in ("open", "close") and not any(o["key"] == "position" for o in operations):
                after["position"] = 100 if motion == "open" else 0
                operations.append(dict(key="position", label="开合度", value=after["position"], unit="%"))
            elif any(o["key"] == "position" for o in operations) and not motion:
                after["motion"] = "stop"
        results.append(dict(device_id=did, device_name=d["name"], room=d["room"], before=before, after=after, operations=operations))
    yield dict(kind="log", level="info", message=f'设备组展开：{group["device_ids"]}')
    yield dict(kind="execute_batch", group_id=group["id"], group_name=group["name"], results=results, skipped=skipped, target_count=len(group["device_ids"]))
    yield dict(kind="result", status="partial" if skipped else "success", text=f'批量模拟控制：目标 {len(group["device_ids"])} 台，成功 {len(results)} 台，跳过 {len(skipped)} 台。')
