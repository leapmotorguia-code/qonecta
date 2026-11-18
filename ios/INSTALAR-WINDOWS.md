# Prueba C10 Widget con Windows e iPhone

Esta es una vía de prueba pendiente de validar en tu iPhone, no una instalación directa ni una app publicada en App Store. La firma Apple la hará AltServer con tu cuenta. El archivo IPA preparado aquí incluye una firma local provisional que no permite instalarlo por sí solo.

## Primera instalación, en casa
1. En Windows, abre https://altstore.io y descarga AltServer para Windows, apartado **AltStore Classic**. No AltStore PAL; es otra distribución.
2. Sigue la guía oficial https://faq.altstore.io/altstore-classic/how-to-install-altstore-windows . Pide iTunes e iCloud de Apple (tiene indicaciones específicas si ya usas la versión de Microsoft Store). No hace falta contratar almacenamiento iCloud.
3. Conecta el iPhone al ordenador con un cable de datos, desbloquéalo y acepta Confiar si lo solicita. Instala AltStore mediante AltServer, seleccionando tu teléfono. La guía oficial explica los permisos de confianza y el modo de desarrollador cuando proceda.
4. Introduce tu cuenta Apple únicamente en las herramientas oficiales durante la firma; NO envíes contraseñas, códigos de verificación ni certificados por ChatGPT.
5. Guarda **C10-Widget-0.2-Para-Firmar.ipa** en Archivos del iPhone. Con AltServer abierto en Windows y ambos equipos conectados según la guía, abre AltStore Classic en iPhone → My Apps → + → selecciona ese IPA.
6. Si pregunta por extensiones, elige **Keep App Extensions / Conservar extensiones**. La extensión es la que permite ver los widgets. Si da un error de firma, App Group, cuenta o compatibilidad, conserva el texto completo del error; no repitas la instalación una y otra vez.
7. Abre **C10 Widget**. Introduce batería y autonomía, y pulsa Guardar. Si aparece un error del grupo compartido, envía una foto de ese aviso: no podremos probar el widget hasta que la firma autorice el mismo grupo para la app y su extensión.

## Prueba en el coche
1. Con coche aparcado, conecta el iPhone a tu AutoKit/Carlink como haces normalmente. No se instala nada nuevo en LeapOS.
2. En iPhone, busca la gestión de widgets de esa conexión en Ajustes → General → CarPlay → tu coche/conexión → Widgets.
3. Añade Mi C10 · Manual, Costes por 100 km o Energía recuperada.
4. En CarPlay, desliza a la zona de widgets. Compara la hora y los valores con el registro del iPhone. iOS decide cuándo refresca.

## Límites importantes de la prueba
- Con una cuenta gratuita, las apps firmadas de esta forma caducan normalmente a los 7 días y hay un límite de aplicaciones; AltStore/AltServer permiten renovarlas según su documentación. La extensión también puede consumir un identificador de app.
- No hemos confirmado la instalación en tu iOS concreto, ni la firma de ambos componentes o su aparición en CarPlay real. Un simulador no valida estos pasos.
- Los datos son manuales. No sustituye el widget oficial Leapmotor ni lee su cuenta, cámaras, OBD o controles.
- No modificar firmware ni AutoKit. El ordenador se necesita para instalar/renovar; una vez instalada, la prueba de widgets no necesita llevarlo al coche.

Fuentes del procedimiento y límites:
https://altstore.io/
https://faq.altstore.io/altstore-classic/how-to-install-altstore-windows
https://github.com/altstoreio/FAQ/blob/main/altstore-world/your-altstore.md
