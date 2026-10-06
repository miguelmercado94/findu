package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class PayoutFailedNotificationHandler implements FinancialNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PayoutFailedNotificationHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "PAYOUT_FAILED".equalsIgnoreCase(eventType);
    }

    @Override
    public Mono<Void> handle(FinancialEventEnvelopeDto event) {
        Map<String, Object> payload = event.getPayload();
        Object providerId = payload != null ? payload.get("providerId") : null;
        Object reason = payload != null ? payload.get("reason") : null;

        log.info("PROC-NOTIF: Procesando PAYOUT_FAILED [eventId={}, providerId={}, reason={}]",
                event.getEventId(), providerId, reason);

        return Mono.empty();
    }
}
