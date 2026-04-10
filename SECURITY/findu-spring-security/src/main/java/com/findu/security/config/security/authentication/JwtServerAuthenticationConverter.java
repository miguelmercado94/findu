package com.findu.security.config.security.authentication;

import com.findu.security.application.service.JwtService;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.JwtClaims;
import com.findu.security.domain.model.Usuario;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Convierte el Bearer token JWT en Authentication.
 * Valida el JWT, obtiene sub y role del payload; carga usuario con UsuarioService (username/email + rol).
 * UsuarioService verifica rol en BD, consulta operaciones del rol y setea authorities (o por defecto).
 */
@Component
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public JwtServerAuthenticationConverter(JwtService jwtService, UsuarioService usuarioService) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(h -> h.startsWith(BEARER_PREFIX))
                .map(h -> h.substring(BEARER_PREFIX.length()).trim())
                .filter(token -> !token.isBlank())
                .flatMap(this::validateAndBuildAuthentication)
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Authentication> validateAndBuildAuthentication(String token) {
        if (!jwtService.isValid(token)) {
            return Mono.empty();
        }
        JwtClaims claims = jwtService.parse(token);
        String sub = claims.getSub();
        if (sub == null || sub.isBlank()) {
            return Mono.empty();
        }
        String roleName = getRoleNameFromClaims(claims);
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
