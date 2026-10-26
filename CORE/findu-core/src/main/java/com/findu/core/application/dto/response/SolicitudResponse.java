package com.findu.core.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Respuesta de solicitud de servicio")
public record SolicitudResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "Nombre del servicio") String servicioNombre,
        @Schema(description = "Categoría del servicio") String categoriaNombre,
        @Schema(description = "Dirección") String direccionTexto,
        @Schema(description = "Nombre del municipio") String municipioNombre,
        @Schema(description = "Código DANE del municipio") String codigoDane,
        @Schema(description = "Distancia aproximada en km") Double distanciaKm,
        @Schema(description = "Fecha programada") LocalDateTime fechaProgramada,
        @Schema(description = "Nombre del contacto") String nombreContacto,
        @Schema(description = "Teléfono del contacto") String telefonoContacto,
        @Schema(description = "Calificación promedio del cliente") BigDecimal clienteCalificacion,
        @Schema(description = "Prioridad") Integer prioridad,
        @Schema(description = "Presupuesto máximo") BigDecimal presupuestoMaximo,
        @Schema(description = "Es presupuesto límite estricto") Boolean esPresupuestoEstricto,
        @Schema(description = "Tarifa acordada (oferta aceptada)") BigDecimal tarifaAcordada,
        @Schema(description = "Detalles adicionales") String detalles,
        @Schema(description = "Fotos adjuntas") String fotos,
        @Schema(description = "Estado de la solicitud") String estadoSolicitud,
        @Schema(description = "Calificación recibida en la finalización") BigDecimal calificacionRecibida,
        @Schema(description = "Reseña dejada por el cliente") String resenaCliente
) {
    public SolicitudResponse(
            Long id, String servicioNombre, String direccionTexto, LocalDateTime fechaProgramada,
            String nombreContacto, String telefonoContacto, Integer prioridad, BigDecimal presupuestoMaximo,
            Boolean esPresupuestoEstricto, String detalles, String fotos, String estadoSolicitud
    ) {
        this(id, servicioNombre, null, direccionTexto, null, null, null, fechaProgramada,
                nombreContacto, telefonoContacto, null, prioridad, presupuestoMaximo, esPresupuestoEstricto,
                null, detalles, fotos, estadoSolicitud, null, null);
    }
}

