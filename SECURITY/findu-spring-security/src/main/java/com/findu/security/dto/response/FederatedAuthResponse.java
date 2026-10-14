package com.findu.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para autenticación federada (Google/Apple).
 * - LOGGED_IN: Usuario ya registrado previamente; incluye tokens JWT.
 * - REGISTRATION_REQUIRED: Identidad nueva en el IdP; no se crea usuario aún, no incluye tokens.
 * - ACCOUNT_EXISTS: El email ya existe en otra cuenta previa (Plan B: no auto-link).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FederatedAuthResponse(
        @Schema(description = "Estado del resultado federado: LOGGED_IN, REGISTRATION_REQUIRED, ACCOUNT_EXISTS")
        String status,
        @Schema(description = "Access token JWT (solo si LOGGED_IN)")
        String jwt,
        @Schema(description = "Refresh token JWT (solo si LOGGED_IN)")
        String jwtRefresh,
        @Schema(description = "true si la sesión está disponible")
        boolean available,
        @Schema(description = "Email provisto por el IdP")
        String email,
        @Schema(description = "Username existente o sugerido")
        String username,
        @Schema(description = "Nombre del proveedor (ej. google)")
        String providerName,
        @Schema(description = "Identificador único en el proveedor")
        String providerUserId
) {
    public static FederatedAuthResponse loggedIn(AuthToken tokens, String email, String username, String providerName, String providerUserId) {
        return new FederatedAuthResponse(
                "LOGGED_IN",
                tokens.jwt(),
                tokens.jwtRefresh(),
                tokens.available(),
                email,
                username,
                providerName,
                providerUserId
        );
    }

    public static FederatedAuthResponse registrationRequired(String email, String username, String providerName, String providerUserId) {
        return new FederatedAuthResponse(
                "REGISTRATION_REQUIRED",
                null,
                null,
                false,
                email,
                username,
                providerName,
                providerUserId
        );
    }

    public static FederatedAuthResponse accountExists(String email, String username, String providerName, String providerUserId) {
        return new FederatedAuthResponse(
                "ACCOUNT_EXISTS",
                null,
                null,
                false,
                email,
                username,
                providerName,
                providerUserId
        );
    }
}
