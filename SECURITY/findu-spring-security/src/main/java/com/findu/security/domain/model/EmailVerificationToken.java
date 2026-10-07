package com.findu.security.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Token de verificación asíncrona de correo del proveedor.
 * Se persiste únicamente el hash del token (nunca el valor en claro).
 */
@Getter
@Setter
public class EmailVerificationToken {
    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;
}
