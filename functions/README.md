# TP3: creación de solicitudes con Cloud Functions v2

`createServiceRequest` es una HTTPS Callable de segunda generación. Android envía `providerId`, `message` e `idempotencyKey`; recibe `{ requestId }`. La función exige autenticación y un perfil `users/{uid}` con rol `CLIENT`. Verifica que el prestador y su categoría estén activos, toma sus nombres de Firestore y crea la solicitud en estado `PENDING` con fecha del servidor. Los errores esperados usan códigos callable; los errores internos se registran en el servidor y se devuelven sin detalles internos.

La clave aleatoria de 128 bits se guarda en las preferencias privadas de Android hasta que se confirma la creación. Un reintento tras error o corte de conexión conserva la misma clave. En el servidor, un hash SHA-256 de `uid + clave` determina el ID del documento. La transacción devuelve ese mismo ID si el documento ya existe y rechaza la reutilización de la clave con otro contenido. La transacción también resuelve las llamadas concurrentes.

## Ubicación y capacidad

La base Firestore del proyecto está en `southamerica-east1` (São Paulo), según la ubicación confirmada para este TP. La función y el cliente Android usan la misma región para reducir latencia y tráfico entre regiones. La función declara `256MiB`, 1 CPU, concurrencia 10, 0 instancias mínimas, 3 máximas y timeout de 30 segundos. Cloud Functions v2, sobre Cloud Run, distribuye solicitudes entre instancias y escala según la carga hasta el máximo configurado. El límite teórico inicial es de 30 solicitudes simultáneas en ejecución; el rendimiento real depende de transacciones, latencia y cuotas. Con mínimo 0 puede haber arranques en frío, y al alcanzar el máximo pueden rechazarse solicitudes.

## Pruebas locales

Usar Node.js 22 y Java 21. En Windows sirve el JDK incluido con Android Studio si se define `JAVA_HOME`. Desde `functions/`:

```powershell
npm ci
npm test
```

`npm test` compila TypeScript y ejecuta las pruebas con Auth `:9099`, Firestore `:8081` y Functions `:5001` usando exclusivamente `demo-serviciosya`. No requiere credenciales reales. Desde `tools/firestore-rules/`, ejecutar `npm ci` y `npm test` para verificar las reglas. Para la integración Android, iniciar un emulador de Android y ejecutar `npm run test:android` en ese mismo directorio, luego de instalar las dependencias de `functions/`. El test instrumental usa una `FirebaseApp` separada con proyecto `demo-serviciosya` y `10.0.2.2` como host.

## Despliegue e integración

No se realizó ningún despliegue. Antes de publicar, confirmar la región de Firestore y que el proyecto tiene habilitados Cloud Functions v2 y su facturación. Revisar el proyecto destino de Firebase CLI: `.firebaserc` apunta a un proyecto real, por lo que en cualquier comando de despliegue se debe pasar `--project` explícitamente.

```powershell
cd functions
npm ci
npm run build
cd ..
firebase deploy --only functions:createServiceRequest --project <PROJECT_ID>
# Después de distribuir la app actualizada:
firebase deploy --only firestore:rules --project <PROJECT_ID>
```

Hay una incompatibilidad de transición: la app anterior crea solicitudes directamente en Firestore y dejará de poder hacerlo cuando se publiquen estas reglas. Coordinar la publicación de la función, la app actualizada y las reglas; comprobar primero que la versión nueva puede crear una solicitud y leerla en “Mis solicitudes”, y luego cerrar la escritura directa. Las versiones viejas de la app perderán la capacidad de enviar solicitudes al cerrar la regla.

Para verificar sin tocar producción, ejecutar la suite de emuladores y la prueba instrumental. Para una comprobación manual posterior al despliegue, iniciar sesión con un cliente, abrir un prestador activo, enviar un mensaje y confirmar que aparece una sola solicitud propia con `PENDING`, nombre/categoría correctos y fecha. Repetir la llamada con la misma clave debe devolver el mismo ID.
