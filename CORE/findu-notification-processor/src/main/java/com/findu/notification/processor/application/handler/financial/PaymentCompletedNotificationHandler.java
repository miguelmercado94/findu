package com.findu.notification.processor.application.handler.financial;

import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class PaymentCompletedNotificationHandler implements FinancialNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedNotificationHandler.class);

    @Override
    public boolean supports(String eventType) {
        return "PAYMENT_COMPLETED".equalsIgnoreCase(eventType);
    }

    @Override
    public Mono<Void> handle(FinancialEventEnvelopeDto event) {
        Map<String, Object> payload = event.getPayload();
        Object customerId = payload != null ? payload.get("customerId") : null;
        Object providerId = payload != null ? payload.get("providerId") : null;
        Object amount = payload != null ? payload.get("amount") : null;

        log.info("PROC-NOTIF: Procesando PAYMENT_COMPLETED [eventId={}, customerId={}, providerId={}, amount={}]",
                event.getEventId(), customerId, providerId, amount);

        // Decisión de canal: Si la sesión Realtime (WebSocket/STOMP) está activa, enviar via Realtime; de lo contrario Dispatcher / FCM.
        boolean isRealtimeAvailable = checkRealtimeActive(customerId);

        if (isRealtimeAvailable) {
            log.info("Canal seleccionado: Realtime WebSocket/STOMP para customerId={}", customerId);
        } else {
            log.info("Canal seleccionado: Asíncrono Dispatcher / FCM Push para customerId={}", customerId);
        }

        return Mono.empty();
    }

    private boolean checkRealtimeActive(Object userId) {
        return userId != null && userId.hashCode() % 2 == 0;
    }
}
