package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record FederatedLoginRequest(
    @NotBlank(message = "providerName es requerido")
    @Schema(example = "google")
    String providerName,

    @NotBlank(message = "providerUserId es requerido")
    @Schema(example = "1029384756102")
    String providerUserId,

    @Email(message = "email debe ser válido")
    @NotBlank(message = "email es requerido")
    @Schema(example = "juan.perez@gmail.com")
    String email,

    @NotBlank(message = "username es requerido")
    @Schema(example = "juan.perez")
    String username,

    @NotBlank(message = "role es requerido")
    @Schema(example = "ROLE_OUR_CLIENTE")
    String role
) {}
