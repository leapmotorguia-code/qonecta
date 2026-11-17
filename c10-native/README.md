# C10 Nativa GPS — prueba independiente

APK experimental para comprobar una pantalla Android de un Leapmotor C10. Puede calcular costes con datos introducidos manualmente y ofrece dos estimaciones de prestaciones con GPS disponible y autorizado: salida desde parado y recuperación desde 80 km/h. El objetivo principal (0–100 o 80–120), fecha y modo introducido manualmente se guardan localmente. No guarda coordenadas, rutas ni usa internet.

El GPS se solicita solo cuando el usuario pulsa «Preparar medición», se usa en primer plano y se detiene al salir de esa pantalla. El resultado depende de la señal, la frecuencia de muestras y de si el vehículo expone un proveedor GPS. La salida desde parado se detecta al superar 3 km/h, por tanto el 0–100 no es una medición oficial. Las marcas se interpolan y son orientativas. Si el GPS es antiguo, deficiente o se interrumpe, la prueba se reinicia. Usar las mediciones únicamente en circuitos autorizados, nunca manipular la pantalla conduciendo.

No implementa Android Auto, CarPlay, datos reales del vehículo, sensores, centinela, cámaras, navegación ni OBD. Estas funciones no deben presentarse como disponibles. No está probado en ningún C10. La instalación y la eliminación dependen del sistema y versión del coche. Mantiene un identificador propio separado de Qonecta+ y sus derivados, y no solicita permisos de vehículo ni intenta sustituir ninguna aplicación existente.

La versión se firma con una clave de prueba; no es una clave del fabricante ni una publicación oficial. Las compilaciones generadas con distintas claves pueden impedir actualizaciones sobre la anterior. Compilar con `./gradlew -p c10-native :app:assembleRelease`. Las pruebas virtuales no demuestran la instalación, estabilidad ni funcionamiento en el coche.
