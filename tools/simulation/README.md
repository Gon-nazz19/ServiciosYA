# Simulación de usuarios

Genera los datos de la etapa **Measure** del TP2 con usuarios simulados. La cátedra permite simular usuarios con IA o scripts.

A diferencia de inventar números, el simulador **maneja la app real** en el emulador de Android: toca la pantalla, escribe y navega como una persona. Por eso:

- Las cuentas se crean en Firebase Auth, y las solicitudes en Firestore, a través de la app.
- Los eventos de Google Analytics los emite la app, igual que con un usuario real.
- Cada usuario simulado arranca con la app recién instalada (se borran sus datos), así Analytics lo cuenta como un usuario nuevo.

## Perfiles de comportamiento

Cada usuario recibe un perfil al azar (con semilla fija, así la corrida es reproducible), y su necesidad (plomero, electricista…) sale de pesos que imitan la demanda de una ciudad chica:

| Perfil | Peso | Comportamiento |
|---|---|---|
| decidido | 30 | Entra a la categoría que necesita y pide contacto al primer prestador |
| comparador | 22 | Abre 2 o 3 prestadores y pide contacto al de mejor rating |
| buscador | 20 | Usa el buscador. A veces busca servicios que la app no tiene (gasista, cerrajero…) o describe el problema ("perdida", "luz") en vez del rubro |
| explorador | 15 | Mira categorías y prestadores pero no pide contacto |
| rebote | 13 | Se registra, mira la pantalla principal y se va |

Además: el 60 % de los que piden contacto escribe un mensaje, y el 60 % revisa después *Mis solicitudes*.

## Uso

Requisitos: Python 3.10 o superior, el emulador de Android encendido con la app instalada (*Run* en Android Studio), y el seed cargado.

```bash
cd tools/simulation
python simulate.py --users 25 --seed 2026   # ~25 minutos
python analyze.py                           # analiza la última corrida (Markdown)
```

| Opción | Qué hace |
|---|---|
| `--users N` | Cantidad de usuarios simulados |
| `--seed N` | Semilla. Con la misma semilla se repiten los mismos perfiles y decisiones |
| `--speed X` | Multiplica las pausas entre acciones (0.5 = el doble de rápido) |
| `--verbose` | Muestra cada toque |

Cada corrida queda en `runs/<fecha-hora>/`:
- `actions.jsonl`: todo lo que hizo cada usuario.
- `analytics.log`: los eventos de Analytics que emitió la app, capturados del emulador.
- `summary.json`: perfil, duración y resultado de cada usuario.

`analyze.py` arma el embudo a partir de las acciones y lo **controla** contra los eventos que realmente emitió la app.

Las cuentas simuladas usan emails `sim-<corrida>-NN@serviciosya.test`. Para las métricas de Firestore, usá `tools/metrics/report.js --since <inicio de la corrida>`.

## Limitaciones

- El comportamiento lo definen los perfiles, no personas: mide **cómo responde la app** a distintos comportamientos plausibles, no la motivación real de los usuarios.
- `adb` no puede escribir letras con tilde ni ñ, así que los términos de búsqueda y los mensajes van sin tildes. La búsqueda de la app ignora tildes, así que no afecta los resultados.
- Los prestadores son los del seed (ficticios).
