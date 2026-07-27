# findu-api-gateway

API Gateway del ecosistema FIND-U, construido con **Spring Cloud Gateway** (reactivo/Netty) + **Netflix Eureka Client**.

Es el punto de entrada único para los frontends. Enruta las peticiones a los microservicios internos por service discovery.

---

## Configuración

| Propiedad | Valor |
|-----------|-------|
| Puerto | `8080` |
| Imagen Docker Hub | `mmercado94/findu-api-gateway:1.0.0` |
| Actuator | http://localhost:8080/actuator/health |

---

## Rutas

| Path en el Gateway | Servicio Eureka | Puerto Interno | Descripción |
|--------------------|-----------------|---------------|-------------|
| `/security-auth/**` | `FINDU-SPRING-SECURITY` | 8081 | Auth, perfiles, registro, recuperación |
| `/authorization-server/**` | `AUTORIZATION-SERVER-OAUTH2` | 9595 | Servidor OAuth2/OIDC |

El gateway usa `lb://SERVICE-NAME` (load balancer via Eureka) para resolver las instancias.

---

## CORS

Configuración global de CORS centralizada en el gateway:

| Propiedad | Valor |
|-----------|-------|
| Orígenes | `http://localhost:3000`, `http://localhost:3001`, `https://www.google.com` |
| Métodos | Todos (`*`) |
| Headers | Todos (`*`) |
| Credentials | `true` |

> Para un frontend móvil, no se necesita CORS (las apps nativas no tienen restricción de origen).

---

## Ejecución

### Con Docker (recomendado):

```bash
docker compose up -d    # Levanta todo el stack desde la raíz
```

El gateway arranca después de que eureka-server esté healthy.

### Local (desarrollo sin Docker):

```bash
cd ARCHITECTURE/api-gateway
./gradlew bootRun       # Linux/macOS
.\gradlew.bat bootRun   # Windows
```

Requiere JDK 21 y eureka-server corriendo en `localhost:8761`.

---

## Docker Compose

En el stack Docker:
- Corre en la red privada `findu-internal`
- Expone el puerto `8080` al host (único punto de acceso para los frontends)
- Se conecta a Eureka via `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- Las BDs y microservicios de seguridad NO tienen puertos expuestos al host — solo son accesibles via el gateway

---

## Notas para el Frontend Móvil

- La URL base para la app móvil en desarrollo local es `http://<IP_DEL_EQUIPO>:8080` (no `localhost`, ya que el emulador/dispositivo no resuelve localhost al host).
- Para Android emulator: `http://10.0.2.2:8080`
- Para iOS simulator: `http://localhost:8080`
- Para dispositivo físico: usar la IP LAN del equipo (ej. `http://192.168.1.X:8080`)
