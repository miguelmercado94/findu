package com.findu.validacion.application.port.out;

import com.findu.validacion.domain.enums.HttpMethod;
import com.findu.validacion.domain.model.MonitoredEndpoint;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MonitoredEndpointRepositoryPort {
    Mono<MonitoredEndpoint> findByProcessCode(String processCode);
    Mono<MonitoredEndpoint> findByServiceAndMethodAndPath(String serviceName, HttpMethod httpMethod, String path);
    Mono<MonitoredEndpoint> save(MonitoredEndpoint endpoint);
    Flux<MonitoredEndpoint> findAll();
    Mono<Boolean> existsByProcessCode(String processCode);
}
