# FIND-U — Dependencias de servicios externos por microservicio y lambda

> Base para el plan de despliegue. **Microservicios → Railway** (contenedores siempre activos).
> **Lambdas → AWS Lambda** (ejecución event-driven / programada).
>
> El objetivo es minimizar costo: por eso se marca para cada dependencia la **opción recomendada**
> (p. ej. Redis/MinIO/RabbitMQ-en-Railway en vez de servicios AWS de pago) y cuáles son
> **opcionales** (desactivables por configuración sin tocar código).
>
> Fuente: análisis del código (`@ConditionalOnProperty`, clientes AWS SDK, `@RabbitListener`,
> `RabbitTemplate`, `RestTemplate`, `@Scheduled`, dependencias de `build.gradle`).

---

## Leyenda de columnas

- **BD**: base de datos propia del servicio.
- **Caché**: Redis u otra.
- **Mensajería**: RabbitMQ / colas.
- **AWS SDK**: servicios AWS que el código invoca vía SDK.
- **HTTP interno**: llamadas síncronas a otros servicios FIND-U.
- **Obligatorio / Opcional**: si el servicio arranca y opera sin esa dependencia.

---

## 1. SECURITY

### `findu-spring-security`  (micro → Railway)

| Dependencia | Tipo | ¿Obligatorio? | Flag / cómo se activa | Recomendación (mín. costo) |
|---|---|---|---|---|
| PostgreSQL (R2DBC) | BD | **Sí** | `SPRING_R2DBC_URL` | Postgres de Railway (DNS interno) |
| Redis | Caché / revocación tokens | Opcional | `FINDU_REDIS_ENABLED` | Redis de Railway |
| **DynamoDB** | AWS SDK (revocación JWT) | **Opcional** | `findu.aws.dynamodb.enabled` (si `false` usa adaptador **en memoria**) | **Desactivar** → usar Redis/memoria (DynamoDB = $0 evitado) |
| Notificaciones (SMS/email) | HTTP al **dispatcher** (`NotificationPort`) | **Opcional** | `findu.notification.enabled` (si `false` → stub que solo loguea) | DNS interno Railway; apagable en fase 1 |
| Email de recuperación (`EmailSenderPort`) | — (hoy **stub**) | No | única impl = `LoggingEmailSenderAdapter` (solo log) | Implementar SMTP/SES real cuando se requiera |
| Google OIDC | Externo (login federado) | Sí (si usas login Google) | config OAuth2 client | Gratis (Google Cloud) |

> **`findu-spring-security` NO usa SES ni SNS directamente** (no hay `SesClient`/`SnsClient` en su código).
> - El **email** del código de recuperación usa `EmailSenderPort`, cuya única implementación es un **stub** que solo escribe en log (`LoggingEmailSenderAdapter`) — hoy no envía correo real.
> - Los avisos de **SMS** (recuperación) y **email** (contraseña cambiada) se delegan por **HTTP al `notification-dispatcher`** vía `NotificationPort` (fire-and-forget, apagable con `findu.notification.enabled=false`).
> - Quien realmente llama a **SES/SNS** es el `notification-processor`, al final de la cadena: `security → dispatcher → RabbitMQ → notification-processor → SES/SNS/FCM`.
> - Tampoco usa RabbitMQ. DynamoDB es su único cliente AWS y es prescindible.

### `findu-autorization-server-oauth2`  (micro → Railway)

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| PostgreSQL (JDBC/JPA) | BD | **Sí** | esquema OAuth2 | Postgres de Railway (otra BD/esquema) |
| `findu-spring-security` | HTTP interno | **Sí** | valida/consulta usuarios | DNS interno Railway |

> Sin AWS, sin mensajería.

---

## 2. CORE (microservicios → Railway)

### `findu-core`

| Dependencia | Tipo | ¿Obligatorio? | Flag / cómo | Recomendación |
|---|---|---|---|---|
| PostgreSQL (JPA) | BD | **Sí** | `SPRING_DATASOURCE_URL` | Postgres de Railway |
| Redis | Caché (catálogos, perfiles) | Opcional | `FINDU_CACHE_ENABLED` (si `false` usa caché in-memory) | Redis de Railway |
| `findu-notification-processor` | HTTP interno (PUT disponibilidad) | Opcional | hardcode `:8083/api/v1/canales/...` → **parametrizar** | DNS interno Railway |
| `notification-dispatcher` | HTTP interno (envío notif.) | Opcional | `findu.notification.enabled` + `findu.notification.lambda-url` | Si fase sin notif., `enabled=false` |

> `findu-core` **no** usa RabbitMQ ni AWS SDK directamente. Las notificaciones salen por HTTP.
> ⚠️ Deuda técnica: la URL a notification-processor está **hardcodeada** (`http://findu-notification-processor:8083`); hay que convertirla en variable de entorno para Railway.

### `findu-transaction`

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| PostgreSQL (JPA) | BD | **Sí** | ledger / liquidaciones | Postgres de Railway |
| Spring Cloud Config | Config externa | Opcional | `spring-cloud-starter-config` (puede ir en modo `optional:`) | Config local o desactivar |

> Sin AWS, sin RabbitMQ, sin HTTP saliente. Emite *domain events* internos (en proceso), no a un broker.

### `findu-help-v2`

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| **MongoDB** | BD documental | **Sí** | tickets de soporte | MongoDB de Railway (plugin/imagen) |
| **S3** | AWS SDK (adjuntos) | **Sí (si hay adjuntos)** | `S3Client` + `S3Properties` (endpoint override admite MinIO) | **MinIO en Railway** (S3-compatible, $0) o S3 real |

> Único consumidor de almacenamiento de objetos. El `S3Client` ya admite `endpointOverride`, así que MinIO funciona sin cambiar código (solo endpoint + credenciales).

### `findu-notification-processor`

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| **RabbitMQ** | Mensajería (consumidor) | **Sí** | `@RabbitListener` en 4 colas (push/email/sms/whatsapp) + `TopicExchange` | **RabbitMQ como contenedor en Railway** (evita reescritura a SQS) |
| Redis | Caché plantillas | Opcional | TTL 6h plantillas | Redis de Railway |
| **DynamoDB** | AWS SDK | Opcional | `DynamoDbClient` (device tokens / estado) | Evaluar: desactivar o DynamoDB real |
| **SES** | AWS SDK (email) | Opcional por canal | `SesClient`, `from-email` | SES si envías email; alternativa SMTP |
| **SNS** | AWS SDK (SMS) | Opcional por canal | `SnsClient` | SNS solo si necesitas SMS (siempre tiene costo) |
| **Firebase (FCM)** | Externo (push) | Opcional por canal | `firebase-admin` + `FIREBASE_CREDENTIALS_PATH` | Gratis (FCM) — **hoy es stub (TODO), no envía push real** |

> Es el servicio con MÁS dependencias externas. Es el que concentra el acoplamiento a AWS (SES/SNS/DynamoDB), a RabbitMQ y a Firebase.
>
> **Nota FCM:** el `firebase-admin` vive aquí (NO en el dispatcher). Sin embargo, `FirebasePushSender`
> es hoy un **stub** (`// TODO: Firebase Admin SDK`) que solo loguea; y `DeviceController.register`
> tampoco persiste los device tokens (`// TODO`). Para push real faltaría: (1) subir el **service
> account JSON** de Firebase y apuntar `FIREBASE_CREDENTIALS_PATH`, (2) inicializar `FirebaseApp`,
> (3) implementar el envío real y la persistencia/lookup de tokens, (4) app móvil con FCM configurado.
> El **`notification-dispatcher` NO usa Firebase**: solo enruta a la cola de fallback (RabbitMQ) para
> que este processor haga el push; sus únicas dependencias son RabbitMQ + Redis.

---

## 2.1. Relación dispatcher ↔ processor (notificaciones y chat)

Ambos trabajan juntos pero con responsabilidades distintas. **Cuidado: los nombres confunden.**

| Servicio | Rol real (según código) | Canal | Dependencias |
|---|---|---|---|
| **`findu-notification-dispatcher`** | **Tiempo real / presencia**: sostiene los **WebSockets** (`/ws/events`, JWT en handshake, heartbeat), registra quién está conectado (presencia en Redis) y entrega mensajes en vivo (incluye **chat**). Decide online vs. offline. | WebSocket (entrega directa) + RabbitMQ (fallback) | **RabbitMQ + Redis** |
| **`findu-notification-processor`** | **Canales salientes**: consume de RabbitMQ y despacha a **email (SES)**, **SMS (SNS)** y **push (Firebase)**. | SES / SNS / FCM | **RabbitMQ + Redis + SES + SNS + Firebase** (+ DynamoDB opc.) |

**Flujo de una notificación/evento:**

```
Evento (p.ej. pago, mensaje de chat)
        │
        ▼
findu-notification-dispatcher
   ¿usuario con sesión WebSocket activa?
        │
        ├─ SÍ  → entrega en vivo por WebSocket (realtime / chat)
        │
        └─ NO (celular desconectado/inactivo)
                 → encola en RabbitMQ (cola fallback)
                         │
                         ▼
                 findu-notification-processor  (consume la cola)
                         ├─ push por Firebase (FCM)   ← caso típico offline
                         ├─ email (SES)
                         └─ SMS (SNS)
```

En resumen: **el dispatcher decide y entrega en tiempo real; el processor envía por los canales externos cuando no hay sesión activa.** El push por Firebase ocurre cuando el dispatcher detecta el celular desconectado y delega al processor vía RabbitMQ.

---

## 3. CORE / LAMBDA  (→ AWS Lambda)

### `findu-notification-dispatcher`

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| **RabbitMQ** | Mensajería (produce **y** consume) | **Sí** | `RabbitTemplate.convertAndSend` + `@RabbitListener` (cola fallback) | Mismo broker RabbitMQ que el processor |
| Redis (reactive) | Presencia / pub-sub | **Sí** | WebFlux + Redis reactivo (usuario online/offline) | Redis de Railway |
| WebSocket (`/ws/events`) | Entrada tiempo real | Sí | notificaciones en vivo | — |

> ⚠️ Depende de RabbitMQ + Redis **persistentes**. Si va como AWS Lambda real, el modelo "siempre escuchando WebSocket + RabbitMQ" **no encaja con Lambda** (que es efímera). Recomendación: este NO debería ser Lambda; es un servicio de larga duración → **mejor micro en Railway**. (Decisión a confirmar.)

### `findu-transaction-daily-balance`

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| PostgreSQL (JPA) | BD | **Sí** | lee/escribe cortes diarios | Misma BD de transaction |
| Spring Cloud Config | Config | Opcional | — | opcional |
| Scheduler (`@Scheduled` cron 22:00) | Interno | **Sí** | corte diario America/Bogota | En Lambda → **EventBridge Scheduler** dispara la Lambda (no `@Scheduled`) |

> Al migrar a Lambda, el cron `@Scheduled` se reemplaza por **EventBridge** (regla programada). Solo necesita acceso a Postgres.

### `findu-transaction-provider-payout`

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| (ninguna externa aún) | — | — | esqueleto web; sin JPA/AWS/Rabbit en `build.gradle` | Definir: ¿accede a BD de transaction? ¿invoca pasarela de pago? |

> Actualmente es un cascarón. Antes de desplegar hay que definir qué hace (pago a proveedor) y qué necesita (BD + pasarela de pagos externa).

### `findu-transaction-refund`

| Dependencia | Tipo | ¿Obligatorio? | Notas | Recomendación |
|---|---|---|---|---|
| (ninguna externa aún) | — | — | esqueleto web; sin dependencias externas | Definir alcance (reembolsos) y pasarela |

> También es un cascarón; mismo comentario que payout.

---

## 4. Resumen: servicios externos a aprovisionar

| Servicio externo | ¿Dónde corre? | Lo consumen | ¿Obligatorio fase 1? |
|---|---|---|---|
| PostgreSQL | Railway (una instancia, varias BD/esquemas) | security, oauth2, core, transaction, daily-balance | **Sí** |
| MongoDB | Railway | help-v2 | Solo si incluyes help-v2 |
| Redis | Railway | security, core, notification-processor, dispatcher | Recomendado |
| RabbitMQ | **Railway (contenedor)** | notification-processor, dispatcher | Solo si incluyes notificaciones |
| MinIO (S3-compat) | Railway | help-v2 | Solo si incluyes help-v2 |
| AWS SES | AWS | notification-processor | Solo si envías email |
| AWS SNS | AWS | notification-processor | Solo si envías SMS |
| AWS DynamoDB | AWS | security (opc.), notification-processor (opc.) | **No** (desactivable) |
| AWS EventBridge | AWS | daily-balance (como Lambda) | Solo si daily-balance va a Lambda |
| Firebase FCM | Google | notification-processor | Solo si envías push |
| Google OIDC | Google | security | Solo si login con Google |

---

## 5. Decisiones clave sobre mensajería y AWS (pendientes de confirmar)

1. **RabbitMQ vs. AWS SQS**: el código usa Spring AMQP con exchange de tópicos y routing keys (processor consume 4 colas; dispatcher produce y consume). Migrar a SQS implica **reescribir** productores/consumidores (SQS no tiene exchanges ni routing keys; requeriría SNS→SQS). **Recomendación: RabbitMQ como contenedor en Railway** = cero reescritura.
2. **DynamoDB**: prescindible en security (hay adaptador en memoria) y configurable en processor. **Recomendación: desactivar** y apoyarse en Redis.
3. **dispatcher como Lambda**: su modelo (WebSocket + listener RabbitMQ permanente + Redis pub/sub) **no es compatible con AWS Lambda efímera**. Debería ser micro en Railway, no Lambda.
4. **Lambdas de transaction (payout/refund)**: hoy son esqueletos; definir alcance antes de desplegar.
5. **SES/SNS/FCM**: son por-canal y opcionales; se habilitan según qué notificaciones quieras en producción.
