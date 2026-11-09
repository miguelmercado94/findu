# Motor de Plantillas de Notificación (`findu-notification-processor`)

Motor de plantillas **dinámico y gestionable por un rol admin**: un administrador puede crear y configurar plantillas sin desplegar código. Soporta **6 tipos funcionales**:

| Tipo | Qué es | Canal de salida |
| :--- | :--- | :--- |
| `SMS` | Mensaje de texto | AWS SNS |
| `EMAIL` | Correo (TXT/HTML) | AWS SES |
| `WHATSAPP` | Mensaje de WhatsApp | WhatsApp Business Cloud API |
| `EVENTO` | Mensaje entre microservicios | Cola RabbitMQ declarada en la plantilla |
| `NOTIFICACION` | Push en tiempo real | WebSocket si el usuario está activo, con fallback a FCM push |
| `ALERTA` | Banner/aviso en pantalla con metadata UI | Entregada al front por pantalla |

### Almacenamiento versionado (DynamoDB)

Tabla `notification_templates` con clave compuesta **PK `id` = `CODE#TIPO#LANG`** + **SK `sk`**:
- `v#<n>` — cada versión inmutable.
- `v#ACTIVE` — puntero desnormalizado (`active_version`) resuelto en 1 solo `GetItem` en el hot path.

Cada edición crea una versión nueva (histórico completo + rollback). GSIs: `GSI_tipo`, `GSI_pantalla`, `GSI_codigo`. El seed de ejemplo está en `src/main/resources/db/dynamodb/seed-templates.sh`.

Los endpoints de administración (CRUD, histórico, activación/rollback) exponen la gestión de plantillas al rol admin.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Eureka Service Discovery (`http://localhost:8761`)
3. **`rabbitmq`**: Broker AMQP (`localhost:5672`, gestión en `localhost:15672`)
4. **`redis`**: Cache de idempotencia (`localhost:6379`)
5. **`localstack`**: AWS SES / SNS / DynamoDB (`http://localhost:4566`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `findu-notification-processor-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config Server URL |
| `RABBITMQ_HOST` | `localhost` | Host RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto RabbitMQ |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host Redis |
| `SES_ENDPOINT` | `http://localhost:4566` | SES LocalStack |
| `SNS_ENDPOINT` | `http://localhost:4566` | SNS LocalStack |

---

## 💻 Prueba Local

```bash
docker compose up -d findu-notification-processor

# Health Check
curl http://localhost:8083/actuator/health
```

---

## 🔔 Push FCM (Firebase Cloud Messaging)

El canal `PUSH` envía notificaciones reales vía **Firebase Admin SDK** (proyecto `achambear-5fae3`).
El flujo de fallback de chat de soporte (usuario offline) arma un push con `type=TICKET_CHAT` +
`ticketCode` en el `data`, que la app usa para navegar al chat del ticket.

### Resolución de la credencial (flexible y degradable)

`FirebaseConfig` resuelve la credencial del service account en este orden y degrada a **no-op** si
ninguna fuente aplica (no rompe el arranque, el push simplemente no se envía):

1. **JSON inline** — `findu.notification.firebase.credentials-json` (env `FIREBASE_CREDENTIALS_JSON`).
2. **Ruta a archivo** — `findu.notification.firebase.credentials-path` (env `FIREBASE_CREDENTIALS_PATH`).
3. **Default local** — `secrets/firebase-service-account.json` (relativo a la raíz del repo).
4. Si nada aplica → `log.warn` y Firebase queda deshabilitado (push no-op).

> La carpeta `secrets/` está en `.gitignore`. **Nunca** se commitea `secrets/firebase-service-account.json`
> ni se loguea el contenido de la credencial o el device token completo (se trunca).

### Railway (QA / PDN)

Usar una de las dos variables:

| Variable | Valor | Descripción |
| :--- | :--- | :--- |
| `FIREBASE_CREDENTIALS_JSON` | JSON inline (secret) | Contenido del service account como variable secreta |
| `FIREBASE_CREDENTIALS_PATH` | ruta (secret file) | Ruta al secret file montado con el service account |

### Tabla de device tokens (DynamoDB)

Tabla `findu_device_tokens` (propiedad `findu.notification.dynamodb.device-tokens-table`,
env `DYNAMODB_DEVICE_TOKENS_TABLE`):

- **PK**: `device_token` (S).
- Atributos: `username`, `platform`, `canonical_recipient`, `created_at`, `updated_at`.
- **GSIs**: `GSI_username` (PK `username`) y `GSI_canonical_recipient` (PK `canonical_recipient`).

El fallback resuelve los tokens por `canonical_recipient` = `PROVEEDOR_<proveedorId>`. Un token
`UNREGISTERED`/`INVALID_ARGUMENT` se elimina del store automáticamente. El DDL de referencia está en
`src/main/resources/db/dynamodb/create-device-tokens-table.sh`.

### Gateway y lookup a security

- **Gateway**: route `Path=/findu-notification-processor/**` → `lb://FINDU-NOTIFICATION-PROCESSOR`
  (sin `StripPrefix`); el processor expone `spring.webflux.base-path=/findu-notification-processor`.
- **Lookup username → proveedorId** (en el registro del token) vía security:

| Variable | Valor local | Descripción |
| :--- | :--- | :--- |
| `FINDU_SECURITY_URL` | `http://localhost:8081/security-auth` | Base URL de `findu-spring-security` |
| `FINDU_INTERNAL_SERVICE_KEY` | `findu-internal-saga-secret-key-2026` | Header `X-Internal-Service-Key` del endpoint interno |

### Verificación e2e (manual, no bloquea CI)

El envío FCM real contra `achambear-5fae3` y el route del gateway requieren el stack docker-compose
arriba y un device token real: es **manual**. Los tests unitarios mockean `FirebaseMessaging` y el
repositorio de tokens (no requieren infraestructura real).
