package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta pública del proveedor vista por el cliente.
 * Solo expone: username, nombre, foto, calificación y el perfil especialista relevante.
 * NO expone: cédula, fecha nacimiento, dirección, celular, datos sensibles.
 */
@Schema(description = "Perfil público del proveedor (vista del cliente)")
public record PerfilProveedorPublicoResponse(
        @Schema(description = "Username del proveedor", example = "laura.gomez")
        String username,

        @Schema(description = "Nombre completo", example = "Laura Gómez Pérez")
        String nombreCompleto,

        @Schema(description = "URL de imagen de perfil")
        String urlImagenPerfil,

        @Schema(description = "Calificación promedio general", example = "4.7")
        BigDecimal calificacionPromedio,

        @Schema(description = "Perfil especialista relacionado con el servicio solicitado")
        PerfilEspecialistaDetalleResponse especialidad,

        @Schema(description = "Portafolio de trabajos anteriores en esta especialidad")
        List<PortafolioItemResponse> portafolio
) {}
