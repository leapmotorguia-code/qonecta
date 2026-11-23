# UNIT C10 Plus — revisión experimental 4.5.1 (23/11/2026)

## Segundo vídeo del 23/11/2026 — hallazgo del canal publicado

- La pestaña Actualizaciones muestra «APK descargada y VERIFICADA: 4.4-base08-actualizador» y el botón de instalación, pero NO demuestra que esa APK se haya instalado.
- El manifiesto `updates/unit-c10-plus.json` en `main` sigue publicado con `versionCode: 450`, `versionName: 4.4-base08-actualizador`. El APK 4.5 experimental analizado contiene `versionCode: 460`; por tanto, el 4.4 publicado **no** puede ser una actualización nueva de esa 4.5.
- El vídeo no muestra una pantalla de información de paquete suficiente para demostrar qué `versionCode` está instalado realmente en el C10. Puede ser una descarga conservada o una versión anterior.
- Se añade un control antes de intentar la instalación: si el archivo descargado tiene versión menor o igual que la versión instalada, se deshabilita el botón y se elimina la referencia a esa descarga en preferencias. **No** se borra ni se instala a la fuerza ninguna APK del coche.
- Comprobación CI GitHub Actions: ejecución `45237703604`, compilación y pruebas automáticas correctas.
- La 4.5.1 continúa sin APK firmada instalable; no se ha actualizado el canal `stable` ni se aconseja instalar la 4.4 sobre 4.5.

## Evidencia del vídeo del vehículo
- La descarga del APK se marca como verificada.
- Al pedir instalar, se muestra `No se puede abrir el permiso del instalador: ActivityNotFoundException`.
- Esto demuestra que el intent de ajustes no tiene una actividad receptora disponible en este LeapOS.
- El vídeo no demuestra la causa de la desaparición de pestañas al conectar el móvil, ni prueba reproducción de Netflix o CarPlay.

## Correcciones de código
- `InAppUpdater.java`: cuando falla `ACTION_MANAGE_UNKNOWN_APP_SOURCES`, intenta `ACTION_INSTALL_PACKAGE` y después `ACTION_VIEW`, siempre a través del instalador oficial. Conserva comprobación previa SHA-256/paquete/firma y mensajes de bloqueo. Registra etapa y error.
- `C10NativeHubActivity.java`: botón «Recuperar nuestras pantallas / Abrir UNIT»; destruye el puente UI anterior y reconstruye el diálogo. Si el diálogo desapareció durante una pausa, intenta restaurarlo solo cuando el usuario vuelve a UNIT (onResume). Selector manual APK con ambas variantes del instalador oficial.
- `StartupDiagnostics.java`: añade fecha del último restablecimiento de UNIT y resultado del intento de instalación.
- `.github/workflows/c10plus-native-classes.yml`: compila Java/DEX y ejecuta pruebas automatizadas en la rama experimental.

## Validación
- GitHub Actions run 45235157434, estado: **success**.
- Se comprobó que los archivos APK 4.4 y 4.5 previamente recuperados pasan integridad ZIP y mantienen idénticas las cuatro clases DEX originales. No se ha montado una APK 4.5.1 nueva.
- Aún no hay ensayo real en el C10 ni validación con iPhone/CarPlay, DRM o telemetría ECU.

## Para entregar APK actualizable
- Compilar el módulo actualizado, incorporarlo a la base original conservada, aumentar `versionCode`, alinear y firmar con la **misma clave privada** que las APK C10 Plus existentes.
- Verificar firma v2 y huella de firmante, paquete, SHA-256, APK completa, cuatro DEX originales y compatibilidad de instalación.
- La clave privada de las versiones instaladas no se ha recuperado en esta sesión: no se debe generar una APK con firma distinta ni cambiar el paquete para simular una actualización.
- Publicar solo después de probar instalación manual desde Vivaldi/gestor en el C10 estacionado. Si LeapOS carece de instalador/permiso, ninguna pantalla alternativa puede saltarse sus restricciones.

## Siguientes pruebas en el C10, estacionado
1. Abrir el diagnóstico de inicio y copiarlo.
2. Conectar iPhone con CarPlay; registrar si UNIT sale de primer plano, si el diálogo sigue presente y si al volver aparecen las pestañas añadidas.
3. Repetir con el teléfono desconectado y comparar fechas de pause/stop/resume.
4. Capturar el informe «Accesos C10»: WebView, DRM, USB, paquetes multimedia, permisos Android y origen real de telemetría.
5. Registrar por separado si Maps/Spotify/Netflix/Prime/Max están instalados o son simples enlaces web. No confundir presencia de clases o sensores Android con señales del vehículo.

**Importante:** esta revisión no promete streaming DRM, receptor CarPlay, acceso a cámaras/ADAS/TPMS ni instalación silenciosa. El vídeo del coche debe utilizarse únicamente estacionado.
