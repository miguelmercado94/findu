package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.application.dto.request.FinancialEventEnvelopeDto;
import reactor.core.publisher.Mono;

public interface FinancialNotificationHandler {
    boolean supports(String eventType);
    Mono<Void> handle(FinancialEventEnvelopeDto event);
}
