package com.findu.security.application.port.output.persistence;

import com.findu.security.domain.model.PasswordRecoveryCode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PasswordRecoveryCodeRepositoryPort {
    Mono<PasswordRecoveryCode> save(PasswordRecoveryCode domain);
    Flux<PasswordRecoveryCode> findActiveByUserId(Long userId);
    Mono<Void> markAsUsed(Long id);
}
