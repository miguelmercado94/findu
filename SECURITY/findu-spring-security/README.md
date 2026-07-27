# findu-spring-security

Servicio de autenticación y autorización principal de **FIND-U**. API reactiva construida sobre **Spring WebFlux**, persistencia reactiva **R2DBC** sobre **PostgreSQL**, emisión/validación de **JWT** locales y federados, y soporte modular de caché de revocación de tokens en **Redis** y persistencia en **DynamoDB** (local o en AWS).

---

## 📁 Estructura y Arquitectura

El código sigue los patrones de la **Arquitectura Hexagonal (Limpia)**, asegurando que las reglas de dominio y los puertos estén totalmente aislados de la infraestructura:

* **Presentación** (`com.findu.security.presentation`): Controladores reactivos (WebFlux) y DTOs de request/response.
* **Aplicación** (`com.findu.security.application`): Casos de uso y puertos de salida.
* **Dominio** (`com.findu.security.domain`): Entidades de negocio, encriptación y lógica pura.
* **Infraestructura** (`com.findu.security.infrastructure`): Adaptadores R2DBC (PostgreSQL), clientes para AWS DynamoDB, caché de Redis y logs.

---

## 🚦 Endpoints Detallados

### 1. Iniciar Sesión (Login)
* **Endpoint**: `POST /api/v1/auth/login`
* **Cabeceras**:
  * `X-JWT-Algorithm`: Tipo de algoritmo para firmar el token (opcional, ej. `HS256`).
* **Request Payload (JSON)**:
  * Iniciar sesión con **Usuario / Correo**:
    ```json
    {
      "usernameOrEmail": "m.mercado.t.94@gmail.com",
      "password": "Password123*",
      "role": "ROLE_OUR_CLIENTE"
    }
    ```
  * Iniciar sesión con **Celular / Teléfono**:
    ```json
    {
      "phone": "3001234567",
      "codPhoneInternational": "+57",
      "password": "Password123*",
      "role": "ROLE_OUR_CLIENTE"
    }
    ```
* **Response Payload (200 OK - JSON)**:
  ```json
  {
    "jwt": "eyJhbGciOiJIUzI1NiJ9...",
    "jwtRefresh": "eyJhbGciOiJIUzI1NiJ9...",
    "available": true
  }
  ```
* **Errores posibles**:
  * `400 Bad Request`: Datos de entrada inválidos (falta contraseña, rol incorrecto).
  * `401 Unauthorized`: Credenciales incorrectas, usuario no encontrado, contraseña inválida.
  * `404 Not Found`: Rol especificado no existe en el sistema.

---

### 2. Registrar Cliente/Proveedor
* **Endpoint**: `POST /api/v1/customers`
* **Request Payload (JSON)**:
  ```json
  {
    "username": "m.mercado.t.94",
    "email": "m.mercado.t.94@gmail.com",
    "phone": "3001234567",
    "codPhoneInternational": "+57",
    "password": "Password123*",
    "roleName": "ROLE_OUR_CLIENTE"
  }
  ```
* **Response Payload (201 Created - JSON)**: Retorna la entidad del usuario registrado (sin contraseña).
* **Errores posibles**:
  * `400 Bad Request`: Formato de correo inválido, contraseña débil o campos faltantes.
  * `409 Conflict`: El nombre de usuario, correo o teléfono móvil ya está registrado por otra cuenta.

---

### 3. Solicitar Código de Recuperación
* **Endpoint**: `POST /api/v1/auth/forgot-password`
* **Request Payload (JSON)**:
  * Por **Email**:
    ```json
    {
      "email": "m.mercado.t.94@gmail.com"
    }
    ```
  * Por **Celular**:
    ```json
    {
      "phone": "3001234567",
      "codPhoneInternational": "+57"
    }
    ```
* **Response Payload (204 No Content)**: Respuesta vacía si el proceso se inicia (incluso si el usuario no existe para prevenir enumeración).
* **Logs del Backend**: Escribe en consola el código de recuperación autogenerado (stub: `123456`).

---

### 4. Restablecer Contraseña
* **Endpoint**: `POST /api/v1/auth/reset-password`
* **Request Payload (JSON)**:
  * Por **Código de 6 dígitos**:
    ```json
    {
      "email": "m.mercado.t.94@gmail.com",
      "code": "123456",
      "newPassword": "NewPassword123*"
    }
    ```
* **Response Payload (204 No Content)**: Contraseña restablecida con éxito.
* **Errores posibles**:
  * `400 Bad Request`: Código incorrecto, expirado (duración 3 minutos) o ya utilizado.

---

### 5. Registro / Login Federado (Google)
* **Endpoint**: `POST /api/v1/auth/federated`
* **Request Payload (JSON)**:
  ```json
  {
    "providerName": "google",
    "providerUserId": "104928374923847923847",
    "email": "m.mercado.t.94@gmail.com",
    "username": "m.mercado.t.94",
    "role": "ROLE_OUR_CLIENTE"
  }
  ```
* **Response Payload (200 OK - JSON)**:
  ```json
  {
    "jwt": "eyJhbGciOiJIUzI1NiJ9...",
    "jwtRefresh": "eyJhbGciOiJIUzI1NiJ9...",
    "available": true
  }
  ```

---

### 6. Cargar Perfil
* **Endpoint**: `GET /api/v1/profile`
* **Cabeceras**:
  * `Authorization`: `Bearer [token]`
* **Response Payload (200 OK - JSON)**:
  ```json
  {
    "id": 1,
    "username": "m.mercado.t.94",
    "email": "m.mercado.t.94@gmail.com",
    "phone": "3001234567",
    "codPhoneInternational": "+57",
    "roleName": "ROLE_OUR_CLIENTE",
    "operationNames": ["ALL"]
  }
  ```

---

## ⚙️ Ejecución en Perfil Local (`dev`)

El perfil por defecto de desarrollo es **`dev`**, el cual desacopla dependencias complejas como Redis y DynamoDB.

### Requisitos previos:
* Tener PostgreSQL en marcha (ej. `docker compose up -d postgres`).

### Instrucciones de arranque:
Crea un archivo `.env` local copiando `.env.example`:
```bash
cp .env.example .env
```
Arranca la aplicación mediante Gradle wrapper:
```bash
# Windows
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Linux / macOS
./gradlew bootRun --args="--spring.profiles.active=dev"
```

---

## 🛠️ Mockear y Simular Servicios Externos

* **Envío de Correos (EmailSender)**:
  * El envío de correos está mockeado a través del bean `LoggingEmailSenderAdapter` en el perfil de desarrollo, imprimiendo el contenido del correo y códigos de 6 dígitos directamente en consola de manera local.
* **AWS DynamoDB (LocalStack)**:
  * Habilita `findu.aws.dynamodb.enabled=true` en tu configuración.
  * Levanta LocalStack con Compose:
    ```yaml
    localstack:
      image: localstack/localstack
      ports:
        - "4566:4566"
    ```
  * Configura el endpoint local: `FINDU_AWS_DYNAMODB_ENDPOINT=http://localhost:4566`.
