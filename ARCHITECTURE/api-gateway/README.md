# findu-api-gateway

Puerta de enlace principal (API Gateway) del ecosistema de **FIND-U**, construida utilizando **Spring Cloud Gateway** (reactivo sobre Netty) y conectada a **Netflix Eureka** para el descubrimiento de microservicios y balanceo de carga.

---

## ⚙️ Configuración y Puerto
* **Puerto de escucha**: `8080` (punto de entrada único para el frontend).
* **Consola de Administración de rutas**: `http://localhost:8080/actuator/gateway`

---

## 🚦 Ruteo Dinámico y Redirecciones

El API Gateway balancea las peticiones de forma dinámica utilizando los nombres de servicio registrados en Eureka:

| Ruta del Gateway | Destino Eureka | Descripción |
|------------------|----------------|-------------|
| `/security-auth/**` | `FINDU-SPRING-SECURITY` | Servicio principal de autenticación, perfiles y recuperación de contraseñas. |
| `/authorization-server/**` | `AUTORIZATION-SERVER-OAUTH2` | Servidor de autorización OAuth2 / OIDC estándar. |

---

## 🌐 Configuración de CORS Global

El Gateway intercepta todas las llamadas CORS entrantes y las autoriza de forma centralizada para evitar duplicidades y conflictos en cabeceras HTTP:

* **Orígenes Permitidos**: `http://localhost:3000` (Portal Clientes), `http://localhost:3001` (Portal Proveedores), `https://www.google.com`.
* **Métodos Soportados**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
* **Filtro de cabeceras**: Deduplica automáticamente las cabeceras `Access-Control-Allow-Origin` para evitar conflictos en peticiones redirigidas.

---

## 🏃 Lanzamiento en Local

### Requisitos previos:
* Servidor Eureka en marcha (`eureka-server` en puerto `8761`).

### Comando de arranque:
```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```
El gateway se conectará automáticamente a Eureka (`http://localhost:8761/eureka/`) para descubrir las rutas disponibles.
