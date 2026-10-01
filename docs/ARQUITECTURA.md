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

Es la arquitectura **completa** que proponemos para la próxima iteración. Parte de la [arquitectura actual](#1-arquitectura-actual-mvp) y le suma lo que surgió de los [hallazgos de la prueba](RESULTADOS.md#4-hallazgos). Cada caja tiene un color según su estado:

- ⬜ **Existente**: ya está implementado y no cambia.
- 🟨 **Modificado**: ya existe, pero hay que ampliarlo. Lo que se agrega aparece con un **+**.
- 🟩 **Nuevo**: todavía no existe.

> **Es una propuesta: nada de esta sección está implementado todavía.** Lo único que se aplicó de la prueba fue **H1**, el teclado que tapaba "Solicitar contacto", que se corrigió en la app sin agregar componentes.

### 3.1 Vista general

```mermaid
flowchart LR
    subgraph LEG["Leyenda"]
        direction TB
        L1["Existente"]
        L2["Modificado"]
        L3["Nuevo"]
    end

    subgraph TOOLS["Herramientas (Node.js + Admin SDK)"]
        direction TB
        SEED["tools/seed"]
        RT["tools/firestore-rules"]
        MET["tools/metrics"]
    end

    subgraph APP["App Android (Kotlin + Jetpack Compose)"]
        direction TB
        PRO["<b>Modo prestador</b><br/>perfil<br/>bandeja de solicitudes<br/>aceptar / rechazar"]
        CLI["<b>Modo cliente</b><br/>buscar · pedir contacto<br/>mis solicitudes<br/>+ sugerencias de mensaje<br/>+ avisame cuando haya"]
        LAYERS["<b>Capas</b><br/>ViewModels<br/>Casos de uso<br/>Repositorios<br/>+ reseñas<br/>+ búsquedas sin resultado<br/>+ solicitudes del prestador"]
        PRO --> LAYERS
        CLI --> LAYERS
    end

    subgraph DATA["Firebase: identidad y datos"]
        direction TB
        AUTH["<b>Authentication</b><br/>+ rol PROVIDER"]
        RULES["<b>Security Rules</b><br/>+ reglas por rol"]
        FS[("<b>Cloud Firestore</b><br/>users · categories<br/>providers<br/>serviceRequests<br/>+ reviews<br/>+ searchMisses<br/>+ campo available")]
        RULES --- FS
    end

    subgraph BACK["Firebase: backend y notificaciones"]
        direction TB
        CF["<b>Cloud Functions</b><br/>avisa solicitudes nuevas<br/>cambia estados<br/>recalcula rating"]
        FCM["<b>Cloud Messaging (FCM)</b><br/>push al prestador<br/>y al cliente"]
        ST["<b>Cloud Storage</b><br/>fotos de perfil"]
        CF -- envía --> FCM
    end

    subgraph OPS["Firebase: medición y calidad"]
        direction TB
        GA["<b>Google Analytics</b><br/>eventos del embudo"]
        CR["<b>Crashlytics</b><br/>errores en producción"]
        RC["<b>Remote Config</b><br/><b>+ A/B Testing</b><br/>próximos experimentos"]
        AC["<b>App Check</b><br/>solo la app oficial"]
    end

    TOOLS -- "catálogo · reglas · métricas" --> DATA
    LAYERS -- "login · lee y escribe" --> DATA
    LAYERS -- "sube fotos" --> ST
    LAYERS -- "eventos · errores · config" --> OPS
    FS -- "trigger: solicitud nueva" --> CF
    LAYERS -- "acepta / rechaza" --> CF

    classDef existing fill:#eceff1,stroke:#607d8b,color:#263238;
    classDef modified fill:#fff3cd,stroke:#e0a800,color:#5d4037;
    classDef new fill:#d9f2d9,stroke:#2e7d32,color:#1b5e20;
    class GA,SEED,RT,MET,L1 existing;
    class CLI,LAYERS,AUTH,FS,RULES,L2 modified;
    class PRO,CF,FCM,ST,CR,RC,AC,L3 new;
```

Las herramientas y Google Analytics no cambian. El cambio principal es que la app pasa a tener dos modos y que Firebase suma un backend propio (Cloud Functions + Cloud Messaging) para que la solicitud le llegue al prestador.

### 3.2 Flujo nuevo: la solicitud le llega al prestador

Hoy la solicitud queda guardada en Firestore y nadie la recibe (**H4**). Con los componentes nuevos, el circuito se cierra así:

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant AC as App (modo cliente)
    participant FS as Cloud Firestore
    participant CF as Cloud Functions
    participant FCM as Cloud Messaging
    participant AP as App (modo prestador)
    actor P as Prestador

    C->>AC: Toca "Solicitar contacto"
    AC->>FS: Crea serviceRequest (PENDING)
    FS-)CF: Trigger: solicitud nueva
    CF->>FCM: Notificación al prestador
    FCM-)AP: Push "Tenés una solicitud nueva"
    P->>AP: Abre la bandeja y acepta
    AP->>CF: Acepta la solicitud
    CF->>FS: Cambia el estado a ACCEPTED
    CF->>FCM: Notificación al cliente
    FCM-)AC: Push "Tu solicitud fue aceptada"
    AC->>C: "Mis solicitudes" muestra Aceptada
```

Las Cloud Functions cambian el estado y envían las notificaciones del lado del servidor. Así la app del prestador nunca necesita permisos para escribir solicitudes de otros usuarios.

### 3.3 Cambios en el modelo de datos

Solo se muestra lo que cambia respecto del [modelo actual](#2-modelo-de-datos-cloud-firestore). La colección `categories` y el resto de los campos quedan igual.

```mermaid
erDiagram
    USERS ||--o| PROVIDERS : "es (si su rol es PROVIDER)"
    USERS ||--o{ SERVICE_REQUESTS : "crea"
    PROVIDERS ||--o{ SERVICE_REQUESTS : "recibe y responde"
    SERVICE_REQUESTS ||--o| REVIEWS : "se califica con"
    USERS ||--o{ SEARCH_MISSES : "genera"

    USERS {
        string role "CLIENT o PROVIDER (antes solo CLIENT)"
    }
    PROVIDERS {
        string userId "uid del prestador (antes vacío)"
        boolean available "NUEVO: si toma trabajos"
    }
    SERVICE_REQUESTS {
        string status "PENDING, ACCEPTED, REJECTED, COMPLETED o CANCELLED (antes solo PENDING)"
    }
    REVIEWS {
        string id PK "NUEVA colección"
        string requestId FK
        string providerId FK
        string clientId FK
        number rating "1 a 5"
        string comment
        timestamp createdAt
    }
    SEARCH_MISSES {
        string id PK "NUEVA colección"
        string query "lo que se buscó"
        string userId FK
        timestamp createdAt
    }
```

### 3.4 Justificación de cada componente

Cada componente se justifica con un hallazgo de la [prueba simulada](RESULTADOS.md#4-hallazgos) (H1 a H6). Los que no tienen un hallazgo que los respalde quedan con prioridad baja.

| Componente | Tipo | Para qué | Hallazgo que lo motiva | Prioridad |
|---|---|---|---|---|
| **Modo prestador** (perfil y bandeja de solicitudes), con el rol PROVIDER en Authentication y reglas por rol | 🟩 Nuevo | Que la solicitud le llegue a alguien y pueda aceptarla o rechazarla. `providers.userId` y `users.role` ya están preparados | **H4**: el 70 % de los que pidieron contacto volvió a mirar su solicitud, pero queda *Pendiente* para siempre | **Alta** |
| **Cloud Functions** | 🟩 Nuevo | Al crearse una solicitud, notificar al prestador y manejar los cambios de estado sin darle a la app permisos sobre datos ajenos | **H4** | **Alta** |
| **Cloud Messaging (FCM)** | 🟩 Nuevo | Aviso push al prestador (solicitud nueva) y al cliente (solicitud aceptada) | **H4** | **Alta** |
| **Registro de búsquedas sin resultado** (colección `searchMisses`) + "avisame cuando haya" en el modo cliente | 🟩 Nuevo · 🟨 Modificado | Saber qué servicios pide la gente y todavía no ofrecemos | **H3**: 2 de 4 búsquedas buscaron servicios inexistentes (herrero, albañil) | Media |
| **Disponibilidad y rotación** en el listado (campo `available` y orden mixto) | 🟨 Modificado | Que la demanda no se concentre siempre en el primero | **H2**: 5 de 15 prestadores recibieron el 100 % de las solicitudes | Media |
| **Reseñas** (colección `reviews`) | 🟩 Nuevo | Dar confianza para decidir. El rating real reemplaza al del seed | H2 (el rating decide todo el tráfico, así que tiene que ser real) | Media |
| **Sugerencias de mensaje** en el modo cliente | 🟨 Modificado | Que el prestador reciba el pedido con contexto ("Urgente", "Presupuesto", "Instalación") | **H5**: el 50 % de las solicitudes llega sin mensaje | Media |
| **Remote Config + A/B Testing** | 🟩 Nuevo | Probar variantes, como las sugerencias de mensaje o el orden del listado, y medir cuál convierte más | **H5** | Media |
| **Crashlytics** | 🟩 Nuevo | Detectar crashes cuando la usen personas fuera del equipo | Preventivo: la simulación no tuvo crashes | Baja |
| **Cloud Storage** | 🟩 Nuevo | Fotos reales de prestadores | Sin evidencia todavía; validar en la prueba con personas | Baja |
| **App Check** | 🟩 Nuevo | Que solo la app oficial acceda a Firestore | Preventivo (seguridad) | Baja |

Ya aplicado en esta iteración: **H1**. El teclado tapaba "Solicitar contacto" y se corrigió en la app (manejo de teclado en Compose), sin componentes nuevos.

## 4. Cómo se relaciona con el ciclo Build-Measure-Learn

```mermaid
flowchart LR
    B["Build<br/>App Android + Firebase<br/>(docs/MVP.md)"] --> M["Measure<br/>Prueba con usuarios reales<br/>Analytics + tools/metrics"]
    M --> L["Learn<br/>docs/RESULTADOS.md<br/>¿pivotar o perseverar?"]
    L --> B2["Próximo Build<br/>componentes nuevos<br/>(sección 3)"]
    B2 -.-> M
```
