# Microservicio Core de Negocio (`findu-core`)

Microservicio Spring Boot responsable de la lógica central de negocio de FIND-U: gestión de perfiles de proveedores, categorías de servicios, catálogo, solicitudes y ciclo de vida de ofertas.

---

## 🛠️ Servicios Necesarios para Probar en Local

1. **`findu-config`**: Config Server (`http://localhost:8888`)
2. **`eureka-server`**: Eureka Service Discovery (`http://localhost:8761`)
3. **`postgres-core`**: Base de Datos PostgreSQL (`localhost:5432/findu_core`)
4. **`redis`**: Cache de búsquedas y catálogo (`localhost:6379`)

---

## ⚙️ Propiedades a Ajustar para Probar en Local

| Variable de Entorno | Valor Local | Descripción |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `local` | Carga `findu-core-local.yml` desde Config Server |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config Server URL |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/findu_core` | Conexión JDBC PostgreSQL Core |
| `SPRING_DATASOURCE_USERNAME` | `findu_core` | Usuario DB |
| `SPRING_DATASOURCE_PASSWORD` | `findu_core` | Contraseña DB |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host Redis |

---

## 💻 Prueba Local

```bash
docker compose up -d findu-core

# Health check
curl http://localhost:8082/findu-core/actuator/health
```

---

## 📡 Documentación de APIs y WebSockets — Sistema de Ofertas y Pestaña "Ofertas" del Proveedor

Base URL vía API Gateway: `/findu-core/api/v1/ofertas` y `/findu-core/api/v1/solicitudes`.

### 1. Enviar Oferta (`POST /api/v1/ofertas`)
Permite al proveedor enviar una oferta inicial o reenviar su oferta sobre una solicitud en estado `ABIERTA` o `EN_NEGOCIACION`.
- Valida que el horario o cronograma propuesto no se solape con otros servicios activos (`ACEPTADA`, `PROGRAMADA`, `EN_CURSO`) del proveedor (`409 Conflict` si hay solapamiento).
- Inicia el temporizador de expiración de **10 minutos** (`ofertadoAt`).
- Soporta el objeto complejo `aproximacionDuracion` (por defecto `1 HORA`, o `MINUTOS` enteros `>= 30`, `HORAS` decimales, o `DIAS` con calendario y múltiples `franjas` horarias por día).

#### Request (`CrearOfertaRequest`)
```json
{
  "solicitudServicioId": 100,
  "perfilProveedorId": 10,
  "valorPropuesto": 95000.00,
  "horaInicioPropuesta": "14:30:00",
  "tiempoEstimado": null,
  "mensajePresentacion": "Incluye mano de obra y materiales",
  "aproximacionDuracion": {
    "unidad": "DIAS",
    "cantidad": 3,
    "mismoHorarioTodosLosDias": false,
    "cronogramaDias": [
      {
        "fecha": "2026-10-27",
        "franjas": [
          { "horaInicio": "08:00:00", "horaFin": "12:00:00" },
          { "horaInicio": "13:00:00", "horaFin": "17:00:00" }
        ]
      }
    ]
  }
}
```
> **Nota**: Para `HORAS` o `MINUTOS`, basta con enviar `"aproximacionDuracion": { "unidad": "HORAS", "cantidad": 1.5 }` o `{ "unidad": "MINUTOS", "cantidad": 45 }`. Si se omite `aproximacionDuracion`, el backend asigna `1 HORA` por defecto.

---

### 2. Consultar Ofertas Activas del Proveedor — Pestaña "Ofertas" (`GET /api/v1/ofertas/proveedor/mis-ofertas`)
- **Query Param**: `proveedorId={Long}` (obligatorio).
- **Descripción**: Retorna las ofertas activas del proveedor (`ENVIADA` vigentes dentro de los 10 minutos, o `PENDIENTE_REOFERTA` cuando el cliente editó la solicitud) junto con el detalle completo de la solicitud, temporizador de expiración y banderas de modificación/solapamiento.

#### Response (`200 OK` — `List<OfertaProveedorDetalleResponse>`)
```json
[
  {
    "ofertaId": 501,
    "solicitudId": 100,
    "oferta": {
      "id": 501,
      "solicitudServicioId": 100,
      "perfilProveedorId": 10,
      "nombreProveedor": "Juan Proveedor",
      "fotoProveedor": "https://...",
      "calificacionProveedor": 4.8,
      "valorPropuesto": 95000.00,
      "horaInicioPropuesta": "14:30:00",
      "tiempoEstimado": "1 hora",
      "mensajePresentacion": "Incluye materiales...",
      "aproximacionDuracion": {
        "unidad": "HORAS",
        "cantidad": 1.00,
        "mismoHorarioTodosLosDias": false,
        "totalHorasEstimadas": 1.00,
        "cronogramaDias": []
      },
      "estadoOferta": "PENDIENTE_REOFERTA",
      "ofertadoAt": "2026-10-27T14:20:00",
      "expiresAt": "2026-10-27T14:30:00",
      "remainingSeconds": 420,
      "solicitudModificada": true,
      "camposModificados": ["fechaProgramada", "fechaInicio", "horaInicio", "detalles"],
      "valoresAnteriores": {
        "fechaProgramada": "2026-10-31T14:30",
        "fechaInicio": "2026-10-31",
        "horaInicio": "14:30",
        "detalles": "Descripción anterior..."
      },
      "solapamientoHorario": true
    },
    "solicitud": {
      "id": 100,
      "servicioNombre": "Enfermería a Domicilio",
      "categoriaNombre": "Salud y Bienestar",
      "direccionTexto": "Calle 614 # 7552 - Sede GPS, Medellín",
      "municipioNombre": "Medellín",
      "codigoDane": "05001",
      "distanciaKm": 2.5,
      "fechaProgramada": "2026-11-01T15:00:00",
      "nombreCliente": "María Cliente",
      "clienteCalificacion": 5.0,
      "prioridad": 3,
      "presupuestoMaximo": 95000.00,
      "esPresupuestoEstricto": false,
      "detalles": "Nueva descripción editada por el cliente...",
      "fotos": null,
      "estadoSolicitud": "ABIERTA"
    },
    "solicitudModificada": true,
    "camposModificados": ["fechaProgramada", "fechaInicio", "horaInicio", "detalles"],
    "valoresAnteriores": {
      "fechaProgramada": "2026-10-31T14:30",
      "fechaInicio": "2026-10-31",
      "horaInicio": "14:30",
      "detalles": "Descripción anterior..."
    },
    "solapamientoHorario": true,
    "ofertadoAt": "2026-10-27T14:20:00",
    "expiresAt": "2026-10-27T14:30:00",
    "remainingSeconds": 420
  }
]
```

---

### 3. Editar / Reenviar Oferta (`PUT /api/v1/ofertas/{id}` o `POST /api/v1/ofertas/{id}/reenviar`)
- **Path Param**: `id` (`ofertaId`).
- **Request Body**: `CrearOfertaRequest` (mismo body que `POST /api/v1/ofertas`).
- **Comportamiento**:
  - Valida nuevamente que no haya solapamiento horario (`409 Conflict` si se solapa con otro trabajo del proveedor).
  - Limpia `solicitudModificada = false`, `camposModificados = []`, `valoresAnteriores = {}`, `solapamientoHorario = false`.
  - Cambia `estadoOferta` a `"ENVIADA"`, reinicia el contador de 10 minutos (`ofertadoAt = now()`, `remainingSeconds = 600`) y notifica al cliente.
- **Response (`200 OK`)**: `OfertaResponse`.

---

### 4. Retirar Oferta (`DELETE /api/v1/ofertas/{id}` o `PATCH /api/v1/ofertas/{id}/retirar`)
- **Path Param**: `id` (`ofertaId`).
- **Query Param opcional**: `proveedorId={Long}`.
- **Comportamiento**: Marca la oferta como `RETIRADA`, desaparece de la pestaña "Ofertas" del proveedor y de la lista del cliente, y emite el evento WebSocket `OFERTA_RETIRADA`.
- **Response (`200 OK`)**:
```json
{
  "ofertaId": 501,
  "estadoOferta": "RETIRADA",
  "mensaje": "Oferta retirada exitosamente"
}
```

---

### 5. Aceptación de Oferta, PIN de 4 Dígitos, Proximidad GPS (20m) e Inicio de Servicio (`ACEPTADA` -> `EN_CURSO`)

#### 5.1. Generación y Consulta del PIN de 4 Dígitos (Exclusivo para el Cliente)
- Al aceptar una oferta (`PATCH /api/v1/ofertas/{id}/aceptar`), el backend genera automáticamente el PIN de inicio:
  - En perfiles `dev` / `local`: el PIN siempre es **`"1234"`**.
  - En perfiles `qa` y `pdn` (`prod`): se genera un PIN aleatorio de 4 dígitos y se almacena encriptado con **AES-GCM** (`solicitud_servicio.pin_inicio_encrypted`).
- **Regla de visibilidad**: El proveedor **nunca** recibe `pinInicio` (`null` en todas las vistas de proveedor). Únicamente el cliente titular puede consultarlo:
  - `GET /api/v1/solicitudes/{id}?clienteId={clienteId}` -> retorna `pinInicio` desencriptado dentro de `SolicitudResponse`.
  - `GET /api/v1/solicitudes/{id}/pin-inicio?clienteId={clienteId}` -> retorna `{ "solicitudId": 100, "pinInicio": "1234" }`.

#### 5.2. Iniciar Servicio con Validación de PIN + GPS <= 20m (`POST /api/v1/solicitudes/{id}/iniciar` o `PATCH`)
Para pasar de `ACEPTADA` (`PROGRAMADA`) a `EN_CURSO`:
1. **Validación de PIN**: `pin` debe coincidir con el PIN de 4 dígitos entregado por el cliente (`400 Bad Request` si no coincide).
2. **Validación GPS (Haversine <= 20 metros)**: `latitud` y `longitud` deben estar a **máximo 20 metros** alrededor de las coordenadas de la dirección del servicio (`409 Conflict` si supera los 20 metros).
3. **Seguro final de no solapamiento horario**: Verifica que el proveedor no tenga otro servicio cruzado aceptado al mismo tiempo (`409 Conflict`).
4. **Auditoría interna GPS/Estado**: Registra fecha/hora, estado anterior/nuevo, actor, coordenadas GPS y distancia en metros en `solicitud_estado_auditoria` (uso exclusivo de investigación y control interno).

##### Request (`IniciarServicioRequest`)
```json
{
  "pin": "1234",
  "latitud": 6.2442030,
  "longitud": -75.5812119,
  "actorId": 10
}
```

#### 5.3. Alarma / Notificación a 1 Hora de Iniciar el Servicio
- Un job programado (`@Scheduled` cada 60s) detecta solicitudes en estado `ACEPTADA` o `PROGRAMADA` cuya `fechaProgramada` esté a `<= 60 minutos` de iniciar y envía por única vez (`recordatorio_1h_enviado = true`) el evento WebSocket/Push `ALARMA_SERVICIO_1H` al proveedor asignado.

---

### 6. Flujo de Cancelaciones de Proveedores y Sistema de Amonestaciones (Ventana Móvil de 30 Días)

Base URL: `/findu-core/api/v1/cancelaciones-proveedor`

#### 6.1. Catálogo de Categorías y Motivos (`GET /api/v1/cancelaciones-proveedor/categorias`)
Retorna las 4 categorías oficiales con sus motivos permitidos y textos de advertencia obligatorios:
- **`CATEGORIA_1_CLIENTE`** (*Motivos relacionados con el cliente*):
  - Advertencia: *"Tu solicitud de cancelación será enviada a soporte para su validación. El servicio no se cancelará hasta que soporte revise y apruebe tu solicitud."*
  - Crea ticket en `findu-help-v2`, deja la solicitud en `PENDIENTE_REVISION_SOPORTE` (no libera el servicio automáticamente) y **no genera amonestación**. Solo soporte puede aprobar (`CANCELADO`) o rechazar (vuelve a `ACEPTADA`).
- **`CATEGORIA_2_FUERZA_MAYOR`** (*Fuerza mayor o emergencias*):
  - Advertencia: *"Por tratarse de una situación de fuerza mayor, debes adjuntar un soporte que permita validar tu solicitud. Puedes cargarlo ahora o hacerlo posteriormente desde tu ticket de soporte. Si después de 3 días no presentas un soporte válido, recibirás una amonestación grave."*
  - Libera el servicio (`CANCELADO`), notifica al cliente (quien puede reabrir la solicitud con `POST /api/v1/solicitudes/{id}/reabrir`), crea ticket de soporte con plazo límite de **3 días calendario** (`fechaLimiteEvidencia`). Si vence el plazo sin soporte válido aprobado, un job automático genera una amonestación **`GRAVE` (7 días de restricción)** sin duplicar eventos.
- **`CATEGORIA_3_PERSONAL_OPERATIVO`** (*Motivos personales u operativos*):
  - Cancela el servicio (`CANCELADO`), marca la cancelación como injustificada (`esInjustificada = true`) y evalúa la **ventana móvil de los últimos 30 días**:
    - **1 a 3 cancelaciones injustificadas** -> Amonestación **`LEVE`** (llamado de atención formal, `0` días de restricción).
    - **4 a 6 cancelaciones injustificadas** -> Amonestación **`MODERADA`** (**`3` días** de restricción para recibir/enviar ofertas).
    - **7 o más cancelaciones injustificadas** -> Amonestación **`GRAVE`** (**`7` días** de restricción para recibir/enviar ofertas).
- **`CATEGORIA_4_OTROS`** (*Otros motivos*):
  - Requiere `descripcion` obligatoria. Cancela el servicio (`CANCELADO`) y crea ticket en soporte (`PENDIENTE_REVISION`). Si soporte la aprueba como justificada (`aprobar: true`), no genera amonestación; si la declara injustificada (`aprobar: false`), se suma a la ventana móvil de 30 días y genera la amonestación correspondiente.

#### 6.2. Previsualización de Advertencia e Impacto antes de Confirmar (`GET /api/v1/cancelaciones-proveedor/previsualizar`)
- **Query Params**: `proveedorId={Long}&categoria={CategoriaCancelacionProveedor}`
- **Response (`PrevisualizacionCancelacionResponse`)**:
```json
{
  "perfilProveedorId": 10,
  "categoria": "CATEGORIA_3_PERSONAL_OPERATIVO",
  "advertenciaTexto": "En los últimos 30 días registras 3 cancelación(es) injustificada(s) y 3 amonestación(es). Si confirmas esta cancelación (y se determina injustificada), alcanzarás 4 cancelación(es) en 30 días, lo que corresponde a una amonestación de nivel MODERADA (Restricción temporal de 3 días para recibir ofertas).",
  "cancelacionesInjustificadasUltimos30Dias": 3,
  "amonestacionesUltimos30Dias": 3,
  "conteoProyectadoSiConfirma": 4,
  "nivelProyectadoSiConfirma": "MODERADA",
  "diasRestriccionProyectados": 3,
  "consecuenciaProyectada": "Restricción temporal de 3 días para recibir ofertas",
  "cancelacionInmediata": true
}
```

#### 6.3. Solicitar / Confirmar Cancelación (`POST /api/v1/cancelaciones-proveedor`)
```json
{
  "solicitudServicioId": 100,
  "perfilProveedorId": 10,
  "categoria": "CATEGORIA_2_FUERZA_MAYOR",
  "motivo": "Emergencia médica o enfermedad",
  "descripcion": "Incapacidad de urgencia",
  "evidenciasUrls": ["https://..."],
  "latitud": 6.2442030,
  "longitud": -75.5812119
}
```

#### 6.4. Adjuntar Evidencias Posteriormente a Ticket de Fuerza Mayor (`POST /api/v1/cancelaciones-proveedor/{cancelacionId}/evidencias`)
```json
{
  "perfilProveedorId": 10,
  "evidenciasUrls": ["https://s3.../incapacidad.pdf"],
  "comentario": "Adjunto certificado médico"
}
```

#### 6.5. Resolución por Soporte (`POST /api/v1/cancelaciones-proveedor/{cancelacionId}/resolver-soporte`)
```json
{
  "agenteSoporteId": 1,
  "aprobar": true,
  "justificacion": "Soporte médico verificado por el equipo de soporte"
}
```

#### 6.6. Historial Disciplinario y Estado de Restricción del Proveedor (`GET /api/v1/cancelaciones-proveedor/proveedor/{proveedorId}/historial-disciplinario`)
Retorna el conteo en ventana móvil de 30 días, si tiene restricción activa (`tieneRestriccionVigente`), fecha de inicio/fin, días y horas restantes, lista de `amonestaciones` y lista de `cancelaciones`.

#### 6.7. Reabrir Solicitud por el Cliente (`POST /api/v1/solicitudes/{id}/reabrir`)
Cuando un proveedor cancela en Categorías 2, 3 o 4, el cliente recibe notificación y puede decidir reabrir su solicitud (`ABIERTA`) para recibir nuevas ofertas de otros proveedores.

---

### 7. Eventos WebSocket y Notificaciones (Canal `SERVICE_UPDATE`)

| `tipoEvento` / Template | Destinatario | Cuándo se emite | Campos clave en `params` |
| :--- | :--- | :--- | :--- |
| `SOLICITUD_CERRADA` | Proveedor | El cliente cancela la solicitud o acepta la oferta de otro proveedor. | `solicitudId`, `motivo` |
| `SOLICITUD_ACTUALIZADA` | Proveedor | El cliente modifica cualquier dato de una solicitud (`PUT /api/v1/solicitudes/{id}`). | `solicitudId`, `ofertaId`, `estadoOferta: "PENDIENTE_REOFERTA"`, `solicitudModificada: "true"`, `camposModificados`, `solapamientoHorario`, `valoresAnteriores` |
| `OFERTA_EXPIRADA` | Proveedor | Transcurren 10 minutos sin que el cliente acepte la oferta (`ENVIADA` -> `EXPIRADA`). | `solicitudId`, `ofertaId`, `proveedorId`, `estadoOferta: "EXPIRADA"` |
| `NUEVA_SOLICITUD` | Proveedor | Nueva solicitud o reapertura por el cliente. (Excluye proveedores con restricción vigente). | `solicitudId`, `reaperturaPorCambioCliente` |
| `OFERTA_RETIRADA` | Proveedor | Confirmación de retiro de oferta por el proveedor. | `solicitudId`, `ofertaId`, `estadoOferta: "RETIRADA"` |
| `OFERTA_ACEPTADA` | Proveedor | El cliente acepta la oferta de este proveedor. | `solicitudId`, `ofertaId`, `valorPropuesto`, `horaInicioPropuesta`, `fechaProgramada` |
| `ALARMA_SERVICIO_1H` | Proveedor | Falta `<= 1 hora` para la `fechaProgramada` de un servicio aceptado. | `solicitudId`, `ofertaId`, `proveedorId`, `fechaProgramada`, `minutosRestantes` |
| `SERVICIO_INICIADO` | Cliente / Proveedor | El proveedor valida el PIN de 4 dígitos y GPS `<= 20m`, pasando a `EN_CURSO`. | `solicitudId`, `estado: "EN_CURSO"` |
| `SOLICITUD_CANCELACION_RECIBIDA` | Proveedor | Confirmación de recepción de solicitud de cancelación. | `solicitudId`, `cancelacionId`, `ticketCodigo`, `categoria` |
| `TICKET_CREADO` | Proveedor | Ticket de soporte creado para Categoría 1, 2 o 4. | `solicitudId`, `ticketCodigo`, `estadoTicket`, `fechaLimiteEvidencia` |
| `SOPORTE_PENDIENTE_CARGAR` | Proveedor | Recordatorio de plazo de 3 días para subir evidencia en Fuerza Mayor. | `solicitudId`, `ticketCodigo`, `fechaLimiteEvidencia` |
| `EVIDENCIA_RECIBIDA` | Proveedor | Confirmación de carga de evidencia en el ticket. | `solicitudId`, `cancelacionId`, `ticketCodigo`, `estadoTicket` |
| `EVIDENCIA_APROBADA` / `EVIDENCIA_RECHAZADA` | Proveedor | Decisión de soporte sobre evidencia de Fuerza Mayor. | `solicitudId`, `cancelacionId`, `ticketCodigo`, `decision`, `justificacion` |
| `RESOLUCION_CANCELACION` | Proveedor | Decisión de soporte en Categoría 1 o Categoría 4. | `solicitudId`, `cancelacionId`, `decision`, `estadoServicio`, `justificacion` |
| `AMONESTACION_GENERADA` | Proveedor | Se registra una amonestación `LEVE`, `MODERADA` o `GRAVE`. | `solicitudId`, `amonestacionId`, `nivel`, `conteo30Dias`, `diasRestriccion` |
| `RESTRICCION_ACTIVADA` | Proveedor | Se activa bloqueo temporal de 3 o 7 días para recibir/enviar ofertas. | `proveedorId`, `nivel`, `diasRestriccion`, `restriccionFin` |
| `RESTRICCION_FINALIZADA` | Proveedor | Vence el plazo de restricción y el proveedor vuelve a recibir ofertas. | `proveedorId`, `amonestacionId` |
| `SERVICIO_CANCELADO_POR_PROVEEDOR` | Cliente | El proveedor cancela en Categorías 2, 3 o 4. | `solicitudId`, `motivo`, `puedeReabrir: "true"` |
| `SERVICIO_DISPONIBLE_PARA_REABRIR` | Cliente | Invita al cliente a reabrir la solicitud para buscar nuevo proveedor. | `solicitudId`, `puedeReabrir: "true"` |
| `CANCELACION_EJECUTADA_POR_SOPORTE` | Cliente | Soporte aprueba y ejecuta la cancelación solicitada en Categoría 1. | `solicitudId`, `motivo`, `justificacionSoporte` |

