package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ProviderDebtCreatedNotificationHandler implements FinancialNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ProviderDebtCreatedNotificationHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "PROVIDER_DEBT_CREATED".equalsIgnoreCase(eventType);
    }

    @Override
    public Mono<Void> handle(FinancialEventEnvelopeDto event) {
        Map<String, Object> payload = event.getPayload();
        Object providerId = payload != null ? payload.get("providerId") : null;
        Object amount = payload != null ? payload.get("amount") : null;

        log.info("PROC-NOTIF: Procesando PROVIDER_DEBT_CREATED [eventId={}, providerId={}, amount={}]",
                event.getEventId(), providerId, amount);

        return Mono.empty();
    }
}
