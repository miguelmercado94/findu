package com.findu.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

/**
 * DTO de entrada para el registro de un usuario.
 * Contiene los datos del usuario (sin id ni active) y el nombre del rol.
 */
public record UserRegisterDto(

    @NotBlank(message = "username es requerido")
    @Schema(example = "juan.perez2")
    String username,

    @Email(message = "email debe ser válido")
    @NotBlank(message = "email es requerido")
    @Schema(example = "juan.perez2@ejemplo.com")
    String email,

    @NotBlank(message = "phone es requerido")
    @Schema(example = "3003763311")
    String phone,

    @NotBlank(message = "password es requerido")
    @Schema(example = "Password123*")
    String password,

    @NotBlank(message = "rol es requerido")
    @Schema(example = "ROLE_CUSTOMER")
    String roleName
) {}
