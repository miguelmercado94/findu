# Cierre de Balance Diario (`findu-transaction-daily-balance`)

Servicio / Lambda programado (Scheduled Cron) que procesa los cierres de caja y balances diarios de transacciones para proveedores.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Eureka Service Discovery (`http://localhost:8761`)
3. **`postgres-transaction`**: Base de Datos PostgreSQL (`localhost:5432/findu_transaction_db`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `findu-transaction-daily-balance-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config Server URL |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/findu_transaction_db` | Conexión JDBC PostgreSQL |
| `FINDU_DAILY_BALANCE_CRON` | `0 0 22 * * *` | Expresión Cron de ejecución |

---

## 💻 Prueba Local

```bash
docker compose up -d findu-transaction-daily-balance

# Health Check
curl http://localhost:8087/actuator/health
```
