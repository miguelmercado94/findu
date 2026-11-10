# Informe: ciclo de vida del WebSocket estilo WhatsApp (ws-ciclo-vida)

## 1. Resumen

El proveedor recibe eventos por un unico WebSocket gobernado por el ciclo de vida de la app (foreground = socket, background = push FCM, al volver = sincronizacion). El dispatcher cierra sesiones de inmediato y detecta zombis, el processor envia push para los eventos del proveedor y el socket lleva un token validado (modo OFF/LOG/ENFORCE). Los contenedores NO se reconstruyeron: los cambios backend aplican al reconstruir processor, dispatcher y core.

## 2. Matriz evento -> estrategia (final)

| Evento | Foreground (socket) | Background (> gracia) | Al volver / toque |
|---|---|---|---|
| CHAT_MESSAGE | Socket (`ticket_detail_chat`) | Push `type=TICKET_CHAT` (existente, intacto; `eventId` aditivo) | Reabre socket + `_fetchRemoteMessages`. En foreground con el chat de ese `ticketCode` abierto: solo sync; si no, banner |
| SYSTEM_ALERT | WS delta | Sin push (`FINDU_PUSH_ALERTS_ENABLED=false`) | Pull `_loadAlerts` |
| OFFER_RECEIVED | Socket | Push solo por compatibilidad: core lo emite al CLIENTE (`DispatcherNotificationObserver.onOfertaRecibida`); el mapper devuelve vacio si el destino no empieza por `PROVEEDOR_` | Sync + Trabajos |
| SERVICE_UPDATE (lista blanca) | Socket | Push `type=SERVICE_UPDATE` + `tipoEvento` | Sync; toque: NUEVA_SOLICITUD y SOLICITUD_ACTUALIZADA sin `ofertaId` -> Servicios; el resto -> Trabajos |
| SERVICE_UPDATE (disipacion) | Socket | Sin push | Sync recupera el estado final |
| Toggle disponibilidad | Sin cambios; no lo gobierna el ciclo de vida | igual | igual |

Eventos que de verdad llegan a `PROVEEDOR_<id>`: SERVICE_UPDATE con tipoEvento NUEVA_SOLICITUD, SOLICITUD_ACTUALIZADA, OFERTA_ACEPTADA, SOLICITUD_CERRADA, OFERTA_RETIRADA, OFERTA_EXPIRADA, SERVICIO_EN_CURSO, SERVICIO_FINALIZADO, ALARMA_SERVICIO_1H (mas disciplinarios/evidencia de otros casos de uso).
"NUEVA_SOLICITUD solo a elegibles en linea": `GestionSolicitudesUseCaseImpl` usa `findEligibleProviders` (disponible=true, estado ACTIVO, especialidad activa, cobertura de municipio) y luego `filtrarProveedoresSinRestriccion`; "en linea" es el flag `disponible` (toggle), no presencia WebSocket.

## 3. Como funcionan el gestor y el ciclo de vida (app)

`RealtimeConnectionManager` (`lib/core/realtime/`) es un singleton; `attach()` se llama en `main.dart`. Es el unico lugar con `WebSocketClient(` en `lib/`.

- Conexion deseada = autenticado && proveedorId>0 && (foreground || gracia). El token no es requisito para estar deseada (hallazgo #4); `clearToken()` con el mismo proveedorId no desconecta.
- Estado inicial: `attach()` lee `WidgetsBinding.lifecycleState` (null = resumed); si arranca en paused/detached inicia gracia (#5).
- Gracia: al pasar a background el socket sigue 25 s; luego se cierra y el backoff se resetea (#9). Al volver (resumed) reabre, resetea backoff y las pantallas hacen sync (`_loadJobs`, `_loadAlerts`, `_fetchRemoteMessages`).
- Backoff exponencial con jitter, tope 60 s, estable tras un tiempo de conexion sostenida (`stableAfter`). Sin paquete de conectividad: reintenta a ciegas.
- `client.connect(url)` con timeout de 15 s; en timeout hace `disconnect()` y backoff; `_connectEpoch` descarta el resultado tardio de un connect abandonado y no crea un segundo socket (#6).
- Dedupe de eventos por `eventId`.
- Token: en cada conexion/reconexion `_open()` llama `_ensureFreshToken` antes de `urlBuilder`. Si el `exp` (decodificado sin firma, `jwt_expiry.dart`) vence en <=60 s renueva con `AuthApiAdapter.refreshAccessToken()` (POST `/security-auth/api/v1/auth/refresh`, con fallback a re-emision federada). Si falla: no conecta, sin bucle, `_sessionExpired=true` y callback opcional `onSessionExpired` (no conectado en `main.dart`). Se rehabilita con cambio de token, `reconnectNow()` o volver a foreground. La URL nunca se loguea.
- Push foreground: `foreground_push_policy.dart` (`decideForegroundPush`), `push_router.dart` (toques, arranque en frio, `flushPending` conserva el payload hasta 2 min si aun no hay sesion/navegador), `shell_navigation.dart`. Claves FCM data leidas por la app: `type`, `tipoEvento`, `ticketCode`, `eventId`, `ofertaId`.

## 4. Cambios backend

### Processor (`findu-notification-processor`)
- `FallbackPushMapper` (nuevo): SERVICE_UPDATE por lista blanca, OFFER_RECEIVED (compat, solo `PROVEEDOR_*`), SYSTEM_ALERT tras flag. Devuelve vacio (DEBUG) si el destino no es `PROVEEDOR_*`.
- `NotificationConsumer.handleFallback`, `PushNotificationTemplate` (EVENT_PUSH; no loguea el cuerpo, solo su longitud), `FirebasePushSender` (lista blanca de data, prioridad HIGH, ttl 10 min). Push data: `type`, `tipoEvento`, `solicitudId`, `ofertaId`, `servicioNombre`, `eventId`.

### Dispatcher (`findu-notification-dispatcher`)
- `EventWebSocketHandler`: `Mono.firstWithSignal(receive, ping, watchdog)`; cierre inmediato de sesion; watchdog por ticks (heartbeat 30 s, cierre GOING_AWAY al tick 3 = 90 s sin frames entrantes); `removePresence` solo si `!hasLocalSession`; errores de sesion con WARN propio separado del de autenticacion.
- `LocalSessionRegistryAdapter.hasLocalSession` cuenta solo sesiones `isOpen()`; envio local sin sesion abierta o fallido cae a la cola fallback (`EventDispatcherService`).
- Token (FEAT-003): `JwtValidator.validateAccessToken` (HS256, rechaza `JWTRefresh`, exp, firma, claim `userId`; secreto Base64 -> UTF-8), `CoreProfileClient` (timeout 1500 ms, cache TTL 300 s solo aciertos), `WebSocketAuthService`, `WebSocketHandshakeAuthFilter` (401/403 antes del upgrade solo en ENFORCE).
- Core: unico cambio `GET /findu-core/internal/perfiles/by-auth/{authUserId}` (X-Internal-Service-Key; 403 sin llave, 404 sin perfil de proveedor).

## 5. Variables y flags nuevos

| Variable | Modulo | Default | Efecto |
|---|---|---|---|
| `FINDU_PUSH_ALERTS_ENABLED` | processor | `false` | Habilita push de SYSTEM_ALERT |
| `FINDU_PUSH_SERVICE_UPDATE_TYPES` | processor | lista por defecto en application.yml | Lista blanca de tipoEvento (coma; trim, mayusculas, vacios fuera, vacia = default) |
| `FINDU_WS_MAX_MISSED_PONGS` | dispatcher | `3` | Ticks sin frames antes de cerrar; <1 se corrige a 3 |
| `FINDU_WS_AUTH_MODE` | dispatcher | `OFF` en application.yml, `LOG` en docker-compose | OFF / LOG / ENFORCE |
| `JWT_SECRET` | dispatcher | (mismo base64 que security/core) | Firma de tokens |
| `FINDU_CORE_URL`, `FINDU_INTERNAL_SERVICE_KEY`, `FINDU_CORE_TIMEOUT_MS`, `FINDU_CORE_CACHE_TTL_SECONDS` | dispatcher | ver compose / 1500 / 300 | Cliente al endpoint interno de core |

`docker-compose.yml` solo se modifico en el hunk del dispatcher (JWT_SECRET, FINDU_WS_AUTH_MODE, FINDU_CORE_URL, FINDU_INTERNAL_SERVICE_KEY); no se agregaron `FINDU_PUSH_*` ni `FINDU_WS_MAX_MISSED_PONGS` al compose (usan default).

## 6. Heartbeat (documentacion)

El dispatcher envia ping cada 30 s. Cada frame entrante (incluido el pong) reinicia el contador `missed`. Si el contador llega a `FINDU_WS_MAX_MISSED_PONGS` (3) ticks seguidos, o sea 90 s sin frames, cierra con GOING_AWAY (timeout de cierre 5 s) y limpia la presencia si no quedan sesiones locales abiertas. Comentario equivalente en `application.yml` del dispatcher. Hueco residual: un zombi cuenta como abierto (`isOpen()` true) hasta 90 s.

## 7. Resultados de verificacion (esta integracion)

Ejecutado sobre el arbol limpio de la app (HEAD e36fc20) y del backend (HEAD 0ef71c3). Los cambios de finanzas ya estaban commiteados, asi que no hizo falta worktree.

| Comando | Resultado |
|---|---|
| `flutter pub get` | OK |
| `flutter analyze` | 41 issues, 0 errores (base ~39-40; info/warnings) |
| `flutter test` | 222 OK, 4 fallos preexistentes: `alert_websocket_mapping` ('OFFLINE'), `biometric_capture_flow`, `profile_alert_pull`, `profile_and_navigation_redesign`. `register_wizard` no fallo |
| `flutter build apk --debug --dart-define=FINDU_BACKEND_HOST=192.168.1.33` | OK (`build/app/outputs/flutter-apk/app-debug.apk`) |
| processor `gradlew clean compileJava -x test` + `test` | OK, 79 tests, 0 fallos |
| dispatcher `gradlew clean compileJava -x test` + `test` | OK, 68 tests, 0 fallos |
| core `gradlew clean compileJava -x test` + `test` | OK |

No se tocaron `findu-spring-security` (rojo preexistente `submit_perfilMissing_409`) ni `findu-transaction`.

## 8. Commits y push

Backend (`findu`, develop, push a origin/develop hecho en FEAT-002/003):
- `ee0e7e7` feat(notification-processor): push FCM generalizado para eventos del proveedor
- `6705d8b` fix(notification-dispatcher): cierre inmediato, watchdog de zombies y fallback ante sesion cerrada
- `2e547c1` feat(core): endpoint interno perfil por authUserId
- `a9c26ab` feat(notification-dispatcher): autenticacion por token del WebSocket con modo OFF/LOG/ENFORCE
- `0ef71c3` test(scripts): E2E del token del WebSocket (ws-auth-e2e.ps1)

App (`findu_proveedores`, develop, solo local, sin remoto):
- `6bcb560` feat(realtime): gestor unico de WebSocket con ciclo de vida y backoff
- `467a03b` refactor(realtime): pantallas se suscriben al gestor unico
- `f24f564` feat(push): politica foreground, enrutado de ofertas y arranque en frio
- `e36fc20` feat(realtime): token vigente en cada conexion del WebSocket

## 9. Token del WebSocket: E2E y paso a ENFORCE

### Resultados E2E
Ejecutado contra el stack docker (2026-11-10): se reconstruyeron solo `findu-core` y `notification-dispatcher` (`gradlew bootJar` + `docker compose up -d --no-deps --build findu-core notification-dispatcher`; localstack y demas contenedores intactos). Script: `findu/scripts/ws-auth-e2e.ps1`. Compose final en LOG (verificado: `FINDU_WS_AUTH_MODE=LOG` en el contenedor).

| Caso | Descripcion | LOG | ENFORCE |
|---|---|---|---|
| a1 | token del duenio + canal propio | OPEN | OPEN |
| a2 | token del duenio sin userId | OPEN | OPEN |
| b1 | otro usuario + canal ajeno | OPEN + log | 403 |
| b2 | duenio + canal de otro proveedor | OPEN + log | 403 |
| b3 | duenio + canal no verificable CLIENTE_9 | OPEN + log | 403 |
| c1 | sin token + userId | OPEN + log | 401 |
| d1 | refresh token | OPEN + log | 401 |
| e1 | token vencido | OPEN + log | 401 |
| f1 | token basura | OPEN + log | 401 |
| g1 | tokens ausentes de los logs | PASS | PASS |
| g2 | violaciones registradas | PASS | PASS |

LOG 11/11 PASS, ENFORCE 11/11 PASS. Tambien se cubren con tests unitarios del dispatcher (valido, vencido, refresh, firma mala, secreto Base64 y UTF-8, canal ajeno y propio, core caido en LOG/ENFORCE, handshake por modo) y test del controller de core. El script se corrigio durante la ejecucion (fuga de pipeline de ConnectAsync y extraccion del codigo HTTP desde excepciones anidadas) y se agrego b3. Nota: el contenedor del dispatcher se llamaba `e768bb976d18_findu-notification-dispatcher` (resto de un recreate previo); el primer `-Apply` lo recreo con el nombre correcto.

Para ejecutarlo:
```
docker compose up -d --build --no-deps findu-core notification-dispatcher
./scripts/ws-auth-e2e.ps1 -Mode LOG
./scripts/ws-auth-e2e.ps1 -Mode ENFORCE -Apply
```

### Procedimiento para pasar a ENFORCE
1. Reconstruir core y dispatcher (comando de arriba).
2. Con la app en LOG, usarla de verdad y revisar: `docker logs findu-notification-dispatcher | Select-String WS_AUTH_VIOLATION`. No debe haber `NO_TOKEN` ni `BAD_TOKEN` de la app actual; si aparecen, el celular no envia o no renueva el token.
3. Poner `FINDU_WS_AUTH_MODE=ENFORCE` (env o compose) y `docker compose up -d --no-deps notification-dispatcher`.
4. Rollback: volver a `LOG` u `OFF` y recrear el dispatcher.

Decisiones del modo: en ENFORCE los canales solicitados que no son `PROVEEDOR_` (CLIENTE_, usernames) se rechazan 403 CHANNEL_MISMATCH porque no se pueden verificar contra el token (en LOG se aceptan y se registra la violacion); en ENFORCE sin `userId` la identidad sale del token; en LOG sin token ni `userId` se rechaza (como antes); usuario sin perfil de proveedor = CHANNEL_MISMATCH; core caido: acepta+log en LOG, rechaza en ENFORCE. Consecuencia: en ENFORCE solo la app de proveedores (canales PROVEEDOR_) puede conectar; antes de activarlo, confirmar en LOG (`WS_AUTH_VIOLATION reason=CHANNEL_MISMATCH`) que ningun otro cliente legitimo usa canales no PROVEEDOR_.

## 10. Estado de hallazgos de la revision de diseno

| # | Sev. | Estado |
|---|---|---|
| 1 | MEDIUM OFFER_RECEIVED no va al proveedor | Resuelto: mapper exige `PROVEEDOR_`, ruteo por tipoEvento reales; test CLIENTE_5 -> vacio |
| 2 | MEDIUM watchdog por reloj de pared | Resuelto: ticks, cierre al tick 3 (90 s), test con `withVirtualTime` |
| 3 | MEDIUM TICKET_CHAT en foreground | Resuelto: syncOnly si el ticket esta abierto, banner si no; tests de la tabla |
| 4 | MEDIUM clearToken / token vacio | Resuelto: test `hallazgo #4` |
| 5 | MEDIUM estado de ciclo inicial | Resuelto: test `hallazgo #5` |
| 6 | MEDIUM connect sin timeout | Resuelto: timeout 15 s + epoch; test `hallazgo #6` |
| 7 | MEDIUM hasLocalSession / descarte silencioso | Resuelto: solo `isOpen()` y fallback a cola; tests de adaptador y servicio |
| 8 | NIT verificacion de timers | Resuelto (sin assert de transientCallbackCount) |
| 9 | NIT reset de backoff | Resuelto: test `hallazgo #9` |
| 10 | NIT parseo de service-update-types | Resuelto |
| 11 | NIT doSend loguea cuerpo | Resuelto: solo longitud |
| 12 | NIT @Value en tests | Resuelto: `ReflectionTestUtils` |
| 13 | NIT onErrorResume de auth | Resuelto: WARN separado |

## 11. Pendientes y riesgos

- Ahorro de bateria de Android (OEM, Doze, restringir actividad en segundo plano) puede matar el proceso y retrasar o descartar pushes; HIGH priority mitiga, no garantiza.
- Banner foreground es un `SnackBar`; la notificacion local real requiere `flutter_local_notifications`.
- Sin paquete de conectividad: backoff a ciegas con tope de 60 s.
- Toque en push no selecciona la sub-pestana "Ofertas" de Trabajos.
- Con canal INACTIVO el socket no recibe eventos aunque este abierto (comportamiento existente); el push en foreground dispara sync para compensarlo.
- `WebSocketClient` conserva su reconexion lineal interna; el gestor la anula tras cada caida (ventana de milisegundos).
- Presencia Redis con TTL 24 h sin limpieza si el nodo muere; zombi hasta 90 s cuenta como abierto.
- `SYSTEM_ALERT` push disponible pero apagado.
- Sesion expirada (refresh fallido): el gestor se detiene sin logout automatico ni callback conectado. Decision de producto pendiente: que debe ver el usuario. Usuarios de login manual sin refresh valido pierden el tiempo real tras 1 h hasta nuevo login (en LOG antes seguia conectando solo con `userId`).
- `/auth/refresh` nunca se habia usado desde la app y no esta verificado contra el gateway real.
- E2E del token pendiente de contenedores reconstruidos.
- Tests preexistentes en rojo: 4 de la app (arriba) y `submit_perfilMissing_409` en `findu-spring-security`.

## 12. Plan de prueba manual en el telefono

Prerrequisito: reconstruir processor, dispatcher y core; instalar el APK debug; sesion de proveedor iniciada, FCM con token registrado. Tener `docker logs -f findu-notification-dispatcher` y `findu-notification-processor` abiertos.

1. Foreground por socket: app abierta en Servicios con toggle en linea. Crear una solicitud elegible desde un cliente. Debe aparecer en vivo sin push; en el log del dispatcher, entrega local. Probar tambien un SYSTEM_ALERT en vivo.
2. Chat: abrir un ticket y mandar un mensaje desde el otro lado; llega por socket, sin banner. Salir del chat y repetir: debe aparecer banner.
3. Background por push: pulsar Home y esperar mas de 25 s (gracia). En el log, el socket se cierra y la presencia se limpia. Generar un evento SERVICE_UPDATE (p. ej. NUEVA_SOLICITUD, OFERTA_ACEPTADA); debe llegar notificacion push (log del processor con `tipoEvento`).
4. Toque en push: tocar la notificacion. Debe abrir la app en Servicios (NUEVA_SOLICITUD) o Trabajos (el resto). Repetir con la app cerrada (arranque en frio).
5. Volver y sincronizar: regresar a la app antes y despues de la gracia; el socket reabre, y la lista de trabajos/alertas se refresca (sync) sin duplicados.
6. Doze: `adb shell dumpsys deviceidle force-idle` o `adb shell am set-inactive com.findu.proveedores true` (usar el applicationId real del APK); enviar un evento y comprobar que el push llega (HIGH). Restaurar con `adb shell am set-inactive <paquete> false` y `adb shell dumpsys deviceidle unforce`.
7. Zombi: con la app en foreground y conectada, activar modo avion unos 2 minutos. El dispatcher debe cerrar la sesion por el watchdog (~90 s, GOING_AWAY) y limpiar presencia; un evento en ese lapso cae a push. Desactivar modo avion: la app reconecta con backoff y sincroniza.
8. Token: con la app abierta mas de 1 h (o token vencido), confirmar que reconecta con token renovado; si el refresh falla, el socket se detiene sin bucle (revisar logs del dispatcher: sin `WS_AUTH_VIOLATION` repetidos).
9. Logs de auditoria: `docker logs findu-notification-dispatcher | Select-String WS_AUTH_VIOLATION` debe estar vacio con la app actual antes de pasar a ENFORCE.
