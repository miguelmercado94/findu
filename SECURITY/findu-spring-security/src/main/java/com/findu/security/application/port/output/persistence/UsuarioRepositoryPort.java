package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.Usuario;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida (output port) para persistencia de Usuario.
 * La aplicación/dominio define el contrato; el adapter en infrastructure lo implementa
 * usando el Mapper (domain ↔ entity) y los repositorios de infraestructura.
 */
public interface UsuarioRepositoryPort {

    Flux<Usuario> findAll();

    Mono<Usuario> findById(Long id);

    Mono<Usuario> getByUsername(String username);

    Mono<Usuario> findByEmail(String email);

    Mono<Usuario> save(Usuario usuario);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByPhone(String phone);

    /**
     * Actualiza la contraseña del usuario (ya codificada, p. ej. BCrypt).
     */
    Mono<Void> updatePassword(Long userId, String encodedPassword);
}
