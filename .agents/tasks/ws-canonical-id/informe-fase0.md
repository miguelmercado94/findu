# Informe Fase 0 — Identificador canónico de WebSocket

Esta fase fija el identificador canónico de proveedor que usarán todas las fases
siguientes (FCM, ciclo de vida de canales). El cambio ya fue implementado,
revisado y commiteado en `develop` de ambos repos.

## Identificador canónico elegido

`PROVEEDOR_<proveedorId>`

Es la **única** clave canónica para:

- Handshake del WebSocket y registro de sesión (`registerSession`).
- Presencia (`registerPresence` / `removePresence`).
- `canalId` de enrutamiento y de estado del canal (ACTIVO/INACTIVO).
- Historial y entrega de eventos (`procesarEntrega`).

Regla invariante objetivo: `canalId == toCanonical(targetUserId)`.

El `proveedorId` (no el `authUserId`) es siempre el valor canónico. Durante la
ventana de transición se usa un `RecipientResolver` como red de seguridad que
aprende aliases `authUserId -> proveedorId` desde el payload del evento, de forma
idempotente. En help-v2 la canonicidad se deriva de `requesterUserId`
(`createTicket(userId: proveedorId)` → `requesterUserId = proveedorId`).

## Puntos tocados

### Backend (`findu`) — commits `ccd00f2`, `5b0e854`

Dispatcher:
- `infrastructure/routing/RecipientResolver.java` — nuevo: `toCanonical` + `learnAlias` idempotente.
- `infrastructure/websocket/EventWebSocketHandler.java` — una sola clave canónica para registrar/presencia/teardown.
- `service/EventDispatcherService.java` — `dispatch(EventEnvelope<?>)` deriva el `canalId` canónico; `learnAliasFromPayload` privado.
- `presentation/controller/NotificationDispatchController.java` — presencia HTTP normaliza el recipient; `aprenderAliasDesdePayload` inline.
- `presentation/controller/ChannelController.java` — `canalId` de disponibilidad envuelto en `toCanonical`.
- Tests: `RecipientResolverTest`, `EventDispatcherServiceTest`, `NotificationDispatchControllerTest` (mock de `RecipientResolver`).

Core:
- `domain/observer/DispatcherNotificationObserver.java` — un solo `send` por proveedor; parámetros de alias aditivos (`canonicalRecipient`/`aliasRecipient`).
- `test/.../observer/DispatcherNotificationObserverTest.java` — nuevo.
- `design.md` — corrección de help-v2 (canonicidad desde `requesterUserId`).

### App Flutter (`findu-movil/apps/findu_proveedores`) — commit `f10a811`

- `core/config/environment.dart` — doc exige `proveedorId`.
- `features/market/.../market_feed_screen.dart` — elimina socket secundario, usa id canónico.
- `features/support/.../ticket_detail_chat_screen.dart` — elimina socket secundario, id canónico, reconexión vía `SessionManager`.
- `test/alert_websocket_mapping_test.dart` — tests unitarios de mapeo canónico (forma de URL).

## Estado de verificación (del veredicto de revisión)

- Builds y tests verdes en los tres módulos: dispatcher (`RecipientResolverTest` 9/0,
  `EventDispatcherServiceTest` 4/0, `NotificationDispatchControllerTest` 2/0); core
  (54 tests, 0 fallos, incluye `DispatcherNotificationObserverTest` 2/0); app
  (`flutter analyze` sin issues en archivos tocados; 5 fallos de `flutter test`
  preexistentes, verificados con `git stash`).
- Hallazgos #8 (doc help-v2) y #9 (propiedad del método de alias) resueltos.

## Pendientes a considerar en fases siguientes

Estos dos puntos (del veredicto) no bloquean el id canónico pero conviene
cerrarlos cuando se aborde FCM / ciclo de vida:

1. **#10 — camino HTTP de presencia sin test de integración.** La invariante
   `canalId == toCanonical(targetUserId)` está impuesta en código para proveedores
   en los tres caminos y con test en el camino RabbitMQ, pero el test del
   controller mockea el dispatcher, así que el camino HTTP de presencia no prueba
   la invariante con un recipient de proveedor aliased. Falta un test de
   integración del dispatcher para ese camino.
2. **Asimetría residual del `canalId` para recipients sin prefijo.** En
   `despacharEventoPresencia`, un recipient no prefijado produce
   `canalId = "CLIENTE_" + canonicalRecipient` mientras el `targetUserId` del
   envelope queda sin prefijo, por lo que estado del canal y entrega se normalizan
   a claves distintas en ese caso. Los proveedores no se ven afectados
   (`toCanonical` siempre da prefijo `PROVEEDOR_`), pero la invariante no se cumple
   de forma uniforme.

## Referencias

- Revisión completa: `.agents/tasks/ws-canonical-id/2026-11-03-103103-review.md`
- Veredicto: `.agents/tasks/ws-canonical-id/verdict.json`
