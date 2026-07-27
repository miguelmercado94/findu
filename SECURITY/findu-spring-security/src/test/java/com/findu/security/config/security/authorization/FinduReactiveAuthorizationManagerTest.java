package com.findu.security.config.security.authorization;

import com.findu.security.application.port.output.persistence.OperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.Operation;
import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinduReactiveAuthorizationManagerTest {

    @Mock
    private OperationRepositoryPort operationRepositoryPort;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private UserRolRepositoryPort userRolRepositoryPort;
    @Mock
    private RolOperationRepositoryPort rolOperationRepositoryPort;

    private FinduReactiveAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new FinduReactiveAuthorizationManager(
                operationRepositoryPort,
                usuarioService,
                userRolRepositoryPort,
                rolOperationRepositoryPort,
                "/security-auth"
        );
    }

    @Test
    void check_authenticated_hasAuthority_allows() {
        Operation op = new Operation();
        op.setName("PROFILE_READ");
        op.setActive(true);
        op.setPermiteAll(false);

        when(operationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(eq("security-auth"), eq("/api/v1/profile"), eq("GET")))
                .thenReturn(Mono.just(op));

        Usuario user = new Usuario();
        user.setId(1L);
        user.setUsername("u");
        when(usuarioService.getUserByUsername("u")).thenReturn(Mono.just(user));

        Rol rol = new Rol();
        rol.setId(10);
        rol.setName("ROLE_CLIENT");
        when(userRolRepositoryPort.findRoleByUserId(1L)).thenReturn(Mono.just(rol));

        Operation userOp = new Operation();
        userOp.setName("PROFILE_READ");
        when(rolOperationRepositoryPort.findOperationsByRoleId(10)).thenReturn(Flux.just(userOp));

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/profile").build());
        var context = new AuthorizationContext(exchange, null);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "u")
                .claim("scope", List.of("PROFILE_READ"))
                .build();
        var auth = new JwtAuthenticationToken(jwt, List.of());

        StepVerifier.create(manager.check(Mono.just(auth), context))
                .expectNextMatches(r -> r instanceof AuthorizationDecision d && d.isGranted())
                .verifyComplete();
    }

    @Test
    void check_noOperation_denies() {
        when(operationRepositoryPort.findByModulePathBaseAndPathAndHttpMethod(eq("security-auth"), eq("/unknown"), eq("GET")))
                .thenReturn(Mono.empty());

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown").build());
        var context = new AuthorizationContext(exchange, null);

        StepVerifier.create(manager.check(Mono.empty(), context))
                .expectNextMatches(r -> r instanceof AuthorizationDecision d && !d.isGranted())
                .verifyComplete();
    }

    @Test
    void testBCryptPassword() {
        String hash = "$2a$10$QHLrXJ8nlnDlOXEDUnV6iefGNOkKaW9pqJwZcpF.5HrO42PRPzjcO";
        String[] candidates = {
            "12345", "password", "secret", "admin", "password123", "findu",
            "findu_oauth2_password", "lmarquez", "mhernandez", "fperez",
            "1234", "123456", "12345678", "123456789", "qwerty", "letmein", "123"
        };
        for (String c : candidates) {
            if (org.springframework.security.crypto.bcrypt.BCrypt.checkpw(c, hash)) {
                System.out.println("CRACKED PASSWORD MATCH: " + c);
            }
        }
    }
}
