package com.findu.help.v2.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
}
