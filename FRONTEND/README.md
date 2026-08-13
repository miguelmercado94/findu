# Portales Frontend y Guía de APIs de FIND-U (Clientes)

Guía completa de integración y documentación técnica para consumir las APIs del ecosistema **FIND-U** a través del **API Gateway** (`http://localhost:8080`).

---

## 🚀 Portales Frontend

| App | Directorio | Puerto | Rol Principal | Estado |
|-----|-----------|--------|---------------|--------|
| **Cliente** | `/FRONTEND/cliente` | `http://localhost:3000` | `ROLE_OUR_CLIENTE` | ✅ Producción / Afinado |
| **Proveedor** | `/FRONTEND/proveedor` | `http://localhost:3001` | `ROLE_OUR_PROVEEDOR` | 🚧 En desarrollo |

---

## 🏗️ Arquitectura de Comunicación

Todas las peticiones del frontend **deben dirigirse al API Gateway**:

```
┌─────────────────┐        ┌──────────────────┐        ┌────────────────────────────────┐
│  Frontend Client│──────▶ │   API Gateway    │──────▶ │  Eureka / Microservicios BD    │
│  (SPA / Móvil)  │  HTTP  │  localhost:8080  │  lb:// │  (Red Docker aislada)          │
└─────────────────┘        └──────────────────┘        └────────────────────────────────┘
```

- **Punto de Entrada Único**: `http://localhost:8080`
- **Autenticación**: `Bearer <jwt_token>` en la cabecera `Authorization`.
- **Rutas Gateway**:
  - `/security-auth/**` ➔ Microservicio `findu-spring-security` (Puerto interno 8081)
  - `/findu-core/**` ➔ Microservicio `findu-core` (Puerto interno 8082)

---

## 🔑 1. Autenticación y Registro (`findu-spring-security`)

### 1.1 Iniciar Sesión (`POST /security-auth/api/v1/auth/login`)
Soporta inicio de sesión con **Usuario / Correo** o **Teléfono Móvil**.

- **URL**: `http://localhost:8080/security-auth/api/v1/auth/login`
- **Autenticación**: Ninguna (Pública)
- **Request Body (Por Usuario/Correo)**:
```json
{
  "usernameOrEmail": "juan.perez",
  "password": "Password123*",
  "role": "ROLE_OUR_CLIENTE"
}
```
- **Request Body (Por Teléfono)**:
```json
{
  "phone": "3001234567",
  "codPhoneInternational": "+57",
  "password": "Password123*",
  "role": "ROLE_OUR_CLIENTE"
}
```
- **Respuesta Exitosa (200 OK)**:
```json
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "jwtRefresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "available": true
}
```

---

### 1.2 Autenticación Federada con Google (`POST /security-auth/api/v1/auth/federated`)
Permite registrar o iniciar sesión utilizando **Google Sign-In (OIDC)**.

- **URL**: `http://localhost:8080/security-auth/api/v1/auth/federated`
- **Autenticación**: Ninguna
- **Request Body**:
```json
{
  "providerName": "google",
  "providerUserId": "112627575228485860910",
  "email": "juan.perez@gmail.com",
  "username": "juan.perez",
  "role": "ROLE_OUR_CLIENTE"
}
```
- **Respuesta Exitosa (200 OK)**: Retorna tokens JWT.
- **Flujo Especial**: Si es la primera vez que se autentica con Google, el campo `phone` estará nulo. El frontend debe redirigir al formulario para solicitar el teléfono.

---

### 1.3 Registro de Usuario Cliente (`POST /security-auth/api/v1/customers`)
Registra la cuenta básica de seguridad del cliente (valida mayor de 18 años).

- **URL**: `http://localhost:8080/security-auth/api/v1/customers`
- **Autenticación**: Ninguna
- **Request Body**:
```json
{
  "username": "juan.perez",
  "email": "juan.perez@gmail.com",
  "phone": "3001234567",
  "codPhoneInternational": "+57",
  "password": "Password123*",
  "roleName": "ROLE_OUR_CLIENTE"
}
```
- **Respuesta Exitosa (201 Created)**:
```json
{
  "id": 5,
  "username": "juan.perez",
  "email": "juan.perez@gmail.com",
  "phone": "3001234567",
  "codPhoneInternational": "+57",
  "roleName": "ROLE_OUR_CLIENTE"
}
```

---

### 1.4 Obtener Perfil de Seguridad (`GET /security-auth/api/v1/profile`)
Obtiene los datos de la cuenta autenticada actual.

- **URL**: `http://localhost:8080/security-auth/api/v1/profile`
- **Headers**: `Authorization: Bearer <token>`
- **Respuesta Exitosa (200 OK)**:
```json
{
  "id": 5,
  "username": "juan.perez",
  "email": "juan.perez@gmail.com",
  "phone": "3001234567",
  "codPhoneInternational": "+57",
  "roleName": "ROLE_OUR_CLIENTE",
  "operationNames": ["AUTH_LOGIN", "PROFILE_READ", "PROFILE_UPDATE"]
}
```

---

### 1.5 Actualizar Perfil de Seguridad (`PUT /security-auth/api/v1/profile`)
Actualiza el teléfono o nombre de usuario en la cuenta de seguridad.

- **URL**: `http://localhost:8080/security-auth/api/v1/profile`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
```json
{
  "username": "juan.perez.updated",
  "phone": "3009876543",
  "codPhoneInternational": "+57"
}
```

---

### 1.6 Recuperación de Contraseña (`POST /forgot-password` & `/reset-password`)
- **Solicitar Código OTP**: `POST /security-auth/api/v1/auth/forgot-password`
```json
{ "email": "juan.perez@gmail.com" }
```
- **Confirmar Restablecimiento**: `POST /security-auth/api/v1/auth/reset-password`
```json
{
  "email": "juan.perez@gmail.com",
  "code": "123456",
  "newPassword": "NuevaPassword123*"
}
```
*(Nota: En perfil `dev`, el código stub es siempre `123456`).*

---

## 👤 2. Perfil de Cliente en Dominio (`findu-core`)

### 2.1 Consultar Perfil de Cliente por User ID (`GET /findu-core/api/v1/perfil-cliente/usuario/{authUserId}`)
Verifica si el usuario ya completó su perfil de dominio en `findu-core`.

- **URL**: `http://localhost:8080/findu-core/api/v1/perfil-cliente/usuario/5`
- **Headers**: `Authorization: Bearer <token>`
- **Respuesta Exitosa (200 OK)**:
```json
{
  "id": 1,
  "authUserId": 5,
  "username": "juan.perez",
  "email": "juan.perez@gmail.com",
  "nombreCompleto": "Juan Pérez",
  "numeroIdentificacion": "10456789",
  "tipoIdentificacion": "CC",
  "fechaNacimiento": "1995-05-15",
  "sexo": "M",
  "celular": "3001234567",
  "codPhoneInternational": "+57",
  "estado": "ACTIVO",
  "urlImagenPerfil": "data:image/png;base64,iVBORw0KGgo...",
  "direcciones": [ ... ]
}
```
- **Si el perfil no existe (404 Not Found)**: El frontend redirige a la vista de "Completar Perfil".

---

### 2.2 Crear / Completar Perfil de Cliente (`POST /findu-core/api/v1/perfil-cliente`)
Guarda la información personal requerida del cliente.

- **URL**: `http://localhost:8080/findu-core/api/v1/perfil-cliente`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
```json
{
  "authUserId": 5,
  "username": "juan.perez",
  "email": "juan.perez@gmail.com",
  "nombreCompleto": "Juan Pérez",
  "numeroIdentificacion": "10456789",
  "tipoIdentificacion": "CC",
  "fechaNacimiento": "1995-05-15",
  "sexo": "M",
  "celular": "3001234567",
  "codPhoneInternational": "+57",
  "urlImagenPerfil": "data:image/png;base64,iVBORw0KGgo..."
}
```

---

## 📍 3. Gestión de Direcciones — Estilo Mercado Libre (`findu-core`)

El sistema soporta múltiples direcciones por cliente. Cada dirección incluye municipio, piso, apartamento, referencias y etiquetas personalizadas. **Una única dirección se marca como principal**.

### 3.1 Listar Municipios Activos (`GET /findu-core/api/v1/direcciones/municipios`)
Obtiene la lista de municipios y departamentos para los desplegables de registro.

- **URL**: `http://localhost:8080/findu-core/api/v1/direcciones/municipios`
- **Respuesta (200 OK)**:
```json
[
  { "id": 1, "nombre": "Medellín", "departamento": "Antioquia" },
  { "id": 2, "nombre": "Cartagena", "departamento": "Bolívar" }
]
```

---

### 3.2 Listar Direcciones del Cliente (`GET /findu-core/api/v1/perfil-cliente/{clienteId}/direcciones`)
Obtiene todas las direcciones registradas del cliente.

- **URL**: `http://localhost:8080/findu-core/api/v1/perfil-cliente/1/direcciones`
- **Headers**: `Authorization: Bearer <token>`
- **Respuesta (200 OK)**:
```json
[
  {
    "id": 10,
    "etiqueta": "Domicilio residencial",
    "direccionTexto": "Calle 7c # 64a-30 casa 1",
    "municipioId": 1,
    "municipioNombre": "Medellín",
    "departamento": "Antioquia",
    "piso": "1",
    "apartamento": "101",
    "referencia": "Vereda carrizales 17 km via palmas",
    "esPrincipal": true
  },
  {
    "id": 12,
    "etiqueta": "Trabajo / Oficina",
    "direccionTexto": "Carrera 43A # 1-50 Envy Building",
    "municipioId": 1,
    "municipioNombre": "Medellín",
    "departamento": "Antioquia",
    "piso": "8",
    "apartamento": "802",
    "referencia": "Piso 8 frente a los ascensores",
    "esPrincipal": false
  }
]
```

---

### 3.3 Crear Dirección (`POST /findu-core/api/v1/perfil-cliente/{clienteId}/direcciones`)
- **URL**: `http://localhost:8080/findu-core/api/v1/perfil-cliente/1/direcciones`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
```json
{
  "etiqueta": "Casa Familiar",
  "direccionTexto": "Cl. 16 21-24",
  "municipioId": 2,
  "latitud": 10.391,
  "longitud": -75.479,
  "piso": "2",
  "apartamento": null,
  "referencia": "Cerca al parque central",
  "esPrincipal": false
}
```

---

### 3.4 Actualizar Dirección (`PUT /findu-core/api/v1/direcciones/{direccionId}`)
- **URL**: `http://localhost:8080/findu-core/api/v1/direcciones/12`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**: Mismo formato que crear dirección.

---

### 3.5 Marcar Dirección como Principal (`PATCH /findu-core/api/v1/direcciones/{direccionId}/principal`)
Establece esta dirección como la principal del cliente y desmarca la anterior automáticamente.

- **URL**: `http://localhost:8080/findu-core/api/v1/direcciones/12/principal`
- **Headers**: `Authorization: Bearer <token>`
- **Respuesta (200 OK)**: Objeto de dirección actualizado.

---

### 3.6 Eliminar Dirección (`DELETE /findu-core/api/v1/direcciones/{direccionId}`)
Elimina una dirección secundaria (la dirección principal no puede ser eliminada).

- **URL**: `http://localhost:8080/findu-core/api/v1/direcciones/12`
- **Headers**: `Authorization: Bearer <token>`
- **Respuesta (204 No Content)**

---

## 🗂️ 4. Catálogo de Servicios y Categorías (Caché Redis) (`findu-core`)

Los datos del catálogo están optimizados con **Redis Cache** (`@Cacheable`) para responder en milisegundos.

### 4.1 Listar Categorías Activas con Servicios (`GET /findu-core/api/v1/categorias?page=0&size=50`)
Retorna **únicamente** las categorías principales que tienen servicios activos configurados.

- **URL**: `http://localhost:8080/findu-core/api/v1/categorias?page=0&size=50`
- **Autenticación**: Pública
- **Respuesta (200 OK)**:
```json
{
  "content": [
    { "id": 1, "nombre": "Salud y Bienestar", "descripcion": "Servicios de salud y enfermería a domicilio", "active": true },
    { "id": 2, "nombre": "Hogar", "descripcion": "Servicios de mantenimiento y limpieza del hogar", "active": true },
    { "id": 3, "nombre": "Belleza y Estética", "descripcion": "Servicios de peluquería, manicure y maquillaje", "active": true }
  ],
  "totalElements": 10,
  "totalPages": 1
}
```

---

### 4.2 Listar Servicios por Categoría (`GET /findu-core/api/v1/servicios?categoriaId={id}`)
Filtra los servicios directamente por la categoría principal seleccionada.

- **URL**: `http://localhost:8080/findu-core/api/v1/servicios?categoriaId=1`
- **Autenticación**: Pública
- **Respuesta (200 OK)**:
```json
{
  "content": [
    {
      "id": 1,
      "nombre": "Enfermería a Domicilio",
      "descripcion": "Atención de enfermería profesional en casa: curaciones, inyecciones, control de signos",
      "tipoCobro": "POR_HORA",
      "urlImagen": null
    },
    {
      "id": 4,
      "nombre": "Fisioterapia Rehabilitación",
      "descripcion": "Sesiones de rehabilitación física muscular y articular",
      "tipoCobro": "POR_SERVICIO",
      "urlImagen": null
    }
  ]
}
```

---

## 🪄 5. Wizard Multi-Paso de Solicitud de Servicio

La interfaz del cliente incluye un **Wizard de 4 Pasos** para generar solicitudes de servicio completas:

1. **Paso 1 — Dirección**: Selección de la dirección de atención mediante tarjetas Mercado Libre, edición inline o agregación de nuevas direcciones al instante.
2. **Paso 2 — Detalles & Presupuesto**:
   - Fecha deseada (`YYYY-MM-DD`) y Hora estimada (`HH:mm`).
   - Duración máxima en horas (si el tipo de cobro es `POR_HORA`).
   - Presupuesto máximo estimado (COP).
   - Indicaciones y detalles del problema.
3. **Paso 3 — Fotos de Referencia (Máximo 10 Fotos)**:
   - Carga de hasta 10 fotos del daño o espacio.
   - Codificación automática a Base64 para subida a buckets S3.
   - Rejilla interactiva con opción para eliminar fotos individuales.
4. **Paso 4 — Resumen y Publicación**: Confirmación final y envío de la solicitud.

---

## 🛠️ Ejecución Local

### Requisitos:
- Node.js (v18+)
- Backend corriendo via Docker Compose (`docker compose up -d`)

### Iniciar Portal Cliente:
```bash
cd FRONTEND/cliente
npm install
npm run dev    # Servidor iniciado en http://localhost:3000
```
