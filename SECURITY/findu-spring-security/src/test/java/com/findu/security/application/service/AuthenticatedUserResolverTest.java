package com.findu.security.application.service;

import com.findu.security.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {

    @Mock
    private UsuarioService usuarioService;

    private AuthenticatedUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthenticatedUserResolver(usuarioService);
    }

    private SecurityContext authContext(Usuario user) {
        SecurityContext ctx = new SecurityContextImpl();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        return ctx;
    }

    @Test
    void resolve_authenticatedUsuario_resolvesByUsername() {
        Usuario u = new Usuario();
        u.setId(42L);
        u.setUsername("prov");
        when(usuarioService.getUserByUsername("prov")).thenReturn(Mono.just(u));

        StepVerifier.create(resolver.resolve()
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authContext(u)))))
                .expectNextMatches(res -> res.getId().equals(42L))
                .verifyComplete();
    }

    @Test
    void resolve_notAuthenticated_401() {
        StepVerifier.create(resolver.resolve())
                .expectError(ResponseStatusException.class)
                .verify();
    }
}
