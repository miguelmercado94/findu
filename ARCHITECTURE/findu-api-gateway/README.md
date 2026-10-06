# API Gateway (`api-gateway`)

Punto de entrada único (Reverse Proxy / Routing / CORS) para las aplicaciones cliente frontend y la aplicación móvil Android de FIND-U.

---

## 🛠️ Servicios Necesarios

1. **`findu-config`** (`http://localhost:8888` en local o `http://findu-config:8888` en Docker)
2. **`eureka-server`** (`http://localhost:8761/eureka/`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local por Defecto | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Perfil activo (`local`, `dev`, `stage`, `prod`) |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | URL del Config Server |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | URL de registro en Eureka |

---

## 💻 Prueba en Local

### Rutas Principales Ruteadas por el Gateway:
- **Seguridad & Auth**: `http://localhost:8000/security-auth/**` → `FINDU-SPRING-SECURITY`
- **OAuth2 Server**: `http://localhost:8000/authorization-server/**` → `AUTORIZATION-SERVER-OAUTH2`
- **Core API**: `http://localhost:8000/findu-core/**` → `FINDU-CORE`
- **Transacciones**: `http://localhost:8000/findu-transaction/**` → `FINDU-TRANSACTION`
- **Soporte & Help**: `http://localhost:8000/findu-help-v2/**` → `FINDU-HELP-V2`
- **WebSockets / Dispatcher**: `http://localhost:8000/ws/events/**` → `NOTIFICATION-DISPATCHER`

### Ejecución Docker
```bash
docker compose up -d api-gateway
```
Salud: `http://localhost:8000/actuator/health`
