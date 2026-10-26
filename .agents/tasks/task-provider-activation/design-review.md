# Design Review — Provider Activation Flow (FIND-U)

Reviewed document: `design.md` (Status: REVISED, pass 3). Reviewer: independent design-review subagent, no prior context.
Review method: every claim the design marks "verified" was re-checked against the actual source in the worktree
(`findu/.worktrees/provider-activation`) and, where help-v2 is not under the worktree, against `findu/CORE/findu-help-v2`.

**Verdict: APPROVED** (0 HIGH, 0 MEDIUM, 3 NIT).

The design is unusually rigorous. The three prior review passes drove the authors to re-verify against source,
and those verifications hold up: I independently confirmed the dispatcher routing/`OFFER_RECEIVED` hard-coding,
the exact-equality authorization lookup, the ALERTA `alert_meta` mapping, the DynamoDB single-GSI wiring, the core
profile write-once gap, the OTP use-case chain, the help-v2 ticket contract, `isEnabled` semantics, the operation/role
seeds, and the template/enum shapes. All HIGH/MEDIUM items from pass 2 are genuinely resolved. What remains are three
low-severity robustness/completeness notes that do not block implementation.

Per the mechanical rule (HIGH+MEDIUM count = 0), this is APPROVED. NITs are recorded for the implementer but do not change the verdict.

---

## Findings

### 1. NIT — Migration `ON CONFLICT (id) DO NOTHING` does not cover the unique constraints on `operation`
**Where:** §3.2.1, the `V2__provider_activation.sql` INSERT into `operation`.
**Problem:** `schema.sql` defines `operation` with two unique constraints besides the PK:
`uq_operation_name UNIQUE (name)` and `uq_operation_module_path_method UNIQUE (module_id, path, http_method)`
(verified in `schema.sql`). The migration guards only `ON CONFLICT (id) DO NOTHING`. On a clean Railway qa that
holds exactly ids 1–17 this is harmless, but if an operation with the same `name` or the same
`(module_id, path, http_method)` already existed under a *different* id (e.g. a partially-applied earlier run),
the INSERT would raise a unique-violation on `name`/`path` that `ON CONFLICT (id)` does not catch, aborting the migration.
**Concrete fix:** either guard the natural key instead of the surrogate id —
`... ON CONFLICT (module_id, path, http_method) DO NOTHING` (and a second pass for name), or make the inserts
`WHERE NOT EXISTS (SELECT 1 FROM operation WHERE name = ...)` per row. Given these are brand-new names/paths
(`ACTIVATION_*`, `/api/v1/activation/*`) a collision is unlikely, hence NIT, but the guard should match the
constraint that can actually fire.

### 2. NIT — `soporte_ticket_code` source field is not pinned to a concrete response attribute
**Where:** §6.3 ("store the returned `ticketCode` in `soporte_ticket_code`"), §3.2 column `soporte_ticket_code VARCHAR(60)`.
**Problem:** The help-v2 aggregate `HelpTicket` exposes both `id` (a `String` UUID) and `ticketCode` (a `TicketCode`
value object whose string is `getValue()`, generated as `TicketCode.generate("HT")`), verified in `HelpTicket.java`.
The design says "store the returned `ticketCode`" but does not name the field on the create *response* DTO that
security's `SupportTicketPort` will read, nor whether it stores `ticketCode.getValue()` (e.g. `HT-...`) or the UUID `id`.
`VARCHAR(60)` is wide enough for either, so this is cosmetic, but the implementer must bind to the real response field.
**Concrete fix:** specify "store `HelpTicketResponse.ticketCode` (the `TicketCode.getValue()` string, format `HT-…`)";
confirm the field name on the create response DTO when wiring `SupportTicketPort`.

### 3. NIT — `externalRef` reconciliation on reject→re-open reuses one ticket but its lifecycle state is unspecified
**Where:** §6.3 (help-v2 dedup by `externalRef=ACT-{userId}`) and §9 ("the re-opened case reuses/updates the same
external-keyed ticket rather than spawning duplicates").
**Problem:** On a support reject the design resets `soporte_notified=false`; a later all-pass re-fires the ticket.
help-v2's dedup returns the *existing* ticket for `externalRef`. But a previously-rejected activation ticket may already
be in `RESUELTO`/`CERRADO` (support acted on it). Returning that same closed ticket as the "re-opened review" signal
means support may never see a fresh ABIERTO item in their queue for the second attempt. The design says
"reuses/updates" but does not state whether the existing ticket is reopened (status transition back to ABIERTO),
or whether a new ticket should be created when the prior one is terminal.
**Concrete fix:** pick one rule and state it: either (a) `externalRef` dedup only suppresses duplicates while the ticket
is non-terminal and a terminal (`CERRADO`) ticket allows a new one to be created (e.g. `externalRef=ACT-{userId}-{attempt}`),
or (b) on re-fire, if the `externalRef` ticket is terminal, help-v2 reopens it (ABIERTO) and appends a system message.
Keeping it a NIT because the double-fire *duplication* is correctly prevented; the gap is only the support-visibility
of the second attempt.

---

## Verified Assumptions

Each item below was checked against the cited source file and found to match the design's claim.

1. **Dispatcher recognizes only `PROVEEDOR_`/`CLIENTE_` prefixes; everything else is coerced to `CLIENTE_<x>`.**
   Confirmed in `findu-notification-dispatcher/.../NotificationDispatchController.despacharEventoPresencia` — the
   `else` branch sets `canalId = "CLIENTE_" + recipient`. (Supports §6.2 Finding 1 HIGH resolution: dropping the
   `SOPORTE_` presence notification is correct — `SOPORTE_5` would be mangled.)
2. **Presence envelope `EventType` is hard-coded to `OFFER_RECEIVED`; `TipoEventoCanal` only becomes `OFERTA`/`EVENTO_SISTEMA`
   on `OFERTA`/`SOLICITUD` substrings; `templateCode`/`type`/`tipoEvento` are copied into the payload.**
   Confirmed in the same method. (Supports §6.2/§8.2: the Flutter client must branch on `payload["templateCode"]`,
   not `EventType`. `PROVEEDOR_ACTIVADO`/`PROVEEDOR_RECHAZADO` contain neither `OFERTA` nor `SOLICITUD`, so they
   arrive as `MENSAJE`.)
3. **Transactional types `{SMS,EMAIL,PUSH,WHATSAPP}` go to the exchange; presence otherwise.**
   Confirmed: `CANALES_TRANSACCIONALES` + `esNotificacionTransaccional`. (Supports §6.1/§6.4 routing table.)
4. **Authorization resolves operations by exact string equality on `operation.path`.**
   Confirmed in `OperationRepository.java` `@Query`: `... AND o.path = :path AND o.http_method = :httpMethod AND o.active = TRUE`,
   and `FinduReactiveAuthorizationManager.resolveOperation` → `findByModulePathBaseAndPathAndHttpMethod`, with
   `switchIfEmpty → AuthorizationDecision(false)`. (Supports §3.2.1 HIGH-1 resolution: static paths with `userId`
   in the body are required; a `{userId}` path template could never match.)
5. **Existing static-path precedent (ids 6/8/9) and seed style.** Confirmed in `data.sql`: ops are plain
   `INSERT INTO operation (path, name, http_method, module_id, permite_all, active)` with id tracked in a trailing
   comment, ending at id 17; `module_id=2=CUSTOMERS`, `module_id=3=PROFILE`; `/api/v1/profile` and `/api/v1/customers`
   are static literals. (Supports §3.2.1 ids 18–21, `module_id=3`, and the "plain INSERT in data.sql" plan.)
6. **`operation.id` is `BIGINT GENERATED BY DEFAULT AS IDENTITY`.** Confirmed in `schema.sql`. (Supports the migration's
   `OVERRIDING SYSTEM VALUE` + `setval(pg_get_serial_sequence(...))` realignment — both are valid for a BY DEFAULT identity.)
7. **`schema.sql` begins with `DROP TABLE` and recreates everything.** Confirmed (DROPs for `user_rol`, `rol_operation`,
   `operation`, `role`, `module`, etc.). (Supports §3.2 constraint: the additive, idempotent migration must be separate
   so Railway qa with `spring.sql.init.mode=never` is never dropped on boot.)
8. **Role ids:** `ROLE_OUR_CLIENTE=1`, `ROLE_ASSISTANT_ADMINISTRATOR=2`, `ROLE_ADMINISTRATOR=3`, `ROLE_OUR_PROVEEDOR=4`
   in `data.sql`; `ROLE_OUR_SOPORTE=5` only in `add_soporte.sql`. (Supports §3.2.1 grants: provider=role 4 gets ops 18/19,
   support=role 5 gets ops 20/21.)
9. **`add_soporte.sql` seeds role 5, grants it ops 1–12, and assigns the `soporte` user BOTH role 5 and role 3.**
   Confirmed verbatim. (Supports §3.2.1 NIT-6 caution: keep ops 20/21 on role 5 only, not role 3.)
10. **`Usuario.isEnabled()` returns `active` (not activation state); `isAccountNonLocked`/`isAccountNonExpired` return true.**
    Confirmed in `Usuario.java`. (Supports §5.1: adding `estado_activacion` does not block login.)
11. **`VerifyPhoneOtpUseCaseImpl` saves via `usuarioService.save(user)`, deletes via `phoneOtpStore.delete(e164)`,
    returns `new PhoneVerificationResponse(saved.isPhoneVerified(), saved.getAccountStatus())`, resolves the user from the
    security context (`resolveAuthenticatedUser`), advances `PENDING_PHONE_VERIFICATION → PENDING_EMAIL_VERIFICATION`.**
    Confirmed. (Supports the §5.2 pseudocode-aligned email-after-OTP chain.)
12. **`SendEmailVerificationUseCase.sendVerificationForUser(Usuario): Mono<Void>` exists.** Confirmed in the interface
    and impl. (Supports §5.2 `.then(sendEmailVerificationUseCase.sendVerificationForUser(saved).onErrorResume(...))` —
    composable and context-preserving.)
13. **Core `GestionPerfilProveedorUseCaseImpl.actualizarPerfil(Long, ActualizarPerfilProveedorRequest)` currently does an
    unconditional `if (request.urlImagenPerfil() != null) perfil.setUrlImagenPerfil(request.urlImagenPerfil());`; there is
    no `PerfilProveedorService.actualizar`.** Confirmed in the use-case file. (Supports §3.4 layer 2 / pass-2 MEDIUM-2:
    the write-once fix lands in this use case, and the stated exact replacement is correct.)
14. **`ActualizarPerfilProveedorRequest` is `record(String nombreCompleto, String urlImagenPerfil)`.** Confirmed.
    (Supports §3.4: `urlImagenPerfil` is editable today, so core does NOT protect the frontal photo — the design no
    longer claims it does.)
15. **`consultarPerfil` returns `PerfilProveedorDetalleResponse` with `id` as the first field (the perfil id, distinct
    from authUserId).** Confirmed. (Supports §5.3 step 6 / pass-2 MEDIUM-9: the core PUT `{perfilId}` is the detalle `id`,
    not the auth user id.)
16. **DynamoDB adapter wires only `GSI_tipo` (`findu.notification.templates.gsi-tipo:GSI_tipo`); `findActive` is a single
    `GetItem` on `v#ACTIVE`; there is no `GSI_pantalla`.** Confirmed in `DynamoDbTemplateAdapter.java`. (Supports §7.4:
    the alerts read endpoint uses by-code `findActive(code, ALERTA, lang)`, no new GSI.)
17. **`NotificationTemplateItemMapper` nests ALERTA fields (`categoria`, `pantalla_destino`, `titulo`, `cta_label`,
    `cta_action`, `cerrable`, `prioridad`, `vigencia_*`, `condicion_aparicion`, `tiempo_desaparicion`) under `alert_meta`;
    it writes top-level `title` for ANY tipo via unconditional `putS(item, TITLE, tpl.getTitle())`.** Confirmed in
    `putAlertMeta`/`toAlertMeta`/`toItem`. (Supports §4 / pass-2 NIT-4: author the ALERTA title under `alert_meta.titulo`
    and leave the top-level `title` unset; it would be dead data on an ALERTA.)
18. **Routing nests under `routing` with `condicion`/`canal_si_activo`/`canal_si_inactivo`.** Confirmed in `putRouting`.
    (Supports §7 NOTIFICACION `routing={condicion:isUserActivo, canal_si_activo:WEBSOCKET, canal_si_inactivo:PUSH}`.)
19. **Enums:** `AlertCategory{INFO,SEGURIDAD,PREVENTIVA}`, `TemplateType{SMS,EMAIL,WHATSAPP,EVENTO,NOTIFICACION,ALERTA}`.
    Confirmed. (Supports §4 INFO category and §7 NOTIFICACION/ALERTA tipos.)
20. **Security `NotificationTemplates` has exactly the 5 codes** `CODIGO_VERIFICACION`, `RECUPERAR_PASSWORD`,
    `PASSWORD_CAMBIADA`, `VERIFICACION_OTP`, `VERIFICACION_EMAIL`; **core `NotificationTemplates` has exactly the 9 codes**
    enumerated in §7. Confirmed in both files. (Supports §7 legacy-seed enumeration — complete and accurate.)
21. **help-v2 `CreateTicketCommand`/`CreateTicketRequest` carry `userId`, `userRole` (enum `UserRole`), `subject`,
    `description`, `categoryId`/`subcategoryId`; `UserRole` includes `PROVEEDOR`; `CreateTicketService` has no dedup today;
    `externalRef` does NOT exist on request/command/`HelpTicket`.** Confirmed. (Supports §6.3: `userRole=PROVEEDOR` is valid,
    and adding `externalRef` + sparse unique index + lookup is a genuine net-new change.)
22. **help-v2 `HelpTicketController` is `@RequestMapping("/api/v1/help/tickets")` with `@PostMapping` to create.**
    Confirmed. (Supports §6.3/§6.4 endpoint `POST /findu-help-v2/api/v1/help/tickets`.)

## Unverified / Wrong Assumptions

No wrong assumptions were found. The items below could not be fully verified from source and are flagged as
*unverified* (not blocking); they are implementation-time confirmations the design itself already calls out.

1. **Mobile `findu_proveedores` specifics** — the existence of `BiometricVerificationService` with a hardcoded Unsplash
   URL (§8.1), `provider_profile_screen.dart`, `register_wizard_screen.dart`, the shared `RestHttpClient`, and the socket/FCM
   message model exposing a `payload` map (§8.2). The mobile repo (`findu-movil`) is a separate repo not under the
   backend worktree; these were not inspected. The design already treats the router/deep-link convention as a
   confirm-at-implementation item (§8.2 Finding 14), so this is consistent, not contradictory.
2. **`FINDU_NOTIFICATION_LAMBDA_URL` dispatcher endpoint and gateway base-url env names** (§1, §6, §11 config) — the
   dispatcher's `@PostMapping({"/dispatchNotification", "/api/v1/canales/dispatchNotification"})` was confirmed, but the
   exact env/property names security will reuse for core/help-v2/dispatcher base URLs were not traced end-to-end. The
   design says "reuse gateway env where possible", which is reasonable; confirm the property keys when wiring the WebClient adapters.
3. **help-v2 create-response field name for the ticket code** — see Finding 2. The aggregate exposes `ticketCode`
   (`TicketCode.getValue()`) and `id`; the response DTO field security reads was not pinned.
4. **Railway qa manual-migration operational model** — the design states the project applies additive SQL manually to
   Railway qa with `spring.sql.init.mode=never`. The `schema.sql` DROP-first structure and the `add_soporte.sql`
   idempotent `ON CONFLICT` pattern are consistent with this, but the actual Railway deploy/runbook was not inspected
   (out of band). The migration being additive + idempotent is the right safeguard regardless.

---

## Summary

Counts: **HIGH = 0, MEDIUM = 0, NIT = 3.** Verdict **APPROVED**.

The design stands on its own for implementation. Its source-verification claims are accurate — I re-derived each of the
load-bearing ones independently (dispatcher coercion + hard-coded `OFFER_RECEIVED`, exact-equality authorization,
`alert_meta` mapping, single-GSI DynamoDB, core write-once gap, OTP chain, help-v2 ticket contract, `isEnabled`, the
operation/role seeds). The state machine (PENDIENTE_VALIDACIONES / EN_REVISION_SOPORTE / ACTIVADO / RECHAZADO with the
four flagged validations and timestamps), the ordering invariant (support ticket only when otp+email+biometric are all
true), the non-blocking login, the mocked biometric behind an interface with documented ML Kit/TFLite wiring, the
accent-normalized name + `LocalDate` birth-date re-validation, the `soporte_notified` idempotency guard plus help-v2
`externalRef` dedup, the two INFO/ALERTA templates with the biometric `cta_action` deep link, the write-once frontal
photo across three layers, and the additive idempotent migration are all specified concretely and consistently.
The three NITs are robustness/completeness refinements that can be handled during implementation.
