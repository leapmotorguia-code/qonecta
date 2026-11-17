# C10 Nativa — prueba independiente

Prototipo mínimo para comprobar instalación, apertura y pantalla en un C10 compatible. Es una app Android independiente con datos técnicos básicos y una calculadora manual de costes. No usa permisos Android, servicios en segundo plano, Internet, GPS, cámaras ni APIs del vehículo.

No implementa Android Auto, CarPlay, sensores, centinela, navegación, conexión OBD ni datos reales. No se ha probado en ningún C10. Su instalación y desinstalación dependen de la versión del coche y del instalador; la ausencia de llamadas al vehículo no demuestra riesgo cero. Se instala únicamente para evaluación deliberada con el coche aparcado.

La versión 0.2 desactiva el modo de depuración para investigar un posible motivo de rechazo. Conserva nuestro identificador propio para no entrar en conflicto con las aplicaciones de otros. Sigue usando un certificado de prueba y no se ha demostrado que este cambio resuelva el error en el C10. Cada compilación con un certificado de prueba distinto puede requerir desinstalar la anterior antes de instalar una actualización; no se debe borrar ninguna otra aplicación.

Se compila con `./gradlew -p c10-native :app:assembleRelease`. La compilación no confirma compatibilidad con el coche. Este proyecto es independiente del proyecto Qonecta+ y no utiliza su código.
