package com.findu.notification.processor.presentation.controller;

import com.findu.notification.processor.application.router.FinancialEventRouter;
import com.findu.notification.processor.dto.financial.FinancialEventEnvelopeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications/events/financial")
public class FinancialEventConsumerController {

    private static final Logger log = LoggerFactory.getLogger(FinancialEventConsumerController.class);
    private final FinancialEventRouter eventRouter;

    public FinancialEventConsumerController(FinancialEventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> consumeFinancialEvent(@RequestBody FinancialEventEnvelopeDto event) {
        log.info("REST Consumer: Recibido evento financiero eventType={} [eventId={}] desde producer={}",
                event.getEventType(), event.getEventId(), event.getProducer());
        return eventRouter.route(event);
    }
}
