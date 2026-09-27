# Testing — ServiciosYa

El proyecto tiene cuatro niveles de prueba (EPIC 15).

| Nivel | Qué cubre | Cómo se corre | Toca Firebase real |
|---|---|---|---|
| Unit tests (US-150) | ViewModels, casos de uso, validaciones, mapeos y navegación | `./gradlew testDebugUnitTest` | No |
| Reglas de Firestore (EPIC 13) | `firestore.rules`: qué puede leer y escribir cada usuario | `cd tools/firestore-rules && npm test` | No (emulador) |
| Repositorios (US-151) | Repositorios reales de Auth y Firestore, con las reglas cargadas | `cd tools/firestore-rules && npm run test:android` | No (emulador, proyecto `demo-`) |
| Prueba manual (US-152) | Flujo completo en la app | Checklist de abajo | Sí |

## Requisitos

- **JDK 21.** El JDK que trae Android Studio es la versión 25 y Gradle 8.13 no la soporta. Configuralo en Android Studio (*Settings → Build Tools → Gradle → Gradle JDK*), o en la terminal:
  ```bash
  export JAVA_HOME="$HOME/.jdks/jdk-21.0.12.1+1"      # Git Bash
  $env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-21.0.12.1+1"  # PowerShell
  ```
- **Node.js 22 o superior**, y `npm install` dentro de `tools/firestore-rules`.
- Para `test:android`: **un emulador de Android encendido**. La app se reinstala y se desinstala sola durante el test, así que después hay que volver a instalarla con *Run* o con `./gradlew installDebug`.

Los tests de reglas y de repositorios levantan solos los emuladores de Firebase (Firestore :8080 y Auth :9099) con el proyecto `demo-serviciosya`, que no puede conectarse a producción.

---

## US-152 — Prueba manual del flujo principal

**Preparación:** tener `app/google-services.json` configurado, el seed cargado (`tools/seed`) y las reglas publicadas (`tools/firestore-rules`). Para ver los eventos en *Firebase Console → Analytics → DebugView*, activá el modo debug:

```bash
adb shell setprop debug.firebase.analytics.app com.example.serviciosya
```

| # | Paso | Resultado esperado | Evento Analytics | OK |
|---|---|---|---|---|
| 1 | Abrir la app sin sesión | Pantalla de login | — | ☐ |
| 2 | *Crear cuenta* con campos vacíos o email inválido | Mensaje de validación en español; no se crea la cuenta | — | ☐ |
| 3 | *Crear cuenta* con un email que ya existe | "Ya existe una cuenta con ese email…" | — | ☐ |
| 4 | *Crear cuenta* con datos válidos | Entra a Home: "Hola, *nombre*" y las categorías aparecen solas, sin tocar *Reintentar* | — | ☐ |
| 5 | Ver Home | 5 categorías | — | ☐ |
| 6 | Tocar **Electricista** | 3 prestadores con rating y ciudad | `category_view` | ☐ |
| 7 | Tocar un prestador | Perfil con "Sobre mí" y *Solicitar contacto* | `provider_view` | ☐ |
| 8 | Escribir un mensaje y tocar *Solicitar contacto* dos veces rápido | "Solicitud enviada correctamente.", el botón queda en "Solicitud enviada" y se crea **una** sola solicitud | `contact_request` | ☐ |
| 9 | Pestaña **Solicitudes** | La solicitud aparece como *Pendiente* | — | ☐ |
| 10 | Pestaña **Inicio**: buscar "plom" | Secciones Categorías y Prestadores; al tocar un resultado se abre | `service_search` | ☐ |
| 11 | Buscar "zzz" | "No encontramos resultados para esa búsqueda." | `service_search` | ☐ |
| 12 | Pestaña **Perfil** → *Cerrar sesión* | Vuelve a Login; "Atrás" no vuelve a la app | — | ☐ |
| 13 | Iniciar sesión con contraseña incorrecta | "El email o la contraseña no son correctos." | — | ☐ |
| 14 | Iniciar sesión con **otra cuenta** y abrir Solicitudes | Solo se ven las solicitudes propias, nunca las de la cuenta anterior | — | ☐ |
| 15 | Cerrar y volver a abrir la app | Entra directo a Home (sesión persistida) | — | ☐ |
| 16 | Modo avión → abrir una categoría → *Reintentar* sin conexión, y después con conexión | Mensaje de error comprensible; al volver la conexión, *Reintentar* carga los datos | — | ☐ |

**Criterio de aceptación:** todo el flujo se completa sin errores, y en DebugView aparecen `category_view`, `provider_view`, `contact_request` y `service_search` con sus parámetros (`category_id`, `category_name`, `provider_id`, `query`).

### Registro de ejecuciones

| Fecha | Dispositivo | Pasos OK | Analytics verificado | Observaciones |
|---|---|---|---|---|
| 2026-09-26 | Emulador Pixel 7 · API 35 | 1–15 | Pendiente (Analytics sin vincular) | Se detectó y corrigió que la pestaña Solicitudes mostraba un instante las solicitudes de la cuenta anterior (commit bd06d31) |
