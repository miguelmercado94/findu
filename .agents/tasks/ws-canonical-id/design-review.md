# Revisión de diseño (revisión 2) — Fase 0: Identificador canónico único de destinatario WebSocket

Esta es una **revisión de una revisión**. Existe un review previo (`design-review.json`, veredicto
`CHANGES_REQUESTED`, 7 hallazgos: 1 HIGH, 4 MEDIUM, 2 NIT) y el diseño nuevo añade una sección
"Respuestas a los hallazgos del review" que los declara todos ATENDIDOS.

Procedimiento: (1) re-chequear cada hallazgo previo contra el diseño nuevo y el código fuente real,
marcándolo **Cerrado** o **Sin cambios**; (2) revisar lo que cambió en busca de hallazgos nuevos.

Fuentes re-leídas y verificadas en esta pasada:
- Dispatcher: `EventDispatcherService`, `EventWebSocketHandler`, `JwtValidator`,
  `NotificationDispatchController`, `ChannelController`.
- Core: `DispatcherNotificationObserver`.
- Validacion: `ValidationEngineService`.
- Help-v2: `HttpRealtimeEventPublisher`, `CreateTicketService`, `HelpTicketWebMapper`,
  eventos `TicketMessageAddedEvent`/`TicketResolvedEvent`.
- App: `session_manager.dart`, `environment.dart`, `market_feed_screen.dart`,
  `ticket_detail_chat_screen.dart`, `create_ticket_screen.dart`, `help_api_adapter.dart`.

---

## Re-chequeo de hallazgos previos

### #1 (HIGH) — Chat de ticket sin gancho de reconexión → **CERRADO**
El diseño nuevo añade a `ticket_detail_chat_screen` la suscripción a `SessionManager`
(`addListener(_onSessionChanged)`), `_initWebSocket` idempotente y `removeListener` en `dispose`.
Verificado contra código: `SessionManager extends ChangeNotifier`; `updateProviderProfile(...)` llama
`notifyListeners()`; el getter `proveedorId` existe. El patrón propuesto es feasible y elimina el riesgo de
"socket nunca abre". El fix elegido es la opción (a) del review previo. Cerrado.

### #2 (MEDIUM) — CA7 no se cumplía por RabbitMQ (`canalId == eventId`) → **CERRADO**
Verificado en `EventDispatcherService.dispatch(EventEnvelope<?>)`: hoy hace `canalId = envelope.eventId()`
(UUID). Verificado en `ChannelController.actualizarDisponibilidadProveedor`: el canal de disponibilidad es
`"PROVEEDOR_" + proveedorId`. Por tanto el defecto es real: un evento Rabbit nunca caería en el canal
INACTIVO del proveedor. El diseño nuevo deriva `canalId = toCanonical(targetUserId)` con fallback a
`eventId` y declara RF5/CA7 aplicables al camino Rabbit, con test de integración. Correcto y necesario.
Cerrado.

### #3 (MEDIUM) — CA2/CA5 condicional para validacion/help-v2 → **CERRADO**
Re-verificado el origen de los ids de cada productor:
- **help-v2**: `HttpRealtimeEventPublisher` emite `role + "_" + reqId`, `reqId = getRequesterUserId() ?? getSenderId()`.
  `requesterUserId` se fija en creación del ticket (`CreateTicketService` → `TicketRequester(command.getUserId(), ...)`).
  En la app `create_ticket_screen._submit` llama `createTicket(userId: proveedorId.toString(), userRole: 'PROVEEDOR', ...)`.
  Por tanto, para tickets creados por la app de proveedores, `requesterUserId == proveedorId` y
  `PROVEEDOR_<reqId>` **sí es canónico**. La conclusión del diseño (help-v2 canónico, sin cambios) es correcta.
- **validacion**: `ValidationEngineService.complete`/`resolve` usan `userId = request.userId() ?? parseUserId(entityId)`
  y emiten `"PROVEEDOR_" + userId`, con `userId` un `Long` genérico de entidad. No hay garantía en este repo de
  que sea `proveedorId`. El diseño lo reconoce, reformula CA2 al caso canónico y mueve el subcaso `authUserId`
  a deuda explícita.

El tratamiento (CA2 reformulada + deuda) es consistente con la evidencia. Cerrado. (Ver hallazgo nuevo #8:
la *justificación textual* que el diseño da para help-v2 cita la evidencia equivocada.)

### #4 (MEDIUM) — Extracción de `canonicalRecipient`/`aliasRecipient` no especificada → **CERRADO**
El diseño nuevo define `learnAliasFromPayload(EventEnvelope<?>)` con `instanceof Map` y lectura de ambos
campos como `String`, ubicado en `dispatch(EventEnvelope<?>)` (Rabbit) y en `despacharEventoPresencia` (HTTP),
con `learnAlias` idempotente. Verificado que `despacharEventoPresencia` arma el payload desde
`request.params()` (`Map<String,Object>`), y que core (`DispatcherNotificationObserver`) usa
`Map<String,String>` para params — los campos aditivos son Strings, compatibles con la lectura defensiva.
Cerrado. (Ver hallazgo nuevo #9: inconsistencia menor de en qué clase vive el método.)

### #5 (MEDIUM) — Doble indexación vs `unregisterSession` (sesiones huérfanas) → **CERRADO**
Verificado en `EventWebSocketHandler.handle`: `doFinally` llama `unregisterSession(userId, session.getId())`
con una sola clave. El diseño nuevo elimina `indexKeysFor` y la doble indexación: registra y da de baja bajo
**una sola** clave canónica (`toCanonical(userId)`), y delega la tolerancia a apps viejas al enrutamiento
(`procesarEntrega` normaliza `targetUserId`). Esto elimina el riesgo de sesiones huérfanas. Cerrado.

### #6 (NIT) — `JwtValidator` `split("=")` → **CERRADO (como deuda documentada)**
Verificado que `extractAndValidateUserId` parte con `param.split("=")` y exige `pair.length == 2`.
Observación adicional verificada en `environment.dart`: `webSocketUrlFor` **sí** adjunta `&token=<token>`
además de `?userId=PROVEEDOR_<id>` cuando hay token, de modo que la afirmación del diseño de que "`?userId=`
es el único parámetro que esta fase usa" no es literalmente exacta. No obstante, `userIdParam` se prioriza y
se devuelve antes de tocar el token, y un JWT base64url normalmente no contiene `=`, así que el parsing frágil
no afecta la extracción del `userId` en esta fase. Dejarlo fuera de alcance con el fix `split("=", 2)` anotado
en deuda es defendible. Cerrado.

### #7 (NIT) — Ambigüedad "numérico puro ⇒ proveedor" → **CERRADO**
El diseño justifica el paso 6 de `toCanonical` revisando los productores (clientes usan `CLIENTE_<id>` o
username/email; help-v2 siempre prefija por rol) y lo anota como red de seguridad acotada con deuda para
restringirlo si apareciera un productor de ids de cliente pelados. Consistente con el código leído. Cerrado.

**Resultado del re-chequeo:** los 7 hallazgos previos quedan Cerrados. No hay reaperturas.

---

## Hallazgos nuevos (de revisar lo que cambió)

### 8. NIT — La justificación de "help-v2 es canónico" cita la evidencia equivocada (confunde `senderId` con `requesterUserId`)

**Dónde:** diseño, sección "Help-v2" y respuesta al hallazgo #3: *"la app (`ticket_detail_chat_screen._sendMessage`)
postea a help-v2 con `senderId: proveedorId` … por tanto `reqId` es el `proveedorId`"*.

**Problema:** el recipient de help-v2 se arma con `reqId = getRequesterUserId() ?? getSenderId()`, y
`requesterUserId` **no** se deriva del `senderId` del mensaje: se fija al **crear** el ticket
(`CreateTicketService` lo toma de `command.getUserId()`). El que la app ponga `senderId: proveedorId` al
enviar un mensaje no prueba nada sobre `requesterUserId`. La conclusión (canónico) resulta **correcta**, pero
por otra razón: `create_ticket_screen` llama `createTicket(userId: proveedorId.toString())`, luego
`requester.userId == proveedorId` y `PROVEEDOR_<requesterUserId>` es canónico. El diseño debería citar esta
cadena (creación de ticket con `proveedorId`), no el `senderId` del mensaje, para que la afirmación sea
verificable y no se rompa si el fallback `getSenderId()` llegara a activarse.

**Fix concreto:** en la sección Help-v2, reemplazar la justificación por:
"Verificado: `create_ticket_screen` crea el ticket con `userId = proveedorId` →
`TicketRequester.userId = proveedorId` → `TicketMessageAddedEvent.requesterUserId = proveedorId`, por lo que
`PROVEEDOR_<requesterUserId>` es canónico. El fallback `reqId = getSenderId()` sólo se usaría si
`requesterUserId` fuese null (no ocurre para tickets creados por la app); anotarlo como invariante a
endurecer." Mantener help-v2 sin cambios de código.

### 9. NIT — Ambigüedad sobre en qué clase vive `learnAliasFromPayload`

**Dónde:** diseño, bullets de `EventDispatcherService` ("vive en un solo método privado
`learnAliasFromPayload`") y de `NotificationDispatchController.despacharEventoPresencia`
("llamar `recipientResolver.learnAliasFromPayload(envelope)` (o inline equivalente)").

**Problema:** el método se describe primero como `private` de `EventDispatcherService`, pero luego se invoca
como `recipientResolver.learnAliasFromPayload(...)` desde el controller. Un `private` de otra clase no es
invocable, y `despacharEventoPresencia` vive en `NotificationDispatchController`, que llama a
`dispatch(canalId, ...)` (la sobrecarga que el diseño dice que **no** debe aprender). Queda ambiguo quién
posee el método y cómo lo llama el controller.

**Fix concreto:** elegir una ubicación única y coherente. Opción simple: mover `learnAliasFromPayload` a
`RecipientResolver` como método **público** (`public void learnAliasFromPayload(EventEnvelope<?> e)`), e
invocarlo desde ambos puntos de entrada (`dispatch(EventEnvelope<?>)` y `despacharEventoPresencia`). Así el
texto "`recipientResolver.learnAliasFromPayload(envelope)`" es literal y no hay método privado inaccesible.

### 10. MEDIUM — El `canalId` que hoy calcula `despacharEventoPresencia` deja de coincidir con el `targetUserId` normalizado, salvo que ambos se normalicen con la misma regla

**Dónde:** diseño, bullets de `NotificationDispatchController.despacharEventoPresencia`
("derivar `canalId` … con `toCanonical(recipient)`") y de
`EventDispatcherService.dispatch(String canalId, ...)` / `procesarEntrega`
("`targetUserId = toCanonical(envelope.targetUserId())`").

**Problema:** `procesarEntrega` opera sobre `toCanonical(envelope.targetUserId())`, mientras que el canal
(persistencia/estado ACTIVO-INACTIVO) se abre con el `canalId` que pasa el llamador. Hoy
`despacharEventoPresencia` construye el `EventEnvelope` con `recipient` **crudo** y arma `canalId` con su
propia lógica de prefijos. Si el diseño normaliza el `targetUserId` dentro de `dispatch` pero el
`EventEnvelope` sigue llevando el `recipient` crudo como `targetUserId`, entonces: (a) el estado del canal se
evalúa sobre `canalId` (una clave) y (b) la entrega local se evalúa sobre `toCanonical(targetUserId)` (otra
clave). Para un recipient no canónico (app vieja por `authUserId`) con alias conocido, `canalId` y el
`targetUserId` canónico pueden diferir, reintroduciendo la desalineación canal↔entrega que esta fase busca
eliminar (es el mismo defecto de clase del hallazgo #2, pero en el camino HTTP de presencia).

El diseño menciona construir el envelope con `toCanonical(recipient)` en `despacharEventoPresencia`, lo que
mitigaría el problema; pero no fija **una** regla única: hay tres lugares que calculan clave de canal
(`despacharEventoPresencia`, `ChannelController`, y `dispatch(EventEnvelope<?>)` para Rabbit) y el enrutamiento
normaliza aparte en `procesarEntrega`. Si no se garantiza que `canalId == toCanonical(targetUserIdDelEnvelope)`
en los tres caminos, queda un hueco.

**Fix concreto:** declarar explícitamente la invariante **"`canalId` siempre es
`toCanonical(targetUserId)` del mismo envelope"** y satisfacerla en un solo punto: normalizar el
`targetUserId` del envelope **antes** de calcular `canalId`, de modo que todos los caminos deriven el
`canalId` del mismo valor canónico. Concretamente, en `despacharEventoPresencia` construir el `EventEnvelope`
con `toCanonical(recipient)` como `targetUserId` y `canalId = esetargetUserId` (no recalcular prefijos aparte);
y en `dispatch(String, ...)` documentar que asume `canalId == toCanonical(envelope.targetUserId())`. Añadir un
test que verifique, para un recipient entrante no canónico con alias conocido, que el canal consultado para
ACTIVO/INACTIVO y el socket de entrega son la misma clave canónica.

---

## Supuestos verificados (esta pasada)

- **help-v2 canónico para tickets de la app de proveedores:** VERIFICADO.
  `create_ticket_screen` → `createTicket(userId: proveedorId.toString())` →
  `CreateTicketService` fija `TicketRequester.userId = command.getUserId()` →
  `TicketMessageAddedEvent.requesterUserId` se propaga a `PROVEEDOR_<reqId>`. Canónico.
- **validacion NO garantizable canónica:** VERIFICADO. `ValidationEngineService` usa un `Long` genérico
  (`request.userId()` o `parseUserId(entityId)`); el diseño lo trata correctamente como deuda.
- **core doble envío a eliminar:** VERIFICADO. `DispatcherNotificationObserver.sendToProvider`/`sendToProviderId`
  emiten `PROVEEDOR_<id>` y, si `authUserId != id`, un segundo `send` con `PROVEEDOR_<authUserId>`.
- **core params son `Map<String,String>`:** VERIFICADO; los campos `canonicalRecipient`/`aliasRecipient`
  aditivos son Strings, compatibles con `learnAliasFromPayload`.
- **`dispatch(EventEnvelope<?>)` usa `canalId = eventId`:** VERIFICADO (raíz del hallazgo #2).
- **`ChannelController` usa `"PROVEEDOR_" + proveedorId`:** VERIFICADO (canal de disponibilidad ya canónico).
- **`EventWebSocketHandler.doFinally` desregistra con una sola clave:** VERIFICADO (sustenta el fix de #5).
- **`SessionManager` es `ChangeNotifier` y `updateProviderProfile` notifica:** VERIFICADO (sustenta el fix de #1).
- **`market_feed_screen._loadActivationAndProfile` reinvoca `_initWebSocket`:** VERIFICADO (gancho de deferral
  existente para RF2/CA3).
- **`environment.dart.webSocketUrlFor` prefija `PROVEEDOR_` y adjunta `&token=`:** VERIFICADO.

## Supuestos no verificados / imprecisos

- **IMPRECISO — "la app postea `senderId: proveedorId`, por tanto `reqId` es `proveedorId`":** la premisa es
  cierta pero no sustenta la conclusión; `reqId` sale de `requesterUserId` (creación del ticket), no de
  `senderId`. Conclusión correcta por otra vía. Ver hallazgo #8.
- **NO ASEGURADO — invariante `canalId == toCanonical(targetUserId)` en los tres caminos de canal:** el
  diseño normaliza el enrutamiento y menciona normalizar en `despacharEventoPresencia`, pero no fija la
  invariante única. Ver hallazgo #10.
- **NO VERIFICADO (correctamente fuera de alcance) — mapa `aliasToCanonical` multi-nodo:** el diseño lo deja
  como deuda (sin Redis). Refuerza el carácter transitorio de la red de seguridad, no bloquea la fase.

---

## Conteo y veredicto

- Hallazgos previos: 7/7 **Cerrados**.
- Hallazgos nuevos: #8 NIT, #9 NIT, #10 MEDIUM.
- HIGH: 0
- MEDIUM: 1 (Hallazgo 10)
- NIT: 2 (Hallazgos 8, 9)

HIGH + MEDIUM = 1 (> 0) → **CHANGES_REQUESTED**
