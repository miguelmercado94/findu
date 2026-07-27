# FIND-U

Plataforma de conexión entre clientes y proveedores de servicios. Arquitectura de microservicios con Spring Boot 3, Spring Cloud, y frontends SPA.

---

## Estructura del Proyecto

```
findu/
├── ARCHITECTURE/
│   ├── eureka-server/          # Service Discovery (Netflix Eureka)
│   └── api-gateway/            # API Gateway (Spring Cloud Gateway)
├── SECURITY/
│   ├── findu-spring-security/  # Auth, JWT, perfiles, recuperación (WebFlux/R2DBC)
│   └── autorization-server-oauth2/  # OAuth2 Authorization Server (JPA)
├── FRONTEND/
│   ├── cliente/                # Portal web Clientes (Vite + JS Vanilla)
│   └── proveedor/              # Portal web Proveedores (Vite + JS Vanilla)
├── docker-compose.yml          # Stack completo local
└── docs/                       # Diagramas de arquitectura
```

---

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Service Discovery | Netflix Eureka Server |
| API Gateway | Spring Cloud Gateway (Netty) |
| Auth & Security | Spring WebFlux, R2DBC, JWT (HS256), Google OIDC |
| Authorization Server | Spring Authorization Server, JPA, PostgreSQL |
| Base de datos | PostgreSQL 16 |
| Contenedores | Docker, Docker Compose |
| Frontend Web | JavaScript Vanilla + Vite |
| Java | JDK 21, Spring Boot 3.4.2, Spring Cloud 2024.0.0 |

---

## Inicio Rápido

### Requisitos:
- Docker Desktop
- Node.js (v18+)
- Puertos libres: 8761, 8080, 3000, 3001

### Levantar el stack:

```bash
# 1. Backend (microservicios + BDs)
docker compose up -d

# 2. Frontend cliente
cd FRONTEND/cliente
npm install
npm run dev

# 3. Frontend proveedor (otra terminal)
cd FRONTEND/proveedor
npm install
npm run dev
```

### URLs:

| Servicio | URL | Acceso |
|----------|-----|--------|
| Eureka Dashboard | http://localhost:8761 | Público |
| API Gateway | http://localhost:8080 | Punto de entrada API |
| Portal Clientes | http://localhost:3000 | Público |
| Portal Proveedores | http://localhost:3001 | Público |

---

## Imágenes Docker Hub

| Imagen | Tag |
|--------|-----|
| `mmercado94/findu-eureka-server` | 1.0.0 |
| `mmercado94/findu-api-gateway` | 1.0.0 |
| `mmercado94/findu-spring-security` | 1.0.0 |
| `mmercado94/findu-autorization-server-oauth2` | 1.0.0 |

---

## Arquitectura de Red (Docker Compose)

```
                    ┌─ Host ──────────────────────────────────┐
                    │                                          │
  localhost:3000 ◀──┤── Frontend Cliente (Vite)                │
  localhost:3001 ◀──┤── Frontend Proveedor (Vite)              │
                    │                                          │
  localhost:8761 ◀──┤── ┌────────────────────────────────────┐ │
  localhost:8080 ◀──┤── │  Red Docker: findu-internal        │ │
                    │   │                                    │ │
                    │   │  eureka-server (:8761)             │ │
                    │   │  api-gateway (:8080)               │ │
                    │   │  findu-spring-security (:8081)     │ │
                    │   │  autorization-server-oauth2 (:9595)│ │
                    │   │  postgres-security (:5432)         │ │
                    │   │  postgres-oauth2 (:5432)           │ │
                    │   └────────────────────────────────────┘ │
                    └──────────────────────────────────────────┘
```

- Solo eureka y gateway exponen puertos al host.
- Los servicios de seguridad y BDs son accesibles únicamente dentro de la red Docker.
- Los frontends corren localmente (no en Docker) y se comunican via `localhost:8080`.

---

## Documentación por Módulo

- [Frontend (endpoints, flujos, Google Sign-In)](./FRONTEND/README.md)
- [API Gateway (rutas, CORS)](./ARCHITECTURE/api-gateway/README.md)
- [Eureka Server](./ARCHITECTURE/eureka-server/README.md)

---

## Variables de Entorno Principales

El `docker-compose.yml` ya incluye todas las variables necesarias. Para desarrollo local sin Docker, revisar:

- `SECURITY/findu-spring-security/.env.example`
- `FRONTEND/cliente/.env`
- `FRONTEND/proveedor/.env`
