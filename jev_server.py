"""Local Jev bridge for the simulated home. API keys stay in this process."""
from __future__ import annotations

import json
import re
import secrets
import threading
import time
from collections import defaultdict, deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent
HOST, PORT = "127.0.0.1", 8766
CATALOG = ROOT / "mijia-100-devices.json"
STATE_FILE = ROOT / ".device-state.json"
COST_FILE = ROOT / ".jev-costs.json"
API_URL = "https://api.typesafe.ai/v1/systemone"
MODEL = "jev-latest"
INPUT_PRICE_PER_MILLION = 0.042
LOCK = threading.RLock()
COST_LOCK = threading.RLock()
RATE_LOCK = threading.Lock()
RATE_WINDOW_SECONDS = 60
RATE_LIMIT_PER_IP = 10
REQUEST_TIMES = defaultdict(deque)
STATIC = {"/": (ROOT / "index.html", "text/html; charset=utf-8"),
          "/index.html": (ROOT / "index.html", "text/html; charset=utf-8"),
          "/mijia-100-devices.json": (CATALOG, "application/json; charset=utf-8")}


def read_key() -> str:
    raw = (ROOT / ".key").read_text(encoding="utf-8-sig").strip()
    if raw.startswith("{"):
        obj = json.loads(raw)
        for name in ("TYPESAFE_API_KEY", "api_key", "apikey", "key"):
            if isinstance(obj.get(name), str) and obj[name].strip():
                return obj[name].strip()
        raise RuntimeError(".key 是 JSON，但没有识别到 API key 字段。")
    if raw.upper().startswith("TYPESAFE_API_KEY="):
        raw = raw.split("=", 1)[1].strip().strip("\"'")
    if not raw:
        raise RuntimeError(".key 文件为空。")
    return raw


def load_cost_ledger():
    empty = {"total_api_requests": 0, "total_instructions": 0,
             "total_usd": 0.0, "recent_instructions": [], "recent_requests": []}
    try:
        saved = json.loads(COST_FILE.read_text(encoding="utf-8"))
        if isinstance(saved, dict):
            empty.update(saved)
    except (OSError, ValueError):
        pass
    return empty


COSTS = load_cost_ledger()


def save_cost_ledger():
    tmp = COST_FILE.with_suffix(".tmp")
    tmp.write_text(json.dumps(COSTS, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp.replace(COST_FILE)


def cost_summary():
    instructions = int(COSTS.get("total_instructions", 0))
    total = float(COSTS.get("total_usd", 0.0))
    return {"request_count": int(COSTS.get("total_api_requests", 0)),
            "instruction_count": instructions, "cumulative_usd": total,
            "average_per_instruction_usd": total / instructions if instructions else 0.0,
            "input_price_per_million_usd": INPUT_PRICE_PER_MILLION,
            "pricing_basis": "public_input_token_rate"}


def begin_instruction(instruction_id):
    with COST_LOCK:
        COSTS["total_instructions"] = int(COSTS.get("total_instructions", 0)) + 1
        COSTS.setdefault("recent_instructions", []).append(
            {"id": instruction_id, "cost_usd": 0.0, "request_count": 0})
        COSTS["recent_instructions"] = COSTS["recent_instructions"][-500:]
        save_cost_ledger()


def record_request_cost(instruction_id, stage, usage):
    input_tokens = int(usage.get("input_tokens", 0) or 0)
    output_tokens = int(usage.get("output_tokens", 0) or 0)
    request_usd = input_tokens * INPUT_PRICE_PER_MILLION / 1_000_000
    with COST_LOCK:
        COSTS["total_api_requests"] = int(COSTS.get("total_api_requests", 0)) + 1
        COSTS["total_usd"] = float(COSTS.get("total_usd", 0.0)) + request_usd
        for item in reversed(COSTS.get("recent_instructions", [])):
            if item.get("id") == instruction_id:
                item["cost_usd"] = float(item.get("cost_usd", 0.0)) + request_usd
                item["request_count"] = int(item.get("request_count", 0)) + 1
                break
        COSTS.setdefault("recent_requests", []).append({
            "instruction_id": instruction_id, "stage": stage,
            "input_tokens": input_tokens, "output_tokens": output_tokens,
            "cost_usd": request_usd})
        COSTS["recent_requests"] = COSTS["recent_requests"][-1000:]
        save_cost_ledger()
        summary = cost_summary()
        instruction_cost = next((x["cost_usd"] for x in reversed(COSTS["recent_instructions"])
                                 if x.get("id") == instruction_id), request_usd)
    return {"request_usd": request_usd, "instruction_usd": instruction_cost,
            "input_tokens": input_tokens, "output_tokens": output_tokens, **summary}


def load_data():
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    initial = {d["id"]: {c["key"]: c["value"] for c in d["capabilities"]}
               for d in data["devices"]}
    if STATE_FILE.exists():
        try:
            saved = json.loads(STATE_FILE.read_text(encoding="utf-8"))
            for did, props in saved.items():
                if did in initial and isinstance(props, dict):
                    initial[did].update(props)
        except (OSError, ValueError):
            pass
    return data, initial


DATA, STATES = load_data()
BY_ID = {d["id"]: d for d in DATA["devices"]}
ALIASES = {"卧室": "主卧", "我的卧室": "主卧", "卧室灯": "主卧主灯",
           "主卧灯": "主卧主灯", "客厅灯": "客厅主灯", "大厅": "客厅"}


def choice(instructions, criteria):
    return {"type": "choice", "instructions": instructions, "criteria": criteria}


def api_call(state, questions, instruction_id, stage):
    body = json.dumps({"state": state, "model": MODEL, "questions": questions},
                      ensure_ascii=False).encode("utf-8")
    req = Request(API_URL, data=body, headers={
        "Authorization": "Bearer " + read_key(),
        "Content-Type": "application/json", "Accept": "application/json"}, method="POST")
    started = time.perf_counter()
    try:
        with urlopen(req, timeout=40) as response:
            raw = response.read(1_500_000).decode("utf-8", "replace")
            result = json.loads(raw)
            costs = record_request_cost(instruction_id, stage, result.get("usage") or {})
            return result, round((time.perf_counter() - started) * 1000), costs
    except HTTPError as e:
        detail = e.read(5000).decode("utf-8", "replace")
        raise RuntimeError(f"Jev HTTP {e.code}: {detail}") from None
    except (URLError, TimeoutError, json.JSONDecodeError) as e:
        raise RuntimeError(f"Jev 请求失败：{type(e).__name__}: {e}") from None


def events_for(text, threshold=0.35, room_context=None, preferred_device_type=None):
    rid = secrets.token_hex(5)
    begin_instruction(rid)
    yield {"kind": "log", "level": "info", "message": f"[{rid}] 收到输入：{text}"}
    first_q = {"intent": choice("这条用户请求最主要属于哪类？若混合多种互不相关任务，选择 unclear。", {
        "home_query": "查询智能家居设备、状态、当前数值或设备清单，不改变状态",
        "home_control": "立即控制一个或多个智能家居设备",
        "ai_question": "向对话大模型咨询知识或闲聊，不是设备查询或控制",
        "scheduled_control": "稍后、未来某个时间或周期性控制智能家居",
        "music": "搜索或播放音乐、歌曲、歌手或播放列表",
        "unclear": "无关、信息不足、相互混合或无法归入以上类别"})}
    yield {"kind": "log", "level": "info", "message": "第一阶段：请求 Jev 判断业务类别…"}
    yield {"kind": "debug", "label": "第一阶段请求（无密钥）", "data": {"state": {"user_text": text}, "model": MODEL, "questions": first_q}}
    try:
        first, elapsed, costs = api_call({"user_text": text}, first_q, rid, "业务分类")
    except Exception as e:
        yield {"kind": "error", "message": str(e)}
        return
    ans = first.get("answers", {}).get("intent", {})
    intent = ans.get("choice", "unclear")
    conf = ans.get("confidence")
    yield {"kind": "decision", "stage": 1, "intent": intent,
           "confidence": conf, "probabilities": ans.get("probabilities", {}),
           "model": first.get("model", MODEL), "usage": first.get("usage", {}), "ms": elapsed,
           "costs": costs}
    yield {"kind": "log", "level": "info", "message": f"当前置信度门槛：{threshold:.2f}"}
    yield {"kind": "debug", "label": "第一阶段原始响应", "data": first}
    if intent != "home_control":
        labels = {"home_query": "智能家居查询", "ai_question": "豆包 AI 问答",
                  "scheduled_control": "定时控制", "music": "音乐播放", "unclear": "无法确定"}
        yield {"kind": "result", "status": "routed", "text": f"Jev 判定为「{labels.get(intent, intent)}」。此分支已完成分类，当前演示只实现智能家居控制。模拟设备状态未改变。"}
        return
    if isinstance(conf, (int, float)) and conf < threshold:
        yield {"kind": "result", "status": "clarify", "text": "业务分类置信度较低，已停止执行。请换一种说法。"}
        return
    if any(term in text for term in ("所有", "全部", "全屋", "整个家", "每个", "各个", "都关", "全关")):
        yield {"kind": "log", "level": "info", "message": "检测到群组/全屋指令；当前演示仅执行单设备，已阻止部分执行。"}
        yield {"kind": "result", "status": "clarify", "text": "当前演示支持单设备控制。为避免只执行群组命令的一部分，请指定一个设备；群组控制尚未启用。"}
        return

    candidates = []
    room_names = [r["name"] for r in DATA.get("rooms", [])]
    explicit_room = next((name for name in room_names if name in text), None)
    room_aliases = {"我的卧室": "主卧", "卧室": "主卧", "大厅": "客厅"}
    alias_room = next((name for alias, name in room_aliases.items() if alias in text), None)
    room = explicit_room or alias_room or (room_context if room_context in room_names else None)
    for d in DATA["devices"]:
        if room and d["room"] != room:
            continue
        if preferred_device_type and d["type"] != preferred_device_type:
            continue
        desc = f'{d["room"]} {d["type"]} {d["name"]} '
        desc += "、".join(c["label"] for c in d["capabilities"] if c.get("kind") == "write")
        candidates.append((d, desc))
    device_options = {d["id"]: desc for d, desc in candidates}
    device_options["none"] = "没有候选设备适合该指令，或用户没有指定可确定的设备。"
    numeric = list(dict.fromkeys(re.findall(r"(?<![0-9.])-?\d+(?:\.\d+)?", text)))
    value_options = {f"n{i}": f'原文数值“{v}”，只能用于用户实际指定且该设备支持的参数。'
                     for i, v in enumerate(numeric)}
    # Every writable capability is a separate atomic decision. Irrelevant keys return no_change.
    cap_defs = {}
    for d in DATA["devices"]:
        for c in d["capabilities"]:
            if c.get("kind") == "write":
                cap_defs.setdefault(c["key"], {"label": c["label"], "caps": {}})
                cap_defs[c["key"]]["caps"].setdefault(d["id"], c)
    # Keep fan-out focused. A query for 50% brightness should not ask Jev to
    # independently reject 25 unrelated controls with high confidence.
    cap_terms = {
        "power": ("开", "关", "启动", "关闭", "打开", "打开", "开启", "关闭"),
        "brightness": ("亮度", "调亮", "调暗", "变亮", "变暗"),
        "color_temperature": ("色温", "暖白", "冷白"),
        "hue": ("颜色", "色彩", "彩色"),
        "mode": ("制冷", "制热", "暖风", "冷风", "模式", "自动模式", "除湿"),
        "target_temperature": (chr(28201) + chr(24230), chr(8451), chr(25645) + chr(27663)),
        "fan_speed": ("风速", "风量"), "motion": ("摇头", "摆头"),
        "position": ("窗帘", "开合", "拉开", "合上"),
        "playback": ("播放", "暂停", "继续播放", "停止播放"),
        "volume": ("音量", "声音大小"), "target_humidity": ("湿度",),
        "level": ("档位", "档", "强度"), "source": ("输入源", "信号源"),
        "speed": ("转速", "速度"), "oscillation": ("摆动",),
        "task": ("扫地", "清扫", "回充", "回充电座"),
        "suction": ("吸力", "吸尘"), "feed": ("喂食", "投喂"),
        "fridge_temperature": ("冷藏",), "freezer_temperature": ("冷冻",),
        "program": ("程序", "洗涤模式", "洗衣模式"),
        "seat_temperature": ("座圈",), "water_temperature": ("水温",),
        "flush": ("冲水",), "light": ("照明", "灯光"),
    }
    requested_keys = {key for key, terms in cap_terms.items() if any(term in text for term in terms)}
    cap_defs = {key: spec for key, spec in cap_defs.items() if key in requested_keys}
    questions = {"device": choice(f"Which single device is the user asking to control? Return its exact ID. Respect any explicit room in the command. Current room context is {room or 'unspecified'}. The user selected device type {preferred_device_type or 'automatic'}; only choose from the supplied candidates. If no target is clear, choose none.", device_options)}
    # Detect write verb by each capability using bounded option choices, rather than generating tool calls.
    for key, spec in cap_defs.items():
        option = {"no_change": "The user did not ask to change this capability."}
        enums = {}
        for cap_item in spec["caps"].values():
            for enum_key, enum_label in (cap_item.get("values") or {}).items():
                enum_id = str(enum_key)
                enums.setdefault(enum_id, set()).add(str(enum_label))
        if enums:
            option.update({k: " / ".join(sorted(v)) for k, v in enums.items()})
        else:
            option.update({k: "value " + numeric[int(k[1:])] for k in value_options})
        questions["set_" + key] = choice(
            f"Does the user explicitly ask to set {spec['label']} on the selected device? Choose the exact value stated in the user's text; choose no_change only if this property is not requested.", option)
    state = {"user_text": text, "room_hint": room, "numeric_candidates": numeric}
    yield {"kind": "log", "level": "info", "message": f"第二阶段：并行判断设备及 {len(cap_defs)} 项候选可写能力…"}
    yield {"kind": "debug", "label": "第二阶段请求（无密钥）", "data": {"state": state, "model": MODEL, "questions": questions}}
    try:
        second, elapsed, costs = api_call(state, questions, rid, "设备与参数")
    except Exception as e:
        yield {"kind": "error", "message": str(e)}
        return
    answers = second.get("answers", {})
    device_answer = answers.get("device", {})
    did = device_answer.get("choice", "none")
    operations = []
    for key, spec in cap_defs.items():
        answer = answers.get("set_" + key, {})
        val = answer.get("choice", "no_change")
        if val == "no_change":
            continue
        op_confidence = answer.get("confidence")
        if isinstance(op_confidence, (int, float)) and op_confidence < threshold:
            yield {"kind": "decision", "stage": 2, "device": did, "operation": key,
                   "value": val, "confidence": op_confidence, "status": "rejected",
                   "reason": f"低于置信度门槛 {threshold:.2f}"}
            continue
        c = spec["caps"].get(did)
        if not c or c.get("kind") != "write":
            continue
        if isinstance(c.get("values"), dict):
            if str(val) not in c["values"]:
                continue
            normalized = val if val in c["values"] else next((k for k in c["values"] if str(k) == str(val)), val)
        else:
            match = re.fullmatch(r"n(\d+)", str(val))
            if not match:
                continue
            idx = int(match.group(1))
            if idx >= len(numeric):
                continue
            try:
                normalized = float(numeric[idx])
                if normalized.is_integer(): normalized = int(normalized)
            except ValueError:
                continue
            if normalized < c["min"] or normalized > c["max"] or abs((normalized-c["min"])/c["step"]-round((normalized-c["min"])/c["step"])) > 1e-6:
                yield {"kind": "decision", "stage": 2, "device": did, "operation": key,
                       "value": normalized, "status": "rejected", "reason": "超出模拟设备范围或步长"}
                continue
        operations.append({"key": key, "label": c["label"], "value": normalized, "unit": c.get("unit", ""),
                           "confidence": answer.get("confidence"), "probabilities": answer.get("probabilities", {})})
    yield {"kind": "decision", "stage": 2, "device": did,
           "device_name": BY_ID.get(did, {}).get("name"),
           "device_confidence": device_answer.get("confidence"),
           "device_probabilities": device_answer.get("probabilities", {}),
           "operations": operations, "model": second.get("model", MODEL),
           "usage": second.get("usage", {}), "ms": elapsed, "costs": costs}
    yield {"kind": "debug", "label": "第二阶段原始响应", "data": second}
    if did not in BY_ID:
        yield {"kind": "result", "status": "clarify", "text": "Jev 未选出有效的单个设备，未执行。"}
        return
    device = BY_ID[did]
    if isinstance(device_answer.get("confidence"), (int, float)) and device_answer["confidence"] < threshold:
        yield {"kind": "result", "status": "clarify", "text": f"设备选择置信度较低（{device_answer['confidence']:.2f}），未执行。请说明房间或设备名称。"}
        return
    if not operations:
        yield {"kind": "result", "status": "clarify", "text": f"识别到目标设备 {device['name']}，但没有可验证的控制参数；未执行。"}
        return
    with LOCK:
        before = dict(STATES[did])
        for op in operations:
            STATES[did][op["key"]] = op["value"]
        if device.get("profile") == "curtain":
            motion = STATES[did].get("motion")
            if motion == "open":
                STATES[did]["position"] = 100
            elif motion == "close":
                STATES[did]["position"] = 0
        tmp = STATE_FILE.with_suffix(".tmp")
        tmp.write_text(json.dumps(STATES, ensure_ascii=False, indent=2), encoding="utf-8")
        tmp.replace(STATE_FILE)
    # Keep demo device state in one authoritative server-side store.
    yield {"kind": "execute", "status": "success", "device_id": did,
           "device_name": device["name"], "room": device["room"],
           "before": before, "after": dict(STATES[did]), "operations": operations}
    yield {"kind": "result", "status": "success", "text": "已更新模拟设备状态。"}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        # Never log request bodies or Authorization headers.
        print("[jev-server] " + (fmt % args))

    def send_json(self, status, obj):
        raw = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers(); self.wfile.write(raw)

    def allow_request(self):
        # Quick Tunnel supplies the visitor address in CF-Connecting-IP. The
        # fallback keeps local development usable without trusting arbitrary
        # X-Forwarded-For headers.
        ip = self.headers.get("CF-Connecting-IP", self.client_address[0])[:80]
        now = time.monotonic()
        with RATE_LOCK:
            times = REQUEST_TIMES[ip]
            while times and now - times[0] >= RATE_WINDOW_SECONDS:
                times.popleft()
            if len(times) >= RATE_LIMIT_PER_IP:
                return False
            times.append(now)
            return True

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/api/costs":
            with COST_LOCK:
                return self.send_json(200, {"summary": cost_summary(),
                                            "recent_requests": COSTS.get("recent_requests", [])[-20:]})
        if path == "/api/state":
            with LOCK:
                return self.send_json(200, {"states": STATES, "devices": DATA["devices"]})
        item = STATIC.get(path)
        if not item:
            return self.send_error(404)
        path, mime = item
        try: raw = path.read_bytes()
        except OSError: return self.send_error(404)
        self.send_response(200); self.send_header("Content-Type", mime)
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Cache-Control", "no-store"); self.end_headers(); self.wfile.write(raw)

    def do_POST(self):
        if self.path not in ("/api/process", "/api/reset"):
            return self.send_error(404)
        origin = self.headers.get("Origin")
        if origin:
            origin_host = urlparse(origin).netloc.lower()
            request_host = self.headers.get("Host", "").lower()
            local_hosts = {f"{HOST}:{PORT}".lower(), f"localhost:{PORT}".lower()}
            if origin_host not in local_hosts and (not request_host or origin_host != request_host):
                return self.send_json(403, {"error": "来源不匹配"})
        if not self.allow_request():
            return self.send_json(429, {"error": "此体验链接每个 IP 每分钟最多提交 10 次，请稍后再试。"})
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length > 20000: return self.send_json(413, {"error": "请求内容过长"})
            body = json.loads(self.rfile.read(length) or b"{}")
        except (ValueError, json.JSONDecodeError):
            return self.send_json(400, {"error": "请求 JSON 无效"})
        if self.path == "/api/reset":
            with LOCK:
                STATE_FILE.unlink(missing_ok=True)
                _, initial = load_data(); STATES.clear(); STATES.update(initial)
            return self.send_json(200, {"ok": True, "states": STATES})
        text = body.get("text")
        if not isinstance(text, str) or not text.strip() or len(text) > 500:
            return self.send_json(400, {"error": "请输入 1 到 500 字的文本"})
        try:
            threshold = float(body.get("threshold", 0.35))
        except (TypeError, ValueError):
            return self.send_json(400, {"error": "置信度门槛必须是 0 到 1 之间的数字"})
        if not 0 <= threshold <= 1:
            return self.send_json(400, {"error": "置信度门槛必须是 0 到 1 之间的数字"})
        room_context = body.get("room")
        room_names = {r["name"] for r in DATA.get("rooms", [])}
        if room_context not in room_names:
            room_context = None
        preferred_device_type = body.get("device_type")
        device_types = {d["type"] for d in DATA["devices"]}
        if preferred_device_type not in device_types:
            preferred_device_type = None
        self.send_response(200)
        self.send_header("Content-Type", "application/x-ndjson; charset=utf-8")
        self.send_header("Cache-Control", "no-store, no-transform")
        self.send_header("Transfer-Encoding", "chunked"); self.end_headers()
        try:
            for event in events_for(text.strip(), threshold, room_context, preferred_device_type):
                chunk = (json.dumps(event, ensure_ascii=False) + "\n").encode("utf-8")
                self.wfile.write(f"{len(chunk):X}\r\n".encode("ascii") + chunk + b"\r\n")
                self.wfile.flush()
            self.wfile.write(b"0\r\n\r\n"); self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError):
            pass


if __name__ == "__main__":
    print(f"JEV 家居模拟服务：http://{HOST}:{PORT}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
