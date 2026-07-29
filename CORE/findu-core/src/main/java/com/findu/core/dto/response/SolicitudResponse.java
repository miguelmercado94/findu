package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Respuesta de solicitud de servicio")
public record SolicitudResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Nombre del servicio") String servicioNombre,
        @Schema(description = "Dirección") String direccionTexto,
        @Schema(description = "Fecha programada") LocalDateTime fechaProgramada,
        @Schema(description = "Nombre del contacto") String nombreContacto,
        @Schema(description = "Teléfono del contacto") String telefonoContacto,
        @Schema(description = "Prioridad") Integer prioridad,
        @Schema(description = "Presupuesto máximo") BigDecimal presupuestoMaximo,
        @Schema(description = "Estado de la solicitud") String estadoSolicitud
) {}
