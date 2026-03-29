package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para token de recuperación de contraseña.
 * Un solo uso; expira en un tiempo configurado.
 */
@Getter
@Setter
public class PasswordRecoveryToken {

    private Long id;
    private Long userId;
    private String token;
    private Instant expiresAt;
    private boolean used;
}
