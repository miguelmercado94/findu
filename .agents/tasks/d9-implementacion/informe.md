# Informe D9 fases 0-2: autorizacion por propietario en findu-core (A1-lite)

Estado final: findu-core corre en modo LOG (compose por defecto). Modo por defecto del codigo: OFF.

## 1. Endpoints instrumentados

| Endpoint | Ambito | Requisito |
|---|---|---|
| GET /api/v1/perfil-proveedor/{id} | perfil | JWT con userId = perfil_proveedor.auth_user_id (BD) |
| PUT /api/v1/perfil-proveedor/{id} | perfil | idem |
| PATCH /api/v1/perfil-proveedor/{id}/estado | perfil | idem |
| GET /api/v1/perfil-proveedor/{id}/cobertura | perfil | idem |
| PUT /api/v1/perfil-proveedor/{id}/cobertura | perfil | idem |
| PATCH /api/v1/perfil-proveedor/{id}/disponibilidad | perfil | idem |
| GET /api/v1/perfil-proveedor/auth/{authUserId} | perfil | claim userId = authUserId (sin BD) |
| GET /api/v1/perfil-proveedor/usuario/{authUserId} | perfil | idem |
| PUT /api/v1/perfil-proveedor/{id}/foto-perfil (json y multipart) | archivos | propietario del perfil {id} |

Sin guard (publicos o internos): GET {id}/foto-perfil, GET {id}/publico, catalogos (categorias, subcategorias, servicios, direcciones/municipios, catalogs/specialties), check-document, /actuator, /v3/api-docs, /swagger-ui, /api/v1/health, /internal/** (siguen con X-Internal-Service-Key). Un request con X-Internal-Service-Key valida pasa el guard.
Semantica: el guard corre ANTES de buscar el recurso; perfil inexistente con token valido da el 404 normal. ENFORCE: 401 sin token/invalido/refresh/expirado, 403 otro proveedor. GlobalExceptionHandler de core ahora mapea ResponseStatusException a su status (antes caia en 500).

## 2. Propiedades / env nuevas

| Propiedad | Env | Default codigo | Compose |
|---|---|---|---|
| findu.security.authz.mode | FINDU_AUTHZ_MODE | OFF | LOG |
| findu.security.authz.enforce-scopes | FINDU_AUTHZ_ENFORCE_SCOPES | vacio (= todos los instrumentados) | no definida |
| findu.security.authz.jwt-secret | JWT_SECRET | vacio | mismo base64 que findu-spring-security |
| findu.security.authz.internal-service-key | FINDU_INTERNAL_SERVICE_KEY | findu-internal-saga-secret-key-2026 | igual (parametrizado) |
| findu.internal.service-key (security) | FINDU_INTERNAL_SERVICE_KEY | mismo default | via default |

Dependencias nuevas en core: jjwt 0.12.6 (mismas versiones que security). Sin spring-security starter. Con modo != OFF y secreto vacio o default del repo se emite un WARN al arrancar (no falla).
Security: CorePerfilWebClientAdapter envia X-Internal-Service-Key en fetchByAuthUserId y pushProfilePhoto.

## 3. Matriz e2e (scripts/authz-e2e.ps1, token forjado HS256, userId propietario=2, perfil id=1)

| Caso | Descripcion | ENFORCE | LOG |
|---|---|---|---|
| a1-a3 | propietario GET perfil, auth/{id}, cobertura | 200 | 200 |
| b1-b2 | otro userId GET perfil, auth | 403 | 200 |
| b3 | otro userId PUT foto-perfil | 403 | 422 (pasa el guard, falla el body vacio) |
| c1-c2 | sin token | 401 | 200 |
| d1 | refresh (typ=JWTRefresh) | 401 | 200 |
| e1 | expirado | 401 | 200 |
| f1 | GET foto-perfil sin token | 404 (sin foto, publico) | 404 |
| g1-g4 | publico, categorias, servicios, actuator sin token | 200 | 200 |
| h1 | /internal sin llave | 403 | 403 |
| h2 | /internal con llave | pasa | pasa |
| h3 | llave interna en endpoint de perfil sin JWT | 200 | 200 |
| i1 | AUTHZ_VIOLATION en docker logs (7 nuevas: b1-b3, c1, c2, d1, e1) | OK | OK |

Resultado: ENFORCE 19/19 PASS, LOG 19/19 PASS. ENFORCE se ejecuto con override temporal FINDU_AUTHZ_MODE y luego se recreo solo findu-core en LOG.
Ajustes al script durante la verificacion: cuerpo por stdin (PowerShell 5 pierde comillas), /publico requiere ?servicioId=1, catalogo de comprobacion = /api/v1/servicios.
Nota: GET /api/v1/catalogs/specialties sin parametros responde 500 (EL1021E pageNumber) en core, error propio del endpoint, no del authz. Fuera de alcance, queda en PENDIENTES.

## 4. Verificacion viva en LOG (decision 11)

- GET http://localhost:8080/findu-core/api/v1/perfil-proveedor/auth/2 por el gateway: 200 (registra AUTHZ_VIOLATION NO_TOKEN, esperado en LOG).
- POST /internal/perfiles/{id}/cobertura con llave: 200 (sin llave 403, como antes).
- Security -> core con llave: cubierto por CorePerfilWebClientAdapterTest (cabecera en fetchByAuthUserId y pushProfilePhoto) y el contenedor security reconstruido con el jar nuevo. NO se disparo una activacion biometrica real (requiere dispositivo/usuario); queda por validar en el celular. Ademas, /internal siempre exento y la llave interna hace pasar el guard, asi que security no puede romperse por el guard.
- Registro por gateway: no se creo un perfil real; se verifico solo POST /internal/perfiles con llave (mismo comportamiento que antes).

## 5. Compile / test

- gradlew clean bootJar -x test: core OK, security OK.
- findu-core test: 145 tests, 0 fallos.
- findu-spring-security test: 161 tests, 1 fallo = SubmitBiometricUseCaseImplTest.submit_perfilMissing_409 (baseline ajeno, no tocado), 1 skipped.
- docker compose up -d --build findu-core findu-spring-security: ambos healthy.

## 6. Commits y push

Commits de la feature (sobre origin/develop a000b6a):
- 095b193 feat(core): infra de autorizacion (JwtVerifier, filtro, ProviderOwnershipGuard) OFF/LOG/ENFORCE + compose
- 0861220 feat(core): guard en PerfilProveedorController y ProveedorArchivosController
- bbb7ce6 feat(security): X-Internal-Service-Key en llamadas a core
- Commit de script e2e + informe: ver hash en `git log` (ultimo commit, "test: script e2e authz + informe D9").
Push a origin/develop sin force (origin no habia avanzado al momento del push).

## 7. Llamadores sin token descubiertos

En la corrida viva solo aparecieron mis propios casos. Por inspeccion de codigo y de este informe:
- El gateway reenvia el Authorization del cliente; la app/llamadores que hagan GET perfil-proveedor sin Bearer quedarian en 401 al pasar a ENFORCE (visible como AUTHZ_VIOLATION reason=NO_TOKEN en LOG).
- security -> core (fetchByAuthUserId, pushProfilePhoto): usaban core sin llave; ya corregido con X-Internal-Service-Key.
- Revisar en LOG durante el uso real de la app (celular) antes de activar ENFORCE: `docker logs findu-core | Select-String AUTHZ_VIOLATION`.

## 8. PENDIENTES

- Fase 3: OfertaController/proveedor, CancelacionProveedorController/proveedor, EspecialidadController, EspecialistaCredencialController, DireccionController (no instrumentados).
- findu-transaction y WebSocket/dispatcher (fase 4); gateway (fase 5).
- Rotacion de FINDU_INTERNAL_SERVICE_KEY y JWT_SECRET: en Railway definir valor unico por entorno en gateway, security, core, processor y transaction (hoy se usan defaults publicos del repo).
- Rechazo de refresh token en security (hoy security acepta typ=JWTRefresh como access; core ya lo rechaza).
- Endpoints compartidos con la app cliente por verificar antes de ENFORCE (p.ej. GET {id} y auth/{authUserId} podrian ser llamados por clientes).
- GET /api/v1/catalogs/specialties responde 500 sin parametros (EL1021E pageNumber).
- Activacion biometrica y registro completos en vivo: validar con la app.

## 9. Pasar a ENFORCE por ambito

1. Revisar que no haya violaciones legitimas: `docker logs findu-core 2>&1 | Select-String AUTHZ_VIOLATION` tras uso real de la app.
2. Correr `./scripts/authz-e2e.ps1 -Mode LOG` (baseline).
3. Un solo ambito (p.ej. archivos): definir en el servicio findu-core del compose (o env del despliegue):
   - FINDU_AUTHZ_MODE=ENFORCE
   - FINDU_AUTHZ_ENFORCE_SCOPES=archivos
   Ambitos: `archivos` (PUT foto-perfil) y `perfil` (resto). En ENFORCE los ambitos fuera de la lista siguen solo registrando AUTHZ_VIOLATION.
4. Recrear solo core: `docker compose up -d --no-deps findu-core`.
5. Verificar: `./scripts/authz-e2e.ps1 -Mode ENFORCE` (con scopes parciales, los casos del ambito no activo daran 200; para validacion completa usar scopes `archivos,perfil` o vacio).
6. Para varios ambitos: FINDU_AUTHZ_ENFORCE_SCOPES=archivos,perfil (o vacio = todos).
7. Rollback inmediato: FINDU_AUTHZ_MODE=LOG (u OFF) y recrear core.
8. Antes de produccion: JWT_SECRET y FINDU_INTERNAL_SERVICE_KEY unicos por entorno.
