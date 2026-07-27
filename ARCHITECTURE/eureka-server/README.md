# findu-eureka-server

Servidor de descubrimiento de servicios (**Netflix Eureka Server**) del ecosistema FIND-U.

Actúa como registro central: cada microservicio se registra al arrancar, permitiendo balanceo de carga reactivo y resolución de nombres internos en el API Gateway.

---

## Configuración

| Propiedad | Valor |
|-----------|-------|
| Puerto | `8761` |
| Dashboard | http://localhost:8761 |
| Imagen Docker Hub | `mmercado94/findu-eureka-server:1.0.0` |

---

## Ejecución

### Con Docker (recomendado):

```bash
docker compose up -d    # Levanta todo el stack desde la raíz del proyecto
```

El eureka-server es el primer servicio en arrancar. Los demás dependen de su healthcheck.

### Local (desarrollo sin Docker):

```bash
cd ARCHITECTURE/eureka-server
./gradlew bootRun       # Linux/macOS
.\gradlew.bat bootRun   # Windows
```

Requiere JDK 21.

---

## Servicios Registrados

Una vez el stack está arriba, el dashboard en http://localhost:8761 muestra:

| Servicio | Puerto Interno | Descripción |
|----------|---------------|-------------|
| API-GATEWAY | 8080 | Puerta de enlace principal |
| FINDU-SPRING-SECURITY | 8081 | Autenticación, perfiles, JWT |
| AUTORIZATION-SERVER-OAUTH2 | 9595 | Servidor OAuth2/OIDC |

---

## Docker Compose

En el `docker-compose.yml` del proyecto, eureka-server:
- Corre en la red privada `findu-internal`
- Expone el puerto `8761` al host (para visualizar el dashboard)
- Los demás servicios se registran usando `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/`
- Los servicios se registran con hostname (no IP) para correcta resolución DNS entre contenedores
