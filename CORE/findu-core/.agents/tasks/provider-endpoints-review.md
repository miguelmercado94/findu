# GET historial de solicitudes del proveedor y GET cobertura dedicada

El commit `bfb8b57` agrega dos endpoints de lectura al módulo `findu-core`. El primero, `GET /api/v1/ofertas/proveedor/mis-solicitudes?proveedorId=&estado=`, devuelve las solicitudes que un proveedor atendió, resolviendo la relación proveedor→solicitud a través de ofertas en estado `ACEPTADA` con una query JPQL real y un filtro opcional por estado de solicitud. El segundo, `GET /api/v1/perfil-proveedor/{id}/cobertura`, expone como endpoint dedicado la misma lista de municipios que ya venía embebida en el detalle del perfil, extrayendo la lógica a un método `consultarCobertura(id)` reutilizado por el detalle. Ambos siguen el estilo bloqueante del módulo, completan la cadena hexagonal (puerto in → servicio de aplicación → puerto out → adaptador → repositorio) y reusan los DTOs existentes `SolicitudResponse` y `MunicipioResponse`.

Watch for: nada bloqueante. El refactor de `consultarPerfil` para delegar en `consultarCobertura` introduce una segunda carga de `findById` sobre el mismo perfil dentro de la misma transacción de lectura (observación de rendimiento menor, **confirmed**), y `mis-solicitudes` carga el catálogo completo de servicios en memoria para resolver nombres (patrón ya presente en el módulo, **confirmed**).

**Verdict**: APPROVED

## High-level view

La cadena hexagonal del endpoint 1 está completa y es real: el puerto de entrada `GestionOfertasUseCase.obtenerMisSolicitudes` baja hasta una `@Query` JPQL en `SolicitudServicioRepository` que filtra solicitudes cuyo id está en el subselect de ofertas `ACEPTADA` del proveedor, con estado opcional vía `(:estado IS NULL OR ...)`. No hay mock ni hardcode en ninguna capa; los nombres de columna/campo del JPQL (`solicitudServicioId`, `perfilProveedorId`, `estadoOferta`) coinciden con `OfertaEntity`, y el literal `'ACEPTADA'` coincide con el valor que `aceptarOferta` escribe en la misma entidad.

El endpoint 2 no añade persistencia nueva: extrae el bloque de cobertura de `consultarPerfil` a un método público `consultarCobertura(id)` y hace que el detalle lo invoque. La lista resultante es idéntica a la que antes se construía inline, de modo que el contrato de `PerfilProveedorDetalleResponse` y el del PUT `/cobertura` quedan intactos.

La superficie de API es coherente con el resto del módulo: `proveedorId` como `@RequestParam` obligatorio y `estado` opcional en ofertas; `id` como `@PathVariable` en cobertura. Ambos validan la existencia del proveedor y lanzan `ResourceNotFoundException` (→ 404) antes de consultar, igual que los demás métodos. Las anotaciones OpenAPI están presentes y describen 200/400/404/500.

No hay tests nuevos y el módulo no tiene `src/test`, así que ninguna de las dos rutas queda cubierta por pruebas automatizadas; la única verificación es la build del bootJar.

<details>
<summary>Issues (2)</summary>

1. **Doble `findById` del perfil en el detalle** — `consultarPerfil` ahora llama a `consultarCobertura(id)`, que vuelve a cargar el mismo perfil que ya cargó al inicio. No afecta corrección; si importa el rendimiento, pasar el perfil ya cargado o un flag que omita la re-validación.
2. **Rutas nuevas sin cobertura de tests** — `mis-solicitudes` y `/cobertura` no tienen pruebas (el módulo carece de `src/test`). La única evidencia es la build. Considerar un slice `@WebMvcTest` o un test de la `@Query` si se quiere blindar la resolución proveedor→solicitud.

</details>

<details>
<summary>Details</summary>

### Resolución proveedor→solicitud vía ofertas aceptadas

El núcleo del endpoint 1 es la `@Query` de `SolicitudServicioRepository.findByProveedorAceptadaAndEstado`:

```java
@Query("SELECT s FROM SolicitudServicioEntity s " +
        "WHERE s.id IN (SELECT o.solicitudServicioId FROM OfertaEntity o " +
        "               WHERE o.perfilProveedorId = :proveedorId AND o.estadoOferta = 'ACEPTADA') " +
        "AND (:estado IS NULL OR s.estadoSolicitud = :estado) " +
        "ORDER BY s.fechaProgramada DESC")
```

La relación "el proveedor atendió esta solicitud" se modela como "existe una oferta `ACEPTADA` de ese proveedor sobre la solicitud", consistente con `aceptarOferta`, que marca exactamente una oferta como `ACEPTADA` por solicitud y pasa las demás a `RECHAZADA`. El subselect por `IN` evita duplicados aunque el proveedor tuviera varias ofertas sobre la misma solicitud. Verificado contra las entidades: los campos del JPQL (`solicitudServicioId`, `perfilProveedorId`, `estadoOferta`, `estadoSolicitud`) corresponden a atributos reales de `OfertaEntity`/`SolicitudServicioEntity`, y como `estadoOferta` es `String`, el literal `'ACEPTADA'` compara contra el mismo valor que escribe `aceptarOferta` — sin mock ni hardcode en la ruta.

El servicio `obtenerMisSolicitudes` valida la existencia del proveedor (404), normaliza el estado en blanco a `null` y mapea a `SolicitudResponse` resolviendo `servicioNombre` y `direccionTexto` reales. Este mapeo es más completo que el del endpoint hermano `obtenerSolicitudesDisponibles`, que deja ambos campos en `null`; el nuevo endpoint entrega la data real que pide la tarea (eliminar el "quemado" de datos en el móvil).

### Cobertura dedicada sin tocar el contrato del detalle

El endpoint 2 no agrega persistencia: el bloque que antes vivía inline en `consultarPerfil` se movió a `consultarCobertura(id)` y el detalle ahora lo invoca.

```java
List<MunicipioResponse> cobertura = consultarCobertura(id);
```

El cuerpo extraído es idéntico al original (misma proyección `coberturaService.findByProveedorId` → `municipioPort.findById` → `MunicipioResponse`, mismo filtro de nulos), por lo que `PerfilProveedorDetalleResponse.cobertura` y el PUT `/cobertura` (`actualizarCobertura`) quedan intactos. El método nuevo repite la validación de existencia del proveedor —correcto como punto de entrada independiente— pero introduce una segunda llamada a `perfilProveedorService.findById(id)` cuando lo invoca `consultarPerfil`, que ya había cargado el perfil. Es una lectura extra dentro de la misma transacción `readOnly`, mencionable solo como afinamiento.

### Cobertura de tests

No hay tests nuevos y el módulo no tiene directorio `src/test`, según registró el coder y es consistente con la ausencia de cualquier slice de controlador en el repo. En consecuencia, ninguna de las dos rutas tiene verificación automatizada: ni la resolución proveedor→solicitud de la `@Query`, ni el filtrado por estado, ni los 404. La única evidencia de que compila y ensambla es `gradlew clean bootJar -x test` → BUILD SUCCESSFUL con el jar generado, que es lo que exige el Dockerfile. Dado que la tarea prohíbe correr suites y no existe infraestructura de test previa, esto no es bloqueante, pero queda como la mayor brecha de confianza del cambio. Un `@WebMvcTest` sobre ambos controladores o un test de datos para la `@Query` sería la forma de blindar la pieza más delicada (el subselect de ofertas aceptadas).

</details>

<details>
<summary>Mapa de archivos</summary>

- `application/port/in/GestionOfertasUseCase.java` — declara `obtenerMisSolicitudes(Long, String)`.
- `application/service/GestionOfertasUseCaseImpl.java` — implementa el historial; nueva dependencia `ServicioRepositoryPort`; valida proveedor, normaliza estado y mapea a `SolicitudResponse` con nombre de servicio y dirección reales.
- `application/port/out/persistence/SolicitudServicioRepositoryPort.java` — declara `findByProveedorIdViaOfertaAceptada`.
- `application/service/SolicitudServicioService.java` + `impl/SolicitudServicioServiceImpl.java` — expone/delegación 1:1 del nuevo método.
- `infrastructure/adapter/out/persistence/repository/SolicitudServicioRepository.java` — `@Query` JPQL con subselect de ofertas `ACEPTADA` y filtro opcional de estado.
- `infrastructure/adapter/out/persistence/adapter/SolicitudServicioRepoAdapter.java` — implementa el puerto delegando en la query y mapeando a dominio.
- `infrastructure/adapter/input/web/controller/OfertaController.java` — `GET /proveedor/mis-solicitudes` + OpenAPI.
- `application/port/in/GestionPerfilProveedorUseCase.java` — declara `consultarCobertura(Long)`.
- `application/service/GestionPerfilProveedorUseCaseImpl.java` — extrae `consultarCobertura` y hace que `consultarPerfil` delegue en él.
- `infrastructure/adapter/input/web/controller/PerfilProveedorController.java` — `GET /{id}/cobertura` + OpenAPI; import de `MunicipioResponse`.
- `.agents/tasks/provider-endpoints-verification.md` — notas de verificación del coder.

Diff completo: `git -C findu show bfb8b57`.

</details>
