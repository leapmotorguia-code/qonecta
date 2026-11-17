# C10 Nativa — prueba independiente

Prototipo mínimo para comprobar instalación, apertura y pantalla en un C10 compatible. Es una app Android independiente con datos técnicos básicos y una calculadora manual de costes. No usa permisos Android, servicios en segundo plano, Internet, GPS, cámaras ni APIs del vehículo.

No implementa Android Auto, CarPlay, sensores, centinela, navegación, conexión OBD ni datos reales. No se ha probado en ningún C10. Su instalación y desinstalación dependen de la versión del coche y del instalador; la ausencia de llamadas al vehículo no demuestra riesgo cero. Se instala únicamente para evaluación deliberada con el coche aparcado.

Se compila con `./gradlew -p c10-native :app:assembleDebug`. La compilación no confirma compatibilidad con el coche. Este proyecto es independiente del proyecto Qonecta+ y no utiliza su código.
