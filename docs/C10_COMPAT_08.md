# C10 Nativa Plus 0.8 — prueba de compatibilidad

Variante experimental de la alfa 0.7. No identifica todavía la causa del rechazo de LeapOS 3.50.62 y no garantiza instalación en el C10.

Cambios:
- Compilación githubRelease, con debuggable=false y sin instrumentación.
- minSdk 29 y targetSdk 29 para comprobar la vía de compatibilidad anterior en Android 11; el APK oficial de AutoKit 2025.03.19.1126 que se analizó también usa target 29. Esto es una hipótesis de compatibilidad, no prueba de qué permite el firmware.
- Solo bibliotecas ARM64 para el archivo del coche.
- Identificador propio com.leapmotorguia.c10nativeplus, versionCode 108. No utiliza la identidad ni el certificado de AutoKit, ni sustituye aplicaciones del fabricante.
- Firma de pruebas propia del entorno de compilación, no certificado de producción o del fabricante.
- Mismo alcance funcional y limitaciones documentadas en C10_NATIVE_ALPHA.md. La variante 0.8 no añade telemetría, cámaras, centinela ni automatismos del vehículo.

Comprobaciones previstas: firma, configuración, ausencia de modo depuración, bibliotecas ARM64; apertura y navegación en emulador API 30 con la compilación universal de distribución. Después se eliminan únicamente las bibliotecas x86_64, se vuelve a alinear y firmar el APK con la misma clave de pruebas del trabajo y se verifica que código, recursos y manifiesto sean idénticos a los probados. Las bibliotecas ARM64 y las políticas de instalación del coche requieren prueba real.

Instalación de prueba: copiar C10-Nativa-Plus-0.8-Compat-Coche.apk al USB, abrirlo desde el explorador del coche con el vehículo estacionado, confirmar instalación. Cancelar si ofrece sustituir una aplicación distinta. No requiere instalar LeapLink ni cambiar AutoKit, firmware o adaptador.

Esta variante de target antiguo es para la prueba en el coche; un Samsung reciente puede advertir de antigüedad. Mantener Play Protect activado. No tratarla como actualización de la 0.6 del Samsung, que tiene otro identificador.

Si vuelve a fallar, conservar el mensaje y la hora para un registro de PackageManager. El fallo no demuestra por sí solo bloqueo por firma o lista de paquetes.
