# Configuración local de Firebase

El proyecto compila sin credenciales y muestra un mensaje de configuración cuando Firebase no está disponible.

Para habilitar autenticación y Firestore:

1. Crear o seleccionar un proyecto en Firebase Console.
2. Registrar una app Android con el package `com.example.serviciosya`.
3. Descargar `google-services.json` y colocarlo en `app/google-services.json`.
4. Habilitar Authentication con Email/Password.
5. Crear la base de datos de Cloud Firestore.
6. Ejecutar la app nuevamente. Gradle aplica el plugin de Google Services automáticamente cuando encuentra el archivo.

`app/google-services.json` está ignorado por Git para mantener la configuración de cada entorno fuera del repositorio.
