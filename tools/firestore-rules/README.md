# Firestore Security Rules

Las reglas viven en [`firestore.rules`](../../firestore.rules), en la raíz del repo.

| Colección | Leer | Escribir |
|---|---|---|
| `categories` | usuarios autenticados | nadie desde la app (solo el seed) |
| `providers` | usuarios autenticados | nadie desde la app (solo el seed) |
| `serviceRequests` | solo el cliente que la creó | crear: el propio usuario, en `PENDING`, con un prestador existente y un mensaje de 500 caracteres como máximo. No se puede editar ni borrar |
| `users` | solo el propio usuario | crear: su propio perfil con `role: CLIENT`. No se puede editar ni borrar |
| cualquier otra | nadie | nadie |

## Requisitos

- Node.js 22 o superior.
- Java 21 en el `PATH` para el emulador de Firestore. Sirve el JDK de `C:\Users\<usuario>\.jdks\jdk-21...`.

## Correr los tests

Los tests usan el emulador local con un proyecto `demo-`, así que **nunca tocan la base real**:

```bash
npm install
npm test
```

## Tests de repositorios en Android (US-151)

Corren los repositorios reales de la app (`FirebaseAuthRepository`, `Firestore*Repository`) en el emulador de Android, contra los emuladores de Firestore y Auth con estas mismas reglas. Hace falta un emulador de Android encendido:

```bash
npm run test:android
```

Al terminar, Gradle desinstala la app del emulador, así que después hay que volver a instalarla (*Run* en Android Studio).

## Publicar las reglas

Usa la misma clave de cuenta de servicio que el seed (ver [`tools/seed/README.md`](../seed/README.md)). Antes de publicar, el script guarda una copia de las reglas actuales en `rules-backup-*.rules`.

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ruta\a\serviciosya-service-account.json"
npm run deploy
```

Otra opción es usar la Firebase CLI desde la raíz del repo, con `firebase deploy --only firestore:rules` (requiere `firebase login`).
