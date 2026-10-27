"""Send concurrent requests to the Azure endpoint and count which replica answered each one."""

import argparse
import json
import statistics
import time
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen


RESULTS = Path(__file__).resolve().parent.parent / "results" / "azure"
BODY = json.dumps({"providerId": "prestador-azure", "message": "Prueba de balanceo"}).encode()


def request(url):
    started = time.perf_counter()
    try:
        with urlopen(Request(url, BODY, {"Content-Type": "application/json"}), timeout=15) as response:
            return response.status, json.load(response).get("instance"), (time.perf_counter() - started) * 1000
    except Exception:
        return 0, None, (time.perf_counter() - started) * 1000


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("url", help="https://<app>.<region>.azurecontainerapps.io/request")
    parser.add_argument("--requests", type=int, default=300)
    parser.add_argument("--concurrency", type=int, default=30)
    parser.add_argument("--out", type=Path, default=RESULTS, help="Carpeta de resultados de la corrida")
    args = parser.parse_args()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        rows = [f.result() for f in as_completed(pool.submit(request, args.url) for _ in range(args.requests))]
    latencies = sorted(row[2] for row in rows)
    result = {
        "timestamp_utc": datetime.now(timezone.utc).isoformat(),
        "requests": len(rows),
        "concurrency": args.concurrency,
        "errors": sum(1 for row in rows if row[0] != 200),
        "latency_avg_ms": round(statistics.mean(latencies), 1),
        "latency_p95_ms": round(latencies[int(len(latencies) * 0.95) - 1], 1),
        "instances": dict(Counter(row[1] for row in rows if row[0] == 200)),
    }
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / "balance.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
