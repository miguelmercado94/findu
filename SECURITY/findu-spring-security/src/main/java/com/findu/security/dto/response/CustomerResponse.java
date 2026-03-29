package com.findu.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para listado de clientes (GET /api/v1/customers).
 * No expone id, password, rol ni authorities.
 */
public record CustomerResponse(
    @Schema(example = "juan.perez2")
    String username,
    @Schema(example = "juan.perez2@ejemplo.com")
    String email,
    @Schema(example = "3003763311")
    String phone,
    @Schema(example = "true")
    boolean active
) {}
