package com.findu.security.application.service;

import com.findu.security.domain.model.Usuario;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

/**
 * Resuelve el usuario autenticado desde el contexto reactivo, siguiendo el mismo patrón que
 * {@code JwtManagerImpl.getCurrentUserProfile()} y {@code VerifyPhoneOtpUseCaseImpl}: acepta un
 * principal {@link Usuario} o {@link Jwt} y busca por username y luego por email. 401 si no hay
 * autenticación válida.
 */
@Component
public class AuthenticatedUserResolver {

    private final UsuarioService usuarioService;

    public AuthenticatedUserResolver(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public Mono<Usuario> resolve() {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    Authentication auth = ctx.getAuthentication();
                    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                        return Mono.empty();
                    }
                    Object principal = auth.getPrincipal();
                    String username;
                    if (principal instanceof Usuario usuario) {
                        username = usuario.getUsername();
                    } else if (principal instanceof Jwt jwt) {
                        username = jwt.getSubject();
                    } else {
                        return Mono.empty();
                    }
                    return usuarioService.getUserByUsername(username)
                            .switchIfEmpty(Mono.defer(() -> usuarioService.getUserByEmail(username)));
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado")));
    }
}
