# Design — Provider Activation Flow (FIND-U)

Status: REVISED (pass 3, after design review pass 2 `design-review.md` — CHANGES_REQUESTED, 1 HIGH / 2 MEDIUM / 3 NIT, all addressed; see §14 for the pass-2 point-by-point responses and §13 for the pass-1 responses). Tier-3 / HEAVY. Crosses `findu-spring-security` (Postgres, R2DBC reactive), `findu-core` (Postgres — write-once profile photo), `findu-help-v2` (MongoDB, Spring MVC blocking), `findu-notification-processor` (DynamoDB template engine + RabbitMQ), the `findu-notification-dispatcher` (WebSocket/FCM presence router — unchanged), and the Flutter app `findu_proveedores`.

Worktree root for backend inspection/edits: `findu/.worktrees/provider-activation`. Use `git -C <that path>` for all backend git ops. Mobile lives in `findu-movil` (separate repo, not under the worktree).

---

## 1. Overview

A provider account becomes `ACTIVADO` only after four validations pass, strictly in this order:

1. **Phone OTP** (ACTIVE) — already implemented (`ProviderVerificationController` + `VerifyPhoneOtpUseCaseImpl`). Marks `phone_verified=true` and advances `account_status PENDING_PHONE_VERIFICATION -> PENDING_EMAIL_VERIFICATION`.
2. **Confirmation email** (PASSIVE) — the confirmation email must be sent *after* a successful OTP; the user confirms via the magic link (`VerifyEmailUseCaseImpl`) -> `email_verified=true`.
3. **Biometric cedula + face** (PASSIVE, OPTIONAL at registration) — done on wizard 'screen 2' or deferred. Name (accent-insensitive) and birth date are re-validated on the backend against the registered values; the face match is resolved client-side (TFLite, mocked in dev). The captured frontal photo becomes the profile photo and cannot be edited.
4. **Support approval** (ACTIVE, FINAL) — role `ROLE_OUR_SOPORTE` gets a NOTIFICATION + TICKET when validations 1–3 are complete, and approves manually. This approval moves the state to `ACTIVADO`.

A non-active state **must not block login**. The provider logs in normally; the profile surfaces `usuario no activo` and the pending INFO alerts. While email or biometric are missing, two `ALERTA` / `INFO` templates render on `perfil_proveedor`; the biometric one carries a `cta_action` deep link. When a validation completes, its alert disappears (the alert's `condicionAparicion` evaluates false).

The whole thing is built around the **existing** notification pipeline: security never talks to RabbitMQ directly — it POSTs to the dispatcher (`FINDU_NOTIFICATION_LAMBDA_URL -> /dispatchNotification`), which bifurcates: transactional types (`EMAIL`/`SMS`/`PUSH`/`WHATSAPP`) are published to the `findu.notifications` exchange for `findu-notification-processor`; presence types go through the WebSocket/FCM path. We reuse this verbatim.

---

## 2. Technology stack (locked once approved)

- **findu-spring-security**: Java 17, Spring Boot WebFlux (reactive), R2DBC PostgreSQL (`r2dbc-postgresql:1.0.7`), Spring Data R2DBC repositories, Lombok, Redis (OTP store), existing `NotificationPort` -> `NotificationLambdaAdapter` (WebClient). No new DB engine, no Flyway (schema is managed by `schema.sql`/`data.sql` + `scripts/load-postgres.ps1`; QA runs with `spring.sql.init.mode=never`).
- **findu-help-v2**: Java 17, Spring Boot **Spring MVC (blocking)**, MongoDB, Lombok, `RestClient` (already used by `HttpRealtimeEventPublisher`). No RabbitMQ dependency today — we will **not** add one (see §6.3).
- **findu-core**: Java 17, Spring Boot, Postgres. One scoped change: `GestionPerfilProveedorUseCaseImpl.actualizarPerfil` enforces write-once on `urlImagenPerfil` (§3.4 layer 2). No new engine/dependency.
- **findu-notification-processor**: Java 17, Spring Boot, DynamoDB (versioned template engine, PK `id=CODE#TIPO#LANG`, SK `v#<n>`/`v#ACTIVE`), RabbitMQ consumers. We add **template seed data** (4 new templates: 2 NOTIFICACION + 2 ALERTA) plus a seed of the legacy codes (§7) — no code change required for sending; one read endpoint for alerts (by-code lookup, no new GSI — see §7.4).
- **findu-notification-dispatcher**: unchanged. It already bifurcates transactional vs presence and forces FCM fallback when the user is offline/deactivated.
- **Mobile `findu_proveedores`**: Flutter, hexagonal per-feature (`domain/application/infrastructure/presentation`), shared `RestHttpClient` (`packages/network_rest`). Biometric real impl will use `google_mlkit_text_recognition`, `google_mlkit_face_detection`, `tflite_flutter` (arm64 device-only) — wired behind an interface, mock by default in dev.

---

## 3. State machine & data model (security — owner of activation state)

### 3.1 Provider global state and validation flags

`account_status` on `"user"` already drives the existing wizard (`PENDING_PHONE_VERIFICATION -> PENDING_EMAIL_VERIFICATION -> DOCUMENTS_PENDING -> ...`). We do **not** overload `account_status` with activation semantics — it keeps meaning "where in the signup wizard the user is". Instead we introduce a dedicated activation aggregate so activation is explicit and auditable (user decision: history + versioning for activation state).

New domain concept: **provider activation state** with its own column on `"user"` plus a child table of per-validation records with timestamps.

Global provider state (`estado_activacion` on `"user"`):

| State | Meaning |
|---|---|
| `PENDIENTE_VALIDACIONES` | one or more of OTP/email/biometric still missing (initial) |
| `EN_REVISION_SOPORTE` | OTP+email+biometric all done; support ticket+notification fired; awaiting manual approval |
| `ACTIVADO` | support approved; provider fully active |
| `RECHAZADO` | support rejected the provider |

Transitions (enforced in the application layer, see §3.4):

```
PENDIENTE_VALIDACIONES --(otp_ok && email_ok && biometria_ok)--> EN_REVISION_SOPORTE
EN_REVISION_SOPORTE     --(support approve)--------------------> ACTIVADO
EN_REVISION_SOPORTE     --(support reject)---------------------> RECHAZADO
RECHAZADO               --(support re-open / provider re-submits biometric)--> EN_REVISION_SOPORTE
```

The four flags live in a child table so each carries its own timestamp (audit + "when did this complete"):

### 3.2 Schema — additive migration (does NOT break Railway qa)

**Critical constraint:** the existing `schema.sql` begins with `DROP TABLE ...` and recreates everything. It is only run manually against a fresh DB. On Railway qa, `spring.sql.init.mode=never`, so Spring does not run it at startup. Therefore the activation changes must be delivered as an **additive, idempotent migration script** that is applied manually to the Railway qa Postgres (same operational model the project already uses). We will:

1. Add an idempotent block to `schema.sql` (so a fresh local bootstrap includes the new objects) using `CREATE TABLE IF NOT EXISTS` / `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, placed AFTER the existing table creation and BEFORE/within the DROP ordering fixed accordingly.
2. Add a new standalone script `scripts/migrations/V2__provider_activation.sql` containing only additive, idempotent DDL + the support role/user seed guarantee. This is the script run against Railway qa. It never drops or truncates existing tables.

New columns on `"user"` (additive). `soporte_notified` is the concrete storage for the ticket/notification idempotency guard the review flagged as undefined (Finding 8): it lives on the activation aggregate (the `"user"` row), is set `TRUE` only when help-v2 returns 2xx for the ticket, and is reset `FALSE` on a support rejection so a re-opened review can re-fire exactly once.

```sql
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS estado_activacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VALIDACIONES';
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS soporte_notified   BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS soporte_ticket_code VARCHAR(60);  -- código de ticket devuelto por help-v2 (NULL hasta que se crea)
```

New table `provider_activation_validation` (one row per validation per user, with history/versioning — never overwritten in place when re-done; a re-submission inserts a new version and the latest is the active one):

```sql
CREATE TABLE IF NOT EXISTS provider_activation_validation (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    tipo          VARCHAR(20) NOT NULL,          -- OTP | EMAIL | BIOMETRIA | SOPORTE
    estado        VARCHAR(20) NOT NULL,          -- PENDIENTE | OK | RECHAZADA
    version       INT NOT NULL DEFAULT 1,        -- histórico: cada re-intento incrementa
    is_current    BOOLEAN NOT NULL DEFAULT TRUE, -- fila vigente de ese tipo
    detalle       VARCHAR(500),                  -- motivo rechazo / notas (p.ej. soporte)
    completed_at  TIMESTAMP,                     -- cuándo pasó a OK/RECHAZADA
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    CONSTRAINT fk_pav_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);
CREATE INDEX IF NOT EXISTS ix_pav_user_tipo_current ON provider_activation_validation (user_id, tipo, is_current);
```

New table `provider_biometric_capture` (biometric payload + the frontal photo that becomes the immutable profile photo):

```sql
CREATE TABLE IF NOT EXISTS provider_biometric_capture (
    id                   BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    cedula_nombre        VARCHAR(200),   -- nombre OCR normalizado (sin tildes) tal como se comparó
    cedula_fecha_nac     DATE,           -- fecha OCR comparada
    face_match_score     NUMERIC(5,4) CHECK (face_match_score IS NULL OR face_match_score BETWEEN 0 AND 1),   -- score del match (cliente en dev-mock); CHECK garantiza el contrato 0.0–1.0 (Finding 13)
    foto_frontal_url     VARCHAR(500),   -- la foto que pasa a ser foto de perfil (inmutable)
    resultado            VARCHAR(20) NOT NULL, -- OK | RECHAZADA
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pbc_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);
```

The registered Full Name and birth date to compare against come from `findu-core`'s `perfil-proveedor` (fields `nombreCompleto`, `fechaNacimiento`), not from security's `"user"` (which has no name/birthdate columns). Resolution of those values is covered in §5.3.

`add_soporte.sql` already seeds `ROLE_OUR_SOPORTE` (id 5) and the `soporte` user. The migration script must guarantee that seed exists (re-run `add_soporte.sql` content idempotently) so the support ticket can address a real role. **Terminology note:** the task says `rol_soporte`; in this codebase that is `ROLE_OUR_SOPORTE`. We use `ROLE_OUR_SOPORTE` everywhere.

#### 3.2.1 New authorization operations + role grants (concrete, Finding 5)

The authorization model (verified in `data.sql`) keys on `operation` rows (`permite_all` + `module_id`) and `rol_operation` grants; an endpoint with `permite_all=FALSE` requires an explicit `rol_operation` row for the calling role. Existing operation ids run 1–17 (auto-increment identity; `data.sql` omits the id and tracks it in a trailing comment). The new activation endpoints are **caller-specific**:

- `POST /api/v1/activation/biometric` and `GET /api/v1/activation/status` are called by the **provider** → granted to `ROLE_OUR_PROVEEDOR` (**id 4**), NOT support.
- `POST /api/v1/activation/approve` and `POST /api/v1/activation/reject` are called by **support** → granted to `ROLE_OUR_SOPORTE` (**id 5**).

**All four endpoint paths are STATIC (no path variables).** This is a hard requirement imposed by how authorization resolves operations, verified against `FinduReactiveAuthorizationManager.resolveOperation` → `OperationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(modulePathBase, apiPath, httpMethod)`, whose SQL is `... AND o.path = :path AND o.http_method = :httpMethod ...` — **exact string equality** against the live request path. A request to `/api/v1/activation/42/approve` would never equal a stored `/api/v1/activation/{userId}/approve`; `resolveOperation` would return empty and `check()` would fall through `switchIfEmpty` → `new AuthorizationDecision(false)`, denying the endpoint for **every** caller, support included. The matcher has no path-template support and every existing operation (ids 6/8/9 = `/api/v1/customers`, `/api/v1/profile`) is a static literal path — there is no path-variable precedent to lean on (review Finding 1, HIGH).

**Decision (chosen): approve/reject carry `userId` in the request body, not the path.** `POST /api/v1/activation/approve` with body `{ "userId": 42 }` and `POST /api/v1/activation/reject` with body `{ "userId": 42, "motivo": "..." }`. The stored `operation.path` is the exact static string `/api/v1/activation/approve` / `/api/v1/activation/reject`, which the exact-equality resolver matches. This mirrors the existing static-path convention and needs **zero** change to the shared authorization code. (Rejected alternative: change `FinduReactiveAuthorizationManager`/`OperationRepository` to path-template matching — a scoped change to shared security authorization code affecting every protected route, far larger blast radius for a two-endpoint need; the design explicitly keeps the authorization manager unchanged, §2/§11.)

New operation rows (all `permite_all=FALSE`, `module_id=3` = the existing `PROFILE` module since they are provider-profile/activation operations). Ids continue from 17, so **18–21**. Every `path` is a static literal (what the exact-equality resolver compares against):

| id | path | name | http_method | module_id | permite_all | granted to role(s) |
|----|------|------|-------------|-----------|-------------|--------------------|
| 18 | `/api/v1/activation/biometric` | `ACTIVATION_BIOMETRIC_SUBMIT` | POST | 3 | FALSE | 4 (provider) |
| 19 | `/api/v1/activation/status` | `ACTIVATION_STATUS_READ` | GET | 3 | FALSE | 4 (provider) |
| 20 | `/api/v1/activation/approve` | `ACTIVATION_APPROVE` | POST | 3 | FALSE | 5 (support) |
| 21 | `/api/v1/activation/reject` | `ACTIVATION_REJECT` | POST | 3 | FALSE | 5 (support) |

**Where these rows go, and idempotency.** Two delivery contexts, kept consistent:

- In `data.sql` (fresh-DB bootstrap, uses plain `INSERT` with no `ON CONFLICT`): append the four `operation` INSERTs after id 17 (comment the ids), and append the matching `rol_operation` INSERTs (role 4 → ops 18,19; role 5 → ops 20,21). Because `data.sql` runs once against an empty DB, no conflict clause is needed there — matching the existing file style.
- In the additive migration `scripts/migrations/V2__provider_activation.sql` (run against the live Railway qa DB, which already has ids 1–17): insert the operations **by explicit id with `OVERRIDING SYSTEM VALUE`** and `ON CONFLICT (id) DO NOTHING`, then the `rol_operation` grants with `ON CONFLICT DO NOTHING`, and re-apply `add_soporte.sql` content idempotently (role 5 + soporte user). Example:

```sql
INSERT INTO operation (id, path, name, http_method, module_id, permite_all, active) OVERRIDING SYSTEM VALUE VALUES
  (18, '/api/v1/activation/biometric', 'ACTIVATION_BIOMETRIC_SUBMIT', 'POST', 3, FALSE, TRUE),
  (19, '/api/v1/activation/status',    'ACTIVATION_STATUS_READ',      'GET',  3, FALSE, TRUE),
  (20, '/api/v1/activation/approve',   'ACTIVATION_APPROVE',          'POST', 3, FALSE, TRUE),
  (21, '/api/v1/activation/reject',    'ACTIVATION_REJECT',           'POST', 3, FALSE, TRUE)
ON CONFLICT (id) DO NOTHING;
-- realinear la secuencia identity tras insertar ids explícitos:
SELECT setval(pg_get_serial_sequence('operation','id'), (SELECT MAX(id) FROM operation));
INSERT INTO rol_operation (role_id, operation_id, active) VALUES
  (4, 18, TRUE), (4, 19, TRUE), (5, 20, TRUE), (5, 21, TRUE)
ON CONFLICT DO NOTHING;
```

The `setval` realignment is required because `data.sql`/`schema.sql` rely on the identity sequence for subsequent inserts; inserting explicit ids 18–21 without realigning would collide on the next auto id. **Path-matching is now a non-issue:** all four `operation.path` values are static literal strings equal to the concrete request path, so the exact-equality resolver (`o.path = :path`) matches them directly, exactly like the existing ids 6/8/9. There is no path-template matching to confirm and no shared-code change.

**Approval-authority scoping (review Finding 6, NIT).** `add_soporte.sql` seeds the `soporte` user with BOTH role 5 (`ROLE_OUR_SOPORTE`, via `user_rol`) AND role 3 (`ROLE_ADMINISTRATOR`), and grants role 5 operations 1–12 only. The approve/reject operations (20/21) are granted to **role 5 exclusively** by the migration above; they are **NOT** granted to role 3. Therefore, for the seed user, approval authority flows through its role-5 membership. Do **not** add ops 20/21 to any `ROLE_ADMINISTRATOR` grant unless admins are intentionally meant to approve providers; keeping 20/21 role-5-only preserves the "support acts via role 5" framing even though the seed user also carries role 3.

### 3.3 Hexagonal layout in security (match existing style)

Following the existing `domain.model` / `infrastructure.entity` / `infrastructure.repository` / `application.usecase(.impl)` / `application.port.output.persistence` split:

- Domain: `domain/model/ProviderActivation.java` (aggregates estado + 4 flags), `ProviderActivationState` enum, `ValidationType` enum, `ValidationState` enum.
- Entities: `infrastructure/entity/ProviderActivationValidationEntity.java`, `ProviderBiometricCaptureEntity.java`. (`estado_activacion` added to existing `UserEntity`/`Usuario`.)
- Repositories: `infrastructure/repository/ProviderActivationValidationRepository.java` (R2DBC `ReactiveCrudRepository<...,Long>` + `@Query` for current rows), `ProviderBiometricCaptureRepository.java`.
- Ports: `application/port/output/persistence/ProviderActivationRepositoryPort.java` + impl adapter in `infrastructure/adapter/persistence`.
- Use cases: `SubmitBiometricUseCase`, `GetActivationStatusUseCase`, `ApproveProviderUseCase`, `RejectProviderUseCase`, plus an internal `ActivationProgressService` that recomputes `estado_activacion` and creates the support ticket (the sole support signal, §6.2) when validations 1–3 complete.

### 3.4 Invariant ownership

- **Ordering invariant (email after OTP; support only after OTP+email+biometric):** owned by the **security application layer** (`ActivationProgressService`), because security is the single source of truth for the flags. Each validation use case sets its flag then calls `ActivationProgressService.recompute(userId)` which (a) refuses to mark `biometria_ok` meaningful before `otp_ok && email_ok`? — No: biometric is OPTIONAL/deferrable and may physically complete before email confirmation. The ordering we enforce is specifically: **the support ticket+notification fire only when `otp_ok && email_ok && biometria_ok` are all true**, and the state only reaches `EN_REVISION_SOPORTE` then. Email *sending* is sequenced after OTP by the existing flow (we only guarantee the trigger point, §5.2).
- **"email sent after OTP" invariant:** owned by security — we trigger `SendEmailVerificationUseCase` from inside the successful OTP verification path (§5.2).
- **Immutable profile photo invariant:** owned by **three layers**, and we must be precise because the review confirmed core does NOT protect it today (Finding 3). `findu-core`'s `ActualizarPerfilProveedorRequest` is `record(String nombreCompleto, String urlImagenPerfil)` — `urlImagenPerfil` is editable through `PUT /api/v1/perfil-proveedor/{id}`, so core currently allows any client to overwrite the frontal photo. We do NOT claim core already blocks it. Enforcement, concretely:
  1. **Security (authoritative source of truth):** stores `foto_frontal_url` write-once — the biometric submit sets it only when it is currently null; a second biometric pass never overwrites a non-null value. Security is the system of record for the activation frontal photo.
  2. **findu-core (scoped change this iteration):** the editable path is `GestionPerfilProveedorUseCaseImpl.actualizarPerfil(Long id, ActualizarPerfilProveedorRequest request)` (invoked by `PerfilProveedorController` `PUT /api/v1/perfil-proveedor/{id}`), verified in the worktree — there is **no** `PerfilProveedorService.actualizar` method (that service exposes only `save`/`findById`/`findByAuthUserId`/`existsByAuthUserId`/`findEligibleProviders`/`findAvailableProviders`), so the write-once rule must land in the use case, not the service (review Finding 2, MEDIUM). Its body today is `if (request.urlImagenPerfil() != null) perfil.setUrlImagenPerfil(request.urlImagenPerfil());` — an unconditional overwrite. Change it to enforce write-once while still allowing `nombreCompleto` edits:

  ```java
  // GestionPerfilProveedorUseCaseImpl.actualizarPerfil
  if (request.nombreCompleto() != null) {
      perfil.setNombreCompleto(request.nombreCompleto());
  }
  // write-once: conserva una foto frontal ya fijada; solo setea si aún no hay una
  if (request.urlImagenPerfil() != null && perfil.getUrlImagenPerfil() == null) {
      perfil.setUrlImagenPerfil(request.urlImagenPerfil());
  }
  ```

  The getter is `PerfilProveedor.getUrlImagenPerfil()` (confirmed on the domain model; the DTO/response field is `urlImagenPerfil`). This closes the open overwrite path through the core `PUT`. This is a real core change and is listed in §11. (Chosen over adding a new 403 branch because silently keeping the existing photo is friendlier to the mobile update flow that resends the full record; the write-once rule still holds.)
  3. **Mobile UI:** the profile frontal-photo widget is read-only (no edit affordance) once `fotoPerfilFrontalUrl` is set.
  The authoritative enforcement is layers 1+2 (backend); mobile UI is defense-in-depth.
- **Idempotency of support ticket+notification:** owned by security's `ActivationProgressService` (guard flag `soporte_notified`) AND defensively by help-v2's create-ticket (dedup key). See §6.3.

---

## 4. Alerts (type ALERTA / category INFO)

Two templates seeded in DynamoDB (`notification_templates`), both `tipo=ALERTA`, `categoria=INFO`, `pantalla_destino=perfil_proveedor`. **For an ALERTA, author the title under `alert_meta.titulo`, not the top-level `title`** — verified against `NotificationTemplateItem` / `NotificationTemplateItemMapper.toAlertMeta/putAlertMeta`: for `tipo=ALERTA` the mapper writes `categoria`, `pantalla_destino`, `titulo`, `cta_label`, `cta_action`, `cerrable`, `prioridad`, `vigencia_desde`, `vigencia_hasta`, `condicion_aparicion`, `tiempo_desaparicion` under the `alert_meta` map, and the alert read path (`toAlertMeta`) exposes the title via `AlertMeta.titulo`. The top-level `title` attribute is NOT tipo-restricted — `putS(item, TITLE, tpl.getTitle())` writes it for **any** tipo whenever `getTitle()` is non-null (review Finding 4, NIT) — but it is read into `NotificationTemplate.title` and is simply **ignored by the alert consumer**, which reads `alert_meta.titulo`. So do not set a top-level `title` on an ALERTA (it would be dead data); put the alert title in `alert_meta.titulo`. Only `body_template` (and the common keys `codigo`/`tipo`/`language`/`variables`) sit at the top level for an alert.

Canonical alert condition identifiers: the two alerts are evaluated **client-side** against the exact field names the profile DTO exposes. The canonical flags are **`emailOk`** and **`biometriaOk`** (booleans). These same identifiers appear verbatim in (a) the template `condicion_aparicion`, (b) the security profile DTO (§5.4), and (c) the mobile expression evaluator (§8.2). We drop `emailVerified`/`biometriaVerificada` from the alert expressions to remove the name drift the review flagged (Finding 4). The supported identifier set the client evaluator must understand is exactly `{emailOk, biometriaOk, otpOk, soporteOk}` (all booleans) with the operators `==`/`!=` against `true`/`false`.

`tiempo_desaparicion` semantics (locked, Finding 7): the value is **seconds**; **`0` means no auto-dismiss** — the alert's visibility is governed solely by `condicion_aparicion`, so the alert disappears the instant its flag flips and never on a timer. A future `SEGURIDAD`/`PREVENTIVA` alert that must self-dismiss sets a positive seconds value; the client starts a timer on render and hides the alert when it elapses (independently of the condition). Both INFO alerts here use `0`.

Template 1 — **`PERFIL_CORREO_PENDIENTE`** ("Confirma tu correo"), authored against `AlertMeta`:

```
tipo            = ALERTA
language        = es
body_template   = "Confirma tu correo para avanzar en la activación de tu cuenta."   // top-level
alert_meta = {
  categoria            = INFO,
  pantalla_destino     = perfil_proveedor,
  titulo               = "Confirma tu correo",
  cta_label            = null,          // confirmation is via the email magic link; no in-app CTA
  cta_action           = null,
  cerrable             = false,
  prioridad            = 1,
  condicion_aparicion  = "emailOk == false",
  tiempo_desaparicion  = 0
}
```

Template 2 — **`PERFIL_BIOMETRIA_PENDIENTE`** ("Completa tu validación biométrica"), authored against `AlertMeta`:

```
tipo            = ALERTA
language        = es
body_template   = "Completa tu validación biométrica para activar tu cuenta."        // top-level
alert_meta = {
  categoria            = INFO,
  pantalla_destino     = perfil_proveedor,
  titulo               = "Completa tu validación biométrica",
  cta_label            = "Validar ahora",
  cta_action           = "findu://perfil/verificacion-biometrica",   // deep link, see §8.2 route note
  cerrable             = false,
  prioridad            = 2,
  condicion_aparicion  = "biometriaOk == false",
  tiempo_desaparicion  = 0
}
```

Per the template-engine design §5 **hybrid** rule and the task's note that the "frontend ya podrá jugar", INFO/profile alerts are **evaluated client-side** against the profile state the app already holds. This avoids an extra backend round-trip and makes the alert disappear instantly when the flag flips. The alert *content and metadata* (title, body, category, target screen, CTA) are owned by the template engine and must be fetchable so content stays admin-editable without an app release — hence §7.4.

The two alerts are category `INFO` (per the task). (The template-engine doc example used `SEGURIDAD`; the task is authoritative — these are `INFO`.)

---

## 5. Security service — flows

### 5.1 Login does not block (confirm existing behavior holds)

Login already succeeds regardless of `account_status`; `Usuario.isEnabled()` returns `active` (the account's active flag), not activation state. Adding `estado_activacion` does **not** touch `isEnabled`, `isAccountNonLocked`, or any authentication filter. No change to auth config. Verified against `Usuario.java` and the login path. The profile endpoint simply exposes the new fields (§5.4).

### 5.2 Trigger email after successful OTP

Today `VerifyPhoneOtpUseCaseImpl` marks `phoneVerified` and advances status but does **not** send the confirmation email. The mobile wizard calls `send-email-verification` separately. To satisfy "ensure the email fires after a successful OTP", we add, inside `VerifyPhoneOtpUseCaseImpl` after the user is saved and the OTP deleted:

- mark `otp_ok` in `provider_activation_validation` (insert/update the current OTP row to `OK`), then **compose** the email send into the reactive chain rather than detaching it with a bare `.subscribe()` (Finding 11). The snippet below is **pseudocode aligned to the real use case** — `VerifyPhoneOtpUseCaseImpl` today saves via `usuarioService.save(user)`, deletes the OTP via `phoneOtpStore.delete(e164)`, and returns `new PhoneVerificationResponse(saved.isPhoneVerified(), saved.getAccountStatus())` (review Finding 5, NIT). The change inserts the `markOtpOk` write and the email send into that existing chain, between the OTP delete and the final `thenReturn`:

  ```java
  // dentro de VerifyPhoneOtpUseCaseImpl, tras validar el OTP:
  return usuarioService.save(user)                                  // guardado real existente
      .flatMap(saved -> phoneOtpStore.delete(e164)                  // borrado de OTP existente
          .then(markOtpOk(saved))                                   // NUEVO: fila OTP -> OK en provider_activation_validation
          .then(sendEmailVerificationUseCase.sendVerificationForUser(saved)   // NUEVO: email tras OTP OK
                    .onErrorResume(e -> {
                        log.warn("Email de verificación no enviado tras OTP (userId={}): {}", saved.getId(), e.getMessage());
                        return Mono.empty();   // no-fatal: el éxito del OTP no se bloquea por fallo de email
                    }))
          .thenReturn(new PhoneVerificationResponse(saved.isPhoneVerified(), saved.getAccountStatus())));  // retorno real existente
  ```

  Composing with `.then(...).onErrorResume(...)` keeps the Reactor context (and the Security context) attached, makes the ordering ("email after a successful OTP") explicit and testable with `StepVerifier`, and still treats the email as non-fatal. `markOtpOk` and `sendEmailVerificationUseCase.sendVerificationForUser` are the two new collaborators; the save/delete/return are the use case's existing operations.

Failure handling: email dispatch failure is swallowed via `onErrorResume` and logged at WARN; OTP verification success is **not** blocked by email send failure (recoverable; the user can resend from the wizard). `SendEmailVerificationUseCaseImpl` additionally logs its own downstream errors.

### 5.3 Biometric submit (backend re-validation; face match client-side in dev)

New endpoint on a new `ProviderActivationController` (base `/api/v1/activation`, authenticated — resolve user from the security context exactly like `VerifyPhoneOtpUseCaseImpl.resolveAuthenticatedUser()`):

`POST /api/v1/activation/biometric` — body `BiometricSubmitRequest`:

```json
{
  "cedulaNombre": "Carlos Alberto Mendoza",
  "cedulaFechaNacimiento": "1994-11-01",
  "faceMatchScore": 0.97,
  "faceMatched": true,
  "fotoFrontalUrl": "https://.../frontal.jpg"
}
```

Backend logic (`SubmitBiometricUseCase`):

1. Resolve authenticated user (401 if not authenticated — fatal to the request).
2. Fetch the registered name + birth date from findu-core: `GET /findu-core/api/v1/perfil-proveedor/auth/{authUserId}` via a new `CorePerfilPort` (WebClient, same base-url env as the gateway). If the provider profile does not exist yet (404), return `409 CONFLICT` with `"perfil_core_requerido"` — biometric needs the registered personal data (recoverable: the app must finish wizard personal data first).
3. **Name comparison, accent-insensitive:** normalize both the OCR `cedulaNombre` and the core `nombreCompleto` with `java.text.Normalizer.normalize(s, Form.NFD).replaceAll("\\p{M}", "")`, lowercase, collapse internal whitespace, then compare for equality. Mismatch -> validation `RECHAZADA`.
4. **Birth date comparison:** parse the request value with `LocalDate.parse(cedulaFechaNacimiento)` (ISO `yyyy-MM-dd`, no time/zone component) and compare with `.isEqual(...)` against core's `fechaNacimiento` (also a `LocalDate`). Using `LocalDate` on both sides avoids any timezone off-by-one that a `Date`/`Instant` coercion could introduce (Finding 12). Mismatch -> `RECHAZADA`.
5. **Face match:** trust the client-provided `faceMatched` (TFLite result; mock in dev). Backend records `face_match_score`. (Real liveness/match happens on-device; backend does not run TFLite.) If `faceMatched == false` -> `RECHAZADA`.
6. On all-pass: insert `provider_biometric_capture` (resultado `OK`), set `biometria_ok` in `provider_activation_validation`, and **write-once** set the profile frontal photo in security: if security has no stored `foto_frontal_url` for the user, store it (security is the source of truth, §3.4). Then **best-effort** propagate the photo to core via `PUT /findu-core/api/v1/perfil-proveedor/{perfilId}` with `{"nombreCompleto": <unchanged>, "urlImagenPerfil": fotoFrontalUrl}`.

   **`{perfilId}` id source (Finding 9):** the id in the core PUT path is `PerfilProveedorDetalleResponse.id` returned by the step-2 `GET /perfil-proveedor/auth/{authUserId}` call — **not** the security auth user id. We already hold that response from step 2, so we reuse its `id`. Do not call the PUT with `authUserId`.

   **Core PUT failure contract (Finding 9):** the core propagation is **best-effort, non-fatal**. If it fails (5xx/timeout) after security has already set `biometria_ok` and stored `foto_frontal_url`, security remains authoritative: we log at WARN and still return `200 {biometriaOk:true}`. The divergence is reconciled by the same scheduler used for support-dispatch retries (§6.3): a periodic job re-pushes `foto_frontal_url` for users whose core `urlImagenPerfil` is still null. Core's own write-once rule (§3.4 layer 2) guarantees the retry never clobbers a value core already has. The user is never blocked by a core-sync hiccup.

   Return `200` with the updated activation status DTO.
7. Call `ActivationProgressService.recompute(userId)`.

Validation rules for the request body (each field):

| Field | Required | Type | Rule | On failure |
|---|---|---|---|---|
| `cedulaNombre` | yes | String | non-blank, <=200 chars | 400 |
| `cedulaFechaNacimiento` | yes | String | ISO `yyyy-MM-dd`, parseable, not future | 400 |
| `faceMatched` | yes | boolean | — | 400 if null |
| `faceMatchScore` | optional | number | 0.0–1.0 | 400 if out of range |
| `fotoFrontalUrl` | yes | String (URL) | non-blank, http(s) | 400 |

Name/date mismatch and `faceMatched=false` are **business rejections**, not 4xx: the endpoint returns `200` with `{ "biometriaOk": false, "motivo": "nombre_no_coincide" | "fecha_no_coincide" | "rostro_no_coincide" }` and records a `RECHAZADA` validation row (new version). The app shows the reason and lets the user retry. Only malformed input is `400`.

### 5.4 Profile exposure (so mobile paints 'no activo' + alerts)

Extend `UserProfileResponse` (and the mapper in `JwtManagerImpl.getCurrentUserProfile`) with:

```java
String estadoActivacion;        // PENDIENTE_VALIDACIONES | EN_REVISION_SOPORTE | ACTIVADO | RECHAZADO
boolean otpOk;                   // canonical identifier for alert eval
boolean emailOk;                 // canonical; sourced from email_verified / EMAIL validation row
boolean biometriaOk;             // canonical; sourced from BIOMETRIA validation row
boolean soporteOk;               // canonical
String fotoPerfilFrontalUrl;     // immutable frontal photo (may be null)
```

**Canonical identifiers (Finding 4):** the four flags exposed here — `otpOk`, `emailOk`, `biometriaOk`, `soporteOk` — are the *only* names used for alert evaluation. The template `condicion_aparicion` expressions (§4) reference exactly `emailOk`/`biometriaOk`; the mobile evaluator (§8.2) reads exactly these fields. We deliberately do NOT expose a second alias (`emailVerified`/`biometriaVerificada`) in the alert path to avoid the drift the review flagged. The pre-existing `emailVerified`/`phoneVerified` fields may remain on the profile for other consumers, but `emailOk` is the authoritative value the alert expression binds to (populated from the same underlying email-verification state). The app evaluates the two ALERTA conditions off `emailOk` and `biometriaOk`. No separate "pending alerts" endpoint is required for correctness (hybrid model); see §7.4 for the content endpoint.

Dedicated lighter endpoint also added: `GET /api/v1/activation/status` returning the same activation fields (used by the biometric screen and after submit, avoids re-parsing the whole profile).

### 5.5 Support approval / rejection

Support agents (role `ROLE_OUR_SOPORTE`) act from the support tooling. Endpoints on `ProviderActivationController`, authorized to the support role via the existing authorization model (`permite_all=false`; the operation rows must be seeded in `data.sql`/migration and granted to role id 5):

- `POST /api/v1/activation/approve` — operation id 20, granted to role 5 (§3.2.1). **Static path; `userId` travels in the body** `{ "userId": <long> }` (not the path — the authorization resolver matches `operation.path` by exact equality, §3.2.1 Finding 1). Validation: `userId` required, positive long (else `400`). Precondition `estado_activacion == EN_REVISION_SOPORTE` for that `userId` (else `409`). Sets `soporte_ok`, `estado_activacion=ACTIVADO`, inserts a `SOPORTE` validation `OK` row. Dispatches `PROVEEDOR_ACTIVADO` (NOTIFICACION) with `recipient=PROVEEDOR_{userId}` (the prefix the dispatcher routes, §6.2). Returns `200`.
- `POST /api/v1/activation/reject` — operation id 21, granted to role 5. **Static path; body** `{ "userId": <long>, "motivo": "..." }` (`userId` required positive long; `motivo` required non-blank, <=500 chars; else `400`). Precondition `EN_REVISION_SOPORTE` for that `userId` (else `409`). Sets `estado_activacion=RECHAZADO`, inserts `SOPORTE` `RECHAZADA` row with `detalle=motivo`, and **resets `"user".soporte_notified=false`** so a later re-submission can re-open review (§6.3). Dispatches `PROVEEDOR_RECHAZADO` (NOTIFICACION) with `recipient=PROVEEDOR_{userId}`. Returns `200`.

---

## 6. Cross-service event contracts & RabbitMQ routing

All three outbound integrations from security reuse the dispatcher HTTP contract (`DispatchNotificationRequest { type, recipient, templateCode, language, params }` at `POST {FINDU_NOTIFICATION_LAMBDA_URL}/dispatchNotification`). The dispatcher decides queue vs WebSocket. Security never binds to RabbitMQ.

### 6.1 Email confirmation (already wired)
`NotificationPort.send("EMAIL", email, "VERIFICACION_EMAIL", "es", params)` -> dispatcher -> transactional branch -> publish to exchange `findu.notifications` routing key `notification.correo` -> queue `findu.notifications.correo` -> `findu-notification-processor` `handleEmail`. No change; we only ensure it is triggered after OTP (§5.2).

### 6.2 Notifying support when 1–3 complete (transport decision, Finding 1)

The first design routed a `SOPORTE_PROVEEDOR_PARA_REVISION` `NOTIFICACION` with `recipient = SOPORTE_{id}` down the dispatcher's presence branch. The review verified this does NOT work: `NotificationDispatchController.despacharEventoPresencia` only recognizes the `PROVEEDOR_` and `CLIENTE_` recipient prefixes; **any other recipient (`SOPORTE_5`) falls into the `else` branch and is coerced to `CLIENTE_SOPORTE_5`**, with the envelope hard-coded to `EventType.OFFER_RECEIVED` / `TipoEventoCanal.MENSAJE`. There is no support presence channel. So a real-time support notification is not deliverable without a dispatcher change.

**Decision (chosen): drop the real-time support presence notification; the support signal is the help-v2 TICKET only.** Support agents work from the support tooling, which reads tickets (help-v2 is exactly the support queue). A real-time websocket ping to a support agent adds a new dispatcher channel for no functional gain this iteration, and the task's own decision (C) is "create the TICKET in help-v2 … fire the support notification when the first 3 are complete" — the ticket IS that signal to support. We therefore:

- When `ActivationProgressService` sees `otp_ok && email_ok && biometria_ok` for the first time, it creates the support ticket in help-v2 (§6.3) and advances to `EN_REVISION_SOPORTE`. No presence notification to support is sent.
- (Rejected alternative) Extend the dispatcher with a `SOPORTE_` prefix + a support presence channel. This is a real, separately-scoped change to `findu-notification-dispatcher` (new `canalId` branch, a support channel concept, and an envelope type other than the hard-coded `OFFER_RECEIVED`). It is **future work**, documented here but not undertaken this iteration; §2/§11 therefore keep "dispatcher unchanged" and we remove the earlier claim that `SOPORTE_` "routes by recipient channelId".

So the `SOPORTE_PROVEEDOR_PARA_REVISION` **NOTIFICACION template is not needed** and is removed from the seed set (§7). If the dispatcher is extended later, add it then with `routing.condicion=isUserActivo`, `canal_si_activo=WEBSOCKET`, `canal_si_inactivo=PUSH`.

**Provider-facing notifications DO route correctly** and are addressed explicitly with the `PROVEEDOR_` prefix the dispatcher recognizes: on approve/reject (§5.5) security dispatches `PROVEEDOR_ACTIVADO` / `PROVEEDOR_RECHAZADO` with `recipient = PROVEEDOR_{providerUserId}`. These flow down the presence branch and apply the `isUserActivo` rule (WebSocket if online, FCM push fallback if offline / forced push if the websocket micro marks the channel deactivated).

**Client discrimination contract (review Finding 3, MEDIUM) — the wire `EventType` is NOT the signal; `payload.templateCode` is.** Verified in `NotificationDispatchController.despacharEventoPresencia`: the presence envelope's `EventType` is **hard-coded to `OFFER_RECEIVED`**, and `TipoEventoCanal` is `MENSAJE` unless the `tipoEvento` string contains `OFERTA` (→ `OFERTA`) or `SOLICITUD` (→ `EVENTO_SISTEMA`). `PROVEEDOR_ACTIVADO` / `PROVEEDOR_RECHAZADO` contain neither token, so on the wire they arrive as `EventType.OFFER_RECEIVED` + `tipoEvento=MENSAJE` — semantically indistinguishable from a real offer **by type alone**. Delivery itself is correct (online WebSocket, offline FCM, forced push when the channel is marked inactive — confirmed in `EventDispatcherService.procesarEntrega`). What distinguishes an activation/rejection from an offer is the **payload**: the dispatcher copies `request.params()` into the payload and injects `payload.templateCode = request.templateCode()` and `payload.tipoEvento = templateCode` (when absent). **The Flutter client therefore branches on `payload["templateCode"]` (values `PROVEEDOR_ACTIVADO` / `PROVEEDOR_RECHAZADO`), never on `EventType`.** This contract is specified in §8.2.

**Decision: do NOT change the dispatcher this iteration.** The payload already carries the discriminator, so a correct wire-level `EventType`/`tipoEvento` is not required for the app to behave correctly. Adding a dispatcher branch that recognizes the activation codes and emits a distinct `EventType`/`tipoEvento` is a scoped change to `findu-notification-dispatcher` (new `EventType` enum value + a branch in `despacharEventoPresencia`) and is **documented as optional future work**, not undertaken here (§2/§11 keep "dispatcher unchanged"). If a product requirement later demands a correct wire type, that scoped dispatcher branch is the place to add it.

### 6.3 Support TICKET in help-v2 (idempotent)
help-v2 is blocking Spring MVC with **no RabbitMQ**. Adding a consumer would mean adding `spring-boot-starter-amqp`, a connection, and reactive-less listeners — larger blast radius and a new infra dependency on a service that currently only talks HTTP. **Decision: security creates the ticket via help-v2's existing REST endpoint** `POST /findu-help-v2/api/v1/help/tickets` (through the gateway), using a new `SupportTicketPort` (WebClient) in security. This matches how help-v2 is already consumed and needs **zero** new infra. (Rejected alternative: RabbitMQ event + help-v2 consumer — more moving parts, new dependency, no current benefit since there is exactly one producer.)

The `CreateTicketCommand`/`CreateTicketRequest` already carries `userId`, `userRole` (`PROVEEDOR`), `subject`, `description`, optional `categoryId`/`subcategoryId`. We send the provider's `userId`, `userRole=PROVEEDOR`, a fixed subject/description identifying it as an activation review, and (optionally) a dedicated activation help category/subcategory to seed in help-v2's category store.

**Idempotency (no duplicate ticket on retry):** two layers, keyed on the concrete `soporte_notified BOOLEAN` column on `"user"` (§3.2, Finding 8):
- Security guard: `ActivationProgressService.recompute(userId)` creates the ticket only when `otp_ok && email_ok && biometria_ok` AND `soporte_notified == false`. On a help-v2 2xx it sets `soporte_notified=true` and stores `soporte_ticket_code`. Any later `recompute` with `soporte_notified=true` is a no-op for the ticket. On a support **reject** (§5.5) `soporte_notified` is reset to `false` so a re-opened review (RECHAZADO → EN_REVISION_SOPORTE) can re-fire exactly once.
- help-v2 defensive dedup: add an optional `externalRef` (`ACT-{userId}`) to `CreateTicketRequest`/`CreateTicketCommand`/`HelpTicket`, with a unique **sparse** index in MongoDB on `externalRef`. `CreateTicketService` first looks up by `externalRef`; if found, returns the existing ticket instead of creating a new one. This makes create idempotent even if security retries after a timeout where the first call actually succeeded (so the `soporte_notified` flag and the actual ticket cannot diverge).

Error handling for ticket creation (security side):
- help-v2 unreachable / 5xx: **recoverable**. Set the flags and `estado_activacion=EN_REVISION_SOPORTE` (local truth) but leave `soporte_notified=false` and log at ERROR; a scheduled retry (reuse the existing `infrastructure/scheduler` package pattern) re-attempts the ticket for users in `EN_REVISION_SOPORTE` with `soporte_notified=false`. The same scheduler re-pushes a pending core photo sync (§5.3 step 6). The caller (the validation that completed) still gets `200`.
- help-v2 returns 2xx: store the returned `ticketCode` in `soporte_ticket_code`, set `soporte_notified=true`.
- Duplicate (help-v2 returns existing via `externalRef`): treated as success (store its code, set `soporte_notified=true`).

### 6.4 Routing summary table

| Event | Producer | Transport | Template (tipo) | Recipient / Queue / channel |
|---|---|---|---|---|
| Confirmation email | security | dispatcher -> Rabbit | `VERIFICACION_EMAIL` (EMAIL) | email addr; queue `findu.notifications.correo` |
| Support "provider ready for review" | security | HTTP REST (ticket only) | n/a (ticket, not a template) | `POST /findu-help-v2/api/v1/help/tickets` — see §6.2 (no presence notification) |
| Provider activated | security | dispatcher -> presence | `PROVEEDOR_ACTIVADO` (NOTIFICACION) | `recipient=PROVEEDOR_{providerUserId}`; WebSocket -> push fallback |
| Provider rejected | security | dispatcher -> presence | `PROVEEDOR_RECHAZADO` (NOTIFICACION) | `recipient=PROVEEDOR_{providerUserId}`; WebSocket -> push fallback |
| Email-pending alert | n/a (content) | template store (read) | `PERFIL_CORREO_PENDIENTE` (ALERTA) | rendered client-side on `perfil_proveedor` |
| Biometric-pending alert | n/a (content) | template store (read) | `PERFIL_BIOMETRIA_PENDIENTE` (ALERTA) | rendered client-side on `perfil_proveedor` |

---

## 7. Templates to seed (DynamoDB `notification_templates`)

Seed via the existing versioned schema: for each, write `v#1` (immutable) + `v#ACTIVE` pointer, attribute names exactly per `NotificationTemplateItem.java`. Delivery mechanism: a seed script / `TemplateAdminController` CRUD calls (preferred — uses the real create path and `TransactWriteItems` versioning) run against the qa table, OR a documented JSON put set. We will add a seed under notification-processor (e.g. `scripts/seed-activation-templates.*` or a `data`/bootstrap runner guarded to run once) that calls the admin create use case.

**Legacy codes to seed ("plantillas que usamos actualmente", Finding 10).** The seed set must include every template code currently dispatched, so the engine resolves them in qa. Enumerated exactly:

- Security (`com.findu.security.util.NotificationTemplates`, all 5): `CODIGO_VERIFICACION`, `RECUPERAR_PASSWORD`, `PASSWORD_CAMBIADA`, `VERIFICACION_OTP`, `VERIFICACION_EMAIL`. Tipos: `VERIFICACION_OTP` is an `SMS` template; `VERIFICACION_EMAIL`, `RECUPERAR_PASSWORD`, `PASSWORD_CAMBIADA`, `CODIGO_VERIFICACION` are `EMAIL` (confirm each against how the security use cases call `NotificationPort.send(type, ...)` when authoring the seed values).
- Core (`com.findu.core.domain.enums.NotificationTemplates`, all 9): `BIENVENIDA_CLIENTE`, `BIENVENIDA_PROVEEDOR`, `SOLICITUD_CREADA`, `OFERTA_RECIBIDA`, `OFERTA_ACEPTADA`, `SOLICITUD_CANCELADA`, `SOLICITUD_ACTUALIZADA`, `CALIFICACION_RECIBIDA`, `SERVICIO_PROGRAMADO`. Tipo per how core dispatches each (NOTIFICACION/presence for most provider/customer events — set to the type the dispatcher expects).

For each legacy code: seed only if the `v#ACTIVE` item is missing (idempotent — do not clobber an admin-edited active version). The exact body/subject text is migrated from the current hard-coded/legacy template source when authoring the seed.

New templates (4: 2 NOTIFICACION + 2 ALERTA; the previously-planned `SOPORTE_PROVEEDOR_PARA_REVISION` is **removed** per §6.2 Finding 1):

1. `PROVEEDOR_ACTIVADO` — `tipo=NOTIFICACION`, `language=es`, top-level `title="Tu cuenta fue activada"`, `body_template` provider-facing "tu cuenta fue activada", `routing={condicion:isUserActivo, canal_si_activo:WEBSOCKET, canal_si_inactivo:PUSH}`, `variables=[provider_nombre]`.
2. `PROVEEDOR_RECHAZADO` — `tipo=NOTIFICACION`, `language=es`, top-level `title="No pudimos activar tu cuenta"`, `body_template` uses `{{motivo}}`, same `routing`, `variables=[provider_nombre, motivo]`.
3. `PERFIL_CORREO_PENDIENTE` — `tipo=ALERTA`, authored against `alert_meta` exactly as in §4 (titulo="Confirma tu correo", `condicion_aparicion="emailOk == false"`, `tiempo_desaparicion=0`, no CTA).
4. `PERFIL_BIOMETRIA_PENDIENTE` — `tipo=ALERTA`, authored against `alert_meta` exactly as in §4 (titulo="Completa tu validación biométrica", `cta_label="Validar ahora"`, `cta_action="findu://perfil/verificacion-biometrica"`, `condicion_aparicion="biometriaOk == false"`, `tiempo_desaparicion=0`).

New `NotificationTemplates` constants added in security for the codes it dispatches: `PROVEEDOR_ACTIVADO`, `PROVEEDOR_RECHAZADO` (the two provider-facing notifications). `SOPORTE_PROVEEDOR_PARA_REVISION` is NOT added (no longer used).

### 7.4 Alert content delivery (chosen variant)

The two ALERTA templates must reach the app for display. Two viable options:
- (A) App hardcodes the alert copy and only toggles visibility off the profile flags.
- (B) App fetches alert content for a screen from the template engine, toggles visibility off the flags locally.

**Choose (B)** so alert copy stays admin-editable without an app release.

**Implementation for this iteration (chosen): by-code `GetItem(v#ACTIVE)`, no new GSI (Finding 6).** The review confirmed `DynamoDbTemplateAdapter` wires only `GSI_tipo` (`findu.notification.templates.gsi-tipo:GSI_tipo`, used by `findByTipo`); there is **no `GSI_pantalla`**. We therefore do NOT query by `pantalla_destino`. Instead the read endpoint resolves the two known alert codes for this screen directly via the existing hot-path `findActive(codigo, ALERTA, language)` (a single `GetItem` on `v#ACTIVE` each) — the server knows the perfil_proveedor alert codes (`PERFIL_CORREO_PENDIENTE`, `PERFIL_BIOMETRIA_PENDIENTE`).

Endpoint: `GET /api/v1/templates/alertas?pantalla=perfil_proveedor&language=es` returns the active `ALERTA` templates for that screen, projecting the `alert_meta` fields the app needs (`categoria`, `titulo`, `body`, `cta_label`, `cta_action`, `prioridad`, `condicion_aparicion`, `tiempo_desaparicion`). For `pantalla=perfil_proveedor` the handler looks up the two known codes; a screen→codes map (small, in-config) lets more screens be added without a GSI. The app evaluates `condicion_aparicion` against the live profile and renders the matches sorted by `prioridad`.

**Future work (not this iteration):** provisioning a real `GSI_pantalla` (table-definition change) to query alerts by `pantalla_destino` generically. Until then the by-code lookup is the specified implementation and needs no infra change. We remove the earlier claim that `GSI_pantalla` already exists / matches the engine design.

---

## 8. Mobile (`findu_proveedores`)

### 8.1 BiometricVerifier abstraction + mock
The current `BiometricVerificationService` is a concrete mock that returns a **hardcoded Unsplash URL** — this violates the project's no-fake-data rule and the task's abstraction requirement. Replace with an interface + swappable impls:

```
features/auth_onboarding/domain/ports/biometric_verifier.dart   // abstract BiometricVerifier
features/auth_onboarding/infrastructure/adapters/mock_biometric_verifier.dart
features/auth_onboarding/infrastructure/adapters/mlkit_tflite_biometric_verifier.dart  // real, dev-stubbed
```

`BiometricVerifier` contract:

```dart
abstract class BiometricVerifier {
  Future<CedulaOcrResult> readCedula(String frontImagePath);          // name, birthDate
  Future<FaceCaptureResult> captureAndDetectFace();                   // frontal photo path + faceDetected/liveness
  Future<FaceMatchResult> matchFaces(String cedulaFacePath, String liveFacePath); // matched + score
}
```

- `MockBiometricVerifier` (default in dev, chosen by an env/build flag `BIOMETRIC_MODE=mock`): returns **configurable** success/failure (not a stub URL). The captured "frontal photo" is the actual image path the user picked/captured on-device, never a remote placeholder. OCR name/birthdate are configurable test values so the whole flow (states, events, alerts, ticket) runs without models.
- `MlkitTfliteBiometricVerifier` (real, left dev-stubbed): documented wiring below. Selected when `BIOMETRIC_MODE=real`.

Real impl wiring (documented, not activated this iteration):
- `pubspec.yaml` deps: `google_mlkit_text_recognition`, `google_mlkit_face_detection`, `tflite_flutter` (+ a face-embedding `.tflite` asset, e.g. MobileFaceNet). These pull native ML Kit; **arm64 physical device only** (ML Kit + TFLite do not run on x86 emulators / are large). Document `--target-platform android-arm64` and that CI/emulator uses the mock.
- Flow: capture frontal cedula image -> `google_mlkit_text_recognition` OCR -> parse name + birth date -> **normalize accents** (Dart: `removeDiacritics` / manual NFD strip) before sending to backend -> capture live selfie -> `google_mlkit_face_detection` ensures a single live face (liveness heuristics: blink/head-turn) -> extract face crops from cedula and selfie -> `tflite_flutter` runs the embedding model on both -> cosine distance -> `matched = distance < threshold`, `score = 1 - distance`. The app posts `{cedulaNombre, cedulaFechaNacimiento, faceMatched, faceMatchScore, fotoFrontalUrl}` to `POST /security-auth/api/v1/activation/biometric` (backend re-checks name/date, §5.3).

The decision of which impl to use is injected at composition root (DI), so swapping mock↔real is one binding change.

### 8.2 Profile screen: 'usuario no activo' + alerts
`provider_profile_screen.dart` reads `estadoActivacion`/`emailOk`/`biometriaOk` from the security profile (`profile_api_adapter.dart` already hits `/security-auth/api/v1/profile`). It:
- Shows a status badge "Usuario no activo" when `estadoActivacion != ACTIVADO`.
- **Realtime activation/rejection handling (review Finding 3):** the realtime notification handler branches on the incoming message's `payload["templateCode"]`, NOT on its `EventType` (which the dispatcher hard-codes to `OFFER_RECEIVED` for every presence event, §6.2). On `templateCode == "PROVEEDOR_ACTIVADO"` the app refreshes the profile (→ `estadoActivacion=ACTIVADO`, badge clears); on `templateCode == "PROVEEDOR_RECHAZADO"` it surfaces the rejection with `payload["motivo"]`. An `OFFER_RECEIVED` whose `templateCode` is neither of these is handled by the existing offer flow unchanged. The existing socket/FCM message model already exposes the `payload` map, so this is a dispatch-key branch, not a new transport.
- Fetches alert content via §7.4 and renders the alerts whose `condicion_aparicion` matches (`emailOk == false`, `biometriaOk == false`), evaluated against the live profile flags. Each alert disappears as soon as its flag flips (reactive on the profile state); `tiempo_desaparicion=0` means no timer (§4).
- The biometric alert's CTA (`findu://perfil/verificacion-biometrica`) navigates to the biometric screen. **Deep-link route registration (Finding 14):** this CTA only works if the app registers the `findu` URL scheme and a router route for `perfil/verificacion-biometrica`. The mobile work must (a) declare the `findu` scheme in the platform manifests (Android `intent-filter` with `android:scheme="findu"`, iOS `CFBundleURLSchemes`), and (b) add a matching route in the app router (GoRouter/AutoRoute per the app's existing router) that resolves `perfil/verificacion-biometrica` to the biometric screen. If the app's convention is in-app route names rather than URL deep links, the alert `cta_action` must instead carry that route name; whichever the app uses, the stored `cta_action` string MUST match a registered route or the CTA is a no-op. Confirm the existing router convention when implementing and align the seeded `cta_action` to it.
- The alert expression evaluator supports exactly the identifier set `{emailOk, biometriaOk, otpOk, soporteOk}` (booleans) with `==`/`!=` vs `true`/`false` (§4).
- The frontal profile photo widget is **read-only**: no edit affordance when `fotoPerfilFrontalUrl` is set (enforced in UI; backend is authoritative).

### 8.3 Register wizard screen 2 (biometric optional / deferrable)
`register_wizard_screen.dart` already captures cedula + biometric optionally. We route its biometric step through `BiometricVerifier` and, on success, call the new activation submit endpoint. If skipped, the provider proceeds; the profile alerts then prompt them later. No fake fallback catalogs introduced.

### 8.4 Mobile hexagonal placement
- `features/auth_onboarding/domain/ports/biometric_verifier.dart` + result value objects.
- `.../infrastructure/adapters/{mock,mlkit_tflite}_biometric_verifier.dart`.
- Activation submit added to `auth_api_adapter.dart` (or a small `activation_api_adapter.dart`) using the shared `RestHttpClient` to `security-auth`.
- Alert content fetch added to a `notification_api_adapter.dart` hitting the processor via the gateway.

---

## 9. Edge cases

- **Email confirmed before biometric, or biometric before email:** both orders are allowed; support ticket/notification fire only when all of OTP+email+biometric are true. `recompute` is idempotent on repeated calls.
- **Biometric re-submission after a `RECHAZADA`:** inserts a new `version` row, marks it `is_current`; a subsequent all-pass can move `RECHAZADO` back to `EN_REVISION_SOPORTE`. The ticket re-fires only because support rejection reset the concrete `"user".soporte_notified` column to `false` (§3.2/§6.3); `recompute` then creates a fresh ticket exactly once (help-v2 dedup on `externalRef=ACT-{userId}` means the re-opened case reuses/updates the same external-keyed ticket rather than spawning duplicates).
- **Accent / casing / whitespace in names:** NFD strip + lowercase + whitespace collapse on both sides. "José Peña" == "Jose Pena".
- **Future or unparseable birth date:** `400` (malformed) vs mismatch `200 {biometriaOk:false}`.
- **Core profile missing at biometric time:** `409 perfil_core_requerido`; app finishes personal data first.
- **Immutable photo:** second biometric pass does not overwrite an existing `foto_frontal_url` in security or `urlImagenPerfil` in core.
- **Support acts on wrong state:** approve/reject require `EN_REVISION_SOPORTE` else `409`.
- **Dispatcher/help-v2 down:** the email send and the provider-facing approve/reject notifications are composed non-fatally (logged on error, §5.2/§6.1); support-ticket creation is retried by the scheduler; the core photo sync is retried by the same scheduler; validations still persist. No user-facing failure for a downstream outage. (Support is signalled only by the ticket — there is no support presence notification, §6.2.)
- **Duplicate dispatch on retry:** `externalRef=ACT-{userId}` dedup in help-v2 + the `"user".soporte_notified` guard in security.
- **Railway qa startup:** migration is additive + idempotent, applied manually; `spring.sql.init.mode=never` unchanged, so no accidental DROP on boot.

---

## 10. Testing

Unit-testable (no infra):
- Security: name-normalization + date comparison (pure function), `ActivationProgressService.recompute` state transitions and the "fire once" guard, biometric submit business-rejection vs 400 branching, state-machine guards for approve/reject. Mock `CorePerfilPort`, `SupportTicketPort`, `NotificationPort`, repositories.
- help-v2: `CreateTicketService` idempotency via `externalRef` (existing-ticket lookup returns existing). Pattern already exists (`CreateTicketServiceTest`).
- Mobile: `MockBiometricVerifier` configurable outcomes; profile alert visibility logic given profile flags; immutable-photo UI guard. Pure Dart unit tests, no device.

Integration-testable:
- Security R2DBC repositories against Testcontainers Postgres (project already depends on `org.testcontainers:postgresql`): the additive migration applies cleanly on top of the existing schema; validation rows + `estado_activacion` persist.
- End-to-end template resolution for the new `ALERTA`/`NOTIFICACION` codes against a local DynamoDB (the processor already supports a dynamodb endpoint override).
- Dispatcher bifurcation (EMAIL -> queue; NOTIFICACION -> presence) is covered by the dispatcher's own tests; we add a contract test that security's dispatch payload matches `DispatchNotificationRequest`.

Hard-to-test (documented, mocked this iteration): real ML Kit OCR + TFLite face match (device-only, arm64) — covered by the mock in dev; the real adapter is behind the interface and exercised manually on a device.

---

## 11. Files to add/modify (concrete)

**security (`SECURITY/findu-spring-security`)**
- `resources/schema.sql` (add idempotent activation DDL block: `estado_activacion`, `soporte_notified`, `soporte_ticket_code` on `"user"`, tables `provider_activation_validation` + `provider_biometric_capture`), new `scripts/migrations/V2__provider_activation.sql` (additive, applied to Railway qa), `resources/data.sql` (append operations 18–21 + grants: role 4 → 18,19; role 5 → 20,21, per §3.2.1).
- `domain/model/`: `ProviderActivation`, enums `ProviderActivationState`, `ValidationType`, `ValidationState`.
- `infrastructure/entity/`: `ProviderActivationValidationEntity`, `ProviderBiometricCaptureEntity`; add `estadoActivacion` to `UserEntity` + `Usuario`.
- `infrastructure/repository/`: `ProviderActivationValidationRepository`, `ProviderBiometricCaptureRepository`.
- `application/port/output/persistence/ProviderActivationRepositoryPort` + persistence adapter.
- `application/port/output/externalapi/`: `CorePerfilPort`, `SupportTicketPort` + WebClient adapters under `infrastructure/adapter/...`.
- `application/usecase(.impl)/`: `SubmitBiometricUseCase`, `GetActivationStatusUseCase`, `ApproveProviderUseCase`, `RejectProviderUseCase`, `ActivationProgressService`.
- Modify `VerifyPhoneOtpUseCaseImpl` (trigger email after OTP + mark `otp_ok`), `VerifyEmailUseCaseImpl` (mark `email_ok` + `recompute`).
- `presentation/controller/ProviderActivationController`.
- DTOs: `BiometricSubmitRequest`, `ActivationStatusResponse`, `ApproveProviderRequest` (`{ userId }`), `RejectProviderRequest` (`{ userId, motivo }`); extend `UserProfileResponse` + mapper in `JwtManagerImpl`. Approve/reject carry `userId` in the body (static endpoint paths, §3.2.1 Finding 1).
- `util/NotificationTemplates` (+2 constants: `PROVEEDOR_ACTIVADO`, `PROVEEDOR_RECHAZADO`); a `TextNormalizer` util.
- `infrastructure/scheduler/` retry for pending support dispatch.
- Config: new properties for core base-url + help-v2 base-url (reuse gateway env where possible).

**core (`CORE/findu-core`)**
- `GestionPerfilProveedorUseCaseImpl.actualizarPerfil(Long, ActualizarPerfilProveedorRequest)` (NOT `PerfilProveedorService.actualizar`, which does not exist — review Finding 2): enforce write-once on `urlImagenPerfil` — if `perfil.getUrlImagenPerfil()` is already non-null, keep it and ignore the incoming `urlImagenPerfil` (still allow `nombreCompleto` edits). Concretely replace the current unconditional `if (request.urlImagenPerfil() != null) perfil.setUrlImagenPerfil(...)` with `if (request.urlImagenPerfil() != null && perfil.getUrlImagenPerfil() == null) perfil.setUrlImagenPerfil(request.urlImagenPerfil());` (§3.4 layer 2). Closes the overwrite path on the frontal photo. No DTO/signature change required.

**help-v2 (`CORE/findu-help-v2`)**
- Add `externalRef` to `CreateTicketRequest`, `CreateTicketCommand`, `HelpTicket` (+ Mongo document + unique **sparse** index on `externalRef`), lookup-by-`externalRef` in `CreateTicketService` (return existing on hit). Optionally seed an "Activación de proveedor" help category.

**notification-processor (`CORE/findu-notification-processor`)**
- Seed script/use-case call for the templates: 3 new (`PROVEEDOR_ACTIVADO`, `PROVEEDOR_RECHAZADO` NOTIFICACION; `PERFIL_CORREO_PENDIENTE`, `PERFIL_BIOMETRIA_PENDIENTE` ALERTA — count: 2 NOTIFICACION + 2 ALERTA = 4 new) **plus** ensuring the legacy codes exist (5 security + 9 core, enumerated in §7). ALERTA templates authored against `alert_meta`/`titulo` (§4). `GET /api/v1/templates/alertas` read endpoint using the by-code `findActive(..., ALERTA, ...)` lookup (no new GSI, §7.4).

**dispatcher (`CORE/LAMBDA/findu-notification-dispatcher`)**: **no change this iteration.** Verified: `despacharEventoPresencia` recognizes only `PROVEEDOR_`/`CLIENTE_` recipients; the provider-facing `PROVEEDOR_{id}` notifications route correctly, so no edit is needed. Support is notified via the help-v2 ticket only (§6.2). Adding a `SOPORTE_` presence channel is documented as future dispatcher work, out of scope here.

**mobile (`findu-movil/apps/findu_proveedores`)**
- `biometric_verifier.dart` (port) + `mock_` / `mlkit_tflite_` adapters; remove hardcoded URL from the old service.
- activation submit in `auth_api_adapter.dart` (or new adapter); alert fetch adapter.
- `provider_profile_screen.dart` (status badge + alerts), `register_wizard_screen.dart` (route biometric via the port), DI composition root binding.
- `pubspec.yaml` deps + doc comment for the real impl (arm64-only), `environment.dart` `BIOMETRIC_MODE` flag.

---

## 12. Assumptions (flagged for the design review)

1. `rol_soporte` == `ROLE_OUR_SOPORTE` (seeded by `add_soporte.sql`, id 5). Support is reached via the help-v2 ticket (no presence notification to a support channel — §6.2); the ticket carries `userRole=PROVEEDOR` + `externalRef`.
2. The registered Full Name + birth date for biometric comparison come from findu-core's `perfil-proveedor` (security has no name/birthdate columns). If product wants these in security, add columns to `"user"` instead — larger change, not assumed here.
3. help-v2 ticket creation stays HTTP/REST (no new RabbitMQ in help-v2). Chosen for minimal blast radius; reversible to an event-driven approach later.
4. INFO/profile alert visibility is evaluated client-side (hybrid model) off the canonical flags `emailOk`/`biometriaOk`; content is admin-editable via the template engine (ALERTA fields under `alert_meta`/`titulo`). The provider-facing activated/rejected messages are `NOTIFICACION` (real-time, `recipient=PROVEEDOR_{id}`), not alerts.
5. Schema change is applied as a manual additive migration against Railway qa (project's existing operational model; no Flyway). `spring.sql.init.mode=never` on qa is preserved.
6. The frontal biometric photo is uploaded to storage elsewhere and the app sends a URL; security/core store the URL (write-once). If raw-bytes upload-through-security is required instead, that adds an upload endpoint + storage adapter — not assumed.

---

## 13. Responses to the design review (pass 1 → pass 2)

Verdict addressed: CHANGES_REQUESTED (3 HIGH, 6 MEDIUM, 5 NIT). Every HIGH and MEDIUM is resolved in-design; all NITs addressed. Source re-verified for each HIGH before changing the design.

**HIGH**

1. **Support `SOPORTE_{id}` won't route through the dispatcher — ADDRESSED.** Verified in `NotificationDispatchController.despacharEventoPresencia`: only `PROVEEDOR_`/`CLIENTE_` prefixes are honored; anything else → `CLIENTE_<x>`, envelope hard-coded to `OFFER_RECEIVED`. Decision: **drop the real-time support presence notification; the help-v2 ticket is the sole support signal** (§6.2). Provider-facing notifications are now addressed explicitly as `recipient=PROVEEDOR_{userId}` (§5.5, §6.4). The `SOPORTE_PROVEEDOR_PARA_REVISION` template is removed from the seed set (§7) and from the security constants (§11). Extending the dispatcher with a `SOPORTE_` channel is documented as future work; dispatcher stays unchanged (§2, §11).

2. **ALERTA fields mapped at wrong level — ADDRESSED.** Verified against `NotificationTemplateItemMapper.putAlertMeta/toAlertMeta` + `NotificationTemplateItem` constants: for `tipo=ALERTA` everything nests under `alert_meta`, title is `titulo`. §4 now authors both ALERTA templates against `alert_meta` with `titulo`, keeping only `body_template` + common keys at top level; §7 items 3–4 reference the §4 authoring. (Pass-2 refinement — Finding 4: the top-level `title` is written by the mapper for any tipo, not NOTIFICACION-only; it is simply ignored by the alert consumer. The authoring rule is unchanged: put the ALERTA title in `alert_meta.titulo`, leave the top-level `title` unset. See §14 Finding 4.)

3. **"Core already excludes the frontal photo" false — ADDRESSED.** Verified `ActualizarPerfilProveedorRequest(nombreCompleto, urlImagenPerfil)` — `urlImagenPerfil` is editable. The false claim is removed. §3.4 now states three-layer enforcement and adds a **scoped core change** (write-once on `urlImagenPerfil`), listed in §11 under a new core section.

**MEDIUM**

4. **Condition identifiers vs profile DTO — ADDRESSED.** Canonicalized to `emailOk`/`biometriaOk` across the template `condicion_aparicion` (§4), the profile DTO (§5.4), and the mobile evaluator (§8.2). `emailVerified`/`biometriaVerificada` dropped from alert expressions. Supported identifier set documented: `{emailOk, biometriaOk, otpOk, soporteOk}`.

5. **Operations + grants under-specified — ADDRESSED.** New §3.2.1 enumerates operation rows 18–21 (path/name/method/`module_id=3`/`permite_all=FALSE`) and the exact `rol_operation` grants: biometric+status → role 4 (provider), approve+reject → role 5 (support). Idempotent `ON CONFLICT DO NOTHING` for the Railway migration (with `OVERRIDING SYSTEM VALUE` + sequence `setval` realignment), plain INSERT in the fresh-DB `data.sql` matching the file's style.

6. **`GSI_pantalla` does not exist — ADDRESSED.** Verified `DynamoDbTemplateAdapter` wires only `GSI_tipo`. §7.4 now specifies the **by-code `findActive(code, ALERTA, lang)` GetItem** implementation (no new GSI); `GSI_pantalla` is marked future work and the "already matches the engine design" claim is removed.

7. **`tiempo_desaparicion` semantics unspecified — ADDRESSED.** §4 defines it as **seconds**, `0` = no auto-dismiss (visibility governed solely by `condicion_aparicion`); positive values start a client timer. Both INFO alerts use `0`.

8. **`soporte_notified` storage undefined — ADDRESSED.** §3.2 adds concrete columns `soporte_notified BOOLEAN` and `soporte_ticket_code VARCHAR(60)` on `"user"`. §6.3 defines set-true (help-v2 2xx) and reset-false (on reject); §5.5 reject resets it; §9 re-open path references the concrete column.

9. **Biometric endpoint error contract — ADDRESSED.** §5.3 step 6 now specifies: core photo PUT is **best-effort/non-fatal** (log+continue, security authoritative, reconciled by the scheduler), response stays `200 {biometriaOk:true}`; and `{perfilId}` is `PerfilProveedorDetalleResponse.id` from the step-2 detalle (not `authUserId`).

**NIT**

10. **Legacy templates to seed — ADDRESSED.** §7 enumerates all 5 security constants (`CODIGO_VERIFICACION`, `RECUPERAR_PASSWORD`, `PASSWORD_CAMBIADA`, `VERIFICACION_OTP`, `VERIFICACION_EMAIL`) and all 9 core constants, each seeded only if its `v#ACTIVE` is missing.

11. **Bare `.subscribe()` email trigger — ADDRESSED.** §5.2 composes the email into the OTP chain with `.then(sendVerificationForUser(saved).onErrorResume(...))` (context-preserving, testable with `StepVerifier`), non-fatal on error.

12. **Birth-date comparison type — ADDRESSED.** §5.3 step 4 pins `LocalDate.parse(cedulaFechaNacimiento)` compared with `.isEqual(...)` to core's `LocalDate` (no zone coercion).

13. **`face_match_score` range — ADDRESSED.** §3.2 adds `CHECK (face_match_score IS NULL OR face_match_score BETWEEN 0 AND 1)`.

14. **Deep-link scheme/route not confirmed — ADDRESSED.** §8.2 notes the required `findu` URL-scheme declaration (Android `intent-filter`, iOS `CFBundleURLSchemes`) and the matching router route for `perfil/verificacion-biometrica`; if the app uses in-app route names instead, `cta_action` must carry that name and match a registered route.

---

## 14. Responses to the design review (pass 2 → pass 3)

Verdict addressed: CHANGES_REQUESTED (1 HIGH, 2 MEDIUM, 3 NIT). Every finding re-verified against the exact source in the worktree before changing the design.

**HIGH**

1. **Templated approve/reject paths can't match the exact-equality authorization lookup — ADDRESSED.** Verified `FinduReactiveAuthorizationManager.resolveOperation` → `OperationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(modulePathBase, apiPath, httpMethod)`, with `o.path = :path` exact equality in the SQL; a `{userId}` path can never match a concrete request path, so `check()` → `switchIfEmpty` → `AuthorizationDecision(false)` denies the endpoint for everyone. **Fix (chosen):** made approve/reject **static paths with `userId` in the body** — `POST /api/v1/activation/approve` `{userId}` and `POST /api/v1/activation/reject` `{userId, motivo}`; seeded `operation.path` = exact literals `/api/v1/activation/approve` and `/api/v1/activation/reject` (ids 20/21). This matches the existing static-path convention (ids 6/8/9) and needs no change to the shared authorization manager. Rejected the path-template-matching alternative (larger blast radius on shared security code). Updated §3.2.1 (op table + migration INSERTs + removed the "confirm path-template matching" note), §5.5 (static endpoints, `userId` in body + validation), §11 (new `ApproveProviderRequest`/`RejectProviderRequest` DTOs; dispatcher/auth-manager stay unchanged).

**MEDIUM**

2. **Core write-once targeted a non-existent method — ADDRESSED.** Verified in the worktree: `PerfilProveedorService` has no `actualizar`; the editable path is `GestionPerfilProveedorUseCaseImpl.actualizarPerfil(Long, ActualizarPerfilProveedorRequest)` (via `PerfilProveedorController PUT /{id}`), whose body today is `if (request.urlImagenPerfil() != null) perfil.setUrlImagenPerfil(request.urlImagenPerfil());`. Retargeted §3.4 layer 2 and §11 core section to that use case, with the exact write-once edit `if (request.urlImagenPerfil() != null && perfil.getUrlImagenPerfil() == null) perfil.setUrlImagenPerfil(request.urlImagenPerfil());` (still applies `nombreCompleto` edits). Getter confirmed as `PerfilProveedor.getUrlImagenPerfil()`.

3. **Provider notifications ride the presence branch but the dispatcher hard-codes `OFFER_RECEIVED`; client discrimination unspecified — ADDRESSED.** Verified `NotificationDispatchController.despacharEventoPresencia` builds every presence envelope with `EventType.OFFER_RECEIVED` and `TipoEventoCanal.MENSAJE` (switched to OFERTA/EVENTO_SISTEMA only on `OFERTA`/`SOLICITUD` substrings), and copies `templateCode`/`tipoEvento` into the payload. §6.2 now states the explicit client contract: the Flutter app branches on `payload["templateCode"]` (`PROVEEDOR_ACTIVADO`/`PROVEEDOR_RECHAZADO`), **not** on `EventType`; §8.2 adds the concrete realtime-handler branch (refresh profile on activated, show `motivo` on rejected). Documented that `EventType` stays hard-coded to `OFFER_RECEIVED` and that emitting a distinct wire type is optional future dispatcher work; §2/§11 keep "dispatcher unchanged."

**NIT**

4. **Mapper doesn't restrict top-level `title` to NOTIFICACION — ADDRESSED.** Verified `NotificationTemplateItemMapper.toItem` writes `title` for any tipo with a non-null `getTitle()`. §4 reworded: for ALERTA author the title under `alert_meta.titulo`; the top-level `title` is read into `NotificationTemplate.title` (any tipo) and is ignored by the alert consumer, so do not set it on an ALERTA. The §13 Finding 2 (pass-1) note was corrected to drop the false "NOTIFICACION-only" claim.

5. **§5.2 snippet referenced identifiers not in the current OTP use case — ADDRESSED.** §5.2 snippet is now marked pseudocode-aligned to the real `VerifyPhoneOtpUseCaseImpl`: it uses `usuarioService.save(user)`, `phoneOtpStore.delete(e164)`, and returns `new PhoneVerificationResponse(saved.isPhoneVerified(), saved.getAccountStatus())`, with `markOtpOk(saved)` and `sendEmailVerificationUseCase.sendVerificationForUser(saved).onErrorResume(...)` composed into that existing chain before the final `thenReturn`.

6. **Seeded soporte user also holds ROLE_ADMINISTRATOR (role 3) — ADDRESSED.** Verified `add_soporte.sql` grants the soporte user both role 5 and role 3 (via `user_rol`) and grants role 5 ops 1–12 only. §3.2.1 now notes this and states that ops 20/21 are granted to **role 5 exclusively** (never role 3), so approval authority stays role-5-scoped for the seed user; a caution not to grant 20/21 to `ROLE_ADMINISTRATOR` is included.
