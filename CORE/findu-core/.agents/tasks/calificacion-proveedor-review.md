# Especialidad nace en 3.5 y calificación general del perfil = promedio de especialidades activas

El cambio fija el piso de negocio para la calificación de un proveedor nuevo y corrige cómo se expone la calificación "general" del perfil. Al crear una especialidad (`agregarEspecialidad`) la calificación nace en `3.5`, y al consultar un perfil (`consultarPerfil`) la calificación general ya no es el valor suelto persistido en el perfil sino el promedio de las calificaciones de las especialidades **activas**, redondeado a 1 decimal con HALF_UP y con fallback `3.5` cuando no hay especialidades aplicables. Es la "Opción A" (cálculo en backend): el promedio se computa en cada lectura y no se persiste. Se agregan dos suites de test unitario que cubren el nacimiento en 3.5 y los casos de promedio/redondeo/fallback.

**Watch for:** el mensaje de commit escribe en prosa `new BigDecimal(3.5)` (constructor `double`), pero el código real usa `new BigDecimal("3.5")` (constructor `String`) en los tres sitios — es un typo de la prosa del mensaje, no del código (confirmado). Sin impacto funcional.

**Verdict**: APPROVED

## High-level view

El nacimiento en 3.5 se setea en el dominio dentro de `agregarEspecialidad`, no en la entidad. Esto es deliberado y necesario: la entidad JPA tiene `@Builder.Default` en `5.00`, pero el mapper MapStruct construye la entidad desde el dominio, así que el valor del dominio sobreescribe el default. Fijarlo en el dominio es el único punto correcto para garantizar 3.5.

La calificación general del perfil pasó a calcularse como promedio. `consultarPerfil` delega en un helper privado `calcularCalificacionGeneral` que promedia las calificaciones no nulas de la lista de especialidades que ya recibió. Esa lista proviene de `findByProveedorId`, que en el adaptador resuelve a `findAllByPerfilProveedorIdAndActiveTrue` — es decir, solo especialidades activas, tal como exige el criterio. El redondeo es `setScale(1, HALF_UP)` vía el `divide(..., 1, HALF_UP)`, y el fallback sin especialidades es `3.5`.

La consistencia entre `GET /perfil-proveedor/{id}` y `GET /perfil-proveedor/auth/{id}` es estructural, no duplicada: `consultarPerfilPorAuthUserId` resuelve el perfil por authUserId y luego llama a `consultarPerfil(perfil.getId())`, heredando exactamente el mismo cálculo. No hay riesgo de divergencia entre ambas rutas.

Las firmas de DTOs/records y las rutas no cambiaron, el esquema de BD tampoco (el promedio del perfil no se persiste; el valor suelto se sigue inicializando en `crearPerfil` solo por consistencia). El build y los tests están registrados como BUILD SUCCESSFUL con 5 tests en verde en el mensaje de commit.

<details>
<summary>Issues (1)</summary>

1. **Typo en prosa del mensaje de commit** — el mensaje dice `new BigDecimal(3.5)` pero el código usa `new BigDecimal("3.5")`. No bloqueante; opcionalmente corregir la redacción del mensaje en un commit futuro. No requiere cambio de código.

</details>

<details>
<summary>Details</summary>

### Nacimiento en 3.5 vía dominio, no vía entidad

`agregarEspecialidad` construye el `PerfilEspecialista` del dominio con `.calificacionPromedio(new BigDecimal("3.5"))`. El punto no obvio es por qué no se dejó al default de la entidad: `PerfilEspecialistaEntity` declara `@Column(name = "calificacion_promedio") @Builder.Default private BigDecimal calificacionPromedio = new BigDecimal("5.00")`. Como el `PerfilEspecialistaMapper` (MapStruct) construye la entidad a partir del dominio en `toEntity`, el valor del dominio gana sobre el default de la entidad. Si el 3.5 se hubiera omitido en el dominio, el mapper habría propagado `null` y el default de 5.00 no habría aplicado de forma predecible. Fijarlo en el dominio es correcto y es lo que el criterio pide. El test `especialidadNaceConCalificacion3_5` captura el argumento pasado a `save` y verifica `isEqualByComparingTo(new BigDecimal("3.5"))`.

### Promedio sobre especialidades activas y fallback

```
consultarPerfil(id)
   └─ findByProveedorId(id)
        └─ findAllByPerfilProveedorIdAndActiveTrue(id)   ← solo ACTIVAS
   └─ calcularCalificacionGeneral(especialidades)
        ├─ filtra calificaciones no nulas
        ├─ vacío → new BigDecimal("3.5").setScale(1, HALF_UP)
        └─ suma / size  con divide(size, 1, HALF_UP)
```

El helper promedia solo los valores no nulos y divide con escala 1 y HALF_UP, de modo que `[3.5, 4.0, 5.0]` → `4.1666…` → `4.2`, y una sola `[4.3]` se mantiene en `4.3`. El filtrado por activas no vive en el use case sino aguas abajo en el adaptador (`findAllByPerfilProveedorIdAndActiveTrue`), lo cual satisface el criterio de "especialidades ACTIVAS" sin que el use case tenga que volver a filtrar por `active`. El comentario del método lo documenta explícitamente. Los tests cubren promedio de dos (`4.0`), fallback sin especialidades (`3.5`), una sola especialidad (`4.3`) y redondeo HALF_UP (`4.2`).

### Consistencia entre las dos rutas de lectura

`consultarPerfilPorAuthUserId` no reimplementa nada: resuelve el perfil por `findByAuthUserId` y delega en `consultarPerfil(perfil.getId())`. Por construcción, `GET /perfil-proveedor/{id}` y `GET /perfil-proveedor/auth/{id}` devuelven el mismo `calificacionPromedio` calculado. No hay un segundo camino que pueda quedar desincronizado.

### Alcance del cambio y no-regresiones

El record `PerfilProveedorDetalleResponse` mantiene su firma (el campo `calificacionPromedio` ya existía); solo cambió el **valor** que se le pasa en el constructor, de `perfil.getCalificacionPromedio()` a `calcularCalificacionGeneral(especialidades)`. `crearPerfil` ahora inicializa el valor suelto del perfil en `3.5`, pero ese valor no se usa en el detalle (el detalle usa el promedio calculado) y no altera el esquema. `consultarPerfilPublico` quedó intacto y sigue usando la calificación de la especialidad puntual con fallback al valor del perfil — comportamiento previo, fuera del alcance de este cambio. La entidad mapea `calificacionPromedio` ↔ `calificacion_promedio` sin cambios de columna.

### Tests

Dos suites nuevas con Mockito puro sobre los puertos/servicios (seams de test estándar, no mocks de producción). Cubren el nacimiento en 3.5 y los cuatro casos del promedio. No se observan tests de integración web/MVC que ejerciten las dos rutas HTTP de extremo a extremo, pero dado que `consultarPerfilPorAuthUserId` delega literalmente en `consultarPerfil`, la cobertura unitaria del cálculo es suficiente para los criterios. El mensaje de commit registra `.\gradlew.bat clean bootJar -x test` → BUILD SUCCESSFUL y la ejecución focalizada de ambas suites → 5 tests OK; no re-ejecuté la build por instrucción.

</details>

<details>
<summary>Mapa de archivos</summary>

- `application/service/GestionEspecialidadesUseCaseImpl.java` — especialidad nueva nace con `calificacionPromedio = new BigDecimal("3.5")`.
- `application/service/GestionPerfilProveedorUseCaseImpl.java` — `consultarPerfil` usa `calcularCalificacionGeneral(especialidades)`; nuevo helper de promedio con fallback 3.5; `crearPerfil` inicializa el valor suelto en 3.5.
- `test/.../GestionEspecialidadesUseCaseImplTest.java` — verifica nacimiento en 3.5.
- `test/.../GestionPerfilProveedorUseCaseImplTest.java` — promedio, fallback, única especialidad, redondeo HALF_UP.

Diff completo: `git show c039435a`.

</details>
