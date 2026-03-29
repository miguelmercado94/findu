package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.JwtManager;
import com.findu.security.dto.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Perfil del usuario autenticado.
 * <p>
 * GET {@code /api/v1/profile} — solo con JWT válido en {@code Authorization: Bearer &lt;access&gt;}.
 * Sin access token, token inválido o expirado: 401. Si se envía el refresh token como Bearer, 401 con mensaje explícito.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Profile", description = "Perfil del usuario autenticado")
public class ProfileController {

    private final JwtManager jwtManager;

    public ProfileController(JwtManager jwtManager) {
        this.jwtManager = jwtManager;
    }

    @GetMapping("/profile")
    @Operation(summary = "Perfil autenticado", security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del usuario",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public Mono<UserProfileResponse> profile() {
        return jwtManager.getCurrentUserProfile();
    }
}
