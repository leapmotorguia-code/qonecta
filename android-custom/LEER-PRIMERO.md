# C10 Laboratorio 1.2 · prueba independiente

APK Android con minSdk28 (Android9) y target35. Android11 entra en el rango de versiones; la aceptación de LeapOS3.50.62 no está comprobada.

Basado en QonectaC10Custom_Project.zip aportado por el usuario, con clases corregidas. No es Qonecta Unit ni una copia del proyecto de CharlysEV, no tiene su firma y no contiene proyección Android Auto/CarPlay. No reemplaza ni modifica UNIT, AutoKit o servicios del coche. Identidad: com.qonecta.custom; nombre: C10 Laboratorio 1.2.

## Instalar y probar
1. Descargar C10-Laboratorio-1.2.apk completo. En Samsung abrir desde Mis archivos > Descargas y autorizar ese origen si Android lo solicita. Mantener Play Protect; permitir que analice el archivo.
2. En el coche, usar el mismo gestor e instalador con el que ya instalaste UNIT. Un reproductor de música no instala APK. No renombrar el archivo a MP3/MP4.
3. Abrir C10 Laboratorio1.2. No desinstalar UNIT. Si falla, fotografiar el mensaje exacto e indicar si ocurrió en Samsung o C10. La aplicación no puede diagnosticar su propio rechazo antes de estar instalada.
4. InstrumentosGPS: primero probar Ejecutar demostración (todo queda marcado DEMO). Para GPS autorizar ubicación precisa y disponer de buena recepción exterior; no necesita internet. Mide 3–100, no 0–100 real, y 80–120 aproximados. Solo pruebas en recinto cerrado autorizado; no manipular el teléfono conduciendo.
5. Eficiencia: cálculos manuales separados y estimación de energía. Balance demostrado con datos simulados. No hay datos CAN/OBD ni lecturas del motor. La carga de batería nunca se presenta automáticamente como regeneración de frenada.
6. Multimedia: enlaces externos Google,Netflix,PrimeVideo,HBO/Max,PlutoTV,Spotify con confirmación de uso estacionado. Necesitan aplicación o navegador externo e internet para webs. No garantiza DRM, vídeo, pantalla dividida ni suscripciones.
7. Vibración: solo acelerómetro del dispositivo si existe y app visible. Se detiene al salir. No es centinela del coche, no ve puertas, cámaras ni impactos mientras el coche duerme. Eventos locales; sin envío remoto.
8. Diagnóstico: guardar informe desde selector de archivos si el coche lo ofrece. Incluye API,ABI,sensores,permisoGPS e historial sin VIN ni coordenadas.

La firma es una clave local de prueba generada en CI, no es la del fabricante ni la de UNIT. No hay garantía de que futuras compilaciones puedan actualizar esta instalación con la misma firma. No necesita root, cambios de firmware ni permisos privilegiados. No aumenta potencia ni cambia frenada o seguridad.

Las pruebas automatizadas, si pasan, solo acreditan cálculos e instalación/navegación del APK en emulador Android11/API30. No acreditan instalación real en LeapOS, recepción GPS, streaming, vibración real ni conexión al C10.
