# RabbitMQ transactional routing fix in notification-dispatcher (pass 2)

The dispatcher now bifurcates `/dispatchNotification`: requests carrying a `templateCode` plus a channel `type` (SMS/EMAIL/PUSH/WHATSAPP) are published to the processor's `findu.notifications` topic exchange with a per-channel routing key, while presence events (no `templateCode`) stay on the existing `EventDispatcherService` path. The transactional body is a dedicated `NotificationMessagePayload` record whose fields match the processor's `NotificationMessage` 1:1, published through a qualified `RabbitTemplate` that carries a Jackson JSON converter. This is the second pass: the prior review flagged (blocking) that the Jackson converter was exposed as a context bean, which Spring Boot would have adopted into the default template and listener factory and broken the presence/fallback paths. That blocking issue is fixed — the converter is now a local variable inside the qualified template bean.

Watch for: nothing blocking. The plan said `application-qa.yml` would not be changed, but the dispatcher's `application-qa.yml` gained the `findu.rabbitmq.notification` block (possible, confirmed) — behaviorally harmless duplication of the defaults it already inherits. The exchange/binding startup-order dependency remains a deploy-time operational note (possible), already documented in `application.yml`.

**Verdict**: APPROVED

## High-level view

The blocking regression from pass 1 is resolved. `RabbitMQConfig` builds the `Jackson2JsonMessageConverter` as a local variable inside `notificationsRabbitTemplate` and exposes no `MessageConverter` bean, so Spring Boot's AMQP autoconfiguration leaves the default `RabbitTemplate` (used by `RabbitFallbackProducer`) and the default listener factory (used by `RabbitEventConsumer`) on their `SimpleMessageConverter`. The presence/fallback flow, which publishes an already-serialized `String`, is untouched.

The transactional payload contract is correct. `NotificationMessagePayload` carries exactly the fields the processor's `NotificationMessage` deserializes (`notificationId, type, recipient, templateCode, language, params:Map<String,String>, requiresConnection, createdAt`), and the controller converts the inbound `Map<String,Object>` params to `Map<String,String>` via `String.valueOf`. A `type=SMS`+`templateCode=VERIFICACION_OTP` request resolves to routing key `notification.sms`, which the processor binds (alongside `notification.celular`) to its SMS queue — so the message reaches the SNS path instead of the FCM fallback.

The routing config is parameterized with defaults that match the processor and require no new Railway env vars. The qa profile inherits the same defaults; the extra copy added to `application-qa.yml` is redundant but not wrong. Processor and security modules were not modified by these commits.

Test coverage is adequate for the behavior under review: the controller branch logic is unit-tested both ways, the JSON round-trips through a replica of the processor contract, and a context test asserts the converter does not leak to the default beans.

<details>
<summary>Issues (2)</summary>

1. **Redundant qa routing block** — `application-qa.yml` duplicates the `findu.rabbitmq.notification` defaults it already inherits from `application.yml`, contradicting the plan's "no qa changes" note. Harmless; optionally drop it to keep a single source of truth.
2. **Exchange/binding startup order** — the dispatcher declares the `findu.notifications` topic exchange but not the processor's queue bindings; a publish before the processor has declared its bindings is dropped at the topic exchange. Operational, already documented in `application.yml`; ensure the processor is deployed and running in QA.

</details>

<details>
<summary>Details</summary>

### Converter isolation — the pass-1 blocker is fixed

The pass-1 blocker was that `@Bean Jackson2JsonMessageConverter` was the only `MessageConverter` in the context, so Spring Boot's `getIfUnique` wired it into the default `RabbitTemplate` and the default `SimpleRabbitListenerContainerFactory`, re-encoding the fallback FCM message and breaking the `String` consumer of the presence flow.

`RabbitMQConfig.notificationsRabbitTemplate` now builds the converter locally:

```java
@Bean
public RabbitTemplate notificationsRabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    return template;
}
```

No `MessageConverter` bean exists in the context, so the default template and listener factory keep their `SimpleMessageConverter`. `RabbitFallbackProducer` injects the unqualified default `RabbitTemplate` and publishes a pre-serialized `String`, so its `contentType` is unchanged; `TransactionalNotificationProducer` injects the qualified `notificationsRabbitTemplate`, so only the transactional path emits `application/json`.

### Transactional payload matches the processor contract

`NotificationMessagePayload` is a record with `notificationId, type, recipient, templateCode, language, params(Map<String,String>), requiresConnection, createdAt` — field-for-field identical to the processor's `NotificationMessage` POJO. `@JsonInclude(NON_NULL)` omits `attachments` (not present in the DTO), which the processor simply leaves null; `requiresConnection` is a primitive `boolean` so `false` still serializes. The controller generates `notificationId` as a UUID, defaults `language` to `es`, sets `requiresConnection=false`, stamps `createdAt=Instant.now().toString()`, and converts params with `String.valueOf(v)` while skipping null values. The presence-only control keys (`templateCode`, `type`, `tipoEvento`) are not injected into transactional params — they only exist on the presence branch.

### Channel-to-routing-key resolution and processor bindings

```
type=SMS       -> notification.sms      -> processor smsBindingAlt      -> findu.notifications.celular (SNS)
type=EMAIL     -> notification.email    -> processor emailBindingAlt    -> findu.notifications.correo (SES)
type=PUSH      -> notification.push     -> processor pushBinding        -> findu.notifications.push (FCM)
type=WHATSAPP  -> notification.whatsapp -> processor whatsappBinding    -> findu.notifications.whatsapp
```

The processor binds both `notification.celular`/`notification.sms` to the SMS queue and `notification.correo`/`notification.email` to the email queue, so the dispatcher's choice of the channel-named aliases lands in the right queues. `resolveRoutingKey` upper-cases/trims and throws `IllegalArgumentException` on an unknown channel — but the controller only enters this branch when `type` is already in `{SMS,EMAIL,PUSH,WHATSAPP}`, so the throw is unreachable from the request path. The key behavioral result: a `type=SMS`+`VERIFICACION_OTP` request no longer flows into `EventEnvelope.of(OFFER_RECEIVED, ...)` → fallback FCM; it goes to `findu.notifications` with `notification.sms`.

### Presence flow preserved

Any request without a `templateCode`, or with a non-channel `type`, falls through to `despacharEventoPresencia`, which is unchanged: it still builds the `EventEnvelope.of(OFFER_RECEIVED, ...)` with the injected `templateCode`/`type`/`tipoEvento` control keys and the `CLIENTE_`/`PROVEEDOR_` channel id, and calls `eventDispatcherService.dispatch(...)`. The unit test `evento_sin_templateCode_...` confirms a `type=OFFER_RECEIVED` request with null `templateCode` reaches `EventDispatcherService.dispatch` and never the transactional producer.

### Configuration and env-var posture

`application.yml` adds `findu.rabbitmq.notification.exchange` and the four `routing-keys.*` with defaults (`findu.notifications`, `notification.sms|email|push|whatsapp`) that match the processor, each overridable via an env var but not requiring one. `RabbitMQConfig` declares the `findu.notifications` topic exchange (durable, non-auto-delete) so publishing works even if the processor hasn't started, without declaring the processor's queues/bindings. The qa profile inherits these defaults, so no Railway env vars are needed. The one deviation from the plan: `application-qa.yml` was given its own copy of the `findu.rabbitmq.notification` block — the plan explicitly said qa needed no changes. It is a redundant restatement of the inherited defaults, not a behavioral change, so it is non-blocking; trimming it would keep a single source of truth.

### Test coverage

Covered: the controller branch both ways (`sms_con_templateCode_...` asserts the transactional producer is called with a 1:1-mapped payload, params as `Map<String,String>`, and never `EventDispatcherService`; `evento_sin_templateCode_...` asserts the inverse), a JSON round-trip through a `NotificationMessageContract` replica of the processor POJO, and `RabbitMQConfigConverterIsolationTest` asserting no `MessageConverter` bean, the transactional template carrying Jackson, and a default template keeping `SimpleMessageConverter`.

Not tested: no end-to-end broker test proves the published message actually binds to the processor's SMS queue (the alias binding is verified by reading the processor config, not by an integration test), and there is no assertion that the real default `RabbitTemplate`/listener factory in a full `@SpringBootTest` context keep `SimpleMessageConverter` — the isolation test uses an `ApplicationContextRunner` with only `RabbitMQConfig` and a mock `ConnectionFactory`, which is a strong proxy but not the full autoconfigured context. Neither gap is blocking given the config-level reasoning.

### Verification evidence relied upon

Per instructions I did not re-run the build or suite. The coder's `dispatcher-routing-verification.md` records `.\gradlew.bat clean bootJar -x test --no-daemon` → BUILD SUCCESSFUL and `.\gradlew.bat test --tests "*NotificationDispatchControllerTest" --tests "*RabbitMQConfigConverterIsolationTest"` → 5 passed, 0 failed. I spot-checked the two test sources and the three main sources directly; the described fixes and assertions are present as claimed, so the evidence is consistent with the code on disk.

### Unrelated change in the tree

The processor's `application-qa.yml` has an uncommitted one-line addition (`SPRING_DATA_REDIS_PASSWORD` default). It is not part of these dispatcher fix commits and does not touch routing, the `NotificationMessage` contract, or any binding, so it is out of scope for this review and does not violate the "do not touch the processor" constraint for this change.

</details>

<details>
<summary>File map</summary>

- `config/RabbitMQConfig.java` — adds `findu.notifications` topic exchange bean and the qualified `notificationsRabbitTemplate` with a local Jackson converter.
- `infrastructure/adapter/rabbitmq/TransactionalNotificationProducer.java` — new producer; maps `type` to routing key and publishes to `findu.notifications`.
- `presentation/controller/NotificationDispatchController.java` — bifurcates transactional vs presence; builds the transactional payload.
- `presentation/controller/dto/NotificationMessagePayload.java` — new record matching the processor's `NotificationMessage`.
- `src/main/resources/application.yml` — adds `findu.rabbitmq.notification.*` defaults + startup-order note.
- `src/main/resources/application-qa.yml` — redundant copy of the same block (deviates from plan).
- `src/test/.../NotificationDispatchControllerTest.java` — branch logic + JSON contract round-trip.
- `src/test/.../config/RabbitMQConfigConverterIsolationTest.java` — converter isolation from default beans.

Full diff: `git diff ccaca02 HEAD -- CORE/LAMBDA/findu-notification-dispatcher`

</details>
