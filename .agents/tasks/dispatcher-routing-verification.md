# Verificación — Fix de enrutamiento RabbitMQ en `findu-notification-dispatcher`

Iteración: **segunda** (existía `dispatcher-routing-review.json` con verdict `CHANGES_REQUESTED`). Se corrigieron los hallazgos del review antes de re-verificar.

## Hallazgos del review y cómo se corrigieron

1. **(bloqueante, confirmed) Converter JSON global rompe presencia.**
   El `@Bean Jackson2JsonMessageConverter notificationJsonConverter` quedaba como el ÚNICO `MessageConverter` del contexto; Spring Boot 3.4.2 (`getIfUnique`) lo adoptaba en el `RabbitTemplate` por defecto (usado por `RabbitFallbackProducer`) y en el `SimpleRabbitListenerContainerFactory` por defecto (usado por `RabbitEventConsumer`), alterando el `contentType` del fallback FCM y rompiendo el consumo `String` del flujo de presencia WebSocket.
   **Fix aplicado:** se eliminó el `@Bean` del converter y se construye como variable LOCAL dentro de `@Bean notificationsRabbitTemplate`:
   ```java
   RabbitTemplate template = new RabbitTemplate(connectionFactory);
   template.setMessageConverter(new Jackson2JsonMessageConverter());
   ```
   Así el contexto NO tiene ningún `MessageConverter` bean, el template por defecto y el listener factory conservan su `SimpleMessageConverter`, y la ruta transaccional sigue publicando con `contentType=application/json`. Se eliminó el import de `@Qualifier` ya no usado en `RabbitMQConfig`. El `TransactionalNotificationProducer` sigue inyectando el `RabbitTemplate` cualificado `notificationsRabbitTemplate` (sin cambios).

2. **(no bloqueante, confirmed) Cobertura ausente del flujo de presencia.**
   **Fix aplicado:** test nuevo `config/RabbitMQConfigConverterIsolationTest.java` con `ApplicationContextRunner` (sin broker) que:
   - afirma que el contexto NO tiene bean `MessageConverter` ni `Jackson2JsonMessageConverter` (prueba de que la autoconfiguración por defecto no adopta ningún converter → presencia intacta);
   - afirma que `notificationsRabbitTemplate` sí lleva `Jackson2JsonMessageConverter` (ruta transaccional con JSON);
   - afirma que un `RabbitTemplate` por defecto conserva `SimpleMessageConverter`.

3. **(no bloqueante, possible) Orden de arranque exchange/binding.**
   **Fix aplicado (documentación):** se añadió una nota en `application.yml` (bloque `findu.rabbitmq.notification`) explicando que el dispatcher declara el exchange topic `findu.notifications` pero no los bindings del processor; el processor (consumidor de larga vida) debe estar desplegado/arrancado en QA para que sus bindings existan antes de publicar, o de lo contrario un publish previo al binding se descartaría en el topic exchange.

## Qué NO se tocó

- `findu-notification-processor` (SOLO LECTURA): `NotificationMessage`, `RabbitMqConfig`, consumers, colas, bindings, senders SNS/SES/FCM.
- `findu-spring-security`.
- Flujo WebSocket de presencia: `EventDispatcherService`, `RabbitFallbackProducer`, `EventFallbackPort`, `EventEnvelope`, `EventType.OFFER_RECEIVED`, exchanges de events/fallback. (Ahora además verificado por test que no cambia su converter.)

## Comandos ejecutados y resultados (PowerShell, Windows)

Directorio: `CORE/LAMBDA/findu-notification-dispatcher`

1. Build/empaquetado del jar (requerido por el Dockerfile `COPY build/libs/*.jar`):

   ```
   .\gradlew.bat clean bootJar -x test --no-daemon
   ```
   Resultado: **BUILD SUCCESSFUL in 1m 41s** (5 actionable tasks: 5 executed).
   Artefacto generado: `build/libs/findu-notification-dispatcher-0.0.1-SNAPSHOT.jar` (≈61.6 MB).

2. Tests (controller + aislamiento del converter):

   ```
   .\gradlew.bat test --tests "*NotificationDispatchControllerTest" --tests "*RabbitMQConfigConverterIsolationTest" --no-daemon
   ```
   Resultado: **BUILD SUCCESSFUL in 1m 52s** — **5 tests completed, 0 failed**.
   - `NotificationDispatchControllerTest`
     - `sms_con_templateCode_publica_al_productor_transaccional_y_no_al_servicio_de_presencia` — PASÓ. `type=SMS` + `templateCode=VERIFICACION_OTP` invoca `TransactionalNotificationProducer.publish(...)` (NO `EventDispatcherService`), payload mapea 1:1 (`params` como `Map<String,String>` con `otp_code`/`expiration_minutes`), y el JSON serializado se deserializa en una réplica del contrato EXACTO del `NotificationMessage` del processor.
     - `evento_sin_templateCode_sigue_la_ruta_de_presencia_y_no_el_productor_transaccional` — PASÓ.
   - `RabbitMQConfigConverterIsolationTest`
     - `noHayNingunMessageConverterComoBeanEnElContexto` — PASÓ (presencia no afectada por el converter).
     - `elRabbitTemplateTransaccionalUsaConverterJsonAislado` — PASÓ.
     - `unRabbitTemplatePorDefectoConservaSimpleMessageConverter` — PASÓ.

## Nota sobre el gate de cobertura global

El `build.gradle` del dispatcher no define jacoco ni un gate de cobertura; no hubo gate que ejecutar ni que bloqueara. No se requirió Docker Desktop para la build ni para los tests en esta máquina.
