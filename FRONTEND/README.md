# Portales Frontend de FIND-U (Clientes y Proveedores)

Aplicaciones frontend SPA de **FIND-U** desarrolladas con **JavaScript Vanilla** + **Vite**:

| App | Directorio | Puerto | Rol |
|-----|-----------|--------|-----|
| Cliente | `/cliente` | 3000 | `ROLE_OUR_CLIENTE` |
| Proveedor | `/proveedor` | 3001 | `ROLE_OUR_PROVEEDOR` |

Ambos portales comparten la misma lógica de autenticación, recuperación de contraseñas e integración con Google Sign-In. Son la referencia para implementar un frontend móvil.

---

## Arquitectura de Comunicación

```
┌──────────────┐        ┌──────────────────┐        ┌──────────────────────────┐
│  Frontend    │──────▶ │  API Gateway     │──────▶ │  Microservicios          │
│  (Vite SPA)  │  HTTP  │  localhost:8080   │  lb:// │  (red Docker interna)    │
└──────────────┘        └──────────────────┘        └──────────────────────────┘
```

- El frontend **solo habla con el API Gateway** (`http://localhost:8080`).
- El gateway enruta por path prefix a cada microservicio via Eureka.
- Los microservicios y BDs **no tienen puertos expuestos** al host.

---

## Variables de Entorno (`.env`)

Crear un archivo `.env` en cada carpeta (`/cliente` y `/proveedor`):

```env
# ID de Cliente OAuth2 para Google Sign-In (consola Google Cloud)
VITE_GOOGLE_CLIENT_ID=tu_cliente_id.apps.googleusercontent.com

# URL base del API Gateway (punto de entrada único para todas las APIs)
VITE_API_GATEWAY_URL=http://localhost:8080
```

> El Google Client ID debe tener `http://localhost:3000` y `http://localhost:3001` como orígenes autorizados en Google Cloud Console.

---

## Endpoints del Backend (via Gateway)

Todas las rutas van prefijadas con `/security-auth/api/v1/`.

### Autenticación

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/auth/login` | No | Login con usuario/correo + contraseña |
| POST | `/auth/federated` | No | Login/registro federado (Google) |
| POST | `/auth/refresh` | No | Renovar access token con refresh token |
| POST | `/auth/logout` | Bearer | Cerrar sesión (revocar tokens) |

### Registro

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/customers` | No | Registrar nuevo usuario (todos los campos obligatorios) |

### Perfil

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/profile` | Bearer | Obtener perfil del usuario autenticado |
| PUT | `/profile` | Bearer | Actualizar perfil (username, phone, codPhoneInternational) |

### Recuperación de Contraseña

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/auth/forgot-password` | No | Solicitar código de recuperación (6 dígitos) |
| POST | `/auth/reset-password` | No | Confirmar nueva contraseña con código |

---

## Flujo de Google Sign-In (OIDC)

### Primera vez (usuario no existe):

```
1. Click "Iniciar sesión con Google"
2. Google devuelve credential JWT con: sub, email, given_name, family_name
3. Frontend llama POST /auth/federated → Backend crea usuario SIN teléfono → devuelve JWT
4. Frontend verifica GET /profile → phone es null
5. Frontend muestra formulario de registro para completar teléfono
6. Usuario ingresa teléfono → PUT /profile actualiza el dato
7. Frontend muestra la vista de perfil completa
```

### Segunda vez (usuario ya existe):

```
1. Click "Iniciar sesión con Google"
2. Google devuelve credential JWT
3. Frontend llama POST /auth/federated → Backend reconoce usuario → devuelve JWT
4. Frontend verifica GET /profile → phone tiene valor
5. Frontend muestra la vista de perfil directamente
```

### Payload de `/auth/federated`:

```json
{
  "providerName": "google",
  "providerUserId": "112627575228485860910",
  "email": "usuario@gmail.com",
  "username": "usuario",
  "role": "ROLE_OUR_CLIENTE"
}
```

### Respuesta exitosa:

```json
{
  "jwt": "eyJhbG...",
  "jwtRefresh": "eyJhbG...",
  "available": true
}
```

---

## Payload de Registro (`POST /customers`)

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

> Para registro via Google, el password se genera internamente (`GoogleAccountLinked123*`).

---

## Payload de Actualizar Perfil (`PUT /profile`)

```json
{
  "username": "juan.perez",
  "phone": "3001234567",
  "codPhoneInternational": "+57"
}
```

Header requerido: `Authorization: Bearer <access_token>`

---

## Respuesta de Perfil (`GET /profile`)

```json
{
  "username": "juan.perez",
  "email": "juan.perez@gmail.com",
  "phone": "3001234567",
  "codPhoneInternational": "+57",
  "roleName": "ROLE_OUR_CLIENTE",
  "operationNames": [
    "AUTH_LOGIN",
    "AUTH_FEDERATED",
    "AUTH_REFRESH",
    "AUTH_LOGOUT",
    "CUST_LIST",
    "CUST_REGISTER",
    "PROFILE_READ",
    "PROFILE_UPDATE",
    "AUTH_FORGOT_PASSWORD",
    "AUTH_RESET_PASSWORD",
    "HEALTH_ACTUATOR"
  ]
}
```

---

## Manejo de Tokens (localStorage)

```javascript
// Guardar después de login/registro exitoso
localStorage.setItem('findu_token', data.jwt);

// Enviar en cada request protegido
headers: { 'Authorization': `Bearer ${localStorage.getItem('findu_token')}` }

// Logout: limpiar storage
localStorage.removeItem('findu_token');
```

---

## Recuperación de Contraseña

### Paso 1 — Solicitar código:

```json
// Por email
{ "email": "usuario@gmail.com" }

// Por teléfono
{ "phone": "3001234567", "codPhoneInternational": "+57" }
```

### Paso 2 — Restablecer con código:

```json
{
  "code": "123456",
  "newPassword": "NuevaPassword123*",
  "email": "usuario@gmail.com"
}
```

> En entorno dev, el código siempre es `123456` (stub del backend).

---

## Lanzamiento Local

### Requisitos:
- Node.js (v18+)
- Backend corriendo (ver `docker-compose.yml` en la raíz del proyecto)

### Pasos:

```bash
# Terminal 1 — Backend
cd findu
docker compose up -d

# Terminal 2 — Frontend cliente
cd FRONTEND/cliente
npm install
npm run dev    # → http://localhost:3000

# Terminal 3 — Frontend proveedor
cd FRONTEND/proveedor
npm install
npm run dev    # → http://localhost:3001
```

---

## Notas para el Frontend Móvil

- Usa los mismos endpoints documentados arriba.
- El `VITE_API_GATEWAY_URL` equivale a la base URL que configurarás en la app móvil.
- Para Google Sign-In en móvil, usa el SDK nativo (Google Sign-In para Android/iOS) y envía el `idToken` al mismo endpoint `/auth/federated`.
- El `providerUserId` es el campo `sub` del token de Google.
- El flujo de "completar teléfono" aplica igual: después del primer login federado, verificar si `phone` es null en `/profile` y pedir al usuario que lo complete via `PUT /profile`.
- Los tokens tienen 1 hora de expiración (access) y 7 días (refresh).
