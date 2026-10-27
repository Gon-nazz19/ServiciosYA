"""Record the replica count of the Azure Container App every few seconds while a load test runs."""

import argparse
import csv
import json
import os
import shlex
import shutil
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path


RESULTS = Path(__file__).resolve().parent.parent / "results" / "azure"
# AZ_CLI overrides the command, e.g. "python -m azure.cli" for a pip-installed CLI.
AZ = shlex.split(os.environ["AZ_CLI"], posix=False) if os.environ.get("AZ_CLI") else [shutil.which("az") or "az"]


def replicas(app, group):
    output = subprocess.run(
        [*AZ, "containerapp", "replica", "list", "--name", app, "--resource-group", group, "-o", "json"],
        check=True, text=True, capture_output=True,
    ).stdout
    return sorted(item["name"] for item in json.loads(output or "[]"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--app", default="serviciosya-backend")
    parser.add_argument("--group", default="rg-serviciosya-tp3")
    parser.add_argument("--seconds", type=int, default=600)
    parser.add_argument("--interval", type=int, default=5)
    parser.add_argument("--out", type=Path, default=RESULTS, help="Carpeta de resultados de la corrida")
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    with (args.out / "replicas.csv").open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(["timestamp_utc", "replicas", "names"])
        end = time.monotonic() + args.seconds
        while time.monotonic() < end:
            try:
                names = replicas(args.app, args.group)
            except subprocess.CalledProcessError as error:
                print(f"az falló: {error.stderr.strip()[:200]}")
                time.sleep(args.interval)
                continue
            writer.writerow([datetime.now(timezone.utc).isoformat(), len(names), " ".join(names)])
            file.flush()
            print(f"{datetime.now():%H:%M:%S} replicas={len(names)}")
            time.sleep(args.interval)


if __name__ == "__main__":
    main()
