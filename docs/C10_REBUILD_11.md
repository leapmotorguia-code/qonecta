# C10 ControlPlus 1.1 — variante experimental de instalación

No se ha identificado todavía el código del rechazo de LeapOS. Esta variante NO es una corrección demostrada.

Se compara con el APK aportado QONECTA UNIT 0.1.6 (SHA256 65d5a7cf2a25280dc91aa36093f75653aa6463e3f7c8d6fea16bdc67a8ba343a). El usuario no ha confirmado que UNIT se instale en su coche.

Cambios frente a 1.0:
- minSdk 28 y targetSdk 36 como UNIT. Ambos admiten Android 11/API30; elevar target NO demuestra resolver el rechazo.
- Bibliotecas JNI sin comprimir y extractNativeLibs=false como UNIT, con verificación ZIP y alineación.
- Versión 111/1.1 para distinguir los intentos.
- Conserva ARM64, ARM32 y x86_64 para probar el mismo APK exacto en el emulador; no añade x86 de 32 bits, sin evidencia de que el coche lo necesite.

Se mantiene com.leapmotorguia.c10rebuilt: no adopta com.tencent.qqmusiccar, no reemplaza AutoKit ni apps del fabricante. No añade permisos privilegiados sin integración que los utilice. No copia claves ni certificados de terceros. Firma local de prueba, sin autorización OEM.

El instalador diagnóstico 1.0 se adjunta sin cambios. La prueba de emulador comprueba instalación y apertura de paneles, no la aceptación de LeapOS ni el acceso al coche.

# C10 ControlPlus 1.0 — reconstrucción de prueba

Rama propia `c10-nativa-rebuild-10`, basada en `082bbeaf5511fe2f68100efc6c5afdc0117778b9` de nuestra copia de Qonecta+ 0.3.19. No modifica el repositorio de CharlysEV ni las ramas anteriores. Se reconstruyen el inicio nativo, diagnóstico y distribución; los motores de las pantallas conservan su código y créditos originales. No es una reescritura completa de Qonecta+ ni promete todas sus funciones de teléfono dentro del coche.

## Cambios comprobables respecto de 0.9
- Paquete separado `com.leapmotorguia.c10rebuilt`; puede coexistir con 0.9, sin conflictos con AutoKit.
- Android mínimo 8/API26 y objetivo 15/API35. Un target superior no impide instalar en Android11: lo determina el mínimo. El manifiesto no exige GPS, cámara, Bluetooth ni pantalla táctil.
- Compilación release optimizada con R8 y recursos recortados. Bibliotecas comprimidas y extracción convencional para cambiar la ruta de carga nativa respecto de 0.9. Se entrega un solo APK completo; no split APK ni APK renombrado a música.
- Inicio sin registrar servicios/receptores de proyección del teléfono ni trabajos de nube en segundo plano. No altera AutoKit, firmware, root o ajustes del fabricante.
- Galería corregida: READ_EXTERNAL_STORAGE en Android11 y permisos multimedia separados desde Android13. Selector normal de carpeta/USB para vídeos.
- Paneles de Qonecta+: coche (ruta, conducción, viajes, estado), instrumentos, eficiencia, radio, navegador, fotos, vídeos e IPTV. Sus fuentes externas, credenciales y conectividad conservan sus requisitos; abrir un panel no prueba todas sus acciones.
- Ajustes nativos y acceso explícito a los ajustes originales de cuenta Leapmotor/HomeAssistant, sin activar una cuenta ni órdenes automáticamente.
- Diagnóstico exportable: API, ABI, espacio, capacidades Android y últimos errores propios, sin VIN, serie, MAC ni credenciales. Inventario de cámaras/sensores no equivale a permiso de acceso al vehículo.
- OBD opcional: adaptador Bluetooth clásico ELM327 ya emparejado. Consultas estándar 010C/010D/0105/0104 y ATRV. Sin escritura de ECU ni borrado de averías. RPM, velocidad, refrigerante y carga solo si responde la ECU; ATRV es tensión del adaptador/12V. Sin batería de tracción, potencia real o regeneración del C10 validada. Se desconecta al salir; no soporta BLE/Wi-Fi/USB aún.
- GPS conserva historial y recuperaciones 80–100/120/130/140. La salida desde parado empieza al cruzar 3km/h: se etiqueta 3–100, no 0–100 exacto. GPS pierde precisión y no sustituye instrumentación certificada. Solo circuito autorizado.
- Herramientas manuales: costes separados por100km y energía recuperada estimada. No son telemetría.

## Instalador de diagnóstico independiente
`C10-Instalador-1.0.apk`, paquete `com.leapmotorguia.c10installer`, mínimo API23/target35. Sin internet ni librerías nativas. Si consigue instalarse en el coche, selecciona el APK principal a través del explorador y utiliza la API pública PackageInstaller. Android requiere su permiso de fuente de instalación y la confirmación habitual. Guarda los estados y el detalle devueltos: BLOCKED, INCOMPATIBLE, INVALID, STORAGE, CONFLICT, ABORTED o SUCCESS.

No esquiva la política de LeapOS y no puede diagnosticarse a sí mismo si tampoco se instala. No lee el motivo privado de intentos hechos desde Vivaldi, ni obtiene logcat de otros procesos. El fallo de 0.9 aún no tiene causa demostrada: firma/whitelist del fabricante, ABI, almacenamiento u otra política son hipótesis. La aceptación de AutoKit no prueba que un paquete de terceros vaya a aceptarse.

## Lo que no se ha conseguido
Sin API confirmada de cámaras ADAS/360: no hay centinela, distancia delantera ni grabación del coche. Sin aparcamiento autónomo, salida remota de plazas, control de dirección/freno/acelerador, reprogramación de potencia o regeneración, ni optimización automática del motor. Esos elementos aparecen como pendientes, nunca como botones que simulan éxito.

## Construir
JDK21, Android SDK36, NDK29.0.14206865 y CMake3.22.1:
```
./gradlew :app:assembleGithubRelease :c10installer:assembleRelease -Pc10Native=true -Pc10NativeAbis=arm64-v8a,armeabi-v7a,x86_64
./gradlew :app:testGithubReleaseUnitTest -Pc10Native=true -Pc10NativeAbis=arm64-v8a,armeabi-v7a,x86_64 --tests 'com.headqlink.link.C10RebuildTest'
```
Para las tareas assemble, ejecutar el selector --tests solo en la tarea de pruebas (como hace el workflow). Se usa un certificado de prueba local de la ejecución, no el de Leapmotor ni AutoKit. No se publica la clave. Las siguientes reconstrucciones con otra clave pueden exigir desinstalar la prueba anterior; exportar los datos antes.

## Procedimiento sencillo
1. Descarga `C10-ControlPlus-1.0.apk` y `C10-Instalador-1.0.apk`. Copia ambos al pendrive como APK; no cambies la extensión. No formatees ni borres el USB si ya funciona en el coche.
2. Con el coche aparcado, abre `C10-ControlPlus-1.0.apk` mediante el explorador/instalador que ya utilizaste. Nombre esperado: C10 ControlPlus1.0. Acepta la instalación normal si el sistema la ofrece.
3. Si abre, entra en Diagnóstico y guarda el informe. Después prueba los paneles por separado. No conectes cuentas ni OBD para la primera apertura.
4. Si el principal se rechaza, prueba instalar el APK pequeño `C10-Instalador-1.0.apk` por el mismo procedimiento. Si abre, pulsa Seleccionar APK. Solo si Android ofrece su permiso normal de instalación, habilítalo para esta herramienta, vuelve y selecciona el APK principal. Confirma en el diálogo Android. Pulsa Actualizar resultado después y guarda/fotografía el texto completo.
5. Si también se rechaza el instalador pequeño, no sirve añadir diagnóstico dentro de una app que no puede arrancar. Conserva el mensaje exacto y el APK: se necesitaría un diagnóstico del instalador/sistema obtenido por una vía admitida por el fabricante. No repetir variantes arbitrarias ni modificar el firmware.
6. Se puede probar primero en Samsung, pero instalar allí no acredita compatibilidad con LeapOS. Para desinstalar usa el administrador de aplicaciones si LeapOS lo permite. No se garantiza ausencia de registros ni efecto sobre la garantía.

## Entrega y revisión externa
La publicación de prueba del repositorio propio contiene los dos APK, código correspondiente, SHA256, manifiesto/configuración, firmas, registros de compilación y de emulador y este informe. Para otras IA: compartir el ZIP de código y este documento. No compartir claves, cuentas, PIN o VIN.

Licencia AGPL-3.0 y avisos originales conservados en LICENSE/NOTICE. Consultas oficiales: https://developer.android.com/guide/topics/manifest/uses-sdk-element y https://developer.android.com/reference/android/content/pm/PackageInstaller . Las pruebas automáticas solo acreditan ese entorno, no un C10 real.
