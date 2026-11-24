# Investigación internacional y decisiones v3
- https://github.com/CharlysEV/qdauto — transporte QDLink, requiere integración y validación; no reutilizado.
- https://github.com/ryazrm/headqlink_devel — proyección desde teléfono; no host propio en esta APK.
- https://leapbrasil.club/leaplink/faq/ — proyección nativa comunitaria, dependencia de firmware.
- https://github.com/Wh1t3co1azZ/DiPlay-Leapmotor — mensaje leapmotor.cantoapp.update documentado. Solo se observa pasivamente; sin heartbeat ni escritura, valores brutos no autenticados.
- https://github.com/kerniger/leapmotor-ha y https://github.com/markoceri/leapconnect — telemetría remota mediante integración independiente.
- https://developers.home-assistant.io/docs/api/rest/ — API GET states: conector HTTPS opcional, token en memoria, no comandos.
- https://github.com/ProtossBlaster/leapmotor-mate — seguimiento de viajes y REEV; no separar regeneración de generador sin fuente validada.
- https://github.com/pro-logic/leapmotor-c10-autokit-fix — compatibilidad con adaptador depende de firmware.
Paquete separado lab3 y firma debug temporal, puede coexistir con v2 y UNIT. No contiene motor AA/CP, acceso directo BMS ni controles remotos. El receptor pasivo no autentica emisor ni valida unidades. Multimedia y permisos pendientes de prueba física. No incluir secretos en informes ni repositorio.
