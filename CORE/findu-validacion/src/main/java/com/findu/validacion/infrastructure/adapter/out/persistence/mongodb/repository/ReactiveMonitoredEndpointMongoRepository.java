package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository;

import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.document.MonitoredEndpointDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReactiveMonitoredEndpointMongoRepository extends ReactiveMongoRepository<MonitoredEndpointDocument, String> {

    Mono<MonitoredEndpointDocument> findByProcessCode(String processCode);

    Mono<MonitoredEndpointDocument> findByServiceNameAndHttpMethodAndPathPattern(
            String serviceName, String httpMethod, String pathPattern);

    Flux<MonitoredEndpointDocument> findByServiceNameAndHttpMethod(String serviceName, String httpMethod);

    Mono<Boolean> existsByProcessCode(String processCode);
}
