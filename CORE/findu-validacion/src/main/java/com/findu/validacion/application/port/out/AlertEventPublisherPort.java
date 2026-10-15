package com.findu.validacion.application.port.out;

import com.findu.validacion.application.dto.event.PendingValidationAlertEvent;
import com.findu.validacion.application.dto.event.ValidationResolvedEvent;
import reactor.core.publisher.Mono;

public interface AlertEventPublisherPort {
    Mono<Void> publishPending(PendingValidationAlertEvent event);
    Mono<Void> publishResolved(ValidationResolvedEvent event);
}
