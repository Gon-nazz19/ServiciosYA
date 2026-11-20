# Campañas de notificaciones push

Envía las campañas de ServiciosYa con **Firebase Cloud Messaging** (API HTTP v1, a través del Admin SDK).

Las campañas se segmentan por **topics**. La app suscribe cada dispositivo a dos tipos de topic:

- `novedades`: cuando el usuario acepta las notificaciones.
- `categoria_<id>`: por cada categoría que el usuario explora, por ejemplo `categoria_electricistas`.

Así no se guarda ningún token en Firestore. Enviar a topics funciona en el plan gratuito de Firebase (Spark) y no necesita Cloud Functions.

Las campañas están en `campaigns.json`. Cada una tiene estos campos:

| Campo | Para qué |
|---|---|
| `id` | Código corto que llega a Analytics como `campaign_id` en `notification_open` |
| `topic` | A quién le llega |
| `title` / `body` | Texto de la notificación |
| `route` | Pantalla que se abre al tocarla: `category/<id>`, `provider/<id>` o `requests`. Si queda vacía, abre el Inicio |

## Requisitos

- Node.js 22 o superior.
- La misma clave de cuenta de servicio que usa [`tools/seed`](../seed/README.md), guardada **fuera del repositorio**.

## Uso

Desde esta carpeta (`tools/push`):

```bash
npm install
npm run list      # lista las campañas
npm run check     # valida todas sin conectarse a Firebase
```

Para enviar una campaña, en Git Bash:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/c/ruta/a/serviciosya-service-account.json"
npm run send -- --campaign electricistas-destacados
```

En PowerShell:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ruta\a\serviciosya-service-account.json"
npm run send -- --campaign electricistas-destacados
```

Agregando `--validate`, FCM revisa el mensaje sin entregarlo. Por seguridad, el script envía una sola campaña por vez.

## Campañas desde la consola de Firebase

También se pueden crear sin código, en **Firebase Console → Messaging → Nueva campaña → Notificaciones**:

1. Completar el título y el texto.
2. En **Destino**, elegir **Tema** y escribir el topic, por ejemplo `novedades`.
3. En **Opciones adicionales → Datos personalizados**, agregar `route` (por ejemplo `category/plomeros`) y `campaign_id` (por ejemplo `consola-plomeros`).
4. Revisar y publicar.

Para probar en un solo dispositivo, usar **Enviar mensaje de prueba** con el token que la app de desarrollo imprime en Logcat (filtro `ServiciosYaPush`).
