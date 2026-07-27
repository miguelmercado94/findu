# FIND-U

Plataforma de servicios bajo demanda que conecta usuarios con proveedores de servicios profesionales (peluquería, enfermería, cuidado de niños, etc.) basados en geolocalización y presupuesto. Marketplace de Servicios Profesionales.

---

## Tabla de Contenidos

1. [Arquitectura General](#arquitectura-general)
2. [Microservicios](#microservicios)
3. [Componentes del Docker Compose](#componentes-del-docker-compose)
4. [Requisitos para Levantar el Proyecto](#requisitos-para-levantar-el-proyecto)
5. [Guía Paso a Paso](#guía-paso-a-paso)
6. [Variables de Entorno Configurables](#variables-de-entorno-configurables)
7. [Perfiles de Spring](#perfiles-de-spring)
8. [Imágenes Docker Hub](#imágenes-docker-hub)
9. [URLs del Stack Local](#urls-del-stack-local)
10. [Estructura del Proyecto](#estructura-del-proyecto)

---

## Arquitectura General

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           HOST (tu PC)                                   │
│                                                                         │
│  ┌─────────────┐  ┌──────────────┐                                     │
│  │  Frontend    │  │  Frontend     │     Vite (Node.js local)            │
│  │  Cliente     │  │  Proveedor    │     No Docker                       │
│  │  :3000       │  │  :3001        │                                     │
│  └──────┬───────┘  └──────┬────────┘                                     │
│         │                  │                                             │
│         └────────┬─────────┘                                             │
│                  ▼                                                        │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                  Red Docker: findu-internal                         │  │
│  │                                                                     │  │
│  │  ┌─────────────────┐     ┌──────────────────┐                      │  │
│  │  │  Eureka Server   │◀───│  API Gateway      │ ◀── :8080 (host)    │  │
│  │  │  :8761 (host)    │     │  :8080            │                      │  │
│  │  └────────┬──────────┘     └────────┬─────────┘                      │  │
│  │           │                          │  lb:// (load balancer)         │  │
│  │           │    ┌─────────────────────┼────────────────────┐          │  │
│  │           ▼    ▼                     ▼                    ▼          │  │
│  │  ┌──────────────────┐     ┌───────────────────────────┐             │  │
│  │  │ findu-spring-     │     │ autorization-server-       │             │  │
│  │  │ security :8081    │     │ oauth2 :9595               │             │  │
│  │  └────────┬──────────┘     └────────────┬──────────────┘             │  │
│  │           │                              │                            │  │
│  │           ▼                              ▼                            │  │
│  │  ┌──────────────────┐     ┌───────────────────────────┐             │  │
│  │  │ PostgreSQL        │     │ PostgreSQL                 │             │  │
│  │  │ (findu) :5432     │     │ (findu_oauth2) :5432       │             │  │
│  │  └──────────────────┘     └───────────────────────────┘             │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Solo el gateway (:8080) y Eureka (:8761)** tienen puertos expuestos al host.
- Las BDs y microservicios de seguridad están aislados en la red Docker interna.
- Los frontends corren localmente con Node.js y se comunican exclusivamente con el gateway.

---

## Microservicios

### eureka-server (Descubrimiento de Servicios)

| Propiedad | Valor |
|-----------|-------|
| Tecnología | Spring Cloud Netflix Eureka Server |
| Puerto | 8761 |
| Función | Registro central de servicios. Cada microservicio se registra al arrancar y el gateway los descubre automáticamente. |
| Dashboard | http://localhost:8761 |

### api-gateway (Puerta de Enlace)

| Propiedad | Valor |
|-----------|-------|
| Tecnología | Spring Cloud Gateway (reactivo, Netty) |
| Puerto | 8080 |
| Función | Punto de entrada único. Enruta peticiones por path prefix, maneja CORS centralizadamente. |

**Rutas configuradas:**

| Path | Servicio destino | Descripción |
|------|-----------------|-------------|
| `/security-auth/**` | FINDU-SPRING-SECURITY | Auth, perfiles, registro |
| `/authorization-server/**` | AUTORIZATION-SERVER-OAUTH2 | OAuth2/OIDC server |

### findu-spring-security (Autenticación y Perfiles)

| Propiedad | Valor |
|-----------|-------|
| Tecnología | Spring WebFlux, R2DBC, JWT (HS256), Google OIDC |
| Puerto interno | 8081 |
| Base path | `/security-auth` |
| BD | PostgreSQL (schema recreado en cada arranque con perfil dev) |
| Función | Login local/federado, registro, perfiles, recuperación de contraseña, autorización reactiva basada en operaciones. |

### autorization-server-oauth2 (Servidor de Autorización)

| Propiedad | Valor |
|-----------|-------|
| Tecnología | Spring Authorization Server, JPA, PostgreSQL |
| Puerto interno | 9595 |
| Base path | `/authorization-server` |
| BD | PostgreSQL (schema recreado en cada arranque con perfil dev) |
| Función | Servidor OAuth2/OIDC estándar. Valida tokens emitidos por findu-security. |

### Frontends (Cliente y Proveedor)

| Propiedad | Valor |
|-----------|-------|
| Tecnología | JavaScript Vanilla + Vite |
| Puertos | 3000 (cliente), 3001 (proveedor) |
| Función | Portales SPA para usuarios y proveedores. Autenticación local + Google Sign-In. |

---

## Componentes del Docker Compose

| Servicio | Imagen | Rol | Puerto host | Depende de |
|----------|--------|-----|-------------|------------|
| `eureka-server` | `mmercado94/findu-eureka-server:1.0.0` | Service Discovery | 8761 | — |
| `api-gateway` | `mmercado94/findu-api-gateway:1.0.0` | API Gateway | 8080 | eureka-server |
| `postgres-security` | `postgres:16-alpine` | BD para findu-security | — (interno) | — |
| `postgres-oauth2` | `postgres:16-alpine` | BD para oauth2 server | — (interno) | — |
| `findu-spring-security` | `mmercado94/findu-spring-security:1.0.0` | Auth/Perfiles/JWT | — (interno) | eureka, postgres-security |
| `autorization-server-oauth2` | `mmercado94/findu-autorization-server-oauth2:1.0.0` | OAuth2 Server | — (interno) | eureka, postgres-oauth2, security |

**Red:** `findu-internal` (bridge) — aislamiento completo.

**Volúmenes:** `findu-pg-security-data`, `findu-pg-oauth2-data` — persistencia de datos PostgreSQL.

---

## Requisitos para Levantar el Proyecto

| Requisito | Versión mínima | Para qué |
|-----------|---------------|----------|
| Docker Desktop | 4.x | Levantar el backend completo |
| Node.js | 18+ | Correr los frontends con Vite |
| npm | 9+ | Instalar dependencias frontend |
| Git | 2.x | Clonar el repositorio |

**Puertos que deben estar libres:** 8761, 8080, 3000, 3001

**NO necesitas instalar:** Java, Gradle, PostgreSQL, Redis — todo corre en Docker.

---

## Guía Paso a Paso

### 1. Clonar el repositorio

```bash
git clone https://github.com/miguelmercado94/findu.git
cd findu
```

### 2. Levantar el backend (Docker Compose)

```bash
docker compose up -d
```

Espera ~60 segundos a que todos los servicios estén healthy:

```bash
docker compose ps
```

Deberías ver todos los contenedores con status `(healthy)`.

### 3. Levantar el frontend cliente

```bash
cd FRONTEND/cliente
npm install
npm run dev
```

Abre http://localhost:3000

### 4. Levantar el frontend proveedor (otra terminal)

```bash
cd FRONTEND/proveedor
npm install
npm run dev
```

Abre http://localhost:3001

### 5. Verificar que todo funciona

- Eureka Dashboard: http://localhost:8761 (deberías ver 3 servicios registrados)
- Gateway Health: http://localhost:8080/actuator/health
- Login en el frontend con usuario `lmarquez` / contraseña `Password123*`

### 6. Apagar todo

```bash
# Backend
docker compose down

# Para borrar los datos de BD también:
docker compose down -v
```

---

## Variables de Entorno Configurables

### findu-spring-security

Estas variables se pueden agregar en el bloque `environment` del servicio `findu-spring-security` en `docker-compose.yml`:

#### Perfil y Despliegue

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil Spring activo: `dev`, `qa`, `pdn` |
| `FINDU_DEPLOYMENT` | `local` | Identificador del entorno de despliegue |

#### JWT

| Variable | Default | Descripción |
|----------|---------|-------------|
| `JWT_SECRET` | (clave Base64 por defecto) | Clave secreta HS256 (≥256 bits). Puede ser texto plano o Base64. |
| `JWT_ACCESS_EXPIRATION_SECONDS` | `300` (5 min) | Tiempo de vida del access token |
| `JWT_REFRESH_EXPIRATION_SECONDS` | `604800` (7 días) | Tiempo de vida del refresh token |

#### Base de Datos (R2DBC PostgreSQL)

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SPRING_R2DBC_URL` | `r2dbc:postgresql://localhost:5432/findu` | URL de conexión R2DBC |
| `SPRING_R2DBC_USERNAME` | `findu` | Usuario de BD |
| `SPRING_R2DBC_PASSWORD` | `findu` | Contraseña de BD |
| `SPRING_SQL_INIT_MODE` | `always` (dev) | `always` recrea schema en cada arranque, `never` para producción |

#### Redis (Caché de Revocación de Tokens)

| Variable | Default | Descripción |
|----------|---------|-------------|
| `FINDU_REDIS_ENABLED` | `false` | Activa/desactiva la revocación de tokens vía Redis |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host de Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Puerto de Redis |
| `SPRING_DATA_REDIS_TIMEOUT` | `300ms` | Timeout de lectura |
| `SPRING_DATA_REDIS_CONNECT_TIMEOUT` | `500ms` | Timeout de conexión |
| `FINDU_REDIS_COMMAND_TIMEOUT_MS` | `300` | Timeout de comandos Redis (ms) |
| `FINDU_REDIS_EVICTION_INTERVAL_MS` | `3600000` | Intervalo de limpieza de tokens expirados |
| `FINDU_REDIS_KEY_PREFIX` | `findu:revoked:` | Prefijo para las keys en Redis |
| `MANAGEMENT_HEALTH_REDIS_ENABLED` | `false` | Incluir Redis en el health check |

#### AWS DynamoDB (Revocación Alternativa de Tokens)

| Variable | Default | Descripción |
|----------|---------|-------------|
| `FINDU_AWS_DYNAMODB_ENABLED` | `false` | Activa revocación vía DynamoDB |
| `FINDU_AWS_DYNAMODB_ENDPOINT` | `http://localhost:4566` | Endpoint (LocalStack o AWS) |
| `FINDU_AWS_DYNAMODB_REGION` | `us-east-1` | Región AWS |
| `FINDU_AWS_DYNAMODB_REVOKED_TOKENS_TABLE` | `findu_revoked_tokens` | Nombre de la tabla |
| `FINDU_AWS_DYNAMODB_CREATE_TABLE_IF_NOT_EXISTS` | `true` | Crear tabla automáticamente |

#### Recuperación de Contraseña

| Variable | Default | Descripción |
|----------|---------|-------------|
| `FINDU_AUTH_RESET_PASSWORD_BASE_URL` | `http://localhost:3000/reset-password` | URL base para links de recovery |
| `FINDU_AUTH_RECOVERY_TOKEN_EXPIRY_MINUTES` | `60` | Expiración del token de recovery |

#### Eureka (Service Discovery)

| Variable | Default | Descripción |
|----------|---------|-------------|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | URL del servidor Eureka |
| `EUREKA_INSTANCE_PREFER_IP_ADDRESS` | `true` | Usar IP vs hostname para registro |
| `EUREKA_INSTANCE_HOSTNAME` | (hostname del contenedor) | Hostname con el que se registra en Eureka |

#### Logging (Niveles de Log)

Valores posibles: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`

| Variable | Default (dev) | Default (qa/pdn) | Paquete que controla |
|----------|--------------|-----------------|---------------------|
| `LOG_LEVEL_ROOT` | `DEBUG` | `INFO` | Todo el framework |
| `LOG_LEVEL_FINDU` | `DEBUG` | `INFO` | `com.findu.security` (lógica principal) |
| `LOG_LEVEL_FINDU_SECURITY_FILTER` | `DEBUG` | `INFO` | Filtro de autenticación JWT |
| `LOG_LEVEL_FINDU_USUARIO` | `DEBUG` | `INFO` | Servicio de usuarios |
| `LOG_LEVEL_FINDU_RECOVERY` | `DEBUG` | `INFO` | Recuperación de contraseña (request) |
| `LOG_LEVEL_FINDU_RESET` | `DEBUG` | `INFO` | Reset de contraseña (confirmación) |
| `LOG_LEVEL_FINDU_ADAPTER` | `DEBUG` | `INFO` | Adaptadores de persistencia |
| `LOG_LEVEL_FINDU_CACHE` | `INFO` | `INFO` | Caché Redis/DynamoDB |
| `LOG_LEVEL_SPRING_SECURITY` | `DEBUG` | `INFO` | Spring Security interno |
| `LOG_LEVEL_R2DBC` | `DEBUG` | `WARN` | Queries SQL R2DBC (muy verboso en DEBUG) |

**Ejemplo para agregar en docker-compose.yml:**

```yaml
findu-spring-security:
  environment:
    # ... otras variables ...
    LOG_LEVEL_ROOT: "INFO"
    LOG_LEVEL_FINDU: "DEBUG"
    LOG_LEVEL_R2DBC: "WARN"
    LOG_LEVEL_SPRING_SECURITY: "WARN"
```

> **Tip:** Para desarrollo, deja `LOG_LEVEL_R2DBC=WARN` y `LOG_LEVEL_SPRING_SECURITY=WARN` para reducir el ruido en los logs. Sube a DEBUG solo cuando necesites diagnosticar problemas de queries o autenticación.

### autorization-server-oauth2

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Perfil: `local` (H2), `dev` (PostgreSQL) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5434/findu_oauth2` | URL JDBC |
| `SPRING_DATASOURCE_USERNAME` | `findu_oauth2` | Usuario BD |
| `SPRING_DATASOURCE_PASSWORD` | `findu_oauth2_password` | Contraseña BD |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | URL Eureka |
| `FINDU_SECURITY_API_BASE_URL` | `http://localhost:8081/security-auth` | URL interna del servicio de seguridad |

### Frontends (.env)

| Variable | Descripción |
|----------|-------------|
| `VITE_GOOGLE_CLIENT_ID` | Client ID de Google Cloud para OAuth2/OIDC |
| `VITE_API_GATEWAY_URL` | URL del API Gateway (`http://localhost:8080`) |

---

## Perfiles de Spring

| Perfil | BD | Schema | Redis | DynamoDB | Logs | Uso |
|--------|----|---------|----|---------|------|-----|
| `dev` | PostgreSQL | `create-drop` (recrea en cada arranque) | Desactivado | Desactivado | DEBUG | Desarrollo local |
| `qa` | PostgreSQL | Manual/migración | Activado | Activado | INFO | Testing integrado |
| `pdn` | PostgreSQL | Producción | Activado | Activado | WARN | Producción |

El docker-compose actual usa perfil **dev** — la BD se recrea limpia en cada reinicio del contenedor.

---

## Imágenes Docker Hub

| Imagen | Tag | Base |
|--------|-----|------|
| `mmercado94/findu-eureka-server` | 1.0.0 | eclipse-temurin:21-jre-alpine |
| `mmercado94/findu-api-gateway` | 1.0.0 | eclipse-temurin:21-jre-alpine |
| `mmercado94/findu-spring-security` | 1.0.0 | eclipse-temurin:21-jre-alpine |
| `mmercado94/findu-autorization-server-oauth2` | 1.0.0 | eclipse-temurin:21-jre-alpine |

Todas usan multi-stage build (JDK 21 para compilar, JRE 21 Alpine para runtime).

---

## URLs del Stack Local

| Servicio | URL | Credenciales de prueba |
|----------|-----|----------------------|
| Eureka Dashboard | http://localhost:8761 | — |
| API Gateway | http://localhost:8080 | — |
| Portal Clientes | http://localhost:3000 | `lmarquez` / `Password123*` |
| Portal Proveedores | http://localhost:3001 | `lmarquez` / `Password123*` |
| Swagger (Security) | http://localhost:8080/security-auth/swagger-ui.html | — |

---

## Estructura del Proyecto

```
findu/
├── ARCHITECTURE/
│   ├── eureka-server/            # Netflix Eureka Server
│   │   ├── Dockerfile
│   │   ├── build.gradle
│   │   └── src/
│   └── api-gateway/              # Spring Cloud Gateway
│       ├── Dockerfile
│       ├── build.gradle
│       └── src/
├── SECURITY/
│   ├── findu-spring-security/    # Auth principal (WebFlux/R2DBC)
│   │   ├── Dockerfile
│   │   ├── .env.example
│   │   ├── build.gradle
│   │   └── src/
│   └── autorization-server-oauth2/  # OAuth2 Authorization Server
│       ├── Dockerfile
│       ├── build.gradle
│       └── src/
├── FRONTEND/
│   ├── README.md                 # Guía de endpoints y flujos (para frontend móvil)
│   ├── cliente/                  # Portal web clientes (Vite)
│   │   ├── .env
│   │   ├── package.json
│   │   ├── main.js
│   │   └── index.html
│   └── proveedor/                # Portal web proveedores (Vite)
│       ├── .env
│       ├── package.json
│       ├── main.js
│       └── index.html
├── docs/                         # Diagramas de arquitectura (.drawio, .png)
├── docker-compose.yml            # Stack completo
├── .env.example                  # Variables para docker-compose
└── README.md                     # Este archivo
```

---

## Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Lenguaje backend | Java | 21 (Temurin) |
| Framework | Spring Boot | 3.4.2 |
| Cloud | Spring Cloud | 2024.0.0 |
| Service Discovery | Netflix Eureka | — |
| API Gateway | Spring Cloud Gateway | Reactivo (Netty) |
| Security | Spring Security + JWT HS256 | — |
| Reactive DB | R2DBC + PostgreSQL | — |
| JPA DB | Hibernate + PostgreSQL | — |
| Base de datos | PostgreSQL | 16 |
| Contenedores | Docker + Docker Compose | — |
| Frontend | JavaScript Vanilla + Vite | 8.x |
| OAuth2/OIDC | Google Sign-In (GSI) | — |
