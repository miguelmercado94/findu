# Despachador de Notificaciones y WebSockets (`findu-notification-dispatcher`)

Servicio reactivo basado en WebFlux que gestiona conexiones bi-direccionales de WebSockets en tiempo real (`/ws/events`), presencia de dispositivos e ingesta HTTP de notificaciones.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Eureka Service Discovery (`http://localhost:8761`)
3. **`rabbitmq`**: Publicador/Consumidor AMQP (`localhost:5672`)
4. **`redis`**: Subscripción Pub/Sub e Idempotencia (`localhost:6379`)
5. **`localstack`**: DynamoDB para plantillas (`http://localhost:4566`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `findu-notification-dispatcher-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config Server URL |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host Redis |
| `RABBITMQ_HOST` | `localhost` | Host RabbitMQ |
| `DYNAMODB_ENDPOINT` | `http://localhost:4566` | DynamoDB LocalStack |

---

## 💻 Prueba Local

```bash
docker compose up -d notification-dispatcher

# Actuator Health Check
curl http://localhost:9000/actuator/health
```
