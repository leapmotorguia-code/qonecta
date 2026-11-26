# DriveCore Native 0.4 — candidato de código, NO APK verificada

Base: Native 0.3, paquete es.drivecore.cockpit, versionCode 4. No contiene código de UNIT. Mantiene AAOS y OBD2 clásico. RadioEngine sustituido por Media3 ExoPlayer, HLS y fuente OkHttp, audio solamente, sin mostrar vídeo. Las dependencias se declaran mediante Gradle, no se han descargado ni compilado en el entorno actual. Kotlin Coroutines pasa a dependencia Maven.

Compilación prevista: JDK 17, Android SDK 36, Gradle 8.13, AGP 8.10.1. Abrir carpeta en Android Studio o ejecutar `gradle :app:assembleDebug`. El workflow adjunto genera app-debug.apk firmado con la clave de depuración de CI y un listado de dependencias. La firma no coincide necesariamente con 0.3; no instalar encima sin verificar. NO incluir ninguna clave privada en GitHub. Firmar localmente con la misma clave de Native 0.3 y comprobar certificado antes de distribuir. La compilación y compatibilidad con el C10 siguen pendientes. No usar como reemplazo de la 0.3 hasta compilar, firmar y probar.

Pendientes: Open Headunit, LibUSB, Protocol Buffers, Room, MapLibre, Nordic BLE, MPAndroidChart, Filament, FFmpeg y CarPlay. No se declaran bibliotecas que no se usan. El cliente OkHttp se usa para radio; NO implementa login Leapmotor. El soporte HLS de audio no equivale a un catálogo TDT. No se incluyen claves, tokens ni binarios de Charly.

## Native 0.5
Room samples, explicit recording while foreground, up to 30000 rows. Protobuf export schema in docs. MapLibre/OpenFreeMap viewer (not turn-by-turn navigation). Nordic BLE GATT discovery for selected paired adapters; no proprietary OBD BLE protocol guessed. Media3 audio and AAOS retained. No actual Android Auto/CarPlay receiver yet. FFmpeg, Filament, MPAndroidChart and standalone libusb not implemented. All changes require CI and in-car validation. Third-party dependencies keep their original licenses; see upstream projects.
