# Arquitectura — ServiciosYa

> Entregable 4 del TP2: arquitectura del MVP y versión actualizada según los resultados de la prueba.

## 1. Arquitectura actual (MVP)

La app sigue una arquitectura en capas: la UI **nunca** accede directamente a Firebase. Cada capa depende solo de la siguiente, y los repositorios se definen como interfaces en el dominio.

```mermaid
flowchart LR
    subgraph APP["App Android (Kotlin + Jetpack Compose)"]
        direction TB
        subgraph UI["Presentación"]
            direction TB
            NAV["Navigation Compose<br/>+ barra inferior"]
            SCREENS["Pantallas Compose<br/>Login · Registro · Home<br/>Prestadores · Perfil prestador<br/>Mis solicitudes · Perfil"]
            VM["ViewModels<br/>(StateFlow)"]
        end
        subgraph DOMAIN["Dominio"]
            direction TB
            UC["Casos de uso<br/>RegisterUser · SignIn<br/>GetActiveCategories · GetProviders<br/>CreateServiceRequest · GetMyRequests<br/>SearchServices"]
            REPOI["Interfaces de repositorio<br/>+ modelos + AnalyticsTracker"]
        end
        subgraph DATA["Datos"]
            direction TB
            REPO["Repositorios Firebase<br/>FirebaseAuthRepository<br/>Firestore*Repository<br/>FirebaseAnalyticsTracker<br/>(+ DTOs y mappers)"]
        end
        DI["AppContainer<br/>(inyección de dependencias)"]
    end

    subgraph FIREBASE["Firebase (proyecto tp-2-54a64)"]
        direction TB
        AUTH["Authentication<br/>email y contraseña"]
        FS[("Cloud Firestore<br/>users · categories<br/>providers · serviceRequests")]
        RULES["Security Rules<br/>firestore.rules"]
        GA["Google Analytics<br/>category_view · provider_view<br/>contact_request · service_search"]
    end

    subgraph TOOLS["Herramientas (Node.js + Admin SDK)"]
        direction TB
        SEED["tools/seed<br/>carga el catálogo"]
        RT["tools/firestore-rules<br/>tests y despliegue"]
        MET["tools/metrics<br/>métricas de uso"]
    end

    NAV --> SCREENS --> VM --> UC --> REPOI
    REPO -. implementa .-> REPOI
    DI -. crea .-> REPO
    REPO -- "SDK de Firebase" --> FIREBASE
    RULES --- FS
    SEED -- escribe --> FS
    RT -- publica --> RULES
    MET -- lee --> FS
```

### Decisiones principales

| Decisión | Motivo |
|---|---|
| Capas UI → ViewModel → UseCase → Repository | La UI no depende de Firebase: se puede testear con repositorios falsos y cambiar de backend sin tocar pantallas |
| Errores de Firebase traducidos en la capa de datos (`AuthException`) | Mensajes claros en español y dominio independiente de Firebase |
| Nombres de prestador y categoría copiados en cada solicitud | "Mis solicitudes" se carga con una sola consulta |
| Orden de solicitudes en el cliente | Evita crear un índice compuesto en Firestore |
| Búsqueda en el cliente sobre los prestadores activos | Firestore no soporta búsqueda por texto; alcanza para unos 15 prestadores |
| Catálogo cargado con un seed (Admin SDK) | Valida la demanda sin construir todavía el alta de prestadores |
| Security Rules versionadas y testeadas | Cada usuario solo lee y crea sus propios datos |

## 2. Modelo de datos (Cloud Firestore)

```mermaid
erDiagram
    USERS ||--o{ SERVICE_REQUESTS : "crea"
    CATEGORIES ||--o{ PROVIDERS : "agrupa"
    PROVIDERS ||--o{ SERVICE_REQUESTS : "recibe"
    CATEGORIES ||--o{ SERVICE_REQUESTS : "clasifica"

    USERS {
        string id PK "uid de Firebase Auth"
        string name
        string email
        string role "CLIENT"
        timestamp createdAt
    }
    CATEGORIES {
        string id PK "ej. electricistas"
        string name
        string icon
        boolean active
    }
    PROVIDERS {
        string id PK
        string userId "vacío en el MVP"
        string name
        string description
        string categoryId FK
        string city
        string phone
        number rating
        number reviewCount
        boolean verified
        boolean active
    }
    SERVICE_REQUESTS {
        string id PK
        string clientId FK "uid del usuario"
        string providerId FK
        string categoryId FK
        string providerName "copiado"
        string categoryName "copiado"
        string message "hasta 500 caracteres"
        string status "PENDING"
        timestamp createdAt
    }
```

## 3. Arquitectura actualizada

Componentes que habría que agregar para la siguiente iteración. Los que están en **verde** son nuevos.

```mermaid
flowchart LR
    subgraph APPS["Apps Android"]
        direction TB
        CAPP["App cliente (actual)<br/>buscar · pedir contacto<br/>mis solicitudes"]
        PAPP["Modo prestador<br/>alta de perfil · bandeja de solicitudes<br/>aceptar / rechazar"]
    end

    subgraph CORE["Firebase (actual, ampliado)"]
        direction TB
        AUTH["Authentication<br/>+ rol PROVIDER"]
        FS[("Cloud Firestore<br/>+ providers.userId · available<br/>+ reviews · searchMisses")]
        RULES["Security Rules<br/>+ reglas por rol"]
        GA["Google Analytics"]
        RULES --- FS
    end

    subgraph NEW["Servicios nuevos"]
        direction TB
        CF["Cloud Functions<br/>solicitud nueva → notificar<br/>cambios de estado · rating"]
        FCM["Cloud Messaging (FCM)<br/>notificaciones push"]
        ST["Cloud Storage<br/>fotos de perfil"]
        CR["Crashlytics<br/>errores en producción"]
        RC["Remote Config + A/B Testing<br/>próximos experimentos"]
        AC["App Check<br/>solo apps legítimas"]
        CF --> FCM
    end

    APPS --> CORE
    APPS --> ST
    APPS --> CR
    APPS --> RC
    FS -- trigger --> CF
    FCM -- push --> APPS
    AC -. protege .-> CORE

    classDef new fill:#d9f2d9,stroke:#2e7d32,color:#1b5e20;
    class PAPP,CF,FCM,ST,CR,RC,AC new;
```

### Justificación de cada componente

Cada componente se justifica con un hallazgo de la [prueba simulada](RESULTADOS.md#4-hallazgos) (H1 a H6). Los que no tienen un hallazgo que los respalde quedan con prioridad baja.

| Componente | Para qué | Hallazgo que lo motiva | Prioridad |
|---|---|---|---|
| **Modo prestador** (alta de perfil y bandeja de solicitudes) | Que la solicitud le llegue a alguien y pueda aceptarla o rechazarla. `providers.userId` y `users.role` ya están preparados | **H4**: el 70 % de los que pidieron contacto volvió a mirar su solicitud, pero queda *Pendiente* para siempre | **Alta** |
| **Cloud Functions** | Al crearse una solicitud, notificar al prestador y manejar los cambios de estado sin darle a la app permisos sobre datos ajenos | **H4** | **Alta** |
| **Cloud Messaging (FCM)** | Aviso push al prestador (solicitud nueva) y al cliente (solicitud aceptada) | **H4** | **Alta** |
| **Registro de búsquedas sin resultado** (colección `searchMisses` o parámetro de Analytics) + "avisame cuando haya" | Saber qué servicios pide la gente y todavía no ofrecemos | **H3**: 2 de 4 búsquedas buscaron servicios inexistentes (herrero, albañil) | Media |
| **Disponibilidad y rotación** en el listado (campo `available` y orden mixto) | Que la demanda no se concentre siempre en el primero | **H2**: 5 de 15 prestadores recibieron el 100 % de las solicitudes | Media |
| **Reseñas** (colección `reviews`) | Dar confianza para decidir. El rating real reemplaza al del seed | H2 (el rating decide todo el tráfico, así que tiene que ser real) | Media |
| **Remote Config + A/B Testing** | Probar variantes, como sugerencias de mensaje u orden del listado, y medir cuál convierte más | **H5**: el 50 % de las solicitudes llega sin mensaje | Media |
| **Crashlytics** | Detectar crashes cuando la usen personas fuera del equipo | Preventivo: la simulación no tuvo crashes | Baja |
| **Cloud Storage** | Fotos reales de prestadores | Sin evidencia todavía; validar en la prueba con personas | Baja |
| **App Check** | Que solo la app oficial acceda a Firestore | Preventivo (seguridad) | Baja |

Ya aplicado en esta iteración: **H1**. El teclado tapaba "Solicitar contacto" y se corrigió en la app (manejo de teclado en Compose), sin componentes nuevos.

## 4. Cómo se relaciona con el ciclo Build-Measure-Learn

```mermaid
flowchart LR
    B["Build<br/>App Android + Firebase<br/>(docs/MVP.md)"] --> M["Measure<br/>Prueba con usuarios reales<br/>Analytics + tools/metrics"]
    M --> L["Learn<br/>docs/RESULTADOS.md<br/>¿pivotar o perseverar?"]
    L --> B2["Próximo Build<br/>componentes nuevos<br/>(sección 3)"]
    B2 -.-> M
```
