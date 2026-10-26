# Implementation Plan — Alinear flujo "Iniciar sesión con Google" del PROVEEDOR con el patrón del CLIENTE

## Alcance y contexto (hallazgos de exploración)

- **Único archivo a modificar:** `findu/FRONTEND/proveedor/main.js`. `index.html` y `style.css` del mismo directorio solo si hiciera falta algún estilo (no se prevé: los estilos de wizard, OTP y banner ya existen).
- **Referencia (solo lectura):** `findu/FRONTEND/cliente/main.js`.
- **Build/verificación:** proyecto Vite vanilla, sin tests. Build real: `npm run build` dentro de `findu/FRONTEND/proveedor` (verificado: `vite build` termina con exit 0). No hay framework de pruebas; la verificación es `npm run build` limpio + revisión manual del flujo con `npm run dev` (puerto 3001).
- **Constantes:** `ROLE_NAME = 'ROLE_OUR_PROVEEDOR'` (ya definido en la línea 6).

### Correcciones a la descripción de la tarea confirmadas leyendo el código

1. El cuerpo de `/auth/federated` que YA usa el proveedor (y el cliente) manda `role: ROLE_NAME`, **no** `roleName`. Mantener `role` (es el contrato que funciona hoy). No cambiarlo.
2. La ruta de actualización del cliente en `handleRegister` es `PUT ${API_GATEWAY_URL}/security-auth/api/v1/profile` con body `{ username, phone, codPhoneInternational }`. Esa es la ruta a replicar en el proveedor.
3. `window.handleGoogleCredentialResponse` del proveedor ya guarda `state.googleData = { email, firstName, lastName, sub }`, llama a `/auth/federated` y delega la navegación a `fetchProfile()`. **No necesita cambios** salvo lo indicado abajo (opcional). El problema NO está en el handler de Google; está en `fetchProfile`, que salta directo al OTP sin pantalla de datos básicos.

### Reutilizar sin reimplementar (ya existe en `proveedor/main.js`)

- Sub-vista OTP con dos estados por `state.otpSent` (ESTADO 1 "pedir celular" / ESTADO 2 "ingresar código"): render en el bloque `state.registerStep === 'otp'` (~línea 2294).
- Timers OTP: `requestPhoneOtp`, `startOtpCooldown` (60s), `startOtpValidity` (`OTP_VALIDITY_SECONDS = 300` = 5 min), `formatMMSS`, `updateOtpCooldownUI`, `updateOtpValidityUI`.
- Handlers OTP globales: `window.submitOtpPhone`, `window.resendPhoneOtp`, `window.changeOtpPhone`, `window.verifyPhoneOtp`.
- Banner de correo asíncrono: `buildEmailVerificationBannerHtml()` + `window.resendEmailVerification` + `startEmailCooldown`. Ya se pinta en la vista `profile` (~línea 2988).
- Wizard: `registerStep` admite `1`, `'otp'`, `2`, `3`; barra de progreso en ~línea 2546.
- `verifyPhoneOtp` en contexto `'federated'` ya hace `await fetchProfile()` tras verificar, lo que re-enruta según el perfil actualizado. Aprovechar esto: tras verificar OTP, `fetchProfile` decidirá panel/wizard.

### Decisión de diseño (registro del porqué)

- **Nuevo valor de estado `state.registerStep = 'federated-basic'`** para la pantalla de datos básicos del flujo Google, en lugar de reutilizar el Paso 1 tradicional. Razón: el Paso 1 del proveedor (`handleRegisterStep1`) crea la cuenta vía `POST /customers` + auto-login; en el flujo federado la cuenta YA existe (hay `state.token`), así que necesitamos un submit distinto que haga `PUT /profile` (como el cliente), sin tocar `handleRegisterStep1`. Mantener ambos caminos separados evita romper el registro tradicional.
- **Enrutado por `accountStatus`** en `fetchProfile` (los 3 caminos). Se usa `data.accountStatus` como fuente de verdad; `data.phone`/`data.phoneVerified` se usan solo para el caso borde descrito.

---

## Pasos

- [ ] 1. Añadir el handler de submit del flujo federado `window.submitFederatedBasic` (datos básicos → guardar teléfono → avanzar a OTP).
      Crear una función global nueva (junto a los demás `window.*`, p.ej. tras `window.submitOtpPhone`). Lee `#fed-phone`, `#fed-phone-code` (fijo `+57`), `#fed-birthdate`; valida teléfono (`/^\d{7,15}$/`) y mayoría de edad (≥18, misma lógica que `handleRegister` del cliente). Guarda en estado: `state.regPhone`, `state.regPhoneCode`, `state.fechaNacimiento`, `state.nombreCompleto` (de `state.googleData`), `state.celular`, `state.codPhoneInternational`. Hace el **PUT de actualización copiado del cliente**: `fetch(`${API_GATEWAY_URL}/security-auth/api/v1/profile`, { method:'PUT', headers:{'Content-Type':'application/json','Authorization':`Bearer ${state.token}`}, body: JSON.stringify({ username: state.regUsername || state.profile?.username, phone: state.regPhone, codPhoneInternational: state.regPhoneCode }) })`. Manejar `503/500` con el mismo mensaje de reintento que el cliente. En éxito: fijar `state.otpContext = 'federated'`, `state.otpSent = false`, `state.otpCode = ''`, `state.registerStep = 'otp'`, `render()` y mostrar aviso "Teléfono guardado. Verifica tu celular con el código que te enviamos." (El envío del código se dispara en la pantalla OTP con el botón "Enviar código" existente — no auto-enviar aquí, para respetar la decisión "no auto-enviar OTP sin teléfono confirmado en pantalla".)
      Files: `findu/FRONTEND/proveedor/main.js`
      Verify: `npm run build` en `findu/FRONTEND/proveedor` termina con exit 0 (sin errores de sintaxis). Confirmar que la función aparece registrada en `window` revisando que el build transforma el módulo sin warnings nuevos.

- [ ] 2. Renderizar la pantalla "Paso 1: Completa tu Teléfono" para el estado `federated-basic` dentro del bloque de la vista `register`.
      En la cadena `if (state.registerStep === 1) ... else if (state.registerStep === 'otp') ...`, añadir una rama `else if (state.registerStep === 'federated-basic')`. Copiar el patrón del cliente (`cliente/main.js`, bloque `currentView === 'register'` con `isGoogle`): encabezado `<h2>Paso 1: Completa tu Teléfono</h2>`; campos **disabled** Nombre de Google (`${state.googleData?.firstName} ${state.googleData?.lastName}`), Correo (`state.googleData?.email || state.profile?.email`), Nombre de Usuario (derivado del email: `email.split('@')[0]`); campo Teléfono = `<select id="fed-phone-code">` con `+57 (Colombia)` + `<input id="fed-phone">`; campo `<input type="date" id="fed-birthdate">`. Botón `<button type="submit">Siguiente ➔</button>` dentro de `<form id="federated-basic-form">`. **No** mostrar botón de Google ni campo password (igual que el cliente con `isGoogle`). Reutilizar las clases CSS existentes (`form-group`, `form-row`, `btn`) para no tocar `style.css`.
      Files: `findu/FRONTEND/proveedor/main.js`
      Verify: `npm run build` exit 0. Manual (`npm run dev`): tras Google con usuario nuevo se ve la pantalla de datos básicos con nombre/correo/usuario deshabilitados y los campos teléfono + fecha editables.

- [ ] 3. Enlazar el submit del formulario `federated-basic` en el bloque de binding de eventos de la vista `register`.
      En la cadena de `addEventListener` (~línea 2585, donde se enlazan `step1-form`, `otp-form`, `step2-form`, `step3-form`), añadir rama `else if (state.registerStep === 'federated-basic') { document.getElementById('federated-basic-form')?.addEventListener('submit', window.submitFederatedBasic); }`. No llamar a `initGoogleSignIn()` en este estado (no hay botón Google).
      Files: `findu/FRONTEND/proveedor/main.js`
      Verify: `npm run build` exit 0. Manual: pulsar "Siguiente ➔" en datos básicos dispara el PUT y navega a la pantalla OTP (ESTADO 2, "ingresar código") sin recargar.

- [ ] 4. Reescribir el enrutado de `fetchProfile` para los TRES caminos por `accountStatus` (reemplaza el salto directo a OTP de la línea ~258).
      Sustituir el bloque actual `if (data.phoneVerified === false) { ... registerStep='otp' ... }` por una decisión basada en `data.accountStatus`, tras cargar catálogos y `fetchProveedorPerfilCore`:
      - Prellenar siempre `state.regUsername`/`state.regEmail`/`state.nombreCompleto` desde `data`/`state.googleData` (como ya hace el cliente).
      - **Camino 1 — nuevo / sin teléfono:** si `!data.phone` (y por tanto datos básicos sin completar) → `state.registerStep = 'federated-basic'`, `setView('register')`, aviso "Autenticado con Google. Por favor, completa tu teléfono.". (Equivale al `if (!data.phone) setView('register')` del cliente.)
      - **Camino 2 — ACTIVE:** si `data.accountStatus === 'ACTIVE'` → si `state.proveedorPerfil?.id` existe `setView('profile')`; si no, enviar al wizard en el paso que falte (ver abajo).
      - **Camino 3 — incompleto por `accountStatus`:**
        - `'PENDING_PHONE_VERIFICATION'`: si YA hay `data.phone` → ir a OTP (`otpContext='federated'`, `otpSent=false` para el caso en que haya que (re)enviar código, conservando el botón "Enviar código"; si se prefiere auto-mostrar ESTADO 2 porque el teléfono ya está guardado, usar `otpSent=false` para no auto-enviar), `registerStep='otp'`, `setView('register')`. Si NO hay `data.phone` (caso borde) → `registerStep='federated-basic'` primero.
        - `'PENDING_EMAIL_VERIFICATION'`: `setView('profile')` (el banner `buildEmailVerificationBannerHtml()` ya aparece porque `state.profile.emailVerified === false`); no bloquear.
        - Falta perfil del wizard (`!state.proveedorPerfil?.id`, o estados tipo `DOCUMENTS_PENDING` / faltan Paso 2 o 3) → fijar `state.registerStep = 2` (o `3` si el perfil existe pero faltan servicios/credenciales) y `setView('register')`.
      - Mantener los reintentos 5xx/red y el manejo de `401 → logout()` existentes.
      Decisión registrada: se usa `accountStatus` como discriminador principal (según "Decisiones ya tomadas"); `data.phone` solo desambigua el caso borde de `PENDING_PHONE_VERIFICATION` sin teléfono.
      Files: `findu/FRONTEND/proveedor/main.js`
      Verify: `npm run build` exit 0. Manual con los 3 escenarios: (a) usuario Google nuevo → datos básicos; (b) usuario ACTIVE → panel/perfil directo; (c) usuario con `PENDING_EMAIL_VERIFICATION` → panel con banner "Confirma tu correo electrónico".

- [ ] 5. Asegurar la cadena datos básicos → OTP → re-enrutado, verificando que `verifyPhoneOtp` (contexto federado) cierra el ciclo.
      `window.verifyPhoneOtp` ya hace `await fetchProfile()` cuando `otpContext === 'federated'`. Confirmar que tras el PUT del paso 1 y la verificación OTP, `fetchProfile` (reescrito en el paso 4) enruta a panel/wizard según corresponda (el backend habrá dejado `accountStatus` en `PENDING_EMAIL_VERIFICATION` o similar y teléfono ya verificado). No reimplementar OTP ni timers; solo validar el encadenamiento. Si `verifyPhoneOtp` necesitara limpiar `state.registerStep`, dejar que `fetchProfile` lo fije.
      Files: `findu/FRONTEND/proveedor/main.js` (revisión; cambios mínimos si el encadenamiento lo requiere)
      Verify: `npm run build` exit 0. Manual: completar datos básicos → recibir y escribir OTP → "Verificar celular" lleva al panel (o al Paso 2 del wizard si no hay perfil de proveedor), con el banner de correo visible si `emailVerified === false`.

- [ ] 6. Ajustar la barra de progreso del wizard para incluir el estado `federated-basic`.
      En el IIFE de `wizard-progress` (~línea 2546) se calcula `stepNum = state.registerStep === 'otp' ? 1.5 : state.registerStep`. Añadir que `'federated-basic'` se trate como paso 1 (`stepNum = 1`) para que la barra marque "Seguridad" activo sin romper el cálculo numérico (evitar `NaN` al comparar strings). Ej.: `const stepNum = state.registerStep === 'federated-basic' ? 1 : state.registerStep === 'otp' ? 1.5 : state.registerStep;`.
      Files: `findu/FRONTEND/proveedor/main.js`
      Verify: `npm run build` exit 0. Manual: en la pantalla de datos básicos la barra muestra el nodo 1 ("Seguridad") activo y no rompe el render.

- [ ] 7. Verificación final del build y limpieza.
      Ejecutar el build completo y revisar que no haya artefactos temporales. Borrar `dist/` si se generó solo para verificar (opcional; no se commitea).
      Files: ninguno (solo verificación)
      Verify: `npm run build` en `findu/FRONTEND/proveedor` termina con exit 0 y sin warnings nuevos. Revisión manual del flujo Google completo (nuevo, ACTIVE, incompleto) en `npm run dev`.

---

## Commit

Trabajar directamente en la rama `develop` (NO crear worktree ni rama nueva). Commit único de frontend, p.ej.:
`feat(proveedor): alinear flujo federado Google con datos básicos previos al OTP (como cliente)`
Stagear solo `findu/FRONTEND/proveedor/main.js` (y `style.css`/`index.html` si se tocaron). No hacer push salvo que se pida.

## Verificación ejecutada (iteración 1)

Comandos corridos en `findu/FRONTEND/proveedor` (PowerShell):

1. `node_modules` ya existía (no se requirió `npm install`).
2. `npm run build` → **exit 0**. Salida de Vite (`vite v8.1.5`): `✓ 5 modules transformed`, `✓ built in ~123ms`; artefactos `dist/index.html`, `dist/assets/index-*.css`, `dist/assets/index-*.js` (94.86 kB). Sin errores ni warnings nuevos.
3. Verificación de bundle (`dist/assets/*.js`): contiene todos los textos/lógica nuevos → `Completa tu Teléfono`=True, `federated-basic`=True, `PENDING_PHONE_VERIFICATION`=True, `PENDING_EMAIL_VERIFICATION`=True, `ACTIVE`=True, `submitFederatedBasic`=True, `Siguiente`=True.
4. NO se ejecutó `docker compose` ni ningún despliegue.

No se rompió el flujo tradicional (no-Google): `handleRegisterStep1` (wizard 3 pasos) y `handleLogin` intactos; `go-register` sigue entrando por `state.registerStep = 1`. El nuevo estado `federated-basic` solo se alcanza vía `fetchProfile` tras login Google cuando `!data.phone`.

## Gaps / supuestos

- `accountStatus` y sus valores (`ACTIVE`, `PENDING_PHONE_VERIFICATION`, `PENDING_EMAIL_VERIFICATION`, `DOCUMENTS_PENDING`) provienen del contrato del backend (fix en paralelo). Se asume que `GET /security-auth/api/v1/profile` los devuelve. Si algún valor no llega, el fallback es: sin `phone` → datos básicos; con `phone` + perfil de proveedor → panel; con `phone` sin perfil → wizard Paso 2.
- Se asume que el PUT `/profile` acepta `{ username, phone, codPhoneInternational }` para el proveedor igual que para el cliente (misma ruta de seguridad, rol distinto). Si el proveedor requiriera un endpoint distinto, ajustar la URL manteniendo el mismo cuerpo.
