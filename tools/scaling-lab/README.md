# Laboratorio local TP3: balanceo, redundancia y autoescalado

Este laboratorio usa contenedores locales y cuesta $0 en servicios cloud. **No usa Firebase**, no ejecuta la callable real y no mide el rendimiento de Cloud Functions ni Firestore. El backend de prueba sólo representa el tiempo de procesamiento de una solicitud: recibe un `POST /request`, espera 250 ms y responde con el ID de su contenedor. No persiste datos ni implementa la seguridad o la idempotencia de producción.

## Herramientas y topología

- Docker Compose crea una red privada y hasta tres réplicas de `backend`, sin publicar sus puertos. Sólo Nginx escucha en `127.0.0.1:8088`.
- Nginx gratuito resuelve `backend` por el DNS de Docker con TTL de 1 s y distribuye solicitudes entre las direcciones disponibles. `/nginx_status` expone conexiones activas para el controlador local.
- `autoscale.py`, ejecutado en la máquina anfitriona, observa esa métrica cada segundo y cambia la escala con `docker compose scale`. Sube después de dos muestras con al menos 8 conexiones por réplica; baja después de diez muestras con como máximo 2 conexiones por réplica. El rango es 1–3.
- k6 genera hasta 24 usuarios virtuales durante 35 s y guarda el resumen en JSON. `verify.py` mide reparto y disponibilidad tras detener una réplica. `summarize.py` reúne los resultados.

Las conexiones activas son un indicador sencillo de carga, no una réplica de la política de autoescalado de Cloud Run. Docker Compose sólo escala porque el script se lo ordena. Nginx balancea entre contenedores dentro de una máquina; Cloud Functions v2 distribuye y escala instancias administradas por Google según sus propias políticas y cuotas.

## Requisitos

- Docker Desktop con motor Linux iniciado y Docker Compose v2.
- Python 3 instalado en la máquina anfitriona. Los scripts usan sólo la biblioteca estándar.
- Puertos locales `8088` libres. Las imágenes Docker se descargan la primera vez; eso usa Internet, pero no activa ni factura servicios de Google Cloud.

Todos los comandos siguientes se ejecutan desde `tools/scaling-lab/`. No requieren credenciales ni variables de Firebase.

## 1. Balanceo y redundancia

```powershell
docker compose up -d --build --scale backend=3 backend nginx
python verify.py
```

`verify.py` envía 90 solicitudes con concurrencia 15, cuenta respuestas por ID de instancia, detiene un contenedor, espera a que caduque la resolución DNS y envía otras 90 solicitudes. Restaura tres réplicas al final. Falla si hay errores o si menos de dos instancias responden en alguna fase. El detalle queda en `results/verify.json`.

## 2. Autoescalado y carga

Terminal A:

```powershell
docker compose --profile load pull k6
python autoscale.py --seconds 80
```

Durante los primeros segundos de la Terminal A, en Terminal B:

```powershell
docker compose --profile load run --rm k6 run /scripts/load.js
```

El controlador parte de una réplica, registra `timestamp_utc`, conexiones activas, réplicas y acción en `results/autoscale.csv`. k6 registra cantidad de solicitudes, errores y latencias en `results/k6-summary.json`. Esperar a que termine la Terminal A y generar el informe:

```powershell
python summarize.py
```

El informe `results/RESULTADOS.md` contiene concurrencia, solicitudes, errores, latencia media y p95, instancias usadas y cambios de escala. Los valores dependen del equipo y de la carga local; conservar los archivos de `results/` junto con el informe permite auditar cada ejecución.

## 3. Despliegue en la nube: Azure Container Apps

El mismo backend se despliega en **Azure Container Apps** con la suscripción *Azure for Students* (sin tarjeta). La plataforma reemplaza las piezas locales: el ingreso HTTPS administrado balancea entre réplicas (en lugar de Nginx) y una regla de escalado HTTP agrega una réplica cada 8 solicitudes concurrentes, entre 2 y 5 réplicas (en lugar de `autoscale.py`). Las 2 réplicas mínimas dan redundancia: si una falla, la otra sigue atendiendo. Tras 5 minutos sin carga, Azure vuelve al mínimo.

| Recurso | Nombre | Para qué |
|---|---|---|
| Grupo de recursos | `rg-serviciosya-tp3` | Agrupa todo; borrarlo elimina el laboratorio completo |
| Container Registry (Basic) | `serviciosyatp3…` | Guarda la imagen del backend |
| Container Apps Environment + Log Analytics | `cae-serviciosya-tp3` | Entorno administrado y registros |
| Container App | `serviciosya-backend` | 0,25 vCPU / 0,5 GiB, ingreso externo, 2–5 réplicas, 8 solicitudes concurrentes por réplica |

La suscripción de estudiante sólo permite algunas regiones; se usa `chilecentral`, la más cercana. ACR Tasks no está disponible en esa suscripción, por eso la imagen se construye con Docker local. El entorno creado por la CLI es de tipo *express*, que no admite identidades administradas: la app descarga la imagen con las credenciales de administrador del registry, que Container Apps guarda como secreto.

Requisitos: Azure CLI con la extensión `containerapp` (`az extension add --name containerapp`), `az login`, Docker iniciado y Bash (Git Bash en Windows). Desde `tools/scaling-lab/azure/`:

```bash
ACR=serviciosyatp3<sufijo-único> bash deploy.sh   # imprime la URL pública
```

Medición (desde `tools/scaling-lab/`, con la URL obtenida). En una terminal, registrar réplicas; en otra, generar carga y luego medir el reparto:

```bash
python azure/watch_replicas.py --seconds 360 --interval 3 --out results/azure/final
docker run --rm -v "$PWD/load.js:/scripts/load.js:ro" -v "$PWD/results/azure/final:/results"   -e TARGET_URL=<URL>/request -e VUS=40 -e HOLD=4m grafana/k6:0.58.0 run --quiet /scripts/load.js
python azure/balance.py <URL>/request --requests 400 --concurrency 40 --out results/azure/final
python azure/summarize.py
```

`watch_replicas.py` usa `az` del PATH; si la CLI se instaló con pip, definir `AZ_CLI="python -m azure.cli"`. El informe queda en `results/azure/RESULTADOS.md`; los datos de cada corrida (`replicas.csv`, `k6-summary.json`, `balance.json`) en `results/azure/final/`. `results/azure/previas/` conserva dos corridas anteriores, hechas con una versión del backend sin conexiones persistentes (HTTP/1.0), que el ingreso de Azure respondía con un 2–3 % de errores 502; la versión actual usa HTTP/1.1. La corrida `previas/min1` registra además la bajada de réplicas.

Para no consumir crédito después de la entrega: `az group delete --name rg-serviciosya-tp3 --yes`.

## Limpieza

```powershell
docker compose --profile load down --remove-orphans
```

La limpieza elimina sólo los contenedores y la red de este Compose; los resultados permanecen en el repositorio. No ejecutar `down --volumes` ni comandos globales de Docker para este TP.
