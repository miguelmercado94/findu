package com.findu.validacion.application.port.out;

import com.findu.validacion.domain.model.EndpointValidationConfig;
import reactor.core.publisher.Mono;

public interface EndpointValidationConfigRepositoryPort {
    Mono<EndpointValidationConfig> findByProcessCode(String processCode);
    Mono<EndpointValidationConfig> save(EndpointValidationConfig config);
    Mono<Boolean> existsByProcessCode(String processCode);
}
