# ServiciosYA

**Encontrá un prestador de tu ciudad por rubro y pedile contacto en pocos toques.**

![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%C2%B7%20Firestore%20%C2%B7%20Analytics%20%C2%B7%20AI%20Logic-FFCA28?logo=firebase&logoColor=black)
![Google Maps](https://img.shields.io/badge/Google-Maps%20%C2%B7%20Location-4285F4?logo=googlemaps&logoColor=white)

ServiciosYA es una app Android que conecta a personas que necesitan resolver un trabajo puntual en su casa (un enchufe que no anda, una pérdida de agua, un aire acondicionado que no enfría) con prestadores de servicios de su ciudad.

Hoy encontrar a alguien confiable depende del boca en boca, de grupos de WhatsApp o de buscar en redes. Esa búsqueda es lenta, no deja registro y no permite comparar. ServiciosYA reúne a los prestadores por rubro, muestra su calificación y permite pedirles contacto desde la app.

> Proyecto desarrollado para **Arquitecturas Móviles 2026** (UTN – Facultad Regional San Francisco): el ciclo **Build-Measure-Learn** de *Lean Mobile App Development* (TP2), la estrategia de escalabilidad (TP3) y la integración de APIs y SDKs (TP7).

## Capturas

| Inicio de sesión | Categorías | Prestadores |
|:---:|:---:|:---:|
| <img src="docs/screenshots/01-login.png" width="220" alt="Pantalla de inicio de sesión"> | <img src="docs/screenshots/02-home.png" width="220" alt="Pantalla principal con las categorías"> | <img src="docs/screenshots/03-prestadores.png" width="220" alt="Listado de plomeros ordenados por calificación"> |
| **Perfil del prestador** | **Búsqueda** | **Mis solicitudes** |
| <img src="docs/screenshots/04-perfil-prestador.png" width="220" alt="Perfil del prestador con el botón Solicitar contacto"> | <img src="docs/screenshots/05-busqueda.png" width="220" alt="Resultados de búsqueda de categorías y prestadores"> | <img src="docs/screenshots/06-mis-solicitudes.png" width="220" alt="Listado de solicitudes enviadas con su estado"> |

## Funcionalidades

- **Cuenta de usuario:** registro, inicio de sesión y cierre de sesión con email y contraseña. La sesión queda guardada, así que al volver a abrir la app se entra directo.
- **Categorías:** Electricista, Plomero, Aire acondicionado, Técnico PC y Jardinero.
- **Prestadores por rubro:** listado ordenado por calificación, con ciudad y rating. Si el usuario activa la ubicación, se ordena por cercanía y muestra la distancia ("a 2,3 km").
- **Perfil del prestador:** descripción del servicio, calificación, ciudad, mapa con la ubicación y botón "Cómo llegar".
- **Contacto directo:** botones para escribirle por **WhatsApp** con el mensaje ya armado, **llamar** o **compartir** el prestador.
- **Asistente con IA (Gemini):** el usuario cuenta su problema con sus palabras y la app le sugiere la categoría y le redacta el mensaje para el prestador.
- **Solicitud de contacto:** con un mensaje opcional de hasta 500 caracteres. La app evita que se envíe dos veces por un doble toque.
- **Mis solicitudes:** el historial de pedidos con su estado.
- **Búsqueda:** por nombre de categoría o de prestador, sin importar tildes ni mayúsculas.
- **Navegación inferior:** Inicio, Solicitudes y Perfil.
- **Mensajes claros en español:** validaciones de formularios, errores de conexión y estados vacíos, con opción de reintentar.
- **Privacidad:** cada usuario ve solo sus propias solicitudes. Lo garantizan las reglas de seguridad de Firestore, no solo la app.
- **Medición:** eventos de Google Analytics en cada paso del recorrido (`category_view`, `provider_view`, `contact_request` y `service_search`), y de las integraciones del TP7 (`contact_channel`, `location_permission_result`, `assistant_used` y `assistant_unmatched`). Nunca se envían datos personales ni coordenadas.

## Cómo funciona

```mermaid
flowchart LR
    A[Registro / Login] --> B[Inicio<br/>categorías y búsqueda]
    B --> C[Prestadores<br/>de la categoría]
    C --> D[Perfil del prestador]
    D --> E[Solicitar contacto]
    E --> F[Mis solicitudes]
```

## Tecnologías

| Área | Herramientas |
|---|---|
| App | Kotlin, Jetpack Compose, Material 3, Navigation Compose |
| Estado y asincronía | ViewModel, StateFlow, Kotlin Coroutines |
| Backend | Firebase Authentication, Cloud Firestore, Google Analytics (Firebase BoM 33.16.0) |
| APIs y SDKs (TP7) | Firebase AI Logic con Gemini (`gemini-3.5-flash`), Google Play Services Location, Maps Compose, Intents de Android (WhatsApp `wa.me`, marcador y compartir) |
| Nube (TP3) | Azure Container Apps para el laboratorio de escalabilidad |
| Seguridad | Firestore Security Rules, versionadas y con tests |
| Herramientas | Node.js con el Firebase Admin SDK (seed, reglas y métricas) y Python (simulación de usuarios) |
| Testing | JUnit 4, kotlinx-coroutines-test, emuladores de Firebase, `@firebase/rules-unit-testing` |

## Arquitectura

La app está organizada en capas, y la interfaz nunca accede directamente a Firebase:

```
Pantallas Compose → ViewModels → Casos de uso → Repositorios (interfaces) ← Implementaciones con Firebase
```

- Los repositorios se definen como interfaces en el dominio. Así los ViewModels y casos de uso se prueban con repositorios falsos.
- `AppContainer` arma las dependencias. Si no encuentra la configuración de Firebase, usa implementaciones alternativas: la app compila igual y muestra un aviso de configuración en vez de fallar.
- Los errores de Firebase se traducen en la capa de datos a mensajes en español.

El detalle completo, con el modelo de datos y la arquitectura propuesta para la próxima iteración, está en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

## Estructura del repositorio

```
ServiciosYA/
├── app/                    App Android
│   └── src/
│       ├── main/           Código (presentation, domain, data, di, navigation)
│       ├── test/           Tests unitarios
│       └── androidTest/    Tests de repositorios contra los emuladores de Firebase
├── docs/                   Entregables del TP2, TP3 y TP7 y documentación técnica
├── tools/
│   ├── seed/               Carga de categorías y prestadores de prueba
│   ├── firestore-rules/    Tests y publicación de las reglas de seguridad
│   ├── metrics/            Métricas de uso a partir de Firestore
│   └── simulation/         Simulación de usuarios sobre la app real
├── firestore.rules         Reglas de seguridad de Firestore
└── FIREBASE_SETUP.md       Cómo conectar la app a un proyecto de Firebase
```

## Cómo ejecutarla

### Requisitos

- Android Studio y un dispositivo o emulador con **Android 7.0 (API 24) o superior**.
- **JDK 21** para Gradle (*Settings → Build Tools → Gradle → Gradle JDK*). El proyecto usa Gradle 9.6, AGP 9.4.1 y Kotlin 2.2.10.
- **Node.js 22 o superior**, solo para las herramientas de `tools/`.

### Pasos

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Gon-nazz19/ServiciosYA.git
   ```
2. Conectar la app a un proyecto de Firebase y colocar `app/google-services.json` (ver [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md)). Este archivo no está en el repositorio. Sin él la app compila, pero muestra un aviso de que no está configurada.
3. *(Opcional)* Configurar la key de Google Maps para ver el mapa en el perfil del prestador. Google exige verificar una tarjeta para habilitar Maps Platform, aunque el uso sea gratuito. Sin key, la app compila y oculta el mapa, pero "Cómo llegar", las distancias y el orden por cercanía siguen funcionando.
   1. En [Google Cloud Console](https://console.cloud.google.com/), elegir el proyecto de Firebase y habilitar **Maps SDK for Android** en *APIs y servicios → Biblioteca*.
   2. En *APIs y servicios → Credenciales*, crear una **clave de API** y restringirla a *Apps para Android*, con el paquete `com.example.serviciosya` y el SHA-1 del certificado de debug (`./gradlew signingReport`). En *Restricciones de API*, dejar solo Maps SDK for Android.
   3. Agregar la clave a `local.properties`, que no se sube a Git:
      ```properties
      MAPS_API_KEY=AIza...
      ```
4. *(Opcional)* Habilitar el asistente con IA: en la consola de Firebase, entrar a **AI Logic → Comenzar** y elegir **Gemini Developer API**. No requiere el plan Blaze. Sin este paso, el asistente responde que hay que elegir la categoría manualmente.
5. Cargar las categorías y los prestadores de prueba con [`tools/seed`](tools/seed/README.md). Desde el TP7 incluyen latitud y longitud, así que hay que volver a cargarlos.
6. Publicar las reglas de seguridad con [`tools/firestore-rules`](tools/firestore-rules/README.md).
7. Abrir el proyecto en Android Studio y ejecutarlo con *Run*, o desde la terminal:
   ```bash
   ./gradlew installDebug
   ```

## Tests

| Nivel | Qué cubre | Comando |
|---|---|---|
| Unitarios | ViewModels, casos de uso, validaciones, mapeos, navegación, distancias, links de contacto y respuestas de la IA | `./gradlew testDebugUnitTest` |
| Reglas de Firestore | Qué puede leer y escribir cada usuario | `cd tools/firestore-rules && npm test` |
| Escalabilidad TP3 | Balanceo, redundancia, autoescalado y carga, en Docker local y en Azure Container Apps | [Laboratorio](tools/scaling-lab/README.md) |
| Repositorios | Repositorios reales contra los emuladores de Auth y Firestore | `cd tools/firestore-rules && npm run test:android` |
| Manual | Flujo completo en la app | Checklist en [`docs/TESTING.md`](docs/TESTING.md) |

Los tests de reglas y de repositorios usan los emuladores locales de Firebase con un proyecto `demo-`, así que nunca tocan la base real.

## Resultados del MVP

La etapa de medición se hizo con una **simulación de 25 usuarios** que manejan la app real en el emulador, una alternativa que habilitó la cátedra. El 80 % de los usuarios simulados envió una solicitud de contacto, y los eventos de Analytics coincidieron uno a uno con las acciones realizadas. Como el comportamiento lo definen perfiles armados por el equipo, ese número es optimista y **no valida el mercado**.

La decisión fue **perseverar**: repetir la prueba con personas reales y, en la próxima iteración, cerrar el circuito para que la solicitud le llegue al prestador. El análisis completo está en [`docs/RESULTADOS.md`](docs/RESULTADOS.md).

## Próximos pasos

- **Modo prestador:** alta de perfil y bandeja para aceptar o rechazar solicitudes.
- **Notificaciones push** al prestador cuando recibe una solicitud y al cliente cuando se la aceptan.
- **Búsquedas sin resultado:** el asistente ya registra los servicios que faltan (`assistant_unmatched`). Falta ofrecer "avisame cuando haya".
- **Reseñas reales**, y disponibilidad y rotación en el listado, para que la demanda no se concentre siempre en los mismos prestadores.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/MVP.md`](docs/MVP.md) | Definición del MVP: problema, hipótesis, alcance y métricas de éxito |
| [`docs/RESULTADOS.md`](docs/RESULTADOS.md) | Resultados de la prueba, hallazgos y decisión de pivotar o perseverar |
| [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) | Arquitectura actual, modelo de datos, arquitectura actualizada, diagrama de escalabilidad (TP3) y de integraciones (TP7) |
| [`docs/ESCALABILIDAD.md`](docs/ESCALABILIDAD.md) | TP3: estrategia de escalabilidad, configuración en la nube y mediciones |
| [`docs/INTEGRACIONES.md`](docs/INTEGRACIONES.md) | TP7: análisis, selección, implementación y pruebas de las integraciones de APIs y SDKs |
| [`docs/PRUEBA_USUARIOS.md`](docs/PRUEBA_USUARIOS.md) | Protocolo para la prueba con usuarios reales |
| [`docs/TESTING.md`](docs/TESTING.md) | Estrategia de testing y checklist de prueba manual |
| [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md) | Configuración de Firebase |
| [`tools/seed`](tools/seed/README.md) · [`tools/firestore-rules`](tools/firestore-rules/README.md) · [`tools/metrics`](tools/metrics/README.md) · [`tools/simulation`](tools/simulation/README.md) | Herramientas de soporte |

## Autores

- **Osvaldo Exequiel Barcos**
- **Gonzalo Nazzetta** ([@Gon-nazz19](https://github.com/Gon-nazz19))

Arquitecturas Móviles 2026 — UTN, Facultad Regional San Francisco.
