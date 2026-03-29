package com.findu.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de respuesta al guardar un usuario.
 * Incluye los datos del usuario (como en el request), el nombre del rol,
 * la lista de nombres de operaciones asociadas al rol y los tokens JWT (por el momento null).
 */
public record SaveUserResponse(
    @Schema(example = "juan.perez2")
    String username,
    @Schema(example = "juan.perez2@ejemplo.com")
    String email,
    @Schema(example = "3003763311")
    String phone,
    @Schema(example = "ROLE_CUSTOMER")
    String roleName,
    @Schema(description = "Operaciones/autorizaciones del rol")
    List<String> operationNames,
    @Schema(description = "Access token JWT")
    String jwt,
    @Schema(description = "Refresh token JWT")
    String jwtRefresh
) {}
