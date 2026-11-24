# C10 Control Pro — Aplicación Android propia

Proyecto Android Java construido desde cero para probar en la pantalla Android del Leapmotor C10. Su código de aplicación no depende de Qonecta, UNIT, HeadQLink ni Charly.

**Nombre:** C10 Control Pro · **Paquete:** `com.leapmotorguia.c10control` · **Versión:** `1.0.0-original` · **SDK mínimo:** 26.

## Funciones reales de v1

- Panel con velocidad GPS de Android y modelo de coche seleccionado.
- Selección C10 REEV, C10 BEV, C10 Pro Max, B10 y T03. Capacidad útil editable.
- Registro local manual de porcentajes de batería, distancia, litros de combustible, consumo bruto y regeneración; cálculos independientes y reseteo.
- GPS con cronometraje experimental 0–100 (desde parada) y 80–120 (tras estabilizar 3 s a 80 ±1,5). Solo cuando la app está abierta y tiene señal GPS válida. No homologado.
- Auditoría read-only de permisos, sensores Android, Camera2, USB VID/PID y aplicaciones visibles.
- Lanzador de aplicaciones Android existentes y enlaces oficiales para Google Maps, Spotify, Netflix, Max, Prime Video y Pluto TV. Vídeo con confirmación de coche parado.
- Navegador web interno basado en Android WebView.
- Comprobador opcional de `update.json` y selección de APK descargada para invocar el instalador Android.

## Límites conocidos

**No incorpora** receptor Android Auto ni Apple CarPlay, telemetría directa CAN/BMS, apertura/cierre remotos, cámaras de aparcamiento ni lectura automática de combustible/regeneración. Detectar APIs Android no equivale a acceso a sistemas propietarios de Leapmotor. Los resultados de energía no se inventan: son calculados exclusivamente sobre campos que el usuario rellena.

Los contenidos DRM pueden no reproducirse en el coche. La compatibilidad de instalación con LeapOS no puede verificarse sin probar el equipo real.

## Construcción

```sh
# JDK 17, Android SDK 35 y Gradle 8.13
# desde el directorio original-c10-control/ dentro del repositorio:
../gradlew :app:assembleRelease
```

El resultado release se genera sin firmar. Se firma localmente con una clave privada que no se incluye en el repositorio. Nunca publicar el almacén de claves ni sus contraseñas.

## Privacidad y seguridad

No hay servidores de telemetría ni cuentas de usuario; los datos manuales se guardan solo en SharedPreferences local. El GPS se utiliza para mostrar velocidad y cronometrar aceleraciones; no se envía ubicación. No se controlan funciones de conducción, frenos, dirección, cámaras ni centralitas. Pruebas de aceleración solo en recinto cerrado autorizado.
