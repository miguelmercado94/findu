# Microservicio de Ayuda y Soporte (`findu-help-v2`)

Servicio responsable de la gestión de tickets de soporte técnico, reclamos, adjuntos multimedia y atención al cliente.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Eureka Service Discovery (`http://localhost:8761`)
3. **`mongo-help`**: MongoDB Database (`localhost:27017/findu_help`)
4. **`localstack`**: AWS S3 para adjuntos multimedia (`http://localhost:4566`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `findu-help-v2-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | URL del Config Server |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/findu_help` | Conexión MongoDB |
| `S3_ENDPOINT` | `http://localhost:4566` | Endpoint S3 LocalStack |

---

## 💻 Prueba Local

```bash
docker compose up -d findu-help-v2

# Health check / Tickets endpoint
curl http://localhost:8085/api/v1/tickets
```
