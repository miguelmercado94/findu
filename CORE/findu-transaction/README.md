# Microservicio de Transacciones y Pagos (`findu-transaction`)

Servicio responsable del registro y procesamiento de transacciones financieras, pagos de servicios y movimientos contables.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Service Discovery (`http://localhost:8761`)
3. **`postgres-transaction`**: Base de Datos PostgreSQL (`localhost:5432/findu_transaction_db`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `findu-transaction-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config Server URL |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/findu_transaction_db` | Conexión JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `findu_transaction` | Usuario DB |
| `SPRING_DATASOURCE_PASSWORD` | `findu_transaction` | Password DB |

---

## 💻 Prueba Local

```bash
docker compose up -d findu-transaction

# Actuator Health Check
curl http://localhost:8086/actuator/health
```
