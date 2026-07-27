package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.PasswordRecoveryCodeRepositoryPort;
import com.findu.security.domain.model.PasswordRecoveryCode;
import com.findu.security.infrastructure.entity.PasswordRecoveryCodeEntity;
import com.findu.security.infrastructure.repository.PasswordRecoveryCodeRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PasswordRecoveryCodeRepoAdapter implements PasswordRecoveryCodeRepositoryPort {
    private final PasswordRecoveryCodeRepository repository;

    public PasswordRecoveryCodeRepoAdapter(PasswordRecoveryCodeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<PasswordRecoveryCode> save(PasswordRecoveryCode domain) {
        PasswordRecoveryCodeEntity entity = toEntity(domain);
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Flux<PasswordRecoveryCode> findActiveByUserId(Long userId) {
        return repository.findByUserIdAndUsedFalse(userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> markAsUsed(Long id) {
        return repository.findById(id)
                .flatMap(entity -> {
                    entity.setUsed(true);
                    return repository.save(entity);
                })
                .then();
    }

    private PasswordRecoveryCode toDomain(PasswordRecoveryCodeEntity entity) {
        PasswordRecoveryCode domain = new PasswordRecoveryCode();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setCode(entity.getCode());
        domain.setEmittedAt(entity.getEmittedAt());
        domain.setExpiresAt(entity.getExpiresAt());
        domain.setUsed(entity.isUsed());
        return domain;
    }

    private PasswordRecoveryCodeEntity toEntity(PasswordRecoveryCode domain) {
        PasswordRecoveryCodeEntity entity = new PasswordRecoveryCodeEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setCode(domain.getCode());
        entity.setEmittedAt(domain.getEmittedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsed(domain.isUsed());
        return entity;
    }
}
