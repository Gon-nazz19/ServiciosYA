# Métricas de uso

Genera las métricas del análisis del TP2 (etapa **Measure**) a partir de Firebase Auth y Firestore. **Solo lee**: nunca escribe en Firebase.

## Requisitos

- Node.js 22 o superior.
- La misma clave de cuenta de servicio que usa el seed (ver [`tools/seed/README.md`](../seed/README.md)).

## Uso

```powershell
cd tools\metrics
npm install
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ruta\a\serviciosya-service-account.json"

# Todo lo que hay en la base
node report.js

# Solo la sesión de prueba: desde la hora de inicio y sin las cuentas del equipo
node report.js --since 2026-10-01T18:00 --exclude dev1@mail.com,dev2@mail.com

# Guardar el resultado para pegarlo en docs/RESULTADOS.md
node report.js --since 2026-10-01T18:00 > resultados-prueba.md
```

| Opción | Qué hace |
|---|---|
| `--since <fecha>` | Cuenta solo los usuarios creados y las solicitudes hechas desde esa fecha y hora local (`AAAA-MM-DD` o `AAAA-MM-DDTHH:MM`) |
| `--exclude <emails>` | Deja afuera esas cuentas y sus solicitudes (separadas por coma) |

## Qué calcula

- Usuarios registrados, usuarios que enviaron al menos una solicitud y la **conversión a solicitud**.
- Solicitudes totales, solicitudes por usuario y porcentaje de solicitudes con mensaje.
- Solicitudes por categoría, por prestador y por estado.

Las vistas de categorías y prestadores y las búsquedas no se guardan en Firestore: se ven en Google Analytics (ver [`docs/RESULTADOS.md`](../../docs/RESULTADOS.md)).
