package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ExtraExpenseRejectedNotificationHandler implements FinancialNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ExtraExpenseRejectedNotificationHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "EXTRA_EXPENSE_REJECTED".equalsIgnoreCase(eventType);
    }

    @Override
    public Mono<Void> handle(FinancialEventEnvelopeDto event) {
        Map<String, Object> payload = event.getPayload();
        Object providerId = payload != null ? payload.get("providerId") : null;
        Object reason = payload != null ? payload.get("reason") : null;

        log.info("PROC-NOTIF: Procesando EXTRA_EXPENSE_REJECTED [eventId={}, providerId={}, reason={}]",
                event.getEventId(), providerId, reason);

        return Mono.empty();
    }
}
