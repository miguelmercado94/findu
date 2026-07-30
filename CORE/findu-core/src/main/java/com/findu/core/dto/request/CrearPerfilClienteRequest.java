package com.findu.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Datos para crear un perfil de cliente")
public record CrearPerfilClienteRequest(
        @NotNull @Schema(description = "ID del usuario en findu-security", example = "4")
        Long authUserId,

        @NotBlank @Schema(description = "Nombre de usuario (del registro en seguridad)", example = "m.mercado.t.94")
        String username,

        @NotBlank @Email @Schema(description = "Correo electrónico", example = "m.mercado.t.94@gmail.com")
        String email,

        @NotBlank @Schema(description = "Nombre completo", example = "Miguel Angel Mercado Tirado")
        String nombreCompleto,

        @NotBlank @Schema(description = "Número de identificación", example = "1045678901")
        String numeroIdentificacion,

        @NotBlank @Schema(description = "Tipo de identificación (CC, CE, TI, PA)", example = "CC")
        String tipoIdentificacion,

        @Schema(description = "Fecha de nacimiento", example = "1994-03-15")
        LocalDate fechaNacimiento,

        @Schema(description = "Sexo (M, F, O)", example = "M")
        String sexo,

        @NotBlank @Schema(description = "Número de celular", example = "3003763300")
        String celular,

        @Schema(description = "Código telefónico internacional", example = "+57")
        String codPhoneInternational
) {}
