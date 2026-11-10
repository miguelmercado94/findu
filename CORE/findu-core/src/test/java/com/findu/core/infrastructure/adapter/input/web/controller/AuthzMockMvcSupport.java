package com.findu.core.infrastructure.adapter.input.web.controller;

import com.findu.core.infrastructure.adapter.input.web.exception.GlobalExceptionHandler;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import com.findu.core.infrastructure.security.AuthzMode;
import com.findu.core.infrastructure.security.AuthzProperties;
import com.findu.core.infrastructure.security.JwtContextFilter;
import com.findu.core.infrastructure.security.JwtVerifier;
import com.findu.core.infrastructure.security.ProviderOwnershipGuard;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Arma MockMvc standalone con el filtro JWT y el guard reales en modo ENFORCE (perfil 1 -> authUserId 2). */
final class AuthzMockMvcSupport {

    static final String SECRET = "findu-test-secret-key-at-least-256-bits-long!!";
    static final String INTERNAL_KEY = "llave-interna-test";

    private AuthzMockMvcSupport() {
    }

    static AuthzProperties enforceProps() {
        AuthzProperties props = new AuthzProperties();
        props.setMode(AuthzMode.ENFORCE);
        props.setJwtSecret(SECRET);
        props.setInternalServiceKey(INTERNAL_KEY);
        return props;
    }

    static ProviderOwnershipGuard guard(AuthzProperties props) {
        PerfilProveedorRepository repo = mock(PerfilProveedorRepository.class);
        PerfilProveedorEntity perfil = new PerfilProveedorEntity();
        perfil.setAuthUserId(2L);
        when(repo.findById(1L)).thenReturn(Optional.of(perfil));
        // El guard se inyecta con un HttpServletRequest "scoped": delega al request actual de MockMvc.
        HttpServletRequest current = (HttpServletRequest) Proxy.newProxyInstance(
                AuthzMockMvcSupport.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> method.invoke(
                        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest(), args));
        return new ProviderOwnershipGuard(props, repo, current);
    }

    static MockMvc mvc(AuthzProperties props, Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new JwtContextFilter(props, new JwtVerifier(SECRET)))
                .build();
    }

    static String bearer(long userId) {
        return "Bearer " + Jwts.builder().header().type("JWT").and().subject("u").claim("userId", userId)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }
}
