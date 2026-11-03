package com.findu.core.application.dto.request;

import com.findu.core.domain.enums.CategoriaCancelacionProveedor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Solicitud de cancelación de servicio por parte de un proveedor")
public record SolicitarCancelacionProveedorRequest(
        @NotNull(message = "El ID de la solicitud es obligatorio")
        @Schema(description = "ID de la solicitud de servicio", example = "100")
        Long solicitudServicioId,

        @NotNull(message = "El ID del proveedor es obligatorio")
        @Schema(description = "ID del perfil de proveedor", example = "10")
        Long perfilProveedorId,

        @NotNull(message = "La categoría de cancelación es obligatoria")
        @Schema(description = "Categoría de cancelación (CATEGORIA_1_CLIENTE, CATEGORIA_2_FUERZA_MAYOR, CATEGORIA_3_PERSONAL_OPERATIVO, CATEGORIA_4_OTROS)")
        CategoriaCancelacionProveedor categoria,

        @NotBlank(message = "El motivo específico de cancelación es obligatorio")
        @Schema(description = "Motivo seleccionado dentro de la categoría", example = "Emergencia médica o enfermedad")
        String motivo,

        @Schema(description = "Descripción detallada (obligatoria para CATEGORIA_4_OTROS)")
        String descripcion,

        @Schema(description = "URLs o referencias de evidencias adjuntas (opcional al crear en CATEGORIA_2_FUERZA_MAYOR)")
        List<String> evidenciasUrls,

        @Schema(description = "Latitud GPS para auditoría interna (opcional)")
        BigDecimal latitud,

        @Schema(description = "Longitud GPS para auditoría interna (opcional)")
        BigDecimal longitud
) {}

