package com.findu.help.v2.application.port.out.event;

import com.findu.help.v2.domain.event.DomainEvent;

public interface RealtimeEventPublisher {
    void publishRealtime(DomainEvent event);
}
