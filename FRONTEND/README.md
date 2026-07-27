# Portales Frontend de FIND-U (Clientes y Proveedores)

Este directorio contiene las aplicaciones frontend SPA de **FIND-U** desarrolladas sobre **JavaScript Vanilla** y empaquetadas con **Vite**:

1. **Cliente (`/cliente`)**: Portal de acceso para usuarios que consumen servicios.
2. **Proveedor (`/proveedor`)**: Portal de acceso para proveedores que ofrecen servicios.

Ambos portales comparten la misma lógica de negocio para autenticación local, recuperación de contraseñas e integración con inicios de sesión federados.

---

## ⚙️ Variables de Entorno (`.env`)

Antes de iniciar cualquier aplicación, debes configurar las siguientes variables de entorno creando un archivo `.env` en las carpetas `/cliente` y `/proveedor`:

```env
# ID de Cliente para la integración con Google Sign-In
VITE_GOOGLE_CLIENT_ID=tu_cliente_id_google.apps.googleusercontent.com

# URL base del API Gateway
VITE_API_GATEWAY_URL=http://localhost:8080
```

---

## 🚦 Consumo de APIs en el Frontend

Todas las llamadas se dirigen a través del API Gateway (`http://localhost:8080`):

### 1. Autenticación Local
* **Endpoint**: `POST /security-auth/api/v1/auth/login`
* **Lógica**: Envía las credenciales y el rol asignado (`ROLE_OUR_CLIENTE` o `ROLE_OUR_PROVEEDOR`). Si la respuesta es exitosa, almacena `jwt`, `jwtRefresh` y el `role` en `localStorage`.

### 2. Registro de Usuario
* **Endpoint**: `POST /security-auth/api/v1/customers`
* **Lógica**: Envía el formulario de registro. En caso de conflicto de datos (ej. correo duplicado), notifica en la interfaz de usuario con un mensaje amigable.

### 3. Recuperación de Contraseña
* **Endpoint**: `POST /security-auth/api/v1/auth/forgot-password` (Solicitud de código)
* **Endpoint**: `POST /security-auth/api/v1/auth/reset-password` (Confirmación de nueva contraseña)
* **Lógica**:
  1. El usuario solicita un código de 6 dígitos ingresando su correo o celular.
  2. El sistema backend emite el código (el cual se visualiza en la consola backend localmente como stub: `123456`).
  3. El usuario ingresa el código de 6 dígitos recibido y su nueva contraseña para completar el cambio.

### 4. Consulta de Perfil y Operaciones
* **Endpoint**: `GET /security-auth/api/v1/profile`
* **Lógica**: Se envía el token en la cabecera `Authorization: Bearer [token]`. Retorna los datos básicos y los permisos/operaciones autorizadas para el usuario.

---

## 🌐 Integración con Inicios de Sesión Federados (Google Sign-In)

El flujo de inicio de sesión con Google funciona bajo el estándar **OpenID Connect (OIDC)**:

1. **Carga del Script**: El frontend carga el script oficial de Google (`https://accounts.google.com/gsi/client`).
2. **Renderización del Botón**: Se inicializa el botón con el ID de cliente provisto en las variables de entorno (`VITE_GOOGLE_CLIENT_ID`).
3. **Recepción del Credential Token (JWT)**: Tras el inicio de sesión exitoso por el usuario en el popup de Google, la librería retorna un `credential` token (un JWT firmado por Google).
4. **Decodificación local**: El frontend decodifica este token (usando su payload base64) para extraer los campos del perfil:
   * ID de usuario de Google (`sub`).
   * Correo electrónico (`email`).
   * Nombre de usuario (`name`).
5. **Autenticación en el Backend**: Envía esta información al endpoint federado de FIND-U:
   * **Endpoint**: `POST /security-auth/api/v1/auth/federated`
   * **Payload**:
     ```json
     {
       "providerName": "google",
       "providerUserId": "sub_de_google",
       "email": "email_de_google",
       "username": "nombre_de_google",
       "role": "ROLE_OUR_CLIENTE"
     }
     ```
   * **Resultado**: El backend valida el usuario (y crea una cuenta automáticamente si es el primer inicio de sesión) y retorna las credenciales JWT de FIND-U para iniciar sesión en la SPA.

---

## 🏃 Lanzamiento en Local

### Requisitos previos:
* Node.js instalado en el sistema.

### Instrucciones de inicio:
```bash
# Entrar a la carpeta
cd FRONTEND/cliente  # o proveedor

# Instalar dependencias
npm install

# Lanzar servidor de desarrollo
npm run dev
```
La aplicación cliente se abrirá en `http://localhost:3000` y la aplicación proveedor en `http://localhost:3001` (según configuraciones de puerto de Vite).
