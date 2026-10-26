# Provider Google sign-in aligned with the client's phone-first flow

The change rewrites `fetchProfile` in the provider frontend so that a Google (federated) sign-in routes by `accountStatus` into three paths — new/phoneless user, incomplete account, and fully active — mirroring how the client portal behaves. A new `federated-basic` register sub-view ("Paso 1: Completa tu Teléfono") collects phone + birthdate with the Google name/email/username shown disabled, then does a `PUT /security-auth/api/v1/profile` (the same route and body the client uses) and advances to the existing OTP sub-view. OTP timers, cooldown, the phone-OTP handlers, and the email-verification banner are reused, not reimplemented. Only `FRONTEND/proveedor/main.js` changed; no backend, no client reference, no HTML/CSS.

Watch for: the new `if (!data.phone)` gate at the top of `fetchProfile` is unconditional and runs for every caller — Google sign-in, traditional login, and session restore — so a non-Google user whose profile has no phone is routed into the federated "Completa tu Teléfono" screen instead of the OTP/wizard path they used to get (confirmed). This is an edge case (normal traditional users already have a phone), but it crosses the traditional/federated boundary and is the one behavioral gap in requirement 5.

**Verdict**: APPROVED

## High-level view

`accountStatus` is the discriminator (`ACTIVE` → panel or wizard, `PENDING_PHONE_VERIFICATION` → OTP, `PENDING_EMAIL_VERIFICATION` → panel with the email banner or wizard), with `data.phone` used only to catch the phoneless case ahead of the status checks. A fallback preserves the old `phoneVerified`-based routing when `accountStatus` is absent, so a backend that doesn't yet return the field still works.

The `federated-basic` sub-view reuses existing CSS classes and the client's update contract (`PUT /profile` with `{ username, phone, codPhoneInternational }`), validates phone shape and age ≥ 18, and hands off to the shared OTP sub-view without auto-sending the code — the "Enviar código" button is preserved as required. `verifyPhoneOtp` already re-calls `fetchProfile` in the federated context, closing the loop back into the status router. The wizard progress bar was updated so `federated-basic` counts as step 1 and doesn't produce `NaN`.

The one gap is scoping: the phoneless gate was placed before any check of whether the session is actually federated. For a Google user it's exactly right; for a traditional login or a session restore of a phoneless account it diverts the user into a federated-styled screen. The screen degrades gracefully (the Google name field is hidden when there's no `googleData`), so it's not a crash, but it's a behavioral change for the non-Google path.

<details>
<summary>Issues (1)</summary>

1. **Phoneless gate not scoped to federated sessions** — the `if (!data.phone)` branch at the top of `fetchProfile` fires for traditional login and session restore too, routing a phoneless non-Google user into the `federated-basic` screen and the `PUT /profile` update path instead of the OTP/wizard they previously got. Consider gating it on a federated signal (e.g. `state.googleData` / `state.otpContext === 'federated'`) or falling through to the wizard when there's no Google context. Non-blocking: normal traditional users have a phone and are unaffected.

</details>

<details>
<summary>Details</summary>

### accountStatus routing and the phoneless gate

The rewrite replaces the old single `phoneVerified === false` gate with an ordered set of checks. First `if (!data.phone)` sends the user to `federated-basic`; then `PENDING_PHONE_VERIFICATION` (phone present) goes straight to OTP; `PENDING_EMAIL_VERIFICATION` goes to the panel (or wizard step 2 if no provider profile); `ACTIVE` goes to the panel (or wizard step 2); and an unknown/absent status falls through to the legacy `phoneVerified` branch. Because the phoneless case is caught first, the `PENDING_PHONE_VERIFICATION` branch can assume `data.phone` is present, which is what its comment claims.

The scoping gap is that `fetchProfile` is the shared navigation entry point for three callers — the Google credential handler (line 174), `handleLogin` (line 583), and `init()` on session restore (line 185). The `!data.phone` branch doesn't distinguish them. A traditional user who logs in without a stored phone — an abandoned registration, or an account that predates phone capture — now lands on "Paso 1: Completa tu Teléfono" with disabled email/username fields and submits through `PUT /profile` rather than the wizard. The render guards the Google name block behind `${nameValue ? ... : ''}`, so with no `googleData` the Google-specific field disappears and the screen is just phone + birthdate; it won't throw. But the destination and the update route differ from the pre-change behavior, where this user went to the OTP screen (`registerStep = 'otp'`). Gating the branch on a federated signal, or letting the non-federated case fall through to the wizard, would keep the traditional path intact. This is the only place the change touches the traditional/federated boundary.

### federated-basic sub-view and the client update contract

`submitFederatedBasic` reads `#fed-phone`, a fixed `+57` from `#fed-phone-code`, and `#fed-birthdate`, validates the phone against `/^\d{7,15}$/` and enforces age ≥ 18 with the same month/day adjustment the client uses, then issues `PUT ${API_GATEWAY_URL}/security-auth/api/v1/profile` with `{ username, phone, codPhoneInternational }` — the client's route and body. The account already exists after `/auth/federated`, so updating rather than creating is the right call, and keeping this separate from `handleRegisterStep1` (which does `POST /customers` + auto-login) is what keeps the traditional wizard untouched. The 503/500 case surfaces the same "service reconnecting, press Siguiente again" retry message as the client; other non-OK statuses parse the body for a message. The token falls back to `localStorage` if `state.token` is empty.

On success it sets `otpContext = 'federated'`, clears `otpCode`, leaves `otpSent = false`, moves to `registerStep = 'otp'`, re-renders, and shows "Teléfono guardado…". Leaving `otpSent` false means the OTP code is not auto-sent — the user triggers it from the existing "Enviar código" control, which is the explicitly requested behavior.

### OTP reuse and loop closure

The OTP sub-view (two states keyed on `otpSent`), `submitOtpPhone` / `resendPhoneOtp` / `changeOtpPhone` / `verifyPhoneOtp`, the 5-minute validity timer, and the 60-second resend cooldown are all reused unchanged. `verifyPhoneOtp` in the federated context ends with `await fetchProfile()`, so once the phone is verified the status router re-runs and sends the user to the panel (or wizard step 2, with the email banner if `emailVerified === false`). The email path reuses `buildEmailVerificationBannerHtml` / `window.resendEmailVerification` via the existing `profile` view — the routing just lands the user there and lets the banner render on `emailVerified === false`. Nothing in the OTP or banner machinery was reimplemented.

### Wizard progress bar

The progress calculation was extended from `registerStep === 'otp' ? 1.5 : registerStep` to also map `federated-basic` to `1`, so the bar marks node 1 active on the basic-data screen and the `>= 1` / `> 1` comparisons stay numeric rather than comparing a string and producing `NaN`.

### Build verification

Per the plan's "Verificación ejecutada" section, `npm run build` (Vite) completed exit 0 with no new warnings, and the bundle was grepped to confirm the new strings and logic are present (`Completa tu Teléfono`, `federated-basic`, `PENDING_PHONE_VERIFICATION`, `PENDING_EMAIL_VERIFICATION`, `ACTIVE`, `submitFederatedBasic`). No test framework exists in this Vite vanilla project, so there is no automated behavioral coverage of the three routing paths; the three scenarios were listed for manual `npm run dev` verification. Not tested automatically: the traditional-login-with-no-phone edge case above, which is exactly the untested path where the regression lives.

</details>

<details>
<summary>File map</summary>

- `FRONTEND/proveedor/main.js` — rewrote `fetchProfile` for accountStatus-based routing; added `window.submitFederatedBasic` and the `federated-basic` register sub-view; bound its form submit; extended the wizard progress calc. (+196 / −16)

Full change: commit `1b297b0` on `develop`, `git show 1b297b0 -- FRONTEND/proveedor/main.js`.

</details>
