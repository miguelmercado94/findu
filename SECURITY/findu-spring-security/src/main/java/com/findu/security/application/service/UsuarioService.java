package com.findu.security.application.service;

import com.findu.security.domain.model.Usuario;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Servicio de aplicación para Usuario.
 * Interface que consumirá el use case / manager (y este el controlador).
 */
public interface UsuarioService {

    Flux<Usuario> findAll();

    Mono<Usuario> findById(Long id);

    Mono<Usuario> getUserByUsername(String username);

    Mono<Usuario> getUserByEmail(String email);

    /**
     * Carga usuario por username verificando que tenga el rol indicado en BD.
     * Consulta las operaciones del rol (rol_operation).
     * Setea rol y authorities en el usuario.
     */
    Mono<Usuario> getUserByUsernameWithRole(String username, String roleName);

    /**
     * Carga usuario por email verificando que tenga el rol indicado en BD.
     * Misma lógica que getUserByUsernameWithRole.
     */
    Mono<Usuario> getUserByEmailWithRole(String email, String roleName);

    Mono<Usuario> save(Usuario usuario);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByPhone(String phone);
}
