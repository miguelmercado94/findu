package com.findu.security.presentation.controller;

import com.findu.security.application.usecase.JwtManager;
import com.findu.security.application.usecase.RequestPasswordRecoveryUseCase;
import com.findu.security.application.usecase.ResetPasswordUseCase;
import com.findu.security.dto.request.ForgotPasswordRequest;
import com.findu.security.dto.request.LoginRequest;
import com.findu.security.dto.request.LogoutRequest;
import com.findu.security.dto.request.RefreshTokenRequest;
import com.findu.security.dto.request.ResetPasswordRequest;
import com.findu.security.dto.response.AuthToken;
import com.findu.security.dto.response.ValidateTokenResponse;
import com.findu.security.util.SecurityConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * API de autenticación: login, refresh, recuperación de contraseña.
 * Path base: /api/v1/auth.
 * En login y refresh se debe enviar el header X-JWT-Algorithm (ej. HS256); por defecto HS256.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, refresh, validacion de token y recuperacion de password")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtManager jwtManager;
    private final RequestPasswordRecoveryUseCase requestPasswordRecoveryUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public AuthController(JwtManager jwtManager,
                          RequestPasswordRecoveryUseCase requestPasswordRecoveryUseCase,
                          ResetPasswordUseCase resetPasswordUseCase) {
        this.jwtManager = jwtManager;
        this.requestPasswordRecoveryUseCase = requestPasswordRecoveryUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    /**
     * Valida la autenticidad del token (JWT access o JWT refresh). Si es válido devuelve tokenValid=true, header y payload decodificados (no se devuelve la firma).
     * GET /api/v1/auth/validate
     * Token por header Authorization: Bearer &lt;token&gt; o por query ?token=&lt;token&gt;.
     */
    @GetMapping("/validate")
    @Operation(summary = "Validar token", description = "Valida un token JWT access o refresh y devuelve header/payload decodificados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de validacion",
                    content = @Content(schema = @Schema(implementation = ValidateTokenResponse.class)))
    })
    public Mono<ValidateTokenResponse> validate(
            @Parameter(description = "Authorization: Bearer <token>", required = false)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "Token en query param (alternativa a Authorization)", required = false)
            @RequestParam(value = "token", required = false) String tokenParam) {
        String token = tokenParam;
        if ((token == null || token.isBlank()) && authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            token = authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return jwtManager.validateToken(token != null ? token : "");
    }

    /**
     * Login con username/email y password. Devuelve access + refresh token.
     * POST /api/v1/auth/login
     * Header: X-JWT-Algorithm (ej. HS256).
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica por username/email + password + rol y devuelve access/refresh JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens emitidos",
                    content = @Content(schema = @Schema(implementation = AuthToken.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    public Mono<AuthToken> login(
            @RequestBody @Valid LoginRequest request,
            @Parameter(description = "Algoritmo JWT (HS256 por defecto)")
            @RequestHeader(name = SecurityConstants.HEADER_JWT_ALGORITHM, defaultValue = SecurityConstants.DEFAULT_JWT_ALGORITHM) String algorithm) {
        return jwtManager.login(request, algorithm);
    }

    /**
     * Cierra sesión: revoca el access JWT en almacén (DynamoDB / memoria); el refresh es opcional.
     * Respuesta: mismo DTO que emisión de tokens con {@code available: false}.
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoca access (obligatorio en body) y opcionalmente refresh; blacklist hasta exp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada (tokens marcados revocados)",
                    content = @Content(schema = @Schema(implementation = AuthToken.class))),
            @ApiResponse(responseCode = "400", description = "Sin accessToken o datos invalidos")
    })
    public Mono<AuthToken> logout(@RequestBody @Valid LogoutRequest request) {
        return jwtManager.logout(request);
    }

    /**
     * Refresca los tokens a partir del refresh token.
     * POST /api/v1/auth/refresh
     * Body: { "refreshToken": "..." }
     * Header: X-JWT-Algorithm (ej. HS256).
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Genera un nuevo access/refresh a partir de un refresh token valido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados",
                    content = @Content(schema = @Schema(implementation = AuthToken.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token invalido")
    })
    public Mono<AuthToken> refresh(
            @RequestBody @Valid RefreshTokenRequest request,
            @Parameter(description = "Algoritmo JWT (HS256 por defecto)")
            @RequestHeader(name = SecurityConstants.HEADER_JWT_ALGORITHM, defaultValue = SecurityConstants.DEFAULT_JWT_ALGORITHM) String algorithm) {
        return jwtManager.refresh(request.refreshToken(), algorithm);
    }

    /**
     * Solicita recuperación de contraseña. Se envía un correo con enlace (si el usuario existe).
     * POST /api/v1/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Solicitar recuperacion de password", description = "Genera token y envia enlace al correo si el usuario existe")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitud procesada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    public Mono<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return requestPasswordRecoveryUseCase.requestRecovery(request.emailOrUsername());
    }

    /**
     * Restablece la contraseña con el token recibido por correo.
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablecer password", description = "Actualiza la password usando token de recuperacion")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password actualizada"),
            @ApiResponse(responseCode = "400", description = "Token invalido o expirado")
    })
    public Mono<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return resetPasswordUseCase.resetPassword(request.token(), request.newPassword());
    }
}
