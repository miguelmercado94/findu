package com.findu.security.application.usecase.impl;

import com.findu.security.application.port.output.persistence.RolOperationRepositoryPort;
import com.findu.security.application.port.output.persistence.RolRepositoryPort;
import com.findu.security.application.port.output.persistence.UserRolRepositoryPort;
import com.findu.security.application.service.UsuarioService;
import com.findu.security.application.usecase.CustomerManager;
import com.findu.security.application.usecase.JwtManager;
import com.findu.security.domain.model.Operation;
import com.findu.security.domain.model.Usuario;
import com.findu.security.dto.request.UserRegisterDto;
import com.findu.security.dto.response.SaveUserResponse;
import com.findu.security.exception.ResourceNotFoundException;
import com.findu.security.util.SecurityConstants;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del caso de uso de registro de cliente.
 * Orquesta UsuarioService, RolRepositoryPort y PasswordEncoder.
 */
@Service
public class CustomerManagerImpl implements CustomerManager {

    private final UsuarioService usuarioService;
    private final RolRepositoryPort rolRepositoryPort;
    private final UserRolRepositoryPort userRolRepositoryPort;
    private final RolOperationRepositoryPort rolOperationRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtManager jwtManager;

    public CustomerManagerImpl(UsuarioService usuarioService,
                              RolRepositoryPort rolRepositoryPort,
                              UserRolRepositoryPort userRolRepositoryPort,
                              RolOperationRepositoryPort rolOperationRepositoryPort,
                              PasswordEncoder passwordEncoder,
                              JwtManager jwtManager) {
        this.usuarioService = usuarioService;
        this.rolRepositoryPort = rolRepositoryPort;
        this.userRolRepositoryPort = userRolRepositoryPort;
        this.rolOperationRepositoryPort = rolOperationRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtManager = jwtManager;
    }

    @Override
    public Mono<SaveUserResponse> registerNewCustomer(UserRegisterDto request, String algorithm) {
        Mono<Void> validation = validateUserNotExists(request.username(), request.email(), request.phone());
        Mono<SaveUserResponse> saveAndRespond = Mono.defer(() -> {
            Usuario usuario = new Usuario();
            usuario.setUsername(request.username());
            usuario.setEmail(request.email());
            usuario.setPhone(request.phone());
            usuario.setPassword(passwordEncoder.encode(request.password()));
            usuario.setActive(true);
            return usuarioService.save(usuario)
                    .flatMap(savedUser -> rolRepositoryPort.findByName(request.roleName())
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rol no encontrado: " + request.roleName())))
                            .flatMap(rol -> userRolRepositoryPort.assignRoleToUser(savedUser.getId(), rol.getId())
                                    .then(Mono.fromCallable(() -> {
                                        savedUser.setRol(rol);
                                        return savedUser;
                                    }))
                                    .flatMap(userWithRol -> setAuthoritiesForUser(userWithRol)
                                            .flatMap(u -> jwtManager.buildTokensForUser(u, algorithm)))
                                    .map(tokens -> buildResponse(savedUser, request.roleName(), tokens.jwt(), tokens.jwtRefresh()))));
        });
        return validation.then(saveAndRespond);
    }

    /**
     * Valida que no exista ya un usuario con el mismo username, email o teléfono.
     * Si alguno existe, devuelve Mono.error con mensaje claro.
     */
    private Mono<Void> validateUserNotExists(String username, String email, String phone) {
        Mono<Boolean> byUser = usuarioService.existsByUsername(username);
        Mono<Boolean> byEmail = usuarioService.existsByEmail(email);
        Mono<Boolean> byPhone = usuarioService.existsByPhone(phone);
        return Mono.zip(byUser, byEmail, byPhone).flatMap(tuple -> {
            if (Boolean.TRUE.equals(tuple.getT1())) {
                return Mono.<Void>error(new IllegalArgumentException("El username ya está registrado"));
            }
            if (Boolean.TRUE.equals(tuple.getT2())) {
                return Mono.<Void>error(new IllegalArgumentException("El email ya está registrado"));
            }
            if (Boolean.TRUE.equals(tuple.getT3())) {
                return Mono.<Void>error(new IllegalArgumentException("El teléfono ya está registrado"));
            }
            return Mono.<Void>empty();
        });
    }

    /** Carga operaciones del rol (o por defecto si vacío) y setea grantedAuthorities en el usuario. */
    private Mono<Usuario> setAuthoritiesForUser(Usuario user) {
        if (user.getRol() == null) {
            user.setGrantedAuthorities(SecurityConstants.DEFAULT_OPERATION_NAMES.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));
            return Mono.just(user);
        }
        return rolOperationRepositoryPort.findOperationsByRoleId(user.getRol().getId())
                .collectList()
                .map(ops -> {
                    List<GrantedAuthority> auth = ops.isEmpty()
                            ? SecurityConstants.DEFAULT_OPERATION_NAMES.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                            : ops.stream()
                                    .map(Operation::getName)
                                    .filter(name -> name != null && !name.isBlank())
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                    user.setGrantedAuthorities(auth);
                    return user;
                });
    }

    private static SaveUserResponse buildResponse(Usuario savedUser, String roleName, String jwt, String jwtRefresh) {
        return new SaveUserResponse(savedUser.getUsername(), savedUser.getEmail(),
                savedUser.getPhone(), roleName, Collections.<String>emptyList(), jwt, jwtRefresh);
    }
}

