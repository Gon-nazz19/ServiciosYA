# BACKLOG.md — ServiciosYa

## 1. Objetivo del proyecto

**ServiciosYa** es una aplicación móvil orientada a conectar personas que necesitan resolver un trabajo puntual con prestadores de servicios locales.

Ejemplos de categorías iniciales:

- Electricista
- Plomero
- Técnico de aire acondicionado
- Técnico de PC
- Jardinero

El objetivo del MVP es validar la siguiente hipótesis:

> Los usuarios utilizarán una aplicación móvil para encontrar prestadores locales y solicitar contacto cuando necesitan resolver un servicio puntual.

El flujo principal a validar es:

```text
Home
  ↓
Categoría
  ↓
Listado de prestadores
  ↓
Perfil del prestador
  ↓
Solicitud de contacto
```

---

# 2. Alcance del MVP

El MVP debe permitir que un usuario:

1. Cree una cuenta.
2. Inicie sesión.
3. Mantenga la sesión iniciada.
4. Visualice categorías de servicios.
5. Seleccione una categoría.
6. Visualice prestadores asociados.
7. Abra el perfil de un prestador.
8. Solicite contacto.
9. Consulte sus solicitudes.
10. Cierre sesión.

También debe registrar eventos de uso mediante Firebase Analytics.

---

# 3. Stack tecnológico

## Android

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- Coroutines
- StateFlow

## Backend / servicios

- Firebase Authentication
- Cloud Firestore
- Firebase Analytics

## Arquitectura

```text
UI
 ↓
ViewModel
 ↓
Use Case
 ↓
Repository
 ↓
Firebase / API
```

La UI no debe acceder directamente a Firebase.

---

# 4. Estructura recomendada

```text
app/
├── data/
│   ├── model/
│   ├── repository/
│   ├── remote/
│   └── mapper/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── presentation/
│   ├── auth/
│   ├── home/
│   ├── category/
│   ├── provider/
│   ├── request/
│   └── profile/
│
├── navigation/
├── di/
└── core/
```

---

# 5. Modelo de datos inicial

## users

```text
users/{userId}
```

```json
{
  "name": "Juan Pérez",
  "email": "juan@email.com",
  "role": "CLIENT",
  "createdAt": "timestamp"
}
```

## categories

```text
categories/{categoryId}
```

```json
{
  "name": "Electricista",
  "icon": "electrical_services",
  "active": true
}
```

## providers

```text
providers/{providerId}
```

```json
{
  "userId": "abc123",
  "name": "Carlos Electricidad",
  "description": "Instalaciones y reparaciones eléctricas domiciliarias.",
  "categoryId": "electricistas",
  "city": "San Francisco",
  "profileImageUrl": "",
  "phone": "3564...",
  "rating": 4.7,
  "reviewCount": 0,
  "verified": false,
  "active": true
}
```

## serviceRequests

```text
serviceRequests/{requestId}
```

```json
{
  "clientId": "user123",
  "providerId": "provider456",
  "categoryId": "electricistas",
  "message": "",
  "status": "PENDING",
  "createdAt": "timestamp"
}
```

Estados previstos:

```text
PENDING
ACCEPTED
REJECTED
COMPLETED
CANCELLED
```

---

# 6. Backlog

---

## EPIC 0 — Base técnica

### US-001 — Crear proyecto Android

**Prioridad:** Crítica  
**Dependencias:** Ninguna

### Objetivo

Crear la base técnica del proyecto.

### Implementación

- Crear proyecto Android.
- Kotlin.
- Jetpack Compose.
- Material 3.
- Configurar Gradle.
- Definir package.
- Configurar Navigation Compose.
- Agregar ViewModel.
- Agregar Coroutines.
- Agregar StateFlow.
- Integrar Firebase.
- Crear estructura de paquetes.

### Criterios de aceptación

- El proyecto compila.
- La app inicia correctamente.
- Existe una pantalla inicial básica.
- No hay credenciales ni secretos hardcodeados.

---

### US-002 — Implementar arquitectura base

**Prioridad:** Crítica  
**Dependencias:** US-001

### Objetivo

Separar UI, dominio y acceso a datos.

### Implementación

Usar:

```text
UI
 ↓
ViewModel
 ↓
Use Case
 ↓
Repository
 ↓
Firebase
```

### Criterios de aceptación

- Los Composables no acceden directamente a Firebase.
- Los repositorios exponen interfaces.
- La UI depende de ViewModels.
- La lógica de negocio no queda dentro de los Composables.

---

# EPIC 1 — Autenticación

### US-010 — Registro de usuario

**Prioridad:** Crítica  
**Dependencias:** US-001, US-002

### Objetivo

Permitir registrar usuarios con email y contraseña.

### Campos

- Nombre
- Email
- Contraseña

### Implementación

- Firebase Authentication.
- Crear documento en `users/{uid}`.
- Guardar nombre, email, rol y fecha de creación.

### Criterios de aceptación

- Se validan campos vacíos.
- Se valida email.
- Se maneja error de Firebase.
- Se muestra loading.
- Al registrarse, el usuario accede a Home.

---

### US-011 — Inicio de sesión

**Prioridad:** Crítica  
**Dependencias:** US-010

### Objetivo

Permitir iniciar sesión.

### Criterios de aceptación

- Valida campos vacíos.
- Muestra loading.
- Muestra error si las credenciales son incorrectas.
- Redirige a Home al autenticarse.

---

### US-012 — Mantener sesión iniciada

**Prioridad:** Alta  
**Dependencias:** US-011

### Objetivo

Evitar pedir login en cada apertura de la app.

### Criterios de aceptación

- Si existe una sesión válida, abrir Home.
- Si no existe sesión, abrir Login.

---

### US-013 — Cerrar sesión

**Prioridad:** Alta  
**Dependencias:** US-011

### Objetivo

Permitir cerrar sesión desde Perfil.

### Criterios de aceptación

```text
Perfil
 ↓
Cerrar sesión
 ↓
Firebase signOut()
 ↓
Login
```

---

# EPIC 2 — Home

### US-020 — Pantalla principal

**Prioridad:** Crítica  
**Dependencias:** US-011

### Objetivo

Mostrar el punto de entrada principal de la aplicación.

### UI esperada

```text
Hola, Juan 👋

¿Qué servicio necesitás?

[ Buscar servicio ]

Categorías

⚡ Electricista
🚿 Plomero
❄️ Aire acondicionado
💻 Técnico PC
🌿 Jardinero
```

### Criterios de aceptación

- Muestra saludo al usuario.
- Muestra categorías.
- Permite navegar a una categoría.
- Tiene campo de búsqueda.
- Maneja loading, error y estado vacío.

---

# EPIC 3 — Categorías

### US-030 — Crear modelo Category

**Prioridad:** Crítica  
**Dependencias:** US-002

### Modelo

```text
Category
---------
id
name
icon
active
```

### Criterios de aceptación

- Existe modelo de dominio.
- Existe mapper si se usa DTO.
- Existe repositorio de categorías.

---

### US-031 — Obtener categorías desde Firestore

**Prioridad:** Crítica  
**Dependencias:** US-030

### Objetivo

Evitar categorías hardcodeadas en la UI.

### Criterios de aceptación

- La Home obtiene categorías desde Firestore.
- Solo se muestran categorías activas.
- Se manejan loading, error y lista vacía.

---

### US-032 — Navegar a categoría

**Prioridad:** Crítica  
**Dependencias:** US-031

### Flujo

```text
Home
 ↓
Tap en categoría
 ↓
ProvidersScreen(categoryId)
```

### Criterios de aceptación

- Se pasa correctamente `categoryId`.
- Se muestra el nombre de la categoría.

---

# EPIC 4 — Prestadores

### US-040 — Crear modelo Provider

**Prioridad:** Crítica  
**Dependencias:** US-002

### Modelo

```text
Provider
--------
id
userId
name
description
categoryId
city
profileImageUrl
phone
rating
reviewCount
verified
active
```

### Criterios de aceptación

- Modelo de dominio creado.
- Repositorio preparado para Firestore.
- No se acopla la UI al modelo de Firebase.

---

# EPIC 5 — Listado de prestadores

### US-050 — Ver prestadores por categoría

**Prioridad:** Crítica  
**Dependencias:** US-032, US-040

### UI esperada

```text
< Electricistas

Carlos Electricidad
★ 4.7
San Francisco

ElectroFix
★ 4.5
San Francisco
```

### Datos mínimos por tarjeta

- Foto/avatar
- Nombre
- Categoría
- Ciudad
- Rating, si existe

### Criterios de aceptación

- Consulta prestadores por `categoryId`.
- Solo muestra prestadores activos.
- Permite abrir detalle.
- Maneja loading.
- Maneja error.
- Maneja estado vacío.

---

### US-051 — Estado vacío de prestadores

**Prioridad:** Media  
**Dependencias:** US-050

### Texto sugerido

> Todavía no encontramos prestadores para esta categoría.

---

### US-052 — Reintento ante error

**Prioridad:** Media  
**Dependencias:** US-050

### Criterios de aceptación

Ante error:

> No pudimos cargar los prestadores.

Botón:

```text
Reintentar
```

---

# EPIC 6 — Perfil del prestador

### US-060 — Ver perfil del prestador

**Prioridad:** Crítica  
**Dependencias:** US-050

### UI esperada

```text
Carlos Electricidad

★ 4.7

Electricista
San Francisco

Sobre mí
Instalaciones eléctricas,
reparaciones y mantenimiento.

[ Solicitar contacto ]
```

### Datos visibles

- Nombre
- Foto
- Categoría
- Descripción
- Ciudad
- Rating

### Criterios de aceptación

- Carga prestador por ID.
- Muestra información completa.
- Tiene acción principal `Solicitar contacto`.

---

# EPIC 7 — Solicitudes de contacto

### US-070 — Crear solicitud de contacto

**Prioridad:** Crítica  
**Dependencias:** US-060

### Objetivo

Registrar la conversión principal del MVP.

### Implementación

Crear:

```text
serviceRequests/{requestId}
```

Con:

```text
clientId
providerId
categoryId
message
status = PENDING
createdAt
```

### Criterios de aceptación

- Solo usuarios autenticados pueden crear solicitudes.
- Se deshabilita el botón mientras se procesa.
- No se generan solicitudes duplicadas por doble tap.
- Se muestra confirmación.

---

### US-071 — Confirmación de solicitud

**Prioridad:** Alta  
**Dependencias:** US-070

### Mensaje

> Solicitud enviada correctamente.

---

# EPIC 8 — Mis solicitudes

### US-080 — Listar solicitudes del usuario

**Prioridad:** Alta  
**Dependencias:** US-070

### UI esperada

```text
Mis solicitudes

Carlos Electricidad
Electricista
Pendiente

Plomería García
Plomero
Contactado
```

### Criterios de aceptación

- Solo muestra solicitudes del usuario actual.
- Muestra prestador.
- Muestra categoría.
- Muestra estado.
- Maneja loading.
- Maneja error.
- Maneja estado vacío.

---

# EPIC 9 — Navegación principal

### US-090 — Bottom Navigation

**Prioridad:** Alta  
**Dependencias:** US-020, US-080

### Secciones

```text
Inicio
Solicitudes
Perfil
```

### Criterios de aceptación

- Navegación funcional.
- Mantiene correctamente el estado de cada sección cuando corresponda.
- No duplica pantallas innecesariamente en el back stack.

---

# EPIC 10 — Búsqueda

### US-100 — Buscar servicios

**Prioridad:** Media  
**Dependencias:** US-031, US-050

### Alcance MVP

Buscar por:

- Nombre de categoría
- Nombre de prestador

### Fuera de alcance

- Búsqueda semántica
- IA
- Ranking inteligente
- Geolocalización

### Criterios de aceptación

- La búsqueda devuelve coincidencias simples.
- Se maneja estado sin resultados.
- Se registra Analytics.

---

# EPIC 11 — Analytics

### US-110 — Integrar Firebase Analytics

**Prioridad:** Crítica  
**Dependencias:** Flujo principal funcional

### Eventos obligatorios

#### category_view

```text
category_id
category_name
```

#### provider_view

```text
provider_id
category_id
```

#### contact_request

```text
provider_id
category_id
```

#### service_search

```text
query
```

### Funnel principal

```text
Home
 ↓
category_view
 ↓
provider_view
 ↓
contact_request
```

### Criterios de aceptación

- Los eventos se registran.
- No se incluyen datos personales sensibles.
- Los nombres de eventos son consistentes.

---

# EPIC 12 — Datos de prueba

### US-120 — Crear seed inicial

**Prioridad:** Alta  
**Dependencias:** Modelos Firestore listos

### Dataset mínimo

- 5 categorías.
- 3 prestadores por categoría.
- Aproximadamente 15 prestadores.

### Criterios de aceptación

- Los datos permiten probar todo el flujo.
- Existe documentación para volver a cargar el seed.
- No es necesario modificar la UI para agregar nuevos datos.

---

# EPIC 13 — Seguridad Firebase

### US-130 — Configurar Firestore Security Rules

**Prioridad:** Crítica  
**Dependencias:** Modelos Firestore listos

### Reglas esperadas

```text
categories
read → usuarios autenticados

providers
read → usuarios autenticados

serviceRequests
create → usuario autenticado

serviceRequests
read → solamente propietario
```

### Criterios de aceptación

- Firestore no queda abierto públicamente.
- Un usuario no puede consultar solicitudes ajenas.
- Las reglas están versionadas en el repositorio.

---

# EPIC 14 — UX y estados de pantalla

### US-140 — Estados comunes

**Prioridad:** Alta

Toda pantalla de datos debe contemplar:

- Loading
- Success
- Empty
- Error

### Criterios de aceptación

- Nunca se muestra pantalla vacía sin explicación.
- Los botones se deshabilitan durante operaciones.
- Los errores se comunican de forma comprensible.
- Se usan Snackbar o mensajes equivalentes cuando corresponde.
- La UI soporta scroll donde sea necesario.

---

# EPIC 15 — Testing

### US-150 — Unit tests

**Prioridad:** Alta

### Cubrir

- ViewModels
- Validaciones
- Use Cases

### Criterios de aceptación

- Los tests relevantes pasan.
- La lógica crítica no depende de Firebase real.

---

### US-151 — Repository tests

**Prioridad:** Media

### Objetivo

Validar comportamiento de repositorios con mocks/fakes.

---

### US-152 — Prueba manual del flujo principal

**Prioridad:** Crítica

### Flujo obligatorio

```text
Registro
→ Login
→ Home
→ Categoría
→ Prestador
→ Solicitud
→ Mis solicitudes
→ Logout
```

### Criterios de aceptación

- Todo el flujo puede completarse sin errores.
- Analytics registra los eventos esperados.

---

# 7. Fuera del MVP

No implementar todavía:

- Chat
- Pagos
- Reseñas
- Mapas
- Geolocalización
- Recomendaciones
- Push notifications
- Presupuestos
- Marketplace de pujas
- Favoritos
- Agenda
- Reserva de horarios
- Verificación de identidad
- Panel administrativo
- Panel web
- Suscripciones
- Ranking complejo
- IA

Estas funcionalidades quedan reservadas para iteraciones futuras.

---

# 8. Roadmap futuro

## Fase 2

```text
Prestadores reales
+
Perfil profesional
+
Reseñas
+
Favoritos
```

## Fase 3

```text
Geolocalización
+
Google Maps
+
Distancia
+
Búsqueda cercana
```

## Fase 4

```text
Chat cliente ↔ prestador
```

## Fase 5

```text
Presupuestos
+
Aceptación
+
Estado del trabajo
```

## Fase 6

```text
Pagos
+
Comisiones
+
Monetización
```

## Fase 7

```text
Push notifications
+
Retención
+
Personalización
```

---

# 9. Orden recomendado de implementación

Codex o Claude Code debe implementar el proyecto en este orden:

1. US-001 — Bootstrap del proyecto.
2. US-002 — Arquitectura base.
3. Firebase configuration.
4. US-010 a US-013 — Authentication.
5. Navegación base.
6. US-030 y US-031 — Categorías.
7. US-040 y US-050 — Prestadores.
8. US-060 — Perfil del prestador.
9. US-070 y US-071 — Solicitud de contacto.
10. US-080 — Mis solicitudes.
11. US-090 — Bottom Navigation.
12. US-100 — Búsqueda.
13. US-110 — Analytics.
14. US-130 — Security Rules.
15. US-120 — Seed de datos.
16. US-140 — Estados UX.
17. US-150 a US-152 — Tests.
18. Polish final.

No implementar múltiples historias a la vez si no es necesario.

---

# 10. Definition of Done global

Una historia se considera terminada cuando:

- Compila.
- No rompe funcionalidades anteriores.
- Respeta la arquitectura definida.
- Maneja loading cuando corresponde.
- Maneja errores.
- Maneja estado vacío cuando corresponde.
- No contiene credenciales hardcodeadas.
- No introduce dependencias innecesarias.
- Puede probarse manualmente.
- Los nombres del código están en inglés.
- Los textos visibles pueden estar en español.
- Incluye tests cuando corresponda.
- Cumple sus criterios de aceptación.

---

# 11. Milestone TP2 — MVP v0.1

El MVP se considera completo cuando una persona puede realizar:

```text
Crear cuenta
     ↓
Iniciar sesión
     ↓
Ver categorías
     ↓
Elegir categoría
     ↓
Ver prestadores
     ↓
Abrir prestador
     ↓
Solicitar contacto
     ↓
Consultar su solicitud
```

Y el equipo puede medir:

```text
qué categorías consulta
qué prestadores visualiza
cuántas solicitudes genera
qué búsquedas realiza
```

---

# 12. Instrucciones para Codex / Claude Code

Al implementar una historia:

1. Leer este archivo completo antes de modificar código.
2. Implementar únicamente el alcance de la historia solicitada.
3. No agregar funcionalidades fuera del MVP salvo que sean necesarias para completar la historia.
4. Respetar la arquitectura definida.
5. Evitar acceso directo a Firebase desde la UI.
6. Mantener nombres de clases, métodos y variables en inglés.
7. Mantener textos visibles de la aplicación en español.
8. Agregar tests cuando corresponda.
9. Ejecutar build y tests antes de dar la tarea por terminada.
10. Informar:
   - archivos creados;
   - archivos modificados;
   - decisiones técnicas;
   - tests ejecutados;
   - pendientes o riesgos detectados.

Ejemplo de instrucción:

```text
Implementá US-050 del BACKLOG.md.

Respetá todas las dependencias, arquitectura y criterios de aceptación definidos.
No avances con historias posteriores.
Al finalizar, ejecutá build/tests y resumí los cambios realizados.
```
