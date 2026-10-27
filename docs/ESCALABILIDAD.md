# Estrategia de escalabilidad — ServiciosYa

> Entregables 1 y 2 del TP3: estrategia de escalabilidad y configuración en la nube. El diagrama actualizado (entregable 3) está en [ARQUITECTURA.md §5](ARQUITECTURA.md#5-arquitectura-con-estrategia-de-escalabilidad-tp3).

Repositorio del proyecto: https://github.com/Gon-nazz19/ServiciosYA

## 1. Estrategia de escalabilidad

### 1.1 Objetivo

ServiciosYa conecta a personas de San Francisco (Córdoba) con prestadores de servicios domiciliarios. El objetivo de esta estrategia es que la app siga respondiendo rápido y sin perder solicitudes cuando aumente la cantidad de usuarios, sin pagar por capacidad ociosa cuando la demanda es baja.

### 1.2 Desafíos al escalar la aplicación

Analizamos qué pasaría con la arquitectura del MVP si la app creciera:

- **Picos de demanda.** La demanda de servicios no es pareja: una tormenta o una ola de calor genera muchas búsquedas y solicitudes de electricistas o de aire acondicionado al mismo tiempo. La capacidad tiene que crecer en minutos y volver a bajar después.
- **Lógica de negocio en el cliente.** En el MVP la app escribía la solicitud directamente en Firestore, y las reglas de seguridad eran la única validación. Con más usuarios y versiones de la app conviviendo, esa lógica tiene que estar en el servidor, en un solo lugar.
- **Reintentos y duplicados.** En una red móvil inestable, un usuario puede tocar "Solicitar contacto" de nuevo o la app puede reintentar. Con más tráfico, también aumentan los timeouts. Un reintento no puede crear dos solicitudes iguales.
- **Puntos únicos de falla.** Un único servidor que atienda todo deja la app sin servicio si se cae o si se reinicia para una actualización.
- **Arranques en frío y costo.** Escalar a cero ahorra dinero, pero la primera solicitud después de un período sin uso tarda más. Hay que elegir entre costo y latencia.
- **Latencia geográfica.** Los usuarios están en Argentina. Los servidores y la base de datos tienen que estar cerca de ellos y cerca entre sí.

### 1.3 Decisiones

| Aspecto | Decisión | Cómo resuelve el desafío |
|---|---|---|
| **Servicios administrados** | Firebase Authentication y Cloud Firestore | Escalan solos y no hay servidores que administrar. Firestore reparte la carga entre servidores sin configuración. |
| **Lógica en el servidor** | La creación de solicitudes pasa a la Cloud Function `createServiceRequest` (HTTPS Callable, segunda generación) | La función valida al usuario, el prestador y la categoría, y crea la solicitud con fecha del servidor. Las reglas de Firestore bloquean la escritura directa. |
| **Balanceo de carga** | El front end de Google en Cloud Functions y el ingreso HTTPS en Azure Container Apps | Las solicitudes se reparten automáticamente entre todas las instancias disponibles, sin un balanceador propio que mantener. |
| **Autoescalado** | Por concurrencia: instancias entre un mínimo y un máximo configurados | La capacidad sigue a la demanda. El máximo pone un techo al costo y protege a la base de datos de un exceso de conexiones. |
| **Redundancia** | Firestore regional, replicado en varias zonas; dos réplicas mínimas en el backend desplegado | Una falla en una zona o en una réplica no deja la app sin servicio. |
| **Idempotencia** | Clave de intento de 128 bits por solicitud, guardada en el teléfono hasta que se confirma | El ID del documento se deriva de la clave y el servidor verifica dentro de una transacción si ya existe, así que repetir el envío devuelve la misma solicitud. |
| **Servicios sin estado** | Las instancias no guardan datos propios; todo el estado vive en Firestore | Cualquier instancia puede atender cualquier solicitud, y agregar o quitar instancias no pierde información. |
| **Región** | Firestore y Cloud Functions en `southamerica-east1` (São Paulo); el despliegue en Azure en Chile Central | Son las regiones disponibles más cercanas a los usuarios. La función y la base comparten región para no sumar latencia entre ellas. |

### 1.4 Balanceo de carga

Ningún usuario se conecta a un servidor en particular: la app llama a una URL, y la plataforma decide qué instancia atiende cada solicitud. En Google Cloud lo hace el front end de Google, delante de Cloud Run, que es la plataforma donde corren las Cloud Functions de segunda generación. En Azure lo hace el ingreso de Container Apps, que además termina HTTPS. Como las instancias no guardan estado, no hace falta que un usuario vuelva siempre a la misma.

### 1.5 Redundancia

- **Datos:** Cloud Firestore en una ubicación regional replica los datos en varias zonas de la región. Si una zona falla, la base sigue disponible sin intervención.
- **Cómputo:** el backend desplegado mantiene **dos réplicas como mínimo**. Si una falla o se reinicia, la otra sigue atendiendo mientras la plataforma la reemplaza.
- **Cliente:** si una solicitud falla por un error de red, la app puede reintentarla con la misma clave de idempotencia sin generar duplicados.

### 1.6 Autoescalado

Las dos plataformas escalan según la **cantidad de solicitudes concurrentes por instancia**:

| Parámetro | Cloud Function `createServiceRequest` | Container App `serviciosya-backend` |
|---|---|---|
| Plataforma | Cloud Functions v2 (Cloud Run) | Azure Container Apps |
| Recursos por instancia | 256 MiB, 1 CPU | 0,5 GiB, 0,25 vCPU |
| Solicitudes concurrentes por instancia | 10 | 8 (regla de escalado HTTP) |
| Instancias mínimas | 0 (sin costo en reposo) | 2 (redundancia) |
| Instancias máximas | 3 | 5 |
| Timeout | 30 s | — |

Cuando las solicitudes en curso superan el umbral, la plataforma agrega instancias hasta el máximo. Cuando la carga baja, espera un período de enfriamiento (5 minutos en Azure) y vuelve al mínimo, para no agregar y quitar instancias a cada momento.

El mínimo de cada caso responde a un compromiso distinto. Para la Cloud Function priorizamos el costo: con 0 instancias en reposo no se paga nada, a cambio de un arranque en frío en la primera solicitud. Para el backend desplegado priorizamos la disponibilidad, con dos réplicas siempre activas.

### 1.7 Costos y límites

- Firebase Authentication y Firestore se pagan por uso, y la cuota gratuita alcanza para la etapa actual.
- El máximo de instancias limita el gasto ante un pico o un ataque. Al alcanzarlo, las solicitudes que no entran pueden rechazarse, y la clave de idempotencia permite reintentarlas sin duplicar.
- Firestore admite hasta una escritura sostenida por segundo en un mismo documento. Las solicitudes usan documentos distintos con IDs aleatorios o derivados de un hash, así que no compiten por el mismo documento.
- El despliegue en Azure usa la suscripción *Azure for Students*: los recursos se crearon en un único grupo de recursos para poder borrarlos juntos al terminar.

## 2. Configuración en la nube

### 2.1 Qué se desplegó y por qué

La implementación en la nube se hizo en **Azure Container Apps** con la suscripción *Azure for Students*. Container Apps es el equivalente de Azure a Cloud Run, la plataforma sobre la que corren las Cloud Functions de segunda generación: ejecuta contenedores sin servidores que administrar, con balanceo de carga integrado y autoescalado por reglas. Así se pudo configurar y medir el mismo patrón que usa la arquitectura de la app.

Se desplegó el **backend del laboratorio de escalabilidad** (`tools/scaling-lab/backend`), que simula el procesamiento de una solicitud de contacto: recibe `POST /request` con `providerId` y `message`, valida los datos, tarda 250 ms en procesarla y responde con un ID de solicitud y el identificador de la réplica que la atendió. Ese identificador permite comprobar el balanceo.

### 2.2 Recursos creados

Todos los recursos están en el grupo `rg-serviciosya-tp3`, en la región **Chile Central**, la más cercana a Argentina entre las habilitadas para la suscripción.

| Recurso | Nombre | Función |
|---|---|---|
| Grupo de recursos | `rg-serviciosya-tp3` | Agrupa todos los recursos del TP |
| Container Registry (Basic) | `serviciosyatp3a71f7e7f` | Almacena la imagen del backend |
| Container Apps Environment | `cae-serviciosya-tp3` | Entorno administrado donde corren las réplicas |
| Log Analytics | `workspace-rgserviciosyatp3V3Iz` | Registros del entorno |
| Container App | `serviciosya-backend` | El backend, con ingreso HTTPS externo y autoescalado |

URL pública: `https://serviciosya-backend.wonderfulflower-00e655b7.chilecentral.azurecontainerapps.io`

### 2.3 Configuración de balanceo y autoescalado

```bash
az containerapp create \
  --name serviciosya-backend --resource-group rg-serviciosya-tp3 \
  --environment cae-serviciosya-tp3 \
  --image serviciosyatp3a71f7e7f.azurecr.io/serviciosya-backend:v4 \
  --ingress external --target-port 8000 \
  --cpu 0.25 --memory 0.5Gi \
  --min-replicas 2 --max-replicas 5 \
  --scale-rule-name http-concurrency --scale-rule-type http \
  --scale-rule-http-concurrency 8
```

- `--ingress external` crea el punto de entrada HTTPS público que **balancea** entre todas las réplicas activas.
- `--min-replicas 2` mantiene **redundancia** permanente.
- `--scale-rule-type http --scale-rule-http-concurrency 8` es la regla de **autoescalado**: cuando hay más de 8 solicitudes simultáneas por réplica, Azure agrega réplicas, hasta `--max-replicas 5`.

El despliegue completo (registro, imagen, entorno y app) está automatizado en [`tools/scaling-lab/azure/deploy.sh`](../tools/scaling-lab/azure/deploy.sh). Los pasos para repetirlo están en el [README del laboratorio](../tools/scaling-lab/README.md#3-despliegue-en-la-nube-azure-container-apps).

### 2.4 Prueba de carga y resultados

**Metodología.** Con [k6](https://k6.io) se simularon **40 usuarios concurrentes enviando solicitudes sin pausa durante 4 minutos** desde la computadora del equipo hacia Azure. Mientras tanto, un script registró cada pocos segundos la cantidad de réplicas activas. Al terminar se enviaron 400 solicitudes con 40 clientes simultáneos para medir cómo se reparten entre las réplicas.

| Métrica | Resultado |
|---|---|
| Solicitudes procesadas (k6) | 12.036 |
| Solicitudes exitosas | 100 % |
| Latencia media / percentil 95 | 815 ms / 850 ms |
| Réplicas durante la prueba | 2, luego 4 a los 56 s y 5 a los 88 s |
| Reparto de 400 solicitudes | 5 réplicas (87 / 79 / 79 / 79 / 76), sin errores |

**Escalado hacia abajo.** En una corrida previa con mínimo de 1 réplica se registró el ciclo completo: la app pasó de 1 a 4 réplicas a los 66 segundos de empezar el registro, a 5 a los 99 segundos, y **volvió a 1 réplica unos 5 minutos y medio después de terminar la carga**. Esto coincide con el período de enfriamiento de 5 minutos de Azure.

**Lectura de los resultados:**

- **Autoescalado:** la regla de concurrencia funcionó como se configuró. Con 40 solicitudes simultáneas y un umbral de 8 por réplica, Azure llegó a las 5 réplicas (40 ÷ 8 = 5) en poco más de un minuto, y las quitó cuando la carga terminó.
- **Balanceo:** el ingreso repartió las solicitudes de forma pareja entre todas las réplicas, sin configurar ningún balanceador.
- **Latencia:** la latencia se mantuvo estable durante toda la prueba. Unos 250 ms corresponden al procesamiento simulado y el resto al viaje de red entre la computadora de prueba y Chile y a la capa HTTPS. No aumentó al crecer la carga, porque el autoescalado agregó capacidad a tiempo.

Los datos de cada corrida están en [`tools/scaling-lab/results/azure/`](../tools/scaling-lab/results/azure/).

### 2.5 Configuración en Firebase

La arquitectura de producción de la app también quedó preparada para escalar dentro de Firebase:

- **Cloud Function `createServiceRequest`** ([`functions/src/index.ts`](../functions/src/index.ts)) con la configuración de escalado de la [sección 1.6](#16-autoescalado): región `southamerica-east1`, 256 MiB, 1 CPU, concurrencia 10, de 0 a 3 instancias y timeout de 30 s. Tiene pruebas automáticas contra los emuladores de Firebase que cubren validación, permisos, creación e idempotencia, incluidas llamadas simultáneas con la misma clave.
- **Reglas de Firestore** que cierran la escritura directa de solicitudes una vez que la función está publicada.
- **App Android** con el repositorio que llama a la función y reintenta con la clave de idempotencia. Una opción de compilación (`CREATE_REQUESTS_WITH_FUNCTION`) permite activar este camino cuando la función esté publicada.

Publicar Cloud Functions requiere pasar el proyecto de Firebase al plan de pago por uso (Blaze). Por eso la medición en la nube de este TP se hizo en Azure. El procedimiento de publicación está documentado en [`functions/README.md`](../functions/README.md).

### 2.6 Laboratorio local

Antes del despliegue en la nube, el mismo backend se probó en contenedores locales con Docker. Nginx funcionó como balanceador, y un script propio actuó como autoescalador según las conexiones activas. Con 3 réplicas, las solicitudes se repartieron entre todas; al detener una, las otras dos siguieron respondiendo sin errores; y el autoescalador pasó de 1 a 3 réplicas y volvió a 1 con la carga de k6. Los resultados están en [`tools/scaling-lab/results/RESULTADOS.md`](../tools/scaling-lab/results/RESULTADOS.md).

## 3. Diagrama de arquitectura actualizado

Ver [ARQUITECTURA.md §5](ARQUITECTURA.md#5-arquitectura-con-estrategia-de-escalabilidad-tp3): incluye los componentes de nube de Google Cloud y Azure, los mecanismos de balanceo, redundancia y autoescalado, y el ciclo de autoescalado.
