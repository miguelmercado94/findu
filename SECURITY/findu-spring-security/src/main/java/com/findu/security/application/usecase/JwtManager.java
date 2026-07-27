package com.findu.security.application.usecase;

import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.LoginRequest;
import com.findu.security.dto.request.LogoutRequest;
import com.findu.security.dto.request.FederatedLoginRequest;
import com.findu.security.dto.response.AuthToken;
import com.findu.security.dto.response.UserProfileResponse;
import com.findu.security.dto.response.ValidateTokenResponse;
import reactor.core.publisher.Mono;

/**
 * Caso de uso: login, refresh y logout con JWT.
 * El algoritmo (ej. HS256) se recibe por header y se usa con la fábrica de signers.
 */
public interface JwtManager {

    /**
     * Autentica con username/email y password; devuelve access + refresh token.
     *
     * @param loginRequest credenciales
     * @param algorithm    algoritmo del header (ej. HS256)
     */
    Mono<AuthToken> login(LoginRequest loginRequest, String algorithm);

    /**
     * Autentica o registra a un usuario a través de un proveedor federado (Google/Apple); devuelve tokens.
     *
     * @param request   datos de autenticación federada
     * @param algorithm algoritmo del header
     */
    Mono<AuthToken> loginFederated(FederatedLoginRequest request, String algorithm);

    /**
     * Genera un nuevo par de tokens a partir del refresh token válido.
     *
     * @param refreshToken token de refresco
     * @param algorithm    algoritmo del header (ej. HS256)
     */
    Mono<AuthToken> refresh(String refreshToken, String algorithm);

    /**
     * Genera access + refresh token para un usuario ya autenticado (p. ej. tras registro).
     *
     * @param user     usuario
     * @param algorithm algoritmo del header (ej. HS256)
     */
    Mono<AuthToken> buildTokensForUser(Usuario user, String algorithm);

    /**
     * Revoca el access en almacén (obligatorio); opcionalmente el refresh.
     * Devuelve {@link AuthToken} con {@code available false} (mismo contrato que emisión de tokens).
     */
    Mono<AuthToken> logout(LogoutRequest request);

    /**
     * Valida el token (JWT access o refresh) y, si es válido, devuelve header y payload decodificados (sin la firma).
     *
     * @param token token en crudo (Bearer sin incluir)
     * @return tokenValid, header y payload; si no es válido, header y payload son null
     */
    Mono<ValidateTokenResponse> validateToken(String token);

    /**
     * Usuario autenticado actual (desde el contexto reactivo), sin campos de token.
     * Requiere access token en {@code Authorization: Bearer ...}.
     */
    Mono<UserProfileResponse> getCurrentUserProfile();

    /**
     * Actualiza el perfil del usuario autenticado actual.
     */
    Mono<UserProfileResponse> updateCurrentUserProfile(com.findu.security.dto.request.UpdateProfileDto request);
}
