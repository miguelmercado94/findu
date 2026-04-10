package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.JwtService;
import com.findu.security.application.service.JwtTokenRevocationService;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.Jwt;
import com.findu.security.domain.model.JwtClaims;
import com.findu.security.domain.model.JwtSignerFactory;
import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.LoginRequest;
import com.findu.security.dto.request.LogoutRequest;
import com.findu.security.dto.response.ValidateTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtManagerImplTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtSignerFactory jwtSignerFactory;
    @Mock
    private RolRepositoryPort rolRepositoryPort;
    @Mock
    private UserRolRepositoryPort userRolRepositoryPort;
    @Mock
    private RolOperationRepositoryPort rolOperationRepositoryPort;
    @Mock
    private ReactiveAuthenticationManager reactiveAuthenticationManager;
    @Mock
    private JwtTokenRevocationService jwtTokenRevocationService;

    private JwtManagerImpl jwtManager;

    @BeforeEach
    void setUp() {
        jwtManager = new JwtManagerImpl(usuarioService, jwtService, jwtSignerFactory, rolRepositoryPort,
                userRolRepositoryPort, rolOperationRepositoryPort, reactiveAuthenticationManager, jwtTokenRevocationService);
        ReflectionTestUtils.setField(jwtManager, "accessExpirationSeconds", 300L);
        ReflectionTestUtils.setField(jwtManager, "refreshExpirationSeconds", 604800L);
    }

    @Test
    void validateToken_blank_returnsInvalid() {
        StepVerifier.create(jwtManager.validateToken(null))
                .expectNextMatches(v -> !v.tokenValid())
                .verifyComplete();
        StepVerifier.create(jwtManager.validateToken("   "))
                .expectNextMatches(v -> !v.tokenValid())
                .verifyComplete();
    }

    @Test
    void validateToken_invalid_returnsInvalid() {
        when(jwtService.isValid("x")).thenReturn(false);
        StepVerifier.create(jwtManager.validateToken("x"))
                .expectNextMatches(v -> !v.tokenValid())
                .verifyComplete();
    }

    @Test
    void validateToken_valid_returnsDecoded() {
        when(jwtService.isValid("tok")).thenReturn(true);
        when(jwtService.getHeader("tok")).thenReturn(java.util.Map.of("alg", "HS256"));
        JwtClaims claims = new JwtClaims();
        claims.setSub("u1");
        when(jwtService.parse("tok")).thenReturn(claims);

        StepVerifier.create(jwtManager.validateToken("tok"))
                .expectNextMatches(ValidateTokenResponse::tokenValid)
                .verifyComplete();
    }

    @Test
    void logout_nullBody_errors() {
        StepVerifier.create(jwtManager.logout(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void logout_invalidAccess_errors() {
        when(jwtService.isValid("a")).thenReturn(false);
        var req = new LogoutRequest("a", null);
        StepVerifier.create(jwtManager.logout(req))
                .expectError()
                .verify();
    }

    @Test
    void refresh_blank_errors() {
        StepVerifier.create(jwtManager.refresh("", "HS256"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void login_success() {
        Rol rol = new Rol();
        rol.setId(10);
        rol.setName("ROLE_CUSTOMER");
        when(rolRepositoryPort.findByName("ROLE_CUSTOMER")).thenReturn(Mono.just(rol));

        Usuario u = new Usuario();
        u.setId(1L);
        u.setUsername("user");
        var auth = new UsernamePasswordAuthenticationToken(u, "pwd", List.of());
        when(reactiveAuthenticationManager.authenticate(any())).thenReturn(Mono.just(auth));

        when(userRolRepositoryPort.findRoleByUserId(1L)).thenReturn(Mono.just(rol));
        when(rolOperationRepositoryPort.findOperationsByRoleId(10)).thenReturn(Flux.empty());
        when(jwtSignerFactory.getSigner(anyString())).thenReturn(data -> "sig");
        when(jwtService.generateToken(any(Jwt.class))).thenReturn("accessJwt", "refreshJwt");
        when(jwtTokenRevocationService.registerIssuedPair("accessJwt", "refreshJwt")).thenReturn(Mono.empty());

        var req = new LoginRequest("user", "pwd", "ROLE_CUSTOMER");
        StepVerifier.create(jwtManager.login(req, "HS256"))
                .expectNextMatches(t -> t.available()
                        && "accessJwt".equals(t.jwt())
                        && "refreshJwt".equals(t.jwtRefresh()))
                .verifyComplete();
    }

    @Test
    void refresh_invalid_expired() {
        when(jwtService.isValid("x")).thenReturn(false);
        StepVerifier.create(jwtManager.refresh("x", "HS256"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void refresh_notRefreshType() {
        when(jwtService.isValid("x")).thenReturn(true);
        when(jwtService.isRefreshToken("x")).thenReturn(false);
        StepVerifier.create(jwtManager.refresh("x", "HS256"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void refresh_revoked() {
        when(jwtService.isValid("r")).thenReturn(true);
        when(jwtService.isRefreshToken("r")).thenReturn(true);
        when(jwtTokenRevocationService.isRevoked("r")).thenReturn(Mono.just(true));
        StepVerifier.create(jwtManager.refresh("r", "HS256"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void refresh_success() {
        when(jwtService.isValid("r")).thenReturn(true);
        when(jwtService.isRefreshToken("r")).thenReturn(true);
        when(jwtTokenRevocationService.isRevoked("r")).thenReturn(Mono.just(false));
        JwtClaims claims = new JwtClaims();
        claims.setSub("user");
        when(jwtService.parse("r")).thenReturn(claims);

        Usuario u = new Usuario();
        u.setId(2L);
        u.setUsername("user");
        Rol rol = new Rol();
        rol.setId(5);
        rol.setName("ROLE_CUSTOMER");
        when(usuarioService.getUserByUsername("user")).thenReturn(Mono.just(u));
        when(usuarioService.getUserByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Mono.empty());
        when(userRolRepositoryPort.findRoleByUserId(2L)).thenReturn(Mono.just(rol));
        when(rolOperationRepositoryPort.findOperationsByRoleId(5)).thenReturn(Flux.empty());
        when(jwtSignerFactory.getSigner(anyString())).thenReturn(data -> "sig");
        when(jwtService.generateToken(any(Jwt.class))).thenReturn("na", "nr");
        when(jwtTokenRevocationService.registerIssuedPair("na", "nr")).thenReturn(Mono.empty());
        when(jwtTokenRevocationService.rotateSession("r", "na", "nr")).thenReturn(Mono.empty());

        StepVerifier.create(jwtManager.refresh("r", "HS256"))
                .expectNextMatches(t -> "na".equals(t.jwt()))
                .verifyComplete();
    }

    @Test
    void logout_success() {
        when(jwtService.isValid("acc")).thenReturn(true);
        when(jwtTokenRevocationService.markSessionUnavailable("acc", null)).thenReturn(Mono.empty());
        var req = new LogoutRequest("acc", null);
        StepVerifier.create(jwtManager.logout(req))
                .expectNextMatches(t -> !t.available())
                .verifyComplete();
    }

    @Test
    void getCurrentUserProfile_success() {
        Usuario u = new Usuario();
        u.setUsername("alice");
        u.setEmail("a@b.com");
        u.setPhone("1");
        Rol r = new Rol();
        r.setName("ROLE_CUSTOMER");
        u.setRol(r);
        var auth = new UsernamePasswordAuthenticationToken(u, null, List.of());
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(auth);

        StepVerifier.create(jwtManager.getCurrentUserProfile()
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(ctx))))
                .expectNextMatches(p -> "alice".equals(p.username()) && "ROLE_CUSTOMER".equals(p.roleName()))
                .verifyComplete();
    }
}
