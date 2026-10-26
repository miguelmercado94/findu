# Plan de implementación — Fix de enrutamiento RabbitMQ en `findu-notification-dispatcher`

## Objetivo

Que `POST /dispatchNotification` con `templateCode` + `type` (SMS/EMAIL/PUSH/WHATSAPP) **publique al exchange `findu.notifications`** con la routing key del canal, en un body JSON deserializable por el `NotificationMessage` del `findu-notification-processor`. El flujo WebSocket de presencia (ofertas en tiempo real) queda intacto.

---

## Diagnóstico confirmado (leído en código)

El bug está en `NotificationDispatchController.dispatchNotification(...)`
(`CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/presentation/controller/NotificationDispatchController.java`):

- Hoy **todo** request entrante se envuelve en un `EventEnvelope.of(EventType.OFFER_RECEIVED, recipient, payload)` y se pasa a `EventDispatcherService.dispatch(...)`.
- `EventDispatcherService.procesarEntrega(...)` resuelve presencia: si el usuario está OFFLINE llama a `eventFallbackPort.sendToFallbackQueue(envelope)` → `RabbitFallbackProducer` publica al exchange **`findu.notifications.fallback.exchange`** con routing key `findu.fallback.notification` y `eventType=OFFER_RECEIVED`.
- Ese exchange/queue de fallback **no es** el `findu.notifications` que consume el processor. Por eso el SMS nunca llega a SNS. (Coincide exactamente con los logs de Railway del enunciado.)

Conclusión de diseño: el controller debe **bifurcar** por tipo de tráfico. Las notificaciones transaccionales (traen `templateCode` + `type` de canal) van por una **nueva ruta de publicación** al exchange del processor; los eventos de presencia siguen por `EventDispatcherService` sin cambios.

---

## Contrato EXACTO del processor (SOLO LECTURA — no se toca)

### Clase destino de deserialización
`CORE/findu-notification-processor/.../domain/model/NotificationMessage.java` — POJO Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`). Campos EXACTOS (nombres JSON = nombres de campo):

| Campo | Tipo | Notas |
|---|---|---|
| `notificationId` | `String` | Id de la notificación (lo usa el processor para status/log). |
| `type` | `String` | Canal: `EMAIL` / `SMS` / `PUSH` / `WHATSAPP` (ver `NotificationChannel` enum; `factory.create(type)` hace `type.toUpperCase()`). |
| `recipient` | `String` | Teléfono E.164 o email. |
| `templateCode` | `String` | Código de plantilla, p.ej. `VERIFICACION_OTP`. **Obligatorio** (`validate()` lo exige). |
| `language` | `String` | p.ej. `es`. Si null, el processor usa `es`. |
| `params` | `Map<String,String>` | Variables de plantilla (p.ej. `otp_code`, `expiration_minutes`, `provider_name`, `verification_link`, `expiration_hours`). **Debe ser Map<String,String>.** |
| `requiresConnection` | `boolean` | No usado en el camino transaccional; enviar `false`. |
| `attachments` | `List<Attachment>` | Opcional; omitir / `null`. |
| `createdAt` | `String` | Opcional; timestamp ISO como String. |

### Exchange y routing keys (de `RabbitMqConfig.java` + `application.yml` del processor)
- Exchange: **`findu.notifications`** (TopicExchange, durable).
- Colas y bindings:
  - PUSH → cola `findu.notifications.push` ← rk `notification.push`
  - EMAIL → cola `findu.notifications.correo` ← rk `notification.correo` **y** `notification.email`
  - SMS → cola `findu.notifications.celular` ← rk `notification.celular` **y** `notification.sms`
  - WHATSAPP → cola `findu.notifications.whatsapp` ← rk `notification.whatsapp`

### Deserialización del consumer
El processor consume con `@RabbitListener` y tiene un bean `Jackson2JsonMessageConverter`. Para que mapee a `NotificationMessage`, el mensaje publicado por el dispatcher **debe llevar `contentType=application/json`** (es decir, publicar con un `Jackson2JsonMessageConverter`, no un String crudo con `text/plain`). El `RabbitFallbackProducer` actual envía un String JSON con `text/plain`; por eso el nuevo productor usa su propio converter JSON y NO reutiliza el camino del fallback.

---

## Mapeo payload de entrada → body publicado

Entrada: `NotificationDispatchController.DispatchNotificationRequest`
`{ String type, String recipient, String templateCode, String language, Map<String,Object> params }`
(Confirmado contra el emisor real: `findu-spring-security` `NotificationLambdaAdapter` envía exactamente `type, recipient, templateCode, language, params`.)

| Campo salida (`NotificationMessage`) | Origen |
|---|---|
| `notificationId` | Generar `UUID.randomUUID().toString()` en el dispatcher. |
| `type` | `request.type()` tal cual (`SMS`/`EMAIL`/`PUSH`/`WHATSAPP`). |
| `recipient` | `request.recipient()`. |
| `templateCode` | `request.templateCode()`. |
| `language` | `request.language()` (default `"es"` si null). |
| `params` | `request.params()` convertido a `Map<String,String>` (ver nota de conversión). |
| `requiresConnection` | `false`. |
| `attachments` | `null` (omitir). |
| `createdAt` | `Instant.now().toString()`. |

Nota de conversión de `params`: la entrada es `Map<String,Object>` pero `NotificationMessage.params` es `Map<String,String>`. Convertir cada valor con `String.valueOf(v)` (ignorando entradas null). Las claves de control que el controller inyecta hoy para el flujo de presencia (`templateCode`, `type`, `tipoEvento`) **no** se agregan a `params` en la ruta transaccional.

Mapeo `type` → routing key (en el nuevo productor):
- `SMS` → `notification.sms`
- `EMAIL` → `notification.email`
- `PUSH` → `notification.push`
- `WHATSAPP` → `notification.whatsapp`

(Se eligen las rk `notification.sms` / `notification.email` porque el processor las bindea como alias equivalentes a `celular`/`correo`; cualquiera de las dos entrega a la misma cola. Se usa la que coincide 1:1 con el nombre del canal para claridad.)

---

## NUEVOS ELEMENTOS (lo que el usuario debe revisar antes del refactor)

### Clases/beans NUEVOS
1. **`TransactionalNotificationProducer`** (nueva clase)
   `.../infrastructure/adapter/rabbitmq/TransactionalNotificationProducer.java`
   - Publica al exchange `findu.notifications` con la rk derivada del `type`.
   - Usa un `RabbitTemplate` propio configurado con `Jackson2JsonMessageConverter` (para `contentType=application/json`).
   - Método: `Mono<Void> publish(NotificationMessagePayload payload)` que elige la rk según `type`.

2. **`NotificationMessagePayload`** (nuevo DTO/record)
   `.../presentation/controller/dto/NotificationMessagePayload.java` (o `domain/model/`)
   - Record con los campos EXACTOS del `NotificationMessage` del processor: `notificationId, type, recipient, templateCode, language, params (Map<String,String>), requiresConnection, createdAt`.
   - Serializa a JSON idéntico al que el processor deserializa. (No se importa la clase del processor; es otro módulo.)

3. **Bean `Jackson2JsonMessageConverter notificationJsonConverter`** y **bean `RabbitTemplate notificationsRabbitTemplate`**
   Añadidos en `config/RabbitMQConfig.java` (dispatcher).
   - El `RabbitTemplate` nuevo (cualificado, p.ej. `@Qualifier("notificationsRabbitTemplate")`) usa el converter Jackson y NO interfiere con el `RabbitTemplate` por defecto que usa `RabbitFallbackProducer`.

4. **Declaración del exchange destino** en `config/RabbitMQConfig.java` (dispatcher):
   - Bean `TopicExchange notificationsExchange()` para `findu.notifications` (durable, no auto-delete), para que el exchange exista aunque el processor aún no haya arrancado. **No** declarar las colas del processor (las declara el processor; el dispatcher solo publica al exchange).

### Propiedades NUEVAS (parametrizadas, con defaults que coinciden con el processor; NO requieren nuevas env vars en Railway)
En `application.yml` del dispatcher, bloque `findu.rabbitmq`:
```yaml
findu:
  rabbitmq:
    notification:
      exchange: ${FINDU_NOTIFICATION_EXCHANGE:findu.notifications}
      routing-keys:
        sms: ${FINDU_NOTIFICATION_RK_SMS:notification.sms}
        email: ${FINDU_NOTIFICATION_RK_EMAIL:notification.email}
        push: ${FINDU_NOTIFICATION_RK_PUSH:notification.push}
        whatsapp: ${FINDU_NOTIFICATION_RK_WHATSAPP:notification.whatsapp}
```
Los defaults bastan en Railway (perfil `qa`), así que no hay que tocar variables de entorno. `application-qa.yml` **no** necesita cambios (hereda los defaults de `application.yml`).

### Archivos a MODIFICAR
- `.../presentation/controller/NotificationDispatchController.java` — bifurcar: si `templateCode != null && type` es de canal (`SMS/EMAIL/PUSH/WHATSAPP`), construir `NotificationMessagePayload` y llamar al `TransactionalNotificationProducer`; en caso contrario, conservar la ruta actual por `EventDispatcherService` (presencia).
- `.../config/RabbitMQConfig.java` — añadir exchange destino, converter Jackson y `RabbitTemplate` cualificado nuevos.
- `src/main/resources/application.yml` — añadir el bloque de propiedades de arriba.

### Qué NO se toca
- `findu-notification-processor` (SOLO LECTURA): `NotificationMessage`, `RabbitMqConfig`, `NotificationConsumer`, colas, bindings, senders SNS/SES/FCM.
- `findu-spring-security`: el `NotificationLambdaAdapter` ya envía el payload correcto; no cambia.
- Flujo WebSocket de presencia: `EventDispatcherService`, `RabbitFallbackProducer`, `EventFallbackPort`, `EventEnvelope`, `EventType.OFFER_RECEIVED`, exchanges `findu.events.exchange` / `findu.notifications.fallback.exchange`. Siguen igual para ofertas en tiempo real.

---

## Comando de build/test del módulo (descubierto)

- Build system: Gradle (wrapper `gradlew.bat`), Spring Boot 3.4.2, Java 21, Windows/PowerShell.
- Dependencia AMQP ya presente (`spring-boot-starter-amqp`), Jackson jsr310 presente, Lombok presente. No hay deps nuevas.
- Comando de compilación/test del módulo dispatcher:
  `./gradlew.bat :build` dentro de `CORE/LAMBDA/findu-notification-dispatcher`, o desde el módulo: `.\gradlew.bat build`.
- Actualmente el módulo no tiene tests unitarios propios (solo el starter-test en deps). Se añadirán tests nuevos en los pasos.

---

## Plan ordenado por dependencias

- [ ] 1. Crear el DTO `NotificationMessagePayload` con los campos EXACTOS del `NotificationMessage` del processor (`notificationId, type, recipient, templateCode, language, params:Map<String,String>, requiresConnection, createdAt`), anotado para serializar JSON compatible (`@JsonInclude(NON_NULL)`).
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/presentation/controller/dto/NotificationMessagePayload.java`
      Verify: `.\gradlew.bat compileJava` en el módulo dispatcher — compila sin errores.

- [ ] 2. Añadir en `RabbitMQConfig` (dispatcher) el bean `TopicExchange` para `findu.notifications`, un bean `Jackson2JsonMessageConverter notificationJsonConverter`, y un `RabbitTemplate notificationsRabbitTemplate` cualificado que use ese converter. No tocar los beans existentes (events/fallback) ni el `RabbitTemplate` por defecto.
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/config/RabbitMQConfig.java`
      Verify: `.\gradlew.bat compileJava` — compila; revisar que no haya conflicto de beans `RabbitTemplate` (el cualificado + el autoconfigurado por defecto siguen inyectables por `@Qualifier`).

- [ ] 3. Añadir el bloque de propiedades `findu.rabbitmq.notification.*` (exchange + routing-keys por canal con defaults que coinciden con el processor) en `application.yml` del dispatcher. No modificar `application-qa.yml`.
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/resources/application.yml`
      Verify: `.\gradlew.bat processResources` — sin errores de YAML; arranque local con perfil `dev` levanta el contexto (paso 6 lo cubre con test).

- [ ] 4. Crear `TransactionalNotificationProducer`: inyecta el `RabbitTemplate` cualificado (paso 2) y lee las propiedades (paso 3); método `Mono<Void> publish(NotificationMessagePayload)` que mapea `type` → routing key y publica al exchange `findu.notifications`. Loggea recipient/templateCode/type/rk.
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/infrastructure/adapter/rabbitmq/TransactionalNotificationProducer.java`
      Verify: `.\gradlew.bat compileJava` — compila.

- [ ] 5. Modificar `NotificationDispatchController.dispatchNotification(...)`: bifurcar el flujo. Si `templateCode` no es blank y `type` ∈ {SMS,EMAIL,PUSH,WHATSAPP} (case-insensitive) → construir `NotificationMessagePayload` (mapeo de la sección "Mapeo", convirtiendo `params` a `Map<String,String>` con `String.valueOf`) y llamar `transactionalNotificationProducer.publish(...)`. En cualquier otro caso → conservar exactamente la ruta actual por `EventDispatcherService` (presencia/ofertas). Devolver `200 OK`.
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/main/java/com/findu/dispatcher/presentation/controller/NotificationDispatchController.java`
      Verify: `.\gradlew.bat compileJava` — compila; revisión manual de que la rama de presencia quedó idéntica.

- [ ] 6. Añadir test unitario del controller que verifique la bifurcación: (a) request `type=SMS`+`templateCode` invoca `TransactionalNotificationProducer.publish` con un payload cuyos campos mapean 1:1 (y `params` como Map<String,String>) y NO invoca `EventDispatcherService`; (b) un evento sin `templateCode` (o `type` no de canal) sí invoca `EventDispatcherService.dispatch` y NO el productor transaccional. Usar Mockito (incluido en `spring-boot-starter-test`).
      Files: `CORE/LAMBDA/findu-notification-dispatcher/src/test/java/com/findu/dispatcher/presentation/controller/NotificationDispatchControllerTest.java`
      Verify: `.\gradlew.bat test --tests "*NotificationDispatchControllerTest"` — todos los tests nuevos pasan.

- [ ] 7. Build completo del módulo para asegurar estado compilable y verde.
      Files: (ninguno — verificación)
      Verify: `.\gradlew.bat build` dentro de `CORE/LAMBDA/findu-notification-dispatcher` — BUILD SUCCESSFUL con los tests pasando.

---

## Notas y supuestos

- Supuesto: `type` de los requests transaccionales llega en mayúsculas (`SMS`, `EMAIL`), consistente con lo que envía `findu-spring-security` y con `NotificationChannel`. El controller normaliza con `toUpperCase()` por robustez.
- Supuesto: no se requiere persistencia de `notificationId` en el dispatcher; el processor lo usa para su propio status (DynamoDB). El dispatcher solo lo genera y lo propaga.
- Se publica con rk `notification.sms`/`notification.email` (alias del processor hacia `celular`/`correo`); entregan a la misma cola. Si se prefiriera `notification.celular`/`notification.correo`, basta cambiar los defaults de las propiedades del paso 3 sin tocar código.
- El exchange se declara también desde el dispatcher (durable) para evitar fallos si el dispatcher publica antes de que el processor haya declarado el exchange; al ser idempotente y con los mismos atributos (TopicExchange durable), no colisiona con la declaración del processor.
