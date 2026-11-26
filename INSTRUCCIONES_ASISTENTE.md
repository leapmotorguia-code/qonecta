# Reglas del proyecto
1. Mantener `es.drivecore.cockpit`, compileSdk 36, minSdk 28, targetSdk 35.
2. Compilar `gradle :app:assembleDebug` antes de declarar una versión lista.
3. No simular datos; indicar — si no disponibles. AAOS/OBD2 solo lectura.
4. No afirmar que Android Auto/CarPlay funcionan sin receptor integrado probado.
5. Respetar ParkedVideoGate y restricciones OEM; nunca habilitar vídeo en movimiento.
6. Mantener las funciones de 0.4 y documentar cambios y enlace al artifact.
7. No subir claves privadas, credenciales ni código de UNIT. El APK debug de CI no tiene la firma de 0.3.
