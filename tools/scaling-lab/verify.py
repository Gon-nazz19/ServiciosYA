"""Measure distribution and availability after stopping one local replica."""

import json
import statistics
import subprocess
import time
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parent
URL = "http://127.0.0.1:8088/request"
BODY = json.dumps({"providerId": "prestador-local", "message": "Prueba local"}).encode()


def docker(*args):
    return subprocess.run(["docker", *args], cwd=ROOT, check=True, text=True, capture_output=True).stdout.strip()


def compose(*args):
    return docker("compose", *args)


def request():
    started = time.perf_counter()
    try:
        with urlopen(Request(URL, BODY, {"Content-Type": "application/json"}), timeout=8) as response:
            body = json.load(response)
            return response.status, body.get("instance"), (time.perf_counter() - started) * 1000
    except Exception:
        return 0, None, (time.perf_counter() - started) * 1000


def measure(count=90, concurrency=15):
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        rows = [future.result() for future in as_completed(pool.submit(request) for _ in range(count))]
    successful = [row for row in rows if row[0] == 200]
    latencies = sorted(row[2] for row in rows)
    return {
        "concurrency": concurrency,
        "requests": count,
        "successes": len(successful),
        "errors": count - len(successful),
        "instances": dict(Counter(row[1] for row in successful)),
        "latency_avg_ms": round(statistics.mean(latencies), 1),
        "latency_p95_ms": round(latencies[int(0.95 * (len(latencies) - 1))], 1),
    }


def main():
    compose("up", "-d", "--build", "--scale", "backend=3", "backend", "nginx")
    time.sleep(3)
    before = measure()
    replicas = compose("ps", "-q", "backend").splitlines()
    if len(replicas) != 3:
        raise RuntimeError(f"Se esperaban 3 réplicas, hay {len(replicas)}")
    stopped = replicas[0]
    try:
        docker("stop", stopped)
        time.sleep(3)  # Docker DNS / Nginx resolver TTL
        after = measure()
    finally:
        compose("up", "-d", "--scale", "backend=3", "backend", "nginx")
    result = {"before": before, "stopped_container": stopped[:12], "after": after}
    output = ROOT / "results" / "verify.json"
    output.parent.mkdir(exist_ok=True)
    output.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(result, indent=2, ensure_ascii=False))
    if (before["errors"] or after["errors"] or len(before["instances"]) != 3 or
            len(after["instances"]) != 2 or stopped[:12] in after["instances"]):
        raise SystemExit("Falló la distribución o la prueba de redundancia")


if __name__ == "__main__":
    main()
