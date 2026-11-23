# UNIT Control · Sensores y balance energético — código fuente individual

**Fecha:** 23 noviembre 2026

**Objetivo:** modificar el código fuente del proyecto Qonecta/UNIT sin tocar el protocolo de Android Auto ni los módulos de proyección que ya usa la APK aportada.

## Archivo APK recibido y análisis de firma
- `C10-UNIT-Control-Solo-Firma(1).apk`, SHA-256: `6d8c7e2d54bc8abcdc578595310f919a7dd24ec0c083e5990bd6f84353a1c5d9`
- Integridad ZIP: correcta.
- Firma Android v2: válida.
- Certificado SHA-256: `45d3db8e822d7fba45034a7056d80973be39f99e44ff5bd384b2e5373a6d937c`.
- La clave de firma disponible de la familia C10 Plus tiene certificado `6861d639e6f428e2d29e24501e8bb05e58de606f927ef24f5ebe8a5952da56f6`: **es otra clave** y no sirve para actualizar la APK que aportó el usuario.
- Por tanto **no existe una APK de actualización firmada con la misma identidad del archivo recibido**. Ni la integridad ZIP, ni la compilación del código, ni un certificado diferente validan una actualización sobre esa APK.

## Cambios introducidos en la rama
- `app/src/main/java/com/headqlink/link/Config.java`: eliminar Karaoke y Juegos de los botones configurables y sustituir su lugar por Sensores C10, migrando listas ya guardadas y sin duplicados.
- `app/src/main/java/com/headqlink/link/CarUi.java`: nueva pantalla `sensors`, icono y etiqueta. Reconducir la antigua pantalla guardada Karaoke/Juegos hacia Sensores. Sin editar el código de decodificación, USB ni Android Auto.
- `app/src/main/java/com/headqlink/link/SensorsAccessScreen.java`: inventario detallado de Android, USB, SensorManager, Camera2, Widevine y apps; lecturas de `CarCloud.car()` /`LeapStatus` disponibles por nube o Mate, con sello temporal, estado de origen y rechazo de datos simulados. Exporta JSON por portapapeles. NO accede al CAN interno ni a cámaras 360 del fabricante.
- `app/src/main/java/com/headqlink/link/EnergyFlowSummary.java`: cálculo físico separado bruto consumido, recuperado, neto, porcentaje de regeneración y medias por 100 km, con valores ausentes sin inventar.
- `app/src/main/java/com/headqlink/link/EfficiencyScreen.java`: tarjeta de balance energético añadida al panel, con ratio recuperado/consumido y neto por 100 km. Explica que la separación de bruto y regeneración es estimación incluso cuando el neto se calibra con datos del coche.
- `app/src/test/java/com/headqlink/link/EnergyFlowSummaryTest.java`: pruebas de balanza, descenso con saldo negativo, datos ausentes y origen de los valores.
- `app/src/test/java/com/headqlink/link/C10SensorsPanelMigrationTest.java`: pruebas de migración de las preferencias guardadas.
- `.github/workflows/android-ci.yml`: CI completo para compilar y ejecutar pruebas en esta rama.

## Acceso al código completo, sin ZIP
Esta rama contiene TODO el repositorio de código fuente (aplicación, protocolo, recursos, módulos, tests y scripts) como archivos normales. Se puede examinar o compartir **archivo por archivo**, sin comprimir:

https://github.com/leapmotorguia-code/qonecta/tree/feature/c10-unit-control-sensores-energia-v1

La base abierta del repositorio permite revisar el código del proyecto, pero no debe confundirse con una decompilación perfecta del APK subido ni con una certificación de funcionamiento en LeapOS.

## Pruebas necesarias
1. Confirmar compilación y pruebas unitarias en GitHub Actions.
2. Para instalar encima del APK recibido, recuperar la **clave privada original correspondiente al certificado 45d3…**, o construir una variante de ensayo con otro paquete e identidad (en paralelo), cuya compatibilidad con AA habría que validar sin alterar la que funciona.
3. Probar físicamente la conectividad Android Auto, y después CarPlay, sin suponer que lo segundo funciona solo porque el primero lo hace.
4. Comprobar que la auditoría muestra datos **solo** cuando hay fuente y marca su antigüedad.
5. Comprobar que las cifras de regeneración estimada no se presentan como un contador físico de kWh recuperados del BMS.
6. No realizar diagnósticos ni pruebas multimedia mientras se conduce.

**No se ha modificado ni desinstalado ninguna aplicación del vehículo.**
