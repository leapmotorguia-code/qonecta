# C10 Prueba C 0.5 — experimento de instalación

Esta variante conserva el código propio de la prueba 0.4. No contiene código de Qonecta Unit, no inicia servicios, no consulta servicios de vehículo ni modifica firmware, motor, frenos o dirección. El GPS se solicita únicamente durante la medición en primer plano. RPM y regeneración real siguen sin fuente de datos; el cálculo de energía es manual y aproximado.

## Cambio que se prueba
`applicationId=com.tencent.qqmusiccar`, observado en el APK Qonecta Unit 0.1.6 aportado por el usuario. No es QQ Music ni una aplicación oficial de Tencent. Etiqueta visible: C10 Prueba C. Usa una firma de desarrollo propia; no es la firma de Tencent, Leapmotor ni Qonecta Unit. Conserva minSdk 23 y targetSdk 28, ya compatibles con un Android 11 estándar, y usa versionCode 1.

El cambio se basa en la restricción por identificadores descrita por un contribuidor de DiPlay en https://github.com/shihabal3amri/DiPlay/issues/322. NO demuestra que el identificador esté permitido en LeapOS 3.50.62 ni que baste para instalar. Si el firmware también comprueba la firma, seguirá rechazando el paquete. Un mensaje de formato no admitido desde el reproductor multimedia puede producirse antes de llegar al instalador.

## Conflictos y prueba
No puede coexistir con QQ Music o Qonecta Unit que usen ese identificador. Si Android ofrece actualizar una aplicación existente, cancelar. No desinstalar aplicaciones originales del coche para resolverlo. La firma independiente impide actualizar un paquete existente que tenga otra firma; la prueba automatizada en Android 11 comprueba este rechazo.

Instalar únicamente desde un gestor de archivos que invoque el instalador Android, con el coche aparcado. Si el instalador no llega a aparecer, cambiar el código de la aplicación no soluciona ese paso.

Las pruebas de compilación, firma, instalación y apertura virtuales no verifican la aceptación del coche ni las mediciones GPS reales. Los resultados GPS son aproximaciones para circuito autorizado. No instala cámaras, centinela, Android Auto, CarPlay ni OBD.

## Antecedentes consultados
- Lista de aplicaciones permitidas y límites de la adaptación: https://github.com/shihabal3amri/DiPlay/issues/322
- Portal público de explorador USB: https://leapbrasil.club/leaplink/
- El autor retiró Qonecta Unit tras un reinicio de causa desconocida: https://www.lpmff.de/thread/3565-qonecta-unit-natives-aa-und-ac/?postID=67302 . No se recomienda instalar ese paquete archivado.

Esta rama no se fusiona en el código de los compañeros.
