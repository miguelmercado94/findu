package com.findu.security.application.usecase;

import com.findu.security.dto.request.UserRegisterDto;
import com.findu.security.dto.response.SaveUserResponse;
import reactor.core.publisher.Mono;

/**
 * Caso de uso: registro de un nuevo cliente (usuario).
 * Recibe el DTO de entrada y devuelve el DTO de respuesta.
 */
public interface CustomerManager {

    /**
     * Registra un nuevo cliente con los datos del request y el rol indicado.
     * Genera access + refresh JWT según el algoritmo indicado (ej. HS256).
     *
     * @param request   datos del usuario y nombre del rol
     * @param algorithm algoritmo JWT del header (ej. HS256); por defecto HS256
     * @return respuesta con datos del usuario guardado, rol, operaciones y tokens (jwt, jwtRefresh)
     */
    Mono<SaveUserResponse> registerNewCustomer(UserRegisterDto request, String algorithm);
}
