package com.findu.validacion.application.port.out;

import com.findu.validacion.domain.enums.ValidationStatus;
import com.findu.validacion.domain.model.ProcessValidationInstance;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProcessValidationInstanceRepositoryPort {
    Mono<ProcessValidationInstance> save(ProcessValidationInstance instance);
    Mono<ProcessValidationInstance> findByEntityAndProcessAndValidation(
            String entityType, String entityId, String processCode, String validationCode);
    Flux<ProcessValidationInstance> findPendingManual(String processCode);
    Flux<ProcessValidationInstance> findByEntityAndStatus(String entityType, String entityId, ValidationStatus status);
    Flux<ProcessValidationInstance> findAll();
}
