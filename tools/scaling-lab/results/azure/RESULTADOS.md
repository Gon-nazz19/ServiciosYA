# Resultados en Azure Container Apps (TP3)

Backend desplegado en Azure Container Apps (Chile Central): ingreso HTTPS con balanceo,
0,25 vCPU y 0,5 GiB por réplica, regla de escalado HTTP de 8 solicitudes concurrentes por réplica
y máximo de 5 réplicas. Cada corrida: k6 con 40 usuarios virtuales durante 4 minutos y luego
400 solicitudes con 40 clientes concurrentes para medir el reparto entre réplicas.

| Configuración | Solicitudes k6 | Éxito | Latencia media / p95 | Reparto (400 solicitudes) | Réplicas en el tiempo |
|---|---:|---:|---:|---|---|
| Mínimo 2, máximo 5 réplicas | 12036 | 100.00 % | 815 / 850 ms | 5 réplicas (87 / 79 / 79 / 79 / 76) | 2 (0 s) → 4 (56 s) → 5 (88 s) |

Los segundos de la última columna se cuentan desde el inicio del registro; la carga empieza unos 20 s después.
Archivos fuente por corrida: `replicas.csv`, `k6-summary.json`, `balance.json`.
