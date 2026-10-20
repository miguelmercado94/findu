package com.findu.transaction.application.port.out.event;

import com.findu.transaction.domain.event.DomainEvent;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}
