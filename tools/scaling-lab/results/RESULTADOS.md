# Resultados del laboratorio local TP3

Estos números provienen de contenedores locales. No miden Cloud Functions ni Firestore.
Ejecución: 2026-10-11T18:39:26.072091+00:00 a 2026-10-11T18:40:45.144751+00:00. Imágenes: `nginx:1.27-alpine`, `python:3.12-alpine`, `grafana/k6:0.58.0`.
Protocolo: backend con 250 ms de trabajo simulado; prueba de reparto con 90 + 90 solicitudes y 15 clientes concurrentes; carga k6 de 35 s con hasta 24 VU.

| Fase | Concurrencia | Solicitudes | Errores | Latencia media / p95 | Instancias |
|---|---:|---:|---:|---:|---|
| Balanceo | 15 | 90 | 0 | 359.1 / 653.9 ms | 3 (42, 24, 24) |
| Redundancia | 15 | 90 | 0 | 266.0 / 274.5 ms | 2 (40, 50) |
| k6 | 24 VU máximo | 2875 | 0.00% | 252.2 / 255.3 ms | 1–3 |

Autoescalado: 4 cambios (up, up, down, down). Muestras: 75; conexiones activas máximas: 24.

Archivos fuente: `results/verify.json`, `results/autoscale.csv`, `results/k6-summary.json`.
Repetir la ejecución cambia los valores según la capacidad de la máquina anfitriona.
