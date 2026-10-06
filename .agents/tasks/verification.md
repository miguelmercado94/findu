# Verification — findu-core canonical hexagonal restructuring

Pure mechanical package/layout refactor of `findu-core` (root package `com.findu.core`) to the
canonical hexagonal layout used by the sibling modules `findu-transaction` and `findu-help-v2`.
No class renames, no method-signature changes, no logic changes, no REST path changes, no DTO
field changes, no JPA mapping changes, no Spring bean-name changes. `FinduCoreApplication` stays
at the root package; `src/main/resources` untouched.

## Gradle commands run (Windows gradle wrapper, `--no-daemon`)

Module dir: `CORE\findu-core` inside worktree `.worktrees\core-hexagonal`.

> Note: the worktree was missing `gradle/wrapper/gradle-wrapper.jar` (it is `.gitignore`d). The jar
> was copied in locally from the main repo's `CORE/findu-core` so the wrapper could run. It is not
> committed (gitignored) and is a build-tool artifact only, not source.

1. Baseline (before refactor):
   `.\gradlew.bat clean compileJava compileTestJava --no-daemon` → **BUILD SUCCESSFUL** (2m 36s)

2. After refactor — main compile:
   `.\gradlew.bat clean compileJava --no-daemon` → **BUILD SUCCESSFUL** (2m 29s)

3. After refactor — full build incl. tests (the authoritative gate):
   `.\gradlew.bat clean build --no-daemon` → **BUILD SUCCESSFUL** (3m 58s), 8 actionable tasks executed.

   - `:test` ran `com.findu.core.FinduCoreApplicationTests.contextLoads`.
     Test report `build/test-results/test/TEST-com.findu.core.FinduCoreApplicationTests.xml`:
     `tests="1" skipped="0" failures="0" errors="0"`. The Spring context booted successfully,
     proving component scan / JPA entity scan / Spring Data repository scan / MapStruct mapper
     wiring all resolve under the new packages.
   - The console shows Eureka `Connection refused` warnings/stack traces during the context
     shutdown hook (no local discovery server at localhost:8761). These are runtime shutdown noise
     only; they do NOT fail the test (`errors="0"`) and Gradle reported `BUILD SUCCESSFUL`. The
     non-zero process exit was from the shutdown-hook logging, not from a task failure.

## Residue checks (expect no matches) — all clean

- No source references any old FQN:
  `com.findu.core.{dto.,messages.,presentation.,application.usecase.,application.port.output.,`
  `infrastructure.entity.,infrastructure.repository.,infrastructure.adapter.persistence.,`
  `infrastructure.adapter.externalapi.,domain.model.constants.,mapper.,exception.,config.}` → 0 matches.
- All old package directories removed; no leftover old `package` declarations.
- `ResourceNotFoundException` → `com.findu.core.domain.exception`;
  `GlobalExceptionHandler` + `ErrorResponse` → `com.findu.core.infrastructure.adapter.input.web.exception`.

## Package moves (OLD → NEW, all under `com.findu.core`)

| OLD | NEW |
|---|---|
| `domain.model.constants` | `domain.enums` |
| `exception.ResourceNotFoundException` | `domain.exception` |
| `exception.{GlobalExceptionHandler,ErrorResponse}` | `infrastructure.adapter.input.web.exception` |
| `application.usecase` (interfaces) | `application.port.in` |
| `application.usecase.impl` | `application.service` (peers of existing service ifaces) |
| `application.service` / `application.service.impl` | unchanged (preserved — second service layer) |
| `application.port.output.persistence` | `application.port.out.persistence` |
| `application.port.output.externalapi` | `application.port.out.externalapi` |
| `messages.ApiMessages` | `application.util` |
| `dto.request` | `application.dto.request` |
| `dto.response` | `application.dto.response` |
| `config` (+ `config.security`) | `infrastructure.config` (+ `.security`) |
| `presentation.controller` | `infrastructure.adapter.input.web.controller` |
| `infrastructure.entity` | `infrastructure.adapter.out.persistence.entity` |
| `infrastructure.repository` | `infrastructure.adapter.out.persistence.repository` |
| `infrastructure.adapter.persistence` | `infrastructure.adapter.out.persistence.adapter` |
| `mapper` | `infrastructure.adapter.out.persistence.mapper` |
| `infrastructure.adapter.externalapi` | `infrastructure.adapter.out.externalapi` |

Both service layers coexist with no collisions: `application.service` holds the 7 moved UseCase
impls + 13 existing service interfaces; `application.service.impl` keeps its 13 existing impls.
MapStruct mappers keep `componentModel = SPRING`. `FinduCoreApplication` kept at `com.findu.core`
with its bare `@SpringBootApplication` (no scan-annotation edits needed).
