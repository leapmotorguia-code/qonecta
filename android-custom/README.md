# Qonecta C10 REEV Custom Modules

Proyecto Android Studio completo en Kotlin listo para compilar o integrar dentro del repositorio oficial de **CharlysEV / Qonecta**.

## Contenido del proyecto
1. `PerformanceTracker.kt`: Cronómetro de **0-100 km/h** y recuperación **80-120 km/h** con reloj monotónico.
2. `TripEnergyBalanceTracker.kt`: Cálculo acumulado de **kWh consumidos**, **kWh regenerados**, **kWh generados por el motor de gasolina (REEV)**, balance neto y **% de recuperación**.
3. `MediaStreamingLauncher.kt`: Buscador **Google** integrado en `WebView` + lanzador directo de **Netflix**, **Prime Video** y **HBO Max**.
4. `VehicleSentryMonitor.kt`: **Modo Centinela / Vigilancia** basado en acelerómetro y estado de puertas.
5. `MainActivity.kt` + `activity_main.xml`: Interfaz lista para pantalla panorámica del Leapmotor C10 con solicitud automática de permisos de **Ubicación** y **Dispositivos cercanos**.

## Cómo integrarlo en el código fuente original de Qonecta
- Copia el paquete `app/src/main/java/com/qonecta/custom/` dentro de tu proyecto Qonecta en Android Studio.
- Conecta la lectura de velocidad y potencia CAN/OBD del coche llamando a `onVehicleTelemetryUpdated(speedKmh, batteryPowerKw, generatorPowerKw)`.
- Para el código base oficial del puente Android Auto / CarPlay (`Qonecta Unit` / `Qonecta+`), clona el repositorio oficial `CharlysEV/qonecta` en GitHub.
