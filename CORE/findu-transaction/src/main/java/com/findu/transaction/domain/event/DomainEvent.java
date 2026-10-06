package com.findu.transaction.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
}
