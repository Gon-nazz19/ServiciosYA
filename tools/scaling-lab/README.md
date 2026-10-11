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

## Limpieza

```powershell
docker compose --profile load down --remove-orphans
```

La limpieza elimina sólo los contenedores y la red de este Compose; los resultados permanecen en el repositorio. No ejecutar `down --volumes` ni comandos globales de Docker para este TP.
