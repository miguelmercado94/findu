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
