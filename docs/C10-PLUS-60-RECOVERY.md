# C10 Plus 6.0 — recuperación y mantenimiento

Estado: rama de recuperación basada en feature/unit-45-cockpit-merge; no equivale al código de la APK 5.8 ni a una versión 6.0 compilada.

## Requisitos
- Mantener 5.8 como referencia sin modificar.
- Navegación, entretenimiento e instrumentación separados; interfaz persistente durante cambios de conexión.
- Diagnóstico de USB, Wi-Fi, Bluetooth y sesiones de proyección; no confundir Bluetooth multimedia con CarPlay/Android Auto.
- Radio y TDT integradas con vídeo únicamente estacionado.
- Telemetría con origen, antigüedad y disponibilidad; no presentar estimaciones como medidas de regeneración.
- No modificar ADAS, firmware ni controles de seguridad.

## Publicación reproducible
1. Guardar todos los fuentes y configuración Gradle en esta rama; preservar licencias y atribuciones.
2. Compilar en CI con Android SDK y dependencias fijadas; ejecutar pruebas y conservar APK y logs por versión.
3. Mantener certificado y clave privada fuera de Git; guardar copia cifrada bajo control del propietario. Verificar huella del certificado contra APK 5.8 antes de usarlo. Una app nueva independiente requiere identificador y firma propios.
4. Antes de publicar actualización, verificar packageName, versionCode creciente, certificado y SHA-256. Confirmar instalación y pruebas de regresión en Android y C10 estacionado.
5. Actualizador HTTPS con verificación de hash, firma y paquete; confirmación explícita de instalación.
6. Etiquetar commit y guardar APK, changelog y manifiesto de actualización. No publicar compilaciones fallidas ni afirmar compatibilidad sin prueba.

## Pendientes
Recuperar código exacto de 5.8, auditar videos, implementar correcciones, verificar CI, firma e instalación física. No hay APK 6.0 en esta entrega.
