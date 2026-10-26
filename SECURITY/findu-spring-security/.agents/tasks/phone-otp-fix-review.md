# Phone OTP verification bound to the authenticated user

The fix changes send-otp and verify-otp in `findu-spring-security` so the provider's phone is associated with, and verified against, the user resolved from the JWT rather than a phone lookup. Previously verify-otp called `getUserByPhoneAndCountry(...)` and returned 404 "Usuario no encontrado para el teléfono" because a federated user's phone is never persisted at account creation. Now send-otp resolves the authenticated user from the reactive security context and persists the request phone + country code before generating the OTP, and verify-otp resolves that same authenticated user, validates the request phone for coherence, marks `phoneVerified=true`, and advances `account_status` PENDING_PHONE_VERIFICATION -> PENDING_EMAIL_VERIFICATION. The OTP generation, Redis store, and SMS notification chain are moved verbatim into a helper and left behaviorally unchanged.

Watch for: the not-authenticated unit tests assert only a generic `ResponseStatusException`, not the 401 status specifically (confirmed, non-blocking); the global coverage gate was not exercised (verification used `test --tests`, documented and acceptable per the task).

**Verdict**: APPROVED

## High-level view

Identity now comes from the token on both endpoints. `resolveAuthenticatedUser()` is a verbatim copy of the principal-resolution block in `JwtManagerImpl.getCurrentUserProfile()`: it rejects null / unauthenticated / anonymous, accepts a `Usuario` or `Jwt` principal, resolves by username then falls back to email, and raises 401 "No autenticado" when the context is empty. This keeps the module's existing pattern intact rather than inventing a new resolution path.

Phone binding happens in send-otp. After computing the trimmed phone and country code, the authenticated user is loaded, `phone` + `codPhoneInternational` are set, and the user is saved before the OTP is generated and sent. verify-otp then operates purely on the token identity: it validates the request phone against the stored phone (400 on a real mismatch), persists phone/country as a federated safety net if the stored phone is still blank, sets `phoneVerified=true`, and advances the status only when it is PENDING_PHONE_VERIFICATION.

The field and column mappings are the real ones. `Usuario` exposes `phone`, `codPhoneInternational`, `phoneVerified`, `accountStatus`; `UserEntity` maps them to `phone`, `cod_phone_international`, `phone_verified`, `account_status`, matching `schema.sql`. `save` routes through the same adapter that `getCurrentUserProfile`/`updateCurrentUserProfile` already use to persist phone changes, so the persistence path is proven.

Both wizard flows converge on the same end state. The traditional flow already has a phone and passes the coherence check; the federated flow gets the phone written in send-otp (and re-asserted in verify-otp if blank). Either way the account ends with phone associated, `phone_verified=true`, `account_status=PENDING_EMAIL_VERIFICATION`.

The OTP/Redis/SMS chain is untouched. The dev fixed-OTP branch, hashing, `phoneOtpStore.save` with TTL, and `notificationPort.send` (template `VERIFICACION_OTP`, params `otp_code` and `expiration_minutes`) were relocated into `generateAndSend` without behavioral change. The frontend, core, and notification services are outside this diff.

<details>
<summary>Issues (1)</summary>

1. **Not-authenticated tests don't assert status code** — `sendOtp_notAuthenticated_errors` and `verifyOtp_notAuthenticated_errors` expect a bare `ResponseStatusException`; they would still pass if the code regressed to a non-401 status. Optionally tighten to assert `HttpStatus.UNAUTHORIZED`. Non-blocking.

</details>

<details>
<summary>Details</summary>

### Principal resolution reuses the established pattern

`resolveAuthenticatedUser()` in both use cases is byte-for-byte the principal-resolution block from `JwtManagerImpl.getCurrentUserProfile()`: null/`!isAuthenticated()`/`AnonymousAuthenticationToken` short-circuit to `Mono.empty()`, a `Usuario` principal yields `getUsername()`, a `Jwt` principal yields `getSubject()`, any other principal type short-circuits, then lookup is `getUserByUsername(...).switchIfEmpty(getUserByEmail(...))`, and the terminal `switchIfEmpty` raises 401 "No autenticado". Reusing the exact pattern means the two endpoints resolve identity coherently with the rest of the module. The helper is duplicated across both files rather than extracted to a shared component, which is a cosmetic duplication, not a correctness issue, and is in line with how the module already repeats this block in `getCurrentUserProfile` and `updateCurrentUserProfile`.

A non-authenticated principal reaches the empty context branch and surfaces as `ResponseStatusException(UNAUTHORIZED)`. In verify-otp this check runs after the OTP has been validated against Redis, so a valid-OTP-but-no-token request is rejected 401 rather than silently proceeding — fail-closed on identity.

### Phone binding in send-otp, coherence in verify-otp

send-otp binds first, sends second:

```
resolveAuthenticatedUser -> setPhone/setCodPhoneInternational -> save -> generateAndSend
```

Because the save precedes OTP generation, by the time the user receives the SMS the phone is already associated with their account, so verify-otp finds a non-blank stored phone that matches the request. verify-otp enforces coherence: a stored phone that is present and differs from the request phone is a 400 "El teléfono no coincide con el usuario autenticado"; a blank stored phone is treated as the federated case and the request phone/country are written as a safety net. Identity is never derived from the request phone — it always comes from the token — so the request phone is a value to validate, not a key to look up. This closes the original 404 defect directly.

The status advance is guarded: `account_status` moves to PENDING_EMAIL_VERIFICATION only when it currently equals PENDING_PHONE_VERIFICATION, so re-verifying a phone on an account already further along the wizard will not regress or jump the state. `phoneVerified=true` is set unconditionally on success, which is the intended terminal effect.

### Field and column mappings are the real ones

`UserEntity` maps `codPhoneInternational -> cod_phone_international`, `phoneVerified -> phone_verified`, `accountStatus -> account_status`, `phone -> phone`, all matching `schema.sql` (`"user"` table), and the status string constants in both use cases (`PENDING_PHONE_VERIFICATION`, `PENDING_EMAIL_VERIFICATION`) match the entity and domain defaults. `UsuarioServiceImpl.save` delegates to the same `usuarioRepoAdapter.save` that `JwtManagerImpl.updateCurrentUserProfile` already uses to persist `phone`/`codPhoneInternational`, so the domain->entity->row path for these fields is exercised elsewhere in the module and not new code.

### OTP / Redis / SMS chain unchanged

`generateAndSend` holds the previous body verbatim: dev fixed-OTP branch keyed on `findu.auth.dev.otp-code`, `%06d` random otherwise, `passwordEncoder.encode`, `PhoneOtp(hash, expiresAt, 0)` serialized to JSON, `phoneOtpStore.save(e164, json, TTL)`, and `notificationPort.send("SMS", e164, VERIFICACION_OTP, "es", {otp_code, expiration_minutes})` fired in `doOnSuccess`. `expiration_minutes` is `max(1, otpExpirySeconds/60)`, which is 5 at the default 300s — consistent with the "5 min" wording in the SMS template. The frontend countdown and any 1-minute cooldown display are in `FRONTEND/proveedor/main.js`, outside this diff and out of scope here.

### Test coverage

send-otp: phone/country persisted on the authenticated user (asserted via `ArgumentCaptor` on `save`), country-code default, empty-phone early return, and not-authenticated error. verify-otp: success advancing status using the authenticated user, federated blank-phone persistence, phone-mismatch 400, not-authenticated error, plus the retained pre-OTP failure tests (blank input, missing/expired/invalid code) that need no security context. The verification evidence records `BUILD SUCCESSFUL`, 12 tests green, via the targeted `test --tests` run for both classes.

Not tested: the two not-authenticated tests assert only `ResponseStatusException` without pinning the status to 401, so a regression to a different status code would go undetected. The `Jwt`-principal branch of `resolveAuthenticatedUser()` is not directly covered (tests use a `Usuario` principal), though it is identical to the already-exercised `getCurrentUserProfile` path.

### Verification evidence and coverage gate

The coder's evidence (`phone-otp-fix-verification.md`) documents the targeted test command and result, a clean `bootJar -x test`, and explains that the global coverage `check` gate was deliberately not run (the targeted `test --tests` path was used instead, consistent with the known Docker Desktop/Testcontainers limitation on this machine). Per the task, a documented coverage-gate skip with the targeted new tests passing is acceptable and is not treated as a blocking finding. No controller/DTO/interface signatures changed; the only constructor change (`SendPhoneOtpUseCaseImpl` gaining `UsuarioService`) is Spring-injected and updated in the unit test.

</details>

<details>
<summary>File map</summary>

- `application/usecase/impl/SendPhoneOtpUseCaseImpl.java` — inject `UsuarioService`; resolve authenticated user and persist phone/country before OTP; OTP/Redis/SMS moved verbatim into `generateAndSend`.
- `application/usecase/impl/VerifyPhoneOtpUseCaseImpl.java` — replace phone lookup + 404 with authenticated-user resolution; request-phone coherence check (400 on mismatch); federated blank-phone safety-net persist; mark verified + advance status.
- `test/.../SendPhoneOtpUseCaseImplTest.java` — security-context setup, `UsuarioService` mock, ArgumentCaptor assertion on persisted phone/country, not-authenticated test.
- `test/.../VerifyPhoneOtpUseCaseImplTest.java` — drop phone-lookup stubs; identity from context; success/federated/mismatch/not-authenticated cases.
- `.agents/tasks/phone-otp-fix-verification.md` — coder's build/test evidence.

Full diff: `git show f108f50`.

</details>
