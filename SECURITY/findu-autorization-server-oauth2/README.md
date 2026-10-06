# Servidor de Autorización OAuth2 (`autorization-server-oauth2`)

Servidor OAuth2 / OIDC basado en Spring Authorization Server 1.x para flujos de login social, clientes OAuth2 y consentimiento de scopes.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Registro en Eureka (`http://localhost:8761`)
3. **`postgres-oauth2`**: Base de datos PostgreSQL (`localhost:5432/findu_oauth2`)
4. **`findu-spring-security`**: API base de autenticación (`http://localhost:8081/security-auth`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `autorization-server-oauth2-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | URL del servidor de configuración |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/findu_oauth2` | JDBC URL PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `findu_oauth2` | Usuario PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | `findu_oauth2_password` | Clave PostgreSQL |
| `FINDU_SECURITY_API_BASE_URL` | `http://localhost:8081/security-auth` | Endpoint interno de comunicación con Security |

---

## 💻 Prueba Local

```bash
docker compose up -d autorization-server-oauth2

# Health check
curl http://localhost:9595/authorization-server/actuator/health
```
