# C10 Control Pro 0.2 — prueba propia independiente

Proyecto iniciado a partir del ZIP C10_Control_Pro_Source_0.1.zip entregado en la otra conversación, corregido y ampliado. No es QONECTA UNIT ni reemplaza su proyección Android Auto/CarPlay. Paquete es.c10.controlpro; Android9/API28 mínimo, target35. Sin JNI, permisos del fabricante, servicios de fondo, cuentas, Internet en proceso propio ni control del coche. Se firma con certificado propio de prueba local, NO con firma de compañeros ni de Leapmotor.

## Qué incluye
- Selector de modelo guardado localmente, sin identificar ni conectar al coche.
- Diagnóstico Android/API/ABI y permiso GPS; exportación de TXT con selector normal. No exporta VIN ni coordenadas.
- Velocidad GPS y cronómetros 3–100 y 80–120 orientativos. Se corrige el falso 0–100 del prototipo: empieza a3km/h. Cruce interpolado, descarta falta de velocidad/precisión mala, muestras antiguas y huecos mayores de2,5s. GPS se detiene al salir o pasar la app a segundo plano. Solo circuito autorizado.
- Costes eléctrico y gasolina separados por100km y regeneración estimada manual. No son lecturas del coche.
- Enlaces externos a Netflix, HBO/Max, PlutoTV, Google, AmazonPrimeVideo y Spotify con confirmación manual de estacionamiento. No contiene reproductor propio, DRM, suscripciones ni garantiza reproducción en el navegador del coche. La app no detecta la marcha P.

## Primera prueba
Descarga C10-Control-Pro-0.2.apk completo, cópialo al USB sin renombrar y abre con el instalador normal del coche, estando aparcado. Nombre C10 Control Pro0.2. No desinstales ni reemplaces UNIT o AutoKit. Si no se instala, conserva el mensaje: esta variante no tiene causa del rechazo anterior confirmada. Puede probarse en Samsung; instalar allí no acredita aceptación de LeapOS. Al primer inicio solo solicita ubicación si se inicia el GPS.

## Segunda prueba solicitada: C10 Control Q
El usuario confirma que QONECTA UNIT0.1.6 se instala y funciona en su C10. Ese dato se conserva. Sin embargo, el APK no incluye el proyecto fuente completo; la referencia contenida en el APK es https://github.com/CharlysEV/qonecta-unit y hoy devuelve404, igual que qonecta-unit-companion. El código público CharlysEV/qonecta corresponde a Qonecta+ móvil y NO acredita ser el código exacto UNIT0.1.6. No se presenta una app nuestra ni el APK intacto como UNIT mejorada.

Para producir fielmente esa segunda versión falta el proyecto fuente correspondiente a UNIT0.1.6 (ZIP o repositorio accesible, con licencia y dependencias). No se requiere su clave privada: una modificación lleva firma propia; la firma existente no se conserva válidamente al cambiar el APK. No cambiar apps OEM ni copiar claves. Existe aviso del autor del13/11/2026 sobre reinicio de fábrica no atribuido concluyentemente y retirada de descargas: https://www.lpmff.de/thread/3565-qonecta-unit-natives-aa-und-ac/?postID=67302 . No se afirma que ese aviso diagnostique el APK0.1.6 del usuario.

## Alcance
Sin cámaras/centinela, sensores delanteros, CAN, generador REEV, aparcamiento autónomo, reprogramación ni OBD en esta app pequeña. El OBD opcional de ControlPlus1.1 es otro proyecto. Nada de ello se simula aquí. Pruebas de emulador no equivalen al coche. El proyecto original de compañeros no se modifica.
