package com.findu.core.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Respuesta de oferta")
public record OfertaResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "ID del perfil de proveedor") Long perfilProveedorId,
        @Schema(description = "Nombre del proveedor") String proveedorNombre,
        @Schema(description = "Foto de perfil del proveedor") String proveedorFoto,
        @Schema(description = "Calificación del proveedor") BigDecimal calificacionProveedor,
        @Schema(description = "Valor propuesto") BigDecimal valorPropuesto,
        @Schema(description = "Hora de inicio propuesta por el proveedor", example = "14:30:00")
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime horaInicioPropuesta,
        @Schema(description = "Tiempo estimado") String tiempoEstimado,
        @Schema(description = "Mensaje de presentación") String mensajePresentacion,
        @Schema(description = "Estado de la oferta (ENVIADA, PENDIENTE_REOFERTA, ACEPTADA, RECHAZADA, RETIRADA, EXPIRADA)") String estadoOferta,
        @Schema(description = "Aproximación de duración estructurada (MINUTOS, HORAS o DIAS con su cronograma)")
        AproximacionDuracionResponse aproximacionDuracion,
        @Schema(description = "Fecha/hora en que se envió o reenvió la oferta")
        LocalDateTime ofertadoAt,
        @Schema(description = "Fecha/hora en que expira la oferta (10 minutos después de ofertadoAt)")
        LocalDateTime expiresAt,
        @Schema(description = "Segundos restantes antes de que expire la oferta")
        Long remainingSeconds,
        @Schema(description = "Indica si el cliente modificó la solicitud después de que el proveedor ofertó (parpadeo en UI)")
        Boolean solicitudModificada,
        @Schema(description = "Lista de campos modificados por el cliente (ej. fechaProgramada, detalles, presupuestoMaximo, direccion)")
        List<String> camposModificados,
        @Schema(description = "Mapa con los valores anteriores antes del cambio del cliente para resaltarlos en negrita")
        Map<String, String> valoresAnteriores,
        @Schema(description = "Indica si el cambio de fecha/hora del cliente genera solapamiento con el calendario del proveedor (parpadeo en amarillo)")
        Boolean solapamientoHorario
) {
    public OfertaResponse(Long id, Long perfilProveedorId, String proveedorNombre, String proveedorFoto,
                          BigDecimal calificacionProveedor, BigDecimal valorPropuesto,
                          String tiempoEstimado, String mensajePresentacion, String estadoOferta) {
        this(id, perfilProveedorId, proveedorNombre, proveedorFoto, calificacionProveedor, valorPropuesto, null,
                tiempoEstimado, mensajePresentacion, estadoOferta, null, null, null, null, false, List.of(), Map.of(), false);
    }

    public OfertaResponse(Long id, Long perfilProveedorId, String proveedorNombre, String proveedorFoto,
                          BigDecimal calificacionProveedor, BigDecimal valorPropuesto,
                          LocalTime horaInicioPropuesta, String tiempoEstimado,
                          String mensajePresentacion, String estadoOferta) {
        this(id, perfilProveedorId, proveedorNombre, proveedorFoto, calificacionProveedor, valorPropuesto, horaInicioPropuesta,
                tiempoEstimado, mensajePresentacion, estadoOferta, null, null, null, null, false, List.of(), Map.of(), false);
    }

    public OfertaResponse(Long id, Long perfilProveedorId, String proveedorNombre, String proveedorFoto,
                          BigDecimal calificacionProveedor, BigDecimal valorPropuesto,
                          LocalTime horaInicioPropuesta, String tiempoEstimado,
                          String mensajePresentacion, String estadoOferta,
                          AproximacionDuracionResponse aproximacionDuracion) {
        this(id, perfilProveedorId, proveedorNombre, proveedorFoto, calificacionProveedor, valorPropuesto, horaInicioPropuesta,
                tiempoEstimado, mensajePresentacion, estadoOferta, aproximacionDuracion, null, null, null, false, List.of(), Map.of(), false);
    }
}
