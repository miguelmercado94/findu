# Verificación de identidad del proveedor (FEAT-001)

Flujo de verificación de identidad para la app del PRESTADOR/PROVEEDOR en el microservicio
`SECURITY/findu-spring-security` (Spring Boot WebFlux reactivo, R2DBC, arquitectura hexagonal):

- Teléfono: validación **síncrona** por OTP de 6 dígitos (bloquea el avance del registro).
- Correo: validación **asíncrona** por enlace mágico con token criptográfico.

El código de país por defecto es `+57` (Colombia).

---

## 1. Plantillas en el microservicio de notificaciones

El servicio de seguridad NO envía SMS/correo directamente por SES/SNS: delega en el
microservicio de notificaciones a través de `NotificationPort.send(type, recipient, templateCode, lang, params)`
(fire-and-forget; se apaga con `findu.notification.enabled=false`, valor por defecto en dev).

Los `templateCode` nuevos están declarados en `util/NotificationTemplates.java`:
`VERIFICACION_OTP` y `VERIFICACION_EMAIL`.

El microservicio de notificaciones resuelve la plantilla en la tabla DynamoDB
`notification_templates`, cuya clave de partición es `id = CODE#CHANNEL#LANG`. Hay que registrar:

### VERIFICACION_OTP#SMS#es

Mensaje corto para SMS/WhatsApp. Placeholders: `${otp_code}`, `${expiration_minutes}`.

```
Tu código de verificación FIND-U para prestadores es: ${otp_code}. No lo compartas. Válido por ${expiration_minutes} minutos.
```

### VERIFICACION_EMAIL#EMAIL#es

Correo HTML responsivo de onboarding. Placeholders: `${provider_name}`, `${verification_link}`,
`${expiration_hours}`. Debe incluir un botón CTA "Confirmar mi correo electrónico" que apunte a
`${verification_link}` y un texto de seguridad del tipo "si no solicitaste esta cuenta, ignora este mensaje".

Ejemplo de cuerpo HTML (responsivo):

```html
<!DOCTYPE html>
<html lang="es">
  <body style="font-family:Arial,Helvetica,sans-serif;background:#f5f5f5;margin:0;padding:24px;">
    <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:8px;padding:32px;">
      <h1 style="font-size:20px;color:#1a1a1a;">Hola ${provider_name}, confirma tu correo</h1>
      <p style="color:#444;font-size:15px;line-height:1.6;">
        Para activar tu cuenta de prestador en FIND-U necesitamos verificar tu correo electrónico.
      </p>
      <p style="text-align:center;margin:32px 0;">
        <a href="${verification_link}"
           style="background:#2e7d32;color:#fff;text-decoration:none;padding:14px 28px;border-radius:6px;font-size:15px;display:inline-block;">
          Confirmar mi correo electrónico
        </a>
      </p>
      <p style="color:#777;font-size:13px;line-height:1.6;">
        El enlace es válido por ${expiration_hours} horas. Si no solicitaste esta cuenta, ignora este mensaje.
      </p>
    </div>
  </body>
</html>
```

---

## 2. Backend: endpoints y estados

Base-path del servicio: `/security-auth`. Controlador: `presentation/controller/ProviderVerificationController.java`
(`@RequestMapping("/api/v1/auth")`).

| Método | Ruta                                   | Body / Query                                             | Respuesta                                                   |
|--------|----------------------------------------|----------------------------------------------------------|-------------------------------------------------------------|
| POST   | `/api/v1/auth/phone/send-otp`          | `{ phone, codPhoneInternational? }`                      | `200` vacío                                                 |
| POST   | `/api/v1/auth/phone/verify-otp`        | `{ phone, code, codPhoneInternational? }`                | `200 PhoneVerificationResponse { phoneVerified, accountStatus }` |
| POST   | `/api/v1/auth/email/send-verification` | `{ email }`                                              | `200` vacío                                                 |
| GET    | `/api/v1/auth/email/verify`            | `?token=<raw>`                                           | `200 text/html` (página de éxito o fallo)                   |

### Estados (`account_status`)

```
PENDING_PHONE_VERIFICATION  --verify-otp-->  PENDING_EMAIL_VERIFICATION  --email/verify-->  DOCUMENTS_PENDING  --(fuera de FEAT-001)-->  ACTIVE
```

- `phone_verified` (boolean) y `email_verified` (boolean) viven en la tabla `"user"`.
- La tabla `email_verification_token` guarda **solo el hash** del token (SHA-256 determinista,
  `util/TokenHasher`), nunca el valor en claro; el token en claro viaja en el enlace del correo.
- El OTP de teléfono se guarda hasheado (`PasswordEncoder`) en `PhoneOtpStorePort`
  (Redis cuando `findu.redis.enabled=true`, fallback in-memory en dev/tests) con TTL de 300 s,
  bajo la clave `findu:otp:phone:<e164>`.

### Perfil y login federado

- `UserProfileResponse` incluye ahora `phoneVerified`, `emailVerified` y `accountStatus`,
  poblados en `JwtManagerImpl.toUserProfileResponse`.
- En `loginFederated`, al usuario nuevo se le asigna `accountStatus=PENDING_PHONE_VERIFICATION`
  y, si su correo no está verificado, se dispara el correo de verificación de forma asíncrona
  (fire-and-forget) sin bloquear la emisión del token.

### URL del enlace de verificación

Por defecto (configurable con la variable `FINDU_AUTH_EMAIL_VERIFY_BASE_URL`):

```
https://api-gateway-production-ff97.up.railway.app/security-auth/api/v1/auth/email/verify?token=<raw>
```

---

## 3. Decisión de autorización: los 4 endpoints son públicos (`permite_all=TRUE`)

La autorización de FIND-U es por BD (tabla `operation`: `module.path_base` + `operation.path` +
`http_method`, con `permite_all`), no solo en `SecurityConfig`. En `data.sql` se siembran las 4
operaciones nuevas (ids 14–17) con `permite_all=TRUE` y, por coherencia, se conceden a los roles
1/2/3 y a `ROLE_OUR_PROVEEDOR` (role_id 4):

| id | método | ruta                                   | nombre                        |
|----|--------|----------------------------------------|-------------------------------|
| 14 | POST   | `/api/v1/auth/phone/send-otp`          | `AUTH_PHONE_SEND_OTP`         |
| 15 | POST   | `/api/v1/auth/phone/verify-otp`        | `AUTH_PHONE_VERIFY_OTP`       |
| 16 | POST   | `/api/v1/auth/email/send-verification` | `AUTH_EMAIL_SEND_VERIFICATION`|
| 17 | GET    | `/api/v1/auth/email/verify`            | `AUTH_EMAIL_VERIFY`           |

Motivos:

1. `GET /email/verify` se abre desde el correo **sin token de sesión**, así que debe ser público
   de forma obligatoria.
2. `send-otp` / `verify-otp` / `send-verification` deben funcionar para un proveedor recién
   registrado (`phone_verified=false`, `email_verified=false`,
   `account_status=PENDING_PHONE_VERIFICATION`) sin que la autorización bloquee el propio flujo de
   alta; se protegen por el secreto del OTP y del token mágico, igual que los endpoints ya
   existentes `forgot-password` / `reset-password`, que también son `permite_all`.
3. Aunque hoy el frontend ya tiene un JWT al llegar al OTP, hacerlos públicos evita bloqueos en el
   flujo federado y en estados intermedios. Concederlos además a los roles mantiene la coherencia
   si en el futuro se decide endurecerlos.

No se modificó `SecurityConfig` ni `AuthToken`.

---

## 4. Nota operativa: usuario de prueba `m.mercado.t.94@gmail.com`

- En `data.sql` el seed de ese usuario **no** lista las columnas nuevas, por lo que hereda los
  `DEFAULT` (`phone_verified=FALSE`, `email_verified=FALSE`,
  `account_status='PENDING_PHONE_VERIFICATION'`) y queda coherente con un flujo de verificación
  limpio; no se re-siembra como `ACTIVE`.
- En **QA/Railway** hay que **BORRAR** ese usuario antes de probar el flujo limpio
  (ya se borró manualmente en la BD de Railway). En dev, el reseed de `data.sql` lo deja en
  `PENDING_PHONE_VERIFICATION`.
