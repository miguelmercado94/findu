# Microservicio Core de Negocio (`findu-core`)

Microservicio Spring Boot responsable de la lógica central de negocio de FIND-U: gestión de perfiles de proveedores, categorías de servicios, catálogo y solicitudes.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Eureka Service Discovery (`http://localhost:8761`)
3. **`postgres-core`**: Base de Datos PostgreSQL (`localhost:5432/findu_core`)
4. **`redis`**: Cache de búsquedas y catálogo (`localhost:6379`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `findu-core-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config Server URL |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/findu_core` | Conexión JDBC PostgreSQL Core |
| `SPRING_DATASOURCE_USERNAME` | `findu_core` | Usuario DB |
| `SPRING_DATASOURCE_PASSWORD` | `findu_core` | Contraseña DB |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host Redis |

---

## 💻 Prueba Local

```bash
docker compose up -d findu-core

# Health check
curl http://localhost:8082/findu-core/actuator/health
```
