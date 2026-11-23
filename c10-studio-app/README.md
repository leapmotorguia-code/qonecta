# C10 Studio 1.0.0 — aplicación Android independiente
La app utiliza una interfaz local WebView y un puente Android nativo. No modifica UNIT, LeapOS ni centralitas. Paquete: `es.leapmotorguia.c10studio`.

## Construcción
El workflow `.github/workflows/c10-studio-apk.yml` compila un APK Android SIN FIRMAR con SDK 35. Se debe firmar localmente con un certificado privado de liberación y conservar esa clave para futuras actualizaciones (no subirla a un repositorio público). Android 8+ (API 26+).

## Límites verificables
- Receptor Android Auto: no incluye un receptor Android Auto propio. Puede iniciar una app receptora previamente instalada. El protocolo y la compatibilidad dependen de hardware/servicio.
- CarPlay: no incluye ni puede prometer un receptor CarPlay autorizado. Abrir AutoKit/Zlink si está instalado no equivale a implementarlo.
- Apps comerciales: se abren mediante sus paquetes instalados o webs externas; el streaming requiere Widevine y certificación de cada proveedor. No se puede forzar su reproducción.
- Parámetros del C10: no hay enlace certificado al bus CAN interno o la nube Leapmotor. Muestra como desconocidos SOC, regeneración, combustible REEV, cámaras/ADAS, TPMS y puertas.
- OBD2 externo: conexión Bluetooth RFCOMM SPP a ELM327 emparejado, consultas estándar modo 01 solamente. Una ECU REEV puede no contestar PID estándar. Sin cambios de parámetros.
- GPS: velocidad y distancia provienen del servicio GPS Android, NO son lecturas de la ECU.
- Actualización: canal JSON por HTTPS, compara versión, SHA-256 y certificado antes de abrir instalador oficial. No realiza instalaciones silenciosas ni elude restricciones de LeapOS.

## Pruebas de aceptación
1. Instalar en un Android de prueba. No desinstalar la APK anterior ni hacer pruebas de vídeo en marcha.
2. Abrir Inicio, Aplicaciones, Proyección, Telemetría, OBD2, Accesos C10 y Actualizaciones; confirmar persistencia de pestaña.
3. Inspeccionar apps instaladas; los enlaces externos nunca deben mostrarse como apps nativas instaladas.
4. Dar permiso GPS y comprobar solo valores reales. No confundir sensores Android con hardware del coche.
5. Vincular ELM327 en Ajustes Bluetooth, leer PID 010D y 010C si están disponibles.
6. Verificar que no ofrece una actualización si el manifiesto solo anuncia la misma versión.
7. Si LeapOS rechaza la instalación por políticas del fabricante, no intentar saltarlas.

Diseño propio; proyecto experimental no avalado por Leapmotor, Apple, Google, Netflix ni otras marcas.
