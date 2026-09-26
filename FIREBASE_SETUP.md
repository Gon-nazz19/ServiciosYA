# Configuración local de Firebase

El proyecto compila sin credenciales y muestra un mensaje de configuración cuando Firebase no está disponible.

Para habilitar autenticación y Firestore:

1. Crear o seleccionar un proyecto en Firebase Console.
2. Registrar una app Android con el package `com.example.serviciosya`.
3. Descargar `google-services.json` y colocarlo en `app/google-services.json`.
4. Habilitar Authentication con Email/Password.
5. Crear la base de datos de Cloud Firestore.
6. Habilitar Google Analytics en el proyecto de Firebase (Configuración del proyecto > Integraciones) y descargar nuevamente `google-services.json` para que incluya la configuración de Analytics.
7. Ejecutar la app nuevamente. Gradle aplica el plugin de Google Services automáticamente cuando encuentra el archivo.

`app/google-services.json` está ignorado por Git para mantener la configuración de cada entorno fuera del repositorio.

## Verificar eventos de Analytics

Los eventos `category_view`, `provider_view`, `contact_request` y `service_search` se pueden ver en tiempo real en Firebase Console > Analytics > DebugView activando el modo debug en el dispositivo:

```bash
adb shell setprop debug.firebase.analytics.app com.example.serviciosya
```
