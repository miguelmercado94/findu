# Implementation Plan — findu-notification-processor dynamic template engine

> Authoritative design: `CORE/findu-notification-processor/DISENO_MODELO_PLANTILLAS_DINAMICAS.md`.
> Worktree micro dir: `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\.worktrees\notif-template-engine\CORE\findu-notification-processor`.
> Verify every item with `.\gradlew.bat build` run from the micro dir (BUILD SUCCESSFUL = pass).
>
> This plan is also expressed as FEAT artifacts in `../features/FEAT-00{1..4}.json`; the workflow
> executes those. This file is the human-readable mirror / fallback.

## Phase 0 — Build prerequisite (do once, before any change)

- [ ] 1. Copy the git-ignored `gradle-wrapper.jar` into the worktree so `.\gradlew.bat` runs.
      The fresh worktree is missing `gradle/wrapper/gradle-wrapper.jar`; copy it from the parent checkout
      `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\CORE\findu-notification-processor\gradle\wrapper\gradle-wrapper.jar`.
      Do NOT `git add` it (ignored).
      Files: `gradle/wrapper/gradle-wrapper.jar` (worktree copy, uncommitted).
      Verify: `.\gradlew.bat build` on the untouched tree prints BUILD SUCCESSFUL (baseline confirmed green).

## Phase 1 — Hexagonal re-layout + domain foundation (FEAT-001)

- [ ] 2. Add `domain/enums`: `TemplateType {SMS,EMAIL,WHATSAPP,EVENTO,NOTIFICACION,ALERTA}`,
      `AlertCategory {INFO,SEGURIDAD,PREVENTIVA}`, `AudienceMode {TODOS,SEGMENTO,USUARIO}`; move
      `NotificationChannel` into `domain/enums` (senders still use it).
      Files: `domain/enums/*.java`.
      Verify: `.\gradlew.bat build`.

- [ ] 3. Add domain value objects `Audience`, `NotificationRouting`, `AlertMeta` and extend
      `NotificationTemplate` with all common + per-type fields (design §3) using those objects, not raw maps.
      Files: `domain/model/{Audience,NotificationRouting,AlertMeta,NotificationTemplate}.java`.
      Verify: `.\gradlew.bat build`.

- [ ] 4. Add `domain/exception/{TemplateNotFoundException,InvalidTemplateException}`; relocate
      `GlobalExceptionHandler`+`ErrorResponse` to `infrastructure/adapter/input/web/exception`.
      Files: `domain/exception/*.java`, `infrastructure/adapter/input/web/exception/*.java`.
      Verify: `.\gradlew.bat build`.

- [ ] 5. Relocate ports (`application/port/out/{persistence,externalapi}`), DTOs (`application/dto/request`),
      controllers (`infrastructure/adapter/input/web/controller`), persistence adapters
      (`infrastructure/adapter/out/persistence/adapter`), senders (`infrastructure/adapter/out/externalapi`);
      fix all imports and wiring. Send flow behavior unchanged.
      Files: moves across `application/`, `infrastructure/`, deletion of `presentation/`, root `dto/`, root `exception/`, `application/port/output/`.
      Verify: `.\gradlew.bat build` — BUILD SUCCESSFUL, `FinancialEventRouterTest` passes.

## Phase 2 — Versioned persistence + resolution + render (FEAT-002)

- [ ] 6. Extend `TemplateRepositoryPort` with `findActive`, `createNewVersion`, `activateVersion`,
      `findHistory`, `findByTipo`, `deactivate`.
      Files: `application/port/out/persistence/TemplateRepositoryPort.java`.
      Verify: `.\gradlew.bat build`.

- [ ] 7. Add DynamoDB `entity/NotificationTemplateItem` + `mapper/NotificationTemplateItemMapper`
      (domain <-> item; only place that knows attribute names).
      Files: `infrastructure/adapter/out/persistence/{entity,mapper}/*.java`.
      Verify: `.\gradlew.bat build` + mapper round-trip unit test passes.

- [ ] 8. Rewrite `DynamoDbTemplateAdapter`: resolve via `GetItem(SK='v#ACTIVE')`; `createNewVersion`
      writes `v#<n+1>` + updates `v#ACTIVE` via `TransactWriteItems`; `activateVersion` rollback;
      `findHistory` via Query `begins_with 'v#'`; `findByTipo` via GSI_tipo. No AWS imports leak into application/domain.
      Files: `infrastructure/adapter/out/persistence/adapter/DynamoDbTemplateAdapter.java`.
      Verify: `.\gradlew.bat build` + adapter transaction-assembly unit test (mocked DynamoDbClient) passes.

- [ ] 9. Add `application/util` renderer + `VariableValidator` (declared `variables` must exist in `params`);
      add `application/port/in/ResolveTemplateUseCase`; update `TemplateServiceImpl` + Factory/AbstractNotificationTemplate
      to resolve by `TemplateType` (PUSH->NOTIFICACION) keeping the 4 consumers and OTP resolution working.
      Files: `application/util/*.java`, `application/port/in/ResolveTemplateUseCase.java`, `application/service/impl/TemplateServiceImpl.java`, `application/template/*.java`.
      Verify: `.\gradlew.bat build` + util tests pass; consumers still resolve active version.

## Phase 3 — Admin management surface (FEAT-003)

- [ ] 10. Add `application/port/in/ManageTemplateUseCase` (create/edit->new version/activateVersion/listByType/history/deactivate),
      request DTOs (`CreateTemplateRequest`,`UpdateTemplateRequest`,`ActivateVersionRequest` with jakarta.validation),
      response DTOs (`TemplateResponse`,`TemplateVersionResponse`), and a domain<->DTO mapper.
      Files: `application/port/in/ManageTemplateUseCase.java`, `application/dto/request/*.java`, `application/dto/response/*.java`.
      Verify: `.\gradlew.bat build`.

- [ ] 11. Implement `TemplateAdminServiceImpl` (delegates to repo ports, evicts cache on mutations) and
      `TemplateAdminController` at `/api/v1/admin/templates` (POST create, PUT edit, POST activate, GET list-by-type,
      GET history, DELETE/PATCH deactivate) with full OpenAPI annotations like core's `OfertaController`.
      Files: `application/service/impl/TemplateAdminServiceImpl.java`, `infrastructure/adapter/input/web/controller/TemplateAdminController.java`.
      Verify: `.\gradlew.bat build` + admin controller/service tests pass (create=v1, edit=v2, rollback, list, history, deactivate).

## Phase 4 — Seed + docs (FEAT-004)

- [ ] 12. Rewrite `seed-templates.sh` to composite key id(HASH)+sk(RANGE) + GSIs GSI_tipo/GSI_pantalla/GSI_codigo,
      writing `v#1` + `v#ACTIVE` per template with the exact mapper attribute names, one example of each of the 6 types
      (design §7) plus OTP SMS+EMAIL; refresh `OpenApiConfig`/README to the template-engine purpose.
      Files: `src/main/resources/db/dynamodb/seed-templates.sh`, `config/OpenApiConfig.java`, `README.md`.
      Verify: `bash -n seed-templates.sh` (no errors) + `.\gradlew.bat build` BUILD SUCCESSFUL.

## Cross-cutting constraints

- Do NOT deploy to Railway or touch real AWS (code + LocalStack seed only).
- Do NOT modify other micros (core, transaction, help, gateway).
- Keep reactive `Mono` style where already used; respect Lombok.
- Preserve Template Method, Factory, and Router-by-eventType patterns.
- Each item leaves the build green; commit locally per phase, never push.
