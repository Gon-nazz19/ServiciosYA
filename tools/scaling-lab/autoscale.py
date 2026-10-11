"""Demonstration autoscaler using Nginx active connections, not Cloud Functions metrics."""

import argparse
import csv
import re
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parent
STATUS_URL = "http://127.0.0.1:8088/nginx_status"


def compose(*args):
    return subprocess.run(["docker", "compose", *args], cwd=ROOT, check=True, text=True, capture_output=True).stdout.strip()


def active_connections():
    with urlopen(STATUS_URL, timeout=3) as response:
        match = re.search(r"Active connections:\s*(\d+)", response.read().decode())
    if not match:
        raise RuntimeError("No se pudo leer nginx_status")
    return max(0, int(match.group(1)) - 1)  # Exclude this polling connection.


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--seconds", type=int, default=80)
    args = parser.parse_args()
    compose("up", "-d", "--scale", "backend=1", "backend", "nginx")
    for attempt in range(20):
        try:
            active_connections()
            break
        except OSError:
            time.sleep(1)
    else:
        raise RuntimeError("Nginx no respondió en 20 segundos")
    replicas = 1
    high_samples = low_samples = 0
    output = ROOT / "results" / "autoscale.csv"
    output.parent.mkdir(exist_ok=True)
    with output.open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(["timestamp_utc", "active_connections", "replicas", "action"])
        end = time.monotonic() + args.seconds
        while time.monotonic() < end:
            active = active_connections()
            high_samples = high_samples + 1 if active >= 8 * replicas else 0
            low_samples = low_samples + 1 if active <= 2 * replicas else 0
            action = "hold"
            if high_samples >= 2 and replicas < 3:
                replicas += 1
                compose("scale", f"backend={replicas}")
                high_samples = low_samples = 0
                action = "up"
            elif low_samples >= 10 and replicas > 1:
                replicas -= 1
                compose("scale", f"backend={replicas}")
                high_samples = low_samples = 0
                action = "down"
            writer.writerow([datetime.now(timezone.utc).isoformat(), active, replicas, action])
            file.flush()
            print(f"active={active:2} replicas={replicas} action={action}")
            time.sleep(1)


if __name__ == "__main__":
    main()
