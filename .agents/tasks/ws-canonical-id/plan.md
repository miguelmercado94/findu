# Implementation Plan — Fase 0: Identificador canónico único de destinatario WebSocket

> Fuente: `design.md` (aprobado) + `design-review.json` (3 hallazgos abiertos no bloqueantes: #10, #8, #9).
> Alcance: unificación de identidad (`PROVEEDOR_<proveedorId>`) + un solo socket por pantalla + arreglo
> del falso OFFLINE. NO FCM, NO `AppLifecycleState`, sin cambiar payloads. Heartbeat/reconexión intactos.
>
> Repos:
> - Backend: `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu` (rama `develop`, remoto `origin`).
>   Dispatcher: `CORE/LAMBDA/findu-notification-dispatcher` (Gradle propio, Java 21, Spring WebFlux).
>   Core: `CORE/findu-core` (Gradle propio, Spring MVC).
> - App: `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu-movil\apps\findu_proveedores` (rama `develop`, sin remoto).
>
> Comandos de build/test verificados durante exploración:
> - Dispatcher: `.\gradlew.bat test` en `CORE/LAMBDA/findu-notification-dispatcher` (JUnit5 + reactor-test ya presentes).
> - Core: `.\gradlew.bat test` en `CORE/findu-core`.
> - App: `flutter analyze` + `flutter test` en `apps/findu_proveedores`.
>
> Orden por dependencia: el dispatcher (RecipientResolver + enrutamiento canónico) es la red de seguridad
> y define el contrato de alias → va primero. Core (emisión canónica + poblado de alias) consume ese contrato
> → va segundo. La app (un solo socket) es independiente del backend → va al final. Cada ítem deja el repo
> compilable.

---

- [ ] 1. Crear `RecipientResolver` en el dispatcher.
      `@Component` con `ConcurrentHashMap<String,String> aliasToCanonical`. Método `String toCanonical(String recipient)`
      con las reglas en orden del diseño (null/blank → tal cual; `trim()`; prefijos `CLIENTE_`/`AGENTE_`/`SOPORTE_` →
      tal cual; alias conocido → canónico; `PROVEEDOR_` → tal cual; numérico puro → prefijar `PROVEEDOR_`; resto → tal cual).
      Método `void learnAlias(String alias, String canonical)` idempotente (no-op si `alias==canonical`, si alguno es
      blank, o si no empiezan ambos por `PROVEEDOR_`). NO incluir `indexKeysFor` (hallazgo #5).
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/infrastructure/routing/RecipientResolver.java`
      Verify: `cd CORE/LAMBDA/findu-notification-dispatcher; .\gradlew.bat compileJava` compila sin errores.

- [ ] 2. Añadir test unitario de `RecipientResolver` (tabla de casos de `toCanonical` + no-op de `learnAlias`).
      Cubre CA5 y CA6: numérico puro, `PROVEEDOR_x`, `CLIENTE_x`, `AGENTE_x`, `SOPORTE_GENERAL`, alias conocido, blank;
      y `learnAlias` con `alias==canonical`, blanks, sin prefijo `PROVEEDOR_`.
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/test/java/com/findu/dispatcher/infrastructure/routing/RecipientResolverTest.java`
      Verify: `.\gradlew.bat test --tests "*RecipientResolverTest"` — todos los casos pasan.

- [ ] 3. Normalizar el handshake en `EventWebSocketHandler.handle` bajo UNA sola clave canónica (hallazgo #5).
      Tras `extractAndValidateUserId`, calcular `canonicalUserId = recipientResolver.toCanonical(userId)` y usar ESA
      clave para `registerSession`, `registerPresence` y el `unregisterSession`/`removePresence` del `doFinally`
      (misma clave en registro y baja; no hay doble indexación). Inyectar `RecipientResolver`.
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/infrastructure/websocket/EventWebSocketHandler.java`
      Verify: `.\gradlew.bat compileJava` compila.

- [ ] 4. Imponer la invariante `canalId == toCanonical(targetUserId)` en TODOS los caminos de entrega (hallazgo #10).
      En `EventDispatcherService`: inyectar `RecipientResolver`; al inicio de `procesarEntrega` operar sobre
      `toCanonical(targetUserId)`; en la sobrecarga `dispatch(EventEnvelope<?>)` derivar
      `canalId = toCanonical(targetUserId)` con fallback a `eventId()` sólo si el canónico es null/blank (hallazgo #2),
      e invocar `learnAliasFromPayload(envelope)` una vez (camino Rabbit). Añadir el método privado
      `learnAliasFromPayload(EventEnvelope<?>)` con `instanceof Map` defensivo que lee `canonicalRecipient`/`aliasRecipient`
      y llama `recipientResolver.learnAlias(...)` (hallazgo #4; este método vive en `EventDispatcherService`, hallazgo #9).
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/service/EventDispatcherService.java`
      Verify: `.\gradlew.bat compileJava` compila.

- [ ] 5. Normalizar el camino HTTP de presencia y el canal de disponibilidad (hallazgo #10, RF5/CA7).
      En `NotificationDispatchController.despacharEventoPresencia`: derivar `canalId` y construir el `EventEnvelope`
      con `recipientResolver.toCanonical(recipient)`, y llamar `learnAliasFromPayload`/`learnAlias` una vez antes de
      `dispatch(canalId, ...)` para aprender el alias de los `params`. En
      `ChannelController.actualizarDisponibilidadProveedor`: envolver `"PROVEEDOR_" + proveedorId` con
      `recipientResolver.toCanonical(...)` (centraliza formato; mismo canal canónico que recibe eventos).
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/presentation/controller/NotificationDispatchController.java`,
      `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/presentation/controller/ChannelController.java`
      Verify: `.\gradlew.bat compileJava` compila.

- [ ] 6. Test de integración del dispatcher: entrega canónica, tolerancia a app vieja (alias en enrutamiento),
      e invariante `canalId==canónico` por el camino Rabbit con canal INACTIVO → FCM (hallazgo #10/#2, CA1/CA2/CA5/CA7).
      Verificar también que `NotificationDispatchControllerTest` existente sigue verde tras los cambios del controller.
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/test/java/com/findu/dispatcher/service/EventDispatcherServiceTest.java`,
      `CORE/LAMBDA/findu-notification-dispatcher/src/test/java/com/findu/dispatcher/presentation/controller/NotificationDispatchControllerTest.java`
      Verify: `.\gradlew.bat test` en el dispatcher — todos los tests pasan.

- [ ] 7. Core: emisión canónica única + poblado de alias en `DispatcherNotificationObserver`.
      En `sendToProvider(PerfilProveedor p, ...)`: emitir UNA sola vez `PROVEEDOR_<p.getId()>`; si
      `authUserId != id`, añadir a `params` las claves aditivas `canonicalRecipient=PROVEEDOR_<id>` y
      `aliasRecipient=PROVEEDOR_<authUserId>`; eliminar el segundo `notificationPort.send`. En `sendToProviderId(...)`:
      resolver `authUserId` vía `proveedorService.findById` sólo para poblar los campos de alias, sin segundo envío.
      No tocar recipients de cliente ni payloads de negocio.
      Files: `CORE/findu-core/src/main/java/com/findu/core/domain/observer/DispatcherNotificationObserver.java`
      Verify: `cd CORE/findu-core; .\gradlew.bat compileJava` compila.

- [ ] 8. Core: test unitario de `DispatcherNotificationObserver` (CA4).
      Verificar que `sendToProvider` llama `notificationPort.send` UNA vez y que, cuando `authUserId != id`, el mapa
      `params` contiene `canonicalRecipient`/`aliasRecipient`.
      Files: `CORE/findu-core/src/test/java/com/findu/core/domain/observer/DispatcherNotificationObserverTest.java`
      Verify: `.\gradlew.bat test --tests "*DispatcherNotificationObserverTest"` pasa; `.\gradlew.bat test` del core verde.

- [ ] 9. App: `environment.dart` — renombrar el parámetro de `webSocketUrlFor` a `proveedorId` y documentar que DEBE
      ser el `proveedorId`, nunca `authUserId`. Sin cambio de firma pública (sigue `int`, prefija `PROVEEDOR_`).
      Files: `apps/findu_proveedores/lib/core/config/environment.dart`
      Verify: `cd apps/findu_proveedores; flutter analyze` sin errores nuevos.

- [ ] 10. App: `market_feed_screen.dart` — un solo socket canónico.
      En `_initWebSocket`: `canonicalId = session.proveedorId ?? _profile?.id`; si null/<=0 NO abrir (el gancho
      `_loadActivationAndProfile()` reinvoca `_initWebSocket()` al llegar el perfil). Eliminar la rama `primaryId` por
      `userId`/`authUserId`. Eliminar por completo `_wsSecondaryClient`, `_wsSecondarySubscription`,
      `_wsSecondaryStatusSubscription`, `_secondaryWsTargetId`, `_secondaryWsConnected` y su limpieza en el teardown;
      `_updateWsConnectionState` depende sólo del canal primario.
      Files: `apps/findu_proveedores/lib/features/market/presentation/screens/market_feed_screen.dart`
      Verify: `flutter analyze` sin errores; `flutter test` verde.

- [ ] 11. App: `ticket_detail_chat_screen.dart` — un solo socket + gancho de reconexión (hallazgo #1).
      Usar `proveedorId` como único id; si null/<=0 NO abrir. Hacer `_initWebSocket` idempotente (retorna si
      `_wsClient != null` o id inválido). Suscribir `SessionManager().addListener(_onSessionChanged)` en `initState`
      que relance `_initWebSocket()` cuando `proveedorId` sea válido, y `removeListener` en `dispose`. Eliminar
      `_wsSecondaryClient` y `_wsSecondarySubscription` y su bloque de conexión. `senderId` ya usa `proveedorId`.
      Files: `apps/findu_proveedores/lib/features/support/presentation/screens/ticket_detail_chat_screen.dart`
      Verify: `flutter analyze` sin errores; `flutter test` verde.

- [ ] 12. Verificación integral + hallazgo #8 (documentación).
      Ejecutar `.\gradlew.bat test` en dispatcher y en core, y `flutter analyze` + `flutter test` en la app; todo verde.
      Corregir en `design.md` la evidencia de help-v2 para citar `requesterUserId` (no `senderId`); la conclusión
      (recipient canónico) no cambia (hallazgo #8, NIT documental).
      Files: `.agents/tasks/ws-canonical-id/design.md`
      Verify: los tres comandos de test pasan; la nota de help-v2 cita `requesterUserId`.

---

## Digest

### Dispatcher — `CORE/LAMBDA/findu-notification-dispatcher`

- **NUEVO `infrastructure/routing/RecipientResolver.java`** (ítems 1, 2): `@Component`,
  `ConcurrentHashMap<String,String> aliasToCanonical`, `String toCanonical(String)`, `void learnAlias(String,String)`.
  Test: `src/test/java/.../infrastructure/routing/RecipientResolverTest.java`.
- **`infrastructure/websocket/EventWebSocketHandler.java`** (ítem 3): `handle(WebSocketSession)` — hoy
  `registerSession(userId, session)` + `registerPresence(userId, nodeId)` y `doFinally` con
  `unregisterSession(userId, session.getId())` / `removePresence(userId, nodeId)`. Cambio: inyectar
  `RecipientResolver`, calcular `canonicalUserId = toCanonical(userId)` y usar esa única clave en registro y baja.
- **`service/EventDispatcherService.java`** (ítems 4, 6): `dispatch(String, EventEnvelope<?>, TipoEventoCanal)`,
  `procesarEntrega(String targetUserId, ...)` (hoy compara String exacto en `hasLocalSession`), sobrecarga
  `dispatch(EventEnvelope<?>)` (hoy `canalId = envelope.eventId()`). Cambios: inyectar `RecipientResolver`;
  normalizar `targetUserId` a canónico; `canalId = toCanonical(targetUserId)` con fallback `eventId()`; NUEVO
  privado `learnAliasFromPayload(EventEnvelope<?>)` (vive aquí, hallazgo #9). Test:
  `src/test/java/.../service/EventDispatcherServiceTest.java`.
- **`presentation/controller/NotificationDispatchController.java`** (ítem 5): `despacharEventoPresencia(request, recipient)`
  — hoy arma `canalId` por prefijo crudo y `EventEnvelope.of(eventType, recipient, payload)`. Cambio: `toCanonical(recipient)`
  en canalId y recipient + `learnAliasFromPayload` una vez. Test existente:
  `src/test/java/.../presentation/controller/NotificationDispatchControllerTest.java` (debe seguir verde).
- **`presentation/controller/ChannelController.java`** (ítem 5): `actualizarDisponibilidadProveedor` — hoy
  `String canalId = "PROVEEDOR_" + request.proveedorId()`. Cambio: envolver con `toCanonical(...)`.
- **Modelo `domain/model/EventEnvelope.java`** (referencia, sin cambios): record con `eventType`, `eventId`,
  `timestamp`, `targetUserId`, `payload` y `of(EventType, String, T)`.
- **`infrastructure/adapter/memory/LocalSessionRegistryAdapter.java`** (referencia, sin cambios de contrato):
  `registerSession`/`unregisterSession`/`hasLocalSession`/`sendToUserLocally` por una sola clave.
- Verificado hoy: existen `src/test/.../config/RabbitMQConfigConverterIsolationTest.java` y
  `.../presentation/controller/NotificationDispatchControllerTest.java`; build `.\gradlew.bat test` (JUnit5 + reactor-test).

### Core — `CORE/findu-core`

- **`domain/observer/DispatcherNotificationObserver.java`** (ítems 7, 8): `sendToProvider(PerfilProveedor, String, String, Map)`
  — hoy hace dos `notificationPort.send` (`PROVEEDOR_<id>` y `PROVEEDOR_<authUserId>`); `sendToProviderId(Long, ...)` —
  idéntico doble envío vía `proveedorService.findById`. Cambio: un solo `send` con canónico + campos de alias en `params`.
  `PerfilProveedor` expone `getId()` y `getAuthUserId()` (S2). Test nuevo:
  `src/test/java/com/findu/core/domain/observer/DispatcherNotificationObserverTest.java`.
- Verificado hoy: tests existentes bajo `src/test/java/com/findu/core/...`; build `.\gradlew.bat test`.

### App — `apps/findu_proveedores`

- **`lib/core/config/environment.dart`** (ítem 9): `webSocketUrlFor(int proveedorId, {String? token})` — ya prefija
  `PROVEEDOR_`; renombrar parámetro + doc.
- **`lib/features/market/presentation/screens/market_feed_screen.dart`** (ítem 10): `_initWebSocket` (líneas ~133-193,
  hoy calcula `primaryId` por `userId` y abre `_wsSecondaryClient`); campos `_wsSecondaryClient`,
  `_wsSecondarySubscription`, `_wsSecondaryStatusSubscription`, `_secondaryWsTargetId`, `_secondaryWsConnected`;
  `_updateWsConnectionState` (líneas ~120-132); teardown (líneas ~208-215). Gancho de reconexión ya existe vía
  `_loadActivationAndProfile()` → `_initWebSocket()`.
- **`lib/features/support/presentation/screens/ticket_detail_chat_screen.dart`** (ítem 11): `_initWebSocket` (líneas ~104-133,
  hoy `primaryId` con fallback literal `1` y `_wsSecondaryClient`); `initState` (~43-44), `dispose` (~55-62);
  `senderId: proveedorId` en `_sendMessage` (~340). Añadir suscripción a `SessionManager` (`ChangeNotifier`, confirmado
  en `lib/core/session/session_manager.dart`, expone `proveedorId`, `userId`, `addListener`/`removeListener`).
- Verificado hoy: `flutter analyze` + `flutter test` en `apps/findu_proveedores` (`pubspec.yaml` presente).

### Qué se verificó durante la exploración

- Diseño leído completo; coinciden las firmas reales de los 7 archivos backend y los 3 de app citados.
- El dispatcher y el core tienen cada uno su propio Gradle wrapper (`gradlew.bat`), Java 21/Spring Boot 3.4.2,
  JUnit5 (`useJUnitPlatform`) y `reactor-test` ya declarados en el dispatcher.
- No se ejecutó aún build/test (se hará en cada ítem de verificación durante la implementación).
