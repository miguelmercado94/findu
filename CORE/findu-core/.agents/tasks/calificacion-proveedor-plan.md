# Plan de implementacion: calificacion inicial y agregada del proveedor (findu-core, Opcion A)

Cambio SOLO backend `findu-core`. Arquitectura hexagonal, Spring Boot 3.4.2, Java 21, Spring Data JPA, MapStruct 1.6.3, Lombok. JUnit 5 (`spring-boot-starter-test`).

Build (Windows PowerShell, desde el modulo `findu\CORE\findu-core`):

```
.\gradlew.bat clean bootJar -x test --no-daemon
```

Tests (desde el modulo):

```
.\gradlew.bat test --no-daemon
```

## Hallazgos de la auditoria (confirmados leyendo el codigo)

1. `GestionEspecialidadesUseCaseImpl.agregarEspecialidad(...)` construye `PerfilEspecialista.builder()` SIN `calificacionPromedio` -> el modelo de dominio queda con `calificacionPromedio = null`.
2. Mapeo dominio->entidad: `PerfilEspecialistaMapper` (MapStruct, `toEntity`) mapea el campo `calificacionPromedio` por nombre. Como el dominio llega en `null`, MapStruct ejecuta `entity.setCalificacionPromedio(null)` y SOBREESCRIBE el `@Builder.Default = new BigDecimal("5.00")` de `PerfilEspecialistaEntity`. Resultado actual: el INSERT persiste `null` (o el default de columna) en `calificacion_promedio`. **Conclusion:** inicializar `calificacionPromedio = 3.5` en el dominio en `agregarEspecialidad` es suficiente para que el INSERT incluya 3.5; no hace falta tocar la entidad ni el mapper. La columna ya mapea `calificacionPromedio <-> calificacion_promedio` (`@Column(name = "calificacion_promedio")`).
3. `GestionPerfilProveedorUseCaseImpl.consultarPerfil(Long id)` arma `PerfilProveedorDetalleResponse` pasando `perfil.getCalificacionPromedio()` DIRECTO (valor suelto de la tabla perfil). Hay que reemplazarlo por el promedio calculado de las especialidades activas.
4. Las especialidades que se listan en `consultarPerfil` vienen de `perfilEspecialistaService.findByProveedorId(id)`, que a nivel repositorio es `findAllByPerfilProveedorIdAndActiveTrue(...)` -> **ya son solo las ACTIVAS**. Por tanto el promedio se calcula sobre la misma lista `especialidades` que ya se construye en el metodo, sin consultas extra.
5. `consultarPerfilPorAuthUserId(Long authUserId)` **reutiliza** `consultarPerfil(perfil.getId())` (llamada directa). Por tanto hereda el calculo automaticamente; NO requiere cambios. `GET /auth/{id}` y `GET /{id}` quedan consistentes por construccion.
6. `crearPerfil(...)` construye `PerfilProveedor` sin `calificacionPromedio`. Decision (ver abajo): inicializarlo a 3.5 por consistencia visual del valor suelto, aunque el detalle ya no lo use.
7. `consultarPerfilPublico(...)` usa `especialidad.getCalificacionPromedio()` de UNA especialidad concreta (fallback a `perfil.getCalificacionPromedio()`). Esta FUERA del alcance (es por-servicio, no el badge general del Perfil). NO se toca; se beneficia indirectamente de que las nuevas especialidades ya traen 3.5.
8. DTOs/records `PerfilProveedorDetalleResponse`, `PerfilEspecialistaResponse`, `PerfilEspecialista` ya tienen `BigDecimal calificacionPromedio`. NO cambia ninguna firma; solo cambia el VALOR que se pasa.

## Decisiones de diseno

- **Inicializacion de especialidad = 3.5**: se setea en el modelo de dominio en `agregarEspecialidad` con `new BigDecimal("3.5")`. Se elige el dominio (no la entidad) porque el mapper sobreescribe el default de la entidad con el valor del dominio; es el unico punto que garantiza el 3.5 persistido sin tocar persistencia.
- **Calificacion general = promedio de especialidades activas, calculada al leer**: no se persiste. Formula: media aritmetica de los `calificacionPromedio` no nulos de las especialidades activas, redondeada con `setScale(1, RoundingMode.HALF_UP)`.
- **Fallback 3.5**: si el proveedor no tiene especialidades activas (proveedor nuevo o lista vacia), el promedio general es `new BigDecimal("3.5")` con scale 1 (`"3.5"`). Razon: un proveedor sin especialidades parte del mismo piso que una especialidad nueva, evitando mostrar `null`/`0` en el badge de estrellas.
- **Especialidades con `calificacionPromedio` null**: se ignoran en el promedio (filtro `!= null`). Si TODAS son null, aplica el mismo fallback 3.5. Esto cubre datos legacy previos al fix del punto 2.
- **`crearPerfil` -> perfil.calificacionPromedio = 3.5**: inicializacion por consistencia. No afecta el badge (el detalle usa el promedio calculado), pero deja el valor suelto coherente con el piso de negocio. Decision de bajo riesgo; `PerfilProveedorResponse` seguira exponiendo este valor suelto en create/update, lo cual es aceptable.
- **Helper del promedio**: se agrega un metodo privado `calcularCalificacionGeneral(List<PerfilEspecialistaResponse> especialidades)` en `GestionPerfilProveedorUseCaseImpl` para que `consultarPerfil` lo use y el calculo quede en un solo lugar testeable.

## Items (ordenados por dependencia)

- [ ] 1. Inicializar la calificacion de la especialidad nueva en 3.5.
      En `agregarEspecialidad(...)`, agregar `.calificacionPromedio(new BigDecimal("3.5"))` al `PerfilEspecialista.builder()`. Anadir `import java.math.BigDecimal;`. El `PerfilEspecialistaResponse` de retorno ya pasa `saved.getCalificacionPromedio()`, por lo que reflejara 3.5 tras el save.
      Files: `src/main/java/com/findu/core/application/service/GestionEspecialidadesUseCaseImpl.java`
      Verify: `.\gradlew.bat clean bootJar -x test --no-daemon` compila sin errores; los tests del item 4 (caso inicializacion) pasan en `.\gradlew.bat test --no-daemon`.

- [ ] 2. Inicializar `calificacionPromedio = 3.5` en el perfil al crear (consistencia).
      En `crearPerfil(...)`, agregar `.calificacionPromedio(new BigDecimal("3.5"))` al `PerfilProveedor.builder()`. Anadir `import java.math.BigDecimal;` en el archivo si no existe. No cambia firmas.
      Files: `src/main/java/com/findu/core/application/service/GestionPerfilProveedorUseCaseImpl.java`
      Verify: incluido en el build/test de los items siguientes (mismo archivo que item 3).

- [ ] 3. Calcular la calificacion general como promedio de especialidades activas en el detalle.
      En el mismo `GestionPerfilProveedorUseCaseImpl`: (a) agregar metodo privado `BigDecimal calcularCalificacionGeneral(List<PerfilEspecialistaResponse> especialidades)` que toma los `calificacionPromedio` no nulos, calcula la media (`reduce` con `BigDecimal.add` dividido por el conteo, o `divide(count, 1, RoundingMode.HALF_UP)`), aplica `setScale(1, RoundingMode.HALF_UP)`, y devuelve `new BigDecimal("3.5").setScale(1, RoundingMode.HALF_UP)` si la lista efectiva esta vacia. (b) En `consultarPerfil(...)`, reemplazar el argumento `perfil.getCalificacionPromedio()` del constructor de `PerfilProveedorDetalleResponse` por `calcularCalificacionGeneral(especialidades)` (la lista ya construida de activas). Anadir `import java.math.RoundingMode;`.
      Files: `src/main/java/com/findu/core/application/service/GestionPerfilProveedorUseCaseImpl.java`
      Verify: `.\gradlew.bat clean bootJar -x test --no-daemon` compila; tests del item 4 (casos promedio y fallback) pasan.
      Nota de dependencia: usa el metodo del mismo item; `consultarPerfilPorAuthUserId` NO se edita (reusa `consultarPerfil`).

- [ ] 4. Anadir tests unitarios del calculo (JUnit 5, sin arrancar Spring; mock de los services con Mockito, ya disponible via spring-boot-starter-test).
      Crear `GestionPerfilProveedorUseCaseImplTest` que: instancia el use case con dependencias mockeadas (`PerfilProveedorService`, `PerfilEspecialistaService`, `DireccionService`, `ProveedorCoberturaService`, `MunicipioRepositoryPort`, `ServicioRepositoryPort`, etc.), stubea `findById` para devolver un `PerfilProveedor`, y stubea `perfilEspecialistaService.findByProveedorId(id)` para devolver listas de `PerfilEspecialista` activos. Casos: (a) especialidades [3.5, 4.5] -> `consultarPerfil(...).calificacionPromedio()` == `new BigDecimal("4.0")`; (b) sin especialidades activas (lista vacia) -> `3.5`; (c) una sola [4.3] -> `4.3`; (d) [3.5, 4.0, 5.0] -> `4.2` (promedio 4.1666 -> HALF_UP 4.2). Comparar con `compareTo == 0` o normalizando scale a 1. Crear tambien `GestionEspecialidadesUseCaseImplTest` que verifica que `agregarEspecialidad(...)` guarda una especialidad con `calificacionPromedio` == `3.5` (capturar el argumento pasado a `especialistaService.save(...)` con `ArgumentCaptor` y stubear el retorno).
      Files: `src/test/java/com/findu/core/application/service/GestionPerfilProveedorUseCaseImplTest.java`, `src/test/java/com/findu/core/application/service/GestionEspecialidadesUseCaseImplTest.java`
      Verify: `.\gradlew.bat test --no-daemon` -> todos los tests nuevos pasan (GREEN).

- [ ] 5. Verificacion final de build empaquetado.
      Ejecutar el build de empaquetado completo para confirmar que el modulo sigue construyendo el jar.
      Files: (ninguno)
      Verify: `.\gradlew.bat clean bootJar -x test --no-daemon` termina con `BUILD SUCCESSFUL` y genera el bootJar.

## Notas / supuestos

- MapStruct regenera el mapper en compilacion; no se edita porque el fix se hace en el dominio (item 1). Si en el futuro se quiere forzar el 3.5 a nivel entidad, habria que anotar el mapeo, pero NO es necesario para esta tarea.
- No se tocan rutas, controllers ni otros microservicios. No se introducen mocks en codigo de produccion. Los mocks viven solo en `src/test`.
- `consultarPerfilPublico` queda intacto (fuera de alcance: es calificacion por servicio, no el badge general).
