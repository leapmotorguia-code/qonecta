# C10 Nativa Plus 0.9 alternativa
Alfa separada de 0.8. No modifica AutoKit, firmware ni el repositorio original.
## Diferencias
- Identificador com.leapmotorguia.c10nativealt: puede coexistir con 0.8 y evita conflictos de firma con ella.
- minSdk/targetSdk 30 (Android 11), frente a 29/29 de 0.8.
- Ubicación, ubicación de red, GPS, pantalla táctil y faketouch no obligatorios en el manifiesto.
- Retirado DummyVpnService heredado del flavor github: no utilizado en el host nativo.
- Misma selección de paneles, herramientas y limitaciones funcionales de 0.8.
## Verificación y límites
Se compila una vez incluyendo ARM64 y x86_64, se prueban los paneles en emulador API30 y se retiran las bibliotecas x86_64. El APK ARM64 final conserva exactamente los demás contenidos probados, se alinea y firma con un certificado local de prueba. No es certificado de Leapmotor.
El target30 cambia las reglas de almacenamiento respecto de target29; las funciones de archivos siguen pendientes de verificación real. Ninguna de estas pruebas demuestra aceptación por LeapOS ni el motivo del rechazo anterior.
No se implementan acceso a cámaras, modo centinela, telemetría real, aparcamiento remoto ni aumento de potencia.
## Prueba
Copiar el APK al pendrive. Con coche aparcado, abrirlo en el explorador ya utilizado. Nombre esperado: C10 Nativa Plus 0.9 Alternativa. Probar 0.8 primero; 0.9 únicamente si falla. Si ambas fallan con el mismo mensaje, detener las pruebas repetidas y obtener diagnóstico del instalador antes de fabricar más variantes.
Fuente y licencia AGPL-3.0 conservadas en esta rama.
