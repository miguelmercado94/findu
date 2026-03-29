package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.Rol;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para la relación usuario-rol (user_rol).
 */
public interface UserRolRepositoryPort {

    /**
     * Asigna un rol a un usuario (insert en user_rol).
     */
    Mono<Void> assignRoleToUser(Long userId, Integer roleId);

    /**
     * Obtiene el primer rol asignado al usuario (para JWT/extraClaims).
     */
    Mono<Rol> findRoleByUserId(Long userId);
}
