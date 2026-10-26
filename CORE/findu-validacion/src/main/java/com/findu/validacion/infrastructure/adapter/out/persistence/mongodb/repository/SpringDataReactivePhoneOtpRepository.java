package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository;

import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.PhoneOtpDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataReactivePhoneOtpRepository
        extends ReactiveMongoRepository<PhoneOtpDocument, String> {

    Mono<PhoneOtpDocument> findFirstByPhoneKeyAndProcessCodeOrderByCreatedAtDesc(
            String phoneKey, String processCode);

    Mono<Void> deleteAllByPhoneKeyAndProcessCode(String phoneKey, String processCode);
}
