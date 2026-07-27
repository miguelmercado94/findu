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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtManager jwtManager;
    @Mock
    private RequestPasswordRecoveryUseCase requestPasswordRecoveryUseCase;
    @Mock
    private ResetPasswordUseCase resetPasswordUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(
                new AuthController(jwtManager, requestPasswordRecoveryUseCase, resetPasswordUseCase)).build();
    }

    @Test
    void validate_usesQueryToken() {
        var res = new ValidateTokenResponse(true, Map.of("alg", "HS256"), Map.of("sub", "u"));
        when(jwtManager.validateToken("abc")).thenReturn(Mono.just(res));

        client.get().uri("/api/v1/auth/validate?token=abc")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tokenValid").isEqualTo(true);
    }

    @Test
    void validate_usesBearerHeader() {
        var res = new ValidateTokenResponse(true, Map.of(), Map.of());
        when(jwtManager.validateToken("tok")).thenReturn(Mono.just(res));

        client.get().uri("/api/v1/auth/validate")
                .header("Authorization", "Bearer tok")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void login_delegates() {
        when(jwtManager.login(org.mockito.ArgumentMatchers.any(LoginRequest.class), eq("HS256")))
                .thenReturn(Mono.just(new AuthToken("a", "r", true)));

        var body = new LoginRequest("u", null, null, "p", "ROLE_CUSTOMER");
        client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header(SecurityConstants.HEADER_JWT_ALGORITHM, "HS256")
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jwt").isEqualTo("a");
    }

    @Test
    void logout_delegates() {
        var req = new LogoutRequest("acc", null);
        when(jwtManager.logout(req)).thenReturn(Mono.just(AuthToken.loggedOut()));

        client.post().uri("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void refresh_delegates() {
        when(jwtManager.refresh("r", "HS256")).thenReturn(Mono.just(new AuthToken("na", "nr", true)));

        client.post().uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .header(SecurityConstants.HEADER_JWT_ALGORITHM, "HS256")
                .bodyValue(new RefreshTokenRequest("r"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void forgotPassword_returns204() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("x@y.com", null, null);
        when(requestPasswordRecoveryUseCase.requestRecovery(req)).thenReturn(Mono.empty());

        client.post().uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isNoContent();

        verify(requestPasswordRecoveryUseCase).requestRecovery(req);
    }

    @Test
    void resetPassword_returns204() {
        ResetPasswordRequest req = new ResetPasswordRequest("t", null, null, null, null, "newpass1");
        when(resetPasswordUseCase.resetPassword(req)).thenReturn(Mono.empty());

        client.post().uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isNoContent();
    }
}
