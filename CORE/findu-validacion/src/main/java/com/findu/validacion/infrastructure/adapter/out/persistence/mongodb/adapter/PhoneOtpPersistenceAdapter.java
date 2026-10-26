package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.adapter;

import com.findu.validacion.application.port.out.PhoneOtpRepositoryPort;
import com.findu.validacion.domain.model.PhoneOtpRecord;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.PhoneOtpDocument;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper.PhoneOtpMapper;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository.SpringDataReactivePhoneOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PhoneOtpPersistenceAdapter implements PhoneOtpRepositoryPort {

    private final SpringDataReactivePhoneOtpRepository repository;
    private final PhoneOtpMapper mapper;

    @Override
    public Mono<PhoneOtpRecord> saveOtp(PhoneOtpRecord record) {
        return repository.save(mapper.toDocument(record))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<PhoneOtpRecord> findLatestOtp(String phoneKey, String processCode) {
        return repository.findFirstByPhoneKeyAndProcessCodeOrderByCreatedAtDesc(phoneKey, processCode)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> markPhoneVerified(String phoneKey, String processCode, Duration ttl) {
        return repository.findFirstByPhoneKeyAndProcessCodeOrderByCreatedAtDesc(phoneKey, processCode)
                .flatMap(doc -> {
                    doc.setVerified(true);
                    doc.setVerifiedAt(Instant.now());
                    doc.setExpiresAt(Instant.now().plus(ttl));
                    return repository.save(doc);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    PhoneOtpDocument newDoc = PhoneOtpDocument.builder()
                            .phoneKey(phoneKey)
                            .processCode(processCode)
                            .verified(true)
                            .verifiedAt(Instant.now())
                            .expiresAt(Instant.now().plus(ttl))
                            .createdAt(Instant.now())
                            .build();
                    return repository.save(newDoc);
                }))
                .then();
    }

    @Override
    public Mono<Boolean> isPhoneVerified(String phoneKey, String processCode) {
        return repository.findFirstByPhoneKeyAndProcessCodeOrderByCreatedAtDesc(phoneKey, processCode)
                .map(doc -> doc.isVerified() && (doc.getExpiresAt() == null || Instant.now().isBefore(doc.getExpiresAt())))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> deleteOtp(String phoneKey, String processCode) {
        return repository.deleteAllByPhoneKeyAndProcessCode(phoneKey, processCode);
    }
}
