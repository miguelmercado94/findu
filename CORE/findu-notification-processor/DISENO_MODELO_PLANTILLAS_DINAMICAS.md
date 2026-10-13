# Diseño — Modelo dinámico de plantillas (`findu-notification-processor`)

> **Estado:** PROPUESTA DE DISEÑO. No se ha tocado código todavía.
> Este documento es la base para la futura refactorización del micro hacia un
> **motor de plantillas gestionable desde un rol admin**.
>
> Persistencia: **DynamoDB** (decisión del usuario — se descartó migrar a Postgres).
> Tabla principal: `notification_templates`.

---

## 1. Objetivo

Hoy el micro resuelve plantillas por **canal técnico** (`EMAIL`, `SMS`, `PUSH`,
`WHATSAPP`) con un modelo plano. El objetivo a largo plazo es que un **admin**
pueda crear y configurar plantillas de 5 **tipos funcionales** sin desplegar
código:

| Tipo           | ¿Qué es?                                                              | Canal de salida                        |
|----------------|-----------------------------------------------------------------------|----------------------------------------|
| `SMS`          | Mensaje de texto                                                      | AWS SNS (`SnsSmsender`)                |
| `EMAIL`        | Correo (TXT/HTML)                                                     | AWS SES (`SesEmailSender`)             |
| `WHATSAPP`     | Mensaje de WhatsApp                                                   | `WhatsappSender` (cola `findu.notifications.whatsapp`) |
| `EVENTO`       | Mensaje entre microservicios                                         | Cola RabbitMQ (declarada en plantilla) |
| `NOTIFICACION` | Push en tiempo real al usuario                                        | WebSocket (dispatcher) → fallback FCM  |
| `ALERTA`       | Banner/aviso en pantalla, con metadata UI rica                       | Entregada al front por pantalla        |

`NOTIFICACION` y `ALERTA` se mantienen **separadas**: la notificación es un
evento puntual que se enruta por presencia; la alerta es contenido persistente
con metadata de UI (categoría, pantalla destino, vigencia, cerrable, etc.).

> **Decisión (C):** `WHATSAPP` deja de ser un "canal" interno y pasa a ser un
> **tipo funcional** de primer nivel, igual que `SMS`/`EMAIL`. Son **6 tipos**.

---

## 2. Estado actual (lo que ya existe en código)

- `NotificationTemplate` (dominio): `id` (PK `CODE#CHANNEL#LANG`), `channel`,
  `language`, `contentType` (TXT/HTML), `subject`, `bodyTemplate`, `version`, `active`.
- `NotificationMessage`: `notificationId`, `type`, `recipient`, `templateCode`,
  `language`, `params`, `requiresConnection`, `attachments`, `createdAt`.
- `DynamoDbTemplateAdapter`: `getItem` por `id` compuesto; mapea `is_active`,
  `content_type`, `body_template`, `version`.
- `NotificationConsumer`: 4 `@RabbitListener` (push/email/sms/whatsapp).
- `FinancialEventRouter` + handlers: enruta **eventos entrantes** por `eventType`.
- Render con `{{clave}}` (`TemplateService`).
- Tabla gemela `findu_notifications` para status de envío.

**Gap:** el modelo es por canal, plano, sin tipo funcional, sin metadata de UI
para alertas, sin declaración de cola para eventos, sin audiencia ni vigencia.

---

## 3. Modelo dinámico propuesto

### 3.1 Campos comunes (todos los tipos)

| Campo            | Tipo DynamoDB | Descripción                                                        |
|------------------|---------------|--------------------------------------------------------------------|
| `id` (PK)        | S             | Partition key lógica: `CODE#TIPO#LANG` (ver §9, versionado)        |
| `sk` (SK)        | S             | Sort key de versión: `v#<n>` + puntero `v#ACTIVE` (ver §9)         |
| `codigo`         | S             | Código funcional, ej. `VERIFICACION_OTP`, `PROMO_NAVIDAD`          |
| `tipo`           | S             | `SMS` \| `EMAIL` \| `WHATSAPP` \| `EVENTO` \| `NOTIFICACION` \| `ALERTA` |
| `language`       | S             | `es`, `en`                                                         |
| `bodyTemplate`   | S             | Cuerpo con placeholders `{{var}}`                                  |
| `variables`      | L<S>          | Lista declarada de variables esperadas (para validación en admin) |
| `version`        | N             | Versión de la plantilla                                           |
| `active`         | BOOL          | `is_active`                                                        |
| `audiencia`      | M             | Ver §4 (modelo de audiencia)                                      |
| `createdAt`      | S             | Auditoría                                                         |
| `createdBy`      | S             | Admin que la creó                                                 |
| `updatedAt`      | S             | Auditoría                                                         |
| `updatedBy`      | S             | Admin que la editó                                               |

### 3.2 Campos específicos por tipo

**`EMAIL`**
- `subject` (S) — asunto
- `contentType` (S) — `TXT` | `HTML`

**`SMS`**
- (ninguno adicional; el sender trunca a 160 chars)

**`WHATSAPP`**
- `waTemplateName` (S) — nombre de la plantilla aprobada en WhatsApp Business (si aplica)
- `contentType` (S) — normalmente `TXT`

**`EVENTO`**
- `rabbitExchange` (S) — exchange destino (ej. `findu.notifications`)
- `rabbitRoutingKey` (S) — routing key / nombre lógico de cola destino
- `rabbitQueue` (S) — **cola RabbitMQ a la que va** (requisito explícito del usuario)
- `eventType` (S) — tipo de evento para que el consumidor lo enrute (consistente con `FinancialEventRouter`)

**`NOTIFICACION`** (push en tiempo real)
- `title` (S) — título del push
- `routing` (M):
  - `condicion` (S) — `isUserActivo` (regla fija del dominio)
  - `canalSiActivo` (S) — `WEBSOCKET`
  - `canalSiInactivo` (S) — `PUSH` (FCM)
  - *Nota:* el **dispatcher (WebSocket) es la fuente de verdad de presencia**.
    Si recibe la notif y detecta al usuario desconectado/desactivado, **fuerza
    el fallback a FCM push**. Este comportamiento ya existe en el dispatcher.

**`ALERTA`** (banner/aviso en pantalla)
- `categoria` (S) — `INFO` | `SEGURIDAD` | `PREVENTIVA`
- `pantallaDestino` (S) — ruta/pantalla donde se muestra (ej. `perfil_proveedor`)
- `titulo` (S)
- `ctaLabel` (S) — texto del botón (ej. "Conocer más")
- `ctaAction` (S) — deep-link / acción al pulsar
- `cerrable` (BOOL) — si el usuario puede descartarla con la X
- `prioridad` (N) — orden en carrusel (como "2 de 3" del ejemplo bancario)
- `vigenciaDesde` / `vigenciaHasta` (S, ISO-8601) — ventana temporal
- `condicionAparicion` (S) — expresión declarativa (ver §5), ej. `emailVerified == false`
- `tiempoDesaparicion` (N) — segundos para auto-ocultar (0 = no desaparece sola)

---

## 4. Modelo de audiencia (punto B — recomendación por defecto)

Campo `audiencia` (mapa) con un `modo` y su payload:

```json
"audiencia": { "modo": "TODOS" }
```
```json
"audiencia": {
  "modo": "SEGMENTO",
  "filtros": { "rol": "PROVEEDOR", "ciudad": "Barranquilla", "sinEspecialidades": true }
}
```
```json
"audiencia": {
  "modo": "USUARIO",
  "userIds": ["3", "17", "42"]
}
```

- `TODOS` — cualquier usuario al que aplique el tipo/pantalla.
- `SEGMENTO` — filtros declarativos evaluados contra atributos del usuario.
- `USUARIO` — lista explícita de IDs (campañas puntuales / pruebas).

> **AJUSTABLE** — confirmar o cambiar los filtros de `SEGMENTO` que realmente
> necesitamos (rol, ciudad, estado de verificación, etc.).

---

## 5. ¿Quién evalúa las condiciones de ALERTA? (punto A — recomendación por defecto)

Enfoque **híbrido**:

- **`SEGURIDAD` / `PREVENTIVA`** → las evalúa el **front**, con `condicionAparicion`
  declarativa contra el estado del usuario que ya tiene en memoria
  (ej. `emailVerified == false`, `biometriaVerificada == false`). Reacción
  inmediata, sin llamada extra al backend.
- **`INFO` / promos** (ej. banner tipo carrusel bancario) → **no** dependen del
  estado del usuario. Se controlan por **vigencia temporal**
  (`vigenciaDesde`/`vigenciaHasta`) + `audiencia`. El admin las activa/desactiva
  por fecha sin desplegar.

> **AJUSTABLE** — si prefieres que el backend evalúe TODO (y el front solo pinte
  lo que recibe), cambiamos a: endpoint `GET /alertas/activas?pantalla=&userId=`
  que ya devuelve la lista filtrada por condición + audiencia + vigencia.

Gramática mínima sugerida para `condicionAparicion` (declarativa, segura):
`<atributo> <op> <valor>` con `op ∈ { ==, !=, >, <, >=, <= }` y combinables con `&&` / `||`.

---

## 6. Índices secundarios (GSI) sugeridos

| GSI             | PK              | Uso                                                           |
|-----------------|-----------------|---------------------------------------------------------------|
| `GSI_tipo`      | `tipo`          | Admin lista todas las plantillas de un tipo                   |
| `GSI_pantalla`  | `pantallaDestino` | Front/back obtiene alertas de una pantalla                  |
| `GSI_codigo`    | `codigo`        | Resolver todas las variantes (tipo/idioma) de un código      |

> **Migración de clave:** `CHANNEL` → `TIPO`. `EMAIL`/`SMS`/`WHATSAPP` coinciden
> 1:1, `PUSH` pasa a `NOTIFICACION`, y se añaden `EVENTO`/`ALERTA`.

---

## 9. Versionado con histórico (punto D — decisión confirmada)

**Decisión:** cada edición **crea una versión nueva**; nunca se sobrescribe. Se
mantiene histórico completo y un puntero a la versión activa.

Esquema de tabla con **PK + SK** (DynamoDB composite primary key):

- **PK** `id` = `CODE#TIPO#LANG` (ej. `VERIFICACION_OTP#SMS#es`)
- **SK** `sk`:
  - `v#1`, `v#2`, `v#3`, … → cada versión inmutable (el `bodyTemplate` de esa versión)
  - `v#ACTIVE` → ítem puntero que guarda `activeVersion` (N) + copia desnormalizada de la versión activa para lookup en 1 solo `GetItem`

Flujo:

1. **Resolver para envío** (hot path): `GetItem(PK=id, SK="v#ACTIVE")` → 1 lectura.
2. **Editar** (admin): `activeVersion+1`, se escribe `v#<n+1>` y se actualiza
   `v#ACTIVE`. Transacción `TransactWriteItems` para consistencia.
3. **Rollback**: el admin apunta `v#ACTIVE` a cualquier `v#<k>` previa.
4. **Historial**: `Query(PK=id, SK begins_with "v#")` lista todas las versiones.

Campos de auditoría por versión (`createdBy`, `createdAt`) permiten saber quién
publicó qué y cuándo. `version` (campo común) = número de esa versión.

> Nota: esto reemplaza el `GetItem` plano actual del `DynamoDbTemplateAdapter`.
> El adapter nuevo lee `v#ACTIVE`; la lógica de versión queda encapsulada en el
> adaptador de persistencia (la capa de servicio sigue sin conocer DynamoDB).

---

## 10. Estándar de arquitectura (alinear con el core)

El micro debe seguir el **mismo layout hexagonal de `findu-core`**. Estructura
objetivo de paquetes (hoy el processor usa `presentation/`, `dto` sueltos y
`application/template`, que se deben reacomodar):

```
com.findu.notification.processor
├── application
│   ├── dto
│   │   ├── request        # CreateTemplateRequest, UpdateTemplateRequest, ...
│   │   └── response       # TemplateResponse, TemplateVersionResponse, ...
│   ├── port
│   │   ├── in             # casos de uso: ManageTemplateUseCase, ResolveTemplateUseCase
│   │   └── out
│   │       ├── persistence   # TemplateRepositoryPort (ya existe), StatusRepositoryPort
│   │       └── externalapi   # NotificationSenderPort (SNS/SES/WA/FCM)
│   ├── service
│   │   └── impl           # TemplateServiceImpl, NotificationProcessorServiceImpl
│   └── util               # render de {{var}}, validación de variables
├── domain
│   ├── enums              # TemplateType, AlertCategory, AudienceMode, NotificationChannel
│   ├── exception          # TemplateNotFound, InvalidTemplate, ...
│   └── model              # NotificationTemplate, NotificationMessage, Audience, AlertMeta...
└── infrastructure
    ├── adapter
    │   ├── input
    │   │   └── web
    │   │       └── controller   # TemplateAdminController (CRUD), consumers
    │   └── out
    │       └── persistence
    │           ├── adapter      # DynamoDbTemplateAdapter (versión v#ACTIVE)
    │           ├── entity        # item DynamoDB
    │           └── mapper        # dominio <-> item
    └── config              # AwsConfig, RabbitMqConfig, CacheConfig, OpenApiConfig
```

Reglas que ya respeta el core y debemos mantener:
- La capa `application`/`domain` **no** conoce DynamoDB/SNS/SES: todo pasa por
  **ports** implementados por adapters en `infrastructure`.
- Patrones ya presentes a conservar: **Template Method**
  (`AbstractNotificationTemplate` + subclases por tipo), **Factory**
  (`NotificationTemplateFactory`), **Router** por `eventType`
  (`FinancialEventRouter`). El nuevo `TemplateType` se integra con la Factory.

---

## 7. Ejemplos JSON por tipo

### SMS
```json
{
  "id": "VERIFICACION_OTP#SMS#es",
  "codigo": "VERIFICACION_OTP",
  "tipo": "SMS",
  "language": "es",
  "bodyTemplate": "FINDU: Tu código es {{otp_code}}. Expira en {{expiration_minutes}} min.",
  "variables": ["otp_code", "expiration_minutes"],
  "version": 1, "active": true,
  "audiencia": { "modo": "USUARIO", "userIds": [] }
}
```

### EMAIL
```json
{
  "id": "OFERTA_RECIBIDA#EMAIL#es",
  "codigo": "OFERTA_RECIBIDA",
  "tipo": "EMAIL",
  "language": "es",
  "subject": "Nueva oferta para {{servicio_nombre}}",
  "contentType": "HTML",
  "bodyTemplate": "<html>...{{user_name}}...{{valor_propuesto}}...</html>",
  "variables": ["user_name", "proveedor_nombre", "valor_propuesto", "servicio_nombre"],
  "version": 1, "active": true,
  "audiencia": { "modo": "TODOS" }
}
```

### WHATSAPP
```json
{
  "id": "OFERTA_RECIBIDA#WHATSAPP#es",
  "sk": "v#ACTIVE",
  "codigo": "OFERTA_RECIBIDA",
  "tipo": "WHATSAPP",
  "language": "es",
  "contentType": "TXT",
  "waTemplateName": "oferta_recibida_v1",
  "bodyTemplate": "Hola {{user_name}}, tienes una oferta de {{proveedor_nombre}} por {{valor_propuesto}} para {{servicio_nombre}}.",
  "variables": ["user_name", "proveedor_nombre", "valor_propuesto", "servicio_nombre"],
  "version": 1, "active": true,
  "audiencia": { "modo": "USUARIO", "userIds": [] }
}
```

### EVENTO
```json
{
  "id": "PAGO_COMPLETADO#EVENTO#es",
  "codigo": "PAGO_COMPLETADO",
  "tipo": "EVENTO",
  "language": "es",
  "rabbitExchange": "findu.notifications",
  "rabbitRoutingKey": "findu.notifications.push",
  "rabbitQueue": "findu.notifications.push",
  "eventType": "PAYMENT_COMPLETED",
  "bodyTemplate": "{\"notificationId\":\"{{notification_id}}\",\"templateCode\":\"PAGO_COMPLETADO\"}",
  "variables": ["notification_id"],
  "version": 1, "active": true,
  "audiencia": { "modo": "TODOS" }
}
```

### NOTIFICACION
```json
{
  "id": "OFERTA_ACEPTADA#NOTIFICACION#es",
  "codigo": "OFERTA_ACEPTADA",
  "tipo": "NOTIFICACION",
  "language": "es",
  "title": "Oferta aceptada",
  "bodyTemplate": "¡Tu oferta para {{servicio_nombre}} fue aceptada! Fecha: {{fecha_programada}}",
  "variables": ["servicio_nombre", "fecha_programada"],
  "routing": {
    "condicion": "isUserActivo",
    "canalSiActivo": "WEBSOCKET",
    "canalSiInactivo": "PUSH"
  },
  "version": 1, "active": true,
  "audiencia": { "modo": "USUARIO", "userIds": [] }
}
```

### ALERTA
```json
{
  "id": "VERIFICA_CORREO#ALERTA#es",
  "codigo": "VERIFICA_CORREO",
  "tipo": "ALERTA",
  "language": "es",
  "categoria": "SEGURIDAD",
  "pantallaDestino": "perfil_proveedor",
  "titulo": "Verifica tu correo",
  "bodyTemplate": "Confirma tu correo y biometría para activar todas las funciones.",
  "ctaLabel": "Verificar ahora",
  "ctaAction": "findu://perfil/verificacion",
  "cerrable": false,
  "prioridad": 1,
  "condicionAparicion": "emailVerified == false || biometriaVerificada == false",
  "tiempoDesaparicion": 0,
  "variables": [],
  "version": 1, "active": true,
  "audiencia": { "modo": "SEGMENTO", "filtros": { "rol": "PROVEEDOR" } }
}
```

```json
{
  "id": "PROMO_NAVIDAD#ALERTA#es",
  "codigo": "PROMO_NAVIDAD",
  "tipo": "ALERTA",
  "language": "es",
  "categoria": "INFO",
  "pantallaDestino": "home",
  "titulo": "Promo de diciembre",
  "bodyTemplate": "20% en servicios del hogar hasta fin de mes.",
  "ctaLabel": "Conocer más",
  "ctaAction": "findu://promos/navidad",
  "cerrable": true,
  "prioridad": 2,
  "vigenciaDesde": "2026-12-01T00:00:00Z",
  "vigenciaHasta": "2026-12-31T23:59:59Z",
  "tiempoDesaparicion": 0,
  "variables": [],
  "version": 1, "active": true,
  "audiencia": { "modo": "TODOS" }
}
```

---

## 8. Puntos abiertos / decisiones

- **(A)** _AJUSTABLE_ — ¿Front evalúa SEGURIDAD/PREVENTIVA y backend solo entrega?
  ¿O backend evalúa todo y expone `GET /alertas/activas`? (recomendación: híbrido, §5)
- **(B)** _AJUSTABLE_ — Filtros concretos del `SEGMENTO` de audiencia (§4).
- **(C)** ✅ **DECIDIDO** — `WHATSAPP` pasa a ser **tipo** funcional de primer nivel. 6 tipos.
- **(D)** ✅ **DECIDIDO** — Versionado **con histórico** (ver §9): cada edición crea
  versión nueva + puntero `v#ACTIVE`; nunca se sobrescribe; permite rollback.

> A y B quedan como recomendación por defecto (ajustables cuando quieras). C y D
> cerrados. Siguiente paso cuando des luz verde: planificar la refactorización
> (`TemplateType` enum, ampliar `NotificationTemplate`, tabla con PK+SK versionada,
> GSIs, endpoints admin CRUD) siguiendo el layout hexagonal del core (§10).
