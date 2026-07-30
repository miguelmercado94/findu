# findu-core

Microservicio principal de FINDU. Gestiona perfiles de clientes y proveedores, direcciones, solicitudes de servicio, ofertas (bidding), facturación y calificaciones.

---

## Configuración

| Propiedad | Valor |
|-----------|-------|
| Puerto | 8082 |
| Context path | `/findu-core` |
| Swagger UI | http://localhost:8082/findu-core/swagger-ui.html |
| Perfil default | `local` (H2 en memoria) |

---

## Ejecución Local

```bash
cd CORE/findu-core
./gradlew bootRun    # Arranca con H2, sin necesidad de Docker
```

Consola H2: http://localhost:8082/findu-core/h2-console (JDBC URL: `jdbc:h2:mem:findu_core`)

---

## APIs Implementadas

Base URL: `http://localhost:8082/findu-core`

### Perfil Cliente

| Método | Endpoint | Descripción | Estado resultante |
|--------|----------|-------------|-------------------|
| POST | `/api/v1/perfil-cliente` | Crear perfil | INCOMPLETO |
| POST | `/api/v1/perfil-cliente/{id}/direcciones` | Crear dirección (activa perfil si es principal) | ACTIVO |
| GET | `/api/v1/perfil-cliente/{id}` | Consultar perfil detallado con direcciones | — |
| GET | `/api/v1/perfil-cliente/{id}/direcciones` | Listar direcciones del cliente | — |
| PUT | `/api/v1/perfil-cliente/{id}` | Actualizar datos libres (nombre, imagen) | — |
| PATCH | `/api/v1/perfil-cliente/{id}/estado` | Cambiar estado (ACTIVO/INACTIVO) | — |

#### Flujo de registro completo:

```
1. POST /api/v1/perfil-cliente          → Perfil creado (estado: INCOMPLETO)
2. POST /api/v1/perfil-cliente/1/direcciones  → Dirección principal creada (estado: ACTIVO)
```

#### POST /api/v1/perfil-cliente — Crear Perfil

```json
{
  "authUserId": 4,
  "username": "m.mercado.t.94",
  "email": "m.mercado.t.94@gmail.com",
  "nombreCompleto": "Miguel Angel Mercado Tirado",
  "numeroIdentificacion": "1045678901",
  "tipoIdentificacion": "CC",
  "fechaNacimiento": "1994-03-15",
  "sexo": "M",
  "celular": "3003763300",
  "codPhoneInternational": "+57"
}
```

Respuesta 201:
```json
{
  "id": 1,
  "username": "m.mercado.t.94",
  "email": "m.mercado.t.94@gmail.com",
  "nombreCompleto": "Miguel Angel Mercado Tirado",
  "celular": "3003763300",
  "codPhoneInternational": "+57",
  "sexo": "M",
  "urlImagenPerfil": null,
  "calificacionPromedio": null,
  "estado": "INCOMPLETO"
}
```

#### POST /api/v1/perfil-cliente/{id}/direcciones — Crear Dirección

```json
{
  "etiqueta": "Casa",
  "direccionTexto": "Cra 45 #67-89, Barrio El Poblado",
  "municipioId": 2,
  "latitud": 6.2086,
  "longitud": -75.5659,
  "piso": "3",
  "apartamento": "301",
  "referencia": "Edificio Torres del Parque, portería principal",
  "esPrincipal": true
}
```

Respuesta 201:
```json
{
  "id": 1,
  "etiqueta": "Casa",
  "direccionTexto": "Cra 45 #67-89, Barrio El Poblado",
  "municipioNombre": "Medellín",
  "latitud": 6.2086,
  "longitud": -75.5659,
  "piso": "3",
  "apartamento": "301",
  "referencia": "Edificio Torres del Parque, portería principal",
  "esPrincipal": true
}
```

> Al crear la dirección con `esPrincipal: true`, el perfil pasa automáticamente de INCOMPLETO → ACTIVO.

---

### Direcciones (operaciones independientes)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| PUT | `/api/v1/direcciones/{id}` | Actualizar dirección existente |
| DELETE | `/api/v1/direcciones/{id}` | Eliminar dirección (rechaza si es la última) |
| GET | `/api/v1/direcciones/{id}` | Consultar dirección por ID |

---

### Catálogos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/categorias` | Listar categorías de servicio |
| GET | `/api/v1/servicios?categoriaId={id}` | Servicios por categoría |
| GET | `/api/v1/direcciones/municipios` | Listar municipios disponibles |

---

### Estados del Perfil

| Estado | Significado |
|--------|-------------|
| `INCOMPLETO` | Perfil recién creado, sin dirección principal |
| `ACTIVO` | Perfil completo, puede operar en la plataforma |
| `INACTIVO` | Perfil desactivado por el usuario |

---

### Códigos de Error

| HTTP | Cuándo |
|------|--------|
| 400 | Campos requeridos faltantes o formato inválido |
| 404 | Perfil, dirección o municipio no encontrado |
| 409 | Perfil duplicado, restricción de negocio violada |
| 500 | Error interno inesperado |
