package com.findu.notification.processor.application.router;

import com.findu.notification.processor.application.handler.financial.FinancialNotificationHandler;
import com.findu.notification.processor.domain.service.FinancialEventDeduplicationService;
import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class FinancialEventRouter {

    private static final Logger log = LoggerFactory.getLogger(FinancialEventRouter.class);

    private final List<FinancialNotificationHandler> handlers;
    private final FinancialEventDeduplicationService deduplicationService;

    public FinancialEventRouter(
            List<FinancialNotificationHandler> handlers,
            FinancialEventDeduplicationService deduplicationService) {
        this.handlers = handlers;
        this.deduplicationService = deduplicationService;
    }

    public Mono<Void> route(FinancialEventEnvelopeDto event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Evento financiero nulo o sin eventType recibido");
            return Mono.empty();
        }

        if (deduplicationService.isDuplicate(event.getEventId())) {
            log.warn("PROC-NOTIF: Evento financiero duplicado omitido por idempotencia: eventId={}", event.getEventId());
            return Mono.empty();
        }

        log.info("PROC-NOTIF: Enrutando evento financiero eventType={} [eventId={}, producer={}]",
                event.getEventType(), event.getEventId(), event.getProducer());

        return handlers.stream()
                .filter(h -> h.supports(event.getEventType()))
                .findFirst()
                .map(h -> h.handle(event))
                .orElseGet(() -> {
                    log.info("No se encontró handler de notificación específico para eventType={}. Evento ignorado silenciosamente.",
                            event.getEventType());
                    return Mono.empty();
                });
    }
}
