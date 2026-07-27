# findu-eureka-server

Servidor de Descubrimiento de Servicios (**Netflix Eureka Server**) central para el ecosistema de microservicios de **FIND-U**.

Este servidor actúa como el directorio telefónico de servicios. Cada microservicio en el ecosistema se registra dinámicamente con Eureka al arrancar, facilitando el balanceo de carga reactivo y la resolución de nombres de dominio internos en el API Gateway.

---

## ⚙️ Configuración y Puerto
* **Puerto de escucha**: `8761`
* **Dashboard de Monitoreo**: `http://localhost:8761`

---

## 🏃 Lanzamiento en Local

### Requisitos previos:
* JDK 21 instalado.

### Comando de arranque:
```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

Una vez levantado, ingresa a [http://localhost:8761](http://localhost:8761) en tu navegador para ver la lista de instancias activas registradas (`Instances currently registered with Eureka`).
