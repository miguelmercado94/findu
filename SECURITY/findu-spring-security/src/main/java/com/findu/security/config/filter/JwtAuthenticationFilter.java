package com.findu.security.config.filter;

import com.findu.security.application.service.JwtService;
import com.findu.security.application.service.JwtTokenRevocationService;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.JwtClaims;
import com.findu.security.domain.model.Usuario;
import com.findu.security.exception.SecurityErrorResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro de autenticación JWT para WebFlux.
 * Authorization: Bearer solo acepta access token ({@code typ: JWT}); el refresh ({@code typ: JWTRefresh}) responde 401.
 */
@Component
public class JwtAuthenticationFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String MSG_REFRESH_AS_BEARER =
            "Token inválido: el refresh token no sirve para autenticar peticiones; use el access token.";

    private static final String MSG_REVOKED =
            "Sesión cerrada: el token fue revocado.";

    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final JwtTokenRevocationService jwtTokenRevocationService;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UsuarioService usuarioService,
                                   JwtTokenRevocationService jwtTokenRevocationService,
                                   SecurityErrorResponseWriter errorResponseWriter) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.jwtTokenRevocationService = jwtTokenRevocationService;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = String.valueOf(exchange.getRequest().getMethod());
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        String token = null;
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            token = authorization.substring(BEARER_PREFIX.length()).trim();
        }
        if (token == null || token.isBlank()) {
            log.debug("JWT filter: sin Authorization Bearer en {} {}", method, path);
            return chain.filter(exchange);
        }

        if (!jwtService.isValid(token)) {
            log.debug("JWT filter: token inválido/expirado en {} {}", method, path);
            return chain.filter(exchange);
        }

        if (jwtService.isRefreshToken(token)) {
            log.debug("JWT filter: refresh token recibido como Bearer en {} {}", method, path);
            return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, MSG_REFRESH_AS_BEARER);
        }

        final String jwt = token;
        log.debug("JWT filter: token válido, comprobando revocación {} {}", method, path);
        return jwtTokenRevocationService.isRevoked(jwt)
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        log.debug("JWT filter: token revocado {} {}", method, path);
                        return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, MSG_REVOKED);
                    }
                    log.debug("JWT filter: intentando autenticar request {} {}", method, path);
                    return validateTokenAndGetAuthentication(jwt)
                            .flatMap(authentication ->
                                    chain.filter(exchange)
                                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                            )
                            .switchIfEmpty(Mono.defer(() -> {
                                log.debug("JWT filter: no se pudo construir Authentication en {} {}", method, path);
                                return chain.filter(exchange);
                            }));
                });
    }

    private Mono<Authentication> validateTokenAndGetAuthentication(String token) {
        JwtClaims claims = jwtService.parse(token);
        String sub = claims.getSub();
        if (sub == null || sub.isBlank()) {
            return Mono.empty();
        }
        String roleName = getRoleNameFromClaims(claims);
        log.debug("JWT filter: claims parseados sub={}, role={}", sub, roleName);
        Mono<Usuario> userMono = (roleName != null && !roleName.isBlank())
                ? usuarioService.getUserByUsernameWithRole(sub, roleName)
                        .switchIfEmpty(usuarioService.getUserByEmailWithRole(sub, roleName))
                : usuarioService.getUserByUsername(sub)
                        .switchIfEmpty(usuarioService.getUserByEmail(sub));
        return userMono.map(usuario -> (Authentication) new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
        ));
    }

    private static String getRoleNameFromClaims(JwtClaims claims) {
        if (claims == null || claims.getClaims() == null) return null;
        Object role = claims.getClaims().get("role");
        return role instanceof String ? (String) role : null;
    }
}
