package com.findu.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Respuesta de credencial o certificado de especialista")
public record EspecialistaCredencialResponse(
        @Schema(description = "ID de la credencial") Long id,
        @Schema(description = "ID de la especialidad asociada") Long perfilEspecialistaId,
        @Schema(description = "Tipo de certificado (SUPERIOR, CERTIFICADO, CURSO, DIPLOMADO)") String tipoCertificado,
        @Schema(description = "Nombre del título o curso") String nombreTitulo,
        @Schema(description = "Institución educativa") String institucion,
        @Schema(description = "Fecha de inicio") LocalDate fechaInicio,
        @Schema(description = "Fecha de finalización") LocalDate fechaFin,
        @Schema(description = "URL del certificado en S3 (PNG/PDF)") String urlCertificadoS3
) {}
