# Servidor de Descubrimiento (`eureka-server`)

`eureka-server` gestiona el registro y descubrimiento dinámico de todos los microservicios de FIND-U.

---

## 🛠️ Servicios Necesarios para Probar en Local

- **Java 21**
- Ningún servicio externo requerido (opera de forma independiente).

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local por Defecto | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8761` | Puerto HTTP principal |
| `EUREKA_SERVER_EVICTION_INTERVAL_TIMER_IN_MS` | `5000` | Frecuencia de verificación de instancias caídas (5s en dev) |
| `EUREKA_SERVER_ENABLE_SELF_PRESERVATION` | `false` | Desactiva auto-preservación en desarrollo local para evicción rápida |

---

## 💻 Ejecución y Prueba Local

1. **Dashboard Visual de Eureka**: Navegar en el navegador a `http://localhost:8761/`
2. **Con Docker**:
```bash
docker compose up -d eureka-server
```
3. **Endpoint de Salud**: `http://localhost:8761/actuator/health`
