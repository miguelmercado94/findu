package com.findu.core.infrastructure.security;

import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilClienteEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.PerfilProveedorEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilClienteRepository;
import com.findu.core.infrastructure.adapter.out.persistence.repository.PerfilProveedorRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentActorResolverTest {

    private static final String SECRET = "findu-test-secret-key-at-least-256-bits-long!!";

    private CurrentActorResolver resolver;

    @BeforeEach
    void setUp() {
        PerfilProveedorRepository proveedores = mock(PerfilProveedorRepository.class);
        PerfilProveedorEntity proveedor = new PerfilProveedorEntity();
        proveedor.setId(1L);
        proveedor.setAuthUserId(2L);
        when(proveedores.findByAuthUserId(2L)).thenReturn(Optional.of(proveedor));
        PerfilClienteRepository clientes = mock(PerfilClienteRepository.class);
        PerfilClienteEntity cliente = new PerfilClienteEntity();
        cliente.setId(5L);
        cliente.setAuthUserId(7L);
        when(clientes.findById(5L)).thenReturn(Optional.of(cliente));
        resolver = new CurrentActorResolver(new JwtVerifier(SECRET), proveedores, clientes);
    }

    private static MockHttpServletRequest conToken(long userId) {
        String jwt = Jwts.builder().subject("u").claim("userId", userId)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt);
        return req;
    }

    private static HttpStatus statusDe(Runnable r) {
        return HttpStatus.valueOf(assertThrows(ResponseStatusException.class, r::run).getStatusCode().value());
    }

    @Test
    void sinTokenOTokenInvalidoDan401() {
        assertEquals(HttpStatus.UNAUTHORIZED, statusDe(() -> resolver.requirePerfilProveedorId(new MockHttpServletRequest())));
        MockHttpServletRequest basura = new MockHttpServletRequest();
        basura.addHeader("Authorization", "Bearer no-es-un-jwt");
        assertEquals(HttpStatus.UNAUTHORIZED, statusDe(() -> resolver.requirePerfilProveedorId(basura)));
        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic abc");
        assertEquals(HttpStatus.UNAUTHORIZED, statusDe(() -> resolver.requirePerfilProveedorId(basic)));
    }

    @Test
    void llaveInternaNoSustituyeAlJwt() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Internal-Service-Key", "cualquiera");
        assertEquals(HttpStatus.UNAUTHORIZED, statusDe(() -> resolver.requirePerfilProveedorId(req)));
    }

    @Test
    void tokenValidoSinPerfilProveedorDa403() {
        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> resolver.requirePerfilProveedorId(conToken(99))));
    }

    @Test
    void resuelveElPerfilProveedorDelJwt() {
        assertEquals(1L, resolver.requirePerfilProveedorId(conToken(2)));
    }

    @Test
    void clienteSoloAccedeASuPropioPerfil() {
        assertEquals(5L, resolver.requirePerfilClienteDueno(conToken(7), 5L));
        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> resolver.requirePerfilClienteDueno(conToken(8), 5L)));
        assertEquals(HttpStatus.FORBIDDEN, statusDe(() -> resolver.requirePerfilClienteDueno(conToken(7), 404L)));
        assertEquals(HttpStatus.UNAUTHORIZED,
                statusDe(() -> resolver.requirePerfilClienteDueno(new MockHttpServletRequest(), 5L)));
    }
}