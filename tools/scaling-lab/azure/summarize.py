"""Turn the Azure measurements of each run into a compact Markdown report."""

import csv
import json
from datetime import datetime
from pathlib import Path


RESULTS = Path(__file__).resolve().parent.parent / "results" / "azure"
RUNS = {"final": "Mínimo 2, máximo 5 réplicas"}


def load_run(folder):
    load = json.loads((folder / "k6-summary.json").read_text(encoding="utf-8"))
    balance = json.loads((folder / "balance.json").read_text(encoding="utf-8"))
    with (folder / "replicas.csv").open(encoding="utf-8", newline="") as file:
        samples = list(csv.DictReader(file))
    return load["metrics"], balance, samples


def timeline(samples):
    """Seconds from the first sample to each change in the replica count."""
    start = datetime.fromisoformat(samples[0]["timestamp_utc"])
    changes, previous = [], None
    for row in samples:
        count = int(row["replicas"])
        if count != previous:
            seconds = (datetime.fromisoformat(row["timestamp_utc"]) - start).total_seconds()
            changes.append(f"{count} ({seconds:.0f} s)")
            previous = count
    return " → ".join(changes)


lines = [
    "# Resultados en Azure Container Apps (TP3)",
    "",
    "Backend desplegado en Azure Container Apps (Chile Central): ingreso HTTPS con balanceo,",
    "0,25 vCPU y 0,5 GiB por réplica, regla de escalado HTTP de 8 solicitudes concurrentes por réplica",
    "y máximo de 5 réplicas. Cada corrida: k6 con 40 usuarios virtuales durante 4 minutos y luego",
    "400 solicitudes con 40 clientes concurrentes para medir el reparto entre réplicas.",
    "",
    "| Configuración | Solicitudes k6 | Éxito | Latencia media / p95 | Reparto (400 solicitudes) | Réplicas en el tiempo |",
    "|---|---:|---:|---:|---|---|",
]
for name, label in RUNS.items():
    folder = RESULTS / name
    if not folder.exists():
        continue
    metrics, balance, samples = load_run(folder)
    duration = metrics["http_req_duration"]["values"]
    success = 1 - metrics["http_req_failed"]["values"]["rate"]
    distribution = " / ".join(str(count) for count in sorted(balance["instances"].values(), reverse=True))
    lines.append(
        f"| {label} | {metrics['http_reqs']['values']['count']} | {success * 100:.2f} % | "
        f"{duration['avg']:.0f} / {duration['p(95)']:.0f} ms | "
        f"{len(balance['instances'])} réplicas ({distribution}) | {timeline(samples)} |"
    )
lines += [
    "",
    "Los segundos de la última columna se cuentan desde el inicio del registro; la carga empieza unos 20 s después.",
    "Archivos fuente por corrida: `replicas.csv`, `k6-summary.json`, `balance.json`.",
    "",
]
report = RESULTS / "RESULTADOS.md"
report.write_text("\n".join(lines), encoding="utf-8")
print(report)
