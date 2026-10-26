# Verification — Provider Activation (cross-FEAT integration, iteration 1)

Reviewer/fixer pass. `review.md` did not exist, so this is the INTEGRATION verification run:
build every touched backend micro + run its tests, run `flutter analyze` / `flutter test`,
validate the DynamoDB seed with `bash -n`, and confirm the seams BETWEEN FEATs.

Environment constraints honored: no Railway deploy, no real AWS writes, mobile never run on a
device (analyze/test only). Reactive/hexagonal/Lombok preserved. Commits are local only.

## Commands run and results

Backend (each from its worktree dir, `.\gradlew.bat build` which also runs `test`):

| Micro | build | tests (total / fail / err) | Notes |
|---|---|---|---|
| SECURITY/findu-spring-security | FAILED (test only) | 146 / 1 / 0 | the 1 failure is the Docker-only context test (see below) |
| CORE/findu-help-v2 | SUCCESSFUL | 31 / 0 / 0 | — |
| CORE/findu-notification-processor | SUCCESSFUL | 29 / 0 / 0 | — |
| CORE/findu-core | FAILED (test only) | 6 / 1 / 0 | the 1 failure is a pre-existing H2 fixture mismatch (see below) |

Mobile (`findu-movil/apps/findu_proveedores`):

- `flutter analyze` -> 45 issues, ALL severity `info` (0 error, 0 warning). None in the
  activation/alert/biometric/verifier files; all are pre-existing deprecation lints
  (`withOpacity`, `use_null_aware_elements`) across legacy screens. Satisfies "no new errors".
- `flutter test` -> 31 passed, 1 failed. The failure is `test/widget_test.dart` (see below).
  All feature tests pass: `activation_realtime_handler_test`, `alert_condition_evaluator_test`,
  `mock_biometric_verifier_test`, and the three `auth_onboarding/*` adapter tests.

Seed:

- `bash -n CORE/findu-notification-processor/src/main/resources/db/dynamodb/seed-templates.sh`
  -> initially FAILED (`syntax error near unexpected token $'{\r'` — CRLF line endings).
  After the LF normalization fix below -> **OK**.

## Cross-FEAT seam checks (all consistent)

1. **Security dispatch payload vs dispatcher `DispatchNotificationRequest`.**
   `NotificationPort.send(type, recipient, templateCode, language, params)` ->
   `NotificationLambdaAdapter` POSTs `{type, recipient, templateCode, language, params}` to
   `/dispatchNotification`. Keys match the dispatcher contract. Approve/reject dispatch
   `NOTIFICACION` with `recipient = "PROVEEDOR_" + userId` and template codes
   `PROVEEDOR_ACTIVADO` / `PROVEEDOR_RECHAZADO` — the `PROVEEDOR_` prefix the dispatcher's
   presence branch recognizes. Consistent.

2. **Activation submit/approve/reject contract vs the mobile adapter.**
   Backend `BiometricSubmitRequest` fields `{cedulaNombre, cedulaFechaNacimiento, faceMatchScore,
   faceMatched, fotoFrontalUrl}` exactly match the mobile `ActivationApiAdapter.submitBiometric`
   JSON body. Endpoints are STATIC (`/api/v1/activation/{biometric,status,approve,reject}`);
   approve/reject carry `userId` (and `motivo`) in the body, matching
   `ApproveProviderRequest`/`RejectProviderRequest`. Mobile hits
   `/security-auth/api/v1/activation/*` via the gateway base URL. Consistent.

3. **Support ticket contract (security -> help-v2).**
   `SupportTicketWebClientAdapter` POSTs `{userId, userRole:"PROVEEDOR", subject, description,
   externalRef}` to `/findu-help-v2/api/v1/help/tickets`. help-v2 `CreateTicketRequest` has all
   those fields; `UserRole.PROVEEDOR` exists; `CreateTicketService` dedups by `externalRef`
   (returns the existing ticket on hit) and the response `HelpTicketResponse.ticketCode` is what
   the adapter reads back. Consistent and idempotent.

4. **Alert codes + `condicion_aparicion` identifiers (seed <-> mobile evaluator).**
   The read endpoint `GET /api/v1/templates/alertas?pantalla=perfil_proveedor` resolves exactly
   `PERFIL_CORREO_PENDIENTE` and `PERFIL_BIOMETRIA_PENDIENTE` by code. Their seeded
   `condicion_aparicion` (`emailOk == false`, `biometriaOk == false`) use the canonical
   identifiers that the mobile `AlertConditionEvaluator` supports exactly
   (`{emailOk, biometriaOk, otpOk, soporteOk}` with `==`/`!=` vs `true`/`false`). The security
   `UserProfileResponse` and `ActivationStatusResponse` expose those same four flags +
   `estadoActivacion` + `fotoPerfilFrontalUrl`. The realtime handler branches on
   `payload['templateCode']` (not `EventType`), per §6.2/§8.2. Consistent.

## Fixes applied this pass

1. **Seed identifier drift (seam 4).** The legacy example alert `VERIFICA_CORREO#ALERTA#es`
   (screen `perfil_proveedor`) still used the dropped identifiers in its condition:
   `emailVerified == false || biometriaVerificada == false`. Design §4 / Finding 4 canonicalized
   these to `emailOk` / `biometriaOk`. Updated the seed to
   `emailOk == false || biometriaOk == false` so the seed is internally consistent with the
   canonical identifier set. No user-visible behavior change: the `perfil_proveedor` by-code
   endpoint returns only the two new INFO alerts, so `VERIFICA_CORREO` is not fetched by the
   mobile evaluator for that screen.

2. **Seed line endings / `bash -n` gate.** `seed-templates.sh` was stored with CRLF, which made
   `bash -n` fail and would break execution on Linux/CI. Normalized the file to LF and added a
   scoped `CORE/findu-notification-processor/.gitattributes` with `*.sh text eol=lf` so the fix
   does not regress on checkout. `bash -n` now passes.

## Known failures — environment/pre-existing, NOT caused by this task

- **security `FinduSpringSecurityApplicationTests.contextLoads`** — requires a Testcontainers
  Docker environment (`Could not find a valid Docker environment`). No Docker available here.
  The test file predates the activation FEATs (last touched in the initial import / Docker-profile
  commits). The other 145 security tests pass, including the activation unit tests.
- **core `FinduCoreApplicationTests.contextLoads`** — fails loading the Spring context because the
  H2 test fixture `db/h2/data.sql` inserts into `perfil_proveedor.estado_verificacion`, a column
  absent from `db/h2/schema.sql`. This is a pre-existing H2 bootstrap mismatch. The only core
  change in this task is the one-line write-once edit in
  `GestionPerfilProveedorUseCaseImpl.actualizarPerfil` (verified present: `nombreCompleto` stays
  editable; `urlImagenPerfil` is set only when currently null). That edit touches no schema/SQL and
  did not introduce the failure.
- **mobile `test/widget_test.dart`** — an UNTRACKED (never committed) default-scaffold test that
  pumps the whole `FinduProveedoresApp` and asserts a literal `find.text('findu')` on the login
  screen. It is unrelated to the activation feature (the login screen itself is also untracked in
  this repo) and is a brittle full-app render assertion. Left as-is to avoid masking intent; it is
  not part of the activation deliverable. All activation feature tests pass.

## Net

All cross-FEAT seams verified consistent. Two seed-level integration defects fixed
(identifier drift + CRLF/`bash -n`). All compilable/unit test suites pass except three failures
that are strictly environment-only (Docker) or pre-existing/unrelated (core H2 fixture, untracked
scaffold widget test), none of which stem from the activation work.

---

# Final summary — provider activation (finalization step)

Convergence verdict: **APPROVED** (`verdict.json`). This step committed all work locally and
merged the backend feature branch into `develop`. **No push and no Railway deploy** were performed
(per the user's instruction to merge to develop only), and **no real AWS writes** were made.

## What shipped, per micro + mobile

- **SECURITY/findu-spring-security (FEAT-001)** — Provider activation state machine with four
  validations (OTP active, email passive, biometric passive, support final) tracked with
  history/versioning; biometric re-validation endpoint with accent-insensitive name + birthdate +
  face-match checks; support approve/reject; profile exposure of `estadoActivacion` + the four
  flags and read-only frontal photo URL (login is **not** blocked for non-active providers);
  event dispatch (confirmation email after OTP; `PROVEEDOR_ACTIVADO`/`PROVEEDOR_RECHAZADO`
  notifications; idempotent support ticket via help-v2). Additive/idempotent `schema.sql` DDL plus
  standalone `scripts/migrations/V2__provider_activation.sql` (ops 18-21, role grants,
  `add_soporte.sql` re-applied idempotently); `spring.sql.init.mode` left `never` for qa.
- **CORE/findu-help-v2 (FEAT-002)** — Idempotent provider-review ticket via `externalRef`
  (`ACT-{userId}`) threaded through request/command/aggregate/document with a sparse unique Mongo
  index and lookup-by-externalRef, so a retry returns the existing ticket with no duplicate ticket
  or event.
- **CORE/findu-notification-processor (FEAT-003)** — Seeded activation templates in the versioned
  `v#1`+`v#ACTIVE` schema: `PROVEEDOR_ACTIVADO`/`PROVEEDOR_RECHAZADO` (NOTIFICACION, routing
  `isUserActivo` -> WEBSOCKET else PUSH) and `PERFIL_CORREO_PENDIENTE`/`PERFIL_BIOMETRIA_PENDIENTE`
  (ALERTA, fields under `alert_meta`, categoria INFO, `pantalla_destino=perfil_proveedor`), plus
  missing legacy codes; added read endpoint `GET /api/v1/templates/alertas`. Seed script only — the
  real DynamoDB table is never dropped/recreated and no AWS call is executed.
- **CORE/findu-core (FEAT-004)** — Single scoped write-once guard on the frontal profile photo in
  `GestionPerfilProveedorUseCaseImpl.actualizarPerfil` (`nombreCompleto` stays editable).
- **findu-movil/apps/findu_proveedores (FEAT-005)** — `BiometricVerifier` port with a dev **mock**
  (default) and a documented ML Kit/TFLite real adapter behind `BIOMETRIC_MODE`; register wizard
  routed through the port and POSTing to the security activation endpoint; profile "usuario no
  activo" banner + the two INFO alerts (biometric alert CTA navigates in-app) consumed from the
  backend/engine and disappearing as each flag flips; realtime handler branches on
  `payload['templateCode']`; frontal photo read-only; login not blocked. The hardcoded Unsplash URL
  was removed.

## How it was verified

Per-micro Gradle `build`/`test` and mobile `flutter analyze`/`flutter test` were run during the
FEAT and integration passes (details above). Results: help-v2 and notification-processor build and
test green; security 145/146 and core 5/6, where each single failure is the Docker-only
`contextLoads` integration test (no Docker environment) or a pre-existing H2 test-fixture mismatch
in core — neither caused by this work. Mobile: `flutter analyze` reports only pre-existing `info`
lints (0 errors/0 warnings) and all activation feature tests pass; the one failing `widget_test.dart`
is the untracked default scaffold test, unrelated to this feature. The DynamoDB seed passes
`bash -n` after LF normalization. Mobile was never run on a device/emulator.

## Git state after finalization

- Backend main repo `develop`: `feature/provider-activation` merged with a `--no-ff` merge commit
  (`8100228`); 34 untracked working-tree files under help-v2 `.../adapter/out` and `.../port/out`
  that collided with the merge were byte-identical or stale encoding-mangled duplicates of the
  committed feature-branch versions, so they were backed up and removed before the merge, which then
  applied cleanly with no conflicts. `develop` is **6 commits ahead of `origin/develop` and NOT
  pushed**. The backend worktree (`feature/provider-activation`) and the mobile repo (`master`) have
  clean working trees for tracked files.
- **No push, no PR, no Railway deploy** in this step.

## Unversioned paths

- `findu-movil/packages/*` (e.g. `network_rest`, `network_ws`, `ui_kit`) are **outside** the
  `findu_proveedores` git repo and remain **unversioned**; FEAT-005 made **no** changes there
  (they were only read).
- Gradle wrapper jars under each backend micro's `gradle/wrapper/` are gitignored and were copied in
  locally only to run builds; they were **not** committed.
