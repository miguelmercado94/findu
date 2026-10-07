# Verificación — Fix de enrutamiento RabbitMQ en `findu-notification-dispatcher`

Iteración: primera (no existía `dispatcher-routing-review.json`).

## Qué se implementó (según `dispatcher-routing-plan.md`)

- **DTO nuevo** `presentation/controller/dto/NotificationMessagePayload.java` (record, `@JsonInclude(NON_NULL)`) con los campos EXACTOS del `NotificationMessage` del processor: `notificationId, type, recipient, templateCode, language, params(Map<String,String>), requiresConnection, createdAt`.
- **`config/RabbitMQConfig.java`**: beans nuevos sin tocar los existentes (events/fallback):
  - `TopicExchange notificationsExchange()` → `findu.notifications` (durable, no auto-delete).
  - `Jackson2JsonMessageConverter notificationJsonConverter()`.
  - `RabbitTemplate notificationsRabbitTemplate(...)` cualificado que usa el converter Jackson (publica con `contentType=application/json`). No interfiere con el `RabbitTemplate` autoconfigurado que usa `RabbitFallbackProducer`.
- **Productor nuevo** `infrastructure/adapter/rabbitmq/TransactionalNotificationProducer.java`: publica al exchange `findu.notifications` con la routing key derivada del `type` (SMS→`notification.sms`, EMAIL→`notification.email`, PUSH→`notification.push`, WHATSAPP→`notification.whatsapp`). Usa el `RabbitTemplate` cualificado.
- **`NotificationDispatchController.java`**: bifurcación. Si `templateCode` no es blank y `type ∈ {SMS,EMAIL,PUSH,WHATSAPP}` → construye `NotificationMessagePayload` (params → `Map<String,String>` con `String.valueOf`, `language` default `es`, `notificationId` UUID, `createdAt` ISO) y publica por el productor transaccional. En cualquier otro caso conserva EXACTAMENTE la ruta de presencia por `EventDispatcherService` (ofertas en tiempo real, fallback FCM) sin cambios.
- **Propiedades con defaults correctos** (no requieren env vars nuevas en Railway, perfil `qa`):
  - `application.yml`: bloque `findu.rabbitmq.notification.exchange` + `routing-keys.{sms,email,push,whatsapp}`.
  - `application-qa.yml`: mismo bloque añadido explícitamente.
  - Exchange = `findu.notifications` (exacto).

## Qué NO se tocó

- `findu-notification-processor` (SOLO LECTURA): `NotificationMessage`, `RabbitMqConfig`, consumers, colas, bindings, senders SNS/SES/FCM.
- `findu-spring-security`.
- Flujo WebSocket de presencia: `EventDispatcherService`, `RabbitFallbackProducer`, `EventFallbackPort`, `EventEnvelope`, `EventType.OFFER_RECEIVED`, exchanges de events/fallback.

## Comandos ejecutados y resultados (PowerShell, Windows)

Directorio: `CORE/LAMBDA/findu-notification-dispatcher`

1. Build/empaquetado del jar (requerido por el Dockerfile `COPY build/libs/*.jar`):

   ```
   .\gradlew.bat clean bootJar -x test --no-daemon
   ```
   Resultado: **BUILD SUCCESSFUL in 1m 44s** (5 actionable tasks: 5 executed).
   Artefacto generado: `build/libs/findu-notification-dispatcher-0.0.1-SNAPSHOT.jar` (≈61.6 MB).

2. Test nuevo (unitario del controller, Mockito):

   ```
   .\gradlew.bat test --tests "*NotificationDispatchControllerTest" --no-daemon
   ```
   Resultado: **BUILD SUCCESSFUL in 1m 26s** — **2 tests completed, 0 failed**.
   - Test: `NotificationDispatchControllerTest`
     - `sms_con_templateCode_publica_al_productor_transaccional_y_no_al_servicio_de_presencia` — PASÓ. Verifica que un payload `type=SMS` + `templateCode=VERIFICACION_OTP` invoca `TransactionalNotificationProducer.publish(...)` (y NO `EventDispatcherService`), que el payload mapea 1:1 (`type`, `recipient`, `templateCode`, `language`, `requiresConnection=false`, `notificationId`/`createdAt` no vacíos, `params` como `Map<String,String>` con `otp_code`/`expiration_minutes`), y que el JSON serializado se deserializa en un POJO con el contrato EXACTO del `NotificationMessage` del processor.
     - `evento_sin_templateCode_sigue_la_ruta_de_presencia_y_no_el_productor_transaccional` — PASÓ. Verifica que un evento sin `templateCode` sigue por `EventDispatcherService.dispatch(...)` y NO por el productor transaccional.

## Nota sobre el gate de cobertura global

El `build.gradle` del dispatcher no define jacoco ni un gate de cobertura; no hubo gate que ejecutar ni que bloqueara. No se requirió Docker Desktop para la build ni para los tests en esta máquina.
