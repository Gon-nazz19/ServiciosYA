# Resultados de las pruebas y análisis

> Entregable 3 del TP2: etapas **Measure** y **Learn**.
> Los campos entre `_…_` se completan con los datos reales de la prueba. **No inventar números**: si un dato no se midió, se aclara.

## 1. Contexto de la prueba

| Dato | Valor |
|---|---|
| Fecha y hora de inicio | _…_ |
| Cantidad de participantes | _…_ |
| Perfil de los participantes | _… (edad aproximada, relación con el equipo, si ya usan apps similares)_ |
| Dispositivos | _… (celulares propios con APK / emulador)_ |
| Versión probada | commit `_…_` del repositorio |

Procedimiento: se siguió la [guía de prueba con usuarios](PRUEBA_USUARIOS.md).

## 2. Métricas de uso (Firestore)

Generadas con:

```text
node tools/metrics/report.js --since _…_ --exclude _…_
```

> _Pegar acá la salida del script (resumen, solicitudes por categoría y por prestador)._

## 3. Métricas de comportamiento (Google Analytics)

Sin configuración extra, Analytics muestra cuántas veces ocurrió cada evento y cuántos usuarios lo generaron (**Informes → Participación → Eventos**, 24 a 48 horas después de la prueba).

| Evento | Qué representa | Cantidad de eventos | Usuarios |
|---|---|---|---|
| `category_view` | Abrió una categoría | _…_ | _…_ |
| `provider_view` | Abrió el perfil de un prestador | _…_ | _…_ |
| `contact_request` | Envió una solicitud | _…_ | _…_ |
| `service_search` | Hizo una búsqueda | _…_ | _…_ |

**Embudo** (con la columna Usuarios):

| Paso | Usuarios | Avance respecto del paso anterior |
|---|---|---|
| Abrió una categoría | _…_ | — |
| Abrió un prestador | _…_ | _… %_ |
| Pidió contacto | _…_ | _… %_ |

> _Capturas de pantalla: Tiempo real durante la sesión e informe de Eventos._

## 4. Resultados cualitativos

**Observación** (resumen de la planilla):

| Tarea | Completada sola | Con dudas | No completada | Principales dudas observadas |
|---|---|---|---|---|
| 1. Crear cuenta | _…_ | _…_ | _…_ | _…_ |
| 2. Encontrar plomero y pedir contacto | _…_ | _…_ | _…_ | _…_ |
| 3. Ver la solicitud | _…_ | _…_ | _…_ | _…_ |
| 4. Buscar técnico de PC | _…_ | _…_ | _…_ | _…_ |
| 5. Buscar un servicio inexistente | _…_ | _…_ | _…_ | _…_ |

**Encuesta:**

| Pregunta | Promedio (1–5) |
|---|---|
| Facilidad para encontrar un prestador | _…_ |
| Confianza para contactar | _…_ |
| La usaría de nuevo | _…_ |

Respuestas abiertas más repetidas:
- ¿Cómo consiguen hoy un prestador? _…_
- Información que les faltó para decidir: _…_
- Servicios que buscaron y no encontraron: _…_

## 5. Análisis: comparación con las métricas de éxito

Umbrales definidos **antes** de la prueba en [`MVP.md`](MVP.md#6-métricas-de-éxito):

| Métrica | Umbral | Resultado | ¿Cumple? |
|---|---|---|---|
| Conversión a solicitud | ≥ 30 % | _…_ | _✅ / ❌_ |
| Avance categoría → prestador | ≥ 60 % | _…_ | _✅ / ❌_ |
| Avance prestador → solicitud | ≥ 30 % | _…_ | _✅ / ❌_ |
| Búsquedas sin resultado | ≤ 20 % | _…_ | _✅ / ❌_ |
| "La usaría de nuevo" | ≥ 4 | _…_ | _✅ / ❌_ |

## 6. Aprendizajes (Learn)

- **¿Se valida la hipótesis?** _…_
- **¿Dónde se pierden los usuarios en el embudo y por qué?** _…_
- **¿Qué nos sorprendió?** _…_
- **Limitaciones de la prueba:** _… (por ejemplo: pocos participantes, prestadores ficticios, conocidos del equipo)_

## 7. Decisión: ¿pivotar o perseverar?

> _Decisión y justificación basada en la sección 5._

**Próximos experimentos o cambios** (cada uno con la métrica que lo evaluaría):

| Cambio propuesto | Hallazgo que lo motiva | Métrica para evaluarlo |
|---|---|---|
| _…_ | _…_ | _…_ |

Los cambios que requieren componentes nuevos se reflejan en el [diagrama de arquitectura actualizado](ARQUITECTURA.md#3-arquitectura-actualizada).
