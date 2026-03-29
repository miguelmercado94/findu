package com.findu.security.application.service.impl;

import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UsuarioRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.domain.model.Operation;
import com.findu.security.domain.model.Usuario;
import com.findu.security.util.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de UsuarioService. Usa los puertos de persistencia (implementados por los RepoAdapters).
 * getUserByUsernameWithRole / getUserByEmailWithRole: verifican que el usuario tenga el rol en BD,
 * cargan operaciones del rol (o por defecto si está vacío) y setean authorities en el usuario.
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {
    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final UsuarioRepositoryPort usuarioRepoAdapter;
    private final RolRepositoryPort rolRepositoryPort;
    private final UserRolRepositoryPort userRolRepositoryPort;
    private final RolOperationRepositoryPort rolOperationRepositoryPort;

    public UsuarioServiceImpl(UsuarioRepositoryPort usuarioRepoAdapter,
                             RolRepositoryPort rolRepositoryPort,
                             UserRolRepositoryPort userRolRepositoryPort,
                             RolOperationRepositoryPort rolOperationRepositoryPort) {
        this.usuarioRepoAdapter = usuarioRepoAdapter;
        this.rolRepositoryPort = rolRepositoryPort;
        this.userRolRepositoryPort = userRolRepositoryPort;
        this.rolOperationRepositoryPort = rolOperationRepositoryPort;
    }

    @Override
    public Flux<Usuario> findAll() {
        return usuarioRepoAdapter.findAll();
    }

    @Override
    public Mono<Usuario> findById(Long id) {
        return usuarioRepoAdapter.findById(id);
    }

    @Override
    public Mono<Usuario> getUserByUsername(String username) {
        log.debug("DB lookup user by username={}", username);
        return usuarioRepoAdapter.getByUsername(username);
    }

    @Override
    public Mono<Usuario> getUserByEmail(String email) {
        log.debug("DB lookup user by email={}", email);
        return usuarioRepoAdapter.findByEmail(email);
    }

    @Override
    public Mono<Usuario> save(Usuario usuario) {
        return usuarioRepoAdapter.save(usuario);
    }

    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return usuarioRepoAdapter.existsByUsername(username);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return usuarioRepoAdapter.existsByEmail(email);
    }

    @Override
    public Mono<Boolean> existsByPhone(String phone) {
        return usuarioRepoAdapter.existsByPhone(phone);
    }

    @Override
    public Mono<Usuario> getUserByUsernameWithRole(String username, String roleName) {
        log.debug("DB lookup user by username+role username={} role={}", username, roleName);
        return usuarioRepoAdapter.getByUsername(username)
                .flatMap(user -> loadUserWithRoleAndAuthorities(user, roleName));
    }

    @Override
    public Mono<Usuario> getUserByEmailWithRole(String email, String roleName) {
        log.debug("DB lookup user by email+role email={} role={}", email, roleName);
        return usuarioRepoAdapter.findByEmail(email)
                .flatMap(user -> loadUserWithRoleAndAuthorities(user, roleName));
    }

    /**
     * 1) Trae el rol por nombre. 2) Verifica que el usuario tenga ese rol en BD. 3) Consulta operaciones del rol;
     * si están vacías, usa operaciones por defecto. 4) Setea rol y grantedAuthorities en el usuario.
     */
    private Mono<Usuario> loadUserWithRoleAndAuthorities(Usuario user, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Mono.just(user);
        }
        log.debug("Loading role and authorities userId={} roleName={}", user.getId(), roleName);
        return rolRepositoryPort.findByName(roleName)
                .flatMap(rol -> userRolRepositoryPort.findRoleByUserId(user.getId())
                        .filter(userRol -> userRol.getId().equals(rol.getId()))
                        .hasElement()
                        .flatMap(hasRole -> {
                            if (!hasRole) {
                                log.debug("User {} does not have role {}", user.getId(), roleName);
                                return Mono.empty();
                            }
                            user.setRol(rol);
                            return rolOperationRepositoryPort.findOperationsByRoleId(rol.getId())
                                    .collectList()
                                    .map(ops -> buildAuthoritiesFromOperations(ops))
                                    .doOnNext(user::setGrantedAuthorities)
                                    .thenReturn(user);
                        }));
    }

    private static List<GrantedAuthority> buildAuthoritiesFromOperations(List<Operation> operations) {
        if (operations == null || operations.isEmpty()) {
            return SecurityConstants.DEFAULT_OPERATION_NAMES.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        return operations.stream()
                .map(Operation::getName)
                .filter(name -> name != null && !name.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
