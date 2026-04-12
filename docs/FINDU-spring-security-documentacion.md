# FINDU — Documentación técnica del módulo `findu-spring-security`

**Ubicación:** `SECURITY/findu-spring-security`  
**Stack:** Spring Boot 4, WebFlux, Spring Security reactivo, R2DBC (H2 en memoria por defecto), JWT (JJWT), DynamoDB opcional (tokens revocados), Redis opcional (caché de revocación), OpenAPI/Swagger.

> **Nota:** Este documento omite a propósito los controladores de ejemplo `ProductController` y `CategoryController`.

---

## 1. Árbol de paquetes (resumen)

```
com.findu.security
├── FinduSpringSecurityApplication
├── presentation.controller     → REST (WebFlux)
├── application
│   ├── usecase / impl          → casos de uso (JwtManager, CustomerManager, recuperación password…)
│   ├── service / impl          → servicios (JWT, usuario, salud, revocación)
│   └── port.output             → puertos (persistencia, caché, email)
├── domain.model                → entidades de dominio (Usuario, Jwt, Rol, Operation…)
├── dto.request / dto.response  → records de API
├── infrastructure
│   ├── entity                  → mapeo R2DBC
│   ├── repository              → R2DBC + custom (user_rol, rol_operation)
│   ├── adapter (persistence, cache, email, dynamodb)
│   └── scheduler
├── config                      → seguridad WebFlux, Redis, Dynamo, R2DBC, OpenAPI, filtros
├── exception                   → errores JSON unificados
├── mapper                      → MapStruct (dominio ↔ entidad / DTO)
└── util                        → constantes, Base64, hash de tokens
```

---

## 2. Tabla — Capa de presentación (`presentation.controller`)

| Clase | Propósito | Dependencias / propiedades | Endpoints / métodos principales |
|--------|-----------|------------------------------|----------------------------------|
| `AuthController` | API de autenticación: login, refresh, logout, validación de token, recuperación de contraseña | `JwtManager`, `RequestPasswordRecoveryUseCase`, `ResetPasswordUseCase` | `GET /api/v1/auth/validate`, `POST /api/v1/auth/login`, `/logout`, `/refresh`, `/forgot-password`, `/reset-password` |
| `CustomerController` | Registro público y listado de clientes (protegido) | `UsuarioService`, `CustomerManager` | `GET /api/v1/customers` (auth), `POST /api/v1/customers` (público; header `X-JWT-Algorithm`) |
| `ProfileController` | Perfil del usuario autenticado | `JwtManager` | `GET /api/v1/profile` |
| `HealthController` | Salud de la aplicación (público) | `HealthApplicationService` | `GET /api/public/health` |

---

## 3. Tabla — Casos de uso (`application.usecase` + `impl`)

| Clase | Tipo | Propósito | Métodos / notas |
|--------|------|-----------|-----------------|
| `JwtManager` | interface | Orquesta login, refresh, emisión de tokens tras registro, logout, validación y perfil | `login`, `refresh`, `buildTokensForUser`, `logout`, `validateToken`, `getCurrentUserProfile` |
| `JwtManagerImpl` | `@Service` | Implementación: valida rol, autenticación reactiva, authorities desde `rol_operation` o defaults, integra `JwtTokenRevocationService` | Depende de `UsuarioService`, `JwtService`, `JwtSignerFactory`, repos de rol/user_rol/rol_operation, `ReactiveAuthenticationManager` |
| `CustomerManager` | interface | Registro de cliente con JWT | `registerNewCustomer` |
| `CustomerManagerImpl` | `@Service` | Persiste usuario, enlaza `user_rol`, genera tokens | Usa `UsuarioService`, `JwtManager`, codificación de password |
| `RequestPasswordRecoveryUseCase` | interface | Solicitud de recuperación de contraseña | `requestRecovery(emailOrUsername)` |
| `RequestPasswordRecoveryUseCaseImpl` | impl | Genera token en BD, envía email vía puerto | — |
| `ResetPasswordUseCase` | interface | Restablecer contraseña con token | `resetPassword(token, newPassword)` |
| `ResetPasswordUseCaseImpl` | impl | Valida token, actualiza password | — |

---

## 4. Tabla — Servicios de aplicación (`application.service` + `impl`)

| Clase | Propósito | Propiedades / deps | Métodos principales |
|--------|-----------|--------------------|---------------------|
| `JwtService` | Contrato JWT (generar, validar, parsear, tipo access/refresh) | — | `generateToken`, `isValid`, `parse`, `getExpiration`, `getHeader`, `isRefreshToken`, `isAccessToken` |
| `JwtServiceImpl` | JJWT + `JwtSigner` opcional; secreto desde `jwt.secret` (Base64 o texto) | `SecretKey signingKey` | Implementa `JwtService` |
| `UsuarioService` | Contrato de negocio sobre usuarios | — | `findAll`, `findById`, `getUserByUsername`, `getUserByEmail`, `getUserByUsernameWithRole`, `getUserByEmailWithRole`, `save`, `existsBy*` |
| `UsuarioServiceImpl` | Carga usuarios, roles vía `user_rol`, operaciones vía `rol_operation` o `SecurityConstants.DEFAULT_OPERATION_NAMES` | Varios puertos | Implementa flujos anteriores |
| `HealthApplicationService` | Salud agregada | — | `getHealth()` (Mono) |
| `HealthApplicationServiceImpl` | Implementación | — | — |
| `JwtTokenRevocationService` | Coordinación revocación: Dynamo como verdad, Redis opcional como caché | `RevokedTokenRepositoryPort`, `TokenRevocationCachePort`, `JwtService` | `isRevoked`, `registerIssuedPair`, `markSessionUnavailable`, `rotateSession` |

---

## 5. Tabla — Puertos de salida (`application.port.output`)

| Puerto | Propósito | Métodos destacados |
|--------|-----------|---------------------|
| `UsuarioRepositoryPort` | Persistencia de `Usuario` | `findAll`, `findById`, `getByUsername`, `findByEmail`, `save`, `existsBy*`, `updatePassword` |
| `RolRepositoryPort` | Roles por nombre/id | Consultas de rol |
| `UserRolRepositoryPort` | Tabla `user_rol` | Asociación usuario–rol |
| `RolOperationRepositoryPort` | Tabla `rol_operation` (en diagrama de dominio/BD: **permission**) | `findOperationsByRoleId` |
| `ModuloRepositoryPort` / `OperationRepositoryPort` | Módulos y operaciones | Según adapters |
| `PasswordRecoveryTokenRepositoryPort` | Tokens de recuperación | CRUD lógico |
| `RevokedTokenRepositoryPort` | Sesiones JWT en Dynamo o memoria | `saveTokenPair`, `markSessionUnavailable`, `isAccessBlocked`, `isRefreshBlocked`, `rotateSession` |
| `TokenRevocationCachePort` | Caché Redis de revocación | `isAccessRevokedInCache`, `isRefreshRevokedInCache`, `putRevokedSession`, `removeExpiredRevokedEntries` |
| `EmailSenderPort` | Envío de correos | `sendPasswordRecoveryEmail` |

---

## 6. Tabla — Dominio (`domain.model`)

| Clase | Propósito | Propiedades / notas |
|--------|-----------|---------------------|
| `Usuario` | Usuario de negocio + `UserDetails` | `id`, `username`, `email`, `phone`, `password`, `active`, `rol`, `grantedAuthorities`; `getAuthorities()` con fallback a operaciones por defecto |
| `Rol`, `Modulo`, `Operation` | Modelos de autorización / menú API | Campos alineados con tablas R2DBC |
| `Jwt`, `JwtHeader`, `JwtPayload`, `JwtClaims` | Representación lógica del JWT | Header/payload/claims; firma vía `JwtSigner` |
| `JwtSigner` (+ `JwtSignerH256`, `JwtSignerE256`, `JwtSignerR256`) | Firmar segmentos del JWT | Algoritmos múltiples vía fábrica |
| `JwtSignerFactory` / `JwtSignerFactoryImpl` | Crea signers según algoritmo (p. ej. HS256) | Usa bytes del secreto |
| `PasswordRecoveryToken` | Token de recuperación en dominio | — |
| `CachedTokenSession` | record para Redis: `jwt`, `jwtRefresh`, `available` | Usado en logout / caché |

---

## 7. Tabla — DTOs (records)

| DTO | Campos / uso |
|-----|----------------|
| `LoginRequest` | `usernameOrEmail`, `password`, `role` |
| `LogoutRequest` | access obligatorio, refresh opcional (según implementación) |
| `RefreshTokenRequest` | `refreshToken` |
| `ForgotPasswordRequest` | email o username |
| `ResetPasswordRequest` | `token`, `newPassword` |
| `UserRegisterDto` | `username`, `email`, `phone`, `password`, `roleName` |
| `AuthToken` | `jwt`, `jwtRefresh`, `available`; `loggedOut()` para logout |
| `ValidateTokenResponse` | Resultado de validación + header/payload decodificados |
| `SaveUserResponse` | Respuesta de registro con datos usuario + tokens |
| `CustomerResponse` | Vista pública de cliente |
| `UserProfileResponse` | Perfil desde JWT |
| `HealthResponse` | Estado de salud |
| `ErrorResponse` | record: `timestamp`, `status`, `error`, `message`, `path`, `method`, `backendMessage`, `details`; fábricas `of`, `accessDenied` |

---

## 8. Tabla — Infraestructura: entidades R2DBC (`infrastructure.entity`)

| Entidad | Tabla SQL | Campos principales |
|---------|-----------|---------------------|
| `UserEntity` | `user` | `id`, `phone`, `username`, `password`, `email`, `active`, auditoría |
| `RoleEntity` | `ROLE` | `id`, `name`, `active`, auditoría |
| `UserRolEntity` | `user_rol` | `roleId`, `userId`, `active`, auditoría |
| `RolOperationEntity` | `rol_operation` | `roleId`, `operationId`, `active`, auditoría *(en `security_model_db.drawio` el concepto se documentó como tabla **permission**; migración de nombre de tabla pendiente si se aplica en BD)* |
| `OperationEntity` | `OPERATION` | `id`, `path`, `name`, `httpMethod`, `moduleId`, `permiteAll`, `active` |
| `ModuleEntity` | `MODULE` | `id`, `name`, `pathBase`, `active` |
| `PasswordRecoveryTokenEntity` | `password_recovery_token` | `id`, `userId`, `token`, `expiresAt`, `used` |

---

## 9. Tabla — Repositorios y adapters destacados

| Componente | Rol |
|------------|-----|
| `UserRepository`, `RoleRepository`, `ModuleRepository`, `OperationRepository`, `PasswordRecoveryTokenRepository` | Spring Data R2DBC |
| `UserRolRepository` + `UserRolRepositoryImpl` | Consultas tabla `user_rol` |
| `RolOperationRepository` + `RolOperationRepositoryImpl` | Consultas `rol_operation` |
| `UsuarioRepoAdapter`, `RolRepoAdapter`, `ModuloRepoAdapter`, `OperationRepoAdapter`, `UserRolRepoAdapter`, `RolOperationRepoAdapter`, `PasswordRecoveryTokenRepoAdapter` | Implementan puertos de persistencia |
| `RevokedTokenDynamoAdapter` | Revocación en DynamoDB (cuando está habilitado) |
| `InMemoryRevokedTokenRepositoryAdapter` | Revocación en memoria (desarrollo / Dynamo desactivado) |
| `RedisTokenRevocationCacheAdapter` | Caché Redis si `findu.redis.enabled=true` |
| `NoOpTokenRevocationCacheAdapter` | No-op si Redis deshabilitado (por defecto) |
| `LoggingEmailSenderAdapter` | Implementa `EmailSenderPort` logueando el enlace |
| `RevokedTokenTableInitializer` | `ApplicationRunner`: crea tabla Dynamo si procede |
| `RevokedTokenRedisEvictionScheduler` | `@Scheduled`: limpia entradas expiradas en Redis |

---

## 10. Tabla — Configuración y seguridad

| Clase | Función |
|--------|---------|
| `SecurityConfig` (`config.security`) | Cadena WebFlux: CSRF off, sin sesión server-side, `JwtAuthenticationFilter`, JSON 401/403; solo OpenAPI/Swagger (`/v3/api-docs/**`, `/swagger-ui*`) con `permitAll()`; el resto pasa por `FinduReactiveAuthorizationManager` y `operation.permite_all` en BD |
| `SecurityBeansInjector` (`config.security.authentication`) | `PasswordEncoder` (BCrypt), `JwtSignerFactory`, `ReactiveUserDetailsService`, `ReactiveAuthenticationManager` |
| `JwtAuthenticationFilter` (`config.security.authentication.filter`) | Valida Bearer = solo **access**; comprueba revocación vía `JwtTokenRevocationService`; carga `Usuario` en contexto |
| `JwtServerAuthenticationConverter` (`config.security.authentication`) | Conversión de autenticación en cadena reactiva |
| `FinduReactiveAuthorizationManager` (`config.security.authorization`) | Resuelve operación por `module.path_base` (= `spring.webflux.base-path`) + `operation.path` + método; sin token solo si `permite_all`; con JWT exige authority `operation.name` |
| `JsonAuthenticationEntryPoint` / `JsonAccessDeniedHandler` | Respuestas JSON en 401/403 |
| `SecurityErrorResponseWriter` | Escribe cuerpo de error en el filtro |
| `RedisReactiveConfig` | `@ConditionalOnProperty findu.redis.enabled=true`: bean `ReactiveRedisTemplate` cualificado |
| `RedisCacheProperties` | `findu.redis.*`: `enabled`, `commandTimeoutMs`, `evictionIntervalMs`, `keyPrefix` |
| `DynamoDbClientConfig` / `DynamoDbProperties` | Cliente AWS y tabla de tokens revocados |
| `R2dbcConfig`, `R2dbcAuditingConfig` | R2DBC y auditoría de columnas |
| `OpenApiConfig` | Swagger / OpenAPI 3 |
| `DotEnvBootstrap` | Carga `.env` antes del arranque (usado en `main`) |

---

## 11. Tabla — Excepciones y utilidades

| Clase | Propósito |
|--------|-----------|
| `GlobalExceptionHandler` | `@RestControllerAdvice`: 404 `ResourceNotFoundException`, 400 `IllegalArgumentException`, 403 `AccessDeniedException`, 500 resto |
| `ErrorResponse` | Formato único de error; 403 con mensaje en español y `path`/`method` |
| `ResourceNotFoundException` | 404 |
| `TokenHashUtils` | Hash de tokens para almacenamiento/comparación |
| `Base64Util` | Base64 URL-safe / estándar para JWT |
| `SecurityConstants` | Rutas públicas, header `X-JWT-Algorithm`, `typ` access/refresh, lista de operaciones por defecto *(incluye nombres de ejemplo tipo PRODUCT/CATEGORY hasta poblar BD)* |

---

## 12. Resumen de lo hecho hasta ahora (línea de trabajo)

- **Módulo de seguridad reactivo** con arquitectura hexagonal (puertos/adapters), JWT con access/refresh, roles y operaciones desde R2DBC (`user`, `ROLE`, `user_rol`, `OPERATION`, `MODULE`, `rol_operation`).
- **Revocación de sesiones**: persistencia en **DynamoDB** (o **memoria** si Dynamo va desactivado) + **caché Redis opcional** para comprobar revocación rápida; logout y refresh actualizan/rotan sesiones.
- **API REST** documentada (Swagger), manejo de errores unificado (`ErrorResponse`), **403** alineado con mensaje de negocio y metadatos (`path`, `method`, `backendMessage`).
- **Recuperación de contraseña** con token en BD y envío de enlace (adapter de email por logs).
- **Infra local**: `docker-compose` con LocalStack (Dynamo) y Redis; `application.yml` con R2DBC H2, JWT, Actuator.
- **Diagrama de BD** (`security_model_db.drawio`): tabla intermedia documentada como **`permission`** (en código la tabla puede seguir como `rol_operation` hasta migración).
- **Git**: rama `HU001`, dependencia explícita `spring-data-redis` y ajustes de excepciones; push a `origin`.

---

## 13. Funcionamiento global (flujos)

1. **Login** (`POST /api/v1/auth/login`): body con usuario/email, password y **nombre de rol**. Se valida que el rol exista, `ReactiveAuthenticationManager` autentica contra `Usuario` (password BCrypt), se enriquece el usuario con rol vía `user_rol` y **authorities** vía `rol_operation` → `Operation`, o con operaciones por defecto si no hay filas. Se generan JWT access (`typ` JWT) y refresh (`typ` JWTRefresh) con `JwtService` / `JwtSignerFactory` según header `X-JWT-Algorithm` (p. ej. HS256). Se registra el par en almacén de revocación (`registerIssuedPair`).

2. **Peticiones autenticadas**: header `Authorization: Bearer <access>`. `JwtAuthenticationFilter` valida firma y expiración, rechaza si es refresh usado como Bearer, consulta **revocación** (Redis primero si está activo, luego Dynamo/memoria), carga `Usuario` y establece el contexto de seguridad reactivo.

3. **Refresh** (`POST /api/v1/auth/refresh`): valida refresh, comprueba bloqueo, rota sesión en almacén (`rotateSession`), emite nuevo par.

4. **Logout** (`POST /api/v1/auth/logout`): marca sesión no disponible en Dynamo y refleja en Redis (`markSessionUnavailable`); respuesta `AuthToken.loggedOut()`.

5. **Registro** (`POST /api/v1/customers`): crea usuario, asocia rol, devuelve `SaveUserResponse` con tokens.

6. **Perfil** (`GET /api/v1/profile`): requiere access válido; datos desde contexto / `JwtManager.getCurrentUserProfile()`.

7. **Validación de token** (`GET /api/v1/auth/validate`): público; devuelve si el token es válido y metadatos decodificados.

---

*Documento generado para apoyo a diagramas (draw.io) y revisión en NotebookLM / exportación a PDF.*
