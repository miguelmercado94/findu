package com.findu.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Perfil del usuario autenticado (sin tokens; para GET /api/v1/profile).
 */
public record UserProfileResponse(
        @Schema(example = "1")
        Long id,
        @Schema(example = "juan.perez2")
        String username,
        @Schema(example = "juan.perez2@ejemplo.com")
        String email,
        @Schema(example = "3003763311")
        String phone,
        @Schema(example = "+57")
        String codPhoneInternational,
        @Schema(example = "ROLE_CUSTOMER")
        String roleName,
        @Schema(example = "INCOMPLETO")
        String estado,
        @Schema(example = "true")
        boolean phoneVerified,
        @Schema(example = "false")
        boolean emailVerified,
        @Schema(example = "PENDING_EMAIL_VERIFICATION")
        String accountStatus,
        @Schema(example = "PENDIENTE_VALIDACIONES")
        String estadoActivacion,
        @Schema(example = "true")
        boolean otpOk,
        @Schema(example = "false")
        boolean emailOk,
        @Schema(example = "false")
        boolean biometriaOk,
        @Schema(example = "false")
        boolean soporteOk,
        @Schema(description = "Foto frontal inmutable (puede ser null)")
        String fotoPerfilFrontalUrl,
        @Schema(description = "Operaciones/autorizaciones del rol")
        List<String> operationNames
) {}
