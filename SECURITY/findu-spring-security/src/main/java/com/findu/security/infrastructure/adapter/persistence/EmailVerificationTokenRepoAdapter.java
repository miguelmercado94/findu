package com.findu.security.infrastructure.adapter.persistence;

import com.findu.security.application.port.output.persistence.EmailVerificationTokenRepositoryPort;
import com.findu.security.domain.model.EmailVerificationToken;
import com.findu.security.infrastructure.entity.EmailVerificationTokenEntity;
import com.findu.security.infrastructure.repository.EmailVerificationTokenRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class EmailVerificationTokenRepoAdapter implements EmailVerificationTokenRepositoryPort {

    private final EmailVerificationTokenRepository repository;

    public EmailVerificationTokenRepoAdapter(EmailVerificationTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<EmailVerificationToken> save(EmailVerificationToken domain) {
        return repository.save(toEntity(domain))
                .map(this::toDomain);
    }

    @Override
    public Mono<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> markUsed(Long id) {
        return repository.findById(id)
                .flatMap(entity -> {
                    entity.setUsed(true);
                    return repository.save(entity);
                })
                .then();
    }

    private EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
        EmailVerificationToken domain = new EmailVerificationToken();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setTokenHash(entity.getTokenHash());
        domain.setExpiresAt(entity.getExpiresAt());
        domain.setUsed(entity.isUsed());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    private EmailVerificationTokenEntity toEntity(EmailVerificationToken domain) {
        EmailVerificationTokenEntity entity = new EmailVerificationTokenEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setTokenHash(domain.getTokenHash());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsed(domain.isUsed());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
