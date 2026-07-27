package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para login (username/email o celular + password + rol).
 * El rol se valida que exista antes de continuar con el login.
 */
public record LoginRequest(

    @Schema(example = "lmarquez", description = "Username o email")
    String usernameOrEmail,

    @Schema(example = "3001234567", description = "Teléfono celular")
    String phone,

    @Schema(example = "+57", description = "Código de país internacional")
    String codPhoneInternational,

    @NotBlank(message = "password es requerido")
    @Schema(example = "Password123*", description = "Password del usuario")
    String password,

    @NotBlank(message = "rol es requerido")
    @Schema(example = "ROLE_CUSTOMER", description = "Rol para autenticacion")
    String role
) {}
