"""Convert local machine-readable measurements into a compact Markdown report."""

import csv
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent
RESULTS = ROOT / "results"
verification = json.loads((RESULTS / "verify.json").read_text(encoding="utf-8"))
load = json.loads((RESULTS / "k6-summary.json").read_text(encoding="utf-8"))
with (RESULTS / "autoscale.csv").open(encoding="utf-8", newline="") as file:
    scaling = list(csv.DictReader(file))

requests = load["metrics"]["http_reqs"]["values"]["count"]
errors = load["metrics"]["http_req_failed"]["values"]["rate"]
latency = load["metrics"]["http_req_duration"]["values"]
counts = [int(row["replicas"]) for row in scaling]
actions = [row["action"] for row in scaling if row["action"] != "hold"]

lines = [
    "# Resultados del laboratorio local TP3",
    "",
    "Estos números provienen de contenedores locales. No miden Cloud Functions ni Firestore.",
    f"Ejecución: {scaling[0]['timestamp_utc']} a {scaling[-1]['timestamp_utc']}. "
    "Imágenes: `nginx:1.27-alpine`, `python:3.12-alpine`, `grafana/k6:0.58.0`.",
    "Protocolo: backend con 250 ms de trabajo simulado; prueba de reparto con 90 + 90 "
    "solicitudes y 15 clientes concurrentes; carga k6 de 35 s con hasta 24 VU.",
    "",
    "| Fase | Concurrencia | Solicitudes | Errores | Latencia media / p95 | Instancias |",
    "|---|---:|---:|---:|---:|---|",
]
for label, item in [("Balanceo", verification["before"]), ("Redundancia", verification["after"])]:
    distribution = ", ".join(str(count) for count in item["instances"].values())
    lines.append(f"| {label} | {item['concurrency']} | {item['requests']} | {item['errors']} | "
                 f"{item['latency_avg_ms']} / {item['latency_p95_ms']} ms | {len(item['instances'])} ({distribution}) |")
lines += [
    f"| k6 | 24 VU máximo | {requests} | {errors * 100:.2f}% | "
    f"{latency['avg']:.1f} / {latency['p(95)']:.1f} ms | {min(counts)}–{max(counts)} |",
    "",
    f"Autoescalado: {len(actions)} cambios ({', '.join(actions) or 'ninguno'}). "
    f"Muestras: {len(scaling)}; conexiones activas máximas: "
    f"{max(int(row['active_connections']) for row in scaling)}.",
    "",
    "Archivos fuente: `results/verify.json`, `results/autoscale.csv`, `results/k6-summary.json`.",
    "Repetir la ejecución cambia los valores según la capacidad de la máquina anfitriona.",
    "",
]
report = RESULTS / "RESULTADOS.md"
report.write_text("\n".join(lines), encoding="utf-8")
print(report)
