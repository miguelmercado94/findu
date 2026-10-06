# Central Server de Configuración (`findu-config`)

`findu-config` es el microservicio servidor de configuración centralizado (**Spring Cloud Config Server**) para toda la suite de microservicios de FIND-U. Concentra las conexiones a bases de datos, URIs de descubrimiento (Eureka), brokers de mensajería (RabbitMQ), sockets/redis, buckets AWS y secretos en un único repositorio.

## 🚀 Perfiles Disponibles

La configuración de cada microservicio está centralizada en `src/main/resources/config-repo/` y dividida por entornos:

1. **`local`**: Configuración para desarrollo local con **Docker Compose** o ejecución directa en la máquina.
2. **`dev`**: Entorno de desarrollo en nube (Railway / AWS Dev).
3. **`stage`**: Entorno de pruebas / Staging (QA).
4. **`prod` / `pdn`**: Entorno de Producción con secretos gestionados vía variables de entorno.

---

## 🛠️ Servicios Necesarios

Para ejecutar `findu-config`:
- **Java 21**
- **Gradle 8.x**

---

## ⚙️ Propiedades Clave

| Propiedad | Valor Local / Docker | Descripción |
| :--- | :--- | :--- |
| `server.port` | `8888` | Puerto de escucha del Config Server |
| `spring.profiles.active` | `native` | Carga las configuraciones del classpath local (`config-repo/`) |
| `spring.cloud.config.server.native.search-locations` | `classpath:/config-repo` | Ubicación interna de las configuraciones YAML |

---

## 💻 Prueba en Local

### 1. Ejecutar de manera independiente (IDE o Terminal)
```bash
./gradlew bootRun
```
Verificar que responda las configuraciones enviando peticiones HTTP:
- `http://localhost:8888/findu-spring-security/local`
- `http://localhost:8888/findu-core/dev`
- `http://localhost:8888/api-gateway/prod`

### 2. Despliegue con Docker Compose
```bash
docker compose up -d findu-config
```
Salud del servicio: `http://localhost:8888/actuator/health`
