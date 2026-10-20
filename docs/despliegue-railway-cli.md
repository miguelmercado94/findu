# FIND-U — Despliegue en Railway por CLI

> Guía de despliegue de los **microservicios** en Railway usando la **CLI**
> (`railway`). La infraestructura AWS (DynamoDB, SES, SNS) ya está creada con
> Terraform (ver `terraform/` y `docs/dependencias-servicios-externos.md`).
>
> **Alcance de esta fase:** solo backend por CLI. Sin frontends. Sin Terraform de
> Railway (se evaluará más adelante). Railway construye cada servicio desde su
> **Dockerfile**.

---

## 0. Principios (heredados del proyecto de referencia `bsg-production`)

- **Sin Eureka ni Config Server.** En Railway la comunicación entre servicios
  usa el **DNS privado interno** (`<servicio>.railway.internal`), no Eureka.
  Se arranca con `EUREKA_ENABLED=false` / `EUREKA_CLIENT_ENABLED=false` y las
  URLs internas se pasan por variables.
- **Solo el `api-gateway` se expone al público** (`*.up.railway.app`). El resto
  son servicios privados (sin dominio público).
- **Memoria acotada para minimizar costo** por servicio Java:
  `JAVA_TOOL_OPTIONS=-XX:+UseSerialGC -Xss256k -XX:MaxRAMPercentage=60.0 -Xmx256m`
- **Perfil Spring**: `qa` (Postgres + Redis activados, sin recreación destructiva).
- Postgres/Redis/RabbitMQ como servicios de Railway con **volumen** persistente.

---

## 1. Requisitos previos

```powershell
railway --version          # CLI instalada
railway whoami             # sesión iniciada
```

Infra AWS ya creada (Terraform). Credenciales para los micros:

```powershell
cd terraform
terraform output app_aws_access_key_id          # AWS_ACCESS_KEY_ID
terraform output -raw app_aws_secret_access_key # AWS_SECRET_ACCESS_KEY
```

Recursos AWS disponibles (región `us-east-1`):
- DynamoDB: `findu_revoked_tokens`, `notification_templates`, `findu_notifications`
- SES remitente verificado: `miguel.mercado@samtel.eu`
- SES/SNS destinos de prueba verificados: `m.mercado.t.94@gmail.com`, `+573003763300`

---

## 2. Crear el proyecto y entorno

```powershell
# Crear proyecto (o usar uno existente)
railway init --name findu-production
# Vincular la carpeta actual al proyecto
railway link --project findu-production --environment production
```

---

## 3. Mapa de servicios, puertos y dependencias

| Servicio Railway | Carpeta (Dockerfile) | Puerto | Público | Depende de |
|---|---|---|---|---|
| `postgres` | plugin/imagen `postgres:16` | 5432 | no | — |
| `redis` | plugin/imagen `redis:7` | 6379 | no | — |
| `rabbitmq` | imagen `rabbitmq:3.13-management` | 5672 | no | — |
| `api-gateway` | `ARCHITECTURE/findu-api-gateway` | 8080 | **sí** | security, oauth2, core |
| `findu-spring-security` | `SECURITY/findu-spring-security` | 8081 | no | postgres, redis, (DynamoDB AWS) |
| `autorization-server-oauth2` | `SECURITY/findu-autorization-server-oauth2` | 9595 | no | postgres, security |
| `findu-core` | `CORE/findu-core` | 8082 | no | postgres, redis |
| `findu-transaction` | `CORE/findu-transaction` | 8084 | no | postgres |
| `findu-help-v2` | `CORE/findu-help-v2` | 8085 | no | mongo, (S3/MinIO) |
| `findu-notification-processor` | `CORE/findu-notification-processor` | 8083 | no | rabbitmq, redis, (SES/SNS/DynamoDB AWS) |
| `findu-notification-dispatcher` | `CORE/LAMBDA/findu-notification-dispatcher` | 9000 | (ws) | rabbitmq, redis |

> **Fase 1 recomendada (núcleo mínimo):** postgres, redis, api-gateway,
> findu-spring-security, autorization-server-oauth2, findu-core.
> **Fase 2:** rabbitmq, notification-processor, notification-dispatcher, transaction, help-v2 (+ mongo/MinIO).

---

## 4. Bases de datos y broker

```powershell
# Postgres y Redis desde la plantilla de Railway (crean volumen automáticamente)
railway add --database postgres
railway add --database redis

# RabbitMQ como servicio de imagen (fase 2)
railway add --service rabbitmq --image rabbitmq:3.13-management-alpine
railway variables --service rabbitmq `
  --set "RABBITMQ_DEFAULT_USER=findu" `
  --set "RABBITMQ_DEFAULT_PASS=<password>"
```

> Las credenciales de Postgres/Redis quedan disponibles como variables de
> referencia de Railway (p. ej. `${{Postgres.PGPASSWORD}}`,
> `${{Redis.REDIS_URL}}`) y por DNS interno `postgres.railway.internal`,
> `redis.railway.internal`.

Crear bases/esquemas por servicio (una instancia Postgres, varias BD):
`security_db`, `oauth2_db`, `core_db`, `transaction_db`.

---

## 5. Desplegar un servicio (patrón general)

Para CADA microservicio: construir el jar, crear el servicio, setear variables, desplegar.

```powershell
# 1. Compilar el jar del micro (Railway construye con el Dockerfile, que espera el jar)
cd SECURITY/findu-spring-security
./gradlew clean bootJar -x test

# 2. Crear el servicio en Railway (si no existe)
railway add --service findu-spring-security

# 3. Setear variables (ver sección 6)
railway variables --service findu-spring-security --set "KEY=VALUE" --set ...

# 4. Desplegar el código (desde la carpeta del micro, usando su Dockerfile)
railway up --service findu-spring-security --detach
```

> `railway up` sube el contexto y Railway construye con el `Dockerfile` de la carpeta.
> Alternativa: conectar el repo de GitHub al servicio (deploy en cada push) —
> se evaluará en otra fase.

---

## 6. Variables de entorno por servicio

Comunes a todos los micros Java:

```
PORT=<puerto del micro>              # Railway enruta a este puerto
SPRING_PROFILES_ACTIVE=qa
JAVA_TOOL_OPTIONS=-XX:+UseSerialGC -Xss256k -XX:MaxRAMPercentage=60.0 -Xmx256m
EUREKA_CLIENT_ENABLED=false
EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
EUREKA_CLIENT_FETCH_REGISTRY=false
```

### api-gateway (8080, público)
> El gateway rutea por URL interna (sin `lb://` de Eureka). Las rutas usan el
> DNS interno de Railway. **Nota:** el `application.yml` actual usa `lb://` +
> Eureka; para Railway hay que sobreescribir cada `uri` con la URL interna, p.ej.
> vía variables `SPRING_CLOUD_GATEWAY_ROUTES_*_URI` o un perfil específico.
```
PORT=8080
SECURITY_SERVICE_URI=http://findu-spring-security.railway.internal:8081
OAUTH2_SERVICE_URI=http://autorization-server-oauth2.railway.internal:9595
CORE_SERVICE_URI=http://findu-core.railway.internal:8082
```

### findu-spring-security (8081)
```
PORT=8081
SPRING_R2DBC_URL=r2dbc:postgresql://postgres.railway.internal:5432/security_db
SPRING_R2DBC_USERNAME=postgres
SPRING_R2DBC_PASSWORD=${{Postgres.PGPASSWORD}}
FINDU_REDIS_ENABLED=true
SPRING_DATA_REDIS_HOST=redis.railway.internal
SPRING_DATA_REDIS_PORT=6379
# JWT
JWT_SECRET=<secreto base64 >=256 bits>
JWT_ACCESS_EXPIRATION_SECONDS=3600
JWT_REFRESH_EXPIRATION_SECONDS=604800
# DynamoDB real (AWS) — SIN endpoint (eso es solo LocalStack)
FINDU_AWS_DYNAMODB_ENABLED=true
FINDU_AWS_DYNAMODB_REGION=us-east-1
FINDU_AWS_DYNAMODB_REVOKED_TOKENS_TABLE=findu_revoked_tokens
AWS_ACCESS_KEY_ID=<app_aws_access_key_id>
AWS_SECRET_ACCESS_KEY=<app_aws_secret_access_key>
# Notificaciones (fase 2; en fase 1 dejar en false)
FINDU_NOTIFICATION_ENABLED=false
```

### autorization-server-oauth2 (9595)
```
PORT=9595
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/oauth2_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
FINDU_SECURITY_API_BASE_URL=http://findu-spring-security.railway.internal:8081/security-auth
```

### findu-core (8082)
```
PORT=8082
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/core_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
FINDU_CACHE_ENABLED=true
SPRING_DATA_REDIS_HOST=redis.railway.internal
SPRING_DATA_REDIS_PORT=6379
# Notificaciones (fase 2)
FINDU_NOTIFICATION_ENABLED=false
FINDU_NOTIFICATION_LAMBDA_URL=http://findu-notification-dispatcher.railway.internal:9000/dispatchNotification
```

### findu-notification-processor (8083) — fase 2
```
PORT=8083
RABBITMQ_HOST=rabbitmq.railway.internal
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=findu
RABBITMQ_PASSWORD=<password>
SPRING_DATA_REDIS_HOST=redis.railway.internal
SPRING_DATA_REDIS_PORT=6379
# AWS real — SIN *_ENDPOINT
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=<app_aws_access_key_id>
AWS_SECRET_ACCESS_KEY=<app_aws_secret_access_key>
DYNAMODB_TEMPLATES_TABLE=notification_templates
SES_FROM_EMAIL=miguel.mercado@samtel.eu
SNS_ENABLED=true
# Firebase (opcional; hoy el sender es stub)
FIREBASE_CREDENTIALS_PATH=
```

### findu-notification-dispatcher (9000) — fase 2
```
PORT=9000
RABBITMQ_HOST=rabbitmq.railway.internal
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=findu
RABBITMQ_PASSWORD=<password>
SPRING_DATA_REDIS_HOST=redis.railway.internal
SPRING_DATA_REDIS_PORT=6379
```

### findu-transaction (8084) — fase 2
```
PORT=8084
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/transaction_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
SPRING_CLOUD_CONFIG_ENABLED=false
```

### findu-help-v2 (8085) — fase 2
```
PORT=8085
SPRING_DATA_MONGODB_URI=mongodb://mongo.railway.internal:27017/help_db
# S3: usar bucket AWS real o MinIO (endpoint override)
FINDU_S3_BUCKET=<bucket>
FINDU_S3_REGION=us-east-1
AWS_ACCESS_KEY_ID=<app_aws_access_key_id>
AWS_SECRET_ACCESS_KEY=<app_aws_secret_access_key>
```

---

## 7. Exponer el gateway

```powershell
railway domain --service api-gateway       # genera *.up.railway.app
```

Solo el gateway recibe dominio público. El resto queda en la red privada.

---

## 8. Orden de arranque recomendado

1. `postgres`, `redis` (+ `rabbitmq`, `mongo` en fase 2)
2. `findu-spring-security`
3. `autorization-server-oauth2`
4. `findu-core`
5. `api-gateway` (último, ya que depende de los anteriores)
6. Fase 2: `notification-processor`, `notification-dispatcher`, `transaction`, `help-v2`

---

## 9. Verificación

```powershell
railway status                                   # estado de los servicios
railway logs --service findu-spring-security     # logs de un micro
# Salud a través del gateway (dominio público):
# https://<api-gateway>.up.railway.app/actuator/health
```

---

## 10. Deuda técnica a resolver antes de un despliegue limpio

- **api-gateway**: su `application.yml` usa `lb://` + Eureka. Para Railway sin
  Eureka hay que enrutar por URL interna (perfil nuevo o variables de ruta).
- **findu-core**: tiene la URL del notification-processor **hardcodeada**
  (`http://findu-notification-processor:8083`); parametrizarla por variable.
- **Servicios que aún traen `spring-cloud-starter-config`** (transaction,
  daily-balance): asegurar `optional:` o desactivar para que no fallen sin Config Server.
- **Perfil `qa`**: revisar que no ejecute `schema.sql` destructivo en cada arranque.

---

## 11. Nota sobre Terraform de Railway (futuro)

Railway se puede gestionar con el provider comunitario
`terraform-community-providers/railway` (v0.6.x): proyecto, entornos, servicios,
variables, volúmenes y dominios. El **build/deploy del código** seguiría por CLI
o GitHub. Se evaluará migrar la configuración a Terraform una vez estabilizado el
despliegue por CLI, para versionar las variables de entorno de forma reproducible.

---

## 12. Estado del despliegue (fase 1 — núcleo mínimo) ✅

Proyecto Railway: **findu-production**. Entorno: `production`.
Gateway público: **https://api-gateway-production-ff97.up.railway.app**

| Servicio | Estado | Puerto | Notas |
|---|---|---|---|
| Postgres | ✅ Online | 5432 | base `railway` compartida (Opción A) |
| Redis | ✅ Online | 6379 | requiere password |
| findu-spring-security | ✅ Online | 8081 | R2DBC + Redis + DynamoDB (AWS) |
| autorization-server-oauth2 | ✅ Online | 9595 | JDBC |
| findu-core | ✅ Online | 8082 | JDBC; caché Redis desactivada (ver abajo) |
| api-gateway | ✅ Online | 8080 | público; rutea por DNS interno |

Verificado end-to-end (HTTP 200 vía gateway público):
- `/actuator/health` → `{"status":"UP"}`
- `/findu-core/api/v1/direcciones/municipios` → 200 (datos sembrados)
- `/findu-core/api/v1/servicios` → 200

### Comando de despliegue que funcionó (patrón por servicio)

```powershell
# 1. compilar el jar (los Dockerfile hacen COPY build/libs/*.jar)
cd <carpeta-del-micro>
./gradlew clean bootJar -x test --no-daemon

# 2. crear servicio + variables (SPRING_PROFILES_ACTIVE=qa, PORT, conexiones internas)
railway add --service <nombre>
railway variables --service <nombre> --set "KEY=VALUE" ...

# 3. desplegar — CLAVE: --path-as-root (contexto = carpeta del micro) y
#    --no-gitignore (para incluir build/libs, que está en .gitignore)
railway up . --path-as-root --no-gitignore --service <nombre> --ci
```

### Aprendizajes / problemas resueltos durante el despliegue

1. **Contexto de build**: `railway up` sube la raíz del repo vinculado. Hay que
   usar `--path-as-root` desde la carpeta del micro para que use SU Dockerfile.
2. **jar no subía**: el `.gitignore` excluye `build/`, y `railway up` lo respeta
   → el `COPY build/libs/*.jar` fallaba. Solución: `--no-gitignore`.
3. **Redis con password**: Railway Redis exige auth (`NOAUTH`). Hay que pasar
   `SPRING_DATA_REDIS_PASSWORD` (variable `REDIS_PASSWORD` del servicio Redis).
4. **Schema no creado**: el perfil `qa` traía `ddl-auto: none` y `sql.init`
   sin rutas. Se añadió `schema-locations`/`data-locations: classpath:db/postgres/*.sql`
   y se arrancó una vez con `SPRING_SQL_INIT_MODE=always`, luego se volvió a `never`.
5. **Caché Redis de `Page` incompatible** (deuda técnica): core cachea respuestas
   `Page` con `GenericJackson2JsonRedisSerializer`, que **no puede deserializar
   `PageImpl`** (no tiene constructor por defecto) → 500. Workaround aplicado:
   `FINDU_CACHE_ENABLED=false`. Arreglo definitivo pendiente: cachear DTOs/listas
   en vez de `Page`, o configurar un serializador/mixin para `PageImpl`.

### Pendiente (fase 2)
- RabbitMQ + MongoDB + MinIO/S3
- findu-notification-processor, findu-notification-dispatcher
- findu-transaction, findu-help-v2
- Salir de sandbox SES/SNS para notificaciones a cualquier destinatario
- Config: dejar `SPRING_SQL_INIT_MODE=never` ya aplicado en core tras el seed inicial
