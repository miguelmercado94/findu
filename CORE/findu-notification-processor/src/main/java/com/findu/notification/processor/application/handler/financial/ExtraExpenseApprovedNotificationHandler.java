package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ExtraExpenseApprovedNotificationHandler implements FinancialNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ExtraExpenseApprovedNotificationHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "EXTRA_EXPENSE_APPROVED".equalsIgnoreCase(eventType);
    }

    @Override
    public Mono<Void> handle(FinancialEventEnvelopeDto event) {
        Map<String, Object> payload = event.getPayload();
        Object providerId = payload != null ? payload.get("providerId") : null;
        Object approvedAmount = payload != null ? payload.get("approvedAmount") : null;

        log.info("PROC-NOTIF: Procesando EXTRA_EXPENSE_APPROVED [eventId={}, providerId={}, approvedAmount={}]",
                event.getEventId(), providerId, approvedAmount);

        return Mono.empty();
    }
}
