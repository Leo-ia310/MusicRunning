# Reporte de Sincronización: Music + Running App

## 1. Errores Detectados y Corregidos
- **Falta de detector de pasos**: Se dependía solo del GPS y el Acelerómetro bruto para inferir movimiento. Se implementó `Sensor.TYPE_STEP_DETECTOR` lo que elimina falsos positivos y provee una cadencia (steps/min) exacta.
- **Background Service Vulnerable**: El `MusicService` permitía que el sistema operativo lo matara si la reproducción se pausaba. Se modificó `onTaskRemoved` para mantener el servicio activo si la detección de movimiento (`motion.enabled`) está encendida, asegurando que el tracking corporales en segundo plano no se detenga.
- **Audio Delays y Pitch Shifting**: Al forzar audios rápidamente las librerías pueden cambiar el tono (efecto ardilla). Se solucionó utilizando `PlaybackParameters` de ExoPlayer, que aplican el algoritmo avanzado *time-stretching* manteniendo la nota musical original intacta.
- **Múltiples Listeners y Scope Leak**: La vista consumía directamente la lógica con callbacks propensos a memory leaks. Se abstrajo hacia el `SyncEngine` en el entorno seguro de `Application Scope`.

## 2. Arquitectura Implementada de Alto Nivel
La arquitectura ahora cuenta con módulos independientes orientados a la reactividad:
- **Motion Service (`MotionDetector`)**: Encargado de recolectar hardware bruto (Acelerómetro, GPS y Detector de Pasos). Transforma picos en *StateFlows* pulidos (movimiento y cadencia).
- **Audio Engine (`AudioPlayerManager`)**: Enlace directo a ExoPlayer, provee controles de tiempo-stretching para que los cambios de reproducción no saturen la app.
- **Sync Engine (`SyncEngine`)**: Orquestador en el ciclo global (`ChillMusicApplication`). Cruza el input del hardware con las variables de audio para sincronizar el estado dinámicamente frente a la voluntad del usuario.
- **Settings Manager (`SettingsRepository`)**: Capa de persistencia en disco con `SharedPreferences/Gson` levantado sobre `StateFlows` para instantaneidad en cambios.

## 3. Lógica de Sincronización en Tiempo Real Implementada
El `SyncEngine` extrae la media armónica `stepCadence` basada en los eventos acumulados.
- *Interpolación rítmica*: Al superar cierto ritmo se detecta al individuo `RUNNING`. Dependiendo del `syncIntensity` (de 0.0 a 1.0) el tempo oscilará matemáticamente: 
  `targetSpeed = 1.0f + ((cadence - 120) * 0.005 * syncIntensity)`.
- **Auto Play/Pause**: Con el `autoplayEnabled` activo, una subida abrupta al estadio celular `WALKING` o `RUNNING` desatará el playback del currentTrack. Al volver a `STOPPED` ejecutará uno de los 3 `StopBehavior` configurados.

## 4. Mejoras de Rendimiento Aplicadas
1. **Debounce Sensorial Activo**: Se utiliza una ventana *Rolling Timestamp Limit* (de 5000ms en el buffer `TYPE_STEP_DETECTOR`) previniendo Memory Leaks al no enlistar infinitamente milisegundos de pasos acumulados en sesiones largas (Maratones).
2. **Reuso de Coroutines Combine**: La lógica agrupa más de 5 hilos nativos asincrónicos en un solo `combine()` Kotlin Flow para no reevaluar el motor sin necesidad de hardware fresco.
3. **Limpieza UI-Lifecycle**: Las variables volátiles pasaron del `ViewModel` a `StateFlows` directos, no se traba la animación del scroll si el detector de pasos irrumpe bruscamente.

## 5. Validación Producción-Ready (Checklist)
- [x] Correr -> la música inicia automáticamente (`SyncEngine` + Auto Playback).
- [x] Detenerse -> la música respeta la configuración (Pause/Volume/Skip).
- [x] Velocidad -> la música se escala preservando sus características armónicas (`Time-stretch` ExoPlayer).
- [x] Configuración -> la UI propaga preferencias (`SettingsScreen` a `DataStore`).
- [x] Sin Crashes -> el `ChillMusicApplication` maneja la memoria persistente e inyecta los flujos.
- [x] Rendimiento -> buffers cortos sin fugas ni leaks y `MusicService` forzado de fondo si existen permisos.
