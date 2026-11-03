# Puesta al día + Estado actual de WebSockets/Chat (FIND-U)

> Investigación **READ-ONLY**. No se modificó código, no se hizo commit, no se reinició ningún servidor.
> Repos: backend `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu` (rama `develop`), app Flutter
> `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu-movil\apps\findu_proveedores` (rama `master`).
> Nota: `findu-movil` NO es un repo git (su raíz no tiene commits); el repo real de la app es
> `apps/findu_proveedores`. El directorio `findu` y `findu-movil` cuelgan de un repo raíz
> `C:\Users\ADMIN\Documents\FIND-U-PROJECT` sin commits (solo untracked).

---

## RESUMEN EJECUTIVO (léelo primero)

### Objetivo A — ¿Qué cambió?

1. **Backend (`findu`, `develop`): 4 commits NUEVOS** por encima del último hash de referencia
   `5607777`. El más alto es `04fc79c`. En orden (más nuevo arriba):
   - `04fc79c` feat(backend): finanzas/movimientos/contrapropuestas de precio-hora + detalle con medio de cobro.
   - `d15370a` fix(transaction): dialecto Hibernate PostgreSQL explícito en perfil qa.
   - `50db9ac` feat(transaction): finanzas de proveedor, webhook Bre-B y `nameuser` en tickets.
   - `be233f2` feat(support): alinea `findu-help-v2`, dispatcher, processor y `frontend-soporte` para **chat de soporte en tiempo real + push FCM offline**.
   Además hay **muchísimos cambios SIN COMMITEAR** en `findu-core` (cancelaciones de proveedor,
   amonestaciones, auditoría de estado, cronograma de oferta por días/franjas, aproximación de duración,
   PIN de seguridad, `HelpServiceClient`). Ver §A.1.
2. **El backend de biometría pendiente SIGUE SIN IMPLEMENTARSE.** No existe `IdentityVerificationPort`,
   no existe nada "Idswyft", no hay `S3Client`/`software.amazon.awssdk` en `findu-core` (solo en
   `findu-help-v2`), y el transporte de imágenes **base64** no existe en los DTO del gateway
   (`OnboardingRequest`/`BiometriaData`) ni en `BiometricSubmitRequest` de security. Hoy la biometría
   sigue siendo: cliente manda flags + una `fotoFrontalUrl` (String URL), y el backend solo re-valida
   nombre/fecha contra core y confía el `faceMatched` del cliente. Ver §A.2.
3. **App Flutter (`findu_proveedores`, `master`): HEAD sigue en `7e03ccd`** (el mismo hash de
   referencia) **pero hay un volumen enorme de trabajo SIN COMMITEAR** encima. Lo relevante:
   - **Se agregó captura REAL de imágenes**: `image_picker: ^1.1.2` ahora está en `pubspec.yaml`
     (antes ausente) y está cableado de verdad en biometría (`biometric_verification_screen.dart`,
     `register_wizard_screen.dart` vía `biometric_capture_flow.dart`) **y** en soporte/chat
     (`ticket_detail_chat_screen.dart`, `create_ticket_screen.dart`). El modo biométrico por defecto
     pasó a `mock` pero "mock" ahora significa **usar la imagen real capturada** sin modelos nativos
     (no el nombre hardcodeado anterior). Ver §A.3.
   - **Se añadió el directorio `android/` completo** (untracked) con permisos CAMERA, READ_MEDIA_IMAGES,
     READ_EXTERNAL_STORAGE (soporte a `image_picker`). Ver §A.4.
   - **NO hay evidencia de un fix de arranque/crash explícito.** No se encontró firebase ni cambios
     anti-crash específicos; `main.dart` solo cambió para cablear `SupportNotificationHandler` + un
     `navigatorKey` global. El crash reportado en el dispositivo sigue sin una corrección rastreable.
   - `USE_ONBOARDING_SINGLE_REQUEST` cambió su default a **`true`** (antes `false`).
4. **Docs nuevos**: en `findu-movil/baseDesing/` aparecieron
   `INTEGRACION_FINANCIERA_Y_PROPUESTA_MAPA.md`, `FINANZAS_API_INTEGRACION.md`,
   `RESUMEN_APIS_PROVEEDOR.md`, `CONSUMO_SERVICIOS_SOPORTE_TICKETS.md` (todos posteriores a
   `PROMPT_Y_RESUMEN_PROVEEDOR.md`). No hay docs nuevos de biometría/Idswyft/S3. El informe previo
   `findu/.agents/tasks/biometria-idswyft-estado-actual.md` sigue siendo la referencia del estado
   biométrico. Ver §A.5.

### Objetivo B — Estado de WebSockets/Chat (para diseñar ciclo de vida por canal)

1. **El socket vive atado a la pantalla, NO al ciclo de vida de la app.** No existe
   `WidgetsBindingObserver`/`AppLifecycleState` en NINGÚN `.dart` del proyecto. El socket se abre en
   `initState` del Home (`market_feed_screen.dart`) y del chat de ticket, y se cierra en `dispose`.
   Nada reacciona a foreground/background. Hueco #1 para el estilo WhatsApp.
2. **NO hay registro de token FCM en la app ni envío real de push.** No existe `firebase_messaging`,
   ni `FirebaseMessaging`, ni `getToken`, ni `onBackgroundMessage` en toda la app (ni en `pubspec`,
   ni en `android/`). El `FirebasePushSender` del backend es un **stub con TODO** (loguea, no envía).
   El `SupportNotificationHandler` de la app solo tiene un `handlePayload` que **nadie invoca desde un
   receiver real de FCM**. Hueco #2.
3. **El dispatcher NO distingue foreground vs background.** Decide socket-local / cluster-redis /
   fallback-FCM únicamente por presencia de sesión WS registrada, más un estado explícito
   ACTIVO/INACTIVO del canal. No hay noción de "app en primer plano". Hueco #3.
4. **La app abre VARIOS sockets a la vez** (no uno): el Home abre 1–2 (primario por `userId`/authUserId
   y secundario por `proveedorId` si difieren) y el chat de ticket abre otros 1–2. Todos apuntan al
   mismo `/ws/events` pero con distinto `userId=PROVEEDOR_<id>`. Ver §B.5.
5. **El "bug OFFLINE pese a sesión registrada" tiene causa estructural y probablemente SIGUE.** El
   registro de sesión se indexa por el `userId` del handshake (`?userId=PROVEEDOR_<proveedorId>`),
   pero el enrutamiento usa `envelope.targetUserId()` (el `recipient`). Si el productor emite un
   `targetUserId` que no es EXACTAMENTE el mismo string con el que la app abrió el socket
   (p.ej. `PROVEEDOR_<proveedorId>` vs id numérico, o authUserId vs proveedorId), `hasLocalSession`
   devuelve false → "OFFLINE" y cae a FCM. La app mitiga abriendo **dos** sockets (authUserId y
   proveedorId) precisamente por esto. Ver §B.6.
6. **Chat: solo existe el chat de SOPORTE (tickets), no un chat cliente↔proveedor.** El `TipoCanal`
   define `CANAL_1_OFERTAS`, `CANAL_2_CHAT_PROVEEDOR`, `CANAL_3_SOPORTE`, **pero `CANAL_2` y `CANAL_3`
   NUNCA se instancian en código**: todo canal se abre como `CANAL_1_OFERTAS`. El chat de soporte va
   por WS (dispatcher) + polling de respaldo + `help-v2` como fuente de historial. Ver §B.2 y §B.7.

---

# OBJETIVO A — Puesta al día (evidencia)

## A.1 Git: commits recientes, status y NUEVOS respecto a la referencia

### Backend `findu` (rama `develop`)

`git log --oneline` (cabecera):
```
04fc79c (HEAD -> develop, origin/develop) feat(backend): finanzas, movimientos, contrapropuestas de precio/hora y detalle con medio de cobro
d15370a fix(transaction): add explicit hibernate postgresql dialect in qa profile
50db9ac feat(transaction): add provider finance, Bre-B webhook, and ticket nameuser support
be233f2 feat(support): align findu-help-v2, notification-dispatcher, processor and frontend-soporte for real-time support chat and offline FCM push
5607777 fix(notifications): processor ignora __TypeId__ ... alert.resolved    <-- REFERENCIA
e467e33 feat(security): cablear desactivacion de alertas email y biometria ...
45392be ... 11c6bd1 ... bac3d8e ... f77dfe3 ...
```

`git log 5607777..develop` → **4 commits nuevos**: `be233f2`, `50db9ac`, `d15370a`, `04fc79c`.
Temática: chat de soporte en tiempo real + fallback FCM (`be233f2`), y un bloque grande de
**finanzas de proveedor / Bre-B / contrapropuestas de precio-hora** (`50db9ac`, `d15370a`, `04fc79c`).
Ninguno toca biometría/Idswyft/S3-core.

`git status` (resumen) — hay **cambios sin commitear sustanciales**, sobre todo en `findu-core`:
- Modificados (M): `OfertaController`, `SolicitudController`, `GestionOfertasUseCaseImpl`,
  `GestionSolicitudesUseCaseImpl`, `OfertaServiceImpl`, modelos `Oferta`/`SolicitudServicio`,
  observers (`SolicitudEventPublisher`, `DispatcherNotificationObserver`), `NotificationTemplates`,
  entidades/mappers/repos de oferta y solicitud, `schema.sql` (h2 y postgres), y un test.
- Nuevos sin trackear (??): toda una feature de **cancelaciones de proveedor** y **amonestaciones**:
  `GestionCancelacionesProveedorUseCase(+Impl)`, `CancelacionProveedorController`,
  modelos `CancelacionProveedor`, `AmonestacionProveedor`, `SolicitudEstadoAuditoria`,
  `OfertaCronogramaDia`; enums `CategoriaCancelacionProveedor`, `NivelAmonestacion`,
  `UnidadDuracionOferta`; `PinSeguridadHelper`, `SolicitudEstadoAuditoriaService`,
  `HelpServiceClient`, y sus entidades/repos; tests `GestionCancelacionesYPinesTest`.

> Lectura: el usuario avanzó fuertemente en **finanzas (ya commiteado)** y en
> **cancelaciones/amonestaciones/auditoría de oferta (aún sin commitear)**. Nada de biometría backend.

### App Flutter `findu_proveedores` (rama `master`)

`git log --oneline` cabecera: HEAD = **`7e03ccd`** (idéntico a la referencia; **no hay commits nuevos**).
Pero `git status` muestra un working tree gigante sin commitear:
- **Modificados (M)**: `environment.dart`, `session_manager.dart`, `main.dart`,
  `biometric_verification_screen.dart`, `login_screen.dart`, `register_wizard_screen.dart`,
  `market_feed_screen.dart`, `profile_api_adapter.dart`, `provider_profile_screen.dart`,
  `support_tickets_screen.dart`, `active_jobs_screen.dart`, `finances_screen.dart`,
  `pubspec.yaml`, `pubspec.lock`, `environment_flag_test.dart`.
- **Nuevos (??)**: directorios completos `android/`, `ios/`, `assets/`, `README.md`, `.metadata`,
  `analysis_options.yaml`, features nuevas de `finances/` (Bre-B, transferencia, historial de
  movimientos, selección de medio de pago, detalle financiero), `support/` (crear ticket, chat de
  ticket, categorías), `market/` (contraoferta, wizard días/duración), `profile/` (coberturas,
  portafolio, specialties, info personal), `shell/`, `active_jobs/`, `core/location/`, y varios
  `.agents/tasks/*` y tests nuevos.

> Lectura: TODO el frontend avanzado está **sin commitear**. El commit `7e03ccd` ("captura real de
> documento y rostro ... Fase 1") existe pero la implementación efectiva de la captura real vive en el
> working tree (ver §A.3). Riesgo: una pérdida de working tree borraría semanas de trabajo no commiteado
> en ambos repos.

## A.2 Backend de biometría / Idswyft / S3 / base64 — SIGUE SIN EXISTIR

Búsquedas en `develop` (código fuente, excluyendo docs):
- `IdentityVerificationPort`: **0** en código fuente (solo aparece citado en el doc
  `.agents/tasks/biometria-idswyft-estado-actual.md`).
- `Idswyft`: **0** en todo `.java/.yml/.gradle`.
- `software.amazon.awssdk` / `S3Client` / `S3AsyncClient` en `findu-core`: **0**.
  (El único S3 real del repo sigue en `findu-help-v2`:
  `CORE/findu-help-v2/.../infrastructure/adapter/out/storage/S3FileStorageAdapter.java`.)

DTOs del gateway (sin campos base64):
- `ARCHITECTURE/findu-api-gateway/src/main/java/com/findu/gateway/dto/BiometriaData.java`:
  `record BiometriaData(Boolean intentada, Boolean aprobada, Double faceMatchScore,
  String fotoFrontalUrl, String cedulaNombre, String cedulaFechaNacimiento)`. **Sin documento/selfie/base64.**
- `.../dto/OnboardingRequest.java`: incluye `BiometriaData biometria` pero **ningún campo de imagen**.
- Security `SECURITY/findu-spring-security/.../dto/request/BiometricSubmitRequest.java`:
  `cedulaNombre, cedulaFechaNacimiento, faceMatchScore (BigDecimal), faceMatched (Boolean),
  fotoFrontalUrl (String URL)`. El javadoc dice explícitamente: el face match se resuelve en cliente
  (TFLite) y el backend re-valida nombre (sin tildes) y fecha contra `findu-core`.

Core storage sigue en stub:
- Puerto `CORE/findu-core/.../application/port/out/externalapi/StorageServicePort.java`.
- Única impl: `CORE/findu-core/.../infrastructure/adapter/out/externalapi/StorageServiceStubAdapter.java`
  (devuelve el base64 de entrada; no sube nada).

**Conclusión A.2:** el trabajo pendiente del backend biométrico (puerto Idswyft MOCK, S3 real en core con
estructura `usuarios/proveedores/{TIPODOC+NUM}/biometricos/{documento,rostro}` y `/perfil`, carpetas
pending/verified con expiración 1 día, transporte base64 en el request único, foto de perfil atada a
biometría write-once + re-validación) **no se empezó**. El detalle exhaustivo del estado base está en
`findu/.agents/tasks/biometria-idswyft-estado-actual.md` (sigue vigente, salvo el punto de captura real,
ver §A.3).

## A.3 App: captura real de imágenes (NUEVO, sin commitear) y modo biométrico

- `pubspec.yaml` ahora declara **`image_picker: ^1.1.2`** (antes ausente). `diff HEAD -- pubspec.yaml`
  confirma 8 líneas añadidas.
- `image_picker` está **realmente usado** (no fabricado):
  - `lib/features/auth_onboarding/presentation/components/biometric_capture_flow.dart`:
    `typedef BiometricImagePickerCallback` + `pickImageWithSourceSheet(...)` con `ImagePicker().pickImage`.
  - `biometric_verification_screen.dart`: `_pickImage(ImageSource, {useFrontCamera})` →
    `picker.pickImage(source:..., preferredCameraDevice: front/rear)`; usado para cédula frente/reverso y rostro.
  - `register_wizard_screen.dart`: `_pickDocument` y foto de perfil ahora llaman a
    `BiometricCaptureFlow.pickImageWithSourceSheet(...)` (ya NO el `Future.delayed` con nombre ficticio).
  - Soporte: `ticket_detail_chat_screen.dart` y `create_ticket_screen.dart` usan
    `_picker.pickImage(source: camera|gallery)` para adjuntos.
- `environment.dart`: `biometricMode` default `mock`, pero el comentario aclara que "mock" ahora
  **usa la imagen real capturada/elegida** sin modelos nativos (apto dev/CI/emulador);
  `--dart-define=BIOMETRIC_MODE=real` activa ML Kit+TFLite en dispositivo físico arm64.
- `environment.dart`: `useOnboardingSingleRequest` default pasó a **`true`** (antes `false`).
- Los diffs del working tree son grandes: `biometric_verification_screen.dart` +1798/-…,
  `register_wizard_screen.dart` ~940 líneas tocadas.

> Esto ES el cambio que el commit `7e03ccd` anunciaba como "captura real ... Fase 1", pero la
> implementación efectiva está en el working tree sin commitear.

## A.4 App: arranque/crash, permisos Android

- `android/` entero es **untracked** (recién agregado). `AndroidManifest.xml`
  (`android/app/src/main/AndroidManifest.xml`) declara:
  `INTERNET`, `ACCESS_FINE/COARSE_LOCATION`, **`CAMERA`**, **`READ_MEDIA_IMAGES`**,
  **`READ_EXTERNAL_STORAGE` (maxSdkVersion=32)**, `uses-feature camera (required=false)`.
  App label `AChambear`, `usesCleartextTraffic="true"`, `networkSecurityConfig` presente.
- **Crash de arranque**: no se encontró un fix específico rastreable. No hay firebase (que a veces
  causa crash por `google-services.json` faltante). El único cambio de `main.dart`
  (`diff HEAD -- lib/main.dart`, +6 líneas) es cablear `SupportNotificationHandler.navigatorKey` y
  exponer un `navigatorKey` global en `MaterialApp`; nada anti-crash. Dado que el usuario confirmó
  "la app no arrancó en mi cel pero déjalo así", **el crash sigue sin corrección confirmada**. Los
  permisos y `image_picker` están bien declarados, así que el crash probablemente NO es por manifest
  de permisos; habría que capturar el logcat real para diagnosticar.

## A.5 Documentos nuevos

- `findu-movil/baseDesing/` (orden por fecha desc):
  `INTEGRACION_FINANCIERA_Y_PROPUESTA_MAPA.md`, `FINANZAS_API_INTEGRACION.md`,
  `RESUMEN_APIS_PROVEEDOR.md`, `CONSUMO_SERVICIOS_SOPORTE_TICKETS.md`
  (todos posteriores a `PROMPT_Y_RESUMEN_PROVEEDOR.md`, 2026-10-11, que el usuario tiene abierto).
  Más antiguos: `PREP_ONBOARDING_REQUEST_UNICO.md`, `DESIGN.md`.
- `DOCUMENTACION_Y_DIAGRAMAS/` vive en la **raíz** `C:\Users\ADMIN\Documents\FIND-U-PROJECT\`
  (no dentro de `findu`). No se detectaron docs nuevos de biometría/Idswyft/S3/WebSocket ahí.
- No hay documento nuevo que mencione Idswyft, verificador externo real, ni la estructura S3 pedida.

---

# OBJETIVO B — Estado de WebSockets y Chat

## B.1 Arquitectura WS del dispatcher (`findu/CORE/LAMBDA/findu-notification-dispatcher`)

Flujo y clases exactas:

- **Endpoint**: `WebSocketConfig.java` registra el handler en `${findu.websocket.endpoint:/ws/events}`.
  Reactivo (Spring WebFlux `WebSocketHandler`), no STOMP.
- **Handshake/auth**: `EventWebSocketHandler.handle(session)` llama
  `JwtValidator.extractAndValidateUserId(uri)`. **Importante**: si la query trae `userId=...` lo usa
  **tal cual sin validar JWT**; solo si no hay `userId` intenta parsear `token=<JWT>` (claim `userId`/`sub`).
  La app hoy conecta con `?userId=PROVEEDOR_<id>` (sin token) → se acepta sin verificación de firma.
- **Registro de sesión**: `sessionRegistry.registerSession(userId, session)` →
  `LocalSessionRegistryAdapter` (mapa `ConcurrentHashMap<String, Set<WebSocketSession>>` indexado por
  el `userId` del handshake). Además `redisPubSubPort.registerPresence(userId, nodeId)` marca presencia
  en el clúster.
- **Heartbeat**: PING estructurado cada `${findu.websocket.ping-interval-seconds:30}`s
  (`EventEnvelope.ping()`), el cliente responde PONG.
- **Cierre**: `doFinally` → `unregisterSession(userId, sessionId)` + `removePresence`.

### Decisión socket-local vs cluster-redis vs fallback-FCM — `EventDispatcherService`

`dispatch(canalId, envelope, tipoEvento)`:
1. Toma `targetUserId = envelope.targetUserId()` (si null/blank → descarta).
2. Crea `EventoCanal` y persiste vía `gestionCanalUseCase.agregarEvento(canalId, ...)`; si el canal no
   existe, lo **abre automáticamente** como `TipoCanal.CANAL_1_OFERTAS` con un `Interoperante(targetUserId,"USER")`.
3. Llama `procesarEntrega(targetUserId, envelope, canalInactivo)`:
   - **Si el canal está `INACTIVO`** (dispositivo "bloqueado") → **SIEMPRE FCM** (`eventFallbackPort.sendToFallbackQueue`).
   - **Si `sessionRegistry.hasLocalSession(targetUserId)`** → WebSocket local (`sendToUserLocally`).
   - **Si no, `redisPubSubPort.isUserOnlineInCluster(targetUserId)`** → Redis Pub/Sub al nodo correcto.
   - **Si offline** → cola fallback RabbitMQ → `notification-processor` → FCM.

Entrada de eventos: `RabbitEventConsumer` (`@RabbitListener` cola `findu.dispatcher.events.queue`) y
`NotificationDispatchController` (`POST /dispatchNotification` y `/api/v1/canales/dispatchNotification`).
El controller decide `canalId` por prefijo del `recipient` (`PROVEEDOR_`, `CLIENTE_`, `AGENTE_`,
`SOPORTE_`; por defecto `CLIENTE_<recipient>`) y mapea `type` → `EventType`
(CHAT/MESSAGE→CHAT_MESSAGE, OFFER/OFERTA→OFFER_RECEIVED, ALERT→SYSTEM_ALERT, resto→SERVICE_UPDATE).

## B.2 Inventario de canales — `TipoCanal` (dispatcher `domain/model/TipoCanal.java`)

| Valor | Propósito declarado | ¿Se instancia hoy? | Evidencia |
|---|---|---|---|
| `CANAL_1_OFERTAS` | Canal principal de ofertas/eventos del proveedor | **SÍ, siempre** | `EventDispatcherService.dispatch` (apertura auto) y `ChannelController.actualizarDisponibilidadProveedor` abren todo como `CANAL_1_OFERTAS` |
| `CANAL_2_CHAT_PROVEEDOR` | Chat del proveedor (cliente↔proveedor) | **NO** (0 usos en código) | grep `CANAL_2_CHAT_PROVEEDOR` → sin resultados fuera del enum |
| `CANAL_3_SOPORTE` | Chat/canal de soporte | **NO** (0 usos en código) | grep `CANAL_3_SOPORTE` → sin resultados fuera del enum |

> En la práctica **existe un único tipo de canal operativo (`CANAL_1_OFERTAS`)** que multiplexa TODOS
> los eventos vía el `EventType` del `EventEnvelope`. Los otros dos valores son placeholders de diseño.
> El repositorio de canales es **in-memory** (`InMemoryChannelRepositoryAdapter`, `ConcurrentHashMap`),
> no Mongo/DynamoDB todavía (el javadoc lo anticipa).

### Estados del canal — `EstadoCanal`
`ACTIVO`, `INACTIVO`, `CERRADO`. Gobernados por:
- **Apertura**: `GestionCanalUseCaseImpl.abrirCanal(...)` lo deja `ACTIVO`.
- **INACTIVO**: lo fija `ChannelController.PUT /api/v1/canales/proveedor/disponibilidad`
  (`{proveedorId, disponible}` → `disponible=false` ⇒ INACTIVO, `canalId="PROVEEDOR_"+proveedorId`),
  invocado **desde `findu-core`** cuando el proveedor se marca no disponible.
- **Reactivación**: `POST /api/v1/canales/reactivar` (`{username, rol}`) pone ACTIVO todos los
  INACTIVOS del usuario (al login / desbloqueo).
- **CERRADO**: `agregarEvento` rechaza eventos en canales CERRADO. No se encontró ninguna ruta que
  setee CERRADO explícitamente (solo se defiende contra él).

> **No existe scheduler real.** El comentario "dispositivo bloqueado por scheduler" en
> `EventDispatcherService.procesarEntrega` es solo un comentario. No hay `@Scheduled` ni
> `@EnableScheduling` en el dispatcher. INACTIVO depende del PUT explícito desde core, no de un job.

## B.3 Inventario de eventos — `EventType` (`domain/model/EventType.java`)

| EventType | Productor (hoy) | Consumidor | Notas |
|---|---|---|---|
| `OFFER_RECEIVED` | `NotificationDispatchController` (type OFFER/OFERTA); observers de core (`SolicitudEventPublisher`/`DispatcherNotificationObserver`) vía dispatch | App (`_handleWebSocketEvent`) | Evento de negocio |
| `CHAT_MESSAGE` | Chat de **soporte** (help-v2 `HttpRealtimeEventPublisher` → dispatcher) y controller (type CHAT/MESSAGE) | App chat de ticket + fallback FCM | Único "chat" real hoy = soporte |
| `SERVICE_UPDATE` | Default del controller cuando no hay match | App | Estado de servicio genérico |
| `SYSTEM_ALERT` | `findu-validacion` → `notification-processor` → dispatcher (canal de alertas en vivo, commit `bac3d8e`) | App carrusel de alertas del Home | Alertas de validación/perfil |
| `PING` / `PONG` | Dispatcher (PING) / cliente (PONG) y viceversa | Heartbeat | No es dato de negocio |

`TipoEventoCanal` (para el historial del canal): `MENSAJE`, `OFERTA`, `EVENTO_SISTEMA`
(`CHAT_MESSAGE→MENSAJE`, `OFFER_RECEIVED→OFERTA`, resto→`EVENTO_SISTEMA`).

`EventEnvelope<T>` (`domain/model/EventEnvelope.java`): `{eventType, eventId, timestamp, targetUserId,
payload}`, `@JsonInclude(NON_NULL)`. El cliente Dart lo decodifica en `packages/network_ws` (`EventEnvelope.fromJson`).

## B.4 Clasificación por canal/evento (insumo para la estrategia de ciclo de vida)

| Evento/canal | Criticidad (¿debe llegar en background?) | Temporalidad | Qué gobierna su ciclo de vida hoy | Estrategia sugerida (WhatsApp-style) |
|---|---|---|---|---|
| `CHAT_MESSAGE` (soporte, y futuro chat cliente↔proveedor) | **Alta** — sí en background | Tiempo real | Pantalla de chat abierta (WS en `initState`/`dispose`) + polling de respaldo | WS vivo en foreground; **push FCM** en background (hoy NO existe FCM real) |
| `OFFER_RECEIVED` | Media-alta — probablemente sí | Casi tiempo real | Home abierto + toggle disponible/INACTIVO | WS en foreground; **push FCM** o pull al abrir si llegó offline |
| `SYSTEM_ALERT` | Baja-media — diferible | Diferible | Home abierto + **PULL** al entrar (ya implementado, commit `4775d6d`/`30b25b8`) | **Pull** al foreground (ya es la fuente de verdad); push opcional |
| `SERVICE_UPDATE` | Media — según urgencia | Casi tiempo real | Home/pantalla abierta | WS en foreground; pull al reabrir |
| Presencia/disponibilidad (toggle) | N/A (control) | Tiempo real | Estado explícito del usuario (toggle) → PUT disponibilidad (ACTIVO/INACTIVO) | Mantener; es la señal que hoy fuerza FCM cuando INACTIVO |
| `PING`/`PONG` | N/A | Tiempo real | Heartbeat del WS | Mantener |

## B.5 App Flutter — cliente WS y ciclo de vida

### `packages/network_ws/lib/src/web_socket_client.dart` (`WebSocketClient`)
- `connect([url])`, `disconnect()`, `dispose()`, `send(data)`.
- Streams: `events` (`Stream<EventEnvelope>`), `connectionStatus` (`Stream<bool>`), `isConnected`.
- **Auto-reconexión** con backoff `(_reconnectAttempts*3).clamp(3,20)`s mientras `_shouldReconnect`.
- Heartbeat: responde `ping`/`PING` texto y PING estructurado (`{eventType:PONG}`).
- `disconnect()` apaga la reconexión; `dispose()` cierra los StreamControllers.

### `market_feed_screen.dart` (Home) — apertura/cierre
- `initState` → `_initWebSocket()`; `dispose` → `_closeWebSocket(fromDispose:true)`.
- `_onSessionChanged`: si no autenticado → `_closeWebSocket()`; si autenticado → `_initWebSocket()`.
- `_initWebSocket()` abre **socket primario** por `primaryId = userId(authUserId) || proveedorId`
  (`Environment.webSocketUrlFor(primaryId)`), y **si `proveedorId != primaryId` abre un socket
  SECUNDARIO** por `proveedorId`. Es decir, hasta **2 sockets simultáneos** desde el Home.
- Botón refresh y `RefreshIndicator`: reconectan si `!_isWsConnected`.
- **No hay `WidgetsBindingObserver`/`AppLifecycleState`** → el socket NO se cierra al pasar a
  background ni se reabre al volver a foreground; depende de que la pantalla siga montada y de la
  reconexión automática.

### `ticket_detail_chat_screen.dart` (chat soporte) — apertura/cierre
- Abre su propio `WebSocketClient` primario (por `primaryId`, con `token`) y un secundario si
  `proveedorId` difiere → **otros 1–2 sockets**. `dispose` los cierra. Además corre un
  `_pollingTimer` como respaldo (pull del historial de help-v2). Sin observer de ciclo de vida.

### URL de conexión — `environment.dart`
- `webSocketUrlFor(proveedorId, {token})`:
  local `ws://<host>:9000/ws/events?userId=PROVEEDOR_<proveedorId>`;
  prod `wss://<base>/ws/events?userId=PROVEEDOR_<proveedorId>`; añade `&token=` si se pasa.
  **Siempre prefija `PROVEEDOR_`** al id que reciba (sea authUserId o proveedorId).

### FCM / push en la app
- **No existe** `firebase_messaging`/`FirebaseMessaging`/`getToken`/`onBackgroundMessage` en la app
  (ni en `pubspec.lock` ni en `android/`). `SupportNotificationHandler`
  (`lib/features/support/infrastructure/support_notification_handler.dart`) solo define
  `handlePayload(data)` que navega al chat de ticket **si alguien lo llamara** — pero no hay receiver
  FCM que lo invoque. Es andamiaje para una integración futura, no FCM operativo.

## B.6 El bug "PROVEEDOR_x OFFLINE pese a sesión registrada"

Mecánica confirmada por código:
- La app registra la sesión bajo la **clave exacta** del handshake: `userId=PROVEEDOR_<proveedorId>`
  (o `PROVEEDOR_<authUserId>`), guardada en `LocalSessionRegistryAdapter` indexada por ese String.
- El enrutamiento en `EventDispatcherService.procesarEntrega` usa `envelope.targetUserId()`
  (el `recipient` que puso el productor). `hasLocalSession(targetUserId)` es una comparación de String
  **exacta**. Si el productor emite un `targetUserId` distinto del String con el que la app abrió el
  socket (p.ej. id numérico puro vs `PROVEEDOR_<id>`, o `authUserId` vs `proveedorId`), la sesión
  figura registrada pero NO se encuentra → log "… OFFLINE" → cae a FCM.
- La app **mitiga** abriendo dos sockets (primario authUserId + secundario proveedorId) para cubrir
  ambos identificadores. Esto sugiere que el bug se vivió y se parcheó del lado cliente, pero la causa
  raíz (acuerdo único de identidad entre productor y handshake) **sigue en el backend**.

Recomendación (no implementada): fijar un **identificador canónico único** de destinatario (p.ej.
siempre `PROVEEDOR_<proveedorId>` o siempre el `authUserId`) y usarlo de forma consistente en:
el `targetUserId` que emiten core/validacion/help-v2, el `recipient`/prefijo del controller, y el
`?userId=` del handshake. Así un solo socket basta y se elimina el falso OFFLINE.

## B.7 Chat: qué hay y qué no

- **Backend**: no hay micro de chat dedicado. El chat que existe es el **chat de tickets de soporte**,
  distribuido entre:
  - `findu-help-v2`: historial y eventos de ticket (`GetTicketChatHistoryUseCase`,
    `TicketMessageAddedEvent`, `HttpRealtimeEventPublisher` → publica al dispatcher), commit `be233f2`.
  - `findu-notification-dispatcher`: entrega en vivo (`CHAT_MESSAGE`) por WS o fallback.
  - `findu-notification-processor`: `NotificationConsumer.handleFallback` convierte el evento offline
    en push FCM (`type: TICKET_CHAT`, `ticketCode`) — pero el `FirebasePushSender` es un **stub TODO**
    que solo loguea (`[FCM] Push enviado ...`), no envía de verdad.
- **No existe** chat **cliente↔proveedor** (negocio). `CANAL_2_CHAT_PROVEEDOR` está declarado pero sin uso.
- **App**: chat de soporte completo (`ticket_detail_chat_screen.dart`) con WS + polling + adjuntos por
  `image_picker`. `support_tickets_screen.dart`, `create_ticket_screen.dart`,
  `support_category_problems_screen.dart`. No hay UI de chat de negocio cliente↔proveedor.

## B.8 Flujo resumido EventEnvelope SYSTEM_ALERT y fallback FCM

```
findu-validacion (motor de alertas, commit f77dfe3/11c6bd1)
   └─(evento alerta)→ findu-notification-processor (consumer) 
        └─(POST /dispatchNotification type=ALERT)→ findu-notification-dispatcher
             └─ NotificationDispatchController → EventEnvelope(SYSTEM_ALERT, targetUserId, payload)
                  └─ EventDispatcherService.dispatch(canalId, env, EVENTO_SISTEMA)
                       1) canal INACTIVO → FCM (fallback queue)
                       2) hasLocalSession → WebSocket local → app (market_feed carrusel)
                       3) online en clúster → Redis Pub/Sub → nodo con el socket
                       4) OFFLINE → RabbitMQ fallback → notification-processor → FirebasePushSender (STUB)
```
Para alertas, la app además hace **PULL** al entrar al Home (fuente de verdad, commits `4775d6d`,
`30b25b8`), así que el WS de alertas es complementario, no la única vía.

---

# HUECOS para "abrir socket en foreground / FCM en background" (estilo WhatsApp)

1. **La app no observa el ciclo de vida.** No hay `WidgetsBindingObserver`/`AppLifecycleState` en
   ningún `.dart`. El socket vive atado a `initState`/`dispose` de pantallas concretas, no al
   foreground/background de la app. → Falta un observador global que cierre/abra sockets al cambiar
   `AppLifecycleState` (resumed/paused).
2. **No hay FCM real.** No hay `firebase_messaging` en la app, ni registro de token, ni
   `onBackgroundMessage`; el `FirebasePushSender` del backend es un stub TODO y no hay almacenamiento
   de device tokens por usuario. → Sin esto, "pasar a FCM en background" no es posible todavía.
3. **El dispatcher no distingue foreground/background.** Solo conoce "sesión WS presente" y el estado
   ACTIVO/INACTIVO (que hoy solo lo mueve un PUT manual desde core, no un scheduler). → Falta señal de
   presencia de app (p.ej. que el cierre del socket al ir a background marque "usa FCM" de inmediato).
4. **Identidad de destinatario no canónica** (ver §B.6): provoca falso OFFLINE y obliga a abrir varios
   sockets. → Unificar el identificador antes de optimizar el ciclo de vida.
5. **Múltiples sockets por pantalla** (Home 1–2, chat 1–2). → La estrategia WhatsApp pide **un** socket
   multiplexado por app (el `EventEnvelope` ya está diseñado para multiplexar por `eventType`); hoy se
   abren varios por el punto 4 y por abrir por pantalla.
6. **`CANAL_2`/`CANAL_3` sin implementar** y repo de canales in-memory → no hay separación real por
   tipo de canal todavía; toda la clasificación por canal del diseño objetivo habría que construirla.

---

# RECOMENDACIONES (no implementadas)

**Objetivo A**
- **Commitear el trabajo pendiente YA** en ambos repos (core: cancelaciones/amonestaciones/auditoría;
  app: finanzas/soporte/market/perfil + `android/` + `image_picker`). Hoy semanas de trabajo viven solo
  en el working tree; una limpieza accidental las borraría.
- Si se retoma el backend biométrico, partir del plan vigente en
  `.agents/tasks/biometria-idswyft-estado-actual.md` (puerto `IdentityVerificationPort` MOCK en core,
  S3 real reutilizando el patrón de `help-v2`, base64 en el request único, lifecycle S3 pending/verified
  1 día, foto de perfil write-once + re-validación).
- Para el crash de arranque en dispositivo: capturar `adb logcat` real; no hay fix rastreable y el
  manifest/permresos/`image_picker` parecen correctos, así que el diagnóstico necesita el stacktrace.

**Objetivo B (estrategia de ciclo de vida por canal)**
- Introducir un `WidgetsBindingObserver` global (en el shell o `main`) que: en `resumed` abra/mantenga
  **un** socket multiplexado; en `paused` lo cierre y active la vía FCM.
- Integrar `firebase_messaging` en la app (token por usuario → persistir en backend) e implementar el
  `FirebasePushSender` real (Firebase Admin SDK) para que el fallback offline funcione de verdad.
- Canonicalizar el `targetUserId` end-to-end para eliminar el falso OFFLINE y pasar a un solo socket.
- Clasificación por canal (ver §B.4): chat = WS+FCM; ofertas = WS+FCM/pull; alertas = pull; presencia =
  toggle. Implementar realmente `CANAL_2_CHAT_PROVEEDOR` si se quiere chat de negocio, y mover el repo
  de canales de in-memory a Mongo/DynamoDB para persistencia multi-nodo.

---

## Apéndice — archivos clave (rutas absolutas)

Backend dispatcher:
- `...\findu\CORE\LAMBDA\findu-notification-dispatcher\src\main\java\com\findu\dispatcher\infrastructure\websocket\EventWebSocketHandler.java`
- `...\dispatcher\service\EventDispatcherService.java`
- `...\dispatcher\infrastructure\adapter\memory\LocalSessionRegistryAdapter.java`
- `...\dispatcher\infrastructure\security\JwtValidator.java`
- `...\dispatcher\config\WebSocketConfig.java`
- `...\dispatcher\presentation\controller\ChannelController.java`, `NotificationDispatchController.java`
- `...\dispatcher\application\usecase\GestionCanalUseCaseImpl.java`
- `...\dispatcher\infrastructure\adapter\persistence\InMemoryChannelRepositoryAdapter.java`
- `...\dispatcher\infrastructure\adapter\rabbitmq\{RabbitEventConsumer,RabbitFallbackProducer}.java`
- `...\dispatcher\domain\model\{TipoCanal,EstadoCanal,EventType,TipoEventoCanal,EventEnvelope,CanalComunicacion}.java`

Backend processor/help/biometría:
- `...\findu\CORE\findu-notification-processor\src\main\java\com\findu\notification\processor\infrastructure\consumer\NotificationConsumer.java`
- `...\findu-notification-processor\...\infrastructure\adapter\out\externalapi\FirebasePushSender.java` (STUB TODO)
- `...\findu\CORE\findu-help-v2\...\infrastructure\adapter\out\event\HttpRealtimeEventPublisher.java`
- `...\findu\ARCHITECTURE\findu-api-gateway\src\main\java\com\findu\gateway\dto\{OnboardingRequest,BiometriaData}.java`
- `...\findu\SECURITY\findu-spring-security\...\dto\request\BiometricSubmitRequest.java`
- `...\findu\CORE\findu-core\...\infrastructure\adapter\out\externalapi\StorageServiceStubAdapter.java`
- `...\findu\.agents\tasks\biometria-idswyft-estado-actual.md` (estado biométrico base)

App Flutter:
- `...\findu-movil\packages\network_ws\lib\src\web_socket_client.dart`
- `...\findu-movil\apps\findu_proveedores\lib\features\market\presentation\screens\market_feed_screen.dart`
- `...\findu_proveedores\lib\features\support\presentation\screens\ticket_detail_chat_screen.dart`
- `...\findu_proveedores\lib\features\support\infrastructure\support_notification_handler.dart`
- `...\findu_proveedores\lib\core\config\environment.dart`
- `...\findu_proveedores\lib\main.dart`
- `...\findu_proveedores\lib\features\auth_onboarding\presentation\components\biometric_capture_flow.dart`
- `...\findu_proveedores\lib\features\auth_onboarding\presentation\screens\{biometric_verification_screen,register_wizard_screen}.dart`
- `...\findu_proveedores\pubspec.yaml`, `...\android\app\src\main\AndroidManifest.xml`

> Nota: contenido de documentos de terceros/diseño citado fue parafraseado. La enumeración de
> commits y símbolos proviene de `git` y de lectura directa del código en este workspace.
