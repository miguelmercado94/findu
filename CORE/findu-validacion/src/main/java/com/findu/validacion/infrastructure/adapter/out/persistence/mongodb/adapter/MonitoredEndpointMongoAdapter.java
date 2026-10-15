package com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.adapter;

import com.findu.validacion.application.port.out.MonitoredEndpointRepositoryPort;
import com.findu.validacion.domain.enums.HttpMethod;
import com.findu.validacion.domain.model.MonitoredEndpoint;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.mapper.MonitoredEndpointMapper;
import com.findu.validacion.infrastructure.adapter.out.persistence.mongodb.repository.ReactiveMonitoredEndpointMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoredEndpointMongoAdapter implements MonitoredEndpointRepositoryPort {

    private final ReactiveMonitoredEndpointMongoRepository repository;
    private final MonitoredEndpointMapper mapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<MonitoredEndpoint> findByProcessCode(String processCode) {
        return repository.findByProcessCode(processCode)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<MonitoredEndpoint> findByServiceAndMethodAndPath(String serviceName, HttpMethod httpMethod, String path) {
        String methodStr = httpMethod != null ? httpMethod.name() : null;

        // First attempt: exact match on pathPattern
        return repository.findByServiceNameAndHttpMethodAndPathPattern(serviceName, methodStr, path)
                .map(mapper::toDomain)
                .switchIfEmpty(
                        // Second attempt: pattern matching using AntPathMatcher
                        repository.findByServiceNameAndHttpMethod(serviceName, methodStr)
                                .filter(doc -> doc.isActive() && doc.getPathPattern() != null
                                        && pathMatcher.match(doc.getPathPattern(), path))
                                .next()
                                .map(mapper::toDomain)
                );
    }

    @Override
    public Mono<MonitoredEndpoint> save(MonitoredEndpoint endpoint) {
        return repository.save(mapper.toDocument(endpoint))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<MonitoredEndpoint> findAll() {
        return repository.findAll()
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByProcessCode(String processCode) {
        return repository.existsByProcessCode(processCode);
    }
}
