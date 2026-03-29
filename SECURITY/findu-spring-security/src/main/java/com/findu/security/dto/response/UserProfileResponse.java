package com.findu.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Perfil del usuario autenticado (sin tokens; para GET /api/v1/profile).
 */
public record UserProfileResponse(
        @Schema(example = "juan.perez2")
        String username,
        @Schema(example = "juan.perez2@ejemplo.com")
        String email,
        @Schema(example = "3003763311")
        String phone,
        @Schema(example = "ROLE_CUSTOMER")
        String roleName,
        @Schema(description = "Operaciones/autorizaciones del rol")
        List<String> operationNames
) {}
