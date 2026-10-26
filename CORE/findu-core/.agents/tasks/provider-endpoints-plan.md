# Implementation Plan — Dos endpoints GET nuevos en `findu-core`

Módulo: `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\CORE\findu-core`
Repo git raíz: `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu` (rama `develop`, SIN worktree).
Build (desde el módulo, Windows): `.\gradlew.bat clean build` o rápido `.\gradlew.bat clean bootJar -x test --no-daemon`.
Todas las rutas de archivos en este plan son relativas a la raíz del módulo salvo que se indique lo contrario.

## Hechos descubiertos en el código real (base de las decisiones)

- **Estilo del módulo: MVC bloqueante + Spring Data JPA.** `build.gradle` usa `spring-boot-starter-web` + `spring-boot-starter-data-jpa` + PostgreSQL JDBC (NO WebFlux, NO R2DBC). Java 21, Lombok, MapStruct 1.6.3, `springdoc-openapi-starter-webmvc-ui`. No se usa `Mono`/`Flux` en ninguna parte; todo es `List`/`Optional` síncrono.
- **Arquitectura hexagonal real (nombres de paquete exactos):**
  - Puerto de entrada (use case): `com.findu.core.application.port.in.*` (p.ej. `GestionOfertasUseCase`, `GestionPerfilProveedorUseCase`).
  - Implementación del use case: `com.findu.core.application.service.*UseCaseImpl` (p.ej. `GestionOfertasUseCaseImpl`, `GestionPerfilProveedorUseCaseImpl`), anotadas `@Service @Transactional`.
  - Servicios CRUD finos (envoltura del puerto out): `com.findu.core.application.service.<X>Service` + impl en `com.findu.core.application.service.impl.<X>ServiceImpl`.
  - Puerto de salida: `com.findu.core.application.port.out.persistence.*RepositoryPort`.
  - Adapter de persistencia: `com.findu.core.infrastructure.adapter.out.persistence.adapter.*RepoAdapter` (anotado `@Component`), que usa un Spring Data `JpaRepository` en `...out.persistence.repository.*Repository`, entidades en `...out.persistence.entity.*Entity`, mappers MapStruct en `...out.persistence.mapper.*Mapper`.
  - Controllers: `com.findu.core.infrastructure.adapter.input.web.controller.*Controller`.
- **Manejo de errores:** `GlobalExceptionHandler` (`@RestControllerAdvice`) ya mapea `ResourceNotFoundException` → 404, `IllegalArgumentException` → 400, `IllegalStateException` → 409. Para el 404 solo hay que lanzar `com.findu.core.domain.exception.ResourceNotFoundException`.
- **DTOs a reusar (confirmados abriendo los records):**
  - `SolicitudResponse(Long id, String servicioNombre, String direccionTexto, LocalDateTime fechaProgramada, String nombreContacto, String telefonoContacto, Integer prioridad, BigDecimal presupuestoMaximo, Boolean esPresupuestoEstricto, String detalles, String fotos, String estadoSolicitud)`.
  - `MunicipioResponse(Long id, String codigoDane, String nombre, String departamento)`.

### Relación proveedor → solicitud (RESUELTA en código)

- `OfertaEntity` → tabla `oferta`, columnas: `perfil_proveedor_id`, `solicitud_servicio_id`, `estado_oferta` (campos Java `perfilProveedorId`, `solicitudServicioId`, `estadoOferta`).
- `SolicitudServicioEntity` → tabla `solicitud_servicio`, columna `estado_solicitud` (campo Java `estadoSolicitud`).
- Un proveedor "atendió" una solicitud cuando tiene una **oferta ACEPTADA** sobre ella. En `GestionOfertasUseCaseImpl.aceptarOferta()` la oferta ganadora pasa a `estadoOferta = "ACEPTADA"` y las demás a `"RECHAZADA"`; estados de oferta vistos: `ENVIADA`, `ACEPTADA`, `RECHAZADA`.
- Estados de solicitud vistos: `ABIERTA`, `EN_NEGOCIACION`, `PROGRAMADA`, `EN_CURSO`, `FINALIZADA`, `CANCELADA_SIN_PENALIDAD`, `CANCELADA_CON_PENALIDAD` (y en el `switch` del dominio también `ACEPTADA`/`COMPLETADO`).
- **Query de unión:** `oferta` (where `perfil_proveedor_id = :proveedorId` AND `estado_oferta = 'ACEPTADA'`) JOIN `solicitud_servicio` ON `oferta.solicitud_servicio_id = solicitud_servicio.id`, filtrando opcionalmente `solicitud_servicio.estado_solicitud = :estado`. Se devuelven las entidades `SolicitudServicioEntity`.

### Decisiones de diseño (con razón)

1. **ENDPOINT 1 — nueva query JPQL en `OfertaRepository` que devuelve `SolicitudServicioEntity`.** `OfertaEntity` no tiene relación JPA `@ManyToOne` hacia `SolicitudServicioEntity` (solo `Long solicitudServicioId`), así que uso un `@Query` JPQL explícito que une ambas entidades por id. Alternativa descartada: dos consultas en el service (traer ofertas aceptadas y luego cada solicitud) — es N+1 y hay más código; una sola query es más limpia y es el patrón esperado por la tarea ("query real en el adapter de persistencia"). El método vive en `OfertaRepository` (no en `SolicitudServicioRepository`) porque el filtro primario es por oferta del proveedor.
2. **ENDPOINT 1 — reusar `SolicitudResponse`, sin DTO nuevo.** El contrato ya existe y `solicitudes-disponibles` ya lo devuelve; no se infla el contrato con un DTO combinado. El monto acordado de la oferta no se pide para "servicios hechos" (el contador es `size()` de la lista con `estado=FINALIZADA`). Enriquecemos `servicioNombre` y `direccionTexto` (ver decisión 3) porque son útiles para la UI de historial y el dato está disponible; el resto de campos se mapean 1:1 desde `SolicitudServicioEntity`.
3. **ENDPOINT 1 — enriquecer `servicioNombre` y `direccionTexto` vía los servicios de aplicación existentes.** `GestionOfertasUseCaseImpl` ya inyecta `DireccionService` y puede resolver nombres de servicio igual que `GestionPerfilProveedorUseCaseImpl` (vía `ServicioRepositoryPort.findAll()`). Construyo un mapa `servicioId -> nombre` una sola vez y resuelvo `direccionTexto` por `direccionService.findById(direccionId)`. Si un dato no se resuelve, va `null` (igual que hoy hace `solicitudes-disponibles`, que pasa `null` en ambos). Sin mocks.
4. **ENDPOINT 2 — nuevo método en el use case que REUSA la lógica de cobertura ya existente, SIN nuevo puerto de salida.** `GestionPerfilProveedorUseCaseImpl.consultarPerfil()` ya construye exactamente `List<MunicipioResponse>` a partir de `coberturaService.findByProveedorId(id)` + `municipioPort.findById(...)`. Extraigo esa lógica a un método `consultarCobertura(Long id)` reutilizado por el detalle, evitando duplicar y garantizando que la lista sea idéntica a la embebida en `cobertura[]`. No se toca el PUT ni el detalle salvo para delegar en el método extraído.

---

## Pasos (ordenados por dependencia, cada uno deja el módulo compilable)

- [ ] 1. **ENDPOINT 1 — Capa persistencia (puerto out): añadir método de consulta al puerto `OfertaRepositoryPort`.**
      Añadir `List<Oferta> findAceptadasByProveedorId(Long perfilProveedorId)` NO; en su lugar, como la unión devuelve solicitudes, añadir el método que devuelve solicitudes del proveedor. Decisión: el método de dominio devuelve `List<SolicitudServicio>` para no exponer entidades fuera de persistencia. Firma a añadir en el puerto out `SolicitudServicioRepositoryPort`: `List<SolicitudServicio> findByProveedorIdViaOfertaAceptada(Long proveedorId, String estadoSolicitud)` (estadoSolicitud puede ser null = todas).
      Files: `src/main/java/com/findu/core/application/port/out/persistence/SolicitudServicioRepositoryPort.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` compila (el adapter aún no implementa el método, por eso este paso se completa junto al paso 2; ejecutar la verificación tras el paso 2).

- [ ] 2. **ENDPOINT 1 — Capa persistencia: query JPQL real + implementación en el adapter.**
      En `SolicitudServicioRepository` añadir una query que una `OfertaEntity` y `SolicitudServicioEntity`:
      ```java
      @Query("SELECT s FROM SolicitudServicioEntity s " +
             "WHERE s.id IN (SELECT o.solicitudServicioId FROM OfertaEntity o " +
             "               WHERE o.perfilProveedorId = :proveedorId AND o.estadoOferta = 'ACEPTADA') " +
             "AND (:estado IS NULL OR s.estadoSolicitud = :estado) " +
             "ORDER BY s.fechaProgramada DESC")
      List<SolicitudServicioEntity> findByProveedorAceptadaAndEstado(@Param("proveedorId") Long proveedorId,
                                                                     @Param("estado") String estado);
      ```
      (Importar `org.springframework.data.jpa.repository.Query` y `org.springframework.data.repository.query.Param`.) En `SolicitudServicioRepoAdapter` implementar `findByProveedorIdViaOfertaAceptada(proveedorId, estadoSolicitud)` delegando en esa query y mapeando `entity -> domain` con el `SolicitudServicioMapper` existente (mismo patrón `.stream().map(mapper::toDomain).toList()` ya usado en el adapter).
      Files: `src/main/java/com/findu/core/infrastructure/adapter/out/persistence/repository/SolicitudServicioRepository.java`, `src/main/java/com/findu/core/infrastructure/adapter/out/persistence/adapter/SolicitudServicioRepoAdapter.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` compila sin errores.

- [ ] 3. **ENDPOINT 1 — Capa application service CRUD: exponer el nuevo método en `SolicitudServicioService`.**
      Añadir `List<SolicitudServicio> findByProveedorIdViaOfertaAceptada(Long proveedorId, String estadoSolicitud)` a la interfaz y su impl, delegando en el puerto out (mismo patrón de delegación 1:1 que los demás métodos de `SolicitudServicioServiceImpl`).
      Files: `src/main/java/com/findu/core/application/service/SolicitudServicioService.java`, `src/main/java/com/findu/core/application/service/impl/SolicitudServicioServiceImpl.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` compila sin errores.

- [ ] 4. **ENDPOINT 1 — Capa application port in (use case): añadir método al puerto `GestionOfertasUseCase`.**
      Añadir `List<SolicitudResponse> obtenerMisSolicitudes(Long proveedorId, String estado)` (estado opcional/null).
      Files: `src/main/java/com/findu/core/application/port/in/GestionOfertasUseCase.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` falla esperado hasta el paso 5 (la impl aún no existe); verificar tras el paso 5.

- [ ] 5. **ENDPOINT 1 — Capa application: implementar el use case en `GestionOfertasUseCaseImpl`.**
      Implementar `obtenerMisSolicitudes(proveedorId, estado)` anotado `@Transactional(readOnly = true)`:
      1) Validar existencia del proveedor con `proveedorService.findById(proveedorId).orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + proveedorId))` (404 coherente).
      2) Normalizar `estado` (si viene vacío/blank tratarlo como null).
      3) Llamar `solicitudService.findByProveedorIdViaOfertaAceptada(proveedorId, estado)`.
      4) Construir un `Map<Long,String> nombresServicio` con `servicioPort.findAll()` (inyectar `ServicioRepositoryPort` igual que en `GestionPerfilProveedorUseCaseImpl`) y resolver `direccionTexto` con `direccionService.findById(s.getDireccionId())` (ya inyectado). Mapear cada `SolicitudServicio` a `SolicitudResponse` llenando TODOS los campos reales (`servicioNombre` desde el mapa, `direccionTexto` desde la dirección, resto 1:1); valores no resueltos → `null`. Sin listas hardcodeadas; si no hay trabajos, la lista sale vacía.
      Files: `src/main/java/com/findu/core/application/service/GestionOfertasUseCaseImpl.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` compila sin errores (puertos in de pasos 1-4 ya satisfechos).

- [ ] 6. **ENDPOINT 1 — Capa infrastructure web: añadir el endpoint en `OfertaController`.**
      Añadir, junto a `solicitudesDisponibles`, un método `GET /api/v1/ofertas/proveedor/mis-solicitudes`:
      ```java
      @GetMapping("/proveedor/mis-solicitudes")
      public ResponseEntity<List<SolicitudResponse>> misSolicitudes(
              @RequestParam Long proveedorId,
              @RequestParam(required = false) String estado) {
          return ResponseEntity.ok(ofertasUseCase.obtenerMisSolicitudes(proveedorId, estado));
      }
      ```
      Añadir anotaciones OpenAPI `@Operation` + `@ApiResponses` (200 con `SolicitudResponse`, 400 proveedorId inválido, 404 proveedor no encontrado, 500) y `@Parameter` para `proveedorId` (required) y `estado` (opcional, con la lista de estados válidos en la descripción), replicando el estilo de `solicitudesDisponibles`.
      Files: `src/main/java/com/findu/core/infrastructure/adapter/input/web/controller/OfertaController.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` compila sin errores.

- [ ] 7. **ENDPOINT 2 — Capa application port in: añadir método a `GestionPerfilProveedorUseCase`.**
      Añadir `List<MunicipioResponse> consultarCobertura(Long id)` (`import com.findu.core.application.dto.response.MunicipioResponse;` y `java.util.List` ya presente).
      Files: `src/main/java/com/findu/core/application/port/in/GestionPerfilProveedorUseCase.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` falla esperado hasta el paso 8; verificar tras el paso 8.

- [ ] 8. **ENDPOINT 2 — Capa application: implementar `consultarCobertura` reutilizando la lógica existente.**
      En `GestionPerfilProveedorUseCaseImpl`, extraer el bloque que hoy construye `cobertura` dentro de `consultarPerfil` a un método público `consultarCobertura(Long id)` anotado `@Transactional(readOnly = true)`:
      1) `perfilProveedorService.findById(id).orElseThrow(() -> new ResourceNotFoundException("Perfil de proveedor no encontrado con id: " + id))` (404).
      2) `return coberturaService.findByProveedorId(id).stream().map(c -> municipioPort.findById(c.getMunicipioId()).orElse(null)).filter(Objects::nonNull).map(m -> new MunicipioResponse(m.getId(), m.getCodigoDane(), m.getNombre(), m.getDepartamento())).toList();`
      Luego, en `consultarPerfil`, reemplazar el cálculo inline de `List<MunicipioResponse> cobertura` por una llamada a `consultarCobertura(id)` (NO cambiar el contrato del detalle: la lista resultante es idéntica).
      Files: `src/main/java/com/findu/core/application/service/GestionPerfilProveedorUseCaseImpl.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` compila sin errores.

- [ ] 9. **ENDPOINT 2 — Capa infrastructure web: añadir el endpoint en `PerfilProveedorController`.**
      Añadir, simétrico al `PUT /{id}/cobertura` ya existente, un método `GET /api/v1/perfil-proveedor/{id}/cobertura`:
      ```java
      @GetMapping("/{id}/cobertura")
      public ResponseEntity<List<MunicipioResponse>> consultarCobertura(@PathVariable Long id) {
          return ResponseEntity.ok(proveedorUseCase.consultarCobertura(id));
      }
      ```
      Importar `com.findu.core.application.dto.response.MunicipioResponse`. Añadir `@Operation` + `@ApiResponses` (200 con `MunicipioResponse`, 404 proveedor no encontrado, 500) y `@Parameter` para `id`, en el estilo de los demás métodos del controller. No tocar el PUT ni `GET /{id}`.
      Files: `src/main/java/com/findu/core/infrastructure/adapter/input/web/controller/PerfilProveedorController.java`
      Verify: `.\gradlew.bat compileJava --no-daemon` compila sin errores.

- [ ] 10. **Verificación integral: build + tests del módulo.**
      Ejecutar la build completa del módulo para asegurar que compila, pasan los tests existentes y no se rompió ningún endpoint ni contrato.
      Files: (ninguno nuevo)
      Verify: `.\gradlew.bat clean build --no-daemon` termina en `BUILD SUCCESSFUL`; si el entorno no permite tests de integración con DB, al menos `.\gradlew.bat clean bootJar -x test --no-daemon` genera el jar (requerido por el `COPY build/libs/*.jar` del Dockerfile).

- [ ] 11. **Smoke test manual opcional (si hay entorno local con `docker compose up -d findu-core`).**
      Levantar el servicio y probar los dos endpoints con `curl` contra `http://localhost:8082/findu-core`:
      `GET /api/v1/ofertas/proveedor/mis-solicitudes?proveedorId=1` y `...&estado=FINALIZADA`; `GET /api/v1/perfil-proveedor/1/cobertura`. Confirmar 200 + JSON esperado (lista vacía si no hay datos) y 404 con proveedorId inexistente.
      Files: (ninguno)
      Verify: respuestas 200/404 coherentes; el `size()` de la lista con `estado=FINALIZADA` es el contador "servicios hechos".

- [ ] 12. **Commit en `develop`.**
      Desde la raíz del repo `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu`, estar en rama `develop`, hacer `git add` SOLO de los archivos modificados/creados listados arriba y commitear con un mensaje tipo Conventional Commits, p.ej. `feat(core): add GET mis-solicitudes del proveedor y GET cobertura dedicada`. NO hacer push salvo que se solicite explícitamente.
      Files: (los de los pasos 1-9)
      Verify: `git status` limpio tras el commit salvo artefactos de build ignorados; `git log --oneline -1` muestra el commit.

## Documentación para el front (incluir en el mensaje final / PR, no es un archivo nuevo obligatorio)

- `GET /api/v1/ofertas/proveedor/mis-solicitudes?proveedorId={id}&estado={estado?}` → `200 List<SolicitudResponse>`. `estado` opcional (`EN_CURSO`, `FINALIZADA`, ...). Contador "servicios hechos" = tamaño de la lista con `estado=FINALIZADA`. `404` si el proveedor no existe. Lista vacía si no tiene trabajos.
- `GET /api/v1/perfil-proveedor/{id}/cobertura` → `200 List<MunicipioResponse>` (`id`, `codigoDane`, `nombre`, `departamento`), idéntica a `cobertura[]` del detalle. `404` si el proveedor no existe.

## Notas / supuestos

- La anotación OpenAPI y los códigos de error replican los endpoints vecinos; no se introduce ninguna librería nueva.
- No se añaden tests nuevos obligatorios porque el módulo no tiene tests para `OfertaController`/`PerfilProveedorController` hoy; si se desea cobertura, añadir `@WebMvcTest`/slice tests siguiendo el ecosistema `spring-boot-starter-test` ya presente. La verificación mínima requerida es la build (paso 10).
- Si al implementar se descubre que `SolicitudServicioMapper` no expone `toDomain` con la firma esperada, usar el mismo método que ya usa `SolicitudServicioRepoAdapter` para el resto de consultas (confirmar abriendo el adapter antes de codificar el paso 2).
