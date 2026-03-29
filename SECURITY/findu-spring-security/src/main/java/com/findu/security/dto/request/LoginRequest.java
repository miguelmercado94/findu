package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para login (username o email + password + rol).
 * El rol se valida que exista antes de continuar con el login.
 */
public record LoginRequest(

    @NotBlank(message = "username o email es requerido")
    @Schema(example = "lmarquez", description = "Username o email")
    String usernameOrEmail,

    @NotBlank(message = "password es requerido")
    @Schema(example = "Password123*", description = "Password del usuario")
    String password,

    @NotBlank(message = "rol es requerido")
    @Schema(example = "ROLE_CUSTOMER", description = "Rol para autenticacion")
    String role
) {}
