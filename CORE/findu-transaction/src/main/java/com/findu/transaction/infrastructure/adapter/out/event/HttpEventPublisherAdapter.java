package com.findu.transaction.infrastructure.adapter.out.event;

import com.findu.transaction.application.port.out.event.EventPublisherPort;
import com.findu.transaction.domain.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpEventPublisherAdapter implements EventPublisherPort {

    @Override
    public void publish(DomainEvent event) {
        if (event == null) return;
        log.info("Evento de dominio publicado en findu-transaction: {} [OccurredOn: {}]",
                event.getClass().getSimpleName(), event.occurredOn());
    }
}
