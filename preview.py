from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
page=Path(__file__).with_name('index.html')
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path not in ('/','/index.html'):
            self.send_error(404); return
        data=page.read_bytes()
        self.send_response(200)
        self.send_header('Content-Type','text/html; charset=utf-8')
        self.send_header('Content-Length',str(len(data)))
        self.end_headers(); self.wfile.write(data)
HTTPServer(('127.0.0.1',8765),Handler).serve_forever()
