# Provider activation across security, core, help-v2, notification-processor, and the Flutter app

The change makes a provider `ACTIVADO` only after four ordered validations (OTP → email → biometric → support approval), without ever blocking login. Security owns the activation aggregate (new `estado_activacion` + two child tables), re-validates the biometric name/birthdate against findu-core, stores the frontal photo write-once, fires a single idempotent support ticket into help-v2 when the first three validations pass, and dispatches provider-facing `PROVEEDOR_ACTIVADO`/`PROVEEDOR_RECHAZADO` notifications through the existing dispatcher. The notification-processor seeds the four new templates (2 NOTIFICACION + 2 ALERTA) plus the missing legacy codes and exposes a by-code alert read endpoint. The Flutter app replaces the hardcoded-URL biometric mock with a `BiometricVerifier` port, renders a "no activo" badge plus the two INFO alerts, keeps the frontal photo read-only, and classifies realtime events by `payload['templateCode']`.

Watch for: (1) the core write-once edit and the security R2DBC repositories were never exercised by an integration/context test — both suites' only failures are the pre-existing H2 fixture (core) and the Docker/Testcontainers environment (security), so the activation DDL and the write-once branch are covered by unit tests only (confirmed). (2) The mobile alert endpoint is unreachable in qa because the API gateway has no route to notification-processor, so alerts silently degrade to empty until that route is provisioned (confirmed, documented as backend-side follow-up). Neither is a code defect against this task's scope.

**Verdict**: APPROVED

<details>
<summary>Issues (2)</summary>

1. **Core write-once + security DDL unverified by integration test** — the only build failures are the pre-existing core H2 fixture mismatch (`perfil_proveedor.estado_verificacion` absent from `schema.sql`) and the security Docker/Testcontainers unavailability; both predate this task and block the context-load tests, so the write-once branch and the additive migration are proven by unit tests only. No action required for this task; apply `V2__provider_activation.sql` to Railway qa manually and run the R2DBC/Testcontainers suite on a Docker-capable host before relying on it in CI.
2. **Alert endpoint not routed through the gateway in qa** — `GET /api/v1/templates/alertas` is not reachable because the API gateway has no notification-processor route; the mobile adapter degrades to an empty alert list (no hardcoded copy). Provision the gateway route + confirm the processor endpoint is deployed so the two INFO alerts actually render in qa. Out of this task's no-deploy scope.

</details>

<details>
<summary>Details</summary>

## Additive idempotent schema that preserves Railway qa

The activation DDL is appended to `schema.sql` as a trailing block of `ALTER TABLE "user" ADD COLUMN IF NOT EXISTS` and `CREATE TABLE IF NOT EXISTS` statements, after the existing DROP/recreate section and touching none of it — so a fresh local bootstrap gets the new objects and the existing DROP ordering is unchanged. The standalone `scripts/migrations/V2__provider_activation.sql` carries the same additive DDL plus the operation rows, and it is the script applied by hand to Railway qa where `spring.sql.init.mode=never` keeps Spring from running anything at boot (confirmed: `application.yml` init-mode untouched). No new DROP or TRUNCATE of an existing table appears anywhere in the diff (confirmed).

The migration inserts operations 18–21 by explicit id with `OVERRIDING SYSTEM VALUE ... ON CONFLICT (id) DO NOTHING`, then realigns the identity sequence with `setval(pg_get_serial_sequence('operation','id'), MAX(id))` so the next auto id does not collide — necessary because `data.sql` relies on the identity sequence. The soporte role/user re-seed and all grants use `ON CONFLICT DO NOTHING`, so the whole script is re-runnable.

The fixer's deviation on `data.sql` is sound and the right call: role 5 (`ROLE_OUR_SOPORTE`) is seeded only in `add_soporte.sql`, so putting a `rol_operation (5, 20/21)` row in `data.sql` would FK-violate on the dev fresh-boot (`sql.init.mode=always` runs `schema.sql`+`data.sql` with no role 5 present). The role-5 approve/reject grants therefore live in `add_soporte.sql` and `V2`, both guarded; `data.sql` keeps only the role-4 grants (ops 18,19). Behavior is identical once `add_soporte.sql`/`V2` is applied, and FK ordering is correct in every path.

## Static authz paths with userId in body

All four endpoints are static literals on `ProviderActivationController` (`/biometric`, `/status`, `/approve`, `/reject`). Approve and reject carry `userId` in the body via `ApproveProviderRequest`/`RejectProviderRequest`, matching the exact-equality authorization resolver that would otherwise deny a templated path for every caller including support. The seeded `operation.path` values are the same static strings, granted to role 4 (provider: biometric, status) and role 5 (support: approve, reject). This matches the design's HIGH-finding resolution exactly.

## Write-once frontal photo, enforced in two layers

Security is the system of record. `ProviderActivationRepoAdapter.insertBiometricCapture` reads the latest OK capture with a photo; if one exists it stores `null` on the new row and returns the already-stored URL, so a second biometric pass never overwrites the frontal photo and `recompute`/core-push both operate on the original (confirmed). `SubmitBiometricUseCaseImpl` uses that returned `fotoVigente` for the best-effort core push, so even the propagated value is the first one.

findu-core's edit is the single intended one-liner: `actualizarPerfil` now sets `urlImagenPerfil` only when the current value is `null`, while `nombreCompleto` stays editable (confirmed against the diff). This closes the overwrite path through `PUT /api/v1/perfil-proveedor/{id}`. The mobile profile screen is the third, defense-in-depth layer: `_showEditPhotoDialog` blocks when `fotoPerfilFrontalUrl` is set.

## Biometric re-validation and failure contract

`SubmitBiometricUseCaseImpl` rejects malformed or future dates with 400, returns 409 `perfil_core_requerido` when the core profile is missing, and treats name mismatch / date mismatch / `faceMatched=false` as business rejections returning 200 `{biometriaOk:false, motivo}` with a versioned `RECHAZADA` row — malformed input is the only 4xx. Name comparison is accent-insensitive via `TextNormalizer`; birthdate uses `LocalDate.isEqual`, avoiding the timezone off-by-one a `Date` coercion could introduce. The core photo push is composed with `onErrorResume` → `Mono.empty()`, so a core outage logs WARN and still returns 200 with security authoritative, reconciled later by the scheduler. Reactive composition and hexagonal boundaries (port/use case/adapter) are preserved throughout.

## Idempotent support ticket and help-v2 dedup

`ActivationProgressServiceImpl.recompute` fires the ticket only when `otp_ok && email_ok && biometria_ok` and `soporte_notified == false`; on 2xx it sets `soporte_notified=true` + stores `soporte_ticket_code` and advances to `EN_REVISION_SOPORTE`. On help-v2 failure it advances the local state but leaves the guard false and logs ERROR for the scheduler to retry — a fail-open-locally posture that never blocks the provider. Already-notified and terminal states are no-ops. The reject path resets `soporte_notified=false` so a re-submission can re-open review exactly once.

help-v2's second layer is real: `CreateTicketService.execute` looks up by `externalRef` first and returns the existing ticket with no save and no event publish on a hit, backed by a sparse unique Mongo index on `externalRef`. Security sends `externalRef=ACT-{userId}`, so a retry after a timeout where the first call actually succeeded cannot create a duplicate or re-publish — the `soporte_notified` flag and the ticket cannot diverge.

## Notifications and dispatcher routing

Approve/reject dispatch `PROVEEDOR_ACTIVADO`/`PROVEEDOR_RECHAZADO` as `NOTIFICACION` with `recipient=PROVEEDOR_{userId}` — the prefix the dispatcher's presence branch recognizes — and both dispatches are non-fatal. The dispatcher micro is unchanged (no diff in the LAMBDA/dispatcher tree, confirmed), consistent with the design's decision to drop the undeliverable `SOPORTE_` presence notification and rely on the help-v2 ticket as the sole support signal. Because the dispatcher hard-codes `EventType.OFFER_RECEIVED` for every presence event, the client cannot discriminate on type — and the mobile `ActivationRealtimeHandler.classify` correctly branches on `payload['templateCode']` (tolerant of root or nested payload), exactly matching the wire contract.

## Alert templates and client evaluation

The two ALERTA templates author every UI field under `alert_meta` (categoria=INFO, pantalla_destino=perfil_proveedor, titulo, condicion_aparicion, tiempo_desaparicion=0, plus the biometric CTA) with no top-level `title`, matching the mapper's read path. The NOTIFICACION templates use `routing{condicion:isUserActivo, canal_si_activo:WEBSOCKET, canal_si_inactivo:PUSH}`. `condicion_aparicion` uses the canonical `emailOk`/`biometriaOk`, and the seed's one legacy drift (`emailVerified`/`biometriaVerificada` on the old `VERIFICA_CORREO` example) was corrected to the canonical identifiers. The mobile `AlertConditionEvaluator` supports exactly `{emailOk, biometriaOk, otpOk, soporteOk}` with `==`/`!=` vs `true`/`false`, treats an empty condition as visible and a malformed/unknown one as hidden (fail-safe), and sorts visible alerts by `prioridad`.

## Login not blocked and profile exposure

No security-config, authentication, authorization-manager, or `isEnabled` file appears in the diff (confirmed by stat). `UserProfileResponse` is extended with `estadoActivacion`, the four canonical flags, and `fotoPerfilFrontalUrl`, populated in `JwtManagerImpl` as a non-fatal load. The mobile profile screen shows the "Usuario no activo" / "En revisión de soporte" / "Activación rechazada" badge off `estadoActivacion` and never touches the login path. OTP success composes the confirmation email into the reactive chain with `onErrorResume` (non-fatal, ordering explicit), and email verification marks `email_ok` and recomputes — both wired as designed.

## Test coverage and evidence

Backend unit coverage is substantial and directly targets the risk surface: `ActivationProgressServiceImplTest` (fire-once guard, 5xx-leaves-flag-false, already-activated no-op), `SubmitBiometricUseCaseImplTest` (400 vs 409 vs 200-business-rejection vs pass+write-once+recompute), approve/reject state guards, `TextNormalizerTest`, and help-v2's `CreateTicketServiceTest` idempotency case (save/publish invoked exactly once across two calls). Mobile adds 21 pure-Dart tests over the mock verifier, the alert evaluator, and the realtime classifier.

Not verified by integration: the security R2DBC repositories against Testcontainers Postgres and the core context load — both suites' sole failures are environment/pre-existing (Docker unavailable; the `estado_verificacion` H2 fixture gap that predates this task), so the additive migration applying cleanly on top of the live schema and the core write-once branch under a real context are unproven here. The mobile `widget_test.dart` failure is an untracked default-scaffold assertion unrelated to this work. None of these stem from the activation changes, but the migration should be applied to qa by hand and the Testcontainers suite run on a Docker-capable host before CI depends on it.

</details>

<details>
<summary>File map</summary>

- `SECURITY/findu-spring-security/.../schema.sql`, `scripts/migrations/V2__provider_activation.sql`, `data.sql`, `add_soporte.sql` — additive idempotent activation DDL + ops 18–21 + grants.
- `.../domain/model/{ProviderActivation,ProviderActivationState,ValidationType,ValidationState}.java`, entities + R2DBC repos + `ProviderActivationRepoAdapter` — aggregate, versioned validation rows, write-once photo.
- `.../usecase/impl/{SubmitBiometric,ApproveProvider,RejectProvider,GetActivationStatus,ActivationProgressService}Impl.java`, `VerifyPhoneOtpUseCaseImpl`, `VerifyEmailUseCaseImpl` — flow, state machine, OTP→email, recompute.
- `.../externalapi/{CorePerfil,SupportTicket}WebClientAdapter.java`, `ProviderActivationController.java`, DTOs, `TextNormalizer.java`, `ProviderActivationRetryScheduler.java` — ports, static endpoints, retry.
- `CORE/findu-core/.../GestionPerfilProveedorUseCaseImpl.java` — one-line write-once on `urlImagenPerfil`.
- `CORE/findu-help-v2/.../CreateTicketService.java` + `externalRef` through command/request/aggregate/document/repo — idempotent ticket.
- `CORE/findu-notification-processor/.../seed-templates.sh`, `AlertTemplateController` + `ScreenAlertServiceImpl` + DTO — template seed + by-code alert endpoint.
- `findu-movil/apps/findu_proveedores/...` — `BiometricVerifier` port + mock/real adapters, activation API adapter, profile banner/alerts/read-only photo, `AlertConditionEvaluator`, `ActivationRealtimeHandler`, tests.

Full diffs: `git -C findu/.worktrees/provider-activation diff develop` and `git -C findu-movil/apps/findu_proveedores diff 9057e1b..master` (mobile baseline differs; see FEAT-005).

</details>
