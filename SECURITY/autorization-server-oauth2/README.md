# findu-authorization-server-oauth2

Servidor de Autorización **OAuth2 y OpenID Connect 1.0 (OIDC)** estándar para el ecosistema de **FIND-U** (basado en **Spring Security Authorization Server**).

Responsable de la autenticación de usuarios, gestión de consentimiento y emisión de tokens de acceso (JWT) y actualización (Refresh) mediante criptografía asimétrica (RSA).

---

## 🚦 Endpoints Detallados de OAuth2 / OIDC

### 1. Descubrimiento de Configuración (Well-Known)
* **Endpoint**: `GET /.well-known/openid-configuration` o `GET /.well-known/oauth-authorization-server`
* **Response Payload (200 OK - JSON)**: Retorna los metadatos del servidor de autorización, incluyendo endpoints soportados, algoritmos de firma y scopes disponibles.

---

### 2. Endpoint de Autorización (Paso 1 del Authorization Code Flow)
* **Endpoint**: `GET /oauth2/authorize`
* **Parámetros obligatorios (Query Params)**:
  * `response_type`: Debe ser `code`.
  * `client_id`: Identificador del cliente registrado (ej. `findu-cliente` o `findu-proveedor`).
  * `redirect_uri`: URI de redirección pre-registrada en BD.
  * `scope`: Scopes requeridos (ej. `openid`, `read`, `write`).
  * `code_challenge`: Hash SHA-256 del verificador de PKCE (para aplicaciones nativas/móviles).
  * `code_challenge_method`: Debe ser `S256`.
* **Comportamiento**:
  * Si el usuario no está autenticado, Spring Security redirige al formulario de Login por defecto de Spring.
  * Tras autenticarse correctamente, el servidor genera una redirección a la `redirect_uri` indicada adjuntando el parámetro `code`.

---

### 3. Obtención e Intercambio de Tokens (Paso 2 / Client Credentials / Refresh)
* **Endpoint**: `POST /oauth2/token`
* **Cabeceras**:
  * `Content-Type`: `application/x-www-form-urlencoded`
  * `Authorization`: Basic Auth (`client_id` y `client_secret` en Base64) solo para clientes confidenciales.
* **Payload (UrlEncoded Form)**:
  * **Intercambio de Código (Authorization Code Flow)**:
    * `grant_type=authorization_code`
    * `code=[código obtenido]`
    * `redirect_uri=[redirect_uri del paso anterior]`
    * `code_verifier=[verificador PKCE original en texto plano]`
    * `client_id=[client_id]` (requerido para clientes públicos/móviles).
  * **Credenciales de Cliente (Client Credentials)**:
    * `grant_type=client_credentials`
    * `scope=admin:write`
  * **Refresh Token**:
    * `grant_type=refresh_token`
    * `refresh_token=[token_de_refresco]`
* **Response Payload (200 OK - JSON)**:
  ```json
  {
    "access_token": "eyJhbGciOiJSUzI1NiJ9...",
    "refresh_token": "g5r4-d4s8-21d9...",
    "scope": "openid read",
    "token_type": "Bearer",
    "expires_in": 3600
  }
  ```
* **Errores posibles (400 Bad Request / 401 Unauthorized)**:
  * `invalid_grant`: Código de autorización expirado o ya usado, verificador PKCE no coincide, o credenciales inválidas.
  * `invalid_client`: Secret o ID del cliente incorrecto.
  * `invalid_scope`: El scope solicitado no está asignado al cliente en base de datos.

---

### 4. Llaves Públicas (JWKS)
* **Endpoint**: `GET /oauth2/jwks`
* **Response Payload (200 OK - JSON)**: Retorna el juego de llaves públicas RSA utilizadas por los Resource Servers para validar las firmas digitales de los Access Tokens (JWT) de manera local sin consultar al servidor de autorización en cada llamada.

---

## ⚙️ Ejecución en Perfil Local (`dev`)

### Requisitos previos:
* Levantar la base de datos dedicada Postgres OAuth2 (`docker compose up -d`).

### Instrucciones de arranque:
```bash
# Windows
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Linux / macOS
./gradlew bootRun --args="--spring.profiles.active=dev"
```
El servidor levanta en el puerto **`9595`** expuesto con la ruta base `/authorization-server`.

---

## 🔗 Integración y Registro en Eureka

El servicio se registra de manera dinámica en el servidor **Eureka** bajo el identificador `AUTORIZATION-SERVER-OAUTH2`. El API Gateway rutea automáticamente las llamadas dirigidas a `/authorization-server/**` hacia esta instancia.
