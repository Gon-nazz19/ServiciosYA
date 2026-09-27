# Resultados de las pruebas y análisis

> Entregable 3 del TP2: etapas **Measure** y **Learn**.

## 1. Cómo se hizo la prueba

La cátedra habilitó reemplazar la prueba con personas por una **simulación de usuarios**. La implementamos con [`tools/simulation`](../tools/simulation/README.md):

- Un script **maneja la app real** en el emulador de Android: toca, escribe y navega como una persona. No se inventan datos: las cuentas, las solicitudes y los eventos de Analytics los genera la app.
- Cada usuario simulado arranca con la app recién instalada, crea su cuenta y sigue un **perfil de comportamiento** asignado al azar, con semilla fija para que sea reproducible.

| Dato | Valor |
|---|---|
| Fecha | 26/09/2026, 23:22 a 23:51 |
| Usuarios simulados | **25**, sin fallas de automatización |
| Dispositivo | Emulador Pixel 7, Android 15 (API 35) |
| Backend | Firebase `tp-2-54a64`, el mismo de producción del MVP |
| Corrida | `tools/simulation/runs/20260926-232257` (semilla `2026`) |
| Duración promedio de una sesión | 69 s (entre 36 y 103 s) |

**Perfiles** (el peso es la probabilidad de cada perfil; "Usuarios" es lo que salió en esta corrida):

| Perfil | Comportamiento | Peso | Usuarios |
|---|---|---|---|
| decidido | Entra a la categoría que necesita y pide contacto al primero | 30 % | 14 |
| comparador | Mira 2 o 3 prestadores y pide contacto al de mejor rating | 22 % | 4 |
| rebote | Se registra, mira la pantalla principal y se va | 13 % | 3 |
| explorador | Mira categorías y prestadores pero no pide contacto | 15 % | 2 |
| buscador | Usa el buscador, a veces buscando servicios que no existen | 20 % | 2 |

> ⚠️ Con solo 25 usuarios, el azar dejó **más "decididos" de lo esperado** (56 % frente a 30 %). Todo lo que depende de la mezcla de perfiles, sobre todo la conversión, sale **optimista**.

## 2. Métricas de uso (Firestore)

Generadas con `node tools/metrics/report.js --since 2026-09-26T23:22`:

| Métrica | Valor |
|---|---|
| Usuarios registrados | 25 |
| Usuarios que enviaron ≥ 1 solicitud | 20 |
| **Conversión a solicitud** | **80,0 %** |
| Solicitudes totales | 20 |
| Solicitudes con mensaje | 10 (50 %) |
| Prestadores con ≥ 1 solicitud | **5 de 15** |

| Categoría | Solicitudes | % |
|---|---|---|
| Electricista | 5 | 25 % |
| Plomero | 5 | 25 % |
| Técnico PC | 5 | 25 % |
| Aire acondicionado | 3 | 15 % |
| Jardinero | 2 | 10 % |

| Prestador | Categoría | Solicitudes |
|---|---|---|
| Luz y Fuerza Martínez (★ 5.0) | Electricista | 5 |
| Plomería García (★ 4.6) | Plomero | 5 |
| PC Doctor (★ 4.9) | Técnico PC | 5 |
| Frío Total (★ 4.8) | Aire acondicionado | 3 |
| Verde Jardín (★ 4.6) | Jardinero | 2 |

## 3. Métricas de comportamiento (embudo y Analytics)

Generadas con `python tools/simulation/analyze.py`:

| Paso | Usuarios | % del total | % del paso anterior |
|---|---|---|---|
| Se registró | 25 | 100 % | — |
| Exploró (categoría o búsqueda) | 22 | 88 % | 88 % |
| Abrió un prestador | 22 | 88 % | 100 % |
| Pidió contacto | 20 | 80 % | 91 % |

| Dónde abandonaron los 5 que no pidieron contacto | Usuarios |
|---|---|
| En la pantalla de inicio, sin explorar | 3 |
| Después de explorar, sin decidirse | 2 |

| Búsqueda | ¿Encontró algo? |
|---|---|
| "plomero" | sí |
| "electric" | sí |
| "herrero" | **no**: el servicio no está en el catálogo |
| "albanil" | **no**: el servicio no está en el catálogo |

**Control de la medición:** los eventos que la app envió a Google Analytics, capturados del emulador, coinciden **uno a uno** con las acciones simuladas. La instrumentación de la EPIC 11 mide lo que tiene que medir.

| Evento | Acciones simuladas | Eventos emitidos por la app |
|---|---|---|
| `category_view` | 23 | 23 ✅ |
| `provider_view` | 33 | 33 ✅ |
| `contact_request` | 20 | 20 ✅ |
| `service_search` | 4 | 4 ✅ |

> _Capturas de Google Analytics: agregar acá **Informes → Tiempo real** y, 24 a 48 horas después, **Informes → Participación → Eventos**._

## 4. Hallazgos

| # | Hallazgo | Evidencia | Qué hicimos o proponemos |
|---|---|---|---|
| H1 | **El teclado tapaba el botón "Solicitar contacto"**, y la tecla 🔍 del teclado no hacía nada en el buscador | Lo detectó el simulador: para pedir contacto después de escribir un mensaje había que cerrar el teclado a mano | ✅ **Corregido** en esta iteración (commit `8789e64`): el contenido se acomoda sobre el teclado, tocar afuera lo cierra y 🔍 muestra los resultados |
| H2 | **Todas las solicitudes fueron al prestador mejor calificado** de su categoría: 5 de 15 prestadores concentran el 100 % | Tabla de solicitudes por prestador | En parte es un supuesto de los perfiles, pero muestra un riesgo real: con la lista ordenada por rating, los nuevos no reciben trabajo. Proponemos disponibilidad y rotación, y medirlo con usuarios reales |
| H3 | **La mitad de las búsquedas no encontró nada**, porque se buscaron servicios que la app no ofrece (herrero, albañil) | 2 de 4 búsquedas | La demanda insatisfecha es información valiosa y hoy se pierde. Proponemos guardar las búsquedas sin resultado y ofrecer "avisame cuando haya" para decidir qué categorías sumar |
| H4 | **El 70 % de los que pidieron contacto volvió a mirar "Mis solicitudes"**, pero la solicitud queda en *Pendiente* para siempre | 14 de 20 usuarios | Del otro lado nadie la recibe: el prestador no tiene app ni aviso. Es el hueco más grande del producto y motiva el **modo prestador** y las **notificaciones** |
| H5 | **La mitad de las solicitudes llega sin mensaje** | 10 de 20 | El prestador recibe un pedido sin contexto. Proponemos sugerencias rápidas ("Urgente", "Presupuesto", "Instalación") |
| H6 | **3 de 25 usuarios se fueron sin explorar** | Abandono en la pantalla de inicio | Con la simulación no se puede saber por qué. Hay que preguntarlo en la prueba con personas |

## 5. Análisis: comparación con las métricas de éxito

Umbrales definidos **antes** de la prueba en [`MVP.md`](MVP.md#6-métricas-de-éxito):

| Métrica | Umbral | Resultado | ¿Cumple? |
|---|---|---|---|
| Conversión a solicitud | ≥ 30 % | 80 % | ✅ (optimista: ver nota de la sección 1) |
| Avance categoría → prestador | ≥ 60 % | 100 % | ✅ |
| Avance prestador → solicitud | ≥ 30 % | 91 % | ✅ |
| Búsquedas sin resultado | ≤ 20 % | 50 % | ❌ (muestra chica: 4 búsquedas) |
| "La usaría de nuevo" | ≥ 4 | — | No se puede medir con una simulación |

## 6. Aprendizajes (Learn)

- **¿Se valida la hipótesis?** **Parcialmente.** La simulación demuestra que el flujo completo funciona de punta a punta sin errores y que la medición es confiable (los eventos coinciden 1:1). Pero la conversión refleja los **supuestos** de los perfiles, no la conducta de personas reales: **no valida el mercado**. La hipótesis de valor sigue abierta.
- **Lo que sí aprendimos del producto:**
  - Había una barrera de usabilidad real en el paso clave del embudo (H1), ya corregida.
  - La app **no cierra el circuito**: la solicitud no le llega a nadie (H4).
  - El orden por rating concentra toda la demanda (H2).
  - Hay demanda de servicios que no ofrecemos y que hoy no registramos (H3).
- **Limitaciones:** los usuarios son simulados; los perfiles y sus pesos los definió el equipo; son solo 25 usuarios, con una mezcla de perfiles desbalanceada; los prestadores son ficticios; no hay datos de satisfacción.

## 7. Decisión: ¿pivotar o perseverar?

**Perseverar**, con dos condiciones:

1. Repetir la prueba con **personas reales** usando el protocolo de [`PRUEBA_USUARIOS.md`](PRUEBA_USUARIOS.md), para validar de verdad la conversión y medir la satisfacción.
2. Priorizar en el próximo ciclo lo que **cierra el circuito**, sin lo cual la conversión no genera valor para ninguno de los dos lados.

**Próximos experimentos o cambios:**

| Cambio propuesto | Hallazgo | Métrica para evaluarlo |
|---|---|---|
| Modo prestador: recibir, aceptar o rechazar solicitudes | H4 | % de solicitudes respondidas y tiempo de respuesta |
| Notificación push al prestador (y al cliente al ser aceptada) | H4 | Tiempo hasta la primera respuesta |
| Guardar búsquedas sin resultado + "avisame cuando haya" | H3 | Ranking de servicios pedidos que no existen |
| Sugerencias de mensaje al pedir contacto | H5 | % de solicitudes con mensaje |
| Disponibilidad y rotación en el listado | H2 | Distribución de solicitudes entre prestadores |
| Prueba con personas reales + encuesta | H6, satisfacción | "La usaría de nuevo" ≥ 4, motivos de abandono |

Los componentes nuevos que requieren estos cambios están en el [diagrama de arquitectura actualizado](ARQUITECTURA.md#3-arquitectura-actualizada).
