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
        FS[("Cloud Firestore<br/>+ providers.userId<br/>+ reviews")]
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

La columna **"Hallazgo que lo motiva"** se completa con los resultados reales ([`RESULTADOS.md`](RESULTADOS.md)). Si un componente no tiene un hallazgo que lo respalde, se posterga.

| Componente | Para qué | Hallazgo que lo motiva | Prioridad |
|---|---|---|---|
| **Modo prestador** (alta de perfil y bandeja de solicitudes) | Hoy las solicitudes no le llegan a nadie: el catálogo es ficticio. Es la Fase 2 del roadmap; `providers.userId` y `users.role` ya están preparados | _…_ | _…_ |
| **Cloud Functions** | Reaccionar a una solicitud nueva (notificar, cambiar estados) sin darle a la app permisos de escritura sobre datos ajenos | _…_ | _…_ |
| **Cloud Messaging (FCM)** | Avisar al prestador de una solicitud nueva y al cliente cuando se la aceptan | _…_ | _…_ |
| **Reseñas** (colección `reviews`) | Si los usuarios exploran pero no piden contacto, puede faltar confianza | _…_ | _…_ |
| **Cloud Storage** | Fotos reales de prestadores en lugar de iniciales | _…_ | _…_ |
| **Crashlytics** | Detectar crashes cuando la app la usen personas fuera del equipo | _…_ | _…_ |
| **Remote Config + A/B Testing** | Probar variantes (textos del botón, orden de prestadores) y medir cuál convierte más: el siguiente ciclo Build-Measure-Learn | _…_ | _…_ |
| **App Check** | Que solo la app oficial pueda leer y escribir Firestore | _…_ | _…_ |

## 4. Cómo se relaciona con el ciclo Build-Measure-Learn

```mermaid
flowchart LR
    B["Build<br/>App Android + Firebase<br/>(docs/MVP.md)"] --> M["Measure<br/>Prueba con usuarios reales<br/>Analytics + tools/metrics"]
    M --> L["Learn<br/>docs/RESULTADOS.md<br/>¿pivotar o perseverar?"]
    L --> B2["Próximo Build<br/>componentes nuevos<br/>(sección 3)"]
    B2 -.-> M
```
