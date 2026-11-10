package com.findu.core.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtContextFilterTest {

    private static final String SECRET = "findu-test-secret-key-at-least-256-bits-long!!";

    private final AuthzProperties props = new AuthzProperties();

    private MockHttpServletRequest run(MockHttpServletRequest req) throws Exception {
        props.setJwtSecret(SECRET);
        props.setInternalServiceKey("llave-interna");
        new JwtContextFilter(props, new JwtVerifier(SECRET))
                .doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return req;
    }

    private static MockHttpServletRequest req(String method, String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, uri);
        r.setContextPath("/findu-core");
        r.setRequestURI("/findu-core" + uri);
        return r;
    }

    private static String token() {
        return Jwts.builder().header().type("JWT").and().subject("p").claim("userId", 2L)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    @Test
    void offNoProcesaNada() throws Exception {
        props.setMode(AuthzMode.OFF);
        MockHttpServletRequest r = req("GET", "/api/v1/perfil-proveedor/1");
        r.addHeader("Authorization", "Bearer " + token());
        assertThat(run(r).getAttribute(JwtContextFilter.ATTR_PRINCIPAL)).isNull();
    }

    @Test
    void tokenValidoColocaPrincipal() throws Exception {
        props.setMode(AuthzMode.LOG);
        MockHttpServletRequest r = req("GET", "/api/v1/perfil-proveedor/1");
        r.addHeader("Authorization", "Bearer " + token());
        assertThat(run(r).getAttribute(JwtContextFilter.ATTR_PRINCIPAL))
                .isEqualTo(new AuthenticatedPrincipal(2L, "p", null));
    }

    @Test
    void sinTokenYTokenInvalidoMarcanEstado() throws Exception {
        props.setMode(AuthzMode.LOG);
        assertThat(run(req("GET", "/api/v1/perfil-proveedor/1")).getAttribute(JwtContextFilter.ATTR_TOKEN_STATE))
                .isEqualTo("NO_TOKEN");
        MockHttpServletRequest bad = req("GET", "/api/v1/perfil-proveedor/1");
        bad.addHeader("Authorization", "Bearer basura");
        assertThat(run(bad).getAttribute(JwtContextFilter.ATTR_TOKEN_STATE)).isEqualTo("BAD_TOKEN");
    }

    @Test
    void llaveInternaValidaMarcaInternalOk() throws Exception {
        props.setMode(AuthzMode.LOG);
        MockHttpServletRequest ok = req("GET", "/api/v1/perfil-proveedor/auth/2");
        ok.addHeader("X-Internal-Service-Key", "llave-interna");
        assertThat(run(ok).getAttribute(JwtContextFilter.ATTR_INTERNAL_OK)).isEqualTo(Boolean.TRUE);
        MockHttpServletRequest bad = req("GET", "/api/v1/perfil-proveedor/auth/2");
        bad.addHeader("X-Internal-Service-Key", "otra");
        assertThat(run(bad).getAttribute(JwtContextFilter.ATTR_INTERNAL_OK)).isNull();
    }

    @Test
    void internalYRutasPublicasSeSaltan() {
        assertThat(JwtContextFilter.isSkippedPath("GET", "/internal/perfiles/1/biometria")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/actuator/health")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/v3/api-docs")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/swagger-ui/index.html")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/servicios")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/categorias/3/subcategorias")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/direcciones/municipios")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/perfil-proveedor/1/publico")).isTrue();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/perfil-proveedor/1/foto-perfil")).isTrue();
    }

    @Test
    void rutasProtegidasNoSeSaltan() {
        assertThat(JwtContextFilter.isSkippedPath("PUT", "/api/v1/perfil-proveedor/1/foto-perfil")).isFalse();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/perfil-proveedor/1")).isFalse();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/perfil-proveedor/auth/2")).isFalse();
        assertThat(JwtContextFilter.isSkippedPath("GET", "/api/v1/direcciones/5")).isFalse();
    }
}
