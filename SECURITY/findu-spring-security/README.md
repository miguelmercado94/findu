# Microservicio de Seguridad y Autenticación (`findu-spring-security`)

Servicio reactivo (Spring WebFlux / R2DBC) encargado de autenticación de usuarios, registro, emisión/validación de tokens JWT, OTPs en Redis y presencia/perfiles.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Service Discovery (`http://localhost:8761`)
3. **`postgres-security`**: PostgreSQL DB (`localhost:5432/findu` o contenedor `findu-postgres-security`)
4. **`redis`**: Cache / Tokens OTP (`localhost:6379`)
5. **`localstack`**: Simulación de AWS DynamoDB (`http://localhost:4566`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga config desde `findu-spring-security-local.yml` en Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Ubicación del Config Server |
| `SPRING_R2DBC_URL` | `r2dbc:postgresql://localhost:5432/findu` | Cadena de conexión R2DBC a PostgreSQL |
| `SPRING_R2DBC_USERNAME` | `findu` | Usuario DB |
| `SPRING_R2DBC_PASSWORD` | `findu` | Clave DB |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host Redis |
| `FINDU_AWS_DYNAMODB_ENDPOINT` | `http://localhost:4566` | Endpoint DynamoDB local (LocalStack) |

---

## 💻 Prueba Local

```bash
# Con Docker Compose
docker compose up -d findu-spring-security

# Verificación de salud
curl http://localhost:8081/security-auth/actuator/health
```
