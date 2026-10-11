# Verificación — Endpoints GET del proveedor (`findu-core`)

Iteración: PRIMERA (no existía `provider-endpoints-review.json`).

## Comandos ejecutados y resultados

### 1. Build + bootJar (requerido por el Dockerfile `COPY build/libs/*.jar`)

Desde el módulo `c:\Users\ADMIN\Documents\FIND-U-PROJECT\findu\CORE\findu-core`:

```
.\gradlew.bat clean bootJar -x test --no-daemon
```

Resultado: **BUILD SUCCESSFUL in 22s** (5 actionable tasks: 5 executed).

- Único warning: `ServicioMapper.java:17: warning: Unmapped target property: "categoria"` — PRE-EXISTENTE, no relacionado con este cambio (MapStruct en un mapper no tocado).
- Artefacto generado: `build/libs/findu-core-0.0.1-SNAPSHOT.jar`.

### 2. Tests

El módulo **no tiene fuentes de test** (`src/test` no existe). No hay infraestructura de tests (`@WebMvcTest`/slice) para `OfertaController` ni `PerfilProveedorController`, de modo que no se añaden tests nuevos (coherente con la nota del plan). La verificación mínima requerida —la build del jar— pasa.

### 3. Railway

No se desplegó (prohibido por la tarea).

## Resumen de capas tocadas

ENDPOINT 1 (historial del proveedor):
- `application/port/out/persistence/SolicitudServicioRepositoryPort.java` — nuevo método `findByProveedorIdViaOfertaAceptada`.
- `infrastructure/adapter/out/persistence/repository/SolicitudServicioRepository.java` — `@Query` JPQL `findByProveedorAceptadaAndEstado`.
- `infrastructure/adapter/out/persistence/adapter/SolicitudServicioRepoAdapter.java` — implementación delegando en la query + map a dominio.
- `application/service/SolicitudServicioService.java` + `impl/SolicitudServicioServiceImpl.java` — expone el método (delegación 1:1).
- `application/port/in/GestionOfertasUseCase.java` — nuevo `obtenerMisSolicitudes`.
- `application/service/GestionOfertasUseCaseImpl.java` — implementación (valida proveedor → 404, normaliza estado, mapea a `SolicitudResponse` resolviendo `servicioNombre` y `direccionTexto`). Nueva dependencia `ServicioRepositoryPort`.
- `infrastructure/adapter/input/web/controller/OfertaController.java` — `GET /api/v1/ofertas/proveedor/mis-solicitudes` + OpenAPI.

ENDPOINT 2 (cobertura dedicada):
- `application/port/in/GestionPerfilProveedorUseCase.java` — nuevo `consultarCobertura`.
- `application/service/GestionPerfilProveedorUseCaseImpl.java` — método `consultarCobertura(id)` extraído de `consultarPerfil` (reusado por el detalle; lista idéntica a `cobertura[]`).
- `infrastructure/adapter/input/web/controller/PerfilProveedorController.java` — `GET /api/v1/perfil-proveedor/{id}/cobertura` + OpenAPI.

## Relación proveedor → solicitud

Un proveedor "atendió" una solicitud cuando tiene una **oferta ACEPTADA** sobre ella:
- Tabla `oferta` (`OfertaEntity`): `perfil_proveedor_id`, `solicitud_servicio_id`, `estado_oferta`.
- Tabla `solicitud_servicio` (`SolicitudServicioEntity`): `estado_solicitud`.
- Query: `SolicitudServicioEntity` cuyo `id IN (SELECT o.solicitudServicioId FROM OfertaEntity o WHERE o.perfilProveedorId = :proveedorId AND o.estadoOferta = 'ACEPTADA')`, filtrando opcionalmente por `estado_solicitud`, ordenado por `fecha_programada DESC`.
