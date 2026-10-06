package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class PaymentFailedNotificationHandler implements FinancialNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedNotificationHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "PAYMENT_FAILED".equalsIgnoreCase(eventType);
    }

    @Override
    public Mono<Void> handle(FinancialEventEnvelopeDto event) {
        Map<String, Object> payload = event.getPayload();
        Object customerId = payload != null ? payload.get("customerId") : null;
        Object reason = payload != null ? payload.get("reason") : null;

        log.info("PROC-NOTIF: Procesando PAYMENT_FAILED [eventId={}, customerId={}, reason={}]",
                event.getEventId(), customerId, reason);

        return Mono.empty();
    }
}
