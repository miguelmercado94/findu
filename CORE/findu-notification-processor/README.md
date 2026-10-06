# Procesador de Notificaciones Multicanal (`findu-notification-processor`)

Consumidor asíncrono AMQP que procesa colas de RabbitMQ para envío multicanal: Push (FCM), Correo (AWS SES / Thymeleaf), SMS (AWS SNS), y WhatsApp Business Cloud API.

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
