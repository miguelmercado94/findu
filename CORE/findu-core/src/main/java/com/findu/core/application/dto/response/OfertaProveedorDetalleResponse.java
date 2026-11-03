package com.findu.core.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Detalle combinado de la oferta realizada por el proveedor junto con la solicitud de servicio asociada (para la pestaña 'Ofertas' en Mis Trabajos)")
public record OfertaProveedorDetalleResponse(
        @Schema(description = "ID de la oferta") Long ofertaId,
        @Schema(description = "ID de la solicitud de servicio") Long solicitudServicioId,
        @Schema(description = "Datos completos de la oferta enviada por el proveedor") OfertaResponse oferta,
        @Schema(description = "Datos completos de la solicitud del cliente") SolicitudResponse solicitud,
        @Schema(description = "True si el cliente editó la solicitud y el proveedor debe revisar y reofertar (parpadeo en UI)")
        Boolean solicitudModificada,
        @Schema(description = "Lista de nombres de campos modificados por el cliente (ej. [\"fechaProgramada\", \"detalles\"])")
        List<String> camposModificados,
        @Schema(description = "Valores anteriores de los campos que el cliente modificó")
        Map<String, String> valoresAnteriores,
        @Schema(description = "True si el cambio de fecha/hora del cliente se solapa con otro trabajo del proveedor (parpadeo en amarillo)")
        Boolean solapamientoHorario,
        @Schema(description = "Fecha/hora en que se envió o reenvió la oferta")
        LocalDateTime ofertadoAt,
        @Schema(description = "Fecha/hora exacta de expiración (10 min después de ofertadoAt)")
        LocalDateTime expiresAt,
        @Schema(description = "Segundos restantes antes de que expire la oferta")
        Long remainingSeconds
) {
}

