# Fase 0 — Identificador canónico único de destinatario WebSocket (FIND-U)

> Alcance: unificación de identidad de destinatario + un solo socket por app + eliminación del
> falso OFFLINE. **NO** incluye ciclo de vida `AppLifecycleState` ni FCM real (fases posteriores).
> Repos: backend `findu` (rama `develop`, remoto `origin`), app `findu-movil/apps/findu_proveedores`
> (rama `develop`, sin remoto). Insumo de investigación: `.agents/tasks/puesta-al-dia-y-ws-estado.md` §B.6.

---

# PARTE 1 — REQUISITOS

## Resumen

Hoy la entrega de eventos en vivo a proveedores falla de forma intermitente (log "… OFFLINE" pese a
socket vivo) porque **no existe un acuerdo único sobre qué String identifica a un proveedor** en el
flujo WebSocket. El handshake de la app registra la sesión bajo el String exacto `?userId=PROVEEDOR_<X>`
(donde `X` es a veces `authUserId`, a veces `proveedorId`), mientras que el enrutamiento
(`EventDispatcherService.procesarEntrega`) hace una comparación de String **exacta** contra
`envelope.targetUserId()` (el recipient que puso el productor). Si productor y handshake no usan el
mismo String, `hasLocalSession` devuelve `false` y el evento cae a FCM aunque el socket esté vivo.

Dos parches conviven hoy para tapar esto, ambos a eliminar:
- **App**: abre hasta **dos sockets** (primario por `authUserId`, secundario por `proveedorId`).
- **Backend (core)**: `DispatcherNotificationObserver` emite **cada evento dos veces**
  (`PROVEEDOR_<proveedorId>` y `PROVEEDOR_<authUserId>`).

El objetivo es definir e imponer **UN identificador canónico único** de destinatario para proveedores,
de modo que un solo socket baste y `hasLocalSession` siempre acierte.

## Requisitos funcionales

- RF1. Existe un identificador canónico único de destinatario para un proveedor, con un único formato
  textual, usado de forma idéntica por: (a) el `?userId=` del handshake, (b) el `targetUserId`/`recipient`
  que emiten los productores, (c) el `canalId` del dispatcher, (d) la clave de `LocalSessionRegistryAdapter`,
  y (e) la presencia en Redis.
- RF2. La app abre **un solo** socket WebSocket por pantalla que lo necesita (Home y chat de ticket),
  usando el identificador canónico. Se elimina el socket secundario en ambas pantallas.
- RF3. El dispatcher enruta por el identificador canónico. Cualquier recipient que llegue en un formato
  equivalente pero no canónico (p.ej. `PROVEEDOR_<authUserId>` en vez de `PROVEEDOR_<proveedorId>`, o un
  id numérico puro) se **normaliza a canónico en un único punto** antes de registrar/enrutar.
- RF4. Los productores de eventos de proveedor (core `DispatcherNotificationObserver`, validacion,
  help-v2) emiten el identificador canónico. Se elimina el doble envío de core.
- RF5. El `canalId` de disponibilidad (`ChannelController.actualizarDisponibilidadProveedor`, que hoy
  arma `"PROVEEDOR_" + proveedorId`) queda alineado con el identificador canónico, de modo que el estado
  ACTIVO/INACTIVO se evalúe sobre el mismo canal que recibe los eventos.
- RF6. Se mantienen sin cambios: el heartbeat PING/PONG, la reconexión automática del cliente, los
  payloads de negocio y los `EventType`.

## Requisitos no funcionales

- RNF1. Sin regresión en la entrega: todo evento que hoy llega por el doble-envío/doble-socket debe
  seguir llegando con el esquema único.
- RNF2. Compatibilidad durante el despliegue: la normalización en el dispatcher debe tolerar apps viejas
  (que aún abran por `authUserId`) y productores viejos durante una ventana de transición, sin exigir
  despliegue atómico de los tres repos.
- RNF3. El cambio no debe degradar el rendimiento del hot-path de enrutamiento (la normalización debe ser
  O(1) o apoyarse en una caché; ver diseño).

## Criterios de aceptación

1. Con proveedor cuyo `authUserId != proveedorId`, autenticado y con **un solo** socket abierto, un evento
   emitido por core (p.ej. `NUEVA_SOLICITUD`) se entrega por WebSocket y el log del dispatcher indica
   "socket local", **no** "OFFLINE".
2. Para un proveedor cuyo id emitido por el productor **es el `proveedorId`** (caso canónico, el habitual):
   un evento de validacion (`SYSTEM_ALERT`, recipient `PROVEEDOR_<proveedorId>`) y uno de help-v2
   (`CHAT_MESSAGE` de soporte, recipient `PROVEEDOR_<proveedorId>` vía `requesterUserId`) se entregan por
   WebSocket al único socket canónico. (El subcaso en que validacion emitiera `authUserId` queda cubierto sólo
   si core ya enseñó el alias en ese nodo; se trata como deuda, no como criterio garantizado — ver §Deuda y
   el análisis de productores en las secciones de Validacion y Help-v2.)
3. `market_feed_screen.dart` y `ticket_detail_chat_screen.dart` ya no declaran ni abren
   `_wsSecondaryClient`; sólo existe un `WebSocketClient` activo por pantalla.
4. `DispatcherNotificationObserver.sendToProvider`/`sendToProviderId` emiten el recipient canónico una sola
   vez por proveedor (no dos `notificationPort.send`).
5. Un recipient entrante no canónico pero resoluble (p.ej. `PROVEEDOR_<authUserId>`) se normaliza y se
   entrega al socket canónico registrado; se cubre con una prueba unitaria del normalizador.
6. Un recipient `CLIENTE_*`, `AGENTE_*` o `SOPORTE_*` **no** se ve afectado por la normalización de
   proveedor (se enruta igual que hoy).
7. El toggle de disponibilidad (INACTIVO) sigue forzando FCM sobre el mismo canal canónico del proveedor.

## Fuera de alcance

- `WidgetsBindingObserver` / `AppLifecycleState` (abrir/cerrar socket por foreground/background).
- Integración FCM real (device token, `firebase_messaging`, `FirebasePushSender` deja de ser stub).
- Chat cliente↔proveedor (`CANAL_2_CHAT_PROVEEDOR`) y cualquier canal nuevo.
- Validar la firma JWT en el handshake (hoy `?userId=` se acepta sin verificar; se deja como está).
- Migrar el repositorio de canales in-memory a NoSQL.

## Supuestos

- S1. El `proveedorId` (id de perfil de negocio) es el identificador estable y preferible; el `authUserId`
  es el id de la cuenta de seguridad. En la app `ProviderSession` expone ambos (`authUserId` no nulo,
  `proveedorId` nullable hasta que carga el perfil).
- S2. `PerfilProveedor` en core conoce tanto `getId()` (proveedorId) como `getAuthUserId()`, por lo que el
  mapeo `authUserId -> proveedorId` es resoluble en el dominio de core.
- S3. El dispatcher **no** tiene hoy acceso directo a la tabla de perfiles; su conocimiento del proveedor
  llega sólo por el String del recipient/handshake. Esto condiciona dónde puede vivir la normalización
  (ver diseño, decisión D2).

---

# PARTE 2 — DISEÑO TÉCNICO

## Overview

Se fija el identificador canónico de un proveedor como **`PROVEEDOR_<proveedorId>`**, donde `<proveedorId>`
es el id del perfil de negocio (`PerfilProveedor.getId()`), nunca el `authUserId`. Ese String se convierte
en la única clave válida para handshake, registro de sesión, presencia, `canalId`, `targetUserId` y
`recipient`.

Para que esto no exija un despliegue atómico de tres repos ni confíe en que la app tenga el `proveedorId`
disponible en el instante del handshake, el diseño combina **dos movimientos complementarios**:

1. **Fuente canónica en el productor**: **core** —que es quien conoce el par `authUserId↔proveedorId`— emite
   directamente `PROVEEDOR_<proveedorId>` **una sola vez** (se elimina el doble envío) y alimenta el mapa de
   alias. help-v2 ya emite el canónico por construcción (`requesterUserId == proveedorId`, verificado) y no se
   toca; validacion queda cubierta por la red de seguridad salvo el subcaso `authUserId`, que es deuda
   explícita (ver hallazgo #3).
2. **Red de seguridad en el dispatcher**: un único componente `RecipientResolver` normaliza, al borde de
   entrada del dispatcher (handshake y enrutamiento), cualquier variante equivalente hacia la forma
   canónica, de modo que una app vieja que abra por `authUserId`, o un productor que aún emita `authUserId`,
   sigan funcionando durante la transición.

La app pasa a abrir **un solo socket** con el id canónico. Como el `proveedorId` puede no estar disponible
en el primer frame, la app difiere/relanza la conexión hasta tener `proveedorId` (ya existe el gancho:
`_initWebSocket()` se reinvoca tras `_loadActivationAndProfile()`), eliminando la necesidad del socket por
`authUserId`.

## Stack tecnológico (bloqueado)

- Backend dispatcher: Java 17 + Spring Boot WebFlux (reactivo, `WebSocketHandler` no-STOMP), Lombok,
  Jackson, Reactor. Sin librerías nuevas.
- Core / validacion / help-v2: Java 17 + Spring Boot (MVC), `RestTemplate`/`RestClient` existentes. Sin
  librerías nuevas.
- App: Flutter/Dart, paquete interno `packages/network_ws` (`WebSocketClient`). Sin dependencias nuevas.

## Decisión D1 — Identificador canónico = `PROVEEDOR_<proveedorId>`

Alternativas consideradas:

- **(A) `PROVEEDOR_<authUserId>`**: el handshake siempre tiene `authUserId` disponible (la sesión lo trae
  desde el login), así que la app no tendría que esperar al perfil. Pero los productores de negocio (core)
  razonan en `proveedorId`; forzarlos a traducir a `authUserId` esparce el mapeo por todos los observers y
  rompe el `canalId` de disponibilidad que hoy ya usa `proveedorId`.
- **(B) `PROVEEDOR_<proveedorId>`** (ELEGIDA): es el id de negocio estable; core ya lo usa como primario,
  `ChannelController` ya arma el canal de disponibilidad con él, y help-v2 lo emite para proveedores. El
  único coste es que la app debe esperar a tener `proveedorId` antes de conectar, lo que ya ocurre de forma
  natural (el Home reconecta tras cargar el perfil).

Se elige **(B)**. Minimiza los puntos de cambio en el backend (core y el canal de disponibilidad ya están
en `proveedorId`) y usa el identificador de negocio correcto. El coste en la app (esperar el perfil) es
local y ya soportado por el flujo actual de reconexión.

Formato exacto: prefijo literal `PROVEEDOR_` + el `proveedorId` en decimal sin ceros a la izquierda
(equivalente a `String.valueOf(Long)`), p.ej. `PROVEEDOR_42`. Se preservan los prefijos existentes para
otros roles: `CLIENTE_`, `AGENTE_`, `SOPORTE_`.

## Decisión D2 — Dónde vive la normalización: `RecipientResolver` en el dispatcher

Alternativas:

- **(A) Normalizar en cada productor**: obliga a tocar core, validacion y help-v2 y a que cada uno resuelva
  `authUserId -> proveedorId`. Validacion a veces sólo tiene un `entityId` numérico ambiguo (ver
  `ValidationEngineService.parseUserId`), así que no siempre puede resolver con certeza. Muchos puntos de
  cambio, riesgo alto.
- **(B) Un único `RecipientResolver` en el dispatcher** (ELEGIDA): el dispatcher es el punto por el que
  pasan **todos** los caminos (handshake, `RabbitEventConsumer`, `NotificationDispatchController`,
  `EventDispatcherService`). Normalizar ahí es el menor número de puntos de cambio y el único lugar que ve
  todo el tráfico. Es también donde hoy ya vive la lógica de prefijos (`despacharEventoPresencia`).

Se elige **(B)**, complementada con la limpieza del doble envío en core (que es trivial y reduce ruido). El
`RecipientResolver` es la **red de seguridad** que hace el sistema tolerante a despliegues no atómicos; la
emisión canónica del productor es la **fuente de verdad** a mediano plazo.

### Problema del mapeo `authUserId -> proveedorId` sin acceso a perfiles

El dispatcher no tiene la tabla de perfiles (S3). Por tanto el `RecipientResolver` **no puede** traducir un
`authUserId` arbitrario a `proveedorId` por sí mismo. Se resuelve con un **registro de equivalencias
alimentado por el propio tráfico**, de forma que el dispatcher aprende el mapeo sin nueva dependencia:

- El `RecipientResolver` mantiene un `ConcurrentHashMap<String,String>` `aliasToCanonical` en memoria del
  nodo (equivalente al `LocalSessionRegistryAdapter`, O(1), sin librerías nuevas).
- **Alimentación desde core**: `DispatcherNotificationObserver`, cuando `authUserId != proveedorId`, en vez
  de emitir el evento dos veces, emite **una sola vez** el canónico `PROVEEDOR_<proveedorId>` **e incluye en
  el payload** los campos `canonicalRecipient = "PROVEEDOR_<proveedorId>"` y
  `aliasRecipient = "PROVEEDOR_<authUserId>"`. El `RecipientResolver` registra esa equivalencia al procesar
  el evento. (Core es el único que conoce con certeza ambos ids.)
- **Uso en handshake**: si una app vieja abre con `?userId=PROVEEDOR_<authUserId>` y el resolver ya conoce la
  equivalencia (porque core ya emitió para ese proveedor en este nodo), `toCanonical` registra la sesión bajo
  el canónico. Si aún no la conoce, registra bajo el String recibido tal cual (comportamiento actual). En
  cualquier caso se registra bajo **una sola** clave y `doFinally` la limpia con esa misma clave —no hay doble
  indexación de sesión (ver hallazgo #5 del review); la tolerancia a apps viejas vive en la normalización del
  **enrutamiento** (`procesarEntrega` sobre `toCanonical(targetUserId)`), no en indexar la sesión dos veces.

> Nota de simplificación: una vez la **app** emite el canónico (RF2) y **core** emite el canónico (RF4), el
> `RecipientResolver` queda como red de seguridad pasiva. No es imprescindible para el caso feliz; es lo que
> permite no exigir despliegue atómico (RNF2). Si el equipo acepta un despliegue coordinado de los tres
> repos, el resolver puede reducirse a una normalización trivial de formato (trim + prefijo) sin el mapa de
> alias. Se recomienda incluir el mapa porque el coste es bajo y la app `develop` no tiene remoto (despliegue
> desacoplado garantizado).

## Archivos a modificar e integración

### Dispatcher (`findu/CORE/LAMBDA/findu-notification-dispatcher`)

- **NUEVO** `infrastructure/routing/RecipientResolver.java` (`@Component`):
  - `String toCanonical(String recipient)`: reglas en orden —
    1. `null`/blank → devuelve el valor tal cual (el llamador ya lo descarta).
    2. `trim()`.
    3. Si empieza por `CLIENTE_` / `AGENTE_` / `SOPORTE_` → devolver tal cual (no es proveedor).
    4. Si está en `aliasToCanonical` → devolver el canónico mapeado.
    5. Si empieza por `PROVEEDOR_` → devolver tal cual (ya es forma proveedor; se asume canónico salvo que
       un alias conocido diga lo contrario, cubierto por el paso 4).
    6. Si es numérico puro (sin prefijo) → prefijar `PROVEEDOR_`. **Justificación (hallazgo #7):** se revisaron
       los productores y ningún recipient de cliente llega como entero sin prefijo — los clientes usan
       `CLIENTE_<id>` (prefijado) o `username`/email (no numérico), y help-v2 siempre prefija por rol
       (`role + "_" + id`, `CLIENTE_`, `PROVEEDOR_`, `AGENTE_`, `SOPORTE_GENERAL`). El único productor que
       emite un recipient con intención de "proveedor por id" sin que ya traiga el prefijo es la app vieja
       antes de que `environment.dart` lo prefijara, caso ya cubierto. Este paso es, por tanto, una red de
       seguridad acotada; si en el futuro apareciera un productor que emita ids de cliente pelados, la regla
       debería moverse a un contexto confirmado de proveedor. Se documenta como supuesto verificado, no como
       suposición ciega.
    7. En cualquier otro caso → devolver tal cual (recipient de cliente por username, email, etc.).
  - `void learnAlias(String alias, String canonical)`: puebla `aliasToCanonical` (idempotente; ignora si
    `alias == canonical`, si alguno es blank, o si no empiezan por `PROVEEDOR_`).
  - (Se elimina `indexKeysFor`: ver hallazgo #5; la sesión se registra bajo una sola clave canónica.)
- **`EventWebSocketHandler.handle` — corrige hallazgo #5.** Tras `extractAndValidateUserId`, pasar el `userId`
  por `recipientResolver.toCanonical(...)` y registrar la sesión y la presencia bajo **esa única clave
  canónica**. Se descarta la idea de la "doble indexación" (registrar también bajo el alias) porque
  `doFinally` hace `unregisterSession(userId, session.getId())` con un **solo** id: registrar bajo varias
  claves y limpiar sólo una dejaría sesiones cerradas colgadas en `localSessions` bajo las demás, provocando
  `hasLocalSession==true` sobre sockets muertos (una nueva fuente de entregas perdidas). Además, en el
  instante del handshake el alias casi nunca se conoce todavía (se aprende después, con el primer evento de
  core), así que indexar por alias sería un no-op en la práctica.

  La cobertura del caso "app vieja que abre por `authUserId`" no necesita doble indexación de sesión: el
  **enrutamiento** ya normaliza (`procesarEntrega` opera sobre `toCanonical(targetUserId)`). Para que eso
  funcione con una app vieja hace falta que el resolver conozca el alias y que la sesión esté indexada bajo el
  canónico — pero la app vieja se registra bajo `PROVEEDOR_<authUserId>` (que para ella **es** su único id).
  Por tanto, para la ventana de transición, el registro usa la forma canónica que el resolver pueda derivar en
  ese momento: `toCanonical(userId)` devuelve el canónico si el alias ya es conocido, o el String recibido tal
  cual si no. `unregisterSession` en `doFinally` usa exactamente esa misma clave (una sola), por lo que el
  cierre siempre limpia lo que registró. No quedan sesiones huérfanas.
- **`EventDispatcherService.dispatch(String canalId, ...)` / `procesarEntrega`**: al inicio, `targetUserId =
  recipientResolver.toCanonical(envelope.targetUserId())`. El resto del flujo (local → cluster → fallback)
  queda igual, pero operando sobre el canónico.
- **`EventDispatcherService.dispatch(EventEnvelope<?>)` (sobrecarga sin `canalId`, usada por el camino
  RabbitMQ) — corrige hallazgo #2.** Hoy hace `canalId = envelope.eventId()` (un UUID aleatorio), de modo que
  para los eventos que entran por `findu.dispatcher.events.queue` (los `SYSTEM_ALERT` de validacion y todo lo
  reencolado por el `notification-processor`) el estado ACTIVO/INACTIVO se evaluaría sobre un canal UUID nuevo
  —siempre ACTIVO— y **el toggle INACTIVO no forzaría FCM** (CA7 incumplido para ese camino). Se cambia a
  derivar el `canalId` del `targetUserId` canónico, con el `eventId` sólo como último recurso:
  ```java
  public Mono<Void> dispatch(EventEnvelope<?> envelope) {
      learnAliasFromPayload(envelope); // ver hallazgo #4
      String canonical = recipientResolver.toCanonical(envelope.targetUserId());
      String canalId = (canonical != null && !canonical.isBlank()) ? canonical : envelope.eventId();
      return dispatch(canalId, envelope, TipoEventoCanal.MENSAJE);
  }
  ```
  Con esto RF5/CA7 aplican también al camino RabbitMQ: el canal de disponibilidad del proveedor
  (`PROVEEDOR_<id>`, el mismo que arma `ChannelController`) es el que se consulta para ACTIVO/INACTIVO.
- **Alimentación del alias (`learnAlias`) — corrige hallazgo #4.** El `payload` de `EventEnvelope<T>` es
  genérico: en el camino de presencia HTTP es un `Map<String,Object>` (los `params` de core llegan ahí,
  verificado), y en el camino RabbitMQ Jackson lo deserializa como `EventEnvelope<Object>` cuyo `payload` es
  un `LinkedHashMap`. Para cubrir ambos de forma segura, la extracción es **defensiva y vive en un solo
  método privado** `learnAliasFromPayload`, invocado **una vez por punto de entrada** (en
  `dispatch(EventEnvelope<?>)` para Rabbit y en `despacharEventoPresencia` para HTTP), nunca dentro de
  `dispatch(String, ...)`. Es idempotente:
  ```java
  private void learnAliasFromPayload(EventEnvelope<?> envelope) {
      if (envelope.payload() instanceof Map<?, ?> p) {
          Object canon = p.get("canonicalRecipient");
          Object alias = p.get("aliasRecipient");
          if (canon instanceof String c && alias instanceof String a) {
              recipientResolver.learnAlias(a, c); // no-op si alias==canonical o blanks
          }
      }
      // Si payload no es Map (p.ej. envelope sin params), no se aprende nada: comportamiento base.
  }
  ```
  Nota: la sobrecarga `dispatch(String, ...)` la invocan `NotificationDispatchController` (presencia HTTP) y la
  sobrecarga sin `canalId` (Rabbit). Como el aprendizaje se hace en cada punto de entrada y no en
  `dispatch(String, ...)`, no se aprende dos veces; y aun si ocurriera, `learnAlias` es idempotente.
- **`NotificationDispatchController.despacharEventoPresencia`**: derivar `canalId` y construir el
  `EventEnvelope` con `recipientResolver.toCanonical(recipient)` en lugar del `recipient` crudo, y llamar
  `recipientResolver.learnAliasFromPayload(envelope)` (o inline equivalente) una vez, antes de invocar
  `dispatch(canalId, ...)`, para aprender el alias que core incluye en los `params`. La lógica de prefijos
  existente se mantiene como fallback dentro del resolver (paso 3/7).
- **`ChannelController.actualizarDisponibilidadProveedor`**: ya usa `"PROVEEDOR_" + proveedorId`; envolver con
  `recipientResolver.toCanonical(...)` por consistencia (no-op funcional, pero centraliza el formato).
- **`LocalSessionRegistryAdapter`**: sin cambios de contrato. La sesión se registra y se da de baja bajo una
  **única** clave (la canónica derivada en el handshake); no hay indexación múltiple (hallazgo #5).

### Core (`findu/CORE/findu-core`)

- **`DispatcherNotificationObserver`**:
  - `sendToProvider(PerfilProveedor p, ...)`: emitir **una sola** vez `PROVEEDOR_<p.getId()>`. Si
    `authUserId != id`, añadir a `params` las claves `canonicalRecipient=PROVEEDOR_<id>` y
    `aliasRecipient=PROVEEDOR_<authUserId>` (no romper payload de negocio; son campos aditivos). Eliminar el
    segundo `notificationPort.send`.
  - `sendToProviderId(Long proveedorId, ...)`: idéntico; resolver `authUserId` vía `proveedorService.findById`
    sólo para poblar los campos de alias, sin segundo envío.
- No se tocan los recipients de cliente (`CLIENTE_*` / username) ni los payloads de negocio.

### Validacion (`findu/CORE/findu-validacion`)

- `ValidationEngineService` emite `"PROVEEDOR_" + userId`, donde `userId = request.userId()` o, en su
  defecto, `parseUserId(request.entityId())` (`Long.parseLong` del `entityId`). **Análisis del productor
  (hallazgo #3):** verificado en el código que ese `userId`/`entityId` lo provee el servicio que llama a
  `complete`/`resolve`; no se pudo localizar en este repo una garantía de que sea siempre el `proveedorId` y
  no el `authUserId` (el campo es un `Long` genérico "id de entidad"). Por eso **no puede afirmarse que
  validacion sea canónico por construcción** y la entrega WS de `SYSTEM_ALERT` queda condicionada:
  - Si `userId == proveedorId` (caso esperado), ya es canónico y CA2 se cumple sin depender del alias.
  - Si `userId == authUserId`, la normalización depende de que **core** ya haya enseñado el alias en ese nodo;
    si no, el evento se enrutaría por `PROVEEDOR_<authUserId>` y reaparecería el falso OFFLINE.

  **Decisión:** no se cambia código de validacion en esta fase (está fuera del foco "un solo socket + dispatcher"),
  pero el criterio de aceptación se reformula (CA2) para exigir sólo el caso canónico, y el subcaso `authUserId`
  se mueve a **deuda explícita**: cuando validacion tenga acceso al `proveedorId` (o cuando se comparta el mapa
  de alias por Redis entre nodos), emitirá/resolverá el canónico de forma incondicional. Se recomienda, como
  paso barato intermedio, que el servicio que invoca a validacion pase ya el `proveedorId` en `request.userId()`.

### Help-v2 (`findu/CORE/findu-help-v2`)

- `HttpRealtimeEventPublisher` emite `role + "_" + reqId` con `reqId = e.getRequesterUserId()` (fallback
  `e.getSenderId()`). **Análisis del productor (hallazgo #3):** verificado que la app
  (`ticket_detail_chat_screen._sendMessage`) postea a help-v2 con `senderId: proveedorId`, es decir, el
  mensaje del proveedor se registra con `senderId = proveedorId`, y el `requesterUserId` del ticket se deriva
  de ese mismo flujo de creación por `proveedorId`. Por tanto, para proveedores, **`reqId` es el `proveedorId`
  y el recipient `PROVEEDOR_<reqId>` ya es canónico** → CA2/CA5 se cumplen para help-v2 sin depender del alias.
  **Sin cambios de código en esta fase.** Se documenta como deuda menor únicamente endurecer esta invariante
  (asegurar que ninguna ruta de creación de ticket de proveedor use `authUserId` como `requesterUserId`).

### App (`findu-movil/apps/findu_proveedores`)

- **`lib/core/config/environment.dart`**: `webSocketUrlFor(int proveedorId, {token})` ya prefija
  `PROVEEDOR_`. Renombrar el parámetro a `proveedorId` (ya lo es semánticamente) y **documentar** que el
  argumento DEBE ser el `proveedorId`, nunca el `authUserId`. No cambia la firma pública.
- **`market_feed_screen.dart` `_initWebSocket`**:
  - Calcular `canonicalId = session.proveedorId ?? _profile?.id`. Si es `null`/`<=0`, **no** abrir socket
    (dejar en espera); el gancho existente `_loadActivationAndProfile()` reinvoca `_initWebSocket()` cuando
    el perfil llega. Eliminar la rama que usaba `userId`/`authUserId` como `primaryId`.
  - Eliminar por completo `_wsSecondaryClient`, `_wsSecondarySubscription`,
    `_wsSecondaryStatusSubscription`, `_secondaryWsTargetId`, `_secondaryWsConnected` y su bloque de
    conexión. `_updateWsConnectionState` pasa a depender sólo del canal primario.
- **`ticket_detail_chat_screen.dart` `_initWebSocket`**:
  - Usar `proveedorId` como único id; si es `null`/`<=0`, **no abrir** y dejar el socket pendiente.
  - El `senderId` de `sendMessage` ya usa `proveedorId`; queda consistente.
  - **Gancho de reconexión (corrige hallazgo #1 del review).** A diferencia de `market_feed_screen`, esta
    pantalla **no** tiene `_loadActivationAndProfile()` que reinvoque `_initWebSocket()`, y hoy nunca queda sin
    socket sólo porque cae al fallback literal `1` (`primaryId = userId>0 ? userId : (proveedorId>0 ? proveedorId : 1)`).
    Al eliminar ese fallback, si `proveedorId` es `null` en `initState` el chat quedaría sin socket para siempre.
    Para evitarlo, `_TicketDetailChatScreenState` se **suscribe a `SessionManager` (es `ChangeNotifier`)** y
    relanza la conexión cuando `proveedorId` pase a ser válido:
    ```dart
    @override
    void initState() {
      super.initState();
      // ... init existente ...
      SessionManager().addListener(_onSessionChanged);
      _initWebSocket(); // no-op si proveedorId aún es null/<=0
    }

    void _onSessionChanged() {
      final pid = SessionManager().proveedorId;
      if (_wsClient == null && pid != null && pid > 0) {
        _initWebSocket();
      }
    }

    @override
    void dispose() {
      SessionManager().removeListener(_onSessionChanged);
      // ... dispose existente ...
    }
    ```
    `_initWebSocket` se vuelve idempotente: si `_wsClient != null` o `proveedorId` no es válido, retorna sin
    abrir. Así el socket se abre exactamente una vez, en cuanto el perfil carga, sin depender de la navegación.
  - Eliminar `_wsSecondaryClient` y `_wsSecondarySubscription` y su bloque de conexión.
- No se toca `WebSocketClient` (`packages/network_ws`): heartbeat y reconexión intactos (RF6).

## Manejo de errores (por operación que puede fallar)

- **Handshake con `userId` ausente** (`JwtValidator`): ya lanza `IllegalArgumentException` →
  `onErrorResume` cierra con `CloseStatus.BAD_DATA`. Sin cambios. Recuperable por el cliente vía reconexión.
- **`RecipientResolver.toCanonical(null/blank)`**: devuelve el valor tal cual; el llamador
  (`EventDispatcherService.dispatch`) ya descarta `targetUserId` nulo/blank con `log.warn` y `Mono.empty()`.
  No fatal, evento descartado, logueado a WARN (como hoy).
- **Alias desconocido en handshake** (app vieja por `authUserId`, resolver aún sin mapeo): se registra bajo
  el String recibido; el evento de core que traiga el alias poblará el mapa y, a partir de ahí, el
  enrutamiento acierta. Degradación temporal controlada, no fatal. Log a INFO cuando se aprende un alias.
- **`learnAlias` con datos inconsistentes** (`alias == canonical`, blanks): no-op, sin log (ruido evitado).
- **Serialización del envelope** (`EventDispatcherService.serializeEnvelope`): ya envuelta en
  `Mono.fromCallable`; un fallo de Jackson propaga el error del `Mono` (como hoy). Fatal para ese evento,
  no para la sesión. Log vía el `doOnError` existente en `sendToUserLocally`.
- **Core `NotificationLambdaAdapter.send`**: ya es `@Async` y atrapa toda excepción con `log.warn`
  (no bloqueante). Añadir los campos de alias al `body` no cambia este contrato.
- **App: `proveedorId` nulo al conectar**: no es error, es estado de espera; se loguea a `debugPrint` y se
  reintenta cuando el perfil carga (gancho existente). Evita abrir un socket con id equivocado.
- **App: reconexión**: la gestiona `WebSocketClient` con backoff existente; no se altera.

## Validación de entradas externas

- **`?userId=` del handshake** (externo, no confiable): requerido, String. El `RecipientResolver` lo
  `trim()` y normaliza. No se valida la firma (fuera de alcance, consistente con hoy). Longitud/placeholder:
  si llega vacío, `JwtValidator` ya lo rechaza. **Parsing de la query (hallazgo #6):** hoy
  `JwtValidator.extractAndValidateUserId` parte cada par con `param.split("=")` y exige `pair.length == 2`,
  lo que descarta pares cuyo valor contiene `=` (p.ej. un JWT en `?token=`). No afecta a
  `userId=PROVEEDOR_<n>` (sin `=`), que es el único parámetro que esta fase usa, por lo que **se deja fuera
  de alcance** (consistente con "no validar firma JWT"). Si en una fase futura el handshake pasa a depender
  de `?token=`, el fix es trivial: `param.split("=", 2)`. Se anota como deuda para no asumir que `?token=`
  conviva con `?userId=` de forma robusta hoy.
- **`recipient`/`targetUserId` de `DispatchNotificationRequest`** (de productores internos): opcional entre
  `recipient` y `targetUserId` (ya resuelto por `effectiveRecipient()`); si ambos blank → HTTP 400 (como
  hoy). Tras resolver, se normaliza a canónico.
- **`canonicalRecipient`/`aliasRecipient` en `params`** (aditivos, de core): opcionales, String con prefijo
  `PROVEEDOR_`. Si faltan, el resolver simplemente no aprende nada nuevo (comportamiento base). Si vienen
  malformados (no prefijo `PROVEEDOR_`), `learnAlias` los ignora.
- **`proveedorId` en la app**: `int?`; sólo se abre socket si `> 0`. Nunca se usa `authUserId` como fuente
  del id de socket.

## Invariantes y quién los posee

- **INV1 — "Una sesión de proveedor se indexa siempre bajo su canónico"**: lo posee el dispatcher
  (`EventWebSocketHandler` + `RecipientResolver`), porque es el único punto por el que pasan todos los
  handshakes y tiene el mapa de alias. No puede vivir en la app (no conoce alias de otros) ni en los
  productores (no ven el handshake).
- **INV2 — "El productor de un evento de proveedor conoce el `proveedorId`"**: lo posee core (fuente de la
  equivalencia `authUserId↔proveedorId`). Por eso core es quien alimenta el alias; validacion/help-v2 se
  apoyan en el resolver.
- **INV3 — "La app abre como máximo un socket de eventos por pantalla"**: lo posee la capa de presentación
  de la app (`market_feed_screen`, `ticket_detail_chat_screen`), al eliminar el cliente secundario.

## Casos borde

- Proveedor con `authUserId == proveedorId`: el canónico es igual al alias; `learnAlias` es no-op y todo
  funciona con una sola clave.
- App que arranca sin `proveedorId` (perfil no cargado): no abre socket hasta que el perfil llega; no se
  cae a `authUserId`. Si el perfil nunca carga (usuario sin perfil completo), no hay socket, que es el
  comportamiento correcto (no hay proveedor a quien enrutar).
- Varios dispositivos del mismo proveedor: `LocalSessionRegistryAdapter` ya soporta `Set<WebSocketSession>`
  por clave; un solo id canónico multiplexa todos.
- Recipient de cliente por `username` o email: el resolver lo deja intacto (no empieza por `PROVEEDOR_`,
  no es numérico puro con intención de proveedor); se enruta como hoy.
- Clúster multi-nodo: el mapa `aliasToCanonical` es por nodo. Un alias aprendido en el nodo A no existe en
  el B hasta que el B procese un evento de core con el alias. Como core emite el canónico, el enrutamiento
  por canónico funciona en cualquier nodo; el alias sólo importa para apps viejas, caso transitorio. No se
  comparte el mapa por Redis en esta fase (deuda documentada).

## Testabilidad

- **Unitario (dispatcher)**: `RecipientResolver` — tabla de casos para `toCanonical` (numérico puro,
  `PROVEEDOR_x`, `CLIENTE_x`, `AGENTE_x`, `SOPORTE_GENERAL`, alias conocido, blank) y para `learnAlias`
  (incluye los no-op: `alias==canonical`, blanks, sin prefijo `PROVEEDOR_`). Cubre CA5 y CA6.
- **Unitario (core)**: `DispatcherNotificationObserver` — verificar que `sendToProvider` llama
  `notificationPort.send` **una** vez y que el payload incluye `canonicalRecipient`/`aliasRecipient` cuando
  `authUserId != id`. Cubre CA4.
- **Integración (dispatcher)**: test WebFlux que abre una sesión por `PROVEEDOR_<proveedorId>`, despacha un
  evento con ese `targetUserId` y verifica entrega local (no fallback). Para el subcaso "app vieja": abrir la
  sesión bajo `PROVEEDOR_<proveedorId>` (canónico), **aprender el alias** `PROVEEDOR_<authUserId>→PROVEEDOR_<proveedorId>`
  (vía `learnAlias`) y luego despachar un evento cuyo `targetUserId` sea `PROVEEDOR_<authUserId>`, verificando
  que el **enrutamiento** lo normaliza y entrega al socket canónico (no fallback). Esto refleja que la
  tolerancia a apps viejas vive en el enrutamiento, no en indexar la sesión dos veces (hallazgo #5).
  Añadir también un test de la sobrecarga `dispatch(EventEnvelope<?>)` que verifique que `canalId` se deriva
  del canónico y que un canal INACTIVO fuerza fallback FCM (hallazgo #2, cubre CA7 por el camino Rabbit).
  Cubre CA1, CA2, CA5, CA7.
- **App (widget/unit)**: `market_feed_screen` y `ticket_detail_chat_screen` — verificar que sólo se
  instancia un `WebSocketClient` y que no hay referencias a `_wsSecondaryClient`. Cubre CA3.
- **Difícil de testear**: el aprendizaje de alias multi-nodo (requiere clúster real); se cubre por
  inspección de diseño y se marca como deuda (compartir alias por Redis en una fase futura).

## Plan de verificación

- Backend: `./gradlew :findu-notification-dispatcher:test` y `:findu-core:test` (o el wrapper del repo)
  tras los cambios; añadir los tests nuevos antes de cerrar.
- App: `flutter analyze` + `flutter test` en `apps/findu_proveedores`.
- Smoke manual (local Docker): un proveedor con `authUserId != proveedorId`, un solo socket, disparar
  `NUEVA_SOLICITUD` desde core y confirmar en el log del dispatcher "socket local" y recepción en la app.

## Deuda técnica documentada (fases futuras, fuera de este alcance)

- Que validacion emita/resuelva el `proveedorId` canónico de forma directa (hoy condicionado al alias de
  core cuando `userId` fuese `authUserId`; help-v2 ya es canónico por construcción, ver hallazgo #3).
- Compartir `aliasToCanonical` entre nodos vía Redis para robustez multi-nodo con apps viejas.
- Validar la firma JWT en el handshake y, si se depende de `?token=`, usar `split("=", 2)` (hallazgo #6).
- Restringir el paso 6 de `toCanonical` a contexto confirmado de proveedor si algún día apareciera un
  productor que emita ids de cliente sin prefijo (hallazgo #7).
- Retirar el `RecipientResolver`/alias cuando todas las apps y productores emitan el canónico (simplificar a
  normalización de formato).

---

## Respuestas a los hallazgos del review (`design-review.json`, verdict `CHANGES_REQUESTED`)

- **#1 (HIGH) — chat de ticket sin gancho de reconexión:** ATENDIDO. Se añadió al diseño de
  `ticket_detail_chat_screen` la suscripción a `SessionManager` (`ChangeNotifier`) con `_onSessionChanged`,
  `_initWebSocket` idempotente y `removeListener` en `dispose`, de modo que el socket se abre en cuanto
  `proveedorId` es válido y nunca queda pendiente indefinidamente. (Opción (a) del review.)
- **#2 (MEDIUM) — CA7/RF5 no se cumplía por el camino RabbitMQ (`canalId == eventId`):** ATENDIDO. La
  sobrecarga `dispatch(EventEnvelope<?>)` ahora deriva `canalId = toCanonical(targetUserId)` con fallback a
  `eventId`, y el diseño declara que RF5/CA7 aplican también al camino Rabbit. CA7 incluido en el test de
  integración.
- **#3 (MEDIUM) — CA2/CA5 condicional para validacion/help-v2:** ATENDIDO. Se verificaron los productores:
  help-v2 emite `PROVEEDOR_<requesterUserId>` que es el `proveedorId` (la app postea `senderId: proveedorId`),
  luego es canónico → CA2/CA5 se cumplen sin alias. Validacion **no** pudo garantizarse canónica (su `userId`
  es un `Long` genérico), así que CA2 se reformuló al caso canónico y el subcaso `authUserId` de validacion se
  movió a deuda explícita. (Combinación de opciones (a) para help-v2 y (b) para validacion.)
- **#4 (MEDIUM) — extracción de `canonicalRecipient`/`aliasRecipient` no especificada:** ATENDIDO. Se definió
  `learnAliasFromPayload` con `instanceof Map` defensivo y lectura de ambos campos como `String`, ubicado en
  `dispatch(EventEnvelope<?>)` (Rabbit) y en `despacharEventoPresencia` (HTTP), con `learnAlias` idempotente.
- **#5 (MEDIUM) — doble indexación vs `unregisterSession` (sesiones huérfanas):** ATENDIDO. Se eliminó
  `indexKeysFor` y la doble indexación; la sesión se registra y se da de baja bajo **una sola** clave
  canónica. La tolerancia a apps viejas vive en la normalización del enrutamiento. (Opción de simplificación
  del review.)
- **#6 (NIT) — `JwtValidator` `split("=")`:** ATENDIDO como documentación. Se declaró fuera de alcance
  (consistente con no validar firma JWT), con el fix `split("=", 2)` anotado en deuda por si se depende de
  `?token=`.
- **#7 (NIT) — ambigüedad "numérico puro ⇒ proveedor":** ATENDIDO. Se justificó con la revisión de
  productores (ningún recipient de cliente llega como entero sin prefijo) y se anotó en deuda restringir el
  paso 6 si eso cambiara.
