package com.findu.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Solicitud para registrar una credencial o certificado de especialista")
public record CrearCredencialRequest(
        @Schema(description = "ID de la especialidad") @NotNull Long perfilEspecialistaId,
        @Schema(description = "Tipo de certificado: SUPERIOR, CERTIFICADO, CURSO, DIPLOMADO") @NotBlank String tipoCertificado,
        @Schema(description = "Nombre del título o curso") @NotBlank String nombreTitulo,
        @Schema(description = "Institución educativa") @NotBlank String institucion,
        @Schema(description = "Fecha de inicio (Requerida solo para tipo SUPERIOR)") LocalDate fechaInicio,
        @Schema(description = "Fecha de finalización o expedición") @NotNull LocalDate fechaFin,
        @Schema(description = "URL del certificado en S3 (PNG/PDF)") String urlCertificadoS3
) {}
