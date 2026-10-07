# Verification — Fix OTP phone verification bound to the authenticated user

Module: `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\SECURITY\findu-spring-security`
Branch: `develop` (confirmed via `git rev-parse --abbrev-ref HEAD` -> `develop`)
Iteration: first (no `phone-otp-fix-review.json` present).

## Changes implemented

- `src/main/java/com/findu/security/application/usecase/impl/SendPhoneOtpUseCaseImpl.java`
  - Injected `UsuarioService` into the constructor.
  - Added private `resolveAuthenticatedUser()` that reads `ReactiveSecurityContextHolder`
    following the exact `JwtManagerImpl.getCurrentUserProfile()` pattern (reject null/anonymous,
    accept `Usuario` or `Jwt` principal, resolve by username then email, else 401 "No autenticado").
  - In `sendOtp`, after computing phone/country, the authenticated user is resolved and persisted
    with the request phone + country code (`setPhone` / `setCodPhoneInternational` + `usuarioService.save`).
  - OTP generation, the dev fixed-OTP branch, the Redis store (`phoneOtpStore.save`) and
    `notificationPort.send` were kept EXACTLY as before (moved verbatim into a private
    `generateAndSend` helper so the save runs first).

- `src/main/java/com/findu/security/application/usecase/impl/VerifyPhoneOtpUseCaseImpl.java`
  - Added the same `resolveAuthenticatedUser()` helper.
  - OTP checks against Redis (find/expiry/`passwordEncoder.matches`) kept EXACTLY as before.
  - Replaced `getUserByPhoneAndCountry(...)` + 404 with `resolveAuthenticatedUser()`.
  - Coherence check: if the authenticated user already has a non-blank phone that differs from the
    request phone -> 400 "El teléfono no coincide con el usuario autenticado". If blank (federated
    path) the phone/country are persisted as a safety net.
  - Then `phoneVerified=true`, advance `PENDING_PHONE_VERIFICATION -> PENDING_EMAIL_VERIFICATION`,
    `usuarioService.save(user)`, `phoneOtpStore.delete(e164)`, return `PhoneVerificationResponse`.

- Tests updated:
  - `SendPhoneOtpUseCaseImplTest`: new `UsuarioService` mock + security context; asserts `save`
    persists phone/country via `ArgumentCaptor`; added `sendOtp_notAuthenticated_errors` (401).
  - `VerifyPhoneOtpUseCaseImplTest`: dropped `getUserByPhoneAndCountry` stubs; identity now from the
    security context. Covers success (status advances), federated blank-phone persistence,
    phone-mismatch (400), and not-authenticated (401). Pre-OTP failure tests kept (no context needed).

## Commands and results

### 1. Targeted tests for the new behavior

Command (PowerShell, from module dir):

    .\gradlew.bat test --tests "com.findu.security.application.usecase.impl.SendPhoneOtpUseCaseImplTest" --tests "com.findu.security.application.usecase.impl.VerifyPhoneOtpUseCaseImplTest" --no-daemon

Result: `BUILD SUCCESSFUL in 35s`. 12 tests completed, all green (4 in SendPhoneOtp..., 8 in
VerifyPhoneOtp...). No Docker/Testcontainers needed for these slice tests.

### 2. Clean bootJar (skip tests)

Command (PowerShell, from module dir):

    .\gradlew.bat clean bootJar -x test --no-daemon

Result: `BUILD SUCCESSFUL in 29s`. Jar produced at
`build\libs\findu-spring-security-0.0.1-SNAPSHOT.jar` (~77 MB), confirmed via
`Get-ChildItem build\libs`.

### 3. Deployment

Not performed — the orchestrator handles Railway. Only the compiled jar is left in `build\libs`.

## Notes

- No global coverage gate was triggered because the verification used `test --tests` (not `check`),
  per the plan. The coverage threshold was NOT lowered and no unrelated tests were disabled.
- No controller/DTO/interface signatures changed; the only constructor change
  (`SendPhoneOtpUseCaseImpl`) is injected by Spring and updated in its unit test. A grep confirmed
  the impl is constructed directly only in its test.
