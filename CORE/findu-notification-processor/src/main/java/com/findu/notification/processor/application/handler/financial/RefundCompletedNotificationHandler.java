package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class RefundCompletedNotificationHandler implements FinancialNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(RefundCompletedNotificationHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "REFUND_COMPLETED".equalsIgnoreCase(eventType);
    }

    @Override
    public Mono<Void> handle(FinancialEventEnvelopeDto event) {
        Map<String, Object> payload = event.getPayload();
        Object customerId = payload != null ? payload.get("customerId") : null;
        Object amount = payload != null ? payload.get("amount") : null;

        log.info("PROC-NOTIF: Procesando REFUND_COMPLETED [eventId={}, customerId={}, amount={}]",
                event.getEventId(), customerId, amount);

        return Mono.empty();
    }
}
