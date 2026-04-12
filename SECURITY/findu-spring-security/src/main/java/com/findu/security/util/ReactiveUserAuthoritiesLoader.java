package com.findu.security.util;

import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.domain.model.Rol;
import com.findu.security.domain.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Carga operaciones del rol desde BD y setea {@link Usuario#setGrantedAuthorities}.
 * Misma lógica en login/refresh ({@link com.findu.security.application.usecase.impl.JwtManagerImpl}),
 * registro de cliente ({@link com.findu.security.application.usecase.impl.CustomerManagerImpl}) y
 * {@link com.findu.security.application.service.impl.UsuarioServiceImpl#loadUserWithRoleAndAuthorities}.
 * Para la lista de nombres de operación en respuestas, usar {@link UserOperationNames#fromUsuario(Usuario)}.
 */
public final class ReactiveUserAuthoritiesLoader {

    private static final Logger log = LoggerFactory.getLogger(ReactiveUserAuthoritiesLoader.class);

    private ReactiveUserAuthoritiesLoader() {
    }

    public static Mono<Usuario> loadAuthoritiesFromDb(RolOperationRepositoryPort rolOperationRepositoryPort, Usuario user) {
        if (user.getRol() == null) {
            log.debug("User {} without role, authorities vacías", user.getUsername());
            user.setGrantedAuthorities(Collections.emptyList());
            return Mono.just(user);
        }
        Rol rol = user.getRol();
        log.debug("Loading authorities from DB for roleId={} user={}", rol.getId(), user.getUsername());
        return rolOperationRepositoryPort.findOperationsByRoleId(rol.getId())
                .collectList()
                .map(ops -> RoleAuthoritySupport.fromOperationsAndRole(ops, rol))
                .doOnNext(user::setGrantedAuthorities)
                .thenReturn(user);
    }
}
