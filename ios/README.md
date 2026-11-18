# C10 Widget 0.2 para iPhone y CarPlay

Proyecto iOS independiente. No instala nada en LeapOS ni modifica AutoKit o el firmware. No usa código Android de Qonecta; está separado en la rama c10-carplay-widget-ios del repositorio propio del usuario.

## Contenido
- Widget pequeño de batería y autonomía eléctrica, introducidas manualmente.
- Widget de costes eléctricos y gasolina por 100 km por separado, con consumos y precios manuales.
- Widget de energía recuperada estimada, potencia media manual multiplicada por duración.
- Cada tarjeta muestra MANUAL y hora del registro. Aviso de registro antiguo tras una hora.
- Sin datos iniciales ficticios. No se presentan costes o recuperación como cero si no se han introducido.
- App iPhone para editar los valores y solicitar refresco. iOS decide cuándo aplica los cambios.

## Límites
No conexión a la cuenta Leapmotor, OBD, cámaras o telemetría. No centinela, control del coche, velocidad o regeneración en directo. No app completa en el lanzador de CarPlay. El widget systemSmall es elegible para la galería de CarPlay en iOS 26 o superior; la prueba con el iPhone, AutoKit y C10 reales sigue pendiente.

## Para instalar en un iPhone real
El archivo SIN-FIRMA.ipa de GitHub NO se puede instalar directamente. Un ZIP de código tampoco es una aplicación instalable. Falta una firma de Apple y perfiles de aprovisionamiento que autoricen tanto la app como su extensión y el App Group. No hay certificados, cuentas Apple ni TestFlight configurados o incluidos. No subir contraseñas, claves o certificados al repositorio ni enviarlos por el chat.

Ruta de distribución sencilla para el usuario final: TestFlight, una vez que una cuenta Apple Developer configure los identificadores, App Group, firma y distribución. No se ha publicado ni aprobado en TestFlight.

En un Mac con Xcode y XcodeGen: ejecutar `xcodegen generate` dentro de ios, abrir C10Widget.xcodeproj, seleccionar un equipo de desarrollo, configurar identificadores propios y un App Group válido compartido por app y extensión (C10_APP_GROUP), firmar y distribuir por el método autorizado. Una cuenta gratuita puede tener limitaciones de capacidades; no se da por verificado el App Group con ella.

## Una vez instalada y firmada
1. Abrir C10 Widget en el iPhone con el coche aparcado.
2. Introducir batería y autonomía; costes y regeneración son opcionales. Guardar.
3. Conectar iPhone a CarPlay por AutoKit/Carlink como siempre.
4. En Ajustes → General → CarPlay → conexión del coche → Widgets, añadir la tarjeta de C10 Widget.
5. Abrir la zona de widgets de CarPlay. La ubicación exacta de los ajustes depende de la versión de iOS.

## Verificación automatizada
GitHub macOS genera el proyecto, compila app y extensión, ejecuta pruebas de unidades, persistencia y estado antiguo y una prueba de edición del formulario. La prueba UI usa almacenamiento aislado DEBUG: no acredita App Groups en teléfono real. La compilación de dispositivo se realiza sin firma y se comprueba que contiene la extensión WidgetKit. No se simula una conexión AutoKit ni la pantalla del C10.

Fuentes oficiales:
- https://developer.apple.com/documentation/widgetkit/adding-standby-and-carplay-support-to-your-widget
- https://developer.apple.com/videos/play/wwdc2025/216/
- https://developer.apple.com/documentation/xcode/configuring-app-groups
