# Implementation Plan — Fix OTP phone verification binding to the authenticated user

## Context (confirmed by reading the code)

**Bug (reproduced in local prod):** Google-federated provider logs in, lands in
`account_status=PENDING_PHONE_VERIFICATION` with `phone` empty, types the phone, gets the SMS OTP
fine, but `POST /api/v1/auth/phone/verify-otp` returns `404 "Usuario no encontrado para el teléfono"`.

**Root cause (confirmed):**
- `SendPhoneOtpUseCaseImpl.sendOtp(phone, codPhoneInternational)` generates/stores the OTP under the
  E.164 key and sends the SMS, but NEVER persists the phone/country on the user record.
- `VerifyPhoneOtpUseCaseImpl.verifyOtp(...)` validates the OTP against Redis (correct), then resolves
  the user via `usuarioService.getUserByPhoneAndCountry(country, phone)`. The federated user has an
  empty phone, so that lookup misses and throws `ResponseStatusException(404, ...)`.

**Decisions grounded in the code:**

1. **How the authenticated principal is actually resolved (critical finding).**
   `JwtAuthenticationFilter` is **disabled** — its class is annotated `//@Component` (commented out,
   file `config/security/authentication/filter/JwtAuthenticationFilter.java`) and it is not registered
   anywhere else (confirmed by grep). The ACTIVE authentication is
   `SecurityConfig.oauth2ResourceServer().jwt(...)`, which puts a `JwtAuthenticationToken` in the
   reactive context whose **principal is a `org.springframework.security.oauth2.jwt.Jwt`, NOT a
   `Usuario`**. Therefore we must NOT assume `principal instanceof Usuario`.
   The established in-repo pattern that already handles this correctly is
   `JwtManagerImpl.getCurrentUserProfile()` / `updateCurrentUserProfile()`:
   ```java
   ReactiveSecurityContextHolder.getContext()
       .flatMap(ctx -> { Authentication auth = ctx.getAuthentication();
           if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return Mono.empty();
           Object principal = auth.getPrincipal();
           String username;
           if (principal instanceof Usuario u) username = u.getUsername();
           else if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) username = jwt.getSubject();
           else return Mono.empty();
           return usuarioService.getUserByUsername(username).switchIfEmpty(usuarioService.getUserByEmail(username)); })
   ```
   `FinduReactiveAuthorizationManager.obtainOperations` corroborates it: it uses `auth.getName()`
   (the JWT subject) and resolves the DB user with `getUserByUsername(...).switchIfEmpty(getUserByEmail(...))`.

   **Chosen approach:** resolve the authenticated user INSIDE each use case via
   `ReactiveSecurityContextHolder`, reusing the exact `getCurrentUserProfile()` pattern (handle both
   `Usuario` and `Jwt` principals; look up by username then email). Rationale: (a) it matches the
   module's existing WebFlux convention, so reviewers see a familiar shape; (b) it keeps the controller
   untouched (task says don't touch the controller beyond what's needed — here nothing changes); (c) it
   works under the real active config (`Jwt` principal) where `@AuthenticationPrincipal Usuario` would
   be null. We will add a small private helper in each use case impl rather than a new shared class, to
   keep the change minimal and localized (two files already own this logic).

2. **Are these endpoints ever called unauthenticated?** No safe assumption of anonymous access.
   `SecurityConfig` routes everything except swagger/api-docs through `anyExchange().access(authorizationManager)`.
   `FinduReactiveAuthorizationManager` only permits anonymous when a matching DB `operation` row has
   `permite_all=true`. The controller javadoc claims "públicas (permite_all en BD)" but that is a DB
   data assumption, not a code guarantee, and the reproduced bug proves the frontend DOES send
   `Authorization: Bearer`. **Decision:** require an authenticated `Usuario`. If the principal cannot be
   resolved to a `Usuario`, return `ResponseStatusException(HttpStatus.UNAUTHORIZED, ...)`, which
   `GlobalExceptionHandler.handleResponseStatus` already maps to a clean 401 JSON body. This covers both
   the federated and traditional wizard flows, since both arrive authenticated.

3. **Field / column / method names (confirmed, not assumed):**
   - `Usuario` (domain, `domain/model/Usuario.java`): `getPhone/setPhone`, `getCodPhoneInternational/setCodPhoneInternational`,
     `setPhoneVerified(boolean)` / `isPhoneVerified()`, `getAccountStatus()/setAccountStatus(String)`, `getUsername()`.
   - `UserEntity` (`infrastructure/entity/UserEntity.java`): `phone` (column `phone`),
     `codPhoneInternational` (`@Column("cod_phone_international")`), `phoneVerified` (`@Column("phone_verified")`),
     `accountStatus` (`@Column("account_status")`, default `"PENDING_PHONE_VERIFICATION"`).
   - `UsuarioService` (`application/service/UsuarioService.java`): `getUserByUsername(String)`,
     `getUserByEmail(String)`, `save(Usuario)` — all exist and return `Mono<Usuario>`.
   - `UsuarioRepoAdapter.save(Usuario)` (confirmed): when `usuario.getId() != null` it loads the existing
     row and copies `phone`, `codPhoneInternational`, `phoneVerified`, `accountStatus` (and others) before
     saving. The federated user already has an id, so persisting phone+country+verified+status via `save`
     works with no schema or adapter change.
   - Default country constant already present in both impls: `DEFAULT_COUNTRY_CODE = "+57"`.
   - Status constants already present in verify impl: `STATUS_PENDING_PHONE = "PENDING_PHONE_VERIFICATION"`,
     `STATUS_PENDING_EMAIL = "PENDING_EMAIL_VERIFICATION"`.

4. **What stays untouched (per task):** OTP generation, the dev fixed-OTP branch
   (`findu.auth.dev.otp-code`, which is `123456` in `application-dev.yml`), Redis store (`PhoneOtpStorePort`),
   the notification send (`notificationPort.send(...)`), the controller, the DTOs, the frontend, and the
   core/notification microservices.

**Build/test (confirmed):** Gradle module at
`c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\SECURITY\findu-spring-security`, wrapper `gradlew.bat`
present, Java 21 toolchain, JUnit 5 + Mockito + reactor-test + spring-security-test. Unit tests for both
use cases already exist (`src/test/java/com/findu/security/application/usecase/impl/SendPhoneOtpUseCaseImplTest.java`,
`VerifyPhoneOtpUseCaseImplTest.java`). Note: `check` enforces 80% line coverage via jacoco, but running
`test` alone does not — plan verifies with `test`. All commands below run from the module directory.

---

## Plan

- [ ] 1. Persist phone+country on the authenticated user in send-otp, keeping OTP/SMS unchanged.
      In `SendPhoneOtpUseCaseImpl`: inject `UsuarioService` (add to constructor), and add a private
      `Mono<Usuario> resolveAuthenticatedUser()` helper that reads `ReactiveSecurityContextHolder`
      following the exact `JwtManagerImpl.getCurrentUserProfile()` pattern (reject null/anonymous,
      accept `Usuario` or `Jwt` principal, look up `getUserByUsername(sub).switchIfEmpty(getUserByEmail(sub))`),
      returning `Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado"))` on
      failure. In `sendOtp`, after computing `country`/`trimmedPhone`, resolve the user, set
      `user.setPhone(trimmedPhone)` and `user.setCodPhoneInternational(country)`, call
      `usuarioService.save(user)`, and THEN run the existing OTP store+notify chain unchanged
      (`phoneOtpStore.save(...).doOnSuccess(... notificationPort.send ...).then()`). Keep the empty-phone
      guard and the dev fixed-OTP branch exactly as they are.
      Files: `src/main/java/com/findu/security/application/usecase/impl/SendPhoneOtpUseCaseImpl.java`
      Verify: `.\gradlew.bat test --tests "com.findu.security.application.usecase.impl.SendPhoneOtpUseCaseImplTest"`
      compiles and passes (existing tests must be updated in step 3 to stub the new `UsuarioService` calls;
      run after step 3).

- [ ] 2. Resolve the authenticated user from the security context in verify-otp instead of lookup-by-phone.
      In `VerifyPhoneOtpUseCaseImpl`: add the same `resolveAuthenticatedUser()` helper (reusing the pattern
      from step 1 — identical logic). After the OTP checks succeed (leave the Redis `find`, expiry, and
      `passwordEncoder.matches` logic exactly as-is), REPLACE the
      `usuarioService.getUserByPhoneAndCountry(country, trimmedPhone).switchIfEmpty(404 ...)` call with
      `resolveAuthenticatedUser()`. For coherence, validate that the request phone matches what is stored
      on the authenticated user: if `user.getPhone()` is non-null/non-blank and does not equal
      `trimmedPhone` (compare country too when stored), return
      `ResponseStatusException(HttpStatus.BAD_REQUEST, "El teléfono no coincide con el usuario autenticado")`;
      if the stored phone is blank (federated path where send-otp just set it), persist `trimmedPhone`/`country`
      as a safety net. Then set `phoneVerified=true`, advance `PENDING_PHONE_VERIFICATION ->
      PENDING_EMAIL_VERIFICATION` (reuse existing constants), `usuarioService.save(user)`,
      `phoneOtpStore.delete(e164)`, and return `PhoneVerificationResponse(saved.isPhoneVerified(),
      saved.getAccountStatus())` as today. Remove the now-unused `getUserByPhoneAndCountry` call path.
      Files: `src/main/java/com/findu/security/application/usecase/impl/VerifyPhoneOtpUseCaseImpl.java`
      Verify: `.\gradlew.bat test --tests "com.findu.security.application.usecase.impl.VerifyPhoneOtpUseCaseImplTest"`
      compiles and passes (tests updated in step 3; run after step 3).

- [ ] 3. Update the two existing unit tests to the context-based identity and add coverage for the fix.
      Both tests construct the use case directly and must set a `ReactiveSecurityContextHolder` context.
      Use `.contextWrite(ReactiveSecurityContextHolder.withAuthentication(new UsernamePasswordAuthenticationToken(usuario, null)))`
      on the `StepVerifier` publisher (the `JwtManagerImplTest` already uses `withSecurityContext`/
      `SecurityContextImpl` — follow that import set). 
      For `SendPhoneOtpUseCaseImplTest`: pass a mock `UsuarioService` into the new constructor; in the
      happy-path tests stub `getUserByUsername(...)` to return a `Usuario` (id set) and `save(any())` to
      echo its argument; assert `save` was called with `phone`/`codPhoneInternational` set
      (`ArgumentCaptor<Usuario>`); keep the empty-phone test (should short-circuit before resolving the user,
      so no security context required). Add a test: no authentication context -> errors with
      `ResponseStatusException` (401).
      For `VerifyPhoneOtpUseCaseImplTest`: drop the `getUserByPhoneAndCountry` stubs; stub
      `getUserByUsername(...)`/`getUserByEmail(...)` for the authenticated principal instead. Update
      `verifyOtp_success_advancesStatus` to assert status advances. Replace `verifyOtp_userNotFound_errors`
      with `verifyOtp_notAuthenticated_errors` (empty security context -> 401). Add
      `verifyOtp_phoneMismatch_errors` (authenticated user has a different stored phone -> 400). Keep the
      blank/notFound(OTP)/expired/wrongCode tests; those fail before user resolution and need no context.
      Files:
      `src/test/java/com/findu/security/application/usecase/impl/SendPhoneOtpUseCaseImplTest.java`,
      `src/test/java/com/findu/security/application/usecase/impl/VerifyPhoneOtpUseCaseImplTest.java`
      Verify: `.\gradlew.bat test --tests "com.findu.security.application.usecase.impl.SendPhoneOtpUseCaseImplTest" --tests "com.findu.security.application.usecase.impl.VerifyPhoneOtpUseCaseImplTest"`
      — all tests pass.

- [ ] 4. Full module build and test to confirm nothing else regressed (controller wiring, Spring context).
      No controller/DTO/interface signatures change, so no caller updates are expected; this step catches
      compile/wiring regressions from the constructor change in step 1.
      Files: none (verification only).
      Verify: `.\gradlew.bat clean test` from
      `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\SECURITY\findu-spring-security` — BUILD SUCCESSFUL,
      all tests green.

- [ ] 5. Commit the fix on branch `develop` (work directly on develop; do NOT create a worktree/branch).
      Stage only the four changed files (the two use-case impls and the two tests). Commit message e.g.
      `fix(security): vincular OTP de teléfono al usuario autenticado (resuelve 404 en verify-otp federado)`.
      Files: none (git only).
      Verify: `git -C c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu status` shows a clean tree on `develop`
      with the new commit; `git -C c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu log --oneline -1` shows it.
      Do NOT push.

---

## Notes / assumptions

- The user's broader asks (clean the users DB; frontend showing 1 min vs the SMS's 5 min; dev default OTP
  `123456` and generic email) are OUTSIDE this backend bug's scope. The dev fixed OTP (`123456`) and the
  OTP expiry (`findu.auth.phone-otp-expiry-seconds:300` = 5 min, surfaced to the SMS as
  `expiration_minutes`) are already configured in code/`application-dev.yml`; the "1 min" discrepancy is a
  frontend display concern and the frontend is explicitly out of scope here. Cleaning the users table is a
  data/ops action, not a code change, and is not part of this fix. These are noted for traceability only.
- Country normalization already defaults to `+57`; the fix preserves that for both persistence and the
  coherence check.
- If, during implementation, the reproduced flow still 404s, re-check that the OTP store key (`e164`) and
  the authenticated subject actually belong to the same user; do not revert to lookup-by-phone — the task
  requires identity from the token.
