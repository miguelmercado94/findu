# Implementation Plan — FIND-U `qa` profile for Railway

Goal: give every microservice and lambda a coherent `application-qa.yml` for Railway, where there is NO Eureka and NO Config Server. Services talk over internal DNS `http://<servicio>.railway.internal:<puerto>`. All connections come from environment variables. The gateway routes by internal URL instead of `lb://`. One mechanical Java change in `findu-core` extracts a hardcoded URL to a property.

## Facts established during exploration

Reference style model (already correct, do NOT break): `SECURITY/findu-spring-security/src/main/resources/application-qa.yml` — everything via env vars, `spring.sql.init.mode: never`, DynamoDB/Redis toggles. It does NOT currently disable Eureka explicitly; the only change there is to add the Eureka-disable block.

Ports come from each `Dockerfile EXPOSE` (authoritative), which in several cases differ from the base `application.yml server.port`. In `qa` always use `server.port: ${PORT:<expose-port>}` so Railway injects `PORT`.

| Module | Path (dir) | EXPOSE | base server.port | eureka-client dep? | @EnableDiscoveryClient? | config.import optional? |
|---|---|---|---|---|---|---|
| api-gateway | ARCHITECTURE/findu-api-gateway | 8080 | 8080 | YES | no | YES |
| oauth2 | SECURITY/findu-autorization-server-oauth2 | 9595 | 9595 | YES | YES | YES |
| spring-security | SECURITY/findu-spring-security | 8081 | 8081 | YES | YES | YES (base) |
| core | CORE/findu-core | 8082 | 8082 | YES | no | YES |
| transaction | CORE/findu-transaction | 8084 | 8086 (mismatch) | NO | no | YES |
| help-v2 | CORE/findu-help-v2 | 8085 | 8085 | NO (yml has inert eureka block) | no | YES |
| notification-processor | CORE/findu-notification-processor | 8083 | 8083 | YES | no | YES |
| notification-dispatcher | CORE/LAMBDA/findu-notification-dispatcher | 9000 | 9000 | YES | YES | YES |
| transaction-daily-balance | CORE/LAMBDA/findu-transaction-daily-balance | 8089 | 8087 (mismatch) | NO | no | YES |
| transaction-provider-payout | CORE/LAMBDA/findu-transaction-provider-payout | (none; app port 8088) | 8088 | NO | no | NO config dep |
| transaction-refund | CORE/LAMBDA/findu-transaction-refund | (none; app port 8089) | 8089 | NO | no | NO config dep |

Notes:
- All `config.import` lines are already `optional:configserver:...` — verify, do not remove.
- payout/refund have NO `spring-cloud-starter-config` and NO eureka — do not add Eureka/Config blocks there.
- help-v2 has an `eureka:` block in its base yml but NO eureka-client dependency; the block is inert. The qa profile does not need a disable block, but adding `eureka.client.enabled: false` is harmless and documents intent. Spec says "Verify the dependency exists before adding the block" — so for help-v2 we OMIT the eureka block (no dependency).
- Each module builds independently with its own `gradlew.bat`. Verification runs inside the module dir.

Standard Eureka-disable block to add in `qa` for every module that HAS the eureka-client dependency:
```yaml
eureka:
  client:
    enabled: false
    register-with-eureka: false
    fetch-registry: false
```
Disabling the client is sufficient even where `@EnableDiscoveryClient` is present (oauth2, dispatcher, spring-security); do NOT remove the annotation.

## The one allowed Java change (findu-core)

File: `CORE/findu-core/src/main/java/com/findu/core/application/service/GestionPerfilProveedorUseCaseImpl.java`, method `notificarDispatcherDisponibilidad` (~line 220) has:
```java
String url = "http://findu-notification-processor:8083/api/v1/canales/proveedor/disponibilidad";
```
The class is `@Service @RequiredArgsConstructor` (Lombok injects only `final` fields). Add a non-final `@Value` field so it does not become a constructor arg:
```java
@org.springframework.beans.factory.annotation.Value("${findu.notification.processor.base-url:http://findu-notification-processor.railway.internal:8083}")
private String notificationProcessorBaseUrl;
```
and build the URL as `notificationProcessorBaseUrl + "/api/v1/canales/proveedor/disponibilidad"`. This is the ONLY Java change. If it proves more invasive, document it as pending and leave the string.

## Verification command (per module, Windows)

Run inside the module directory:
```
.\gradlew.bat clean compileJava -x test --no-daemon
```
For `api-gateway` and `findu-spring-security` also confirm packaging:
```
.\gradlew.bat clean bootJar -x test --no-daemon
```
Expected outcome: `BUILD SUCCESSFUL`. (Grep is not verification.)

---

## Plan items

- [ ] 1. api-gateway (CRITICAL): create `qa` profile that disables discovery locator + Eureka and rewrites every route to Railway internal DNS by env var, preserving ids, predicates, paths, CORS (globalcors) and default-filters.
      Routes: security-auth-route → `${SECURITY_SERVICE_URI:http://findu-spring-security.railway.internal:8081}` (Path=/security-auth/**); authorization-server-route → `${OAUTH2_SERVICE_URI:http://autorization-server-oauth2.railway.internal:9595}` (Path=/authorization-server/**); findu-core-route → `${CORE_SERVICE_URI:http://findu-core.railway.internal:8082}` (Path=/findu-core/**); findu-transaction-route → `${TRANSACTION_SERVICE_URI:http://findu-transaction.railway.internal:8084}` (Path=/findu-transaction/**, /internal/**); findu-help-route → `${HELP_SERVICE_URI:http://findu-help-v2.railway.internal:8085}` (Path=/findu-help-v2/**); notification-dispatcher-route → `${DISPATCHER_SERVICE_URI:http://findu-notification-dispatcher.railway.internal:9000}` (Path=/ws/events/**, /notification-dispatcher/**). Set `spring.cloud.gateway.discovery.locator.enabled: false`, `server.port: ${PORT:8080}`, and the Eureka-disable block.
      Files: ARCHITECTURE/findu-api-gateway/src/main/resources/application-qa.yml
      Verify: in `ARCHITECTURE/findu-api-gateway` run `.\gradlew.bat clean bootJar -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 2. findu-core Java change + `qa` profile. Extract the hardcoded processor URL to a `@Value` field (see "allowed Java change" above). Create `qa` with `server.port: ${PORT:8082}`, Postgres JPA via `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `spring.sql.init.mode: never`, `ddl-auto: none`, Redis via `spring.data.redis.host/port` env vars, `findu.cache.enabled`, `findu.notification.enabled`/`lambda-url` kept env-driven, `findu.notification.processor.base-url` default `http://findu-notification-processor.railway.internal:8083`, and the Eureka-disable block.
      Files: CORE/findu-core/src/main/java/.../GestionPerfilProveedorUseCaseImpl.java; CORE/findu-core/src/main/resources/application-qa.yml
      Verify: in `CORE/findu-core` run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 3. oauth2 `qa` profile: `server.port: ${PORT:9595}`, Postgres JPA (JDBC) via `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` with `ddl-auto: none` and `spring.sql.init.mode: never`, `findu.security-api.base-url` default `http://findu-spring-security.railway.internal:8081/security-auth`, Eureka-disable block. Keep `@EnableDiscoveryClient`; do not remove.
      Files: SECURITY/findu-autorization-server-oauth2/src/main/resources/application-qa.yml
      Verify: in `SECURITY/findu-autorization-server-oauth2` run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 4. spring-security `qa` profile: add ONLY the Eureka-disable block to the existing `application-qa.yml` (everything else already correct). Do not touch R2DBC/DynamoDB/Redis blocks.
      Files: SECURITY/findu-spring-security/src/main/resources/application-qa.yml
      Verify: in `SECURITY/findu-spring-security` run `.\gradlew.bat clean bootJar -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 5. transaction `qa` profile: `server.port: ${PORT:8084}` (note base says 8086 but Dockerfile EXPOSE is 8084 — use 8084), Postgres JPA via `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `spring.sql.init.mode: never`, `ddl-auto: none`. NO Eureka block (no eureka-client dependency). Keep `config.import` optional.
      Files: CORE/findu-transaction/src/main/resources/application-qa.yml
      Verify: in `CORE/findu-transaction` run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 6. help-v2 `qa` profile: `server.port: ${PORT:8085}`, MongoDB via `SPRING_DATA_MONGODB_URI`, S3 via `S3_BUCKET`/`AWS_REGION`/`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` and NO `S3_ENDPOINT` (omit/empty so real S3 is used), notification dispatcher/processor URLs via env with Railway internal defaults. NO Eureka block (no dependency; omit the inert eureka block).
      Files: CORE/findu-help-v2/src/main/resources/application-qa.yml
      Verify: in `CORE/findu-help-v2` run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 7. notification-processor `qa` profile: `server.port: ${PORT:8083}`, RabbitMQ via `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`, Redis via `spring.data.redis.host/port`, AWS region `AWS_REGION`, `DYNAMODB_TEMPLATES_TABLE`, `SES_FROM_EMAIL`, `SNS_ENABLED`; do NOT set `*_ENDPOINT` (leave omitted/empty for real AWS), Eureka-disable block.
      Files: CORE/findu-notification-processor/src/main/resources/application-qa.yml
      Verify: in `CORE/findu-notification-processor` run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 8. notification-dispatcher `qa` profile: `server.port: ${PORT:9000}`, Redis via `spring.data.redis.host/port`, RabbitMQ via `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`, `findu.jwt.secret` via `JWT_SECRET`, Eureka-disable block. Keep `@EnableDiscoveryClient`; do not remove.
      Files: CORE/LAMBDA/findu-notification-dispatcher/src/main/resources/application-qa.yml
      Verify: in `CORE/LAMBDA/findu-notification-dispatcher` run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 9. transaction-daily-balance `qa` profile: `server.port: ${PORT:8089}` (base says 8087, EXPOSE is 8089 — use 8089), Postgres JPA via `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `ddl-auto: none`, `findu.daily-balance.cron` and `findu.timezone` kept env-driven. NO Eureka block (no dependency). Keep `config.import` optional.
      Files: CORE/LAMBDA/findu-transaction-daily-balance/src/main/resources/application-qa.yml
      Verify: in `CORE/LAMBDA/findu-transaction-daily-balance` run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

- [ ] 10. payout + refund skeleton `qa` profiles: minimal file each with `server.port: ${PORT:8088}` (payout) / `${PORT:8089}` (refund) and `findu.transaction.url` env-driven default `http://findu-transaction.railway.internal:8084`. NO Eureka/Config blocks (neither dependency present). Do NOT invent dependencies.
      Files: CORE/LAMBDA/findu-transaction-provider-payout/src/main/resources/application-qa.yml; CORE/LAMBDA/findu-transaction-refund/src/main/resources/application-qa.yml
      Verify: in each module run `.\gradlew.bat clean compileJava -x test --no-daemon` → BUILD SUCCESSFUL.

## Open assumptions
- payout app port 8088 / refund 8089 taken from their base `server.port` (no Dockerfile EXPOSE for these two). If Railway assigns `PORT` they bind to it regardless.
- help-v2 keeps S3 real (no endpoint override) per the rule "do not set *_ENDPOINT". If MinIO-on-Railway is chosen later, add `S3_ENDPOINT` then.
- `qa` aligns Redis config to `spring.data.redis.*` (Spring Boot 3 location, as in the security reference), even though core dev/pdn still use legacy `spring.redis.*`.
