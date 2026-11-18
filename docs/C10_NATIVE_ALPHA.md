# C10 Nativa Plus — adaptación alfa

Objetivo: ejecutar directamente en Android 11 del C10 las pantallas propias de Qonecta+, conservando sus autores, LICENSE y NOTICE. Rama independiente; no modifica main ni el repositorio original.

## Estado verificable

Esta alfa incorpora un anfitrión Activity nativo para las implementaciones originales CarHubScreen (ruta, conducción, viajes, instrumentos, eficiencia y estado), InstrumentsScreen, EfficiencyScreen, RadioScreen, WebScreen, GalleryScreen e IptvScreen. Añade nuestro módulo de medición GPS, costes y estimación manual de energía.

No utiliza VirtualDisplay, SSPLink ni proyección del teléfono para dibujar esos paneles. No activa DemoMode. La apertura de paneles y su integración con cada servicio deben probarse; conservar una clase no garantiza que todas sus acciones dependientes de LinkService funcionen en este anfitrión.

No equivale aún a todas las funciones de Qonecta+. No incluye receptor Android Auto o CarPlay nativo, acceso a cámaras, centinela, sensores de distancia, OBD, cambios del motor ni aparcamiento remoto. No hay credenciales de Leapmotor configuradas: los paneles no disponen de datos reales del coche. Las estimaciones siguen siendo estimaciones.

El acceso al instalador y el rechazo de APK de LeapOS siguen pendientes de diagnóstico. Compilar o pasar emuladores no valida instalación, garantía ni funcionamiento en el C10.

## Variante independiente

Compilar con:

```sh
./gradlew :app:assembleGithubDebug -Pc10Native=true
```

Identificador: com.leapmotorguia.c10nativeplus. La variante habitual del teléfono se conserva al compilar sin esa propiedad. La variante nativa usa un manifiesto reducido sin arranque automático, accesibilidad, instalación de APK, root ni receptores de control del teléfono. Las preferencias están aisladas.

El inicio nativo evita registrar root, reparar accesibilidad o programar consultas de nube. Los paneles solo se abren a petición del usuario. Al salir, se destruye el panel y se detiene la radio. La ubicación se solicita al entrar en paneles que la necesitan. Multimedia y web requieren confirmar uso estacionado; no hay señal P o velocidad del vehículo validada, y no se afirma que ese aviso sea un bloqueo automático de conducción.

Las herramientas propias solo guardan resultados locales; no contienen telemetría del vehículo.

## Antes de distribuir como versión terminada

- Resolver instalación en el firmware real y confirmar que se puede desinstalar.
- Verificar cada panel, sus acciones, selección de archivos, ajustes y conectividad.
- Integrar un flujo de ajustes nativo y proveedores de datos comprobados.
- Separar estimaciones, datos GPS, nube y OBD; mostrar antigüedad y ausencia de datos.
- Verificar suspensión, salida y consumo eléctrico sin mantener despierto el coche.
- Mantener clave de firma estable y privada antes de actualizaciones públicas.
- Publicar fuentes correspondientes, créditos y licencias de cualquier distribución derivada.
