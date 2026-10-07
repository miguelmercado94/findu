package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.EmailVerificationToken;
import reactor.core.publisher.Mono;

/**
 * Puerto de persistencia para los tokens de verificación de correo.
 */
public interface EmailVerificationTokenRepositoryPort {
    Mono<EmailVerificationToken> save(EmailVerificationToken domain);
    Mono<EmailVerificationToken> findByTokenHash(String tokenHash);
    Mono<Void> markUsed(Long id);
}
