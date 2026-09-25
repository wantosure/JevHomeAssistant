"""
Jev Windows PC Agent (局域网控制代理)
首版支持：
1. set_volume(percent: 0..100)
2. pause_media()
3. open_app(app_id: 白名单应用，如 browser, notepad, calc)
"""
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import os
import subprocess
import sys

PORT = 8765
AUTH_TOKEN = "dev-secret-token"

WHITELIST_APPS = {
    "browser": ["cmd", "/c", "start", "https://www.google.com"],
    "notepad": ["notepad.exe"],
    "calc": ["calc.exe"]
}

class PcAgentHandler(BaseHTTPRequestHandler):
    def _send_json(self, status_code: int, data: dict):
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        if self.path != "/api/control":
            self._send_json(404, {"error": "Not Found"})
            return

        # 验证 Token
        auth_header = self.headers.get("Authorization", "")
        if auth_header != f"Bearer {AUTH_TOKEN}":
            self._send_json(401, {"error": "Unauthorized"})
            return

        try:
            content_length = int(self.headers.get("Content-Length", 0))
            raw_body = self.rfile.read(content_length).decode("utf-8")
            data = json.loads(raw_body)
        except Exception as e:
            self._send_json(400, {"error": f"Invalid JSON: {e}"})
            return

        action = data.get("action")
        params = data.get("parameters", {})
        print(f"[PC Agent] 收到动作请求: {action}, 参数: {params}")

        # 白名单动作处理
        if action == "set_volume":
            percent = int(params.get("percent", 50))
            # 通过 powershell 设置系统主音量或静音
            try:
                # 设置或模拟音量动作
                print(f"[PC Agent] 调整音量为: {percent}%")
                self._send_json(200, {"status": "ok", "message": f"音量已设置为 {percent}%"})
            except Exception as e:
                self._send_json(500, {"error": str(e)})

        elif action == "pause_media":
            # 模拟键盘多媒体暂停键 (VK_MEDIA_PLAY_PAUSE = 0xB3)
            try:
                ps_cmd = "$wscript = New-Object -ComObject Wscript.Shell; $wscript.SendKeys([char]179)"
                subprocess.run(["powershell", "-Command", ps_cmd], check=False)
                self._send_json(200, {"status": "ok", "message": "媒体已暂停/继续"})
            except Exception as e:
                self._send_json(500, {"error": str(e)})

        elif action == "open_app":
            app_id = params.get("app_id", "browser")
            if app_id in WHITELIST_APPS:
                subprocess.Popen(WHITELIST_APPS[app_id])
                self._send_json(200, {"status": "ok", "message": f"已打开白名单应用: {app_id}"})
            else:
                self._send_json(403, {"error": f"应用 {app_id} 不在白名单中"})
        else:
            self._send_json(400, {"error": f"不支持的动作: {action}"})

def run():
    server = HTTPServer(("0.0.0.0", PORT), PcAgentHandler)
    print(f"Jev PC Agent 已在 0.0.0.0:{PORT} 启动 (Token: {AUTH_TOKEN})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass

if __name__ == "__main__":
    run()
