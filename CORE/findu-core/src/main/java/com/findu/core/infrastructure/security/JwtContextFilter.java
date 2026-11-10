package com.findu.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Coloca en el request la identidad (JWT valido) o el estado del token. NUNCA bloquea: la decision la toma
 * {@link ProviderOwnershipGuard}. Salta /internal/** (X-Internal-Service-Key en sus controllers) y rutas publicas.
 */
@Component
public class JwtContextFilter extends OncePerRequestFilter {

    public static final String ATTR_PRINCIPAL = "findu.authz.principal";
    /** NO_TOKEN o BAD_TOKEN cuando no hay principal. */
    public static final String ATTR_TOKEN_STATE = "findu.authz.tokenState";
    public static final String ATTR_INTERNAL_OK = "findu.authz.internalOk";
    public static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";

    static final String NO_TOKEN = "NO_TOKEN";
    static final String BAD_TOKEN = "BAD_TOKEN";

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator", "/v3/api-docs", "/swagger-ui",
            "/api/v1/health",
            "/api/v1/categorias", "/api/v1/subcategorias", "/api/v1/servicios",
            "/api/v1/direcciones/municipios", "/api/v1/catalogs/specialties",
            "/api/v1/registration/check-document");
    private static final Pattern PUBLICO = Pattern.compile("^/api/v1/perfil-proveedor/[^/]+/publico/?$");
    private static final Pattern FOTO_PERFIL = Pattern.compile("^/api/v1/perfil-proveedor/[^/]+/foto-perfil/?$");

    private final AuthzProperties props;
    private final JwtVerifier verifier;

    public JwtContextFilter(AuthzProperties props, JwtVerifier verifier) {
        this.props = props;
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return props.getMode() == AuthzMode.OFF || isSkippedPath(request.getMethod(), pathOf(request));
    }

    static String pathOf(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        return ctx != null && !ctx.isEmpty() && uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
    }

    static boolean isSkippedPath(String method, String path) {
        if (path.equals("/internal") || path.startsWith("/internal/")) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix + "?")) {
                return true;
            }
        }
        if (PUBLICO.matcher(path).matches()) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && FOTO_PERFIL.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String serviceKey = request.getHeader(HEADER_INTERNAL_KEY);
        if (Secrets.constantTimeEquals(serviceKey, props.getInternalServiceKey())) {
            request.setAttribute(ATTR_INTERNAL_OK, Boolean.TRUE);
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            request.setAttribute(ATTR_TOKEN_STATE, NO_TOKEN);
        } else if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            Optional<AuthenticatedPrincipal> p = verifier.verify(auth.substring(7));
            if (p.isPresent()) {
                request.setAttribute(ATTR_PRINCIPAL, p.get());
            } else {
                request.setAttribute(ATTR_TOKEN_STATE, BAD_TOKEN);
            }
        } else {
            request.setAttribute(ATTR_TOKEN_STATE, BAD_TOKEN);
        }
        chain.doFilter(request, response);
    }
}
