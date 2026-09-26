# Seed de datos de prueba

Carga en Cloud Firestore el dataset mínimo para probar todo el flujo de la app:

- 5 categorías (`categories/{id}`): Electricista, Plomero, Aire acondicionado, Técnico PC y Jardinero.
- 15 prestadores (`providers/{id}`), 3 por categoría. Todos están activos. Uno no tiene rating, para probar ese caso.

Los datos están en `seed-data.json`. Para sumar categorías o prestadores alcanza con editar ese archivo y volver a correr el script. No hace falta tocar la app.

## Requisitos

- Node.js 22 o superior.
- Una clave de cuenta de servicio del proyecto de Firebase:
  1. En Firebase Console, entrar a **Configuración del proyecto > Cuentas de servicio**.
  2. Tocar **Generar nueva clave privada** y guardar el JSON **fuera del repositorio**, por ejemplo en `C:\Users\<usuario>\firebase\serviciosya-service-account.json`.

> La clave da acceso de administrador al proyecto. No la subas a Git ni la compartas.

## Cargar o recargar el seed

Desde esta carpeta (`tools/seed`):

```bash
npm install
npm run check     # valida seed-data.json sin conectarse a Firebase
```

En PowerShell:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ruta\a\serviciosya-service-account.json"
npm run seed
```

En Git Bash:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/c/ruta/a/serviciosya-service-account.json"
npm run seed
```

El script es idempotente. Cada documento tiene un id fijo, así que correrlo de nuevo actualiza los mismos documentos sin duplicarlos. Si borrás un prestador del JSON, también tenés que borrarlo a mano en Firestore.

El script usa el Admin SDK, que no pasa por las Security Rules. Por eso funciona aunque la app solo tenga permisos de lectura sobre `categories` y `providers`.
