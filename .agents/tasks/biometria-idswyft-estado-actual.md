# Estado actual: biometría / activación de proveedor / foto de perfil / S3 / onboarding único

> Investigación READ-ONLY. Base para diseñar la integración con un verificador de identidad externo
> (Idswyft Community, Docker self-hosted) vía puerto hexagonal en `findu-core`.
> Repo raíz: `c:\Users\ADMIN\Documents\FIND-U-PROJECT`. No se modificó código.

---

## 0. Resumen ejecutivo (lee esto primero)

1. **La biometría de HOY NO verifica documento ni rostro en el backend.** El endpoint
   `POST /api/v1/activation/biometric` (micro `findu-spring-security`) recibe solo **metadatos**
   (`cedulaNombre`, `cedulaFechaNacimiento`, `faceMatchScore`, `faceMatched`, `fotoFrontalUrl`).
   El backend únicamente re-valida **nombre (sin tildes) y fecha de nacimiento contra `findu-core`**
   y **confía ciegamente el `faceMatched` que envía el cliente**. No hay documento ni selfie en el
   request; `fotoFrontalUrl` es un **String URL**, no base64 ni binario. No se detecta cara, no se
   compara con documento, no hay prueba de vida en el servidor.

2. **El "face match" del cliente está MOCKEADO con datos hardcodeados.** En la app Flutter
   `findu_proveedores`, el puerto `BiometricVerifier` por defecto resuelve a `MockBiometricVerifier`,
   que devuelve nombre OCR fijo **"Carlos Alberto Mendoza"** y fecha **"1994-11-01"**. La
   implementación real `MlkitTfliteBiometricVerifier` es un **stub que lanza `UnimplementedError`**.

3. **Por eso el registro "rechaza aunque esté mockeado" (bug reportado):** el mock inventa el nombre
   "Carlos Alberto Mendoza"; el backend lo compara contra el nombre real registrado del proveedor en
   `findu-core`. Si no coincide → rechazo `nombre_no_coincide` (HTTP 200, `biometriaOk=false`). La
   biométrica mockeada "pasa" solo si el usuario de prueba se llama exactamente igual a los valores
   hardcodeados del mock.

4. **La app NO deja capturar ni elegir fotos (bug confirmado).** No existe `image_picker` ni `camera`
   en `pubspec.yaml`; no hay `ImagePicker`/`XFile`/`CameraController` en ningún `.dart`. `_pickDocument`
   **fabrica un nombre de archivo ficticio** (`cedula_frente_<timestamp>.jpg`) tras un `Future.delayed`,
   y el mock de rostro genera `captura_rostro_<timestamp>.jpg`. No se abre galería ni cámara.

5. **NO existe almacenamiento S3 real en `findu-core`.** Solo hay un **stub** (`StorageServiceStubAdapter`)
   detrás del puerto `StorageServicePort`, que **devuelve el propio base64 de entrada** (no sube nada).
   El único adaptador S3 real del repo vive en **`findu-help-v2`** (AWS SDK v2 `S3Client`). No existe
   el micro `findu-s3-servicios` que el stub menciona (solo artefactos de build de help-v2).
   `docker-compose.yml` sí levanta **LocalStack con el servicio `s3` habilitado**, pero nadie en core
   lo consume.

6. **El request único de onboarding YA transporta biometría, pero solo como flags/URLs — NO imágenes
   base64.** El flag `USE_ONBOARDING_SINGLE_REQUEST` es **cliente-side** (Flutter, default `false`). El
   DTO `BiometriaData` del gateway lleva `{intentada, aprobada, faceMatchScore, fotoFrontalUrl,
   cedulaNombre, cedulaFechaNacimiento}`. En el flujo actual `fotoFrontalUrl` viaja `null`. **No hay
   ningún campo para documento frente/reverso ni selfie en base64 en todo el pipeline.**

7. **Huecos clave para la feature objetivo (Idswyft):** no existe (a) puerto de verificación de
   identidad en `findu-core`, (b) transporte de imágenes base64 (documento+rostro) en el request único,
   (c) adaptador S3 real en core con la estructura de carpetas `usuarios/proveedores/{TIPODOC+NUM}/...`,
   (d) re-validación biométrica al editar foto de perfil, (e) selección real de imágenes en la app.

---

## 1. Biometría / activación actual en SECURITY (`findu-spring-security`)

### 1.1 Controller — `POST /api/v1/activation/biometric`
`presentation/controller/ProviderActivationController.java`
(`@RequestMapping("/api/v1/activation")`, base-path del servicio `/security-auth`).

- `@PostMapping("/biometric")` → `submitBiometricUseCase.submit(request)`.
- También expone `GET /status`, `POST /approve`, `POST /reject` (soporte).
- Nota en el javadoc: todas las rutas son estáticas porque el resolver de autorización compara
  `operation.path` por igualdad exacta (por eso approve/reject llevan `userId` en el body).

### 1.2 DTO — `BiometricSubmitRequest`
`dto/request/BiometricSubmitRequest.java` (record). Campos y tipos exactos:

| Campo | Tipo | Validación |
|---|---|---|
| `cedulaNombre` | `String` | `@NotBlank`, `@Size(max=200)` |
| `cedulaFechaNacimiento` | `String` | `@NotBlank` (se parsea ISO en el use case) |
| `faceMatchScore` | `BigDecimal` | `@DecimalMin(0.0) @DecimalMax(1.0)` |
| `faceMatched` | `Boolean` | `@NotNull` |
| `fotoFrontalUrl` | `String` | `@NotBlank` (es una **URL**, ejemplo `https://cdn.findu.example/frontal.jpg`) |

**No hay campos de imagen del documento (frente/reverso), ni selfie, ni base64, ni prueba de vida.**

### 1.3 Use case — `SubmitBiometricUseCaseImpl`
`application/usecase/impl/SubmitBiometricUseCaseImpl.java`. Qué valida HOY el backend, en orden:

1. **Forma:** `cedulaFechaNacimiento` ISO parseable y no futura (si no → `400 fecha_malformada`/`fecha_futura`).
2. Resuelve el usuario autenticado (`AuthenticatedUserResolver.resolve()`), obtiene el perfil de core
   (`corePerfilPort.fetchByAuthUserId`); si no existe → `409 perfil_core_requerido`.
3. **Nombre:** `TextNormalizer.equalsIgnoringAccents(request.cedulaNombre(), perfil.nombreCompleto())`.
   Si no coincide → rechazo de negocio `nombre_no_coincide` (HTTP **200**, `biometriaOk=false`).
4. **Fecha:** `perfil.fechaNacimiento().isEqual(fechaRequest)`. Si no → `fecha_no_coincide` (200).
5. **Rostro:** `Boolean.TRUE.equals(request.faceMatched())`. **Solo confía el flag del cliente.**
   Si es false → `rostro_no_coincide` (200).
6. **Éxito:** `insertBiometricCapture(...)` (write-once de la foto frontal), `upsertValidation(BIOMETRIA, OK)`,
   `corePerfilPort.pushProfilePhoto(...)` (best-effort), `activationProgressService.recompute(userId)`
   y `validacionResolvePort.markPassed(userId, "BIOMETRIC_FACIAL", "auto:biometric-passed")` (best-effort).

**Dónde se marca `biometria_ok`:** en `upsertValidation(userId, ValidationType.BIOMETRIA, ValidationState.OK, null)`;
el flag agregado lo recompone `ProviderActivation` al cargar desde las filas vigentes de
`provider_activation_validation`.

### 1.4 Puerto de persistencia — `ProviderActivationRepositoryPort`
`application/port/output/persistence/ProviderActivationRepositoryPort.java`. Métodos relevantes:

- `insertBiometricCapture(userId, cedulaNombre, cedulaFechaNac, faceMatchScore, fotoFrontalUrl, resultado)`
  → inserta en `provider_biometric_capture` y, **write-once**, fija la foto frontal si aún no hay una;
  devuelve la URL de la foto frontal vigente.
- `upsertValidation(userId, tipo, estado, detalle)` → nueva versión `is_current=true` (histórico/versionado),
  devuelve el agregado recargado.
- `loadByUserId`, `updateUserActivationState`, `findPendingSupportDispatch`.

### 1.5 `ActivationProgressService.recompute`
`application/usecase/impl/ActivationProgressServiceImpl.java`. Dueña de los invariantes de orden:
carga el agregado y, si `otp_ok && email_ok && biometria_ok` y `soporte_notified == false`, dispara el
ticket a soporte (help-v2) y avanza a `EN_REVISION_SOPORTE`; idempotente; si help-v2 falla (5xx) deja la
guarda en false y reintenta por scheduler. No contiene lógica biométrica propia (solo orquesta estado).

### 1.6 `CorePerfilPort` / `CorePerfilWebClientAdapter` (cómo security habla con core hoy)
Puerto: `application/port/output/externalapi/CorePerfilPort.java`.
Adapter: `infrastructure/adapter/externalapi/CorePerfilWebClientAdapter.java` (WebClient, vía gateway).

- `record PerfilProveedor(Long id, String nombreCompleto, LocalDate fechaNacimiento, String urlImagenPerfil)`.
- `fetchByAuthUserId(authUserId)` → `GET {base}/findu-core/api/v1/perfil-proveedor/auth/{authUserId}`;
  404 → `Mono.empty()`.
- `pushProfilePhoto(perfilId, nombreCompleto, urlImagenPerfil)` → `PUT {base}/findu-core/api/v1/perfil-proveedor/{id}`
  con body `{nombreCompleto, urlImagenPerfil}`; **best-effort** (se traga errores).
- Config: `findu.activation.core.base-url` (default `http://localhost:8080`) y
  `findu.activation.core.perfil-path` (default `/findu-core/api/v1/perfil-proveedor`).

### 1.7 Tablas (R2DBC, `src/main/resources/schema.sql` y `scripts/migrations/V2__provider_activation.sql`)

`provider_activation_validation`:
```
id BIGINT PK | user_id BIGINT | tipo VARCHAR(20)  -- OTP|EMAIL|BIOMETRIA|SOPORTE
estado VARCHAR(20)  -- PENDIENTE|OK|RECHAZADA | version INT | is_current BOOLEAN
detalle VARCHAR(500) | completed_at | created_at | updated_at | created_by | updated_by
INDEX ix_pav_user_tipo_current (user_id, tipo, is_current)
```
`provider_biometric_capture`:
```
id BIGINT PK | user_id BIGINT | cedula_nombre VARCHAR(200) | cedula_fecha_nac DATE
face_match_score NUMERIC(5,4) CHECK (0..1) | foto_frontal_url VARCHAR(500)
resultado VARCHAR(20)  -- OK|RECHAZADA | created_at
```
> `foto_frontal_url` es un VARCHAR(500): guarda una **URL**, no una imagen. No hay columnas para
> documento frente/reverso ni para selfie/rostro. Entidades R2DBC: `ProviderBiometricCaptureEntity`,
> `ProviderActivationValidationEntity`. Enums: `ValidationType {OTP,EMAIL,BIOMETRIA,SOPORTE}`,
> `ValidationState {PENDIENTE,OK,RECHAZADA}`.

### 1.8 Camino del onboarding único (interno)
`POST /internal/activation/init` recibe `InternalActivationInitRequest`
(`userId, biometriaIntentada, biometriaAprobada, faceMatchScore, fotoFrontalUrl, cedulaNombre, cedulaFechaNacimiento`)
— de nuevo, **solo flags + URL, sin imágenes**.

---

## 2. Foto de perfil

### 2.1 Dónde vive
`findu-core`, tabla `perfil_proveedor`, columna `url_imagen_perfil` (`@Column(name="url_imagen_perfil",
columnDefinition="TEXT")` en
`CORE/findu-core/.../infrastructure/adapter/out/persistence/entity/PerfilProveedorEntity.java`).
El tipo `TEXT` sugiere que puede guardar una URL **o** un base64 largo.

### 2.2 Cómo se setea hoy
- Desde security (biométrica OK): `CorePerfilWebClientAdapter.pushProfilePhoto` → `PUT /api/v1/perfil-proveedor/{id}`
  (`PerfilProveedorController.actualizarPerfil`). Es la foto frontal **write-once** de la biométrica.
- En creación/edición de perfil cliente/proveedor: `GestionPerfilClienteUseCaseImpl` /
  `GestionPerfilProveedorUseCaseImpl` llaman `storageServicePort.uploadFile("perfiles", username, urlImagenPerfil)`
  — pero el adapter es el **stub** (§3), así que el "upload" solo devuelve el mismo string.

### 2.3 Write-once y edición en la app (`provider_profile_screen.dart`)
`findu-movil/apps/findu_proveedores/lib/features/profile/presentation/screens/provider_profile_screen.dart`:
- `_showEditPhotoDialog()`: si `_profile.fotoPerfilFrontalUrl` existe (foto biométrica write-once),
  **bloquea la edición** con snackbar "Tu foto frontal proviene de la validación biométrica y no se puede
  editar." → coherente con la regla write-once del backend.
- Si NO hay foto frontal biométrica: ofrece **avatares predefinidos** + un campo para **pegar una URL**
  (`labelText: 'O pega la URL de tu imagen'`), y persiste con `updateProfilePhoto(proveedorId, newPhotoUrl)`.
- **Huecos:** no abre galería/cámara (igual que el registro) y **no re-dispara validación biométrica**
  al cambiar la foto (la feature objetivo exige que editar la foto de perfil re-valide contra biométrico).

### 2.4 Respuesta de estado
`ActivationStatusResponse` incluye `fotoPerfilFrontalUrl` (y `otpOk/emailOk/biometriaOk/soporteOk`,
`estadoActivacion`, `motivo`). La app lo mapea en `provider_full_profile.dart`.

---

## 3. S3 / Storage

**NO existe un adaptador de storage real en `findu-core`.** Evidencia:

- Puerto: `CORE/findu-core/.../application/port/out/externalapi/StorageServicePort.java`
  — `uploadFile(bucket, fileName, fileBase64)` y `getFileBase64(fileUrl)`. El javadoc dice que el
  contrato apunta a un micro `findu-s3-servicios` (`POST /api/v1/files`, `GET /api/v1/files?url=`).
- Única implementación: `StorageServiceStubAdapter` (`@Component`), que loguea y **devuelve el propio
  `fileBase64` de entrada** (no sube nada; `getFileBase64` devuelve la URL tal cual). Comentario explícito:
  "Se usará mientras el micro findu-s3-servicios no esté desarrollado... reemplazar por un adapter real
  (WebClient, lb://FINDU-S3-SERVICIOS)".
- **El micro `findu-s3-servicios` NO existe** (los únicos matches `*s3*` son directorios `bin/`, `build/`
  de `findu-help-v2`).
- Quién llama al stub: `GestionPerfilClienteUseCaseImpl` y `GestionPerfilProveedorUseCaseImpl`
  (bucket `"perfiles"`, fileName = username).

**Único S3 real del repo:** `CORE/findu-help-v2` (`build.gradle`: `software.amazon.awssdk:s3:2.29.51`):
- `infrastructure/adapter/out/storage/S3FileStorageAdapter.java` (usa `S3Client.putObject`).
- `infrastructure/config/aws/s3/S3Config.java`, `infrastructure/config/properties/S3Properties.java`
  (`@ConfigurationProperties("findu.help.aws.s3")`: `bucket=findu-help-bucket`, `region=us-east-1`,
  `endpoint`, `accessKey`, `secretKey` — soporta `endpointOverride`, compatible con MinIO/LocalStack).
- Objetos de dominio: `S3ObjectReference`, `S3ObjectReferenceDocument`.

**Infra disponible pero no usada por core:** `docker-compose.yml` levanta `localstack` con
`SERVICES: dynamodb,ses,sns,s3`. `findu-spring-security` tiene AWS SDK solo para **DynamoDB**
(revocación de JWT), no S3.

> Conclusión: para la feature objetivo habría que crear un adaptador S3 real **en `findu-core`**
> (o reutilizar el patrón de help-v2) con la estructura de carpetas pedida
> `usuarios/proveedores/{TIPODOCUMENTO+NUMERO}/biometricos/{documento,rostro}` y `.../perfil`.
> Hoy no existe absolutamente nada de eso.

---

## 4. Request único de onboarding

### 4.1 Flag `USE_ONBOARDING_SINGLE_REQUEST`
Es **cliente-side** (Flutter), NO backend:
`findu-movil/apps/findu_proveedores/lib/core/config/environment.dart` →
`Environment.useOnboardingSingleRequest = bool.fromEnvironment('USE_ONBOARDING_SINGLE_REQUEST', defaultValue:false)`.
Con OFF, el wizard conserva el flujo multi-llamada; con ON envía un único
`POST /api/v1/registration/onboarding`. El test `environment_flag_test.dart` fija el default en `false`.

### 4.2 Gateway: controller, saga y DTOs
- `ARCHITECTURE/findu-api-gateway/.../controller/OnboardingController.java`:
  `@PostMapping("/api/v1/registration/onboarding")`; revalida OTP en Redis, idempotencia por
  `Idempotency-Key`, delega en `SagaOrchestratorService.executeSaga`.
- `service/SagaOrchestratorService.java`, pasos:
  - **T1** `POST {security}/internal/users` (crea usuario).
  - **T2** `POST {core}/internal/perfiles` (perfil + `persona` + `direccion`).
  - **T3/T4** cobertura / especialidades (solo PROVEEDOR).
  - **T5** `POST {security}/internal/activation/init` con los **flags de biometría**
    (`biometriaIntentada, biometriaAprobada, faceMatchScore, fotoFrontalUrl, cedulaNombre,
    cedulaFechaNacimiento`).
  - **T6** `POST {security}/internal/users/{id}/tokens`.
  - Post-éxito best-effort: `POST {validacion}/api/v1/validations/complete-process` con evidencia
    `BIOMETRIC_FACIAL { faceMatchScore, fotoFrontalUrl, cedulaNombre, cedulaFechaNacimiento }`.
  - Compensaciones C1/C2 (DELETE user / DELETE perfil en cascada) con reintentos + outbox.
- DTOs: `OnboardingRequest` {flowType, isFederated, federated, security, persona, direccion, cobertura,
  especialidades, **biometria**, idempotencyKey}; `SecurityData`, `PersonaData`, `BiometriaData`.

### 4.3 ¿Transporta imágenes base64 hoy?
**No.** `BiometriaData` = `{Boolean intentada, Boolean aprobada, Double faceMatchScore, String
fotoFrontalUrl, String cedulaNombre, String cedulaFechaNacimiento}`. Todo son flags/strings; en el
flujo actual `fotoFrontalUrl` viaja `null` (ver `PREP_ONBOARDING_REQUEST_UNICO.md` §3 y
`DISENO_ONBOARDING_SAGA_GATEWAY.md` §3). No hay campos para documento frente/reverso ni selfie.

### 4.4 Dónde encajaría el envío de documento+rostro (hueco)
El request único es el punto natural para recibir las imágenes base64 (documento frente/reverso +
selfie + liveness). Hoy **no existe** ese transporte: habría que (a) ampliar `BiometriaData`/
`InternalActivationInitRequest` o introducir un nuevo paso/puerto, (b) decidir que `findu-core`
reciba las imágenes (es el dueño de S3 y del perfil) y exponga el **puerto de verificación de
identidad** que llame a Idswyft, (c) devolver `verified|failed|manual_review` y, si `verified`,
subir a S3 y fijar la foto de perfil.

---

## 5. App Flutter (`findu-movil/apps/findu_proveedores`)

### 5.1 Puerto y adaptadores biométricos
- Puerto: `features/auth_onboarding/domain/ports/biometric_verifier.dart` — `BiometricVerifier` con
  `readCedula(frontImagePath)`, `captureAndDetectFace()`, `matchFaces(cedulaFacePath, liveFacePath)`;
  resultados `CedulaOcrResult`, `FaceCaptureResult`, `FaceMatchResult`.
- `MockBiometricVerifier` (`infrastructure/adapters/mock_biometric_verifier.dart`): **CONFIRMADO**
  datos hardcodeados `ocrNombre='Carlos Alberto Mendoza'`, `ocrFechaNacimiento='1994-11-01'`,
  `faceMatchScore=0.97`, `livenessScore=0.98`, todos los `*Succeeds/Matches` en `true`. No abre
  cámara: `captureAndDetectFace` devuelve una ruta ficticia `captura_rostro_<timestamp>.jpg`.
- `MlkitTfliteBiometricVerifier` (`.../mlkit_tflite_biometric_verifier.dart`): **CONFIRMADO stub** —
  los tres métodos lanzan `UnimplementedError(_notWired)`. Documenta el cableado real (google_mlkit_*,
  tflite_flutter, asset MobileFaceNet) pero no lo implementa.
- `BiometricVerifierFactory.resolve()` elige por `Environment.biometricMode` (`BIOMETRIC_MODE`,
  default `mock`); `isBiometricReal` solo si vale `real`.

### 5.2 Pantalla de registro (`register_wizard_screen.dart`)
- Estado: `_cedulaFrentePath`, `_cedulaReversoPath`, `_profilePhotoPath`, `_biometricPhotoPath`.
- `_runBiometric...` (≈líneas 1489-1560): `captureAndDetectFace()` → `readCedula(cedulaPath)` →
  `matchFaces(...)` → publica `{cedulaNombre, cedulaFechaNacimiento, faceMatched, faceMatchScore,
  fotoFrontalUrl}` vía `ActivationApiAdapter`. `fotoFrontalUrl` = la **ruta local ficticia**, no una URL
  real ni base64.
- **BUG de captura confirmado:** `_pickDocument({isFront})` (≈líneas 1329-1357) hace
  `await Future.delayed(400ms)` y asigna `docPath = 'cedula_${side}_${timestamp}.jpg'`. No hay galería
  ni cámara: solo genera un string. Los botones "Cédula Frente/Reverso" llaman a este método.

### 5.3 Pantalla standalone del CTA (`biometric_verification_screen.dart`)
`features/auth_onboarding/presentation/screens/biometric_verification_screen.dart`. Reutiliza el mismo
`BiometricVerifier` (mock por defecto) y el mismo flujo captura→OCR→match→`ActivationApiAdapter`. Se
navega desde las alertas del feed (`market_feed_screen.dart`, CTA "Completa tu validación biométrica"
para `PERFIL_BIOMETRIA_PENDIENTE`/`BIOMETRIC_FACIAL`).
- **Explica "no hace nada / no se parece al registro":** corre el mock (rutas ficticias, sin pedir foto
  de documento frente/reverso ni foto de cara real); como el nombre mock no coincide con el del perfil,
  el backend devuelve `biometriaOk=false` y la alerta sigue visible.

### 5.4 `image_picker` / `camera`
**CONFIRMADO ausentes.** `grep` de `image_picker|camera|ImagePicker|XFile|CameraController|pickImage`
en todo `apps/findu_proveedores`: **0 matches**. `pubspec.yaml` no los declara. Las "rutas de imagen"
de hoy son strings fabricados por el mock, no archivos reales del dispositivo.

---

## 6. Documentos de diseño existentes (resumen)

> Contenido parafraseado para cumplir con restricciones de licencia.

- **`findu/docs/verificacion-identidad-proveedor.md` (FEAT-001):** cubre validación de **teléfono (OTP)**
  y **correo (enlace mágico)**, estados `PENDING_PHONE_VERIFICATION → PENDING_EMAIL_VERIFICATION →
  DOCUMENTS_PENDING → ACTIVE`. **No** describe biometría de documento/rostro ni S3; su "DOCUMENTS_PENDING"
  marca el punto fuera de ese FEAT donde entraría la verificación de identidad.
- **`DOCUMENTACION_Y_DIAGRAMAS/DISENO_MICRO_FINDU_VALIDACION.md`:** define `BIOMETRIC_FACIAL` como
  validación **PASSIVE / CLIENT_ACTION** con `evidence: { faceMatchScore, docType }` y alerta
  `PERFIL_BIOMETRIA_PENDIENTE`. La "evidencia" es un score, **no** imágenes; confirma que hoy la
  biometría es pasiva y no bloqueante.
- **`DOCUMENTACION_Y_DIAGRAMAS/DISENO_ONBOARDING_SAGA_GATEWAY.md`:** diseño del agregador y la saga
  (T1..T6, compensaciones C1/C2, idempotencia, rate-limiting). §2.4 explicita que la biometría
  **"no bloquea"** y que el resultado viaja como **flags dentro del payload consolidado**. El payload
  `biometria{}` lleva `fotoFrontalUrl` (nullable), nunca base64. Lista `biometrics/verify` como endpoint
  "que falta" ligado al `activation/biometric` existente.
- **`findu-movil/baseDesing/PREP_ONBOARDING_REQUEST_UNICO.md`:** plan móvil del request único detrás del
  flag `USE_ONBOARDING_SINGLE_REQUEST`; §3 fija el payload (biometría = flags), §4.5/§1 dicen que la
  biometría ya existente (mock) solo **alimenta flags**, no sube imágenes.
- **`findu-movil/baseDesing/PROMPT_Y_RESUMEN_PROVEEDOR.md`:** prompt/resumen de contexto del proveedor
  (apoya la preparación del request único). No define integración de verificación de identidad externa.

**Ningún documento existente menciona Idswyft, un verificador externo real, ni la estructura S3
`usuarios/proveedores/{TIPODOC+NUM}/...`.** Es terreno nuevo.

---

## 7. Huecos / mockeado / inexistente (checklist para el diseño)

| # | Área | Estado actual | Hueco para la feature objetivo |
|---|---|---|---|
| 1 | Verificación real de documento+rostro+liveness | **Mockeada en cliente; backend solo confía `faceMatched` y compara nombre/fecha** | No existe puerto de verificación de identidad en core; hay que crearlo y que llame a Idswyft |
| 2 | OCR/face match real en app | `MlkitTfliteBiometricVerifier` = **stub `UnimplementedError`** | Decidir si la verificación real se mueve al backend (Idswyft) y el cliente solo captura/envía imágenes |
| 3 | Captura de imágenes en app | **Sin `image_picker`/`camera`; rutas ficticias** | Agregar captura/selección real (documento frente/reverso, selfie) y envío base64 |
| 4 | Transporte de imágenes base64 | **No existe** en `BiometricSubmitRequest`, `BiometriaData`, `InternalActivationInitRequest` | Ampliar el request único / endpoint para llevar documento+rostro base64 |
| 5 | Storage S3 en core | **Solo stub** (`StorageServiceStubAdapter`); micro `findu-s3-servicios` inexistente | Adaptador S3 real en core + estructura de carpetas pedida; LocalStack ya está disponible |
| 6 | Estructura de carpetas S3 | No existe | `usuarios/proveedores/{TIPODOCUMENTO+NUMERO}/biometricos/{documento,rostro}` y `.../perfil` |
| 7 | Foto de perfil = rostro frontal biométrico | Write-once respetado (backend + app) | Falta: al **editar** la foto de perfil, re-disparar validación biométrica (hoy solo avatares/URL) |
| 8 | Bug "rechaza aunque mockeado" | Mock inventa "Carlos Alberto Mendoza" ≠ nombre real → `nombre_no_coincide` | Al integrar Idswyft, el veredicto debe venir del verificador, no de comparar el nombre mock |

---

## 8. Recomendaciones (para el documento de diseño — NO implementadas aquí)

1. **Crear el puerto hexagonal de verificación de identidad en `findu-core`** (p.ej.
   `IdentityVerificationPort` con `verify(documento, selfie, liveness) → {verified|failed|manual_review,
   score}`), con un `IdswyftIdentityVerificationAdapter` (API v2 multipart: `initialize` →
   `front-document` / `back-document` / `live-capture` → `GET status`, auth `X-API-Key`). El dominio no
   debe conocer Idswyft; a futuro se sustituye por Lambda/tercero. Esto encaja con el patrón ya usado
   (`StorageServicePort` + stub) y con la arquitectura hexagonal del repo.
2. **Hacer que las imágenes base64 lleguen a `findu-core`** (dueño de S3 y del perfil) a través del
   request único del gateway. Decidir si `findu-spring-security` sigue siendo quien marca
   `biometria_ok` (recibiendo el veredicto de core) o si core resuelve y notifica a security/validación.
3. **Implementar un adaptador S3 real en core** reutilizando el patrón de `findu-help-v2`
   (`S3Client` + `endpointOverride` para LocalStack/MinIO), con los buckets/carpetas pedidos. Reemplaza
   el `StorageServiceStubAdapter`.
4. **Reemplazar la comparación de nombre mock por el veredicto de Idswyft**; conservar la re-validación
   nombre/fecha contra core como control secundario si se desea, pero el `verified` debe mandar.
5. **Añadir captura real de imágenes en la app** (`image_picker`/`camera`) para documento frente/reverso
   y selfie, y enviarlas base64 en el request único; mantener el `BiometricVerifier` como puerto, pero su
   rol real pasa a ser "capturar imágenes" (la verificación se hace en backend con Idswyft).
6. **Editar foto de perfil debe re-disparar validación biométrica** (comparar la nueva foto con el
   biométrico) antes de persistir; hoy el diálogo de perfil solo acepta avatares/URL sin re-validar.

---

### Apéndice: archivos clave citados (rutas absolutas)

- `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\SECURITY\findu-spring-security\src\main\java\com\findu\security\presentation\controller\ProviderActivationController.java`
- `...\findu-spring-security\src\main\java\com\findu\security\application\usecase\impl\SubmitBiometricUseCaseImpl.java`
- `...\findu-spring-security\src\main\java\com\findu\security\dto\request\BiometricSubmitRequest.java`
- `...\findu-spring-security\src\main\java\com\findu\security\application\port\output\persistence\ProviderActivationRepositoryPort.java`
- `...\findu-spring-security\src\main\java\com\findu\security\application\usecase\impl\ActivationProgressServiceImpl.java`
- `...\findu-spring-security\src\main\java\com\findu\security\application\port\output\externalapi\CorePerfilPort.java`
- `...\findu-spring-security\src\main\java\com\findu\security\infrastructure\adapter\externalapi\CorePerfilWebClientAdapter.java`
- `...\findu-spring-security\src\main\java\com\findu\security\dto\internal\InternalActivationInitRequest.java`
- `...\findu-spring-security\src\main\java\com\findu\security\dto\response\ActivationStatusResponse.java`
- `...\findu-spring-security\src\main\resources\schema.sql` (+ `scripts\migrations\V2__provider_activation.sql`)
- `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\CORE\findu-core\src\main\java\com\findu\core\application\port\out\externalapi\StorageServicePort.java`
- `...\findu-core\src\main\java\com\findu\core\infrastructure\adapter\out\externalapi\StorageServiceStubAdapter.java`
- `...\findu-core\src\main\java\com\findu\core\infrastructure\adapter\out\persistence\entity\PerfilProveedorEntity.java`
- `...\findu-core\src\main\java\com\findu\core\infrastructure\adapter\input\web\controller\PerfilProveedorController.java`
- `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\CORE\findu-help-v2\src\main\java\com\findu\help\v2\infrastructure\adapter\out\storage\S3FileStorageAdapter.java` (+ `...\config\properties\S3Properties.java`)
- `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\ARCHITECTURE\findu-api-gateway\src\main\java\com\findu\gateway\controller\OnboardingController.java`
- `...\findu-api-gateway\src\main\java\com\findu\gateway\service\SagaOrchestratorService.java`
- `...\findu-api-gateway\src\main\java\com\findu\gateway\dto\{OnboardingRequest,BiometriaData,SecurityData,PersonaData}.java`
- `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu-movil\apps\findu_proveedores\lib\features\auth_onboarding\domain\ports\biometric_verifier.dart`
- `...\auth_onboarding\infrastructure\adapters\{mock_biometric_verifier,mlkit_tflite_biometric_verifier,biometric_verifier_factory}.dart`
- `...\auth_onboarding\presentation\screens\{register_wizard_screen,biometric_verification_screen}.dart`
- `...\lib\core\config\environment.dart`
- `...\features\profile\presentation\screens\provider_profile_screen.dart`
- `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\docker-compose.yml` (LocalStack `s3` habilitado)
- Docs: `findu\docs\verificacion-identidad-proveedor.md`, `DOCUMENTACION_Y_DIAGRAMAS\DISENO_MICRO_FINDU_VALIDACION.md`, `DOCUMENTACION_Y_DIAGRAMAS\DISENO_ONBOARDING_SAGA_GATEWAY.md`, `findu-movil\baseDesing\PREP_ONBOARDING_REQUEST_UNICO.md`, `findu-movil\baseDesing\PROMPT_Y_RESUMEN_PROVEEDOR.md`

> Nota: contenido de documentos de terceros/diseño fue parafraseado para cumplir restricciones de licencia.
