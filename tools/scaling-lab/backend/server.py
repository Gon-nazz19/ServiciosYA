"""Small local request processor. It never contacts Firebase."""

import json
import os
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


HOSTNAME = os.environ.get("HOSTNAME", "local")
# Azure Container Apps replicas share the same hostname, so each one adds a random suffix.
INSTANCE = f"{HOSTNAME}-{uuid.uuid4().hex[:6]}" if os.environ.get("UNIQUE_INSTANCE") else HOSTNAME
WORK_SECONDS = int(os.environ.get("WORK_MS", "250")) / 1000


class Handler(BaseHTTPRequestHandler):
    # Keep-alive: with HTTP/1.0 every response closes the connection, and a load balancer that
    # reuses upstream connections (Azure Container Apps ingress) answers 502 when it picks a closed one.
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        if self.path != "/health":
            self.send_error(404)
            return
        self.reply(200, {"ok": True, "instance": INSTANCE})

    def do_POST(self):
        if self.path != "/request":
            self.send_error(404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 2048:
                raise ValueError("invalid body size")
            payload = json.loads(self.rfile.read(length))
            if not isinstance(payload, dict) or not isinstance(payload.get("providerId"), str):
                raise ValueError("invalid providerId")
            message = payload.get("message")
            if not isinstance(message, str) or len(message) > 500:
                raise ValueError("invalid message")
        except (ValueError, json.JSONDecodeError):
            self.reply(400, {"error": "invalid request"})
            return

        time.sleep(WORK_SECONDS)
        self.reply(200, {
            "requestId": uuid.uuid4().hex,
            "instance": INSTANCE,
            "status": "PENDING",
        })

    def reply(self, status, body):
        encoded = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, _format, *args):
        pass


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8000), Handler)
    server.daemon_threads = True
    server.serve_forever()
