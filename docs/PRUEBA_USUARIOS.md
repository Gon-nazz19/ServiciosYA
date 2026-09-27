# Guía para la prueba con usuarios reales

> Etapa **Measure** del TP2. Resultado: los datos para [`RESULTADOS.md`](RESULTADOS.md).

> **Cómo se hizo en este TP:** la cátedra habilitó reemplazar la prueba con personas por una **simulación de usuarios**. La hicimos con [`tools/simulation`](../tools/simulation/README.md), que maneja la app real en el emulador siguiendo perfiles de comportamiento, con las mismas tareas de esta guía. Esta guía queda como protocolo para repetir la prueba con personas reales en la próxima iteración.

## Objetivo

Validar la hipótesis del [MVP](MVP.md): *¿las personas usan la app para encontrar un prestador y pedirle contacto?* Además, detectar dónde se traban.

## Antes de la sesión

1. **Participantes:** entre 5 y 10 personas que **no** hayan participado del desarrollo. Pueden ser familiares, amigos o compañeros de otras materias.
2. **Anotar la fecha y hora de inicio**, porque después se usa para filtrar las métricas (`--since`).
3. **Cómo prueban la app** (una de estas opciones):
   - **En su celular Android (7.0 o superior):** generar el APK con *Build → Build App Bundle(s) / APK(s) → Build APK(s)* en Android Studio, o `./gradlew assembleDebug`. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`. Pasarlo por WhatsApp o Drive e instalarlo; el celular va a pedir permitir "instalar apps de origen desconocido".
   - **En la PC:** usar el emulador de Android Studio y que la persona lo maneje con el mouse.
4. Cada participante **crea su propia cuenta**. Puede ser un email inventado, porque no se verifica.
5. El equipo **no usa la app** durante la sesión, o anota sus emails para excluirlos después con `--exclude`.

## Durante la sesión (5 a 10 minutos por persona)

Leer la consigna y **no ayudar**. Si la persona pregunta, responder "¿qué harías vos?" y anotar la duda: cada duda es un hallazgo.

> "Imaginá que se te rompió un caño en tu casa y necesitás un plomero. Usá esta app para conseguir uno."

Tareas, en este orden:

| # | Tarea | Qué observar |
|---|---|---|
| 1 | Crear una cuenta | ¿Entiende los campos? ¿Hay errores? |
| 2 | Encontrar un plomero y pedirle contacto | ¿Va por categoría o por búsqueda? ¿Duda antes de pedir contacto? ¿Escribe mensaje? |
| 3 | Confirmar que la solicitud quedó enviada | ¿Encuentra la pestaña Solicitudes? |
| 4 | "Ahora necesitás alguien que te arregle la computadora" | ¿Usa la búsqueda? ¿Qué palabras escribe? |
| 5 | Buscar un servicio que no existe, por ejemplo "cerrajero" | ¿Qué hace cuando no hay resultados? |

### Planilla de observación (una fila por participante)

| Participante | Tarea 1 | Tarea 2 | Tarea 3 | Tarea 4 | Tarea 5 | Tiempo total | Dudas / comentarios textuales |
|---|---|---|---|---|---|---|---|
| P1 | ✅ / ⚠️ / ❌ | | | | | | |
| P2 | | | | | | | |
| P3 | | | | | | | |
| P4 | | | | | | | |
| P5 | | | | | | | |

✅ la completó sola · ⚠️ la completó con dudas o errores · ❌ no la completó

## Encuesta post-prueba

Hacerla al terminar. Puede ser en Google Forms.

1. ¿Qué tan fácil fue encontrar un prestador? (1 = muy difícil … 5 = muy fácil)
2. ¿Qué tan seguro/a te sentirías de contactar a un prestador desde esta app? (1 … 5)
3. ¿Usarías esta app la próxima vez que necesites un servicio? (1 = seguro que no … 5 = seguro que sí)
4. Hoy, cuando necesitás un plomero o electricista, ¿cómo lo conseguís?
5. ¿Qué información te faltó para decidir a quién contactar?
6. ¿Qué servicio buscaste o te gustaría que tenga la app y no encontraste?

## Después de la sesión

1. Correr las métricas de Firestore (ver [`tools/metrics/README.md`](../tools/metrics/README.md)):
   ```powershell
   node report.js --since <fecha-hora-de-inicio> --exclude <emails-del-equipo>
   ```
2. Revisar Google Analytics: **Informes → Tiempo real** durante la sesión (hacer capturas) e **Informes → Participación → Eventos** 24 a 48 horas después.
3. Completar [`RESULTADOS.md`](RESULTADOS.md).
